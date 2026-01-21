package hskl.cn.serverless.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LoggingFilter.
 * Tests request logging functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingFilter Tests")
class LoggingFilterTest {

    private LoggingFilter loggingFilter;

    @Mock
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        loggingFilter = new LoggingFilter();
    }

    @Test
    @DisplayName("should have highest precedence order")
    void shouldHaveHighestPrecedence() {
        assertThat(loggingFilter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    @DisplayName("should log incoming GET request")
    void shouldLogIncomingGetRequest() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/functions")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = loggingFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        // Verify start time was set
        assertThat(exchange.getAttributes().get("startTime")).isNotNull();
    }

    @Test
    @DisplayName("should log incoming POST request")
    void shouldLogIncomingPostRequest() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/execute/test")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = loggingFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("should track request duration")
    void shouldTrackRequestDuration() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/functions")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        // Simulate some delay
        when(filterChain.filter(any(ServerWebExchange.class)))
                .thenReturn(Mono.delay(java.time.Duration.ofMillis(50)).then());

        // When
        Mono<Void> result = loggingFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        // Start time should have been recorded
        Long startTime = exchange.getAttribute("startTime");
        assertThat(startTime).isNotNull();
    }

    @Test
    @DisplayName("should handle different HTTP methods")
    void shouldHandleDifferentMethods() {
        // Test PUT
        MockServerHttpRequest putRequest = MockServerHttpRequest
                .put("/api/v1/functions/123")
                .build();
        ServerWebExchange putExchange = MockServerWebExchange.from(putRequest);
        
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        
        StepVerifier.create(loggingFilter.filter(putExchange, filterChain))
                .verifyComplete();

        // Test DELETE
        MockServerHttpRequest deleteRequest = MockServerHttpRequest
                .delete("/api/v1/functions/123")
                .build();
        ServerWebExchange deleteExchange = MockServerWebExchange.from(deleteRequest);
        
        StepVerifier.create(loggingFilter.filter(deleteExchange, filterChain))
                .verifyComplete();
    }
}
