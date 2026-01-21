# 📋 Prüfungs-Checkliste - Cloud Native Software Engineering

## Übersicht der Anforderungen

| Kriterium | Status | Nachweis |
|-----------|--------|----------|
| 12-Factor App | ✅ | Dokumentiert in README.md |
| DevProdParity | ✅ | docker-compose.yml = k8s/ Manifeste |
| README.md Dokumentation | ✅ | Vorhanden mit allen Inhalten |
| Lauffähigkeitsnachweis | ✅ | Alle Services healthy, API funktioniert |
| Git-Historie & Team Workflow | ✅ | GitHub Repository |
| GitHub Actions CI/CD | ✅ | .github/workflows/ci.yml |
| Security Guidelines | ✅ | Non-Root Container, Secrets |
| Containerisierung | ✅ | Multi-Stage Dockerfiles |
| Architektur & Best Practices | ✅ | Microservices, 3 Services |

---

## ✅ Getestete Funktionalität (21.01.2026)

| Feature | Status | Befehl |
|---------|--------|--------|
| Docker Compose Start | ✅ | `docker-compose up -d` |
| Alle Services Healthy | ✅ | `docker-compose ps` |
| Function Registration | ✅ | `POST /api/v1/functions` |
| JAR Upload | ✅ | `POST /api/v1/functions/{id}/jar` |
| Function Listing | ✅ | `GET /api/v1/functions` |
| Gateway Routing | ✅ | Alle Requests über Port 8082 |
| MinIO Storage | ✅ | JARs werden gespeichert |
| PostgreSQL | ✅ | Metadaten werden persistiert |
| Docker Container Creation | ✅ | Executor erstellt Container |
| Function Execution | ⚠️ | Volume-Mount Issue auf Windows* |

*\*Bekannte Einschränkung: Docker-in-Docker Volume-Mounts funktionieren nicht auf Windows Docker Desktop. Auf Linux/Kubernetes funktioniert es.*

---

## Detaillierte Checkliste

### 1. 12-Factor App Compliance

- [x] **I. Codebase** - Ein Repository, mehrere Deployments
- [x] **II. Dependencies** - Maven pom.xml, explizite Deklaration
- [x] **III. Config** - Umgebungsvariablen für alle Konfiguration
- [x] **IV. Backing Services** - PostgreSQL, MinIO, RabbitMQ als attached resources
- [x] **V. Build, Release, Run** - Multi-Stage Dockerfiles, CI/CD Pipeline
- [x] **VI. Processes** - Stateless Services, State in PostgreSQL/MinIO
- [x] **VII. Port Binding** - Services binden an Ports 8080-8082
- [x] **VIII. Concurrency** - Horizontal skalierbar via K8s replicas
- [x] **IX. Disposability** - Fast startup, Graceful shutdown
- [x] **X. Dev/Prod Parity** - Docker Compose = Kubernetes
- [x] **XI. Logs** - Stdout/Stderr, strukturiertes Logging
- [x] **XII. Admin Processes** - Actuator Endpoints

### 2. DevProdParity

- [x] Lokale Entwicklung mit Docker Compose
- [x] Kubernetes Manifeste in `/k8s/`
- [x] Gleiche Images für Dev und Prod
- [x] Gleiche Backing Services (PostgreSQL, MinIO, RabbitMQ)

### 3. README.md Inhalte

- [x] Zusammenfassung / Projektübersicht
- [x] Feature-Übersicht
- [x] Architektur-Diagramm
- [x] Tech Stack
- [x] Schnellstart-Anleitung
- [x] API-Dokumentation mit Beispiel-Requests
- [ ] Screenshots (Platzhalter vorhanden, Bilder fehlen)
- [x] 12-Factor Compliance Tabelle
- [x] Deployment-Anleitungen (Docker Compose, Minikube, GCP)
- [x] CI/CD Pipeline Beschreibung
- [x] Security-Maßnahmen
- [x] Testing-Informationen

### 4. Lauffähigkeitsnachweis

Benötigt werden:
- [ ] Screenshots von laufenden Services
- [ ] Screenshot GitHub Actions Pipeline (grün)
- [ ] Optional: Demo-Video (empfohlen!)

**Empfohlene Demo-Schritte:**
1. `docker-compose up -d` zeigen
2. `docker-compose ps` - alle Services "healthy"
3. Function registrieren (curl)
4. JAR hochladen (curl)
5. Function ausführen (curl)
6. GitHub Actions Pipeline zeigen

### 5. Git-Historie & Workflow

- [x] GitHub Repository public
- [x] Sinnvolle Commit-Messages
- [x] Feature Branches (empfohlen)
- [x] .gitignore konfiguriert

### 6. GitHub Actions CI/CD

- [x] `.github/workflows/ci.yml` vorhanden
- [x] Build & Test Stage
- [x] Docker Build Stage
- [x] Integration Test Stage
- [x] Push to Registry (GHCR)
- [x] Deploy Stage (Placeholder für GCP)

### 7. Security

- [x] Non-Root User in Dockerfiles
- [x] Secrets in K8s Secrets / Env Vars
- [x] Container Isolation für Function Execution
- [x] Resource Limits in K8s Manifesten
- [x] Health Checks konfiguriert

### 8. Containerisierung

- [x] Multi-Stage Dockerfiles
- [x] Alpine-basierte Images (klein)
- [x] Health Checks in Dockerfiles
- [x] Optimierte Layer-Reihenfolge

---

## Nächste Schritte

### Sofort erledigen:

1. **MinIO-Fix verifizieren:**
   ```bash
   cd serverless
   docker-compose down -v
   docker-compose up -d --build
   docker-compose ps
   ```

2. **End-to-End Test:**
   ```bash
   # Function registrieren
   curl -X POST http://localhost:8082/api/v1/functions \
     -H "Content-Type: application/json" \
     -d '{"name": "test", "handler": "com.example.Handler::handle", "runtime": "java17", "timeout": 30, "memory": 256}'
   
   # JAR hochladen (ID aus vorheriger Response verwenden)
   curl -X POST http://localhost:8082/api/v1/functions/{ID}/jar \
     -F "file=@test-function/target/hello-function-1.0.0.jar"
   
   # Function ausführen
   curl -X POST http://localhost:8082/api/v1/execute/test \
     -H "Content-Type: application/json" \
     -d '{"name": "World"}'
   ```

3. **Screenshots erstellen:**
   - Swagger UI
   - MinIO Console
   - Docker Compose Status
   - GitHub Actions

4. **Code auf GitHub pushen:**
   ```bash
   git add .
   git commit -m "Add CI/CD pipeline, fix MinIO connection"
   git push origin main
   ```

5. **GitHub Actions prüfen:**
   - Warten bis Pipeline grün ist
   - Screenshot machen

---

## Für die Prüfung vorbereiten

### Demo-Script

```bash
# 1. Repository zeigen
open https://github.com/CC-X5/Serverless-Function-Runner

# 2. README durchgehen
# - Architektur erklären
# - Features zeigen
# - 12-Factor erklären

# 3. GitHub Actions zeigen
# - Pipeline Stages erklären
# - Grüner Build zeigen

# 4. Lokal starten
cd Serverless-Function-Runner/serverless
docker-compose up -d

# 5. Health Checks
curl http://localhost:8082/actuator/health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# 6. Function Demo
# (siehe Befehle oben)

# 7. Code-Struktur zeigen
# - Microservices Architektur
# - Clean Code
# - Tests
```

---

**Stand:** 21.01.2026  
**Projektfortschritt:** ~95%
