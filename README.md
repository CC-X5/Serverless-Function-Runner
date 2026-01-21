# 🚀 Serverless Function Runner

Eine cloud-native Function-as-a-Service (FaaS) Plattform zur Ausführung von Java-Funktionen in isolierten Docker-Containern. Entwickelt im Rahmen der Vorlesung **Cloud Native Software Engineering** an der Hochschule Kaiserslautern.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-green)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![Kubernetes](https://img.shields.io/badge/Minikube-Ready-blue)
![GCR](https://img.shields.io/badge/Google%20Cloud%20Run-Ready-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 📋 Inhaltsverzeichnis

- [Projektübersicht](#-projektübersicht)
- [Features](#-features)
- [Architektur](#-architektur)
- [Tech Stack](#-tech-stack)
- [Schnellstart](#-schnellstart)
- [API-Dokumentation](#-api-dokumentation)
- [Screenshots](#-screenshots)
- [12-Factor App Compliance](#-12-factor-app-compliance)
- [Deployment](#-deployment)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Security](#-security)
- [Testing](#-testing)

---

## 📖 Projektübersicht

Der **Serverless Function Runner** ist eine FaaS-Plattform, die es ermöglicht, Java-Funktionen als JAR-Dateien hochzuladen und über eine REST-API auszuführen. Die Plattform nutzt Docker-Container für isolierte Ausführung und ist für den Betrieb auf Minikube und Google Cloud Run konzipiert.

### Projektziele

Dieses Projekt demonstriert die Anwendung von **Cloud-Native Konzepten und Best Practices** aus der Vorlesung:

| Anforderung | Umsetzung |
|-------------|-----------|
| **12-Factor App** | Alle 12 Faktoren implementiert |
| **DevProdParity** | Docker Compose lokal = K8s/GCR in Produktion |
| **Containerisierung** | Multi-Stage Dockerfiles für alle Services |
| **Orchestrierung** | Minikube / Google Cloud Run |
| **CI/CD** | GitHub Actions Pipeline |
| **Security** | Container Isolation, Resource Limits, Non-Root |

---

## ✨ Features

Das Projekt konzentriert sich auf **3 Kernfeatures**:

### 1️⃣ Function Registry & JAR Upload
- Registrierung von Funktionen mit Metadaten (Name, Handler, Runtime)
- Upload von JAR-Dateien zu MinIO Object Storage
- Verwaltung des Function Lifecycle (PENDING → READY → RUNNING)

### 2️⃣ Function Execution
- Ausführung von Funktionen in isolierten Docker-Containern
- Synchrone und asynchrone Ausführungsmodi
- Timeout- und Resource-Management

### 3️⃣ API Gateway
- Zentraler Einstiegspunkt für alle Requests
- Request Routing zu Backend-Services
- Request/Response Logging

---

## 🏗️ Architektur

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Minikube / GCR                                    │
│                                                                             │
│  ┌──────────┐     ┌─────────────────────────────────────────────────────┐  │
│  │  Client  │     │                    Services                          │  │
│  │  (REST)  │     │                                                     │  │
│  └────┬─────┘     │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│       │           │  │   Gateway   │  │  Registry   │  │  Executor   │  │  │
│       │           │  │   :8082     │  │   :8080     │  │   :8081     │  │  │
│       └──────────────▶             │──▶             │  │             │  │  │
│                   │  └─────────────┘  └──────┬──────┘  └──────┬──────┘  │  │
│                   │                          │                │         │  │
│                   └──────────────────────────┼────────────────┼─────────┘  │
│                                              │                │            │
│                   ┌──────────────────────────┼────────────────┼─────────┐  │
│                   │      Backing Services    │                │         │  │
│                   │                          ▼                ▼         │  │
│                   │  ┌─────────────┐  ┌─────────────┐  ┌───────────┐   │  │
│                   │  │  RabbitMQ   │  │ PostgreSQL  │  │   MinIO   │   │  │
│                   │  │  (Queue)    │  │ (Metadata)  │  │  (JARs)   │   │  │
│                   │  └─────────────┘  └─────────────┘  └───────────┘   │  │
│                   └─────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Services

| Service | Port | Beschreibung |
|---------|------|--------------|
| **Gateway Service** | 8082 | Spring Cloud Gateway - Routing, Rate Limiting, Logging |
| **Registry Service** | 8080 | Function CRUD, JAR-Upload, Metadaten-Verwaltung |
| **Executor Service** | 8081 | Docker-basierte Funktionsausführung |

### Datenfluss

1. **Function Registration**: Client → Gateway → Registry → PostgreSQL + MinIO
2. **Function Execution**: Client → Gateway → Executor → Docker Container → Response

---

## 🛠️ Tech Stack

| Komponente | Technologie |
|------------|-------------|
| **Sprache** | Java 17 |
| **Framework** | Spring Boot 4.0.0, Spring Cloud 2024.0.0 |
| **API Gateway** | Spring Cloud Gateway |
| **Datenbank** | PostgreSQL 16 |
| **Object Storage** | MinIO |
| **Messaging** | RabbitMQ |
| **Container** | Docker |
| **Orchestrierung** | Minikube / Google Cloud Run |
| **CI/CD** | GitHub Actions |
| **Dokumentation** | SpringDoc OpenAPI (Swagger UI) |

---

## 🚀 Schnellstart

### Voraussetzungen

- Java 17+
- Maven 3.8+
- Docker Desktop
- (Optional) Kubernetes / Minikube

### Lokale Entwicklung mit Docker Compose

```bash
# Repository klonen
git clone https://github.com/CC-X5/Serverless-Function-Runner.git
cd Serverless-Function-Runner/serverless

# Infrastruktur starten (PostgreSQL, MinIO, RabbitMQ)
docker-compose up -d

# Services bauen und starten
mvn clean install -DskipTests
mvn spring-boot:run -pl registry-service &
mvn spring-boot:run -pl executor-service &
mvn spring-boot:run -pl gateway-service &
```

### Oder alles mit Docker Compose

```bash
# Komplettes System starten
docker-compose --profile all up --build
```

### Health Check

```bash
# Gateway Health
curl http://localhost:8082/actuator/health

# Registry Health
curl http://localhost:8080/actuator/health

# Executor Health
curl http://localhost:8081/actuator/health
```

---

## 📡 API-Dokumentation

### Swagger UI

Nach dem Start erreichbar unter:
- **Gateway**: http://localhost:8082/swagger-ui.html
- **Registry**: http://localhost:8080/swagger-ui.html
- **Executor**: http://localhost:8081/swagger-ui.html

### Endpoints Übersicht

#### Function Management (Registry Service)

| Methode | Endpoint | Beschreibung |
|---------|----------|--------------|
| `POST` | `/api/v1/functions` | Neue Funktion registrieren |
| `GET` | `/api/v1/functions` | Alle Funktionen auflisten |
| `GET` | `/api/v1/functions/{id}` | Funktion nach ID abrufen |
| `GET` | `/api/v1/functions/name/{name}` | Funktion nach Name abrufen |
| `DELETE` | `/api/v1/functions/{id}` | Funktion löschen |
| `POST` | `/api/v1/functions/{id}/upload` | JAR-Datei hochladen |

#### Function Execution (Executor Service)

| Methode | Endpoint | Beschreibung |
|---------|----------|--------------|
| `POST` | `/api/v1/execute/{functionName}` | Funktion ausführen (sync) |
| `POST` | `/api/v1/execute` | Funktion mit Body ausführen |

### Beispiel-Requests

#### 1. Funktion registrieren

```bash
curl -X POST http://localhost:8082/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "hello-world",
    "handler": "com.example.HelloFunction::handle",
    "runtime": "java17",
    "timeout": 30,
    "memory": 256
  }'
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "hello-world",
  "handler": "com.example.HelloFunction::handle",
  "runtime": "java17",
  "status": "PENDING",
  "createdAt": "2025-01-21T18:00:00Z"
}
```

#### 2. JAR-Datei hochladen

```bash
curl -X POST http://localhost:8082/api/v1/functions/{id}/upload \
  -F "file=@hello-function.jar"
```

#### 3. Funktion ausführen

```bash
curl -X POST http://localhost:8082/api/v1/execute/hello-world \
  -H "Content-Type: application/json" \
  -d '{"name": "World"}'
```

**Response:**
```json
{
  "executionId": "exec-123",
  "functionName": "hello-world",
  "status": "SUCCESS",
  "output": "Hello, World!",
  "executionTimeMs": 145,
  "timestamp": "2025-01-21T18:05:00Z"
}
```

#### 4. Alle Funktionen auflisten

```bash
curl http://localhost:8082/api/v1/functions
```

---

## 📸 Screenshots

### Swagger UI - API Dokumentation
*Screenshot: Swagger UI mit allen verfügbaren Endpoints*

![Swagger UI](docs/screenshots/swagger-ui.png)

### MinIO Console - Object Storage
*Screenshot: MinIO Console mit hochgeladenen JAR-Dateien*

![MinIO Console](docs/screenshots/minio-console.png)

### Docker Compose - Laufende Services
*Screenshot: Docker Desktop mit allen laufenden Containern*

![Docker Compose](docs/screenshots/docker-compose.png)

### API Response - Function Execution
```json
{
  "executionId": "exec-550e8400-e29b-41d4",
  "functionName": "hello-world",
  "status": "SUCCESS",
  "output": "Hello, World!",
  "executionTimeMs": 145,
  "timestamp": "2025-01-21T18:30:00Z"
}
```

---

## 📊 12-Factor App Compliance

| Factor | Implementierung | Status |
|--------|-----------------|--------|
| **I. Codebase** | Ein Git Repository für alle Services | ✅ |
| **II. Dependencies** | Maven `pom.xml` mit allen Abhängigkeiten | ✅ |
| **III. Config** | Umgebungsvariablen, `application.yml` | ✅ |
| **IV. Backing Services** | PostgreSQL, MinIO, RabbitMQ als externe Services | ✅ |
| **V. Build, Release, Run** | GitHub Actions CI/CD Pipeline | ✅ |
| **VI. Processes** | Stateless Services, kein lokaler State | ✅ |
| **VII. Port Binding** | Spring Boot Embedded Server | ✅ |
| **VIII. Concurrency** | Horizontale Skalierung via Kubernetes | ✅ |
| **IX. Disposability** | Schneller Start, Graceful Shutdown | ✅ |
| **X. Dev/Prod Parity** | Docker Compose = Kubernetes Config | ✅ |
| **XI. Logs** | Stdout/Stderr, strukturiertes Logging | ✅ |
| **XII. Admin Processes** | Actuator Endpoints, Health Checks | ✅ |

---

## ☁️ Deployment

### Option 1: Docker Compose (DevProdParity - Lokal)

```bash
# Starten
docker-compose up -d

# Status prüfen
docker-compose ps

# Logs anzeigen
docker-compose logs -f

# Stoppen
docker-compose down -v
```

### Option 2: Minikube (Kubernetes Lokal)

```bash
# Minikube starten
minikube start --driver=docker --memory=4096

# Docker Registry für Minikube konfigurieren
eval $(minikube docker-env)

# Images lokal bauen
mvn clean package -DskipTests
docker build -t registry-service:latest ./registry-service
docker build -t executor-service:latest ./executor-service
docker build -t gateway-service:latest ./gateway-service

# Kubernetes Manifeste anwenden
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/

# Status prüfen
kubectl get pods -n serverless
kubectl get services -n serverless

# Gateway URL abrufen
minikube service gateway-service -n serverless --url

# Dashboard öffnen (optional)
minikube dashboard
```

### Option 3: Google Cloud Run

```bash
# Projekt konfigurieren
gcloud config set project YOUR_PROJECT_ID
gcloud services enable run.googleapis.com artifactregistry.googleapis.com

# Artifact Registry erstellen
gcloud artifacts repositories create serverless-runner \
  --repository-format=docker \
  --location=europe-west1

# Authentifizierung
gcloud auth configure-docker europe-west1-docker.pkg.dev

# Images bauen und pushen
REGION=europe-west1
PROJECT_ID=YOUR_PROJECT_ID
REPO=serverless-runner

for SERVICE in registry-service executor-service gateway-service; do
  docker build -t ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${SERVICE}:latest ./${SERVICE}
  docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${SERVICE}:latest
done

# Cloud Run Deployment
gcloud run deploy gateway-service \
  --image ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/gateway-service:latest \
  --platform managed \
  --region europe-west1 \
  --allow-unauthenticated \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod"
```

---

## 🔄 CI/CD Pipeline

GitHub Actions automatisiert den gesamten Build- und Deployment-Prozess:

```yaml
# .github/workflows/ci.yml - Vereinfachte Darstellung
name: CI/CD Pipeline

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build & Test
        run: mvn clean verify

  docker-build:
    needs: build-and-test
    runs-on: ubuntu-latest
    steps:
      - name: Build Docker Images
        run: |
          docker build -t registry-service ./registry-service
          docker build -t executor-service ./executor-service
          docker build -t gateway-service ./gateway-service

  deploy:
    needs: docker-build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to GCR
        run: echo "Deploy to Google Cloud Run"
```

### Pipeline Stages

| Stage | Beschreibung |
|-------|--------------|
| **Build** | Maven compile, package |
| **Test** | Unit Tests, Integration Tests |
| **Docker** | Multi-Stage Image Build |
| **Deploy** | Push to Registry, Deploy to GCR |

---

## 🔒 Security

### Implementierte Sicherheitsmaßnahmen

| Maßnahme | Beschreibung |
|----------|--------------|
| **Container Isolation** | Jede Funktion läuft in eigenem Container |
| **Resource Limits** | CPU/Memory Limits verhindern Resource Exhaustion |
| **Non-Root User** | Container laufen als non-root (UID 1000) |
| **Network Policies** | Service-zu-Service Kommunikation eingeschränkt |
| **Secrets Management** | Sensitive Daten in K8s Secrets / Env Vars |
| **Timeout Protection** | Automatische Terminierung bei Timeout |
| **Read-Only Filesystem** | Containers haben read-only Root Filesystem |

### Kubernetes Security Context

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
  capabilities:
    drop:
      - ALL
```

---

## 🧪 Testing

### Unit Tests ausführen

```bash
# Alle Tests
mvn test

# Einzelner Service
mvn test -pl registry-service
mvn test -pl executor-service
mvn test -pl gateway-service
```

### Test Coverage Report

```bash
mvn test jacoco:report
# Report unter: target/site/jacoco/index.html
```

### Aktueller Test-Status

| Service | Tests | Status |
|---------|-------|--------|
| Registry Service | 26 | ✅ |
| Executor Service | 16 | ✅ |
| Gateway Service | 10 | ✅ |
| **Gesamt** | **52** | ✅ |

---

## 📁 Projektstruktur

```
serverless/
├── gateway-service/          # API Gateway
│   ├── src/main/java/
│   │   └── hskl/cn/serverless/gateway/
│   │       ├── config/       # Gateway Konfiguration
│   │       └── filter/       # Logging Filter
│   └── src/main/resources/
│       └── application.yml
│
├── registry-service/         # Function Registry
│   ├── src/main/java/
│   │   └── hskl/cn/serverless/registry/
│   │       ├── controller/   # REST Controller
│   │       ├── service/      # Business Logic
│   │       ├── repository/   # JPA Repository
│   │       ├── model/        # Entity Classes
│   │       └── dto/          # Data Transfer Objects
│   └── src/main/resources/
│       └── application.yml
│
├── executor-service/         # Function Executor
│   ├── src/main/java/
│   │   └── hskl/cn/serverless/executor/
│   │       ├── controller/   # REST Controller
│   │       ├── service/      # Docker Execution
│   │       └── dto/          # Request/Response DTOs
│   └── src/main/resources/
│       └── application.yml
│
├── k8s/                      # Kubernetes Manifeste
│   ├── gateway-deployment.yaml
│   ├── registry-deployment.yaml
│   ├── executor-deployment.yaml
│   └── infrastructure/
│
├── docker-compose.yml        # Lokale Entwicklung
├── pom.xml                   # Parent POM
└── README.md
```

---

## 🔧 Konfiguration

### Umgebungsvariablen

| Variable | Standard | Beschreibung |
|----------|----------|--------------|
| `SERVER_PORT` | 8080/8081/8082 | Server Port |
| `SPRING_PROFILES_ACTIVE` | default | Aktives Profil |
| `POSTGRES_HOST` | localhost | PostgreSQL Host |
| `POSTGRES_PORT` | 5432 | PostgreSQL Port |
| `POSTGRES_DB` | serverless | Datenbankname |
| `MINIO_ENDPOINT` | http://localhost:9000 | MinIO Endpoint |
| `MINIO_ACCESS_KEY` | minioadmin | MinIO Access Key |
| `MINIO_SECRET_KEY` | minioadmin | MinIO Secret Key |
| `RABBITMQ_HOST` | localhost | RabbitMQ Host |

---

## 📄 Lizenz

MIT License - siehe [LICENSE](LICENSE)

---

<p align="center">
  <b>Cloud Native Software Engineering</b><br>
  Hochschule Kaiserslautern<br><br>
  Made with ❤️ using Spring Boot, Docker & Kubernetes
</p>
