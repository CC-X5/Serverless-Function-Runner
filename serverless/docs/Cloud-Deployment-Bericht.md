# 🌩️ Cloud Deployment - Erfahrungsbericht

## Projektübersicht

| Attribut | Wert |
|----------|------|
| **Projekt-ID** | `triple-hour-485121-e0` |
| **Region** | `europe-west1` |
| **Zeitraum** | 17.-23. Januar 2026 |
| **Gesamtkosten** | 0,00 € (0,22 € Kosten - 0,22 € Einsparungen) |
| **Guthaben verbleibend** | 255 € |

---

# Teil 1: Kubernetes (Minikube) Deployment

## 🚀 Lokales K8s Deployment

### Deployment starten
```bash
kubectl apply -f k8s/
```

**Erstellte Ressourcen:**
- `namespace/serverless` ✅
- `configmap/serverless-config` ✅
- `secret/serverless-secrets` ✅
- `persistentvolumeclaim/postgres-pvc` ✅
- `persistentvolumeclaim/minio-pvc` ✅
- `deployment.apps/postgres` ✅
- `deployment.apps/minio` ✅
- `deployment.apps/rabbitmq` ✅
- `deployment.apps/registry-service` ✅
- `deployment.apps/executor-service` ✅
- `deployment.apps/gateway-service` ✅

### Pod Status nach ~20 Sekunden
```
NAME                                READY   STATUS              RESTARTS   AGE
executor-service-d55bc547f-hd8wr    0/1     ContainerCreating   0          19s
gateway-service-cbf78d58f-mjjlq     0/1     ContainerCreating   0          19s
minio-54cc7bfd84-dc8cz              1/1     Running             0          19s
postgres-6656df4c9d-jf28w           0/1     Running             0          19s
rabbitmq-6b679dbd8-bdnpb            0/1     ContainerCreating   0          19s
registry-service-55fbb4f9f7-cf85q   0/1     ContainerCreating   0          19s
```

### Pod Status nach ~7-21 Minuten (RUNNING!)
```
NAME                                READY   STATUS    RESTARTS   AGE
executor-service-6bc58559bb-7vv4b   1/1     Running   0          7m25s
gateway-service-7cf9b6c7c6-d94zw    1/1     Running   0          7m17s
minio-54cc7bfd84-dc8cz              1/1     Running   0          21m
postgres-6656df4c9d-jf28w           1/1     Running   0          21m
rabbitmq-6b679dbd8-bdnpb            0/1     Running   9 (63s ago) 21m
registry-service-647c56896b-fd8gn   1/1     Running   0          7m35s
```

### Services
```
NAME               TYPE        CLUSTER-IP       PORT(S)           AGE
executor-service   ClusterIP   10.109.155.68    8081/TCP          22m
gateway-service    NodePort    10.106.216.129   8082:30082/TCP    22m
minio              ClusterIP   10.111.243.250   9000/TCP,9001/TCP 22m
postgres           ClusterIP   10.110.234.23    5432/TCP          22m
rabbitmq           ClusterIP   10.97.179.45     5672/TCP,15672/TCP 22m
registry-service   ClusterIP   10.109.155.68    8080/TCP          22m
```

### Minikube Service Tunnel (Windows)
```bash
minikube service gateway-service -n serverless --url
```
**Ausgabe:**
```
NAMESPACE    NAME             TARGET PORT   URL
serverless   gateway-service  8082          http://192.168.49.2:30082
                                            http://127.0.0.1:50445
```
⚠️ *"Because you are using a Docker driver on windows, the terminal needs to be open to run it."*

### API Tests (Kubernetes)

**Health Check:**
```bash
curl http://127.0.0.1:50445/actuator/health
```
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"],
  "components": {
    "discoveryComposite": {"description": "Discovery Client not initialized", "status": "UNKNOWN"},
    "diskSpace": {"status": "UP", "details": {"total": 1081101176832, "free": 1007099871232}},
    "livenessState": {"status": "UP"},
    "ping": {"status": "UP"},
    "readinessState": {"status": "UP"},
    "ssl": {"status": "UP"}
  }
}
```

**Function erstellen:**
```bash
curl -X POST http://127.0.0.1:50445/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name": "k8s-test", "handler": "hskl.cn.serverless.function.HelloFunction::handle", "runtime": "java17", "timeout": 30, "memory": 256}'
```
```json
{
  "id": "cd3aafae-b408-4a0a-9136-cd57f715573e",
  "name": "k8s-test",
  "runtime": "java17",
  "handler": "hskl.cn.serverless.function.HelloFunction::handle",
  "status": "PENDING",
  "timeoutSeconds": 30,
  "memoryMb": 256
}
```

**Functions auflisten:**
```bash
curl http://127.0.0.1:50445/api/v1/functions
```
```json
[{"id":"cd3aafae-b408-4a0a-9136-cd57f715573e","name":"k8s-test","status":"PENDING",...}]
```

### K8s Deployment Status: ✅ ERFOLGREICH

| Komponente | Status | Notizen |
|------------|--------|---------|
| PostgreSQL | ✅ Running | PVC mounted |
| MinIO | ✅ Running | PVC mounted |
| RabbitMQ | ⚠️ Running | 9 Restarts |
| Registry Service | ✅ Running | API funktioniert |
| Executor Service | ✅ Running | |
| Gateway Service | ✅ Running | NodePort 30082 |

---

# Teil 2: Google Cloud Run Deployment

## 📋 Deployment-Ablauf

### 1. Projekt-Setup
- Google Cloud Shell gestartet
- Repository geklont: `git clone https://github.com/CC-X5/Serverless-Function-Runner.git`
- APIs aktiviert:
  - `artifactregistry.googleapis.com`
  - `cloudbuild.googleapis.com`
  - `run.googleapis.com`

### 2. Infrastruktur erstellt

#### PostgreSQL (Cloud SQL)
```bash
gcloud sql instances create serverless-postgres \
  --database-version=POSTGRES_16 \
  --tier=db-g1-small \
  --region=europe-west1 \
  --root-password=postgres123 \
  --edition=ENTERPRISE
```
- **Status**: ✅ Erfolgreich erstellt
- **IP-Adresse**: `35.205.195.191`
- **Tier**: `db-g1-small`

#### Cloud Storage Bucket (für JAR-Dateien)
```bash
gsutil mb -l europe-west1 gs://triple-hour-485121-e0-functions
```
- **Status**: ⚠️ Bucket existierte bereits (409 Error)
- **Lösung**: Bestehenden Bucket verwendet

### 3. Docker Images gebaut

#### Cloud Build (cloudbuild.yaml)
```yaml
steps:
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'europe-west1-docker.pkg.dev/$PROJECT_ID/serverless-runner/registry-service:latest', 
           '-f', 'registry-service/Dockerfile', '.']
  # ... executor-service, gateway-service
timeout: 2400s
options:
  machineType: 'E2_HIGHCPU_8'
```

**Build-Ergebnis**:
- **Dauer**: 3 Minuten 12 Sekunden
- **Status**: ✅ SUCCESS
- **Images**: 
  - `registry-service:latest`
  - `executor-service:latest`
  - `gateway-service:latest`

### 4. Services deployed

#### Registry Service
```bash
gcloud run deploy registry-service \
  --image=europe-west1-docker.pkg.dev/triple-hour-485121-e0/serverless-runner/registry-service:latest \
  --region=europe-west1 \
  --platform=managed \
  --allow-unauthenticated \
  --port=8080 \
  --memory=1Gi \
  --timeout=300 \
  --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://35.205.195.191:5432/serverless,..."
```
- **URL**: `https://registry-service-771740314856.europe-west1.run.app`
- **Status**: ✅ Deployed

#### Executor Service
- **URL**: `https://executor-service-771740314856.europe-west1.run.app`
- **Status**: ✅ Deployed

#### Gateway Service
- **URL**: `https://gateway-service-771740314856.europe-west1.run.app`
- **Status**: ✅ Deployed

---

## ⚠️ Aufgetretene Probleme

### Problem 1: IAM Permission Denied
**Fehlermeldung**:
```
ERROR: (gcloud.run.deploy) PERMISSION_DENIED: Build failed because the default 
service account is missing required IAM permissions.
```
**Lösung**:
```bash
PROJECT_NUMBER=771740314856
gcloud projects add-iam-policy-binding triple-hour-485121-e0 \
  --member=serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com \
  --role=roles/cloudbuild.builds.builder

gcloud projects add-iam-policy-binding triple-hour-485121-e0 \
  --member=serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com \
  --role=roles/storage.admin
```

### Problem 2: Maven Wrapper fehlt
**Fehlermeldung**:
```
COPY failed: file not found in build context: stat .mvn/: file does not exist
```
**Projektstruktur zeigt**:
```
drwxrwxr-x  .mvn/
-rwxrwxr-x  mvnw
drwxrwxr-x  wrapper/
```
**Lösung**: `.mvn/` Ordner war vorhanden, aber `wrapper/` Unterordner fehlte im Build-Context

### Problem 3: RabbitMQ Connection Refused
**Health Check Ausgabe**:
```json
{
  "rabbit": {
    "status": "DOWN",
    "details": {
      "error": "org.springframework.amqp.AmqpConnectException: Connection refused"
    }
  }
}
```
**Ursache**: RabbitMQ läuft nicht in Cloud Run (kein Managed Service)

### Problem 4: Discovery Client nicht initialisiert
```json
{
  "discoveryComposite": {
    "description": "Discovery Client not initialized",
    "status": "UNKNOWN"
  }
}
```
**Ursache**: Spring Cloud Discovery ist konfiguriert aber kein Eureka/Consul vorhanden

### Problem 5: Build failed nach IAM Fix
**Fehlermeldung**:
```
Deployment failed
ERROR: (gcloud.run.deploy) Build failed; check build logs for details
```
**Lösung**: Mehrere Versuche mit korrigierten Permissions

---

## 🔧 Test-Functions bauen

```bash
cd ~/Serverless-Function-Runner/test-function
mvn package -DskipTests
```
```
[INFO] Scanning for projects...
[INFO] ----------------< hskl.cn.serverless:hello-function >-----------------
[INFO] Building hello-function 1.0.0
[INFO] --------------------------------[ jar ]--------------------------------
```

---

## ✅ Funktionierende Komponenten

### Kubernetes (Minikube)
| Komponente | Status |
|------------|--------|
| PostgreSQL | ✅ Running |
| MinIO | ✅ Running |
| RabbitMQ | ⚠️ Running (Restarts) |
| Registry Service | ✅ Running |
| Executor Service | ✅ Running |
| Gateway Service | ✅ Running |
| **API Tests** | ✅ Funktioniert |

### Google Cloud Run
| Komponente | Status | URL |
|------------|--------|-----|
| Registry Service | ✅ Deployed | https://registry-service-771740314856.europe-west1.run.app |
| Executor Service | ✅ Deployed | https://executor-service-771740314856.europe-west1.run.app |
| Gateway Service | ✅ Deployed | https://gateway-service-771740314856.europe-west1.run.app |
| Cloud SQL | ✅ Running | 35.205.195.191:5432 |

---

## 🧹 Cleanup durchgeführt

```bash
# Cloud Run Services löschen
gcloud run services delete registry-service --region=europe-west1 --quiet
gcloud run services delete executor-service --region=europe-west1 --quiet
gcloud run services delete gateway-service --region=europe-west1 --quiet

# Cloud SQL Instanz löschen
gcloud sql instances delete serverless-postgres --quiet

# Docker Images löschen
gcloud artifacts docker images delete europe-west1-docker.pkg.dev/triple-hour-485121-e0/serverless-runner/registry-service --quiet
gcloud artifacts docker images delete europe-west1-docker.pkg.dev/triple-hour-485121-e0/serverless-runner/executor-service --quiet
gcloud artifacts docker images delete europe-west1-docker.pkg.dev/triple-hour-485121-e0/serverless-runner/gateway-service --quiet
```

---

## 📊 Kostenanalyse

| Ressource | Kosten |
|-----------|--------|
| Cloud Run | ~0,10 € |
| Cloud SQL | ~0,10 € |
| Artifact Registry | ~0,02 € |
| **Gesamt** | **0,22 €** |
| **Nach Einsparungen** | **0,00 €** |

*Alle Kosten durch kostenloses Testguthaben abgedeckt*

---

## 🎯 Vergleich: Kubernetes vs. Cloud Run

| Aspekt | Kubernetes (Minikube) | Google Cloud Run |
|--------|----------------------|------------------|
| **Setup-Komplexität** | Mittel | Einfach |
| **RabbitMQ** | ✅ Funktioniert | ❌ Nicht verfügbar |
| **MinIO** | ✅ Funktioniert | ❌ Nicht verfügbar |
| **PostgreSQL** | ✅ Als Pod | ✅ Cloud SQL |
| **Skalierung** | Manuell | Automatisch |
| **Kosten** | Lokal kostenlos | Pay-per-use |
| **Docker-in-Docker** | ✅ Möglich | ❌ Nicht möglich |

---

## 📁 Architektur-Empfehlung

### Für lokale Entwicklung: **Kubernetes/Minikube**
```
┌─────────────────────────────────────────────────────────────┐
│                    Minikube Cluster                         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│  │Registry │ │Executor │ │ Gateway │ │RabbitMQ │          │
│  └────┬────┘ └────┬────┘ └────┬────┘ └─────────┘          │
│       │           │           │                            │
│  ┌────┴───────────┴───────────┴────┐  ┌─────────┐         │
│  │          PostgreSQL             │  │  MinIO  │         │
│  └─────────────────────────────────┘  └─────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### Für Produktion: **Cloud Run + Managed Services**
```
┌─────────────────────────────────────────────────────────────┐
│                    Google Cloud Run                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                    │
│  │Registry │  │Executor*│  │ Gateway │                    │
│  └────┬────┘  └────┬────┘  └────┬────┘                    │
└───────┼────────────┼────────────┼──────────────────────────┘
        │            │            │
   ┌────┴────┐  ┌────┴────┐  ┌────┴────┐
   │Cloud SQL│  │Cloud    │  │Cloud    │
   │PostgreSQL│ │Storage  │  │Pub/Sub  │
   └─────────┘  └─────────┘  └─────────┘

*Executor benötigt Cloud Run Jobs für Docker-Execution
```

---

**Fazit**: Beide Deployment-Varianten wurden erfolgreich getestet. Kubernetes bietet mehr Flexibilität für die komplette Architektur, während Cloud Run einfacher zu deployen ist aber Anpassungen an den Backing Services erfordert.

