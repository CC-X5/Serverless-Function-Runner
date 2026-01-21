package hskl.cn.serverless.executor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hskl.cn.serverless.executor.dto.ExecutionRequest;
import hskl.cn.serverless.executor.dto.ExecutionResponse;
import hskl.cn.serverless.executor.dto.ExecutionResponse.ExecutionStatus;
import hskl.cn.serverless.executor.service.DockerExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ExecutionController.
 * Uses @WebMvcTest for isolated controller testing.
 */
@WebMvcTest(ExecutionController.class)
@DisplayName("ExecutionController Tests")
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DockerExecutionService executionService;

    @Nested
    @DisplayName("POST /api/v1/execute/{functionName}")
    class ExecuteByNameEndpoint {

        @Test
        @DisplayName("should execute function successfully")
        void shouldExecuteFunctionSuccessfully() throws Exception {
            // Given
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-123")
                    .functionName("hello")
                    .status(ExecutionStatus.SUCCESS)
                    .result("Hello, World!")
                    .durationMs(150L)
                    .startedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();

            when(executionService.execute(any(ExecutionRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/execute/hello")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"World\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.executionId").value("exec-123"))
                    .andExpect(jsonPath("$.functionName").value("hello"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.result").value("Hello, World!"))
                    .andExpect(jsonPath("$.durationMs").value(150));
        }

        @Test
        @DisplayName("should handle execution failure")
        void shouldHandleExecutionFailure() throws Exception {
            // Given
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-456")
                    .functionName("failing-function")
                    .status(ExecutionStatus.FAILED)
                    .error("NullPointerException: Something went wrong")
                    .startedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();

            when(executionService.execute(any(ExecutionRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/execute/failing-function")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("should handle execution timeout")
        void shouldHandleExecutionTimeout() throws Exception {
            // Given
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-789")
                    .functionName("slow-function")
                    .status(ExecutionStatus.TIMEOUT)
                    .error("Execution timed out after 30 seconds")
                    .startedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();

            when(executionService.execute(any(ExecutionRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/execute/slow-function")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("TIMEOUT"));
        }

        @Test
        @DisplayName("should execute without payload")
        void shouldExecuteWithoutPayload() throws Exception {
            // Given
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-no-payload")
                    .functionName("no-input")
                    .status(ExecutionStatus.SUCCESS)
                    .result("Done")
                    .build();

            when(executionService.execute(any(ExecutionRequest.class))).thenReturn(response);

            // When/Then - no body at all
            mockMvc.perform(post("/api/v1/execute/no-input")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/execute")
    class ExecuteWithBodyEndpoint {

        @Test
        @DisplayName("should execute function with full request body")
        void shouldExecuteWithFullBody() throws Exception {
            // Given
            ExecutionRequest request = ExecutionRequest.builder()
                    .functionName("my-function")
                    .payload(Map.of("key", "value"))
                    .async(false)
                    .build();

            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-body")
                    .functionName("my-function")
                    .status(ExecutionStatus.SUCCESS)
                    .result("{\"processed\": true}")
                    .build();

            when(executionService.execute(any(ExecutionRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.executionId").value("exec-body"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("should handle async execution request")
        void shouldHandleAsyncRequest() throws Exception {
            // Given
            ExecutionRequest request = ExecutionRequest.builder()
                    .functionName("async-function")
                    .payload(Map.of("data", "test"))
                    .async(true)
                    .build();

            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId("exec-async")
                    .functionName("async-function")
                    .status(ExecutionStatus.SUCCESS)
                    .build();

            when(executionService.execute(any(ExecutionRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.executionId").value("exec-async"));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should return 500 when service throws exception")
        void shouldReturn500OnServiceException() throws Exception {
            // Given
            when(executionService.execute(any(ExecutionRequest.class)))
                    .thenThrow(new RuntimeException("Docker connection failed"));

            // When/Then
            mockMvc.perform(post("/api/v1/execute/test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
