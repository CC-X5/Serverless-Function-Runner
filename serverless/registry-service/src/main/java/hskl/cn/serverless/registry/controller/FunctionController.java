package hskl.cn.serverless.registry.controller;

import hskl.cn.serverless.registry.dto.CreateFunctionRequest;
import hskl.cn.serverless.registry.dto.FunctionResponse;
import hskl.cn.serverless.registry.service.FunctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/functions")
@RequiredArgsConstructor
@Tag(name = "Function Management", description = "API für die Verwaltung von Serverless Functions")
public class FunctionController {

    private final FunctionService functionService;

    @Operation(summary = "Neue Function erstellen", 
               description = "Registriert eine neue Serverless Function im System. Nach dem Erstellen ist die Function im Status PENDING bis eine JAR-Datei hochgeladen wird.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Function erfolgreich erstellt",
                    content = @Content(schema = @Schema(implementation = FunctionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten", 
                    content = @Content(examples = @ExampleObject(value = "{\"handler\": \"Handler must be in format 'package.ClassName::methodName'\"}"))),
            @ApiResponse(responseCode = "409", description = "Function mit diesem Namen existiert bereits",
                    content = @Content(examples = @ExampleObject(value = "{\"message\": \"Function already exists with name: hello\"}")))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Function-Konfiguration",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = CreateFunctionRequest.class),
                    examples = {
                            @ExampleObject(name = "Hello Function", 
                                    summary = "Einfache Begrüßungsfunktion",
                                    value = """
                                            {
                                              "name": "hello",
                                              "runtime": "java17",
                                              "handler": "hskl.cn.serverless.function.HelloFunction::handle",
                                              "memoryMb": 256,
                                              "timeoutSeconds": 30,
                                              "description": "Gibt eine Begrüßung zurück"
                                            }"""),
                            @ExampleObject(name = "Reverse Function",
                                    summary = "String-Umkehr-Funktion", 
                                    value = """
                                            {
                                              "name": "reverse",
                                              "runtime": "java17",
                                              "handler": "hskl.cn.serverless.function.ReverseFunction::handle",
                                              "memoryMb": 256,
                                              "timeoutSeconds": 30
                                            }"""),
                            @ExampleObject(name = "Sum Function",
                                    summary = "Summierungsfunktion",
                                    value = """
                                            {
                                              "name": "sum",
                                              "runtime": "java17",
                                              "handler": "hskl.cn.serverless.function.SumFunction::handle",
                                              "memoryMb": 256,
                                              "timeoutSeconds": 30
                                            }""")
                    }
            )
    )
    @PostMapping
    public ResponseEntity<FunctionResponse> createFunction(@Valid @RequestBody CreateFunctionRequest request) {
        log.info("POST /api/v1/functions - Creating function: {}", request.getName());
        FunctionResponse response = functionService.createFunction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Alle Functions auflisten", 
               description = "Gibt eine Liste aller registrierten Functions zurück, sortiert nach Erstellungsdatum")
    @ApiResponse(responseCode = "200", description = "Liste der Functions",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FunctionResponse.class))))
    @GetMapping
    public ResponseEntity<List<FunctionResponse>> getAllFunctions() {
        log.info("GET /api/v1/functions - Listing all functions");
        return ResponseEntity.ok(functionService.getAllFunctions());
    }

    @Operation(summary = "Function nach ID abrufen", 
               description = "Gibt eine Function anhand ihrer UUID zurück")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Function gefunden",
                    content = @Content(schema = @Schema(implementation = FunctionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Function nicht gefunden",
                    content = @Content(examples = @ExampleObject(value = "{\"message\": \"Function not found\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<FunctionResponse> getFunction(
            @Parameter(description = "UUID der Function", example = "550e8400-e29b-41d4-a716-446655440000") 
            @PathVariable("id") UUID id) {
        log.info("GET /api/v1/functions/{} - Getting function", id);
        return ResponseEntity.ok(functionService.getFunction(id));
    }

    @Operation(summary = "Function nach Name abrufen", 
               description = "Gibt eine Function anhand ihres Namens zurück. Dies ist die bevorzugte Methode.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Function gefunden",
                    content = @Content(schema = @Schema(implementation = FunctionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Function nicht gefunden",
                    content = @Content(examples = @ExampleObject(value = "{\"message\": \"Function not found: hello\"}")))
    })
    @GetMapping("/name/{name}")
    public ResponseEntity<FunctionResponse> getFunctionByName(
            @Parameter(description = "Name der Function", example = "hello") 
            @PathVariable("name") String name) {
        log.info("GET /api/v1/functions/name/{} - Getting function by name", name);
        return ResponseEntity.ok(functionService.getFunctionByName(name));
    }

    @Operation(summary = "JAR-Datei hochladen (per ID)", 
               description = "Lädt eine JAR-Datei für eine Function hoch. Nach dem Upload wechselt der Status zu READY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "JAR erfolgreich hochgeladen",
                    content = @Content(schema = @Schema(implementation = FunctionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ungültige Datei (leer oder keine .jar)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Function nicht gefunden", content = @Content)
    })
    @PostMapping(value = "/{id}/jar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FunctionResponse> uploadJar(
            @Parameter(description = "UUID der Function", example = "550e8400-e29b-41d4-a716-446655440000") 
            @PathVariable("id") UUID id,
            @Parameter(description = "JAR-Datei (max. 50MB)") 
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/v1/functions/{}/jar - Uploading JAR: {}", id, file.getOriginalFilename());
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (!file.getOriginalFilename().endsWith(".jar")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(functionService.uploadJar(id, file));
    }

    @Operation(summary = "JAR-Datei hochladen (per Name)", 
               description = "Lädt eine JAR-Datei für eine Function über ihren Namen hoch. Dies ist die bevorzugte Methode.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "JAR erfolgreich hochgeladen",
                    content = @Content(schema = @Schema(implementation = FunctionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ungültige Datei (leer oder keine .jar)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Function nicht gefunden", content = @Content)
    })
    @PostMapping(value = "/name/{name}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FunctionResponse> uploadJarByName(
            @Parameter(description = "Name der Function", example = "hello") 
            @PathVariable("name") String name,
            @Parameter(description = "JAR-Datei (max. 50MB)") 
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/v1/functions/name/{}/upload - Uploading JAR: {}", name, file.getOriginalFilename());
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (!file.getOriginalFilename().endsWith(".jar")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(functionService.uploadJarByName(name, file));
    }

    @Operation(summary = "Function aktualisieren", 
               description = "Aktualisiert eine bestehende Function (z.B. Memory, Timeout)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Function aktualisiert",
                    content = @Content(schema = @Schema(implementation = FunctionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Function nicht gefunden", content = @Content)
    })
    @PutMapping("/name/{name}")
    public ResponseEntity<FunctionResponse> updateFunctionByName(
            @Parameter(description = "Name der Function", example = "hello") 
            @PathVariable("name") String name,
            @RequestBody CreateFunctionRequest request) {
        log.info("PUT /api/v1/functions/name/{} - Updating function", name);
        return ResponseEntity.ok(functionService.updateFunctionByName(name, request));
    }

    @Operation(summary = "Function löschen (per Name)", 
               description = "Löscht eine Function und ihre JAR-Datei aus MinIO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Function gelöscht"),
            @ApiResponse(responseCode = "404", description = "Function nicht gefunden", content = @Content)
    })
    @DeleteMapping("/name/{name}")
    public ResponseEntity<Void> deleteFunctionByName(
            @Parameter(description = "Name der Function", example = "hello") 
            @PathVariable("name") String name) {
        log.info("DELETE /api/v1/functions/name/{} - Deleting function", name);
        functionService.deleteFunctionByName(name);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Function löschen (per ID)", 
               description = "Löscht eine Function und ihre JAR-Datei aus MinIO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Function gelöscht"),
            @ApiResponse(responseCode = "404", description = "Function nicht gefunden", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFunction(
            @Parameter(description = "UUID der Function", example = "550e8400-e29b-41d4-a716-446655440000") 
            @PathVariable("id") UUID id) {
        log.info("DELETE /api/v1/functions/{} - Deleting function", id);
        functionService.deleteFunction(id);
        return ResponseEntity.noContent().build();
    }
}