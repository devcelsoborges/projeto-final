#!/usr/bin/env bash
set -euo pipefail

required_vars=(AWS_ACCOUNT_ID AWS_REGION ECR_REPOSITORY IMAGE_TAG)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Erro: defina a variavel $var_name antes de executar."
    exit 1
  fi
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REGISTRY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
IMAGE_URI="$REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG"
LOCAL_IMAGE="$ECR_REPOSITORY:$IMAGE_TAG"
DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/amd64}"

echo "Login no Amazon ECR: $REGISTRY"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"

echo "Verificando repositorio ECR: $ECR_REPOSITORY"
if ! aws ecr describe-repositories \
  --region "$AWS_REGION" \
  --repository-names "$ECR_REPOSITORY" >/dev/null 2>&1; then
  echo "Criando repositorio ECR: $ECR_REPOSITORY"
  aws ecr create-repository \
    --region "$AWS_REGION" \
    --repository-name "$ECR_REPOSITORY" >/dev/null
fi

echo "Build da imagem Docker: $LOCAL_IMAGE"
docker build --platform "$DOCKER_PLATFORM" -t "$LOCAL_IMAGE" "$BACKEND_DIR"

echo "Aplicando tag ECR: $IMAGE_URI"
docker tag "$LOCAL_IMAGE" "$IMAGE_URI"

echo "Enviando imagem para o ECR"
docker push "$IMAGE_URI"

echo "Imagem publicada: $IMAGE_URI"
