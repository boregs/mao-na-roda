# Requer Docker Desktop (Linux containers), Compose v2 e JDK 25.
# Somente banco efêmero local. Nenhum parâmetro permite apontar para Supabase.
$ErrorActionPreference = 'Stop'
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker não encontrado. Instale e inicie Docker Desktop para executar o teste real.'
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME/bin/javac.exe")) {
    throw 'Configure JAVA_HOME para o JDK 25 antes de executar.'
}
$apiDir = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $PSScriptRoot 'compose.test.yml'
$project = 'mnr-jdbc-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$names = @('MNR_TEST_ADMIN_PASSWORD','MNR_TEST_API_PASSWORD','MNR_TEST_JDBC_URL',
    'FLYWAY_URL','FLYWAY_USER','FLYWAY_PASSWORD')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
function Confirmar-Comando([string]$etapa) {
    if ($LASTEXITCODE -ne 0) { throw "Falha na etapa: $etapa (código $LASTEXITCODE)." }
}
Push-Location $apiDir
try {
    $env:MNR_TEST_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
    $env:MNR_TEST_API_PASSWORD = [guid]::NewGuid().ToString('N')
    $env:MNR_TEST_JDBC_URL = 'jdbc:postgresql://127.0.0.1:55432/maonaroda_jdbc_test'
    & docker compose -f $composeFile -p $project up -d --wait --wait-timeout 120
    Confirmar-Comando 'iniciar banco local'
    # template0 evita herdar PostGIS em public da imagem; a migration o instala em extensions.
    'CREATE DATABASE maonaroda_jdbc_test TEMPLATE template0;' |
        & docker compose -f $composeFile -p $project exec -T db psql -X -v ON_ERROR_STOP=1 -U postgres -d postgres
    Confirmar-Comando 'criar banco descartável'
    & docker compose -f $composeFile -p $project exec -T db psql -X -U postgres -d maonaroda_jdbc_test -f /tests/bootstrap-local.sql
    Confirmar-Comando 'preparar fixture Auth'
    $env:FLYWAY_URL = $env:MNR_TEST_JDBC_URL
    $env:FLYWAY_USER = 'postgres'
    $env:FLYWAY_PASSWORD = $env:MNR_TEST_ADMIN_PASSWORD
    & ./mvnw.cmd -B -ntp flyway:migrate flyway:validate
    Confirmar-Comando 'migrations e checksums'
    # Login exclusivo de testes; senha aleatória hexadecimal, nunca persistida em arquivo.
    "CREATE ROLE maonaroda_jdbc_test_api LOGIN INHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS PASSWORD '$env:MNR_TEST_API_PASSWORD'; GRANT maonaroda_api TO maonaroda_jdbc_test_api;" |
        & docker compose -f $composeFile -p $project exec -T db psql -X -v ON_ERROR_STOP=1 -U postgres -d maonaroda_jdbc_test
    Confirmar-Comando 'provisionar login restrito'
    & docker compose -f $composeFile -p $project exec -T db psql -X -U postgres -d maonaroda_jdbc_test -f /tests/integridade.sql
    Confirmar-Comando 'integridade e privilégios SQL'
    & ./mvnw.cmd -B -ntp '-Dtest=UsuarioPostgresTests,UsuarioRepositoryTests,UsuarioControllerTests' test
    Confirmar-Comando 'testes JDBC e controller'
    & ./mvnw.cmd -B -ntp flyway:migrate flyway:validate
    Confirmar-Comando 'reaplicação sem alterações'
    Write-Host 'Verificação de banco concluída. Consulte target/surefire-reports.'
} finally {
    # Remove somente o projeto Docker com identificador gerado nesta execução.
    & docker compose -f $composeFile -p $project down --remove-orphans
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
    Pop-Location
}
