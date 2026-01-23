package hskl.cn.serverless.registry.dto;

import hskl.cn.serverless.registry.model.Function;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response mit Function-Details")
public class FunctionResponse {

    @Schema(description = "Eindeutige ID der Function", 
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "Name der Function", 
            example = "hello")
    private String name;
    
    @Schema(description = "Beschreibung der Function", 
            example = "Gibt eine Begrüßung zurück")
    private String description;
    
    @Schema(description = "Runtime-Umgebung", 
            example = "java17")
    private String runtime;
    
    @Schema(description = "Handler-Referenz", 
            example = "hskl.cn.serverless.function.HelloFunction::handle")
    private String handler;
    
    @Schema(description = "Aktueller Status der Function", 
            example = "READY",
            allowableValues = {"PENDING", "READY", "RUNNING", "FAILED"})
    private String status;
    
    @Schema(description = "Timeout in Sekunden", 
            example = "30")
    private Integer timeoutSeconds;
    
    @Schema(description = "Memory-Limit in MB", 
            example = "256")
    private Integer memoryMb;
    
    @Schema(description = "Pfad zur JAR-Datei in MinIO", 
            example = "functions/hello/hello-function.jar")
    private String jarPath;
    
    @Schema(description = "Größe der JAR-Datei in Bytes", 
            example = "280576")
    private Long jarSize;
    
    @Schema(description = "Erstellungszeitpunkt", 
            example = "2026-01-23T21:48:02")
    private LocalDateTime createdAt;
    
    @Schema(description = "Letzter Aktualisierungszeitpunkt", 
            example = "2026-01-23T21:48:15")
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
