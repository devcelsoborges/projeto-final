# Config do deploy (TEMPLATE). Copie para deploy.local.ps1 e preencha com os valores reais.
# deploy.local.ps1 esta no .gitignore (NAO versionado).
$AwsAccountId    = '000000000000'                   # ID da conta AWS
$AwsRegion       = 'us-east-1'
$EcrRepository   = 'brjobs-api'
$ImageTag        = 'latest'
$Ec2Host         = '0.0.0.0'                         # Elastic IP da EC2
$Ec2User         = 'ubuntu'
$Ec2KeyPath      = 'C:\caminho\para\brjobs-key.pem'  # chave .pem de SSH
$SecurityGroupId = 'sg-xxxxxxxxxxxx'                 # SG da EC2 (porta 22 e liberada/revogada no deploy)
