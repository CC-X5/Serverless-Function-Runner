# Test Functions

Separate Test-Functions für den Serverless Function Runner.

## Struktur

```
test-functions/
├── pom.xml              # Parent POM
├── build.sh             # Build-Script
├── test-local.sh        # Lokales Test-Script
├── jars/                # Output-Verzeichnis für JARs
├── helloF/              # Hello Function
│   ├── pom.xml
│   └── src/main/java/hskl/cn/serverless/function/HelloFunction.java
├── sumF/                # Sum Function
│   ├── pom.xml
│   └── src/main/java/hskl/cn/serverless/function/SumFunction.java
└── reverseF/            # Reverse Function
    ├── pom.xml
    └── src/main/java/hskl/cn/serverless/function/ReverseFunction.java
```

## Build

### Alle Functions bauen
```bash
./build.sh all
# oder einfach
./build.sh
```

### Einzelne Function bauen
```bash
./build.sh hello
./build.sh sum
./build.sh reverse
```

### Mit Maven direkt
```bash
# Alle
mvn clean package

# Einzeln
cd helloF && mvn clean package
```

## Lokales Testen

```bash
# Alle testen
./test-local.sh all

# Einzeln
./test-local.sh hello
./test-local.sh sum
./test-local.sh reverse
```

## Mit dem Serverless Runner testen

Nach dem Build findest du die JARs in `./jars/`:

```bash
# 1. Function registrieren
curl -X POST http://localhost:8082/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name": "hello-test", "runtime": "java17", "handler": "hskl.cn.serverless.function.HelloFunction::handle"}'

# 2. JAR hochladen
curl -X POST http://localhost:8082/api/v1/functions/hello-test/upload \
  -F "file=@jars/hello-function.jar"

# 3. Ausführen
curl -X POST http://localhost:8082/api/v1/functions/hello-test/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "Peter"}'
```

## Functions

### HelloFunction
- **Input:** `{"name": "World"}`
- **Output:** `"Hello, World!"`

### SumFunction
- **Input:** `{"a": 5, "b": 3}`
- **Output:** `8`

### ReverseFunction
- **Input:** `{"text": "CloudNative"}`
- **Output:** `"evitaNduolC"`
