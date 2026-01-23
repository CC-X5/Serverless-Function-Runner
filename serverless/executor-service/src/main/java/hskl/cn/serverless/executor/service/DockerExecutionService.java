package hskl.cn.serverless.executor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Executes serverless functions in isolated Docker containers.
 * 
 * This service downloads JAR files from MinIO and uses Docker's copy-to-container
 * API to transfer the JAR into a newly created container. This approach avoids
 * the volume mount path issues that occur in Docker-in-Docker scenarios.
 */
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

    /**
     * Executes a function in an isolated Docker container.
     * 
     * The execution flow:
     * 1. Validate function exists and is ready
     * 2. Download JAR from MinIO
     * 3. Create container (without starting)
     * 4. Copy JAR into container using Docker API
     * 5. Start container and capture output
     * 6. Cleanup container and temp files
     */
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

            // Create container WITHOUT volume mounts - we'll copy the JAR in
            CreateContainerResponse container = dockerClient.createContainerCmd(dockerConfig.getRuntimeImage())
                    .withName("fn-" + executionId)
                    .withCmd("java", "-jar", "/app/function.jar", payloadJson)
                    .withHostConfig(HostConfig.newHostConfig()
                            .withMemory((long) function.getMemoryMb() * 1024 * 1024)
                            .withCpuCount(1L)
                            .withNetworkMode("none"))
                    .withWorkingDir("/app")
                    .withLabels(java.util.Map.of(
                            "function", function.getName(),
                            "execution-id", executionId
                    ))
                    .exec();

            containerId = container.getId();
            log.info("Created container: {}", containerId);

            // Copy JAR into container using Docker API (works in Docker-in-Docker!)
            copyFileToContainer(containerId, tempJarPath, "/app/function.jar");
            log.info("Copied JAR into container");

            // Now start the container
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
                            log.error("Error reading container logs", throwable);
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

    /**
     * Downloads a JAR file from MinIO to a temporary location.
     */
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

    /**
     * Copies a file into a Docker container using the Docker API.
     * 
     * This method creates a TAR archive containing the file and uses
     * Docker's copyArchiveToContainer API to transfer it. This approach
     * works reliably in Docker-in-Docker scenarios where volume mounts
     * would fail due to path resolution issues.
     */
    private void copyFileToContainer(String containerId, Path sourceFile, String destPath) throws Exception {
        // Docker API requires TAR format for copying files
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream(baos)) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            
            // Extract just the filename from destPath
            String fileName = destPath.substring(destPath.lastIndexOf('/') + 1);
            
            TarArchiveEntry entry = new TarArchiveEntry(sourceFile.toFile(), fileName);
            entry.setSize(Files.size(sourceFile));
            tarOut.putArchiveEntry(entry);
            Files.copy(sourceFile, tarOut);
            tarOut.closeArchiveEntry();
            tarOut.finish();
        }

        // Extract directory from destPath
        String destDir = destPath.substring(0, destPath.lastIndexOf('/'));
        if (destDir.isEmpty()) {
            destDir = "/";
        }

        // Copy the TAR archive to the container
        try (InputStream tarInput = new ByteArrayInputStream(baos.toByteArray())) {
            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(tarInput)
                    .withRemotePath(destDir)
                    .exec();
        }
    }
}
