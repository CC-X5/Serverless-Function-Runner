package hskl.cn.serverless.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/registry")
    public Mono<ResponseEntity<Map<String, Object>>> registryFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "message", "Registry service is currently unavailable",
                        "timestamp", LocalDateTime.now().toString()
                )));
    }

    @GetMapping("/executor")
    public Mono<ResponseEntity<Map<String, Object>>> executorFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "message", "Executor service is currently unavailable",
                        "timestamp", LocalDateTime.now().toString()
                )));
    }
}
