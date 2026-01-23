package hskl.cn.serverless.function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


public class ReverseFunction {

    private static final Gson gson = new Gson();


    public String handle(String input) {
        JsonObject json = gson.fromJson(input, JsonObject.class);
        String text = json.has("text") ? json.get("text").getAsString() : "";
        return new StringBuilder(text).reverse().toString();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("");
            return;
        }
        ReverseFunction function = new ReverseFunction();
        String result = function.handle(args[0]);
        System.out.println(result);
    }
}
