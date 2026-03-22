#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PERSISTENT_DIR="$ROOT_DIR/terraform/persistent"

# Get ECR URLs from terraform state
BACKEND_ECR_URL=$(terraform -chdir="$PERSISTENT_DIR" output -raw backend_ecr_url)
KAFKA_ECR_URL=$(terraform -chdir="$PERSISTENT_DIR" output -raw kafka_ecr_url)
MOCK_JWKS_ECR_URL=$(terraform -chdir="$PERSISTENT_DIR" output -raw mock_jwks_ecr_url)

# Extract registry URL (everything before the first /)
REGISTRY=$(echo "$BACKEND_ECR_URL" | cut -d'/' -f1)

echo "Backend ECR:   $BACKEND_ECR_URL"
echo "Kafka ECR:     $KAFKA_ECR_URL"
echo "Mock JWKS ECR: $MOCK_JWKS_ECR_URL"
echo "Registry:      $REGISTRY"

# Login to ECR
aws ecr get-login-password --region us-east-2 --profile fallboot | docker login --username AWS --password-stdin "$REGISTRY"

# Build and push backend
echo "Building backend..."
docker build --platform linux/amd64 -f "$ROOT_DIR/fallboot-backend/Dockerfile" -t "$BACKEND_ECR_URL:latest" "$ROOT_DIR"
echo "Pushing backend..."
docker push "$BACKEND_ECR_URL:latest"

# Build and push kafka consumer
echo "Building kafka consumer..."
docker build --platform linux/amd64 -f "$ROOT_DIR/fallboot-kafka/Dockerfile" -t "$KAFKA_ECR_URL:latest" "$ROOT_DIR"
echo "Pushing kafka consumer..."
docker push "$KAFKA_ECR_URL:latest"

# Build and push mock JWKS server
echo "Building mock JWKS server..."
docker build --platform linux/amd64 -f "$ROOT_DIR/fallboot-mock-jwks/Dockerfile" -t "$MOCK_JWKS_ECR_URL:latest" "$ROOT_DIR/fallboot-mock-jwks"
echo "Pushing mock JWKS server..."
docker push "$MOCK_JWKS_ECR_URL:latest"

echo "Done! All images pushed to ECR."
