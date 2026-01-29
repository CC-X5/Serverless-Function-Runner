# Quickstart - Serverless Function Runner

Vollstaendige Anleitung zum Aufsetzen und Demonstrieren des Projekts auf Google Kubernetes Engine.

> **Hinweis:** Alle Befehle sind fuer Windows CMD optimiert. Jeder Befehl ist einzeln kopierbar.

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

## Voraussetzungen

| Tool | Installation |
|------|-------------|
| gcloud CLI | https://cloud.google.com/sdk/docs/install |
| Java 17+ (JDK) | https://adoptium.net/ |
| Maven | `choco install maven -y` (CMD als Administrator) |
| jq | `winget install jqlang.jq` |

---

## Teil 1: GCP Setup (einmalig)

### 1.1 Authentifizieren und Projekt erstellen

```
gcloud auth login
```

```
gcloud projects create serverless-function-runner --name="Serverless Function Runner"
```

```
gcloud config set project serverless-function-runner
```

```
gcloud config set compute/region europe-west3
```

### 1.2 Billing verknuepfen

```
gcloud billing accounts list
```

```
gcloud billing projects link serverless-function-runner --billing-account=DEINE_BILLING_ACCOUNT_ID
```

### 1.3 APIs aktivieren

```
gcloud services enable container.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com
```

### 1.4 Artifact Registry erstellen

```
gcloud artifacts repositories create serverless-runner --repository-format=docker --location=europe-west3
```

### 1.5 GKE Cluster erstellen

```
gcloud container clusters create serverless-cluster --region europe-west3 --num-nodes 1 --machine-type e2-standard-2 --disk-size 30 --enable-ip-alias
```

> GKE Standard (nicht Autopilot) ist noetig, da der Executor-Service einen privilegierten DinD-Container benoetigt.

### 1.6 Plugins installieren

```
gcloud components install gke-gcloud-auth-plugin
```

```
gcloud components install kubectl
```

> Nach der Installation CMD neu starten, damit die Befehle verfuegbar sind.

### 1.7 kubectl verbinden

```
gcloud container clusters get-credentials serverless-cluster --region europe-west3
```

Verbindung testen:

```
kubectl get nodes
```

---

## Teil 2: Docker Images bauen und pushen

```
cd serverless
```

```
gcloud builds submit --config=cloudbuild.yaml .
```

Baut alle 3 Services und pusht sie nach Google Artifact Registry.

---

## Teil 3: Deployment

### Option A: Mit Script (Windows CMD)

```
cd k8s
```

```
deploy-gke.bat
```

### Option B: Manuell Schritt fuer Schritt

```
cd k8s
```

Namespace und Konfiguration:

```
kubectl apply -f 00-namespace.yaml
```

```
kubectl apply -f 01-configmap.yaml
```

```
kubectl apply -f 02-secrets.yaml
```

Infrastruktur:

```
kubectl apply -f 10-postgres.yaml
```

```
kubectl apply -f 11-minio.yaml
```

```
kubectl apply -f 12-rabbitmq.yaml
```

Warten bis Infrastruktur bereit ist:

```
kubectl wait --for=condition=ready pod -l app=postgres -n serverless --timeout=120s
```

```
kubectl wait --for=condition=ready pod -l app=minio -n serverless --timeout=120s
```

Application Services:

```
kubectl apply -f 20-registry-service.yaml
```

```
kubectl apply -f 21-executor-service.yaml
```

```
kubectl apply -f 22-gateway-service.yaml
```

Warten bis Services bereit sind:

```
kubectl wait --for=condition=ready pod -l app=registry-service -n serverless --timeout=240s
```

```
kubectl wait --for=condition=ready pod -l app=executor-service -n serverless --timeout=240s
```

```
kubectl wait --for=condition=ready pod -l app=gateway-service -n serverless --timeout=240s
```

### Option C: Automatisch via GitHub Actions

Bei jedem Push auf `main` wird automatisch gebaut, getestet und deployed.

Voraussetzung: GitHub Secrets konfiguriert (siehe Teil 5).

```
git push origin main
```

---

## Teil 3.5: Nach dem Deployment

### DinD Docker Image vorladen

Der Executor braucht das Java-Image im DinD-Container. Ohne diesen Schritt schlaegt die erste Ausfuehrung fehl.

Zuerst den Pod-Namen ermitteln:

```
kubectl get pods -n serverless -l app=executor-service
```

Dann das Image pullen (POD_NAME ersetzen):

```
kubectl exec -n serverless POD_NAME -c dind -- docker pull eclipse-temurin:17-jre-alpine
```

### Status pruefen

```
kubectl get pods -n serverless
```

```
kubectl get svc -n serverless
```

> Die EXTERNAL-IP vom `gateway-service` ist die oeffentliche URL. Diese IP fuer alle folgenden Befehle als `EXTERNAL_IP` verwenden.

### Erreichbare Seiten

| Seite | URL | Zugang |
|-------|-----|--------|
| Health Check | http://EXTERNAL_IP:8082/actuator/health | Direkt im Browser |
| Swagger UI (API-Doku) | http://EXTERNAL_IP:8082/swagger-ui.html | Direkt im Browser |
| Functions API | http://EXTERNAL_IP:8082/api/v1/functions | Direkt im Browser |
| MinIO Console | http://localhost:9001 | Port-Forward noetig (siehe Teil 6) |
| RabbitMQ Console | http://localhost:15672 | Port-Forward noetig (siehe Teil 6) |

---

## Teil 4: Test-Functions bauen

```
cd serverless\test-functions
```

```
mvn clean package -q
```

```
mkdir jars
```

```
copy helloF\target\hello-function.jar jars\
```

```
copy reverseF\target\reverse-function.jar jars\
```

```
copy sumF\target\sum-function.jar jars\
```

Oder mit dem Build-Script:

```
build.bat
```

Danach liegen 3 JARs in `serverless\test-functions\jars\`:
- `hello-function.jar`
- `reverse-function.jar`
- `sum-function.jar`

---

## Teil 5: Live Demo

> **Hinweis:** `EXTERNAL_IP` in allen Befehlen durch die tatsaechliche IP ersetzen (siehe `kubectl get svc -n serverless`).
>
> Die `demo.bat` im Ordner `serverless\test-functions` fuehrt alle Schritte (5.2 - 5.6) automatisch aus.

### 5.1 Health Check

```
curl -s http://EXTERNAL_IP:8082/actuator/health
```

### 5.2 Functions registrieren

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions -H "Content-Type: application/json" -d "{\"name\":\"hello\",\"runtime\":\"java17\",\"handler\":\"hskl.cn.serverless.function.HelloFunction::handle\",\"memoryMb\":256,\"timeoutSeconds\":30,\"description\":\"Begruessung\"}"
```

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions -H "Content-Type: application/json" -d "{\"name\":\"reverse\",\"runtime\":\"java17\",\"handler\":\"hskl.cn.serverless.function.ReverseFunction::handle\",\"memoryMb\":256,\"timeoutSeconds\":30,\"description\":\"String umkehren\"}"
```

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions -H "Content-Type: application/json" -d "{\"name\":\"sum\",\"runtime\":\"java17\",\"handler\":\"hskl.cn.serverless.function.SumFunction::handle\",\"memoryMb\":256,\"timeoutSeconds\":30,\"description\":\"Zahlen summieren\"}"
```

### 5.3 Alle Functions auflisten

```
curl -s http://EXTERNAL_IP:8082/api/v1/functions
```

### 5.4 JARs hochladen

Aus dem `serverless\test-functions` Verzeichnis:

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions/name/hello/upload -F "file=@jars/hello-function.jar"
```

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions/name/reverse/upload -F "file=@jars/reverse-function.jar"
```

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions/name/sum/upload -F "file=@jars/sum-function.jar"
```

### 5.5 Status pruefen (alle sollten READY sein)

```
curl -s http://EXTERNAL_IP:8082/api/v1/functions
```

### 5.6 Functions ausfuehren

Hello -> "Hello, Peter!"

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute -H "Content-Type: application/json" -d "{\"functionName\":\"hello\",\"payload\":{\"name\":\"Peter\"}}"
```

Reverse -> "evitaNduolC"

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute -H "Content-Type: application/json" -d "{\"functionName\":\"reverse\",\"payload\":{\"text\":\"CloudNative\"}}"
```

Sum -> "15"

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute -H "Content-Type: application/json" -d "{\"functionName\":\"sum\",\"payload\":{\"numbers\":[1,2,3,4,5]}}"
```

Sum gross -> "550"

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute -H "Content-Type: application/json" -d "{\"functionName\":\"sum\",\"payload\":{\"numbers\":[10,20,30,40,50,60,70,80,90,100]}}"
```

### 5.7 Fehlerszenarien

Function existiert nicht:

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/execute -H "Content-Type: application/json" -d "{\"functionName\":\"gibt-es-nicht\",\"payload\":{}}"
```

Doppelter Name:

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions -H "Content-Type: application/json" -d "{\"name\":\"hello\",\"runtime\":\"java17\",\"handler\":\"test.Test::handle\",\"memoryMb\":256,\"timeoutSeconds\":30}"
```

Ungueltiger Handler:

```
curl -s -X POST http://EXTERNAL_IP:8082/api/v1/functions -H "Content-Type: application/json" -d "{\"name\":\"bad\",\"runtime\":\"java17\",\"handler\":\"InvalidHandler\",\"memoryMb\":256,\"timeoutSeconds\":30}"
```

### 5.8 Functions loeschen

```
curl -s -X DELETE http://EXTERNAL_IP:8082/api/v1/functions/name/hello
```

```
curl -s -X DELETE http://EXTERNAL_IP:8082/api/v1/functions/name/reverse
```

```
curl -s -X DELETE http://EXTERNAL_IP:8082/api/v1/functions/name/sum
```

Pruefen ob leer:

```
curl -s http://EXTERNAL_IP:8082/api/v1/functions
```

---

## Teil 6: Web-Consolen

### MinIO Console (Object Storage)

Port-Forward starten (laeuft im Vordergrund, eigenes CMD-Fenster oeffnen):

```
kubectl port-forward svc/minio 9001:9001 -n serverless
```

Dann im Browser: http://localhost:9001

| Feld | Wert |
|------|------|
| Username | `minioadmin` |
| Password | `minioadmin` |

> Hier sichtbar: `functions` Bucket mit den hochgeladenen JARs

### RabbitMQ Console (Message Queue)

Port-Forward starten (eigenes CMD-Fenster):

```
kubectl port-forward svc/rabbitmq 15672:15672 -n serverless
```

Dann im Browser: http://localhost:15672

| Feld | Wert |
|------|------|
| Username | `guest` |
| Password | `guest` |

### Swagger UI (API-Dokumentation)

Direkt im Browser (keine Port-Forward noetig):

```
http://EXTERNAL_IP:8082/swagger-ui.html
```

---

## Teil 7: CI/CD mit GitHub Actions

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

```
gcloud iam service-accounts create github-deploy --display-name="GitHub Actions Deploy"
```

```
gcloud projects add-iam-policy-binding serverless-function-runner --member="serviceAccount:github-deploy@serverless-function-runner.iam.gserviceaccount.com" --role="roles/container.developer"
```

```
gcloud projects add-iam-policy-binding serverless-function-runner --member="serviceAccount:github-deploy@serverless-function-runner.iam.gserviceaccount.com" --role="roles/artifactregistry.writer"
```

```
gcloud iam service-accounts keys create key.json --iam-account=github-deploy@serverless-function-runner.iam.gserviceaccount.com
```

Den Inhalt von `key.json` als `GCP_SA_KEY` Secret einfuegen, dann `key.json` loeschen.

### Deployment ausloesen

```
git add .
```

```
git commit -m "Update"
```

```
git push origin main
```

Pipeline verfolgen: https://github.com/CC-X5/Serverless-Function-Runner/actions

---

## Teil 8: Logs und Debugging

```
kubectl get pods -n serverless
```

```
kubectl logs -l app=registry-service -n serverless --tail=30
```

```
kubectl logs -l app=executor-service -n serverless -c executor-service --tail=30
```

```
kubectl logs -l app=gateway-service -n serverless --tail=30
```

```
kubectl describe pod -l app=registry-service -n serverless
```

---

## Teil 9: Google Cloud Console Links

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

---

## Teil 10: Shutdown

Nur die App stoppen (Cluster bleibt):

```
kubectl delete namespace serverless
```

Cluster loeschen (spart Kosten):

```
gcloud container clusters delete serverless-cluster --region europe-west3 --quiet
```

Alles loeschen (Projekt weg):

```
gcloud projects delete serverless-function-runner --quiet
```

> Nach der Praesentation mindestens den Cluster loeschen! 3 Nodes e2-standard-2 erzeugen laufende Kosten.
