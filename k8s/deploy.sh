#!/bin/bash
# ===========================================
# Serverless Function Runner - Minikube Deploy
# ===========================================

set -e

echo "🚀 Deploying Serverless Function Runner to Minikube"

# Check if minikube is running
if ! minikube status | grep -q "Running"; then
    echo "⚠️  Minikube is not running. Starting..."
    minikube start --driver=docker --memory=4096
fi

# Use minikube's Docker daemon
echo "🐳 Configuring Docker environment..."
eval $(minikube docker-env)

# Build Docker images locally
echo "🔨 Building Docker images..."
cd ../serverless

docker build -t registry-service:latest ./registry-service
docker build -t executor-service:latest ./executor-service
docker build -t gateway-service:latest ./gateway-service

cd ../k8s

# Apply Kubernetes manifests
echo "📦 Applying Kubernetes manifests..."
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-configmap.yaml
kubectl apply -f 02-secrets.yaml

echo "⏳ Waiting for namespace..."
sleep 2

kubectl apply -f 10-postgres.yaml
kubectl apply -f 11-minio.yaml
kubectl apply -f 12-rabbitmq.yaml

echo "⏳ Waiting for infrastructure (60s)..."
kubectl wait --for=condition=ready pod -l app=postgres -n serverless --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=minio -n serverless --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=rabbitmq -n serverless --timeout=120s || true

kubectl apply -f 20-registry-service.yaml
kubectl apply -f 21-executor-service.yaml
kubectl apply -f 22-gateway-service.yaml

echo "⏳ Waiting for services to be ready..."
kubectl wait --for=condition=ready pod -l app=registry-service -n serverless --timeout=180s || true
kubectl wait --for=condition=ready pod -l app=executor-service -n serverless --timeout=180s || true
kubectl wait --for=condition=ready pod -l app=gateway-service -n serverless --timeout=180s || true

# Show status
echo ""
echo "✅ Deployment complete!"
echo ""
echo "📊 Pod Status:"
kubectl get pods -n serverless

echo ""
echo "🌐 Services:"
kubectl get services -n serverless

echo ""
echo "🔗 Access Gateway:"
echo "   URL: $(minikube service gateway-service -n serverless --url)"
echo ""
echo "📖 Swagger UI: <gateway-url>/swagger-ui.html"
echo ""
