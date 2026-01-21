package hskl.cn.serverless.registry.dto;

import hskl.cn.serverless.registry.model.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionResponse {

    private UUID id;
    private String name;
    private String description;
    private String runtime;
    private String handler;
    private String status;
    private Integer timeoutSeconds;
    private Integer memoryMb;
    private String jarPath;
    private Long jarSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FunctionResponse from(Function function) {
        return FunctionResponse.builder()
                .id(function.getId())
                .name(function.getName())
                .description(function.getDescription())
                .runtime(function.getRuntime())
                .handler(function.getHandler())
                .status(function.getStatus().name())
                .timeoutSeconds(function.getTimeoutSeconds())
                .jarPath(function.getJarPath())
                .memoryMb(function.getMemoryMb())
                .jarSize(function.getJarSize())
                .createdAt(function.getCreatedAt())
                .updatedAt(function.getUpdatedAt())
                .build();
    }
}
