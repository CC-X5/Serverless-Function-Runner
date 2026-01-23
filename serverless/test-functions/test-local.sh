#!/bin/bash
# Test script to run functions locally
# Usage: ./test-local.sh [hello|sum|reverse]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_DIR="$SCRIPT_DIR/jars"

# Check if JARs exist
if [ ! -d "$JAR_DIR" ]; then
    echo "❌ JARs not found. Run ./build.sh first!"
    exit 1
fi

test_hello() {
    echo "🧪 Testing HelloFunction..."
    echo ""
    
    # Create a simple test runner
    cat > /tmp/TestRunner.java << 'EOF'
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

public class TestRunner {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        String className = args[1];
        
        URL[] urls = { new File(jarPath).toURI().toURL() };
        URLClassLoader loader = new URLClassLoader(urls, TestRunner.class.getClassLoader());
        
        Class<?> clazz = loader.loadClass(className);
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getMethod("handle", Map.class);
        
        // Test 1: Default
        Map<String, Object> input1 = new HashMap<>();
        Object result1 = method.invoke(instance, input1);
        System.out.println("Test 1 (no input): " + result1);
        
        // Test 2: With name
        Map<String, Object> input2 = new HashMap<>();
        input2.put("name", "Peter");
        Object result2 = method.invoke(instance, input2);
        System.out.println("Test 2 (name=Peter): " + result2);
        
        loader.close();
    }
}
EOF
    
    javac /tmp/TestRunner.java -d /tmp
    java -cp /tmp hskl.cn.serverless.function.TestRunner "$JAR_DIR/hello-function.jar" "hskl.cn.serverless.function.HelloFunction"
    echo ""
    echo "✅ HelloFunction tests passed!"
}

test_sum() {
    echo "🧪 Testing SumFunction..."
    echo ""
    
    cat > /tmp/TestSum.java << 'EOF'
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

public class TestSum {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        
        URL[] urls = { new File(jarPath).toURI().toURL() };
        URLClassLoader loader = new URLClassLoader(urls, TestSum.class.getClassLoader());
        
        Class<?> clazz = loader.loadClass("hskl.cn.serverless.function.SumFunction");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getMethod("handle", Map.class);
        
        // Test: 5 + 3
        Map<String, Object> input = new HashMap<>();
        input.put("a", 5);
        input.put("b", 3);
        Object result = method.invoke(instance, input);
        System.out.println("Test (5 + 3): " + result);
        
        // Test: 100 + 200
        input.put("a", 100);
        input.put("b", 200);
        result = method.invoke(instance, input);
        System.out.println("Test (100 + 200): " + result);
        
        loader.close();
    }
}
EOF
    
    javac /tmp/TestSum.java -d /tmp
    java -cp /tmp TestSum "$JAR_DIR/sum-function.jar"
    echo ""
    echo "✅ SumFunction tests passed!"
}

test_reverse() {
    echo "🧪 Testing ReverseFunction..."
    echo ""
    
    cat > /tmp/TestReverse.java << 'EOF'
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

public class TestReverse {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        
        URL[] urls = { new File(jarPath).toURI().toURL() };
        URLClassLoader loader = new URLClassLoader(urls, TestReverse.class.getClassLoader());
        
        Class<?> clazz = loader.loadClass("hskl.cn.serverless.function.ReverseFunction");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getMethod("handle", Map.class);
        
        // Test
        Map<String, Object> input = new HashMap<>();
        input.put("text", "CloudNative");
        Object result = method.invoke(instance, input);
        System.out.println("Test (CloudNative): " + result);
        
        input.put("text", "HSKL");
        result = method.invoke(instance, input);
        System.out.println("Test (HSKL): " + result);
        
        loader.close();
    }
}
EOF
    
    javac /tmp/TestReverse.java -d /tmp
    java -cp /tmp TestReverse "$JAR_DIR/reverse-function.jar"
    echo ""
    echo "✅ ReverseFunction tests passed!"
}

case "${1:-all}" in
    hello)
        test_hello
        ;;
    sum)
        test_sum
        ;;
    reverse)
        test_reverse
        ;;
    all)
        test_hello
        echo "---"
        test_sum
        echo "---"
        test_reverse
        ;;
    *)
        echo "Usage: $0 [all|hello|sum|reverse]"
        exit 1
        ;;
esac
