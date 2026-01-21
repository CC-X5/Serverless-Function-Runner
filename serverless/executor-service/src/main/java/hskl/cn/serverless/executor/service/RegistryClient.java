package hskl.cn.serverless.executor.service;

import hskl.cn.serverless.executor.dto.FunctionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryClient {

    private final WebClient registryWebClient;

    public Optional<FunctionInfo> getFunction(String functionName) {
        try {
            FunctionInfo function = registryWebClient
                    .get()
                    .uri("/api/v1/functions/name/{name}", functionName)
                    .retrieve()
                    .bodyToMono(FunctionInfo.class)
                    .block();
            return Optional.ofNullable(function);
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Function not found: {}", functionName);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get function from registry: {}", e.getMessage());
            throw new RuntimeException("Failed to get function from registry", e);
        }
    }
}
