#Requires -Version 5
<#
.SYNOPSIS
  Deploy completo do backend: Fase A (build + push no ECR) e Fase B (atualiza a EC2).
.DESCRIPTION
  Orquestra build-and-push.ps1 (Fase A) e deploy-remote.ps1 (Fase B).
  Config em scripts/deploy.local.ps1.
  Uso: .\scripts\release.ps1
#>
$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$cfg = Join-Path $scriptDir 'deploy.local.ps1'
if (-not (Test-Path $cfg)) { throw "Falta $cfg. Copie de deploy.local.example.ps1 e preencha." }
. $cfg

Write-Host "===== FASE A: build + push (ECR) =====" -ForegroundColor Magenta
$env:AWS_ACCOUNT_ID = $AwsAccountId
$env:AWS_REGION     = $AwsRegion
$env:ECR_REPOSITORY = $EcrRepository
$env:IMAGE_TAG      = $ImageTag
& (Join-Path $scriptDir 'build-and-push.ps1')
if ($LASTEXITCODE -ne 0) { throw "Fase A (build/push) falhou (exit $LASTEXITCODE)." }

Write-Host "`n===== FASE B: deploy na EC2 =====" -ForegroundColor Magenta
& (Join-Path $scriptDir 'deploy-remote.ps1')
if ($LASTEXITCODE -ne 0) { throw "Fase B (EC2) falhou (exit $LASTEXITCODE)." }

Write-Host "`n===== Deploy completo! =====" -ForegroundColor Green
