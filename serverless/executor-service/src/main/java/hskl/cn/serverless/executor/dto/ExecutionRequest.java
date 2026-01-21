package hskl.cn.serverless.executor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {

    private String functionName;
    private Map<String, Object> payload;
    @Builder.Default
    private boolean async = false;
}
