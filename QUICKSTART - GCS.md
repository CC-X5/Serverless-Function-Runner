# Quickstart - Serverless Function Runner

Vollstaendige Anleitung zum Aufsetzen und Demonstrieren des Projekts auf Google Kubernetes Engine.

---

## Architektur

```
                     Internet
                        |
                  [LoadBalancer]
                        |
               [Gateway Service :8082]
                     /        \
     [Registry Service :8080]  [Executor Service :8081]
           |        |                    |
     [PostgreSQL] [MinIO]         [DinD Sidecar]
                                        |
                                [Function Container]
           |
      [RabbitMQ]
```

| Komponente | Beschreibung |
|------------|-------------|
| Gateway Service | API-Gateway, Routing, Request-Logging |
| Registry Service | Function-Verwaltung, JAR-Upload nach MinIO |
| Executor Service | Fuehrt Functions in isolierten Docker-Containern aus |
| DinD Sidecar | Docker-in-Docker (GKE nutzt containerd, kein Docker) |
| PostgreSQL | Metadaten-Speicher (Function-Definitionen) |
| MinIO | Object Storage fuer JAR-Dateien |
| RabbitMQ | Message Queue fuer asynchrone Operationen |

---

## Teil 1: GCP Setup (einmalig)

### 1.1 gcloud CLI installieren

Download: https://cloud.google.com/sdk/docs/install

### 1.2 Authentifizieren und Projekt erstellen

```bash
gcloud auth login

gcloud projects create serverless-function-runner --name="Serverless Function Runner"
gcloud config set project serverless-function-runner
gcloud config set compute/region europe-west3
```

### 1.3 Billing verknuepfen

```bash
gcloud billing accounts list
gcloud billing projects link serverless-function-runner --billing-account=DEINE_BILLING_ACCOUNT_ID
```

### 1.4 APIs aktivieren

```bash
gcloud services enable container.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com
```

### 1.5 Artifact Registry erstellen

```bash
gcloud artifacts repositories create serverless-runner \
  --repository-format=docker \
  --location=europe-west3
```

### 1.6 GKE Cluster erstellen

```bash
gcloud container clusters create serverless-cluster \
  --region europe-west3 \
  --num-nodes 1 \
  --machine-type e2-standard-2 \
  --disk-size 30 \
  --enable-ip-alias
```

> GKE Standard (nicht Autopilot) ist noetig, da der Executor-Service einen privilegierten DinD-Container benoetigt.

### 1.7 kubectl verbinden

```bash
gcloud container clusters get-credentials serverless-cluster --region europe-west3
```

---

## Teil 2: Docker Images bauen und pushen

```bash
cd serverless
gcloud builds submit --config=cloudbuild.yaml .
```

Baut alle 3 Services und pusht sie nach Google Artifact Registry.

---

## Teil 3: Deployment

### Option A: Manuell mit Script

```bash
cd k8s
chmod +x deploy-gke.sh
./deploy-gke.sh
```

### Option B: Manuell Schritt fuer Schritt

```bash
cd k8s

# Namespace und Konfiguration
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-configmap.yaml
kubectl apply -f 02-secrets.yaml

# Infrastruktur
kubectl apply -f 10-postgres.yaml
kubectl apply -f 11-minio.yaml
kubectl apply -f 12-rabbitmq.yaml

# Warten bis Infrastruktur bereit ist
kubectl wait --for=condition=ready pod -l app=postgres -n serverless --timeout=120s
kubectl wait --for=condition=ready pod -l app=minio -n serverless --timeout=120s

# Application Services
kubectl apply -f 20-registry-service.yaml
kubectl apply -f 21-executor-service.yaml
kubectl apply -f 22-gateway-service.yaml

# Warten bis Services bereit sind
kubectl wait --for=condition=ready pod -l app=registry-service -n serverless --timeout=240s
kubectl wait --for=condition=ready pod -l app=executor-service -n serverless --timeout=240s
kubectl wait --for=condition=ready pod -l app=gateway-service -n serverless --timeout=240s
```

### Option C: Automatisch via GitHub Actions

Bei jedem Push auf `main` wird automatisch gebaut, getestet und deployed.

Voraussetzung: GitHub Secrets konfiguriert (siehe Teil 5).

```bash
git push origin main
```

---

## Teil 3.5: Test-Functions bauen

Vor der Demo muessen die JAR-Dateien der Test-Functions gebaut werden.

### Voraussetzung

- Java 17 (JDK) installiert
- Maven installiert

### JARs bauen

```bash
cd serverless/test-functions
mvn clean package -q
mkdir -p jars
cp helloF/target/hello-function.jar jars/
cp reverseF/target/reverse-function.jar jars/
cp sumF/target/sum-function.jar jars/
```

Oder mit dem Build-Script:

```bash
cd serverless/test-functions
chmod +x build.sh
./build.sh
```

Danach liegen 3 JARs in `serverless/test-functions/jars/`:
- `hello-function.jar`
- `reverse-function.jar`
- `sum-function.jar`

---

## Teil 4: Live Demo

### 4.1 Status pruefen

```bash
kubectl get pods -n serverless
kubectl get svc -n serverless
```

Die EXTERNAL-IP vom gateway-service ist die oeffentliche URL.

### 4.2 Health Check

```bash
curl -s http://EXTERNAL_IP:8082/actuator/health | jq
```

### 4.3 Functions registrieren

```bash
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"hello","runtime":"java17","handler":"hskl.cn.serverless.function.HelloFunction::handle","memoryMb":256,"timeoutSeconds":30,"description":"Begruessung"}' | jq

curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"reverse","runtime":"java17","handler":"hskl.cn.serverless.function.ReverseFunction::handle","memoryMb":256,"timeoutSeconds":30,"description":"String umkehren"}' | jq

curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"sum","runtime":"java17","handler":"hskl.cn.serverless.function.SumFunction::handle","memoryMb":256,"timeoutSeconds":30,"description":"Zahlen summieren"}' | jq
```

### 4.4 Alle Functions auflisten

```bash
curl -s http://EXTERNAL_IP:8082/api/v1/functions | jq
```

### 4.5 JARs hochladen

Aus dem `serverless/` Verzeichnis:

```bash
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions/name/hello/upload \
  -F "file=@test-functions/jars/hello-function.jar" | jq

curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions/name/reverse/upload \
  -F "file=@test-functions/jars/reverse-function.jar" | jq

curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions/name/sum/upload \
  -F "file=@test-functions/jars/sum-function.jar" | jq
```

### 4.6 Status pruefen (alle sollten READY sein)

```bash
curl -s http://EXTERNAL_IP:8082/api/v1/functions | jq '.[].status'
```

### 4.7 Functions ausfuehren

```bash
# Hello -> "Hello, Peter!"
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"hello","payload":{"name":"Peter"}}' | jq

# Reverse -> "evitaNduolC"
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"reverse","payload":{"text":"CloudNative"}}' | jq

# Sum -> "15"
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"sum","payload":{"numbers":[1,2,3,4,5]}}' | jq

# Sum gross -> "550"
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"sum","payload":{"numbers":[10,20,30,40,50,60,70,80,90,100]}}' | jq
```

### 4.8 Fehlerszenarien

```bash
# Function existiert nicht
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{"functionName":"gibt-es-nicht","payload":{}}' | jq

# Doppelter Name
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"hello","runtime":"java17","handler":"test.Test::handle","memoryMb":256,"timeoutSeconds":30}' | jq

# Ungueltiger Handler
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"bad","runtime":"java17","handler":"InvalidHandler","memoryMb":256,"timeoutSeconds":30}' | jq
```

### 4.9 Function loeschen

```bash
curl -s -X DELETE http://EXTERNAL_IP:8082/api/v1/functions/name/hello | jq
curl -s -X DELETE http://EXTERNAL_IP:8082/api/v1/functions/name/reverse | jq
curl -s -X DELETE http://EXTERNAL_IP:8082/api/v1/functions/name/sum | jq

# Pruefen ob leer
curl -s http://EXTERNAL_IP:8082/api/v1/functions | jq
```

---

## Teil 5: CI/CD mit GitHub Actions

Die Pipeline hat 5 Stages:

```
Build & Test -> Code Quality -> Docker Build -> Integration Test -> Deploy to GKE
```

### GitHub Secrets einrichten

Repository Settings > Secrets and variables > Actions:

| Secret | Wert |
|--------|------|
| `GCP_PROJECT_ID` | `serverless-function-runner` |
| `GCP_SA_KEY` | JSON-Key des Service Accounts (siehe unten) |

### Service Account erstellen

```bash
gcloud iam service-accounts create github-deploy \
  --display-name="GitHub Actions Deploy"

gcloud projects add-iam-policy-binding serverless-function-runner \
  --member="serviceAccount:github-deploy@serverless-function-runner.iam.gserviceaccount.com" \
  --role="roles/container.developer"

gcloud projects add-iam-policy-binding serverless-function-runner \
  --member="serviceAccount:github-deploy@serverless-function-runner.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

gcloud iam service-accounts keys create key.json \
  --iam-account=github-deploy@serverless-function-runner.iam.gserviceaccount.com
```

Den Inhalt von `key.json` als `GCP_SA_KEY` Secret einfuegen, dann `key.json` loeschen.

### Deployment ausloesen

```bash
git add .
git commit -m "Update"
git push origin main
```

Pipeline verfolgen: https://github.com/CC-X5/Serverless-Function-Runner/actions

---

## Teil 6: Google Cloud Console & Praesentations-Links

### Was zeigen bei der Praesentation?

Empfohlene Reihenfolge fuer die Live-Demo:

#### 1. Cluster & Infrastruktur zeigen

**GKE Cluster (Nodes laufen)**
https://console.cloud.google.com/kubernetes/clusters/details/europe-west3/serverless-cluster/details?project=serverless-function-runner

> Hier zeigen: 3 Nodes, Machine Type, Region, Kubernetes Version

**GKE Nodes**
https://console.cloud.google.com/kubernetes/clusters/details/europe-west3/serverless-cluster/nodes?project=serverless-function-runner

> Hier zeigen: CPU/Memory Auslastung der Nodes

#### 2. Workloads & Pods zeigen

**Alle Workloads (Deployments)**
https://console.cloud.google.com/kubernetes/workload/overview?project=serverless-function-runner

> Hier zeigen: 6 Deployments (3 Infra + 3 Services), alle gruen/OK

**Pod Details**
https://console.cloud.google.com/kubernetes/workload/overview?project=serverless-function-runner&pageState=(%22savedViews%22:(%22n%22:%5B%22serverless%22%5D))

> Hier zeigen: Pod Status, Restarts, Container (executor hat 2: app + dind)

#### 3. CI/CD Pipeline zeigen

**GitHub Actions (alle Stages)**
https://github.com/CC-X5/Serverless-Function-Runner/actions

> Hier zeigen: 5 Stages, Build -> Test -> Docker -> Integration -> Deploy

**Cloud Build (Image Builds)**
https://console.cloud.google.com/cloud-build/builds?project=serverless-function-runner

> Hier zeigen: Build History, Build Dauer, Logs

#### 4. Docker Images zeigen

**Artifact Registry (gepushte Images)**
https://console.cloud.google.com/artifacts/docker/serverless-function-runner/europe-west3/serverless-runner?project=serverless-function-runner

> Hier zeigen: 3 Images (registry, executor, gateway), Tags, Image Size

#### 5. Live API Demo im Terminal

```bash
# Status der Pods
kubectl get pods -n serverless

# Services mit External IP
kubectl get svc -n serverless

# Health Check
curl -s http://EXTERNAL_IP:8082/actuator/health | jq

# Function registrieren, JAR hochladen, ausfuehren (siehe Teil 4)
```

> Hier zeigen: Kompletter Flow von Function-Erstellung bis Ausfuehrung

#### 6. Object Storage zeigen

**MinIO Console** (vorher Port-Forward starten):
```bash
kubectl port-forward svc/minio 9001:9001 -n serverless
```
http://localhost:9001 (User: `minioadmin` / Passwort: `minioadmin`)

> Hier zeigen: functions Bucket, hochgeladene JARs, Ordnerstruktur

#### 7. Logs zeigen

```bash
# Executor Logs waehrend Function-Ausfuehrung
kubectl logs -l app=executor-service -n serverless -c executor-service --tail=20
```

> Hier zeigen: Wie der Executor einen Docker-Container startet und das Ergebnis zurueckgibt

#### 8. Automatisches Deployment zeigen (optional)

Kleine Aenderung committen und pushen, dann in GitHub Actions zeigen wie die Pipeline automatisch deployed:

```bash
git add .
git commit -m "Demo commit"
git push origin main
```

> Hier zeigen: Pipeline startet automatisch, alle 5 Stages laufen durch

---

### Alle Console Links auf einen Blick

| Was | Link |
|-----|------|
| Cluster | https://console.cloud.google.com/kubernetes/clusters/details/europe-west3/serverless-cluster/details?project=serverless-function-runner |
| Nodes | https://console.cloud.google.com/kubernetes/clusters/details/europe-west3/serverless-cluster/nodes?project=serverless-function-runner |
| Workloads | https://console.cloud.google.com/kubernetes/workload/overview?project=serverless-function-runner |
| Services | https://console.cloud.google.com/kubernetes/discovery?project=serverless-function-runner |
| Artifact Registry | https://console.cloud.google.com/artifacts/docker/serverless-function-runner/europe-west3/serverless-runner?project=serverless-function-runner |
| Cloud Build | https://console.cloud.google.com/cloud-build/builds?project=serverless-function-runner |
| Billing | https://console.cloud.google.com/billing?project=serverless-function-runner |
| GitHub Actions | https://github.com/CC-X5/Serverless-Function-Runner/actions |
| GitHub Repo | https://github.com/CC-X5/Serverless-Function-Runner |
| MinIO Console | http://localhost:9001 (nach port-forward) |

---

## Teil 7: Logs und Debugging

```bash
# Pod-Status
kubectl get pods -n serverless

# Logs eines Services
kubectl logs -l app=registry-service -n serverless --tail=30
kubectl logs -l app=executor-service -n serverless -c executor-service --tail=30
kubectl logs -l app=gateway-service -n serverless --tail=30

# Pod beschreiben (bei Problemen)
kubectl describe pod -l app=registry-service -n serverless
```

---

## Teil 8: Shutdown

### Nur die App stoppen (Cluster bleibt)

```bash
kubectl delete namespace serverless
```

### Cluster loeschen (spart Kosten)

```bash
gcloud container clusters delete serverless-cluster --region europe-west3 --quiet
```

### Alles loeschen (Projekt weg)

```bash
gcloud projects delete serverless-function-runner --quiet
```

> Nach der Praesentation mindestens den Cluster loeschen!
> 3 Nodes e2-standard-2 erzeugen laufende Kosten.
