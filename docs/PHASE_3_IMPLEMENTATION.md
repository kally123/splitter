# Phase 3: MVP Complete - Testing, Deployment & Production Readiness

## Overview

Phase 3 focuses on completing the MVP by implementing comprehensive testing, setting up production infrastructure, establishing monitoring and observability, and deploying the application. This phase ensures the application is production-ready with proper quality assurance.

**Duration:** Week 11-12  
**Dependencies:** Phase 1 (Core Services) and Phase 2 (Integration & Frontend) must be complete

---

## Goals

1. **End-to-End Testing** - Comprehensive test coverage for all critical user flows
2. **Bug Fixes & Polish** - Address issues found during testing, improve UX
3. **Production Infrastructure** - Kubernetes/container orchestration setup
4. **Monitoring & Observability** - Prometheus, Grafana, distributed tracing, log aggregation
5. **CI/CD Pipeline Enhancement** - Automated deployment to staging and production
6. **Security Hardening** - Security audit and fixes
7. **Production Deployment** - Go-live with rollback capabilities

---

## Sprint 3.1: End-to-End Testing (Week 11)

### 3.1.1 E2E Test Infrastructure Setup

**Objective:** Set up Playwright for comprehensive end-to-end testing

**Dependencies:**
- Node.js 18+
- Frontend application running
- All backend services running

**Files to Create:**
```
frontend/web/
├── e2e/
│   ├── playwright.config.ts
│   ├── fixtures/
│   │   ├── auth.fixture.ts
│   │   └── test-data.ts
│   ├── pages/
│   │   ├── login.page.ts
│   │   ├── dashboard.page.ts
│   │   ├── groups.page.ts
│   │   ├── expenses.page.ts
│   │   └── settings.page.ts
│   └── tests/
│       ├── auth.spec.ts
│       ├── groups.spec.ts
│       ├── expenses.spec.ts
│       ├── balances.spec.ts
│       └── settlements.spec.ts
```

**Playwright Configuration:**
```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html'],
    ['junit', { outputFile: 'test-results/e2e-results.xml' }]
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
```

**Page Object Model:**
```typescript
// e2e/pages/login.page.ts
import { Page, Locator, expect } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.locator('input[id="email"]');
    this.passwordInput = page.locator('input[id="password"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.errorMessage = page.locator('[role="alert"]');
  }

  async goto() {
    await this.page.goto('/auth/login');
  }

  async login(email: string, password: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.submitButton.click();
  }

  async expectError(message: string) {
    await expect(this.errorMessage).toContainText(message);
  }
}
```

---

### 3.1.2 Critical User Journey Tests

**Objective:** Automate testing for all critical user flows

**Test Scenarios:**

#### Authentication Tests
```typescript
// e2e/tests/auth.spec.ts
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';

test.describe('Authentication', () => {
  test('should register a new user', async ({ page }) => {
    await page.goto('/auth/register');
    
    await page.fill('input[id="displayName"]', 'Test User');
    await page.fill('input[id="email"]', `test-${Date.now()}@example.com`);
    await page.fill('input[id="password"]', 'Password123!');
    await page.fill('input[id="confirmPassword"]', 'Password123!');
    await page.click('button[type="submit"]');
    
    await expect(page).toHaveURL('/dashboard');
    await expect(page.locator('h1')).toContainText('Dashboard');
  });

  test('should login with valid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('user@example.com', 'password123');
    
    await expect(page).toHaveURL('/dashboard');
  });

  test('should show error for invalid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('invalid@example.com', 'wrongpassword');
    
    await loginPage.expectError('Invalid email or password');
  });

  test('should logout successfully', async ({ page }) => {
    // Login first
    await page.goto('/auth/login');
    await page.fill('input[id="email"]', 'user@example.com');
    await page.fill('input[id="password"]', 'password123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');
    
    // Logout
    await page.click('[data-testid="user-menu"]');
    await page.click('text=Log out');
    
    await expect(page).toHaveURL('/auth/login');
  });
});
```

#### Expense Flow Tests
```typescript
// e2e/tests/expenses.spec.ts
import { test, expect } from '@playwright/test';
import { authFixture } from '../fixtures/auth.fixture';

test.describe('Expense Management', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('complete expense flow', async ({ page }) => {
    // 1. Navigate to groups
    await page.goto('/groups');
    
    // 2. Create a new group
    await page.click('text=Create Group');
    await page.fill('input[id="name"]', 'Test Trip');
    await page.selectOption('select[id="type"]', 'TRIP');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('h1')).toContainText('Test Trip');
    
    // 3. Add an expense
    await page.click('text=Add Expense');
    await page.fill('input[id="description"]', 'Dinner at Restaurant');
    await page.fill('input[id="amount"]', '120');
    await page.selectOption('select[id="category"]', 'FOOD');
    await page.click('button[type="submit"]');
    
    // 4. Verify expense appears
    await expect(page.locator('.expense-card')).toContainText('Dinner at Restaurant');
    await expect(page.locator('.expense-card')).toContainText('$120');
    
    // 5. Check balance updated
    await page.click('text=Balances');
    await expect(page.locator('.balance-summary')).toBeVisible();
  });

  test('should split expense equally', async ({ page }) => {
    await page.goto('/groups/test-group-id');
    await page.click('text=Add Expense');
    
    await page.fill('input[id="description"]', 'Shared Lunch');
    await page.fill('input[id="amount"]', '60');
    await page.selectOption('select[id="splitType"]', 'EQUAL');
    await page.click('button[type="submit"]');
    
    // Verify splits (assuming 3 members)
    await page.click('.expense-card:has-text("Shared Lunch")');
    await expect(page.locator('.split-details')).toContainText('$20.00');
  });

  test('should edit an expense', async ({ page }) => {
    await page.goto('/groups/test-group-id');
    
    await page.click('.expense-card:first-child >> [data-testid="expense-menu"]');
    await page.click('text=Edit');
    
    await page.fill('input[id="description"]', 'Updated Dinner');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('.expense-card')).toContainText('Updated Dinner');
  });

  test('should delete an expense', async ({ page }) => {
    await page.goto('/groups/test-group-id');
    
    const expenseCount = await page.locator('.expense-card').count();
    
    await page.click('.expense-card:first-child >> [data-testid="expense-menu"]');
    await page.click('text=Delete');
    await page.click('text=Confirm');
    
    await expect(page.locator('.expense-card')).toHaveCount(expenseCount - 1);
  });
});
```

#### Settlement Flow Tests
```typescript
// e2e/tests/settlements.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Settlement Flow', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should settle up between users', async ({ page }) => {
    await page.goto('/groups/test-group-id');
    await page.click('text=Balances');
    
    // Click settle button for a specific balance
    await page.click('.balance-row:first-child >> text=Settle');
    
    // Fill settlement modal
    await page.fill('input[id="amount"]', '25');
    await page.selectOption('select[id="paymentMethod"]', 'VENMO');
    await page.fill('input[id="note"]', 'Venmo payment');
    await page.click('text=Record Payment');
    
    // Verify settlement recorded
    await expect(page.locator('.toast')).toContainText('Settlement recorded');
  });

  test('should confirm a pending settlement', async ({ page }) => {
    // Login as the receiving user
    await page.goto('/activity');
    
    await page.click('.notification:has-text("settlement")');
    await page.click('text=Confirm');
    
    await expect(page.locator('.toast')).toContainText('Settlement confirmed');
  });
});
```

---

### 3.1.3 API Contract Tests

**Objective:** Ensure API contracts are maintained between services

**Files to Create:**
```
services/
├── expense-service/
│   └── src/test/java/com/splitter/expense/contract/
│       └── ExpenseProviderContractTest.java
├── balance-service/
│   └── src/test/java/com/splitter/balance/contract/
│       └── BalanceConsumerContractTest.java
```

**Provider Contract Test (Expense Service):**
```java
// ExpenseProviderContractTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("expense-service")
@PactBroker(
    host = "${pact.broker.host}",
    authentication = @PactBrokerAuth(token = "${pact.broker.token}")
)
public class ExpenseProviderContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ExpenseRepository expenseRepository;

    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("expense exists with id 123")
    void expenseExists() {
        Expense expense = Expense.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000123"))
            .description("Test Expense")
            .amount(new BigDecimal("50.00"))
            .currency("USD")
            .build();
        expenseRepository.save(expense).block();
    }
}
```

---

### 3.1.4 Performance Tests

**Objective:** Establish performance baselines and identify bottlenecks

**Tools:** k6, Artillery, or Gatling

**k6 Load Test Script:**
```javascript
// load-tests/expense-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const createExpenseTrend = new Trend('create_expense_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up
    { duration: '1m', target: 50 },    // Stay at 50 users
    { duration: '30s', target: 100 },  // Spike to 100
    { duration: '1m', target: 100 },   // Stay at 100
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% requests under 500ms
    errors: ['rate<0.01'],              // Error rate under 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN;

export default function () {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${AUTH_TOKEN}`,
  };

  // Create expense
  const expensePayload = JSON.stringify({
    groupId: 'test-group-id',
    description: `Load test expense ${Date.now()}`,
    amount: Math.random() * 100,
    currency: 'USD',
    category: 'OTHER',
    splitType: 'EQUAL',
  });

  const createRes = http.post(`${BASE_URL}/api/v1/expenses`, expensePayload, { headers });
  createExpenseTrend.add(createRes.timings.duration);
  
  check(createRes, {
    'expense created': (r) => r.status === 201,
    'response time < 500ms': (r) => r.timings.duration < 500,
  }) || errorRate.add(1);

  sleep(1);

  // Get balances
  const balanceRes = http.get(`${BASE_URL}/api/v1/balances?groupId=test-group-id`, { headers });
  
  check(balanceRes, {
    'balances retrieved': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);
}
```

**Performance Test Script (package.json):**
```json
{
  "scripts": {
    "test:load": "k6 run load-tests/expense-load-test.js",
    "test:stress": "k6 run --vus 200 --duration 5m load-tests/expense-load-test.js",
    "test:soak": "k6 run --vus 50 --duration 30m load-tests/expense-load-test.js"
  }
}
```

---

## Sprint 3.2: Bug Fixes & Polish (Week 11)

### 3.2.1 Bug Triage & Fixes

**Objective:** Identify and fix all P0/P1 bugs

**Bug Priority Definitions:**
| Priority | Definition | SLA |
|----------|------------|-----|
| P0 | Critical - App unusable, data loss risk | Fix immediately |
| P1 | High - Major feature broken | Fix within 24h |
| P2 | Medium - Minor feature impact | Fix within 1 week |
| P3 | Low - Cosmetic, nice-to-have | Next release |

**Common Issues to Check:**
- [ ] Authentication token expiration handling
- [ ] Form validation edge cases
- [ ] Error message clarity
- [ ] Loading state coverage
- [ ] Empty state displays
- [ ] Mobile responsiveness issues
- [ ] WebSocket reconnection logic
- [ ] Race conditions in balance updates
- [ ] Timezone handling for dates
- [ ] Currency formatting edge cases

---

### 3.2.2 UX Polish

**Objective:** Improve user experience with polish items

**Checklist:**
- [ ] Add skeleton loaders for all async content
- [ ] Implement proper error boundaries
- [ ] Add retry buttons for failed requests
- [ ] Improve form feedback (success/error states)
- [ ] Add confirmation dialogs for destructive actions
- [ ] Implement proper focus management
- [ ] Add keyboard navigation support
- [ ] Optimize images and assets
- [ ] Add proper page titles and meta tags
- [ ] Implement proper 404 and 500 pages

**Skeleton Loader Component:**
```typescript
// components/ui/skeleton.tsx
import { cn } from "@/lib/utils/cn";

interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {}

export function Skeleton({ className, ...props }: SkeletonProps) {
  return (
    <div
      className={cn("animate-pulse rounded-md bg-muted", className)}
      {...props}
    />
  );
}

// Usage in ExpenseList
export function ExpenseListSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(5)].map((_, i) => (
        <div key={i} className="flex items-center space-x-4 p-4 border rounded-lg">
          <Skeleton className="h-10 w-10 rounded-lg" />
          <div className="space-y-2 flex-1">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-1/2" />
          </div>
          <Skeleton className="h-6 w-16" />
        </div>
      ))}
    </div>
  );
}
```

**Error Boundary:**
```typescript
// components/error-boundary.tsx
'use client';

import { Component, ReactNode } from 'react';
import { Button, Card, CardContent, CardHeader, CardTitle } from '@/components/ui';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error boundary caught:', error, errorInfo);
    // Report to error tracking service (Sentry, etc.)
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <Card className="m-4">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-destructive">
              <AlertTriangle className="h-5 w-5" />
              Something went wrong
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-muted-foreground">
              We encountered an error. Please try refreshing the page.
            </p>
            <Button onClick={() => window.location.reload()}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Refresh Page
            </Button>
          </CardContent>
        </Card>
      );
    }

    return this.props.children;
  }
}
```

---

## Sprint 3.3: Production Infrastructure (Week 12)

### 3.3.1 Kubernetes Deployment

**Objective:** Set up Kubernetes infrastructure for production

**Files to Create:**
```
infrastructure/
├── kubernetes/
│   ├── namespace.yaml
│   ├── config/
│   │   ├── configmap.yaml
│   │   └── secrets.yaml
│   ├── services/
│   │   ├── api-gateway/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   └── hpa.yaml
│   │   ├── user-service/
│   │   ├── group-service/
│   │   ├── expense-service/
│   │   ├── balance-service/
│   │   ├── settlement-service/
│   │   └── notification-service/
│   ├── ingress/
│   │   └── ingress.yaml
│   └── monitoring/
│       ├── prometheus/
│       └── grafana/
```

**Namespace:**
```yaml
# infrastructure/kubernetes/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: splitter
  labels:
    name: splitter
    environment: production
```

**ConfigMap:**
```yaml
# infrastructure/kubernetes/config/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: splitter-config
  namespace: splitter
data:
  SPRING_PROFILES_ACTIVE: "production"
  KAFKA_BOOTSTRAP_SERVERS: "kafka-headless.splitter.svc.cluster.local:9092"
  REDIS_HOST: "redis-master.splitter.svc.cluster.local"
  POSTGRES_HOST: "postgresql.splitter.svc.cluster.local"
```

**API Gateway Deployment:**
```yaml
# infrastructure/kubernetes/services/api-gateway/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: splitter
  labels:
    app: api-gateway
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
      - name: api-gateway
        image: splitter/api-gateway:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        envFrom:
        - configMapRef:
            name: splitter-config
        - secretRef:
            name: splitter-secrets
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        lifecycle:
          preStop:
            exec:
              command: ["sh", "-c", "sleep 10"]
```

**Horizontal Pod Autoscaler:**
```yaml
# infrastructure/kubernetes/services/api-gateway/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: splitter
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

**Ingress:**
```yaml
# infrastructure/kubernetes/ingress/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: splitter-ingress
  namespace: splitter
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
spec:
  tls:
  - hosts:
    - api.splitter.example.com
    secretName: splitter-tls
  rules:
  - host: api.splitter.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
```

---

### 3.3.2 Monitoring & Observability

**Objective:** Set up comprehensive monitoring with Prometheus, Grafana, and distributed tracing

**Prometheus ServiceMonitor:**
```yaml
# infrastructure/kubernetes/monitoring/prometheus/servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: splitter-services
  namespace: splitter
  labels:
    release: prometheus
spec:
  selector:
    matchLabels:
      app.kubernetes.io/part-of: splitter
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

**Grafana Dashboard ConfigMap:**
```yaml
# infrastructure/kubernetes/monitoring/grafana/dashboard-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: splitter-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  splitter-overview.json: |
    {
      "annotations": { "list": [] },
      "editable": true,
      "fiscalYearStartMonth": 0,
      "graphTooltip": 0,
      "id": null,
      "links": [],
      "liveNow": false,
      "panels": [
        {
          "title": "Request Rate",
          "type": "timeseries",
          "datasource": { "type": "prometheus" },
          "targets": [
            {
              "expr": "sum(rate(http_server_requests_seconds_count{namespace=\"splitter\"}[5m])) by (service)",
              "legendFormat": "{{service}}"
            }
          ],
          "gridPos": { "h": 8, "w": 12, "x": 0, "y": 0 }
        },
        {
          "title": "Response Time (p95)",
          "type": "timeseries",
          "datasource": { "type": "prometheus" },
          "targets": [
            {
              "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{namespace=\"splitter\"}[5m])) by (le, service))",
              "legendFormat": "{{service}}"
            }
          ],
          "gridPos": { "h": 8, "w": 12, "x": 12, "y": 0 }
        },
        {
          "title": "Error Rate",
          "type": "timeseries",
          "datasource": { "type": "prometheus" },
          "targets": [
            {
              "expr": "sum(rate(http_server_requests_seconds_count{namespace=\"splitter\", status=~\"5..\"}[5m])) by (service) / sum(rate(http_server_requests_seconds_count{namespace=\"splitter\"}[5m])) by (service) * 100",
              "legendFormat": "{{service}}"
            }
          ],
          "gridPos": { "h": 8, "w": 12, "x": 0, "y": 8 }
        },
        {
          "title": "Kafka Consumer Lag",
          "type": "timeseries",
          "datasource": { "type": "prometheus" },
          "targets": [
            {
              "expr": "sum(kafka_consumer_fetch_manager_records_lag{namespace=\"splitter\"}) by (client_id, topic)",
              "legendFormat": "{{client_id}} - {{topic}}"
            }
          ],
          "gridPos": { "h": 8, "w": 12, "x": 12, "y": 8 }
        }
      ],
      "refresh": "30s",
      "schemaVersion": 38,
      "style": "dark",
      "tags": ["splitter"],
      "templating": { "list": [] },
      "time": { "from": "now-1h", "to": "now" },
      "title": "Splitter Overview",
      "uid": "splitter-overview"
    }
```

**Alerting Rules:**
```yaml
# infrastructure/kubernetes/monitoring/prometheus/alerting-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: splitter-alerts
  namespace: splitter
  labels:
    release: prometheus
spec:
  groups:
  - name: splitter.rules
    rules:
    - alert: HighErrorRate
      expr: |
        sum(rate(http_server_requests_seconds_count{namespace="splitter", status=~"5.."}[5m])) 
        / sum(rate(http_server_requests_seconds_count{namespace="splitter"}[5m])) > 0.05
      for: 5m
      labels:
        severity: critical
      annotations:
        summary: "High error rate detected"
        description: "Error rate is above 5% for the last 5 minutes"

    - alert: HighLatency
      expr: |
        histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{namespace="splitter"}[5m])) by (le)) > 1
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High latency detected"
        description: "95th percentile latency is above 1 second"

    - alert: KafkaConsumerLag
      expr: |
        sum(kafka_consumer_fetch_manager_records_lag{namespace="splitter"}) by (client_id) > 1000
      for: 10m
      labels:
        severity: warning
      annotations:
        summary: "Kafka consumer lag is high"
        description: "Consumer {{ $labels.client_id }} has lag > 1000 for 10 minutes"

    - alert: PodNotReady
      expr: |
        kube_pod_status_ready{namespace="splitter", condition="true"} == 0
      for: 5m
      labels:
        severity: critical
      annotations:
        summary: "Pod not ready"
        description: "Pod {{ $labels.pod }} has been not ready for 5 minutes"

    - alert: HighMemoryUsage
      expr: |
        container_memory_usage_bytes{namespace="splitter"} 
        / container_spec_memory_limit_bytes{namespace="splitter"} > 0.9
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High memory usage"
        description: "Container {{ $labels.container }} memory usage is above 90%"
```

---

### 3.3.3 Distributed Tracing with Jaeger

**Jaeger Deployment:**
```yaml
# infrastructure/kubernetes/monitoring/jaeger/jaeger.yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: splitter-jaeger
  namespace: splitter
spec:
  strategy: production
  storage:
    type: elasticsearch
    options:
      es:
        server-urls: http://elasticsearch:9200
  ingress:
    enabled: true
```

**Spring Boot Tracing Configuration:**
```yaml
# application-production.yml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests in production
  zipkin:
    tracing:
      endpoint: http://splitter-jaeger-collector.splitter.svc.cluster.local:9411/api/v2/spans
```

---

### 3.3.4 Log Aggregation with Loki

**Loki Configuration:**
```yaml
# infrastructure/kubernetes/monitoring/loki/loki-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: loki-config
  namespace: monitoring
data:
  loki.yaml: |
    auth_enabled: false
    server:
      http_listen_port: 3100
    ingester:
      lifecycler:
        ring:
          kvstore:
            store: inmemory
          replication_factor: 1
    schema_config:
      configs:
        - from: 2020-10-24
          store: boltdb-shipper
          object_store: filesystem
          schema: v11
          index:
            prefix: index_
            period: 24h
    storage_config:
      boltdb_shipper:
        active_index_directory: /loki/index
        cache_location: /loki/cache
        shared_store: filesystem
      filesystem:
        directory: /loki/chunks
```

**Promtail DaemonSet (for log collection):**
```yaml
# infrastructure/kubernetes/monitoring/loki/promtail.yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: promtail
  namespace: monitoring
spec:
  selector:
    matchLabels:
      name: promtail
  template:
    metadata:
      labels:
        name: promtail
    spec:
      containers:
      - name: promtail
        image: grafana/promtail:2.9.0
        args:
          - -config.file=/etc/promtail/promtail.yaml
        volumeMounts:
        - name: config
          mountPath: /etc/promtail
        - name: varlog
          mountPath: /var/log
          readOnly: true
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
      volumes:
      - name: config
        configMap:
          name: promtail-config
      - name: varlog
        hostPath:
          path: /var/log
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
```

---

## Sprint 3.4: CI/CD Pipeline Enhancement (Week 12)

### 3.4.1 Enhanced GitHub Actions Workflow

**Production Deployment Workflow:**
```yaml
# .github/workflows/deploy-production.yml
name: Deploy to Production

on:
  push:
    tags:
      - 'v*'

env:
  REGISTRY: ghcr.io
  IMAGE_PREFIX: ${{ github.repository }}

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    strategy:
      matrix:
        service:
          - api-gateway
          - user-service
          - group-service
          - expense-service
          - balance-service
          - settlement-service
          - notification-service
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      
      - name: Build with Maven
        run: |
          cd shared && mvn clean install -DskipTests
          cd ../services/${{ matrix.service }} && mvn clean package -DskipTests
      
      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}
          tags: |
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha,prefix=
      
      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: services/${{ matrix.service }}
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up kubectl
        uses: azure/setup-kubectl@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1
      
      - name: Update kubeconfig
        run: aws eks update-kubeconfig --name splitter-production --region us-east-1
      
      - name: Deploy to Kubernetes
        run: |
          # Update image tags in manifests
          cd infrastructure/kubernetes
          kustomize edit set image "*:${{ github.ref_name }}"
          kubectl apply -k .
      
      - name: Wait for rollout
        run: |
          for service in api-gateway user-service group-service expense-service balance-service settlement-service notification-service; do
            kubectl rollout status deployment/$service -n splitter --timeout=5m
          done
      
      - name: Run smoke tests
        run: |
          GATEWAY_URL=$(kubectl get ingress splitter-ingress -n splitter -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
          curl -f https://$GATEWAY_URL/actuator/health || exit 1
      
      - name: Notify on success
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "✅ Splitter ${{ github.ref_name }} deployed to production successfully!"
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}

  rollback:
    needs: deploy
    if: failure()
    runs-on: ubuntu-latest
    steps:
      - name: Rollback deployment
        run: |
          for service in api-gateway user-service group-service expense-service balance-service settlement-service notification-service; do
            kubectl rollout undo deployment/$service -n splitter
          done
      
      - name: Notify on rollback
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "⚠️ Splitter ${{ github.ref_name }} deployment failed. Rolled back to previous version."
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

---

## Sprint 3.5: Security Hardening (Week 12)

### 3.5.1 Security Checklist

**Authentication & Authorization:**
- [ ] JWT tokens have appropriate expiration (15min access, 7d refresh)
- [ ] Refresh token rotation implemented
- [ ] Password requirements enforced (min 8 chars, complexity)
- [ ] Account lockout after failed attempts
- [ ] Secure password hashing (bcrypt with cost 12+)

**API Security:**
- [ ] Rate limiting on all endpoints
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (output encoding)
- [ ] CSRF protection for state-changing operations
- [ ] Proper CORS configuration

**Infrastructure Security:**
- [ ] All secrets in Kubernetes Secrets (not ConfigMaps)
- [ ] Network policies restricting pod communication
- [ ] Pod security policies/standards
- [ ] Container images scanned for vulnerabilities
- [ ] TLS everywhere (internal and external)

**Data Security:**
- [ ] Sensitive data encrypted at rest
- [ ] PII logged appropriately (masked)
- [ ] Database backups encrypted
- [ ] Audit logging for sensitive operations

**Network Policy:**
```yaml
# infrastructure/kubernetes/network-policies/default-deny.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: splitter
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress

---
# Allow internal service communication
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-internal
  namespace: splitter
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: splitter
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: splitter
  - to:  # Allow DNS
    - namespaceSelector: {}
      podSelector:
        matchLabels:
          k8s-app: kube-dns
    ports:
    - protocol: UDP
      port: 53
```

---

## Deliverables Summary

### Testing
- [ ] Playwright E2E test suite with 20+ test cases
- [ ] API contract tests for all service interactions
- [ ] k6 performance test scripts
- [ ] 80%+ code coverage on backend services

### Infrastructure
- [ ] Kubernetes manifests for all services
- [ ] Horizontal Pod Autoscalers configured
- [ ] Ingress with TLS termination
- [ ] Network policies for security

### Monitoring
- [ ] Prometheus ServiceMonitors
- [ ] Grafana dashboards (3+)
- [ ] Alerting rules (10+)
- [ ] Jaeger distributed tracing
- [ ] Loki log aggregation

### CI/CD
- [ ] Production deployment workflow
- [ ] Automated rollback on failure
- [ ] Slack notifications
- [ ] Smoke tests post-deployment

### Security
- [ ] Security audit completed
- [ ] Network policies enforced
- [ ] Secrets management configured
- [ ] Container image scanning

---

## Success Criteria

1. **All E2E tests pass** - 100% of critical user journeys automated and passing
2. **No P0/P1 bugs** - All critical and high-priority bugs resolved
3. **Performance targets met** - p95 latency < 500ms, error rate < 1%
4. **Monitoring coverage** - All services have dashboards and alerts
5. **Successful production deployment** - Application running in production
6. **Zero downtime deployment** - Rolling updates work correctly
7. **Security audit passed** - No critical vulnerabilities

---

## Timeline

| Day | Focus | Deliverables |
|-----|-------|--------------|
| Day 1-2 | E2E Test Setup | Playwright configured, auth tests |
| Day 3-4 | E2E Test Implementation | All critical flow tests |
| Day 5 | Performance Testing | k6 scripts, baseline metrics |
| Day 6-7 | Bug Fixes | P0/P1 bugs resolved |
| Day 8 | UX Polish | Loading states, error handling |
| Day 9-10 | Infrastructure | K8s manifests, monitoring |
| Day 11 | CI/CD | Production pipeline |
| Day 12 | Security & Deployment | Security hardening, go-live |

---

*This document should be updated as implementation progresses.*
