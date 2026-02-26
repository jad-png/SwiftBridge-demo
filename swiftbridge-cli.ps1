# SwiftBridge - Quick Start Script for Windows
# This PowerShell script automates common Docker and Maven operations
# Usage: .\swiftbridge-cli.ps1 -Command "up"

param(
    [Parameter(Position=0)]
    [ValidateSet("up", "down", "logs", "status", "build", "clean", "rebuild", "test", "shell", "ps", "help")]
    [string]$Command = "help",
    
    [Parameter(Position=1)]
    [string]$Service = $null,
    
    [string]$BuildProfile = "docker"
)

$ErrorActionPreference = "Stop"

function Show-Help {
    Write-Host @"
╔════════════════════════════════════════════════════════════════════════════╗
║                          SwiftBridge CLI - Help                            ║
╚════════════════════════════════════════════════════════════════════════════╝

USAGE:
  .\swiftbridge-cli.ps1 -Command <command> [options]

COMMANDS:
  up              Start all services (docker-compose up -d)
  down            Stop all services (docker-compose down)
  logs            Show logs (-Service optional: postgres, orchestrator, converter)
  status          Show service status
  build           Build Docker images
  clean           Remove containers and volumes
  rebuild         Clean and rebuild everything
  test            Run health checks on all services
  shell           Open shell in service container (-Service required)
  ps              List running containers
  help            Show this help message

EXAMPLES:
  .\swiftbridge-cli.ps1 -Command up                    # Start all services
  .\swiftbridge-cli.ps1 -Command logs -Service orchestrator
  .\swiftbridge-cli.ps1 -Command shell -Service postgres
  .\swiftbridge-cli.ps1 -Command clean                 # Stop and remove

REQUIREMENTS:
  - Docker Desktop
  - Docker Compose (usually included with Docker Desktop)
  - Windows PowerShell 5.0 or later

"@
}

function Check-Prerequisites {
    Write-Host "Checking prerequisites..."
    
    # Check Docker
    try {
        $dockerVersion = docker --version
        Write-Host "✓ Docker: $dockerVersion"
    } catch {
        Write-Error "✗ Docker not found. Please install Docker Desktop from https://www.docker.com/products/docker-desktop"
        exit 1
    }
    
    # Check Docker Compose
    try {
        $composeVersion = docker-compose --version
        Write-Host "✓ Docker Compose: $composeVersion"
    } catch {
        Write-Error "✗ Docker Compose not found. Please install Docker Desktop with Compose."
        exit 1
    }
}

function Start-Services {
    Write-Host @"

╔════════════════════════════════════════════════════════════════════════════╗
║                    Starting SwiftBridge Services...                        ║
╚════════════════════════════════════════════════════════════════════════════╝

"@
    
    Check-Prerequisites
    
    Write-Host "Building images..."
    docker-compose build
    
    Write-Host "Starting services..."
    docker-compose up -d
    
    Write-Host "Waiting for services to become healthy..."
    Start-Sleep -Seconds 10
    
    Write-Host @"

✓ Services started successfully!

ACCESS ENDPOINTS:
  • Orchestrator Service:  http://localhost:8080/api/actuator/health
  • Core Converter Service: http://localhost:8081/api/v1/actuator/health
  • PostgreSQL Database:   localhost:5432
    - Username: swiftbridge_user
    - Password: SwiftBridge@123!Secure
    - Database: swiftbridge

NEXT STEPS:
  1. Test: .\swiftbridge-cli.ps1 -Command test
  2. View logs: .\swiftbridge-cli.ps1 -Command logs
  3. Check status: .\swiftbridge-cli.ps1 -Command status

"@
}

function Stop-Services {
    Write-Host @"

╔════════════════════════════════════════════════════════════════════════════╗
║                    Stopping SwiftBridge Services...                        ║
╚════════════════════════════════════════════════════════════════════════════╝

"@
    
    docker-compose down
    
    Write-Host "✓ Services stopped."
}

function Show-Logs {
    if ($Service) {
        Write-Host "Showing logs for: $Service"
        docker-compose logs -f $Service
    } else {
        Write-Host "Showing logs from all services (press Ctrl+C to exit)..."
        docker-compose logs -f
    }
}

function Show-Status {
    Write-Host @"

╔════════════════════════════════════════════════════════════════════════════╗
║                         Service Status                                     ║
╚════════════════════════════════════════════════════════════════════════════╝

"@
    
    docker-compose ps
    
    Write-Host @"

HEALTH CHECK RESULTS:
"@
    
    # Orchestrator Health
    try {
        $health = Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" -TimeoutSec 5 | ConvertFrom-Json
        Write-Host "  ✓ Orchestrator Service: $($health.status)"
    } catch {
        Write-Host "  ✗ Orchestrator Service: UNREACHABLE"
    }
    
    # Converter Health
    try {
        $health = Invoke-WebRequest -Uri "http://localhost:8081/api/v1/actuator/health" -TimeoutSec 5 | ConvertFrom-Json
        Write-Host "  ✓ Core Converter Service: $($health.status)"
    } catch {
        Write-Host "  ✗ Core Converter Service: UNREACHABLE"
    }
    
    # PostgreSQL Check
    try {
        $result = docker exec swiftbridge_postgres psql -U swiftbridge_user -d swiftbridge -c "SELECT version();" 2>&1
        Write-Host "  ✓ PostgreSQL Database: CONNECTED"
    } catch {
        Write-Host "  ✗ PostgreSQL Database: UNREACHABLE"
    }
}

function Build-Services {
    Write-Host @"

╔════════════════════════════════════════════════════════════════════════════╗
║                    Building Docker Images...                               ║
╚════════════════════════════════════════════════════════════════════════════╝

"@
    
    docker-compose build
    Write-Host "✓ Build complete."
}

function Clean-Services {
    Write-Host @"

╔════════════════════════════════════════════════════════════════════════════╗
║               Cleaning Up Containers & Volumes...                          ║
╚════════════════════════════════════════════════════════════════════════════╝

WARNING: This will delete all containers and volumes!
"@
    
    $confirm = Read-Host "Are you sure? (yes/no)"
    
    if ($confirm -eq "yes") {
        docker-compose down -v
        Write-Host "✓ Cleanup complete."
    } else {
        Write-Host "Cancelled."
    }
}

function Rebuild-Services {
    Write-Host @"

╔════════════════════════════════════════════════════════════════════════════╗
║              Rebuilding Everything (Clean + Build)...                      ║
╚════════════════════════════════════════════════════════════════════════════╝

"@
    
    docker-compose down -v
    Start-Sleep -Seconds 2
    docker-compose build --no-cache
    docker-compose up -d
    
    Write-Host "✓ Rebuild complete!"
}

function Run-Tests {
    Write-Host @"

╔════════════════════════════════════════════════════════════════════════════╗
║                    Running Health Checks...                                ║
╚════════════════════════════════════════════════════════════════════════════╝

"@
    
    $allHealthy = $true
    
    # Test Orchestrator
    Write-Host "Testing Orchestrator Service..."
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✓ Orchestrator responding (HTTP 200)"
        }
    } catch {
        Write-Host "  ✗ Orchestrator failed: $_"
        $allHealthy = $false
    }
    
    # Test Converter
    Write-Host "Testing Core Converter Service..."
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081/api/v1/actuator/health" -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✓ Converter responding (HTTP 200)"
        }
    } catch {
        Write-Host "  ✗ Converter failed: $_"
        $allHealthy = $false
    }
    
    # Test Database
    Write-Host "Testing PostgreSQL Database..."
    try {
        docker exec swiftbridge_postgres psql -U swiftbridge_user -d swiftbridge -c "SELECT 1;" > $null 2>&1
        Write-Host "  ✓ Database responding"
    } catch {
        Write-Host "  ✗ Database failed: $_"
        $allHealthy = $false
    }
    
    Write-Host ""
    if ($allHealthy) {
        Write-Host "✓ All services are healthy!"
        exit 0
    } else {
        Write-Host "✗ Some services are not responding. Check logs with: .\swiftbridge-cli.ps1 -Command logs"
        exit 1
    }
}

function Open-Shell {
    if (-not $Service) {
        Write-Error "Service name required. Use: .\swiftbridge-cli.ps1 -Command shell -Service <service>"
        exit 1
    }
    
    $serviceMap = @{
        "postgres" = "swiftbridge_postgres"
        "orchestrator" = "swiftbridge_orchestrator"
        "converter" = "swiftbridge_converter"
    }
    
    $container = $serviceMap[$Service]
    if (-not $container) {
        Write-Error "Unknown service: $Service. Available: postgres, orchestrator, converter"
        exit 1
    }
    
    Write-Host "Opening shell in $container..."
    docker exec -it $container sh
}

function Show-ProcsStatus {
    Write-Host @"

╔════════════════════════════════════════════════════════════════════════════╗
║                       Running Containers                                   ║
╚════════════════════════════════════════════════════════════════════════════╝

"@
    
    docker-compose ps
}

# Main execution
switch ($Command) {
    "up" { Start-Services }
    "down" { Stop-Services }
    "logs" { Show-Logs }
    "status" { Show-Status }
    "build" { Build-Services }
    "clean" { Clean-Services }
    "rebuild" { Rebuild-Services }
    "test" { Run-Tests }
    "shell" { Open-Shell }
    "ps" { Show-ProcsStatus }
    "help" { Show-Help }
    default { Show-Help }
}
