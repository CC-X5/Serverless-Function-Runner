# 🚀 Serverless Function Runner

Eine cloud-native Function-as-a-Service (FaaS) Plattform zur Ausführung von Java-Funktionen in isolierten Docker-Containern.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 📋 Inhaltsverzeichnis

- [Features](#-features)
- [Architektur](#-architektur)
- [Schnellstart](#-schnellstart)
- [Test Functions](#-test-functions)
- [API Referenz](#-api-referenz)
- [Deployment](#-deployment)
- [Konfiguration](#-konfiguration)

---

## ✨ Features

| Feature | Beschreibung |
|---------|--------------|
| **Function Registry** | Registrierung & Verwaltung von Funktionen mit Metadaten |
| **JAR Upload** | Upload von Java-Funktionen als JAR-Dateien zu MinIO |
| **Isolated Execution** | Ausführung in isolierten Docker-Containern |
| **API Gateway** | Zentraler Einstiegspunkt mit Routing & Logging |
| **Health Monitoring** | Spring Actuator Health Endpoints |

---

## 🏗️ Architektur

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Compose                          │
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐        │
│  │   Gateway   │───▶│  Registry   │    │  Executor   │        │
│  │    :8082    │    │    :8080    │    │    :8081    │        │
│  └─────────────┘    └──────┬──────┘    └──────┬──────┘        │
│                            │                  │                │
│                     ┌──────┴──────┐    ┌──────┴──────┐        │
│                     │             │    │             │        │
│  ┌─────────────┐    │  ┌───────┐  │    │  ┌───────┐  │        │
│  │  RabbitMQ   │    │  │  DB   │  │    │  │ MinIO │  │        │
│  │   :5672     │    │  │ :5432 │  │    │  │ :9000 │  │        │
│  └─────────────┘    │  └───────┘  │    │  └───────┘  │        │
│                     └─────────────┘    └─────────────┘        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Services

| Service | Port | Funktion |
|---------|------|----------|
| **Gateway** | 8082 | API Gateway, Request Routing |
| **Registry** | 8080 | Function CRUD, JAR Upload |
| **Executor** | 8081 | Docker-basierte Ausführung |
| **PostgreSQL** | 5432 | Metadaten-Speicher |
| **MinIO** | 9000/9001 | JAR-Dateien Storage |
| **RabbitMQ** | 5672/15672 | Message Queue |

---

## 🚀 Schnellstart

### Voraussetzungen

- Docker Desktop
- Java 17+ (für lokale Entwicklung)
- Maven 3.8+ (für lokale Entwicklung)

### 1. System starten

```bash
cd serverless
docker-compose up -d --build
```

### 2. Health Check

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### 3. Function erstellen & ausführen

```bash
# Function registrieren
curl -X POST http://localhost:8080/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "hello",
    "runtime": "java17",
    "handler": "hskl.cn.serverless.function.HelloFunction::handle",
    "memoryMb": 256,
    "timeoutSeconds": 30
  }'

# JAR hochladen
curl -X POST http://localhost:8080/api/v1/functions/name/hello/upload \
  -F "file=@test-functions/jars/hello-function.jar"

# Ausführen
curl -X POST http://localhost:8081/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName": "hello", "payload": {"name": "World"}}'
```

**Erwartete Antwort:**
```json
{
  "status": "SUCCESS",
  "result": "Hello, World!",
  "durationMs": 324
}
```

### 📖 Vollständige Test-Dokumentation

➡️ **[docs/TESTING.md](serverless/docs/TESTING.md)** - Alle API-Befehle mit und ohne `jq`

---

## 🧪 Test Functions

Drei vorgefertigte Test-Funktionen sind im Projekt enthalten:

| Function | Beschreibung | Input | Output |
|----------|--------------|-------|--------|
| **hello** | Begrüßung | `{"name": "Peter"}` | `Hello, Peter!` |
| **reverse** | String umkehren | `{"text": "ABC"}` | `CBA` |
| **sum** | Zahlen addieren | `{"numbers": [1,2,3]}` | `6` |

### Test-Functions neu bauen

```bash
cd serverless/test-functions
./build.sh
```

Das Script:
1. Löscht alte `target/` Ordner
2. Baut alle Funktionen mit Maven
3. Kopiert die JARs nach `jars/`

### Manuelle Schritte

```bash
cd serverless/test-functions
mvn clean package
cp helloF/target/hello-function.jar jars/
cp reverseF/target/reverse-function.jar jars/
cp sumF/target/sum-function.jar jars/
```

---

## 📡 API Referenz

### Function Management

| Methode | Endpunkt | Beschreibung |
|---------|----------|--------------|
| `GET` | `/api/v1/functions` | Alle Functions auflisten |
| `POST` | `/api/v1/functions` | Neue Function erstellen |
| `GET` | `/api/v1/functions/name/{name}` | Function nach Name |
| `PUT` | `/api/v1/functions/name/{name}` | Function aktualisieren |
| `DELETE` | `/api/v1/functions/name/{name}` | Function löschen |
| `POST` | `/api/v1/functions/name/{name}/upload` | JAR hochladen |

### Function Execution

| Methode | Endpunkt | Beschreibung |
|---------|----------|--------------|
| `POST` | `/api/v1/execute` | Function ausführen |

### Request/Response Beispiele

**Function erstellen:**
```json
POST /api/v1/functions
{
  "name": "hello",
  "runtime": "java17",
  "handler": "package.ClassName::methodName",
  "memoryMb": 256,
  "timeoutSeconds": 30,
  "description": "Optional description"
}
```

**Function ausführen:**
```json
POST /api/v1/execute
{
  "functionName": "hello",
  "payload": { "name": "World" }
}
```

**Response:**
```json
{
  "executionId": "uuid",
  "functionName": "hello",
  "status": "SUCCESS",
  "result": "Hello, World!",
  "durationMs": 324,
  "startedAt": "2026-01-23T21:48:02",
  "completedAt": "2026-01-23T21:48:03"
}
```

---

## 🚢 Deployment

### Option 1: Docker Compose (Lokal)

```bash
cd serverless
docker-compose up -d
```

### Option 2: Kubernetes / Minikube

```bash
cd k8s
./deploy.sh

# Oder manuell:
kubectl apply -f 00-namespace.yaml
kubectl apply -f .
```

### Web UIs

| Service | URL | Credentials |
|---------|-----|-------------|
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |
| RabbitMQ Management | http://localhost:15672 | guest / guest |

---

## ⚙️ Konfiguration

### Umgebungsvariablen

| Variable | Default | Beschreibung |
|----------|---------|--------------|
| `DATABASE_URL` | `jdbc:postgresql://postgres:5432/serverless` | DB Connection |
| `DATABASE_USER` | `postgres` | DB User |
| `DATABASE_PASSWORD` | `postgres` | DB Password |
| `MINIO_ENDPOINT` | `http://minio:9000` | MinIO URL |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO Access Key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO Secret Key |
| `RABBITMQ_HOST` | `rabbitmq` | RabbitMQ Host |
| `DOCKER_HOST` | `unix:///var/run/docker.sock` | Docker Socket |

---

## 📁 Projektstruktur

```
serverless/
├── gateway-service/        # API Gateway (Port 8082)
├── registry-service/       # Function Registry (Port 8080)
├── executor-service/       # Function Executor (Port 8081)
├── test-functions/         # Vorgefertigte Test-Funktionen
│   ├── helloF/            
│   ├── reverseF/          
│   ├── sumF/              
│   ├── jars/              # Kompilierte JARs
│   └── build.sh           # Build Script
├── docs/
│   └── TESTING.md         # Vollständige Test-Dokumentation
├── docker-compose.yml
└── pom.xml

k8s/                        # Kubernetes Manifeste
├── 00-namespace.yaml
├── 01-configmap.yaml
├── 02-secrets.yaml
├── 10-postgres.yaml
├── 11-minio.yaml
├── 12-rabbitmq.yaml
├── 20-registry-service.yaml
├── 21-executor-service.yaml
├── 22-gateway-service.yaml
└── deploy.sh
```

---

## 📄 Lizenz

MIT License - siehe [LICENSE](LICENSE)

---

<p align="center">
  <b>Cloud Native Software Engineering</b><br>
  Hochschule Kaiserslautern
</p>
