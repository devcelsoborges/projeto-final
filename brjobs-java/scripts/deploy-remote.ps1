#Requires -Version 5
<#
.SYNOPSIS
  Fase B do deploy: atualiza o container do backend na EC2 (pull + up) a partir da sua maquina.
.DESCRIPTION
  Como o SSH e bloqueado pelo Security Group (IP dinamico), o script:
    1) libera a porta 22 do SG para o seu IP publico atual (temporario),
    2) conecta por SSH e roda o pull/up na EC2,
    3) revoga a liberacao no final (try/finally), mesmo se der erro.
  Config em scripts/deploy.local.ps1 (copie de deploy.local.example.ps1).
#>
$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$cfg = Join-Path $scriptDir 'deploy.local.ps1'
if (-not (Test-Path $cfg)) { throw "Falta $cfg. Copie de deploy.local.example.ps1 e preencha." }
. $cfg

foreach ($v in 'AwsAccountId','AwsRegion','EcrRepository','Ec2Host','Ec2User','Ec2KeyPath','SecurityGroupId') {
  if (-not (Get-Variable -Name $v -ValueOnly -ErrorAction SilentlyContinue)) { throw "Config faltando em deploy.local.ps1: `$$v" }
}
if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) { throw "ssh.exe nao encontrado. Instale o OpenSSH Client do Windows." }
if (-not (Test-Path $Ec2KeyPath)) { throw "Chave SSH nao encontrada: $Ec2KeyPath" }

$registry = "$AwsAccountId.dkr.ecr.$AwsRegion.amazonaws.com"

# IP publico atual -> /32
$myIp = (Invoke-RestMethod -Uri 'https://api.ipify.org?format=json' -TimeoutSec 15).ip
$cidr = "$myIp/32"

Write-Host "[1/4] Liberando porta 22 do SG $SecurityGroupId para $cidr (temporario)..." -ForegroundColor Cyan
aws ec2 authorize-security-group-ingress --group-id $SecurityGroupId --protocol tcp --port 22 --cidr $cidr --region $AwsRegion *> $null
$addedRule = ($LASTEXITCODE -eq 0)  # se ja existia (Duplicate) nao revoga no final
if ($addedRule) { Write-Host "      regra adicionada." } else { Write-Host "      regra ja existia (sera mantida)." }

# ssh.exe recusa a chave se ela for acessivel por outros usuarios.
# Usa o SID do usuario (evita ambiguidade quando o nome do usuario == nome da maquina)
# e zera ACEs explicitas/herdadas, deixando so o usuario atual.
$mySid = ([System.Security.Principal.WindowsIdentity]::GetCurrent()).User.Value
icacls $Ec2KeyPath /reset *> $null
icacls $Ec2KeyPath /inheritance:r /grant:r "*${mySid}:R" *> $null

$remoteCmd = @"
set -e
cd /home/$Ec2User/brjobs-api
aws ecr get-login-password --region $AwsRegion | sudo docker login --username AWS --password-stdin $registry
echo '--- liberando espaco (remove imagens/cache nao usados; mantem container rodando e volumes) ---'
sudo docker system prune -af
df -h / | tail -1
sudo docker compose --env-file .env.prod -f docker-compose.prod.yml pull api
sudo docker compose --env-file .env.prod -f docker-compose.prod.yml up -d api
echo '--- imagem rodando ---'
sudo docker inspect brjobs-api --format 'Running={{.Image}}'
sudo docker compose --env-file .env.prod -f docker-compose.prod.yml ps
"@

try {
  Write-Host "[2/4] Conectando em $Ec2User@$Ec2Host e atualizando o container..." -ForegroundColor Cyan
  $remoteCmd = $remoteCmd -replace "`r", ""   # CRLF do Windows -> LF (o bash do Linux nao lida com CR)
  $remoteCmd | ssh -T -o BatchMode=yes -o StrictHostKeyChecking=accept-new -i $Ec2KeyPath "$Ec2User@$Ec2Host" "bash -s"
  if ($LASTEXITCODE -ne 0) { throw "Deploy remoto falhou (exit $LASTEXITCODE)." }
  Write-Host "[3/4] Deploy aplicado com sucesso." -ForegroundColor Green
}
finally {
  if ($addedRule) {
    Write-Host "[4/4] Revogando porta 22 do SG ($cidr)..." -ForegroundColor Cyan
    aws ec2 revoke-security-group-ingress --group-id $SecurityGroupId --protocol tcp --port 22 --cidr $cidr --region $AwsRegion *> $null
    if ($LASTEXITCODE -eq 0) { Write-Host "      regra revogada." } else { Write-Warning "Nao consegui revogar a regra ($cidr). Remova manualmente se necessario." }
  }
}
