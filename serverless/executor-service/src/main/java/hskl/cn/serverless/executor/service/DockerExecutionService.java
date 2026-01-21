package hskl.cn.serverless.executor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import hskl.cn.serverless.executor.config.DockerConfig;
import hskl.cn.serverless.executor.dto.ExecutionRequest;
import hskl.cn.serverless.executor.dto.ExecutionResponse;
import hskl.cn.serverless.executor.dto.ExecutionResponse.ExecutionStatus;
import hskl.cn.serverless.executor.dto.FunctionInfo;
import hskl.cn.serverless.executor.exception.ExecutionException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerClient dockerClient;
    private final DockerConfig dockerConfig;
    private final RegistryClient registryClient;
    private final ObjectMapper objectMapper;
    private final MinioClient minioClient;

    @Value("${minio.bucket:functions}")
    private String minioBucket;

    public ExecutionResponse execute(ExecutionRequest request) {
        String executionId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("Starting execution {} for function: {}", executionId, request.getFunctionName());

        FunctionInfo function = registryClient.getFunction(request.getFunctionName())
                .orElseThrow(() -> new ExecutionException("Function not found: " + request.getFunctionName()));

        if (!"READY".equals(function.getStatus())) {
            throw new ExecutionException("Function is not ready: " + function.getStatus());
        }

        if (function.getJarPath() == null || function.getJarPath().isEmpty()) {
            throw new ExecutionException("Function has no JAR uploaded");
        }

        Path tempJarPath = null;
        String containerId = null;
        try {
            // Download JAR from MinIO to temp file
            tempJarPath = downloadJarFromMinio(function.getJarPath(), executionId);
            log.info("Downloaded JAR to: {}", tempJarPath);

            String payloadJson = objectMapper.writeValueAsString(request.getPayload());

            // Create container with volume mount for the JAR
            CreateContainerResponse container = dockerClient.createContainerCmd(dockerConfig.getRuntimeImage())
                    .withName("fn-" + executionId)
                    .withCmd("java", "-jar", "/app/function.jar", payloadJson)
                    .withHostConfig(HostConfig.newHostConfig()
                            .withMemory((long) function.getMemoryMb() * 1024 * 1024)
                            .withCpuCount(1L)
                            .withNetworkMode("none")
                            .withBinds(new Bind(tempJarPath.toAbsolutePath().toString(), 
                                    new Volume("/app/function.jar"))))
                    .withLabels(java.util.Map.of(
                            "function", function.getName(),
                            "execution-id", executionId
                    ))
                    .exec();

            containerId = container.getId();
            log.info("Created container: {}", containerId);

            dockerClient.startContainerCmd(containerId).exec();

            StringBuilder output = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback<Frame>() {
                        @Override
                        public void onStart(Closeable closeable) {}

                        @Override
                        public void onNext(Frame frame) {
                            output.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            latch.countDown();
                        }

                        @Override
                        public void onComplete() {
                            latch.countDown();
                        }

                        @Override
                        public void close() {}
                    });

            boolean completed = latch.await(function.getTimeoutSeconds(), TimeUnit.SECONDS);
            LocalDateTime completedAt = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();

            if (!completed) {
                log.warn("Execution {} timed out", executionId);
                return ExecutionResponse.builder()
                        .executionId(executionId)
                        .functionName(function.getName())
                        .status(ExecutionStatus.TIMEOUT)
                        .error("Execution timed out after " + function.getTimeoutSeconds() + " seconds")
                        .durationMs(durationMs)
                        .startedAt(startedAt)
                        .completedAt(completedAt)
                        .build();
            }

            Integer exitCode = dockerClient.inspectContainerCmd(containerId).exec().getState().getExitCode();

            if (exitCode != null && exitCode == 0) {
                return ExecutionResponse.builder()
                        .executionId(executionId)
                        .functionName(function.getName())
                        .status(ExecutionStatus.SUCCESS)
                        .result(output.toString().trim())
                        .durationMs(durationMs)
                        .startedAt(startedAt)
                        .completedAt(completedAt)
                        .build();
            } else {
                return ExecutionResponse.builder()
                        .executionId(executionId)
                        .functionName(function.getName())
                        .status(ExecutionStatus.FAILED)
                        .error(output.toString().trim())
                        .durationMs(durationMs)
                        .startedAt(startedAt)
                        .completedAt(completedAt)
                        .build();
            }

        } catch (Exception e) {
            log.error("Execution {} failed: {}", executionId, e.getMessage(), e);
            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .functionName(request.getFunctionName())
                    .status(ExecutionStatus.FAILED)
                    .error(e.getMessage())
                    .startedAt(startedAt)
                    .completedAt(LocalDateTime.now())
                    .build();
        } finally {
            // Cleanup container
            if (containerId != null) {
                try {
                    dockerClient.stopContainerCmd(containerId).withTimeout(5).exec();
                } catch (Exception ignored) {}
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {}
            }
            // Cleanup temp JAR file
            if (tempJarPath != null) {
                try {
                    Files.deleteIfExists(tempJarPath);
                    Files.deleteIfExists(tempJarPath.getParent());
                } catch (Exception ignored) {}
            }
        }
    }

    private Path downloadJarFromMinio(String jarPath, String executionId) throws Exception {
        Path tempDir = Files.createTempDirectory("fn-" + executionId);
        Path tempJarPath = tempDir.resolve("function.jar");

        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(jarPath)
                        .build())) {
            Files.copy(is, tempJarPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempJarPath;
    }
}
