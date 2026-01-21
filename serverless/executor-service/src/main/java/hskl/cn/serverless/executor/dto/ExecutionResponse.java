package hskl.cn.serverless.executor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResponse {

    private String executionId;
    private String functionName;
    private ExecutionStatus status;
    private Object result;
    private String error;
    private Long durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT
    }
}
