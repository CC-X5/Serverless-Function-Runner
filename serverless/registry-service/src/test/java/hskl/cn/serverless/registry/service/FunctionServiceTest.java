package hskl.cn.serverless.registry.service;

import hskl.cn.serverless.registry.dto.CreateFunctionRequest;
import hskl.cn.serverless.registry.dto.FunctionResponse;
import hskl.cn.serverless.registry.exception.FunctionAlreadyExistsException;
import hskl.cn.serverless.registry.exception.FunctionNotFoundException;
import hskl.cn.serverless.registry.model.Function;
import hskl.cn.serverless.registry.model.Function.FunctionStatus;
import hskl.cn.serverless.registry.repository.FunctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FunctionService.
 * Tests all CRUD operations and business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FunctionService Tests")
class FunctionServiceTest {

    @Mock
    private FunctionRepository functionRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FunctionService functionService;

    private Function testFunction;
    private CreateFunctionRequest createRequest;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testFunction = createTestFunction(testId, "test-function", FunctionStatus.PENDING);

        createRequest = CreateFunctionRequest.builder()
                .name("test-function")
                .description("Test description")
                .runtime("java17")
                .handler("com.example.Handler::handle")
                .timeoutSeconds(30)
                .memoryMb(256)
                .build();
    }

    // Helper method to create Function without using Lombok builder in tests
    private Function createTestFunction(UUID id, String name, FunctionStatus status) {
        Function f = new Function();
        f.setId(id);
        f.setName(name);
        f.setDescription("Test description");
        f.setRuntime("java17");
        f.setHandler("com.example.Handler::handle");
        f.setStatus(status);
        f.setTimeoutSeconds(30);
        f.setMemoryMb(256);
        return f;
    }

    @Nested
    @DisplayName("createFunction")
    class CreateFunctionTests {

        @Test
        @DisplayName("should create function successfully")
        void shouldCreateFunctionSuccessfully() {
            // Given
            when(functionRepository.existsByName(anyString())).thenReturn(false);
            when(functionRepository.save(any(Function.class))).thenReturn(testFunction);

            // When
            FunctionResponse response = functionService.createFunction(createRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("test-function");
            assertThat(response.getRuntime()).isEqualTo("java17");
            assertThat(response.getStatus()).isEqualTo("PENDING");
            verify(functionRepository).existsByName("test-function");
            verify(functionRepository).save(any(Function.class));
        }

        @Test
        @DisplayName("should throw exception when function already exists")
        void shouldThrowExceptionWhenFunctionExists() {
            // Given
            when(functionRepository.existsByName("test-function")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> functionService.createFunction(createRequest))
                    .isInstanceOf(FunctionAlreadyExistsException.class)
                    .hasMessageContaining("test-function");

            verify(functionRepository).existsByName("test-function");
            verify(functionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getFunction")
    class GetFunctionTests {

        @Test
        @DisplayName("should return function by id")
        void shouldReturnFunctionById() {
            // Given
            when(functionRepository.findById(testId)).thenReturn(Optional.of(testFunction));

            // When
            FunctionResponse response = functionService.getFunction(testId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testId);
            assertThat(response.getName()).isEqualTo("test-function");
        }

        @Test
        @DisplayName("should throw exception when function not found by id")
        void shouldThrowExceptionWhenNotFoundById() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(functionRepository.findById(unknownId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> functionService.getFunction(unknownId))
                    .isInstanceOf(FunctionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getFunctionByName")
    class GetFunctionByNameTests {

        @Test
        @DisplayName("should return function by name")
        void shouldReturnFunctionByName() {
            // Given
            when(functionRepository.findByName("test-function")).thenReturn(Optional.of(testFunction));

            // When
            FunctionResponse response = functionService.getFunctionByName("test-function");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("test-function");
        }

        @Test
        @DisplayName("should throw exception when function not found by name")
        void shouldThrowExceptionWhenNotFoundByName() {
            // Given
            when(functionRepository.findByName("unknown")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> functionService.getFunctionByName("unknown"))
                    .isInstanceOf(FunctionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllFunctions")
    class GetAllFunctionsTests {

        @Test
        @DisplayName("should return all functions")
        void shouldReturnAllFunctions() {
            // Given
            Function function2 = createTestFunction(UUID.randomUUID(), "function-2", FunctionStatus.READY);
            when(functionRepository.findAll()).thenReturn(List.of(testFunction, function2));

            // When
            List<FunctionResponse> responses = functionService.getAllFunctions();

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(FunctionResponse::getName)
                    .containsExactly("test-function", "function-2");
        }

        @Test
        @DisplayName("should return empty list when no functions exist")
        void shouldReturnEmptyList() {
            // Given
            when(functionRepository.findAll()).thenReturn(List.of());

            // When
            List<FunctionResponse> responses = functionService.getAllFunctions();

            // Then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("uploadJar")
    class UploadJarTests {

        @Mock
        private MultipartFile mockFile;

        @Test
        @DisplayName("should upload JAR and update function status")
        void shouldUploadJarSuccessfully() {
            // Given
            when(functionRepository.findById(testId)).thenReturn(Optional.of(testFunction));
            when(mockFile.getSize()).thenReturn(1024L);
            when(storageService.uploadJar(eq("test-function"), any(MultipartFile.class)))
                    .thenReturn("test-function/test.jar");
            when(functionRepository.save(any(Function.class))).thenAnswer(i -> i.getArgument(0));

            // When
            FunctionResponse response = functionService.uploadJar(testId, mockFile);

            // Then
            assertThat(response.getStatus()).isEqualTo("READY");
            assertThat(response.getJarPath()).isEqualTo("test-function/test.jar");
            assertThat(response.getJarSize()).isEqualTo(1024L);
            verify(storageService).uploadJar(eq("test-function"), any(MultipartFile.class));
        }

        @Test
        @DisplayName("should throw exception when function not found for upload")
        void shouldThrowExceptionWhenFunctionNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(functionRepository.findById(unknownId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> functionService.uploadJar(unknownId, mockFile))
                    .isInstanceOf(FunctionNotFoundException.class);
            verify(storageService, never()).uploadJar(anyString(), any());
        }
    }

    @Nested
    @DisplayName("deleteFunction")
    class DeleteFunctionTests {

        @Test
        @DisplayName("should delete function and its JAR")
        void shouldDeleteFunctionAndJar() {
            // Given
            testFunction.setJarPath("test-function/test.jar");
            when(functionRepository.findById(testId)).thenReturn(Optional.of(testFunction));
            doNothing().when(storageService).deleteJar("test-function/test.jar");
            doNothing().when(functionRepository).delete(testFunction);

            // When
            functionService.deleteFunction(testId);

            // Then
            verify(storageService).deleteJar("test-function/test.jar");
            verify(functionRepository).delete(testFunction);
        }

        @Test
        @DisplayName("should delete function without JAR")
        void shouldDeleteFunctionWithoutJar() {
            // Given
            testFunction.setJarPath(null);
            when(functionRepository.findById(testId)).thenReturn(Optional.of(testFunction));
            doNothing().when(functionRepository).delete(testFunction);

            // When
            functionService.deleteFunction(testId);

            // Then
            verify(storageService, never()).deleteJar(anyString());
            verify(functionRepository).delete(testFunction);
        }

        @Test
        @DisplayName("should throw exception when function not found for deletion")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(functionRepository.findById(unknownId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> functionService.deleteFunction(unknownId))
                    .isInstanceOf(FunctionNotFoundException.class);
            verify(functionRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("should update function status")
        void shouldUpdateStatus() {
            // Given
            when(functionRepository.findById(testId)).thenReturn(Optional.of(testFunction));
            when(functionRepository.save(any(Function.class))).thenAnswer(i -> i.getArgument(0));

            // When
            FunctionResponse response = functionService.updateStatus(testId, FunctionStatus.READY);

            // Then
            assertThat(response.getStatus()).isEqualTo("READY");
        }
    }
}
