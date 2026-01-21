package hskl.cn.serverless.registry.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFunctionRequest {

    @NotBlank(message = "Function name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Name must contain only lowercase letters, numbers, and hyphens")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Runtime is required")
    @Builder.Default
    private String runtime = "java17";

    @NotBlank(message = "Handler is required")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.]*::[a-zA-Z][a-zA-Z0-9_]*$",
            message = "Handler must be in format 'package.ClassName::methodName'")
    private String handler;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    @Min(value = 128, message = "Memory must be at least 128 MB")
    @Builder.Default
    private Integer memoryMb = 256;
}
