package hskl.cn.serverless.function;

/**
 * Simple Hello World Function for testing the Serverless Function Runner.
 * 
 * This function reads JSON input from command line args and outputs a greeting.
 * Expected input format: {"name": "World"}
 * Output: Hello, World!
 */
public class HelloFunction {

    public static void main(String[] args) {
        try {
            // Get the input payload from command line argument
            String payload = args.length > 0 ? args[0] : "{}";
            
            // Simple JSON parsing (without external dependencies)
            String name = extractName(payload);
            
            // Output the result (this will be captured by the executor)
            System.out.println("Hello, " + name + "!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static String extractName(String json) {
        // Simple parsing: look for "name":"value" or "name": "value"
        int nameIndex = json.indexOf("\"name\"");
        if (nameIndex == -1) {
            return "World";
        }
        
        int colonIndex = json.indexOf(":", nameIndex);
        if (colonIndex == -1) {
            return "World";
        }
        
        // Find the value after the colon
        int valueStart = json.indexOf("\"", colonIndex);
        if (valueStart == -1) {
            return "World";
        }
        
        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) {
            return "World";
        }
        
        return json.substring(valueStart + 1, valueEnd);
    }
}
