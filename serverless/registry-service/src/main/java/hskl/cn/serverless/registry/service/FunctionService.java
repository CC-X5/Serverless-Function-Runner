package hskl.cn.serverless.registry.service;

import hskl.cn.serverless.registry.dto.CreateFunctionRequest;
import hskl.cn.serverless.registry.dto.FunctionResponse;
import hskl.cn.serverless.registry.exception.FunctionAlreadyExistsException;
import hskl.cn.serverless.registry.exception.FunctionNotFoundException;
import hskl.cn.serverless.registry.model.Function;
import hskl.cn.serverless.registry.model.Function.FunctionStatus;
import hskl.cn.serverless.registry.repository.FunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionService {

    private final FunctionRepository functionRepository;
    private final StorageService storageService;

    @Transactional
    public FunctionResponse createFunction(CreateFunctionRequest request) {
        log.info("Creating function: {}", request.getName());
        if (functionRepository.existsByName(request.getName())) {
            throw new FunctionAlreadyExistsException(request.getName());
        }
        Function function = Function.builder()
                .name(request.getName())
                .description(request.getDescription())
                .runtime(request.getRuntime())
                .handler(request.getHandler())
                .timeoutSeconds(request.getTimeoutSeconds())
                .memoryMb(request.getMemoryMb())
                .status(FunctionStatus.PENDING)
                .build();
        function = functionRepository.save(function);
        log.info("Created function with id: {}", function.getId());
        return FunctionResponse.from(function);
    }

    @Transactional(readOnly = true)
    public List<FunctionResponse> getAllFunctions() {
        return functionRepository.findAll().stream()
                .map(FunctionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FunctionResponse getFunction(UUID id) {
        return functionRepository.findById(id)
                .map(FunctionResponse::from)
                .orElseThrow(() -> FunctionNotFoundException.byId(id));
    }

    @Transactional(readOnly = true)
    public FunctionResponse getFunctionByName(String name) {
        return functionRepository.findByName(name)
                .map(FunctionResponse::from)
                .orElseThrow(() -> FunctionNotFoundException.byName(name));
    }

    @Transactional
    public FunctionResponse uploadJar(UUID id, MultipartFile file) {
        log.info("Uploading JAR for function: {}", id);
        Function function = functionRepository.findById(id)
                .orElseThrow(() -> FunctionNotFoundException.byId(id));
        String jarPath = storageService.uploadJar(function.getName(), file);
        function.setJarPath(jarPath);
        function.setJarSize(file.getSize());
        function.setStatus(FunctionStatus.READY);
        function = functionRepository.save(function);
        log.info("Function {} is now READY", function.getName());
        return FunctionResponse.from(function);
    }

    @Transactional
    public void deleteFunction(UUID id) {
        log.info("Deleting function: {}", id);
        Function function = functionRepository.findById(id)
                .orElseThrow(() -> FunctionNotFoundException.byId(id));
        if (function.getJarPath() != null) {
            try {
                storageService.deleteJar(function.getJarPath());
            } catch (Exception e) {
                log.warn("Failed to delete JAR for function {}: {}", id, e.getMessage());
            }
        }
        functionRepository.delete(function);
        log.info("Deleted function: {}", function.getName());
    }

    @Transactional
    public FunctionResponse updateStatus(UUID id, FunctionStatus status) {
        Function function = functionRepository.findById(id)
                .orElseThrow(() -> FunctionNotFoundException.byId(id));
        function.setStatus(status);
        function = functionRepository.save(function);
        log.info("Updated function {} status to {}", function.getName(), status);
        return FunctionResponse.from(function);
    }

    /**
     * Upload JAR by function name.
     */
    @Transactional
    public FunctionResponse uploadJarByName(String name, MultipartFile file) {
        log.info("Uploading JAR for function: {}", name);
        Function function = functionRepository.findByName(name)
                .orElseThrow(() -> FunctionNotFoundException.byName(name));
        String jarPath = storageService.uploadJar(function.getName(), file);
        function.setJarPath(jarPath);
        function.setJarSize(file.getSize());
        function.setStatus(FunctionStatus.READY);
        function = functionRepository.save(function);
        log.info("Function {} is now READY", function.getName());
        return FunctionResponse.from(function);
    }

    /**
     * Update function by name.
     */
    @Transactional
    public FunctionResponse updateFunctionByName(String name, CreateFunctionRequest request) {
        log.info("Updating function: {}", name);
        Function function = functionRepository.findByName(name)
                .orElseThrow(() -> FunctionNotFoundException.byName(name));
        
        if (request.getDescription() != null) {
            function.setDescription(request.getDescription());
        }
        if (request.getHandler() != null) {
            function.setHandler(request.getHandler());
        }
        if (request.getTimeoutSeconds() != null) {
            function.setTimeoutSeconds(request.getTimeoutSeconds());
        }
        if (request.getMemoryMb() != null) {
            function.setMemoryMb(request.getMemoryMb());
        }
        
        function = functionRepository.save(function);
        log.info("Updated function: {}", function.getName());
        return FunctionResponse.from(function);
    }

    /**
     * Delete function by name.
     */
    @Transactional
    public void deleteFunctionByName(String name) {
        log.info("Deleting function by name: {}", name);
        Function function = functionRepository.findByName(name)
                .orElseThrow(() -> FunctionNotFoundException.byName(name));
        if (function.getJarPath() != null) {
            try {
                storageService.deleteJar(function.getJarPath());
            } catch (Exception e) {
                log.warn("Failed to delete JAR for function {}: {}", name, e.getMessage());
            }
        }
        functionRepository.delete(function);
        log.info("Deleted function: {}", name);
    }
}
