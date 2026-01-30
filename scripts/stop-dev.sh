#!/bin/bash
# Stop Development Environment Script for Linux/macOS

set -e

echo "========================================"
echo "Stopping Splitter Development Environment"
echo "========================================"
echo

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT/infrastructure/docker"
docker compose -f docker-compose.dev.yml down

echo
echo "Development environment stopped."
echo
echo "To remove volumes (WARNING: deletes all data):"
echo "  docker compose -f infrastructure/docker/docker-compose.dev.yml down -v"
echo
