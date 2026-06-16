$ErrorActionPreference = "Stop"

$requiredVars = @("AWS_ACCOUNT_ID", "AWS_REGION", "ECR_REPOSITORY", "IMAGE_TAG")

foreach ($varName in $requiredVars) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($varName))) {
        Write-Error "Defina a variavel de ambiente $varName antes de executar."
    }
}

$awsAccountId = $env:AWS_ACCOUNT_ID
$awsRegion = $env:AWS_REGION
$ecrRepository = $env:ECR_REPOSITORY
$imageTag = $env:IMAGE_TAG
$dockerPlatform = if ([string]::IsNullOrWhiteSpace($env:DOCKER_PLATFORM)) { "linux/amd64" } else { $env:DOCKER_PLATFORM }

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Resolve-Path (Join-Path $scriptDir "..")
$registry = "$awsAccountId.dkr.ecr.$awsRegion.amazonaws.com"
$imageUri = "$registry/$ecrRepository`:$imageTag"
$localImage = "$ecrRepository`:$imageTag"

Write-Host "Login no Amazon ECR: $registry"
aws ecr get-login-password --region $awsRegion | docker login --username AWS --password-stdin $registry

Write-Host "Verificando repositorio ECR: $ecrRepository"
aws ecr describe-repositories --region $awsRegion --repository-names $ecrRepository *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Criando repositorio ECR: $ecrRepository"
    aws ecr create-repository --region $awsRegion --repository-name $ecrRepository *> $null
}

Write-Host "Build da imagem Docker: $localImage"
docker build --platform $dockerPlatform -t $localImage $backendDir

Write-Host "Aplicando tag ECR: $imageUri"
docker tag $localImage $imageUri

Write-Host "Enviando imagem para o ECR"
docker push $imageUri

Write-Host "Imagem publicada: $imageUri"
