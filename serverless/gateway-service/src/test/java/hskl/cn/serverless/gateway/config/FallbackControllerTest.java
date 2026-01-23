package hskl.cn.serverless.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("FallbackController Tests")
class FallbackControllerTest {

    private FallbackController fallbackController;

    @BeforeEach
    void setUp() {
        fallbackController = new FallbackController();
    }

    @Test
    @DisplayName("should return service unavailable for registry fallback")
    void shouldReturnUnavailableForRegistryFallback() {

        Mono<ResponseEntity<Map<String, Object>>> result = fallbackController.registryFallback();


        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getBody()).containsKey("error");
                    assertThat(response.getBody()).containsKey("message");
                    assertThat(response.getBody()).containsKey("timestamp");
                    assertThat(response.getBody().get("message").toString())
                            .contains("Registry service");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("should return service unavailable for executor fallback")
    void shouldReturnUnavailableForExecutorFallback() {

        Mono<ResponseEntity<Map<String, Object>>> result = fallbackController.executorFallback();


        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getBody()).containsKey("error");
                    assertThat(response.getBody()).containsKey("message");
                    assertThat(response.getBody()).containsKey("timestamp");
                    assertThat(response.getBody().get("message").toString())
                            .contains("Executor service");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("should include timestamp in fallback response")
    void shouldIncludeTimestampInResponse() {

        Mono<ResponseEntity<Map<String, Object>>> result = fallbackController.registryFallback();


        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getBody().get("timestamp")).isNotNull();
                    // Timestamp should be a valid date string
                    String timestamp = response.getBody().get("timestamp").toString();
                    assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("should return error key in response body")
    void shouldReturnErrorKey() {

        Mono<ResponseEntity<Map<String, Object>>> result = fallbackController.executorFallback();


        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getBody().get("error")).isEqualTo("Service Unavailable");
                })
                .verifyComplete();
    }
}
