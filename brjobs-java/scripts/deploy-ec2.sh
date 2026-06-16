#!/usr/bin/env bash
set -euo pipefail

required_vars=(AWS_ACCOUNT_ID AWS_REGION ECR_REPOSITORY IMAGE_TAG)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Erro: defina a variavel $var_name antes de executar."
    exit 1
  fi
done

REGISTRY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

echo "Login no Amazon ECR: $REGISTRY"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"

echo "Baixando imagem da API"
docker compose -f docker-compose.prod.yml pull api

echo "Subindo somente a API"
docker compose -f docker-compose.prod.yml up -d api

echo "Containers em execucao"
docker ps
