$ErrorActionPreference = 'Stop'
$taskApiRoot = Split-Path -Parent $PSScriptRoot
$taskEnvNames = @('FLYWAY_URL', 'FLYWAY_USER', 'FLYWAY_PASSWORD', 'TEST_POSTGIS_URL', 'TEST_POSTGIS_USER', 'TEST_POSTGIS_PASSWORD')
$taskPreviousEnv = @{}
foreach ($taskName in $taskEnvNames) { $taskPreviousEnv[$taskName] = [Environment]::GetEnvironmentVariable($taskName) }
Push-Location -LiteralPath $taskApiRoot
try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Instale e inicie Docker para executar os testes PostGIS.' }
    docker compose -f compose.test.yml up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível iniciar o banco de testes.' }
    $taskPrepared = docker compose -f compose.test.yml exec -T postgres psql -U postgres -d maonaroda_test -tAc "SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname = 'auth')"
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível consultar o banco descartável.' }
    if ($taskPrepared.Trim() -eq 'f') {
        docker compose -f compose.test.yml exec -T postgres psql -U postgres -d maonaroda_test -v ON_ERROR_STOP=1 -f /workspace/database/tests/preparar-container.sql
        if ($LASTEXITCODE -ne 0) { throw 'Falha no bootstrap do banco descartável.' }
    }
    $env:FLYWAY_URL = 'jdbc:postgresql://127.0.0.1:55432/maonaroda_test'
    $env:FLYWAY_USER = 'postgres'
    $env:FLYWAY_PASSWORD = 'teste-local-descartavel'
    $env:TEST_POSTGIS_URL = $env:FLYWAY_URL
    $env:TEST_POSTGIS_USER = $env:FLYWAY_USER
    $env:TEST_POSTGIS_PASSWORD = $env:FLYWAY_PASSWORD
    & .\mvnw.cmd --batch-mode --no-transfer-progress flyway:migrate flyway:validate
    if ($LASTEXITCODE -ne 0) { throw 'Falha nas migrations PostGIS.' }
    docker compose -f compose.test.yml exec -T postgres psql -U postgres -d maonaroda_test -v ON_ERROR_STOP=1 -f /workspace/database/tests/integridade.sql
    if ($LASTEXITCODE -ne 0) { throw 'Falha na integridade SQL.' }
    & .\mvnw.cmd --batch-mode --no-transfer-progress verify
    if ($LASTEXITCODE -ne 0) { throw 'Falha na suíte da API.' }
} finally {
    foreach ($taskName in $taskEnvNames) { [Environment]::SetEnvironmentVariable($taskName, $taskPreviousEnv[$taskName]) }
    Pop-Location
}
