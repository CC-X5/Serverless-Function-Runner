package hskl.cn.serverless.executor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionInfo {

    private UUID id;
    private String name;
    private String runtime;
    private String handler;
    private String jarPath;
    private String status;
    private Integer timeoutSeconds;
    private Integer memoryMb;
}
