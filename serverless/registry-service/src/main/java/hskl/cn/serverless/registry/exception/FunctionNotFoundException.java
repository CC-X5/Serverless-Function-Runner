package hskl.cn.serverless.registry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FunctionNotFoundException extends RuntimeException {

    public FunctionNotFoundException(String message) {
        super(message);
    }

    public static FunctionNotFoundException byId(UUID id) {
        return new FunctionNotFoundException("Function not found with id: " + id);
    }

    public static FunctionNotFoundException byName(String name) {
        return new FunctionNotFoundException("Function not found with name: " + name);
    }
}
