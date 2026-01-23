package hskl.cn.serverless.executor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response nach Ausführung einer Serverless Function")
public class ExecutionResponse {

    @Schema(description = "Eindeutige ID der Ausführung", 
            example = "exec-550e8400-e29b-41d4-a716-446655440000")
    private String executionId;
    
    @Schema(description = "Name der ausgeführten Function", 
            example = "hello")
    private String functionName;
    
    @Schema(description = "Status der Ausführung", 
            example = "SUCCESS")
    private ExecutionStatus status;
    
    @Schema(description = "Ergebnis der Function (bei SUCCESS)", 
            example = "Hello, Peter!")
    private Object result;
    
    @Schema(description = "Fehlermeldung (bei FAILED/TIMEOUT)", 
            example = "null")
    private String error;
    
    @Schema(description = "Ausführungsdauer in Millisekunden", 
            example = "324")
    private Long durationMs;
    
    @Schema(description = "Startzeitpunkt der Ausführung", 
            example = "2026-01-23T21:48:02")
    private LocalDateTime startedAt;
    
    @Schema(description = "Endzeitpunkt der Ausführung", 
            example = "2026-01-23T21:48:03")
    private LocalDateTime completedAt;

    @Schema(description = "Mögliche Status einer Function-Ausführung")
    public enum ExecutionStatus {
        @Schema(description = "Ausführung wartet")
        PENDING,
        @Schema(description = "Ausführung läuft")
        RUNNING,
        @Schema(description = "Ausführung erfolgreich")
        SUCCESS,
        @Schema(description = "Ausführung fehlgeschlagen")
        FAILED,
        @Schema(description = "Ausführung durch Timeout abgebrochen")
        TIMEOUT
    }
}
