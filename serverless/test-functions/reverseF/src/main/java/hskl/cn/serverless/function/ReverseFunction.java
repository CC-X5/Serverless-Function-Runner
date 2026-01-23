package hskl.cn.serverless.function;

import java.util.Map;

/**
 * Reverses a given string.
 */
public class ReverseFunction {

    /**
     * Handles the function invocation.
     * @param input Map containing "text" parameter
     * @return Reversed string
     */
    public String handle(Map<String, Object> input) {
        String text = input.getOrDefault("text", "").toString();
        return new StringBuilder(text).reverse().toString();
    }
}
