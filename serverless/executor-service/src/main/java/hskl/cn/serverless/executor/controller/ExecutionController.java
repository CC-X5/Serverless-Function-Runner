package hskl.cn.serverless.executor.controller;

import hskl.cn.serverless.executor.dto.ExecutionRequest;
import hskl.cn.serverless.executor.dto.ExecutionResponse;
import hskl.cn.serverless.executor.service.DockerExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
public class ExecutionController {

    private final DockerExecutionService executionService;

    @PostMapping("/{functionName}")
    public ResponseEntity<ExecutionResponse> execute(
            @PathVariable("functionName") String functionName,
            @RequestBody(required = false) Map<String, Object> payload) {
        log.info("POST /api/v1/execute/{} - Executing function", functionName);
        ExecutionRequest request = ExecutionRequest.builder()
                .functionName(functionName)
                .payload(payload != null ? payload : Map.of())
                .async(false)
                .build();
        ExecutionResponse response = executionService.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ExecutionResponse> executeWithBody(@RequestBody ExecutionRequest request) {
        log.info("POST /api/v1/execute - Executing function: {}", request.getFunctionName());
        ExecutionResponse response = executionService.execute(request);
        return ResponseEntity.ok(response);
    }
}
