@echo off
echo === Functions registrieren ===
echo.

echo [1/3] Hello Function...
call curl -s -X POST http://35.234.75.103:8082/api/v1/functions -H "Content-Type: application/json" -d "{\"name\":\"hello\",\"runtime\":\"java17\",\"handler\":\"hskl.cn.serverless.function.HelloFunction::handle\",\"memoryMb\":256,\"timeoutSeconds\":30,\"description\":\"Begruessung\"}"
echo.

echo [2/3] Reverse Function...
call curl -s -X POST http://35.234.75.103:8082/api/v1/functions -H "Content-Type: application/json" -d "{\"name\":\"reverse\",\"runtime\":\"java17\",\"handler\":\"hskl.cn.serverless.function.ReverseFunction::handle\",\"memoryMb\":256,\"timeoutSeconds\":30,\"description\":\"String umkehren\"}"
echo.

echo [3/3] Sum Function...
call curl -s -X POST http://35.234.75.103:8082/api/v1/functions -H "Content-Type: application/json" -d "{\"name\":\"sum\",\"runtime\":\"java17\",\"handler\":\"hskl.cn.serverless.function.SumFunction::handle\",\"memoryMb\":256,\"timeoutSeconds\":30,\"description\":\"Zahlen summieren\"}"
echo.

echo === JARs hochladen ===
echo.

echo [1/3] Hello JAR...
call curl -s -X POST http://35.234.75.103:8082/api/v1/functions/name/hello/upload -F "file=@jars/hello-function.jar"
echo.

echo [2/3] Reverse JAR...
call curl -s -X POST http://35.234.75.103:8082/api/v1/functions/name/reverse/upload -F "file=@jars/reverse-function.jar"
echo.

echo [3/3] Sum JAR...
call curl -s -X POST http://35.234.75.103:8082/api/v1/functions/name/sum/upload -F "file=@jars/sum-function.jar"
echo.

echo === Status pruefen ===
call curl -s http://35.234.75.103:8082/api/v1/functions
echo.

echo === Functions ausfuehren ===
echo.

echo Hello:
call curl -s -X POST http://35.234.75.103:8082/api/v1/execute -H "Content-Type: application/json" -d "{\"functionName\":\"hello\",\"payload\":{\"name\":\"Peter\"}}"
echo.

echo Reverse:
call curl -s -X POST http://35.234.75.103:8082/api/v1/execute -H "Content-Type: application/json" -d "{\"functionName\":\"reverse\",\"payload\":{\"text\":\"CloudNative\"}}"
echo.

echo Sum:
call curl -s -X POST http://35.234.75.103:8082/api/v1/execute -H "Content-Type: application/json" -d "{\"functionName\":\"sum\",\"payload\":{\"numbers\":[1,2,3,4,5]}}"
echo.

echo === Demo Complete ===
