#!/bin/bash
# ===========================================
# Serverless Function Runner - GKE Deploy
# ===========================================
# Project:  serverless-function-runner
# Region:   europe-west3 (Frankfurt)
# Cluster:  serverless-cluster
# ===========================================

set -e

PROJECT_ID="serverless-function-runner"
REGION="europe-west3"
CLUSTER_NAME="serverless-cluster"
NAMESPACE="serverless"

echo "=== Serverless Function Runner - GKE Deployment ==="
echo ""

# --- Pre-flight checks ---
echo "[1/8] Pre-flight checks..."

if ! command -v gcloud &> /dev/null; then
    echo "ERROR: gcloud CLI not found. Install from https://cloud.google.com/sdk/docs/install"
    exit 1
fi

if ! command -v kubectl &> /dev/null; then
    echo "ERROR: kubectl not found. Install with: gcloud components install kubectl"
    exit 1
fi

CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null)
if [ "$CURRENT_PROJECT" != "$PROJECT_ID" ]; then
    echo "WARNING: Current project is '$CURRENT_PROJECT', expected '$PROJECT_ID'"
    echo "Setting project..."
    gcloud config set project "$PROJECT_ID"
fi

echo "  Project: $PROJECT_ID"
echo "  Region:  $REGION"
echo "  Cluster: $CLUSTER_NAME"
echo ""

# --- Ensure cluster credentials ---
echo "[2/8] Connecting to GKE cluster..."
gcloud container clusters get-credentials "$CLUSTER_NAME" --region "$REGION"
echo ""

# --- Apply namespace and config ---
echo "[3/8] Applying namespace and configuration..."
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-configmap.yaml
kubectl apply -f 02-secrets.yaml
sleep 2
echo ""

# --- Deploy infrastructure ---
echo "[4/8] Deploying infrastructure (PostgreSQL, MinIO, RabbitMQ)..."
kubectl apply -f 10-postgres.yaml
kubectl apply -f 11-minio.yaml
kubectl apply -f 12-rabbitmq.yaml
echo ""

# --- Wait for infrastructure ---
echo "[5/8] Waiting for infrastructure to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=minio -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=rabbitmq -n "$NAMESPACE" --timeout=120s || true
echo ""

# --- Deploy application services ---
echo "[6/8] Deploying application services..."
kubectl apply -f 20-registry-service.yaml
kubectl apply -f 21-executor-service.yaml
kubectl apply -f 22-gateway-service.yaml
echo ""

# --- Wait for application services ---
echo "[7/8] Waiting for application services to be ready..."
kubectl wait --for=condition=ready pod -l app=registry-service -n "$NAMESPACE" --timeout=180s || true
kubectl wait --for=condition=ready pod -l app=executor-service -n "$NAMESPACE" --timeout=180s || true
kubectl wait --for=condition=ready pod -l app=gateway-service -n "$NAMESPACE" --timeout=180s || true
echo ""

# --- Show status ---
echo "[8/8] Deployment status"
echo ""
echo "--- Pod Status ---"
kubectl get pods -n "$NAMESPACE"
echo ""
echo "--- Services ---"
kubectl get services -n "$NAMESPACE"
echo ""

# Get gateway external IP
EXTERNAL_IP=$(kubectl get svc gateway-service -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
if [ -n "$EXTERNAL_IP" ]; then
    echo "=== Deployment Complete ==="
    echo ""
    echo "  Gateway URL:  http://$EXTERNAL_IP:8082"
    echo "  Health Check: http://$EXTERNAL_IP:8082/actuator/health"
    echo "  Functions:    http://$EXTERNAL_IP:8082/api/v1/functions"
    echo "  Swagger UI:   http://$EXTERNAL_IP:8082/swagger-ui.html"
else
    echo "=== Deployment Complete ==="
    echo ""
    echo "  LoadBalancer IP is still being provisioned."
    echo "  Run this to check:"
    echo "    kubectl get svc gateway-service -n $NAMESPACE"
fi
echo ""
