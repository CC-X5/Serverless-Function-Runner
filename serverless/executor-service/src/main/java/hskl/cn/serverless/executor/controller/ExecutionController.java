package hskl.cn.serverless.executor.controller;

import hskl.cn.serverless.executor.dto.ExecutionRequest;
import hskl.cn.serverless.executor.dto.ExecutionResponse;
import hskl.cn.serverless.executor.service.DockerExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
@Tag(name = "Function Execution", description = "API für die Ausführung von Serverless Functions in isolierten Docker-Containern")
public class ExecutionController {

    private final DockerExecutionService executionService;

    @Operation(summary = "Function ausführen (per Name in URL)", 
               description = """
                       Führt eine Function in einem isolierten Docker-Container aus.
                       
                       **Voraussetzungen:**
                       - Function muss existieren
                       - Function muss im Status READY sein (JAR hochgeladen)
                       
                       **Ablauf:**
                       1. JAR wird aus MinIO geladen
                       2. Docker-Container wird erstellt
                       3. JAR wird in Container kopiert
                       4. Function wird ausgeführt
                       5. Ergebnis wird zurückgegeben
                       """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Function erfolgreich ausgeführt",
                    content = @Content(
                            schema = @Schema(implementation = ExecutionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "executionId": "exec-550e8400",
                                      "functionName": "hello",
                                      "status": "SUCCESS",
                                      "result": "Hello, Peter!",
                                      "error": null,
                                      "durationMs": 324,
                                      "startedAt": "2026-01-23T21:48:02",
                                      "completedAt": "2026-01-23T21:48:03"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Function nicht gefunden oder nicht bereit", 
                    content = @Content(examples = @ExampleObject(value = "{\"message\": \"Function not found: hello\"}"))),
            @ApiResponse(responseCode = "500", description = "Fehler bei der Ausführung",
                    content = @Content(examples = @ExampleObject(value = "{\"message\": \"Container execution failed\"}")))
    })
    @PostMapping("/{functionName}")
    public ResponseEntity<ExecutionResponse> execute(
            @Parameter(description = "Name der auszuführenden Function", example = "hello") 
            @PathVariable("functionName") String functionName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payload für die Function",
                    content = @Content(
                            examples = {
                                    @ExampleObject(name = "Hello Payload",
                                            summary = "Payload für Hello-Function",
                                            value = "{\"name\": \"Peter\"}"),
                                    @ExampleObject(name = "Reverse Payload",
                                            summary = "Payload für Reverse-Function",
                                            value = "{\"text\": \"CloudNative\"}"),
                                    @ExampleObject(name = "Sum Payload",
                                            summary = "Payload für Sum-Function",
                                            value = "{\"numbers\": [1, 2, 3, 4, 5]}")
                            }
                    )
            )
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

    @Operation(summary = "Function ausführen (mit Request Body)", 
               description = "Führt eine Function mit vollständigem Request-Objekt aus. Alternative zum URL-basierten Aufruf.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Function erfolgreich ausgeführt",
                    content = @Content(schema = @Schema(implementation = ExecutionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Function nicht gefunden oder nicht bereit", content = @Content),
            @ApiResponse(responseCode = "500", description = "Fehler bei der Ausführung", content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Execution Request mit functionName und payload",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ExecutionRequest.class),
                    examples = {
                            @ExampleObject(name = "Hello Request",
                                    summary = "Hello-Function ausführen",
                                    value = """
                                            {
                                              "functionName": "hello",
                                              "payload": {"name": "Peter"},
                                              "async": false
                                            }"""),
                            @ExampleObject(name = "Reverse Request",
                                    summary = "Reverse-Function ausführen",
                                    value = """
                                            {
                                              "functionName": "reverse",
                                              "payload": {"text": "CloudNative"},
                                              "async": false
                                            }"""),
                            @ExampleObject(name = "Sum Request",
                                    summary = "Sum-Function ausführen",
                                    value = """
                                            {
                                              "functionName": "sum",
                                              "payload": {"numbers": [10, 20, 30, 40, 50]},
                                              "async": false
                                            }""")
                    }
            )
    )
    @PostMapping
    public ResponseEntity<ExecutionResponse> executeWithBody(@RequestBody ExecutionRequest request) {
        log.info("POST /api/v1/execute - Executing function: {}", request.getFunctionName());
        ExecutionResponse response = executionService.execute(request);
        return ResponseEntity.ok(response);
    }
}
