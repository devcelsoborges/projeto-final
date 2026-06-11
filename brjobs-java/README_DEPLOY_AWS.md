# Deploy AWS EC2 com Docker e ECR

Este backend e uma API Java/Spring Boot 3.3.5 com Maven, Java 17, Spring Data JPA e PostgreSQL. A API sobe na porta `8080`.

Este fluxo usa apenas Docker, Docker Compose, Amazon ECR e uma EC2 Ubuntu ja existente. Ele nao cria RDS, ECS, Load Balancer, NAT Gateway, Route 53, nem expoe PostgreSQL na internet.

## Arquivos

- `Dockerfile`: build multi-stage Maven e imagem final com JRE 17.
- `.dockerignore`: remove `target`, IDEs, logs, envs e caches do contexto Docker.
- `docker-compose.prod.yml`: sobe somente o servico `api`.
- `.env.prod.example`: exemplo para copiar para `.env.prod` na EC2.
- `.env.test-ec2.example`: exemplo temporario para teste/MVP na EC2.
- `scripts/build-and-push.sh`: build e push para ECR em Linux/macOS.
- `scripts/build-and-push.ps1`: build e push para ECR em Windows PowerShell.
- `scripts/deploy-ec2.sh`: pull e deploy da API na EC2.

## Variaveis locais para build/push

Configure no terminal local:

```bash
export AWS_ACCOUNT_ID="SEU_ACCOUNT_ID"
export AWS_REGION="us-east-1"
export ECR_REPOSITORY="brjobs-api"
export IMAGE_TAG="latest"
```

No PowerShell:

```powershell
$env:AWS_ACCOUNT_ID="SEU_ACCOUNT_ID"
$env:AWS_REGION="us-east-1"
$env:ECR_REPOSITORY="brjobs-api"
$env:IMAGE_TAG="latest"
```

## Criar repositorio ECR

O script cria o repositorio automaticamente se ele nao existir. Para criar manualmente:

```bash
aws ecr create-repository \
  --region "$AWS_REGION" \
  --repository-name "$ECR_REPOSITORY"
```

## Build e push

Linux/macOS:

```bash
chmod +x scripts/build-and-push.sh
./scripts/build-and-push.sh
```

Windows PowerShell:

```powershell
.\scripts\build-and-push.ps1
```

Por padrao o build usa `linux/amd64`. Para mudar:

```bash
export DOCKER_PLATFORM="linux/arm64"
```

## Configurar a EC2

Copie os arquivos do backend para `~/brjobs` ou use o clone do repositorio na EC2. Depois:

```bash
cd ~/brjobs/brjobs-java
cp .env.prod.example .env.prod
```

Edite `.env.prod` antes de subir. Os valores de banco preenchidos no exemplo sao apenas para teste/MVP temporario:

```env
DB_HOST=brjobs-postgres
DB_PORT=5432
DB_NAME=brjobs
DB_USER=brjobs_user
DB_PASSWORD=brjobs_senha_forte_123
SPRING_DATASOURCE_URL=jdbc:postgresql://brjobs-postgres:5432/brjobs
SPRING_DATASOURCE_USERNAME=brjobs_user
SPRING_DATASOURCE_PASSWORD=brjobs_senha_forte_123
```

Troque essa senha antes de producao real. O arquivo `.env.prod` nao deve ser commitado.

Garanta que a rede Docker externa exista:

```bash
docker network ls | grep brjobs-net || docker network create brjobs-net
```

O container PostgreSQL existente deve estar na mesma rede e acessivel pelo nome `brjobs-postgres`. O `docker-compose.prod.yml` nao cria banco e nao expoe a porta `5432`.

## Deploy na EC2

Na EC2, configure as variaveis:

```bash
export AWS_ACCOUNT_ID="SEU_ACCOUNT_ID"
export AWS_REGION="us-east-1"
export ECR_REPOSITORY="brjobs-api"
export IMAGE_TAG="latest"
```

Execute:

```bash
chmod +x scripts/deploy-ec2.sh
./scripts/deploy-ec2.sh
```

## Verificar logs

```bash
docker compose -f docker-compose.prod.yml logs -f api
```

## Parar somente a API

```bash
docker compose -f docker-compose.prod.yml stop api
```

## Variaveis que precisam ser preenchidas manualmente

- `AWS_ACCOUNT_ID`
- `AWS_REGION`
- `ECR_REPOSITORY`
- `IMAGE_TAG`
- `BRJOBS_JWT_SECRET`
- `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET`, se login Google for usado
- `FACEBOOK_APP_ID` e `FACEBOOK_APP_SECRET`, se login Facebook for usado
- `APPLE_CLIENT_ID`, `APPLE_TEAM_ID` e `APPLE_KEY_ID`, se login Apple for usado
- `STRIPE_SECRET_KEY` e `HIGHLIGHT_WEBHOOK_SECRET`, se pagamentos/destaques forem usados
- `DB_PASSWORD`/`SPRING_DATASOURCE_PASSWORD`, antes de producao real

## Cuidados

- Nao commite `.env.prod`.
- Nao coloque AWS secrets, tokens ou senhas reais no codigo.
- Nao exponha `5432` no Security Group nem no Docker Compose.
- Nao rode `docker compose down -v` neste fluxo, para nao remover volumes.
