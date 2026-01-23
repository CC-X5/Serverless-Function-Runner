package hskl.cn.serverless.function;

import java.util.Map;

/**
 * Simple greeting function that returns a personalized hello message.
 */
public class HelloFunction {

    /**
     * Handles the function invocation.
     * @param input Map containing optional "name" parameter
     * @return Greeting message
     */
    public String handle(Map<String, Object> input) {
        String name = input.getOrDefault("name", "World").toString();
        return "Hello, " + name + "!";
    }
}
