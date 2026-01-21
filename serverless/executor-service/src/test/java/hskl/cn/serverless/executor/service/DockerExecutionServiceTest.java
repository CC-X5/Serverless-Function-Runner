package hskl.cn.serverless.executor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import hskl.cn.serverless.executor.config.DockerConfig;
import hskl.cn.serverless.executor.dto.ExecutionRequest;
import hskl.cn.serverless.executor.dto.ExecutionResponse;
import hskl.cn.serverless.executor.dto.ExecutionResponse.ExecutionStatus;
import hskl.cn.serverless.executor.dto.FunctionInfo;
import hskl.cn.serverless.executor.exception.ExecutionException;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DockerExecutionService.
 * Tests function execution logic with mocked Docker client.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DockerExecutionService Tests")
class DockerExecutionServiceTest {

    @Mock
    private DockerClient dockerClient;

    @Mock
    private DockerConfig dockerConfig;

    @Mock
    private RegistryClient registryClient;

    @Mock
    private MinioClient minioClient;

    private ObjectMapper objectMapper;
    private DockerExecutionService executionService;

    private FunctionInfo testFunctionInfo;
    private ExecutionRequest executionRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Create service with mocks - we'll test individual components
        testFunctionInfo = FunctionInfo.builder()
                .id(UUID.randomUUID())
                .name("test-function")
                .runtime("java17")
                .handler("com.example.Handler::handle")
                .jarPath("test-function/test.jar")
                .status("READY")
                .timeoutSeconds(30)
                .memoryMb(256)
                .build();

        executionRequest = ExecutionRequest.builder()
                .functionName("test-function")
                .payload(Map.of("name", "World"))
                .async(false)
                .build();
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should reject execution when function not found")
        void shouldRejectWhenFunctionNotFound() {
            // Given
            when(registryClient.getFunction("unknown")).thenReturn(Optional.empty());
            DockerExecutionService service = new DockerExecutionService(
                    dockerClient, dockerConfig, registryClient, objectMapper, minioClient);

            ExecutionRequest request = ExecutionRequest.builder()
                    .functionName("unknown")
                    .payload(Map.of())
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.execute(request))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("Function not found");
        }

        @Test
        @DisplayName("should reject execution when function not ready")
        void shouldRejectWhenFunctionNotReady() {
            // Given
            FunctionInfo pendingFunction = FunctionInfo.builder()
                    .id(UUID.randomUUID())
                    .name("pending-function")
                    .status("PENDING")
                    .jarPath("some/path.jar")
                    .timeoutSeconds(30)
                    .memoryMb(256)
                    .build();
            when(registryClient.getFunction("pending-function")).thenReturn(Optional.of(pendingFunction));
            DockerExecutionService service = new DockerExecutionService(
                    dockerClient, dockerConfig, registryClient, objectMapper, minioClient);

            ExecutionRequest request = ExecutionRequest.builder()
                    .functionName("pending-function")
                    .payload(Map.of())
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.execute(request))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("not ready");
        }

        @Test
        @DisplayName("should reject execution when function has no JAR")
        void shouldRejectWhenNoJar() {
            // Given
            FunctionInfo noJarFunction = FunctionInfo.builder()
                    .id(UUID.randomUUID())
                    .name("no-jar-function")
                    .status("READY")
                    .jarPath(null)
                    .timeoutSeconds(30)
                    .memoryMb(256)
                    .build();
            when(registryClient.getFunction("no-jar-function")).thenReturn(Optional.of(noJarFunction));
            DockerExecutionService service = new DockerExecutionService(
                    dockerClient, dockerConfig, registryClient, objectMapper, minioClient);

            ExecutionRequest request = ExecutionRequest.builder()
                    .functionName("no-jar-function")
                    .payload(Map.of())
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.execute(request))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("no JAR");
        }
    }

    @Nested
    @DisplayName("ExecutionRequest Tests")
    class ExecutionRequestTests {

        @Test
        @DisplayName("should build request with default values")
        void shouldBuildRequestWithDefaults() {
            ExecutionRequest request = ExecutionRequest.builder()
                    .functionName("test")
                    .build();

            assertThat(request.getFunctionName()).isEqualTo("test");
            assertThat(request.isAsync()).isFalse();
        }

        @Test
        @DisplayName("should build request with payload")
        void shouldBuildRequestWithPayload() {
            Map<String, Object> payload = Map.of("key", "value", "number", 42);
            ExecutionRequest request = ExecutionRequest.builder()
                    .functionName("test")
                    .payload(payload)
                    .async(true)
                    .build();

            assertThat(request.getPayload()).containsEntry("key", "value");
            assertThat(request.getPayload()).containsEntry("number", 42);
            assertThat(request.isAsync()).isTrue();
        }
    }

    @Nested
    @DisplayName("ExecutionResponse Tests")
    class ExecutionResponseTests {

        @Test
        @DisplayName("should build success response")
        void shouldBuildSuccessResponse() {
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-123")
                    .functionName("test-function")
                    .status(ExecutionStatus.SUCCESS)
                    .result("Hello, World!")
                    .durationMs(150L)
                    .build();

            assertThat(response.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
            assertThat(response.getResult()).isEqualTo("Hello, World!");
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("should build failed response")
        void shouldBuildFailedResponse() {
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-456")
                    .functionName("test-function")
                    .status(ExecutionStatus.FAILED)
                    .error("NullPointerException")
                    .build();

            assertThat(response.getStatus()).isEqualTo(ExecutionStatus.FAILED);
            assertThat(response.getError()).isEqualTo("NullPointerException");
            assertThat(response.getResult()).isNull();
        }

        @Test
        @DisplayName("should build timeout response")
        void shouldBuildTimeoutResponse() {
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-789")
                    .functionName("slow-function")
                    .status(ExecutionStatus.TIMEOUT)
                    .error("Execution timed out after 30 seconds")
                    .build();

            assertThat(response.getStatus()).isEqualTo(ExecutionStatus.TIMEOUT);
        }
    }

    @Nested
    @DisplayName("FunctionInfo Tests")
    class FunctionInfoTests {

        @Test
        @DisplayName("should create function info with all fields")
        void shouldCreateFunctionInfo() {
            UUID id = UUID.randomUUID();
            FunctionInfo info = FunctionInfo.builder()
                    .id(id)
                    .name("my-function")
                    .runtime("java17")
                    .handler("com.example.Handler::handle")
                    .jarPath("my-function/v1.0.jar")
                    .status("READY")
                    .timeoutSeconds(60)
                    .memoryMb(512)
                    .build();

            assertThat(info.getId()).isEqualTo(id);
            assertThat(info.getName()).isEqualTo("my-function");
            assertThat(info.getRuntime()).isEqualTo("java17");
            assertThat(info.getJarPath()).isEqualTo("my-function/v1.0.jar");
            assertThat(info.getStatus()).isEqualTo("READY");
            assertThat(info.getTimeoutSeconds()).isEqualTo(60);
            assertThat(info.getMemoryMb()).isEqualTo(512);
        }
    }
}
