[CmdletBinding()]
param(
    [ValidateRange(30, 600)]
    [int]$DockerTimeoutSeconds = 180,

    [ValidateRange(30, 600)]
    [int]$AppTimeoutSeconds = 240,

    [switch]$NoBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$envFile = Join-Path $repoRoot '.env'
$envExample = Join-Path $repoRoot '.env.example'
$appUrl = 'http://localhost:8080'
$readinessUrl = "$appUrl/actuator/health/readiness"

function Write-Step {
    param([Parameter(Mandatory)][string]$Message)

    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Test-DockerEngine {
    & docker info --format '{{.ServerVersion}}' *> $null
    return $LASTEXITCODE -eq 0
}

function Start-DockerDesktopIfNeeded {
    if (Test-DockerEngine) {
        return
    }

    $desktopCandidates = @(
        (Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'),
        (Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\Docker Desktop.exe'),
        (Join-Path $env:LOCALAPPDATA 'Docker\Docker Desktop.exe')
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    if ($desktopCandidates.Count -eq 0) {
        throw 'Docker engine is not running and Docker Desktop could not be found. Start Docker Desktop, then run this script again.'
    }

    Write-Step 'Starting Docker Desktop'
    Start-Process -FilePath $desktopCandidates[0] -WindowStyle Hidden

    $deadline = [DateTime]::UtcNow.AddSeconds($DockerTimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Seconds 2
        if (Test-DockerEngine) {
            return
        }
    }

    throw "Docker Desktop did not become ready within $DockerTimeoutSeconds seconds."
}

function Invoke-Compose {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & docker compose @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed with exit code $LASTEXITCODE."
    }
}

function Test-SmartClinicReady {
    try {
        $health = Invoke-RestMethod -Uri $readinessUrl -TimeoutSec 5
        $info = Invoke-RestMethod -Uri "$appUrl/actuator/info" -TimeoutSec 5
        return $health.status -eq 'UP' -and $info.app.name -eq 'smart-clinic'
    }
    catch {
        return $false
    }
}

function Test-AppPortInUse {
    try {
        return [bool](Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction Stop)
    }
    catch {
        return Test-NetConnection -ComputerName '127.0.0.1' -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue
    }
}

function Wait-ForApplication {
    $deadline = [DateTime]::UtcNow.AddSeconds($AppTimeoutSeconds)

    while ([DateTime]::UtcNow -lt $deadline) {
        try {
            $health = Invoke-RestMethod -Uri $readinessUrl -TimeoutSec 5
            if ($health.status -eq 'UP') {
                return
            }
        }
        catch {
            # The application may still be starting. Retry until the deadline.
        }

        $containerId = (& docker compose ps --quiet app 2>$null | Select-Object -First 1)
        if ($containerId) {
            $containerState = (& docker inspect --format '{{.State.Status}}' $containerId 2>$null)
            if ($containerState -eq 'exited' -or $containerState -eq 'dead') {
                throw "Smart Clinic container stopped before readiness became UP. Run 'docker compose logs app' for details."
            }
        }

        Start-Sleep -Seconds 2
    }

    throw "Smart Clinic readiness did not become UP within $AppTimeoutSeconds seconds. Run 'docker compose ps' and 'docker compose logs app' for details."
}

try {
    Set-Location -LiteralPath $repoRoot

    if (Test-SmartClinicReady) {
        Write-Host "`nSmart Clinic is already ready: $appUrl" -ForegroundColor Green
        if (-not $NoBrowser) {
            Write-Step 'Opening Smart Clinic in the default browser'
            Start-Process -FilePath $appUrl
        }
        return
    }

    if (Test-AppPortInUse) {
        throw "Port 8080 is already used by another application. Stop that application, then run this script again."
    }

    Write-Step 'Checking Docker'
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI was not found. Install Docker Desktop, then run this script again.'
    }

    & docker compose version *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose is unavailable. Update Docker Desktop, then run this script again.'
    }

    Start-DockerDesktopIfNeeded

    if (-not (Test-Path -LiteralPath $envFile)) {
        if (-not (Test-Path -LiteralPath $envExample)) {
            throw '.env.example is missing, so local demo configuration cannot be created.'
        }

        Write-Step 'Creating local demo configuration from .env.example'
        Copy-Item -LiteralPath $envExample -Destination $envFile
    }
    else {
        Write-Step 'Using existing .env configuration'
    }

    Write-Step 'Starting MySQL and MongoDB'
    Invoke-Compose -Arguments @('up', '--detach', '--wait', 'mysql', 'mongodb')

    Write-Step 'Building and starting Smart Clinic'
    Invoke-Compose -Arguments @('up', '--detach', '--build', 'app')

    Write-Step 'Waiting for Smart Clinic readiness'
    Wait-ForApplication

    Write-Host "`nSmart Clinic is ready: $appUrl" -ForegroundColor Green
    Write-Host 'Stop later with: docker compose down'

    if (-not $NoBrowser) {
        Write-Step 'Opening Smart Clinic in the default browser'
        Start-Process -FilePath $appUrl
    }
}
catch {
    Write-Host "`nStart failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
finally {
    Set-Location -LiteralPath $repoRoot
}
