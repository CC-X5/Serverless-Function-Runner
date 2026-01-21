package hskl.cn.serverless.registry.controller;

import hskl.cn.serverless.registry.dto.CreateFunctionRequest;
import hskl.cn.serverless.registry.dto.FunctionResponse;
import hskl.cn.serverless.registry.service.FunctionService;
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
public class FunctionController {

    private final FunctionService functionService;

    @PostMapping
    public ResponseEntity<FunctionResponse> createFunction(@Valid @RequestBody CreateFunctionRequest request) {
        log.info("POST /api/v1/functions - Creating function: {}", request.getName());
        FunctionResponse response = functionService.createFunction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FunctionResponse>> getAllFunctions() {
        log.info("GET /api/v1/functions - Listing all functions");
        return ResponseEntity.ok(functionService.getAllFunctions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FunctionResponse> getFunction(@PathVariable("id") UUID id) {
        log.info("GET /api/v1/functions/{} - Getting function", id);
        return ResponseEntity.ok(functionService.getFunction(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<FunctionResponse> getFunctionByName(@PathVariable("name") String name) {
        log.info("GET /api/v1/functions/name/{} - Getting function by name", name);
        return ResponseEntity.ok(functionService.getFunctionByName(name));
    }

    @PostMapping(value = "/{id}/jar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FunctionResponse> uploadJar(@PathVariable("id") UUID id, @RequestParam("file") MultipartFile file) {
        log.info("POST /api/v1/functions/{}/jar - Uploading JAR: {}", id, file.getOriginalFilename());
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (!file.getOriginalFilename().endsWith(".jar")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(functionService.uploadJar(id, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFunction(@PathVariable("id") UUID id) {
        log.info("DELETE /api/v1/functions/{} - Deleting function", id);
        functionService.deleteFunction(id);
        return ResponseEntity.noContent().build();
    }
}