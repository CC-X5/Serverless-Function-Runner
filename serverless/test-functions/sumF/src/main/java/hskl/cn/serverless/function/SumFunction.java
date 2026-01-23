package hskl.cn.serverless.function;

import java.util.Map;

/**
 * Adds two numbers and returns the result.
 */
public class SumFunction {

    /**
     * Handles the function invocation.
     * @param input Map containing "a" and "b" parameters
     * @return Sum of a and b
     */
    public Integer handle(Map<String, Object> input) {
        int a = Integer.parseInt(input.getOrDefault("a", 0).toString());
        int b = Integer.parseInt(input.getOrDefault("b", 0).toString());
        return a + b;
    }
}
