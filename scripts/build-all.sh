#!/bin/bash
# Build All Services Script for Linux/macOS

set -e

echo "========================================"
echo "Building Splitter Services"
echo "========================================"
echo

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# Build shared libraries first
echo "[1/4] Building shared libraries..."
cd shared
mvn clean install -DskipTests
cd ..

# Build API Gateway
echo "[2/4] Building API Gateway..."
cd services/api-gateway
mvn clean package -DskipTests
cd ../..

# Build User Service
echo "[3/4] Building User Service..."
cd services/user-service
mvn clean package -DskipTests
cd ../..

echo
echo "[4/4] Build complete!"
echo
echo "To run services:"
echo "  API Gateway:   cd services/api-gateway && mvn spring-boot:run"
echo "  User Service:  cd services/user-service && mvn spring-boot:run"
echo
