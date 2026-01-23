package hskl.cn.serverless.executor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request zur Ausführung einer Serverless Function")
public class ExecutionRequest {

    @Schema(description = "Name der auszuführenden Function", 
            example = "hello",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String functionName;
    
    @Schema(description = "Payload/Input für die Function als JSON-Objekt", 
            example = "{\"name\": \"Peter\"}")
    private Map<String, Object> payload;
    
    @Builder.Default
    @Schema(description = "Asynchrone Ausführung (aktuell nicht unterstützt)", 
            example = "false",
            defaultValue = "false")
    private boolean async = false;
}
