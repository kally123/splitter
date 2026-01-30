# 💸 Splitter - Expense Sharing Application

A modern expense-sharing application inspired by Splitwise, built with reactive microservices architecture.

## 📋 Overview

Splitter helps you track shared expenses with friends, roommates, and groups. Split bills easily, track who owes what, and settle up with minimal transactions.

### Key Features

- ✅ **Track Expenses** - Add and categorize shared expenses
- ✅ **Smart Splitting** - Equal, percentage, shares, or exact amounts
- ✅ **Group Management** - Organize expenses by trips, households, or events
- ✅ **Balance Tracking** - Real-time balance calculations
- ✅ **Debt Simplification** - Minimize the number of payments needed
- ✅ **Multi-Currency** - Support for 100+ currencies (coming soon)
- ✅ **Mobile Apps** - iOS and Android apps (coming soon)

## 🏗️ Architecture

This project uses a **reactive microservices architecture** with:

- **Backend**: Java 21 + Spring Boot 3.x + WebFlux
- **Database**: PostgreSQL with R2DBC (reactive)
- **Messaging**: Apache Kafka for event-driven communication
- **Caching**: Redis
- **Frontend**: Next.js 14 + TypeScript + TailwindCSS

### Services

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Entry point, routing, authentication |
| User Service | 8081 | User management, authentication, friendships |
| Group Service | 8082 | Group CRUD, membership management |
| Expense Service | 8083 | Expense tracking, split calculations |
| Balance Service | 8084 | Balance calculations, debt simplification |
| Settlement Service | 8085 | Payment recording, settlement history |
| Notification Service | 8086 | Push notifications, emails |

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Node.js 18+ (for frontend)
- Maven 3.9+

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/splitter.git
cd splitter
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL, Redis, Kafka
docker-compose -f infrastructure/docker/docker-compose.dev.yml up -d

# Verify services are running
docker-compose -f infrastructure/docker/docker-compose.dev.yml ps
```

### 3. Build Shared Libraries

```bash
cd shared
mvn clean install
cd ..
```

### 4. Run Services

**Option A: Run all services with Docker**
```bash
docker-compose up -d
```

**Option B: Run services individually (for development)**

```bash
# Terminal 1 - User Service
cd services/user-service
mvn spring-boot:run

# Terminal 2 - Group Service
cd services/group-service
mvn spring-boot:run

# Terminal 3 - Expense Service
cd services/expense-service
mvn spring-boot:run

# Terminal 4 - Balance Service
cd services/balance-service
mvn spring-boot:run

# Terminal 5 - Settlement Service
cd services/settlement-service
mvn spring-boot:run

# Terminal 6 - API Gateway
cd services/api-gateway
mvn spring-boot:run
```

### 5. Run Frontend

```bash
cd frontend/web
npm install
```

Create environment file for the frontend:
```bash
# Create .env.local with API configuration
echo "NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1" > .env.local
echo "NEXT_PUBLIC_WS_URL=ws://localhost:8080" >> .env.local
```

Start the development server:
```bash
npm run dev
```

### 6. Access the Application

- **Web App**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Kafka UI**: http://localhost:8090

## 📁 Project Structure

```
splitter/
├── .github/                    # GitHub workflows and guidelines
│   ├── workflows/              # CI/CD pipelines
│   └── *.md                    # Coding guidelines
├── infrastructure/             # Infrastructure configuration
│   ├── docker/                 # Docker Compose files
│   ├── kubernetes/             # K8s manifests
│   └── terraform/              # Cloud infrastructure
├── services/                   # Microservices
│   ├── api-gateway/
│   ├── user-service/
│   ├── group-service/
│   ├── expense-service/
│   ├── balance-service/
│   ├── settlement-service/
│   └── notification-service/
├── shared/                     # Shared libraries
│   ├── common-dto/
│   ├── common-events/
│   └── common-security/
├── frontend/                   # Frontend applications
│   └── web/                    # Next.js web app
├── docs/                       # Documentation
└── scripts/                    # Utility scripts
```

## 🌐 Frontend Application

The web frontend is built with **Next.js 14**, **TypeScript**, and **TailwindCSS**.

### Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: TailwindCSS + shadcn/ui components
- **State Management**: Zustand (with localStorage persistence)
- **Data Fetching**: TanStack React Query
- **Form Handling**: React Hook Form + Zod validation
- **Real-time**: WebSocket for live updates

### Frontend Structure

```
frontend/web/src/
├── app/                    # Next.js App Router pages
│   ├── auth/              # Login & Register pages
│   ├── dashboard/         # Main dashboard
│   ├── groups/            # Group management
│   ├── expenses/          # Expense management
│   ├── balances/          # Balance overview
│   ├── activity/          # Activity & notifications
│   └── settings/          # User settings
├── components/            # React components
│   ├── ui/               # Base UI components (Button, Card, Dialog, etc.)
│   ├── auth/             # Auth components (LoginForm, RegisterForm)
│   ├── layout/           # Layout (Header, Sidebar, MobileNav)
│   ├── groups/           # Group components
│   ├── expenses/         # Expense components
│   ├── balances/         # Balance & settlement components
│   └── notifications/    # Notification components
└── lib/
    ├── api/              # Axios API client with token refresh
    ├── hooks/            # React Query hooks
    ├── stores/           # Zustand stores
    ├── types/            # TypeScript interfaces
    └── utils/            # Utility functions (formatters, validators)
```

### Frontend Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | Backend API base URL | `http://localhost:8080/api/v1` |
| `NEXT_PUBLIC_WS_URL` | WebSocket URL for real-time updates | `ws://localhost:8080` |

### Frontend Scripts

```bash
cd frontend/web

# Development
npm run dev          # Start dev server on http://localhost:3000

# Production
npm run build        # Build for production
npm run start        # Start production server

# Linting
npm run lint         # Run ESLint
```

## 🧪 Running Tests

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run tests for a specific service
cd services/expense-service
mvn test

# Run integration tests
mvn verify -P integration-tests

# Run with coverage
mvn test jacoco:report
```

### End-to-End Tests (Playwright)

```bash
cd frontend/web

# Install Playwright browsers
npx playwright install

# Run E2E tests
npm run test:e2e

# Run with UI mode for debugging
npx playwright test --ui

# Run specific test file
npx playwright test auth.spec.ts

# View test report
npx playwright show-report
```

### Performance Tests (k6)

```bash
# Install k6 (https://k6.io/docs/getting-started/installation/)

# Run load test
k6 run load-tests/expense-load-test.js

# Run stress test
k6 run load-tests/stress-test.js

# Run soak test (4 hours)
k6 run load-tests/soak-test.js
```

## 🔧 Configuration

### Environment Variables

Copy the example environment file and customize:

```bash
cp infrastructure/docker/.env.example infrastructure/docker/.env
```

Key variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_HOST` | PostgreSQL host | localhost |
| `POSTGRES_PORT` | PostgreSQL port | 5432 |
| `POSTGRES_USER` | Database user | splitter |
| `POSTGRES_PASSWORD` | Database password | splitter |
| `REDIS_HOST` | Redis host | localhost |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | localhost:9092 |
| `JWT_SECRET` | JWT signing secret | (generate one) |

### Frontend Configuration

Create a `.env.local` file in `frontend/web/`:

```bash
# API endpoint (API Gateway)
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1

# WebSocket for real-time notifications
NEXT_PUBLIC_WS_URL=ws://localhost:8080
```

### Service Configuration

Each service has its own `application.yml`:

```yaml
# services/expense-service/src/main/resources/application.yml
spring:
  application:
    name: expense-service
  r2dbc:
    url: r2dbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/splitter_expenses
```

## 📚 API Documentation

API documentation is available via Swagger UI when services are running:

- **User Service**: http://localhost:8081/swagger-ui.html
- **Group Service**: http://localhost:8082/swagger-ui.html
- **Expense Service**: http://localhost:8083/swagger-ui.html

Or access all APIs through the gateway:
- **Gateway**: http://localhost:8080/swagger-ui.html

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the [coding guidelines](.github/coding-guidelines.md)
4. Commit your changes (`git commit -m 'feat: add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

### Commit Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes
- `refactor:` - Code refactoring
- `test:` - Adding tests
- `chore:` - Maintenance tasks

## 📖 Documentation

- [Architecture Overview](ARCHITECTURE.md)
- [Project Plan](PROJECT_PLAN.md)
- [API Reference](docs/API_DOCUMENTATION.md)
- [Development Guide](docs/DEVELOPMENT_GUIDE.md)
- [Phase 1 Implementation](docs/PHASE_1_IMPLEMENTATION.md)
- [Phase 3 Implementation](docs/PHASE_3_IMPLEMENTATION.md)
- [Security Checklist](docs/SECURITY_CHECKLIST.md)

## 🚀 Production Deployment

### Kubernetes Deployment

```bash
# Apply namespace and configs
kubectl apply -f infrastructure/kubernetes/namespace.yaml
kubectl apply -f infrastructure/kubernetes/config/

# Deploy services
kubectl apply -f infrastructure/kubernetes/services/

# Apply ingress
kubectl apply -f infrastructure/kubernetes/ingress/

# Set up monitoring
kubectl apply -f infrastructure/kubernetes/monitoring/

# Apply security policies
kubectl apply -f infrastructure/kubernetes/security/
```

### CI/CD Pipeline

The project includes GitHub Actions workflows for:

- **CI** (`.github/workflows/ci.yml`): Runs on every push/PR
  - Build all services
  - Run unit and integration tests
  - Code quality checks (SonarCloud)
  - Security scanning (Trivy, OWASP)

- **Production Deployment** (`.github/workflows/deploy-production.yml`): Runs on tags
  - Build Docker images
  - Push to container registry
  - Deploy to Kubernetes
  - Run smoke tests
  - Automatic rollback on failure

### Monitoring

- **Prometheus**: Metrics collection
- **Grafana**: Dashboards and visualization
- **Loki**: Log aggregation
- **Jaeger**: Distributed tracing

Dashboards available at: http://grafana.splitter.example.com

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by [Splitwise](https://www.splitwise.com/)
- Built with [Spring Boot](https://spring.io/projects/spring-boot)
- UI components from [shadcn/ui](https://ui.shadcn.com/)
