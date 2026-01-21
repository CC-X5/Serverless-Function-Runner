package hskl.cn.serverless.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        exchange.getAttributes().put(START_TIME, Instant.now().toEpochMilli());
        log.info("Incoming request: {} {}", method, path);
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME);
            if (startTime != null) {
                long duration = Instant.now().toEpochMilli() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value()
                        : 0;
                log.info("Completed request: {} {} - {} ({}ms)", method, path, statusCode, duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
