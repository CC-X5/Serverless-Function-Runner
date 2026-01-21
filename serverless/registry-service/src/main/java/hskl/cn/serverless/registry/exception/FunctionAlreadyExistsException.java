package hskl.cn.serverless.registry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class FunctionAlreadyExistsException extends RuntimeException {

    public FunctionAlreadyExistsException(String name) {
        super("Function already exists with name: " + name);
    }
}
