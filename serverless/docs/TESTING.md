# 🧪 Testing Guide - Serverless Function Runner

Vollständige Anleitung zum Testen aller API-Endpunkte.

## Inhaltsverzeichnis

- [Voraussetzungen](#voraussetzungen)
- [Services starten](#services-starten)
- [Health Checks](#1-health-checks)
- [Function Management](#2-function-management)
- [JAR Upload](#3-jar-upload)
- [Function Execution](#4-function-execution)
- [Gateway Tests](#5-gateway-tests)
- [Fehlerszenarien](#6-fehlerszenarien)
- [Aufräumen](#7-aufräumen)
- [Troubleshooting](#troubleshooting)

---

## Voraussetzungen

- Docker Desktop läuft
- `curl` installiert
- `jq` installiert (optional, für formatierte Ausgabe)

```bash
# jq installieren (falls nicht vorhanden)
# Windows (Chocolatey): choco install jq
# macOS: brew install jq
# Linux: sudo apt install jq
```

---

## Services starten

```bash
# Ins serverless Verzeichnis wechseln
cd serverless

# Alle Services starten
docker-compose up -d --build

# Warten bis healthy (~30 Sekunden)
docker-compose ps
```

**Erwartete Ausgabe:** Alle 6 Container sollten `running` oder `healthy` sein.

| Service | Port | URL |
|---------|------|-----|
| Registry Service | 8080 | http://localhost:8080 |
| Executor Service | 8081 | http://localhost:8081 |
| Gateway Service | 8082 | http://localhost:8082 |
| MinIO Console | 9001 | http://localhost:9001 |
| RabbitMQ Console | 15672 | http://localhost:15672 |

---

## 1. Health Checks

### Ohne jq

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### Mit jq

```bash
curl -s http://localhost:8080/actuator/health | jq
curl -s http://localhost:8081/actuator/health | jq
curl -s http://localhost:8082/actuator/health | jq
```

---

## 2. Function Management

### 2.1 Alle Functions auflisten

**Ohne jq:**
```bash
curl http://localhost:8080/api/v1/functions
```

**Mit jq:**
```bash
curl -s http://localhost:8080/api/v1/functions | jq
```

### 2.2 Functions erstellen

> ⚠️ **WICHTIG:** Handler-Format muss `package.ClassName::methodName` sein!

**Ohne jq:**
```bash
# Hello Function
curl -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"hello","runtime":"java17","handler":"hskl.cn.serverless.function.HelloFunction::handle","memoryMb":256,"timeoutSeconds":30,"description":"Simple greeting function"}'

# Reverse Function
curl -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"reverse","runtime":"java17","handler":"hskl.cn.serverless.function.ReverseFunction::handle","memoryMb":256,"timeoutSeconds":30,"description":"Reverses a string"}'

# Sum Function
curl -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"sum","runtime":"java17","handler":"hskl.cn.serverless.function.SumFunction::handle","memoryMb":256,"timeoutSeconds":30,"description":"Calculates sum of numbers"}'
```

**Mit jq:**
```bash
curl -s -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"hello","runtime":"java17","handler":"hskl.cn.serverless.function.HelloFunction::handle","memoryMb":256,"timeoutSeconds":30}' | jq

curl -s -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"reverse","runtime":"java17","handler":"hskl.cn.serverless.function.ReverseFunction::handle","memoryMb":256,"timeoutSeconds":30}' | jq

curl -s -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"sum","runtime":"java17","handler":"hskl.cn.serverless.function.SumFunction::handle","memoryMb":256,"timeoutSeconds":30}' | jq
```

### 2.3 Function abrufen (über Namen)

**Ohne jq:**
```bash
curl http://localhost:8080/api/v1/functions/name/hello
curl http://localhost:8080/api/v1/functions/name/reverse
curl http://localhost:8080/api/v1/functions/name/sum
```

**Mit jq:**
```bash
curl -s http://localhost:8080/api/v1/functions/name/hello | jq
curl -s http://localhost:8080/api/v1/functions/name/reverse | jq
curl -s http://localhost:8080/api/v1/functions/name/sum | jq
```

### 2.4 Function aktualisieren

**Ohne jq:**
```bash
curl -X PUT http://localhost:8080/api/v1/functions/name/hello \
  -H "Content-Type: application/json" \
  -d '{"memoryMb":512,"timeoutSeconds":60,"description":"Updated greeting function"}'
```

**Mit jq:**
```bash
curl -s -X PUT http://localhost:8080/api/v1/functions/name/hello \
  -H "Content-Type: application/json" \
  -d '{"memoryMb":512,"timeoutSeconds":60,"description":"Updated greeting function"}' | jq
```

### 2.5 Function löschen

```bash
curl -X DELETE http://localhost:8080/api/v1/functions/name/hello
```

---

## 3. JAR Upload

> ⚠️ **WICHTIG:** Befehle aus dem `serverless/` Verzeichnis ausführen!

### 3.1 JARs hochladen

**Ohne jq:**
```bash
curl -X POST http://localhost:8080/api/v1/functions/name/hello/upload \
  -F "file=@test-functions/jars/hello-function.jar"

curl -X POST http://localhost:8080/api/v1/functions/name/reverse/upload \
  -F "file=@test-functions/jars/reverse-function.jar"

curl -X POST http://localhost:8080/api/v1/functions/name/sum/upload \
  -F "file=@test-functions/jars/sum-function.jar"
```

**Mit jq:**
```bash
curl -s -X POST http://localhost:8080/api/v1/functions/name/hello/upload \
  -F "file=@test-functions/jars/hello-function.jar" | jq

curl -s -X POST http://localhost:8080/api/v1/functions/name/reverse/upload \
  -F "file=@test-functions/jars/reverse-function.jar" | jq

curl -s -X POST http://localhost:8080/api/v1/functions/name/sum/upload \
  -F "file=@test-functions/jars/sum-function.jar" | jq
```

### 3.2 Status prüfen

**Ohne jq:**
```bash
curl http://localhost:8080/api/v1/functions
```

**Mit jq:**
```bash
# Nur Status ausgeben
curl -s http://localhost:8080/api/v1/functions | jq '.[].status'

# Name und Status
curl -s http://localhost:8080/api/v1/functions | jq '.[] | {name: .name, status: .status}'
```

---

## 4. Function Execution

### 4.1 Hello Function

**Ohne jq:**
```bash
curl -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"hello","payload":{"name":"Peter"}}'
```

**Mit jq:**
```bash
curl -s -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"hello","payload":{"name":"Peter"}}' | jq
```

**Erwartete Antwort:** `"result": "Hello, Peter!"`

### 4.2 Reverse Function

**Ohne jq:**
```bash
curl -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"reverse","payload":{"text":"CloudNative"}}'
```

**Mit jq:**
```bash
curl -s -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"reverse","payload":{"text":"CloudNative"}}' | jq
```

**Erwartete Antwort:** `"result": "evitaNduolC"`

### 4.3 Sum Function

**Ohne jq:**
```bash
curl -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"sum","payload":{"numbers":[1,2,3,4,5]}}'

curl -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"sum","payload":{"numbers":[10,20,30,40,50,60,70,80,90,100]}}'
```

**Mit jq:**
```bash
curl -s -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"sum","payload":{"numbers":[1,2,3,4,5]}}' | jq

curl -s -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"sum","payload":{"numbers":[10,20,30,40,50,60,70,80,90,100]}}' | jq
```

**Erwartete Antworten:** `"result": "15"` und `"result": "550"`

---

## 5. Gateway Tests

Alle Requests können auch über den Gateway (Port 8082) gehen:

**Ohne jq:**
```bash
# Functions auflisten
curl http://localhost:8082/api/v1/functions

# Function abrufen
curl http://localhost:8082/api/v1/functions/name/hello

# Function ausführen
curl -X POST http://localhost:8082/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"hello","payload":{"name":"via Gateway"}}'
```

**Mit jq:**
```bash
curl -s http://localhost:8082/api/v1/functions | jq

curl -s http://localhost:8082/api/v1/functions/name/hello | jq

curl -s -X POST http://localhost:8082/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"hello","payload":{"name":"via Gateway"}}' | jq
```

---

## 6. Fehlerszenarien

### 6.1 Nicht existierende Function

```bash
curl -s -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"nicht-vorhanden","payload":{}}' | jq
```

**Erwartete Antwort:** `"message": "Function not found: nicht-vorhanden"`

### 6.2 Function ohne JAR (PENDING)

```bash
# Function ohne JAR erstellen
curl -s -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"no-jar","runtime":"java17","handler":"test.Handler::handle","memoryMb":256,"timeoutSeconds":30}' | jq

# Ausführen versuchen
curl -s -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"no-jar","payload":{}}' | jq
```

**Erwartete Antwort:** `"message": "Function is not ready: PENDING"`

### 6.3 Doppelter Function-Name

```bash
curl -s -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"hello","runtime":"java17","handler":"test.Test::handle","memoryMb":256,"timeoutSeconds":30}' | jq
```

**Erwartete Antwort:** `"message": "Function already exists with name: hello"`

### 6.4 Ungültiges Handler-Format

```bash
curl -s -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"bad-handler","runtime":"java17","handler":"InvalidHandler","memoryMb":256,"timeoutSeconds":30}' | jq
```

**Erwartete Antwort:** `"handler": "Handler must be in format 'package.ClassName::methodName'"`

---

## 7. Aufräumen

```bash
# Functions löschen
curl -X DELETE http://localhost:8080/api/v1/functions/name/hello
curl -X DELETE http://localhost:8080/api/v1/functions/name/reverse
curl -X DELETE http://localhost:8080/api/v1/functions/name/sum
curl -X DELETE http://localhost:8080/api/v1/functions/name/no-jar

# Prüfen ob leer
curl -s http://localhost:8080/api/v1/functions | jq

# Docker Compose stoppen
docker-compose down

# Mit Volumes löschen (Datenbank + MinIO zurücksetzen)
docker-compose down -v
```

---

## Troubleshooting

### Port bereits belegt

```bash
# Windows
netstat -ano | findstr :8080
netstat -ano | findstr :8081

# Linux/macOS
lsof -i :8080
lsof -i :8081

# Lösung: IntelliJ/andere Java-Prozesse stoppen
```

### Container startet nicht

```bash
# Logs prüfen
docker logs serverless-executor
docker logs serverless-registry

# Neu bauen
docker-compose down
docker-compose up -d --build
```

### "Function not found" obwohl erstellt

```bash
# 1. Prüfen ob Function existiert
curl -s http://localhost:8080/api/v1/functions | jq '.[].name'

# 2. Status prüfen (muss "READY" sein)
curl -s http://localhost:8080/api/v1/functions/name/hello | jq '.status'

# 3. Falls "PENDING": JAR hochladen!
```

### curl kann Datei nicht finden

```bash
# Problem: curl: (26) Failed to open/read local data from file/application

# Lösung: Prüfen ob im richtigen Verzeichnis
pwd  # Sollte .../serverless sein

# Oder relativen Pfad anpassen
# Falls in test-functions: -F "file=@jars/hello-function.jar"
# Falls in serverless:     -F "file=@test-functions/jars/hello-function.jar"
```

---

## Logs ansehen

```bash
# Alle Logs
docker-compose logs -f

# Einzelne Services
docker logs -f serverless-executor
docker logs -f serverless-registry
docker logs -f serverless-gateway

# Letzte 50 Zeilen
docker logs --tail 50 serverless-executor
```

---

## API Schnellreferenz

| Aktion | Methode | Endpunkt |
|--------|---------|----------|
| Functions auflisten | GET | `/api/v1/functions` |
| Function erstellen | POST | `/api/v1/functions` |
| Function abrufen | GET | `/api/v1/functions/name/{name}` |
| Function aktualisieren | PUT | `/api/v1/functions/name/{name}` |
| Function löschen | DELETE | `/api/v1/functions/name/{name}` |
| JAR hochladen | POST | `/api/v1/functions/name/{name}/upload` |
| Function ausführen | POST | `/api/v1/execute` |
