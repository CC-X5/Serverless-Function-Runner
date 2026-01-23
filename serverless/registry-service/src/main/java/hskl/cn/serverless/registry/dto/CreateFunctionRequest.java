package hskl.cn.serverless.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request zum Erstellen einer neuen Serverless Function")
public class CreateFunctionRequest {

    @NotBlank(message = "Function name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Name must contain only lowercase letters, numbers, and hyphens")
    @Schema(description = "Eindeutiger Name der Function", 
            example = "hello", 
            requiredMode = Schema.RequiredMode.REQUIRED,
            pattern = "^[a-z0-9-]+$")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Optionale Beschreibung der Function", 
            example = "Gibt eine Begrüßung zurück")
    private String description;

    @NotBlank(message = "Runtime is required")
    @Builder.Default
    @Schema(description = "Runtime-Umgebung für die Function", 
            example = "java17", 
            defaultValue = "java17",
            allowableValues = {"java17", "java21"})
    private String runtime = "java17";

    @NotBlank(message = "Handler is required")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.]*::[a-zA-Z][a-zA-Z0-9_]*$",
            message = "Handler must be in format 'package.ClassName::methodName'")
    @Schema(description = "Handler im Format 'package.ClassName::methodName'", 
            example = "hskl.cn.serverless.function.HelloFunction::handle",
            requiredMode = Schema.RequiredMode.REQUIRED,
            pattern = "^[a-zA-Z][a-zA-Z0-9_.]*::[a-zA-Z][a-zA-Z0-9_]*$")
    private String handler;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Builder.Default
    @Schema(description = "Timeout in Sekunden", 
            example = "30", 
            defaultValue = "30",
            minimum = "1")
    private Integer timeoutSeconds = 30;

    @Min(value = 128, message = "Memory must be at least 128 MB")
    @Builder.Default
    @Schema(description = "Memory-Limit in MB", 
            example = "256", 
            defaultValue = "256",
            minimum = "128")
    private Integer memoryMb = 256;
}
