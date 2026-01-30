#!/bin/bash
# Start Development Environment Script for Linux/macOS
# This script starts all required infrastructure services for local development

set -e

echo "========================================"
echo "Starting Splitter Development Environment"
echo "========================================"
echo

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not running. Please start Docker first."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "[1/3] Starting infrastructure services..."
cd "$PROJECT_ROOT/infrastructure/docker"
docker compose -f docker-compose.dev.yml up -d

echo
echo "[2/3] Waiting for services to be healthy..."
sleep 10

echo
echo "[3/3] Checking service status..."
docker compose -f docker-compose.dev.yml ps

echo
echo "========================================"
echo "Development Environment Started!"
echo "========================================"
echo
echo "Available Services:"
echo "  - PostgreSQL:    localhost:5432"
echo "  - Redis:         localhost:6379"
echo "  - Kafka:         localhost:9094"
echo "  - Kafka UI:      http://localhost:8090"
echo "  - pgAdmin:       http://localhost:5050"
echo "  - Mailhog:       http://localhost:8025"
echo
echo "To view logs: docker compose -f infrastructure/docker/docker-compose.dev.yml logs -f"
echo "To stop:      scripts/stop-dev.sh"
echo
