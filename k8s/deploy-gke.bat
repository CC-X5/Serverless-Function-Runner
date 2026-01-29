@echo off
echo === Serverless Function Runner - GKE Deployment ===
echo.

echo [1/8] Pre-flight checks...
where gcloud >nul 2>nul || (echo ERROR: gcloud CLI not found. && exit /b 1)
where kubectl >nul 2>nul || (echo ERROR: kubectl not found. && exit /b 1)

set PROJECT_ID=serverless-function-runner
set REGION=europe-west3
set CLUSTER_NAME=serverless-cluster
set NAMESPACE=serverless

echo   Project: %PROJECT_ID%
echo   Region:  %REGION%
echo   Cluster: %CLUSTER_NAME%
echo.

echo [2/8] Connecting to GKE cluster...
call gcloud container clusters get-credentials %CLUSTER_NAME% --region %REGION%
echo.

echo [3/8] Applying namespace and configuration...
call kubectl apply -f 00-namespace.yaml
call kubectl apply -f 01-configmap.yaml
call kubectl apply -f 02-secrets.yaml
timeout /t 2 >nul
echo.

echo [4/8] Deploying infrastructure (PostgreSQL, MinIO, RabbitMQ)...
call kubectl apply -f 10-postgres.yaml
call kubectl apply -f 11-minio.yaml
call kubectl apply -f 12-rabbitmq.yaml
echo.

echo [5/8] Waiting for infrastructure to be ready...
call kubectl wait --for=condition=ready pod -l app=postgres -n %NAMESPACE% --timeout=120s
call kubectl wait --for=condition=ready pod -l app=minio -n %NAMESPACE% --timeout=120s
call kubectl wait --for=condition=ready pod -l app=rabbitmq -n %NAMESPACE% --timeout=120s
echo.

echo [6/8] Deploying application services...
call kubectl apply -f 20-registry-service.yaml
call kubectl apply -f 21-executor-service.yaml
call kubectl apply -f 22-gateway-service.yaml
echo.

echo [7/8] Waiting for application services to be ready...
call kubectl wait --for=condition=ready pod -l app=registry-service -n %NAMESPACE% --timeout=180s
call kubectl wait --for=condition=ready pod -l app=executor-service -n %NAMESPACE% --timeout=180s
call kubectl wait --for=condition=ready pod -l app=gateway-service -n %NAMESPACE% --timeout=180s
echo.

echo [8/8] Deployment status
echo.
echo --- Pod Status ---
call kubectl get pods -n %NAMESPACE%
echo.
echo --- Services ---
call kubectl get services -n %NAMESPACE%
echo.
echo === Deployment Complete ===
