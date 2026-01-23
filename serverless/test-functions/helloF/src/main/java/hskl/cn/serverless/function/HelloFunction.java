package hskl.cn.serverless.function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class HelloFunction {

    private static final Gson gson = new Gson();

    public String handle(String input) {
        JsonObject json = gson.fromJson(input, JsonObject.class);
        String name = json.has("name") ? json.get("name").getAsString() : "World";
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Hello, World!");
            return;
        }
        HelloFunction function = new HelloFunction();
        String result = function.handle(args[0]);
        System.out.println(result);
    }
}