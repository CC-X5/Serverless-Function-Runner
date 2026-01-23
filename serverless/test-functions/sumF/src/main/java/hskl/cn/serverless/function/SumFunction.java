package hskl.cn.serverless.function;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class SumFunction {

    private static final Gson gson = new Gson();


    public Integer handle(String input) {
        JsonObject json = gson.fromJson(input, JsonObject.class);
        
        if (json.has("numbers") && json.get("numbers").isJsonArray()) {
            JsonArray numbers = json.getAsJsonArray("numbers");
            int sum = 0;
            for (int i = 0; i < numbers.size(); i++) {
                sum += numbers.get(i).getAsInt();
            }
            return sum;
        }

        int a = json.has("a") ? json.get("a").getAsInt() : 0;
        int b = json.has("b") ? json.get("b").getAsInt() : 0;
        return a + b;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("0");
            return;
        }
        SumFunction function = new SumFunction();
        Integer result = function.handle(args[0]);
        System.out.println(result);
    }
}
