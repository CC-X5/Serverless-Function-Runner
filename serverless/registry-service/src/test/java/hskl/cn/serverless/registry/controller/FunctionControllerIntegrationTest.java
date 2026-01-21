package hskl.cn.serverless.registry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hskl.cn.serverless.registry.dto.CreateFunctionRequest;
import hskl.cn.serverless.registry.model.Function;
import hskl.cn.serverless.registry.model.Function.FunctionStatus;
import hskl.cn.serverless.registry.repository.FunctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import io.minio.MinioClient;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FunctionController.
 * Uses H2 in-memory database for testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FunctionController Integration Tests")
class FunctionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FunctionRepository functionRepository;

    @MockBean
    private MinioClient minioClient;

    @BeforeEach
    void setUp() {
        functionRepository.deleteAll();
    }

    // Helper method to create Function without Lombok builder
    private Function createFunction(String name, String runtime, String handler, FunctionStatus status) {
        Function f = new Function();
        f.setName(name);
        f.setRuntime(runtime);
        f.setHandler(handler);
        f.setStatus(status);
        f.setTimeoutSeconds(30);
        f.setMemoryMb(256);
        return f;
    }

    @Nested
    @DisplayName("POST /api/v1/functions")
    class CreateFunctionEndpoint {

        @Test
        @DisplayName("should create function with valid request")
        void shouldCreateFunction() throws Exception {
            // Given
            CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .name("test-function")
                    .runtime("java17")
                    .handler("com.example.Handler::handle")
                    .description("Test function")
                    .timeoutSeconds(30)
                    .memoryMb(256)
                    .build();

            // When
            ResultActions result = mockMvc.perform(post("/api/v1/functions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("test-function"))
                    .andExpect(jsonPath("$.runtime").value("java17"))
                    .andExpect(jsonPath("$.handler").value("com.example.Handler::handle"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 400 for invalid handler format")
        void shouldReturn400ForInvalidHandler() throws Exception {
            // Given
            CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .name("test-function")
                    .runtime("java17")
                    .handler("invalid-handler")  // Missing ::methodName
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/functions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.handler").exists());
        }

        @Test
        @DisplayName("should return 400 for missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            // Given
            String invalidJson = "{}";

            // When/Then
            mockMvc.perform(post("/api/v1/functions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 for duplicate function name")
        void shouldReturn409ForDuplicateName() throws Exception {
            // Given
            Function existing = createFunction("existing-function", "java17", 
                    "com.example.Handler::handle", FunctionStatus.PENDING);
            functionRepository.save(existing);

            CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .name("existing-function")
                    .runtime("java17")
                    .handler("com.example.Handler2::handle")
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/functions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(containsString("existing-function")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/functions")
    class GetAllFunctionsEndpoint {

        @Test
        @DisplayName("should return all functions")
        void shouldReturnAllFunctions() throws Exception {
            // Given
            Function function1 = createAndSaveFunction("function-1");
            Function function2 = createAndSaveFunction("function-2");

            // When/Then
            mockMvc.perform(get("/api/v1/functions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("function-1", "function-2")));
        }

        @Test
        @DisplayName("should return empty list when no functions exist")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/api/v1/functions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/functions/{id}")
    class GetFunctionByIdEndpoint {

        @Test
        @DisplayName("should return function by id")
        void shouldReturnFunctionById() throws Exception {
            // Given
            Function function = createAndSaveFunction("test-function");

            // When/Then
            mockMvc.perform(get("/api/v1/functions/{id}", function.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(function.getId().toString()))
                    .andExpect(jsonPath("$.name").value("test-function"));
        }

        @Test
        @DisplayName("should return 404 for unknown id")
        void shouldReturn404ForUnknownId() throws Exception {
            mockMvc.perform(get("/api/v1/functions/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/functions/name/{name}")
    class GetFunctionByNameEndpoint {

        @Test
        @DisplayName("should return function by name")
        void shouldReturnFunctionByName() throws Exception {
            // Given
            createAndSaveFunction("my-function");

            // When/Then
            mockMvc.perform(get("/api/v1/functions/name/{name}", "my-function"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("my-function"));
        }

        @Test
        @DisplayName("should return 404 for unknown name")
        void shouldReturn404ForUnknownName() throws Exception {
            mockMvc.perform(get("/api/v1/functions/name/{name}", "unknown"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/functions/{id}")
    class DeleteFunctionEndpoint {

        @Test
        @DisplayName("should delete function")
        void shouldDeleteFunction() throws Exception {
            // Given
            Function function = createAndSaveFunction("to-delete");

            // When/Then
            mockMvc.perform(delete("/api/v1/functions/{id}", function.getId()))
                    .andExpect(status().isNoContent());

            // Verify deleted
            mockMvc.perform(get("/api/v1/functions/{id}", function.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when deleting unknown function")
        void shouldReturn404WhenDeletingUnknown() throws Exception {
            mockMvc.perform(delete("/api/v1/functions/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    // Helper method to create and save a function
    private Function createAndSaveFunction(String name) {
        Function function = createFunction(name, "java17", "com.example.Handler::handle", FunctionStatus.PENDING);
        return functionRepository.save(function);
    }
}
