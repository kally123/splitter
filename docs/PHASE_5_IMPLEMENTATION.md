# Phase 5: Production Readiness Implementation Plan

## Overview
**Duration:** Week 19-20  
**Focus:** Performance optimization, security hardening, observability, and production go-live  
**Target:** 500+ concurrent users, <100ms p95 latency, zero critical vulnerabilities

---

## 📊 Sprint 5.1: Performance Optimization (Week 19)

### Task 5.1.1: Load Testing Infrastructure

| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | All services deployed to staging |

**Deliverables:**
- [ ] Set up k6 load testing framework
- [ ] Create test scenarios for critical paths
- [ ] Configure InfluxDB + Grafana for results visualization
- [ ] Document baseline metrics

**Files to Create:**
```
load-tests/
├── k6/
│   ├── config.js
│   ├── scenarios/
│   │   ├── auth-flow.js
│   │   ├── expense-crud.js
│   │   ├── group-operations.js
│   │   ├── balance-queries.js
│   │   └── mixed-workload.js
│   └── utils/
│       ├── api-client.js
│       └── data-generators.js
├── docker-compose.loadtest.yml
└── README.md
```

**Test Scenarios:**
```javascript
// scenarios/expense-crud.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up
    { duration: '5m', target: 100 },   // Steady state
    { duration: '2m', target: 200 },   // Stress test
    { duration: '5m', target: 200 },   // Sustained load
    { duration: '2m', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'],  // 95% under 200ms
    http_req_failed: ['rate<0.01'],    // <1% errors
  },
};

export default function() {
  // Create expense
  const createRes = http.post(`${BASE_URL}/api/v1/expenses`, JSON.stringify({
    description: 'Load test expense',
    amount: 100.00,
    groupId: GROUP_ID,
    splitType: 'EQUAL',
  }), { headers: { 'Authorization': `Bearer ${TOKEN}` }});
  
  check(createRes, { 'expense created': (r) => r.status === 201 });
  sleep(1);
}
```

**Acceptance Criteria:**
- Load tests run in CI pipeline
- Results visualized in Grafana
- Baseline established for all endpoints

---

### Task 5.1.2: Database Query Optimization

| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer / DBA |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Load test results |

**Deliverables:**
- [ ] Analyze slow query logs
- [ ] Add missing indexes
- [ ] Optimize N+1 queries
- [ ] Implement query result caching
- [ ] Configure connection pooling

**Optimization Areas:**

```sql
-- 1. Expense queries - Add composite indexes
CREATE INDEX CONCURRENTLY idx_expenses_group_created 
ON expenses(group_id, created_at DESC);

CREATE INDEX CONCURRENTLY idx_expenses_payer_created 
ON expenses(paid_by, created_at DESC);

CREATE INDEX CONCURRENTLY idx_expense_shares_user_expense 
ON expense_shares(user_id, expense_id);

-- 2. Balance calculations - Materialized view
CREATE MATERIALIZED VIEW mv_user_balances AS
SELECT 
    es.user_id,
    e.group_id,
    SUM(CASE WHEN e.paid_by = es.user_id THEN e.amount - es.share_amount 
             ELSE -es.share_amount END) as net_balance
FROM expense_shares es
JOIN expenses e ON es.expense_id = e.id
WHERE e.deleted_at IS NULL
GROUP BY es.user_id, e.group_id;

CREATE UNIQUE INDEX idx_mv_user_balances 
ON mv_user_balances(user_id, group_id);

-- 3. Refresh strategy
CREATE OR REPLACE FUNCTION refresh_user_balances()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_user_balances;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 4. Query optimization examples
-- Before: N+1 query pattern
-- After: Single query with JOIN
SELECT e.*, 
       json_agg(json_build_object(
           'userId', es.user_id,
           'shareAmount', es.share_amount
       )) as shares
FROM expenses e
LEFT JOIN expense_shares es ON e.id = es.expense_id
WHERE e.group_id = $1
GROUP BY e.id
ORDER BY e.created_at DESC
LIMIT 20;
```

**Connection Pool Configuration:**
```yaml
# application.yml
spring:
  r2dbc:
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m
      validation-query: SELECT 1
```

**Acceptance Criteria:**
- No queries > 100ms under load
- N+1 queries eliminated
- Connection pool sized appropriately

---

### Task 5.1.3: Redis Caching Strategy

| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | common-cache module |

**Deliverables:**
- [ ] Implement cache-aside pattern for read-heavy data
- [ ] Configure cache TTLs per data type
- [ ] Implement cache warming on startup
- [ ] Add cache hit/miss metrics
- [ ] Configure Redis cluster for HA

**Cache Strategy by Data Type:**

| Data Type | TTL | Strategy | Invalidation |
|-----------|-----|----------|--------------|
| Exchange rates | 1 hour | Cache-aside | Time-based |
| User profiles | 15 min | Cache-aside | Event-driven |
| Group details | 10 min | Cache-aside | Event-driven |
| Balance summaries | 5 min | Write-through | On expense/settlement |
| Session data | 24 hours | Write-through | On logout |

**Implementation:**
```java
@Service
public class CachedBalanceService {
    
    private static final Duration BALANCE_TTL = Duration.ofMinutes(5);
    
    private final MultiLevelCacheService cache;
    private final BalanceCalculator calculator;
    
    public Mono<UserBalance> getUserBalance(UUID userId) {
        String cacheKey = "balance:user:" + userId;
        
        return cache.get(cacheKey, UserBalance.class, 
            Duration.ofMinutes(1),  // L1 TTL
            BALANCE_TTL,            // L2 TTL
            () -> calculator.calculateUserBalance(userId)
        );
    }
    
    @EventListener
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        // Invalidate affected user balances
        event.getParticipantIds().forEach(userId -> 
            cache.evict("balance:user:" + userId)
        );
        cache.evict("balance:group:" + event.getGroupId());
    }
}
```

**Redis Cluster Configuration:**
```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-node-1:6379
          - redis-node-2:6379
          - redis-node-3:6379
        max-redirects: 3
      lettuce:
        pool:
          max-active: 50
          max-idle: 10
          min-idle: 5
```

**Acceptance Criteria:**
- Cache hit rate > 80% for read-heavy endpoints
- Balance queries < 10ms with cache hit
- No stale data after invalidation events

---

### Task 5.1.4: API Response Optimization

| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | None |

**Deliverables:**
- [ ] Enable HTTP compression (gzip/brotli)
- [ ] Implement response pagination
- [ ] Add ETag support for caching
- [ ] Configure keep-alive connections
- [ ] Optimize JSON serialization

**Implementation:**
```java
@Configuration
public class WebOptimizationConfig {
    
    @Bean
    public WebFilter compressionFilter() {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add("Content-Encoding", "gzip");
            return chain.filter(exchange);
        };
    }
    
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        return new Jackson2ObjectMapperBuilder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .modules(new JavaTimeModule());
    }
}

// ETag support for expense lists
@GetMapping("/groups/{groupId}/expenses")
public Mono<ResponseEntity<PagedResponse<ExpenseDto>>> getExpenses(
        @PathVariable UUID groupId,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
    
    return expenseService.getExpensesByGroup(groupId)
        .collectList()
        .map(expenses -> {
            String etag = calculateETag(expenses);
            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).build();
            }
            return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                .body(new PagedResponse<>(expenses));
        });
}
```

**Acceptance Criteria:**
- Response sizes reduced by 60%+ with compression
- ETag reduces redundant data transfer
- API response times improved by 20%

---

### Task 5.1.5: Async Processing & Background Jobs

| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | None |

**Deliverables:**
- [ ] Move non-critical operations to async
- [ ] Implement job queue for heavy processing
- [ ] Add dead letter queue handling
- [ ] Configure retry policies

**Async Operations:**
```java
@Service
public class AsyncNotificationService {
    
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendNotifications(ExpenseCreatedEvent event) {
        return CompletableFuture.runAsync(() -> {
            event.getParticipantIds().stream()
                .filter(id -> !id.equals(event.getPaidBy()))
                .forEach(userId -> {
                    NotificationEvent notification = NotificationEvent.builder()
                        .userId(userId)
                        .type(NotificationType.EXPENSE_ADDED)
                        .data(Map.of(
                            "expenseId", event.getExpenseId(),
                            "amount", event.getAmount(),
                            "description", event.getDescription()
                        ))
                        .build();
                    kafkaTemplate.send("notification.events", notification);
                });
        });
    }
}

@Configuration
public class AsyncConfig {
    
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

**Acceptance Criteria:**
- Expense creation response time < 100ms
- Notifications sent within 5 seconds
- Failed jobs retried with backoff

---

## 🔒 Sprint 5.2: Security Hardening (Week 19-20)

### Task 5.2.1: OWASP Top 10 Review

| Field | Details |
|-------|---------|
| **Assignee** | Tech Lead / Security Engineer |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | None |

**Deliverables:**
- [ ] Review and mitigate each OWASP Top 10 vulnerability
- [ ] Document security controls
- [ ] Create security testing checklist

**OWASP Top 10 Checklist:**

| # | Vulnerability | Status | Mitigation |
|---|---------------|--------|------------|
| A01 | Broken Access Control | ⬜ | Role-based access, resource ownership checks |
| A02 | Cryptographic Failures | ⬜ | TLS 1.3, bcrypt passwords, encrypted secrets |
| A03 | Injection | ⬜ | Parameterized queries, input validation |
| A04 | Insecure Design | ⬜ | Threat modeling, security requirements |
| A05 | Security Misconfiguration | ⬜ | Security headers, disable debug endpoints |
| A06 | Vulnerable Components | ⬜ | Dependency scanning, regular updates |
| A07 | Auth Failures | ⬜ | MFA support, rate limiting, secure sessions |
| A08 | Software Integrity | ⬜ | Signed artifacts, integrity checks |
| A09 | Logging Failures | ⬜ | Audit logging, log injection prevention |
| A10 | SSRF | ⬜ | URL validation, network segmentation |

**Implementation Examples:**

```java
// A01: Broken Access Control
@PreAuthorize("@securityService.isGroupMember(#groupId, authentication.principal)")
@GetMapping("/groups/{groupId}/expenses")
public Flux<ExpenseDto> getGroupExpenses(@PathVariable UUID groupId) {
    return expenseService.getExpensesByGroup(groupId);
}

// A03: Injection Prevention
@Repository
public interface ExpenseRepository extends ReactiveCrudRepository<Expense, UUID> {
    // Use parameterized queries only
    @Query("SELECT * FROM expenses WHERE group_id = :groupId AND description ILIKE :search")
    Flux<Expense> searchByDescription(UUID groupId, String search);
}

// A07: Rate Limiting
@Component
public class AuthenticationRateLimiter {
    
    private final RateLimitService rateLimitService;
    
    public boolean isLoginAllowed(String email) {
        // 5 attempts per 15 minutes
        return rateLimitService.isAllowed("auth:login:" + email, 5, 900);
    }
    
    public boolean isPasswordResetAllowed(String email) {
        // 3 attempts per hour
        return rateLimitService.isAllowed("auth:reset:" + email, 3, 3600);
    }
}
```

**Acceptance Criteria:**
- All OWASP Top 10 items addressed
- Security controls documented
- No critical/high vulnerabilities

---

### Task 5.2.2: Security Headers Configuration

| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer |
| **Duration** | 0.5 day |
| **Priority** | P0 - Critical |
| **Dependencies** | None |

**Deliverables:**
- [ ] Configure all security headers
- [ ] Implement CSP policy
- [ ] Test with security scanners

**Implementation:**
```java
@Configuration
@EnableWebFluxSecurity
public class SecurityHeadersConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .headers(headers -> headers
                // Prevent clickjacking
                .frameOptions(frame -> frame.deny())
                // Prevent MIME sniffing
                .contentTypeOptions(Customizer.withDefaults())
                // XSS Protection
                .xssProtection(xss -> xss.headerValue(XXssProtectionServerHttpHeadersWriter.HeaderValue.ENABLED_MODE_BLOCK))
                // HSTS
                .hsts(hsts -> hsts
                    .includeSubdomains(true)
                    .maxAge(Duration.ofDays(365))
                    .preload(true))
                // CSP
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: https:; " +
                        "connect-src 'self' https://api.stripe.com; " +
                        "frame-ancestors 'none'; " +
                        "form-action 'self';"))
                // Referrer Policy
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Permissions Policy
                .permissionsPolicy(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=(self), payment=(self)"))
            )
            .build();
    }
}
```

**Expected Headers:**
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'; ...
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=(self)
```

**Acceptance Criteria:**
- All headers present in responses
- CSP doesn't break functionality
- A+ rating on securityheaders.com

---

### Task 5.2.3: Dependency Vulnerability Scanning

| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | None |

**Deliverables:**
- [ ] Set up OWASP Dependency Check
- [ ] Integrate Snyk/Trivy in CI
- [ ] Create vulnerability remediation SLA
- [ ] Document update procedures

**CI Pipeline Integration:**
```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on:
  push:
    branches: [main, develop]
  pull_request:
  schedule:
    - cron: '0 6 * * *'  # Daily at 6 AM

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'splitter'
          path: '.'
          format: 'HTML'
          args: >
            --failOnCVSS 7
            --enableRetired
            
      - name: Upload Report
        uses: actions/upload-artifact@v4
        with:
          name: dependency-check-report
          path: reports/

  container-scan:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [user-service, group-service, expense-service, balance-service]
    steps:
      - uses: actions/checkout@v4
      
      - name: Build Image
        run: docker build -t splitter/${{ matrix.service }}:scan ./services/${{ matrix.service }}
        
      - name: Trivy Scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'splitter/${{ matrix.service }}:scan'
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          
      - name: Upload Trivy Results
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-results.sarif'

  secret-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
          
      - name: Gitleaks Scan
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**Vulnerability SLA:**
| Severity | Remediation Time |
|----------|------------------|
| Critical | 24 hours |
| High | 7 days |
| Medium | 30 days |
| Low | 90 days |

**Acceptance Criteria:**
- No critical/high vulnerabilities in dependencies
- Automated scanning in CI
- Clear remediation process

---

### Task 5.2.4: Penetration Testing

| Field | Details |
|-------|---------|
| **Assignee** | External Security Firm / Tech Lead |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 5.2.1, 5.2.2 |

**Deliverables:**
- [ ] Conduct penetration testing (internal or external)
- [ ] Document findings with severity ratings
- [ ] Remediate critical/high findings
- [ ] Retest after remediation

**Testing Scope:**
1. **Authentication & Authorization**
   - Session management
   - JWT token security
   - Password policies
   - Account lockout

2. **API Security**
   - IDOR (Insecure Direct Object References)
   - Mass assignment
   - Rate limiting bypass
   - API versioning

3. **Input Validation**
   - SQL injection
   - XSS (stored/reflected)
   - Command injection
   - Path traversal

4. **Business Logic**
   - Balance manipulation
   - Split calculation tampering
   - Payment bypass
   - Race conditions

**Testing Checklist:**
```markdown
## Authentication
- [ ] Brute force protection works
- [ ] Session tokens are secure (HttpOnly, Secure, SameSite)
- [ ] Password reset tokens expire
- [ ] JWT signature validation

## Authorization
- [ ] Users can only access their data
- [ ] Group members only see group expenses
- [ ] Admin-only operations enforced
- [ ] Cannot modify other users' expenses

## Input Validation
- [ ] SQL injection in search fields
- [ ] XSS in expense descriptions
- [ ] Amount field accepts only valid numbers
- [ ] File upload validation (receipts)

## Business Logic
- [ ] Cannot create expense in group you're not a member of
- [ ] Cannot settle more than owed
- [ ] Split amounts must equal total
- [ ] Currency conversion accuracy
```

**Acceptance Criteria:**
- All critical findings remediated
- High findings remediated or risk accepted
- Retest confirms fixes

---

### Task 5.2.5: Audit Logging

| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | None |

**Deliverables:**
- [ ] Implement comprehensive audit logging
- [ ] Log security-relevant events
- [ ] Protect logs from tampering
- [ ] Configure log retention

**Implementation:**
```java
@Aspect
@Component
public class AuditLogAspect {
    
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    
    @AfterReturning(
        pointcut = "@annotation(auditable)",
        returning = "result"
    )
    public void logAuditEvent(JoinPoint joinPoint, Auditable auditable, Object result) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : "anonymous";
        
        AuditEvent event = AuditEvent.builder()
            .timestamp(Instant.now())
            .userId(userId)
            .action(auditable.action())
            .resourceType(auditable.resourceType())
            .resourceId(extractResourceId(joinPoint, result))
            .clientIp(getClientIp())
            .userAgent(getUserAgent())
            .success(true)
            .build();
            
        auditLog.info("{}", objectMapper.writeValueAsString(event));
    }
}

// Usage
@Auditable(action = "CREATE", resourceType = "EXPENSE")
public Mono<Expense> createExpense(CreateExpenseRequest request) {
    // ...
}
```

**Audit Events to Log:**
| Category | Events |
|----------|--------|
| Authentication | Login, logout, failed login, password change |
| Authorization | Access denied, permission changes |
| Data | Create, update, delete of expenses, groups, settlements |
| Admin | User management, system configuration |
| Security | Rate limit triggered, suspicious activity |

**Log Format:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "action": "CREATE",
  "resourceType": "EXPENSE",
  "resourceId": "123e4567-e89b-12d3-a456-426614174000",
  "clientIp": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "success": true,
  "metadata": {
    "groupId": "789...",
    "amount": 100.00
  }
}
```

**Acceptance Criteria:**
- All security events logged
- Logs cannot be modified
- 90-day retention configured
- Logs searchable in ELK/Loki

---

## 📊 Sprint 5.3: Observability & Monitoring (Week 20)

### Task 5.3.1: Distributed Tracing

| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | None |

**Deliverables:**
- [ ] Implement OpenTelemetry tracing
- [ ] Configure Jaeger/Zipkin
- [ ] Add custom spans for critical operations
- [ ] Trace Kafka message flows

**Implementation:**
```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests
  otlp:
    tracing:
      endpoint: http://jaeger:4317

spring:
  application:
    name: expense-service
```

```java
@Configuration
public class TracingConfig {
    
    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }
}

@Service
public class ExpenseService {
    
    private final ObservationRegistry observationRegistry;
    
    public Mono<Expense> createExpense(CreateExpenseRequest request) {
        return Observation.createNotStarted("expense.create", observationRegistry)
            .lowCardinalityKeyValue("splitType", request.getSplitType().name())
            .observe(() -> doCreateExpense(request));
    }
}
```

**Acceptance Criteria:**
- End-to-end traces visible across services
- Kafka message correlation works
- < 5% performance overhead

---

### Task 5.3.2: Custom Metrics & Dashboards

| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | Prometheus setup |

**Deliverables:**
- [ ] Define business metrics
- [ ] Create Grafana dashboards
- [ ] Configure alerting rules
- [ ] Document runbooks

**Business Metrics:**
```java
@Component
public class BusinessMetrics {
    
    private final MeterRegistry registry;
    
    // Expense metrics
    private final Counter expensesCreated;
    private final DistributionSummary expenseAmount;
    private final Timer expenseProcessingTime;
    
    // Balance metrics
    private final Gauge totalOutstandingBalance;
    private final Counter settlementsRecorded;
    
    // User metrics
    private final Counter usersRegistered;
    private final Gauge activeUsers;
    
    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        this.expensesCreated = Counter.builder("splitter.expenses.created")
            .description("Total expenses created")
            .tags("service", "expense-service")
            .register(registry);
            
        this.expenseAmount = DistributionSummary.builder("splitter.expense.amount")
            .description("Expense amount distribution")
            .baseUnit("usd")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
    
    public void recordExpenseCreated(BigDecimal amount, String currency) {
        expensesCreated.increment();
        expenseAmount.record(amount.doubleValue());
    }
}
```

**Grafana Dashboards:**
1. **Service Health Dashboard**
   - Request rate, error rate, latency (RED metrics)
   - JVM metrics (heap, GC, threads)
   - Database connection pool
   - Kafka consumer lag

2. **Business Metrics Dashboard**
   - Expenses created per hour/day
   - Average expense amount
   - Settlements recorded
   - Active users
   - Groups created

3. **Infrastructure Dashboard**
   - CPU/Memory usage per service
   - Database connections
   - Redis hit rate
   - Kafka throughput

**Alert Rules:**
```yaml
# prometheus-rules.yml
groups:
  - name: splitter-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate on {{ $labels.service }}"
          
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High p95 latency on {{ $labels.service }}"
          
      - alert: KafkaConsumerLag
        expr: kafka_consumer_lag > 10000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High Kafka consumer lag"
          
      - alert: DatabaseConnectionExhaustion
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool nearly exhausted"
```

**Acceptance Criteria:**
- All services have metrics exposed
- Dashboards show real-time data
- Alerts configured and tested

---

### Task 5.3.3: Error Tracking & APM

| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer |
| **Duration** | 0.5 day |
| **Priority** | P1 - High |
| **Dependencies** | None |

**Deliverables:**
- [ ] Integrate Sentry for error tracking
- [ ] Configure error grouping
- [ ] Set up release tracking
- [ ] Add user context to errors

**Implementation:**
```java
@Configuration
public class SentryConfig {
    
    @Bean
    public SentryOptions sentryOptions() {
        SentryOptions options = new SentryOptions();
        options.setDsn("${SENTRY_DSN}");
        options.setEnvironment("${ENVIRONMENT}");
        options.setRelease("${GIT_COMMIT_SHA}");
        options.setTracesSampleRate(0.1);
        options.setBeforeSend((event, hint) -> {
            // Scrub sensitive data
            if (event.getRequest() != null) {
                event.getRequest().setHeaders(null);
                event.getRequest().setCookies(null);
            }
            return event;
        });
        return options;
    }
}

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, ServerWebExchange exchange) {
        // Capture to Sentry
        Sentry.withScope(scope -> {
            scope.setUser(getCurrentUser());
            scope.setTag("endpoint", exchange.getRequest().getPath().value());
            Sentry.captureException(ex);
        });
        
        return ResponseEntity.status(500)
            .body(new ErrorResponse("Internal server error", null));
    }
}
```

**Acceptance Criteria:**
- All unhandled exceptions captured
- Errors grouped by root cause
- User context available for debugging

---

## 🚀 Sprint 5.4: Production Deployment (Week 20)

### Task 5.4.1: Production Readiness Checklist

| Field | Details |
|-------|---------|
| **Assignee** | Tech Lead |
| **Duration** | 0.5 day |
| **Priority** | P0 - Critical |
| **Dependencies** | All previous tasks |

**Checklist:**
```markdown
## Infrastructure
- [ ] Production Kubernetes cluster deployed
- [ ] Database high availability configured
- [ ] Redis cluster with persistence
- [ ] Kafka cluster with replication
- [ ] CDN configured for static assets
- [ ] SSL certificates installed
- [ ] DNS configured

## Application
- [ ] All services passing health checks
- [ ] Database migrations applied
- [ ] Feature flags configured
- [ ] Environment variables set
- [ ] Secrets in vault/secrets manager

## Security
- [ ] No critical vulnerabilities
- [ ] Security headers configured
- [ ] Rate limiting enabled
- [ ] Audit logging enabled
- [ ] Penetration testing passed

## Monitoring
- [ ] All dashboards working
- [ ] Alerts configured and tested
- [ ] On-call rotation set up
- [ ] Runbooks documented
- [ ] Error tracking enabled

## Operations
- [ ] Backup/restore tested
- [ ] Disaster recovery plan documented
- [ ] Rollback procedures tested
- [ ] Incident response plan ready
- [ ] Support escalation path defined

## Documentation
- [ ] API documentation published
- [ ] Architecture diagrams updated
- [ ] Operational runbooks complete
- [ ] User documentation ready
```

---

### Task 5.4.2: Blue-Green Deployment Setup

| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | Kubernetes setup |

**Deliverables:**
- [ ] Configure blue-green deployment strategy
- [ ] Create deployment pipeline
- [ ] Test rollback procedure
- [ ] Document deployment process

**Kubernetes Deployment Strategy:**
```yaml
# deployment.yml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: expense-service
spec:
  replicas: 3
  strategy:
    blueGreen:
      activeService: expense-service-active
      previewService: expense-service-preview
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
          - templateName: success-rate
        args:
          - name: service-name
            value: expense-service
  template:
    spec:
      containers:
        - name: expense-service
          image: splitter/expense-service:{{ .Values.image.tag }}
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
```

**Acceptance Criteria:**
- Zero-downtime deployments
- Automatic rollback on failure
- Preview environment for validation

---

### Task 5.4.3: Go-Live

| Field | Details |
|-------|---------|
| **Assignee** | Entire Team |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | All previous tasks |

**Go-Live Procedure:**
1. **Pre-Launch (T-4 hours)**
   - [ ] Final production backup
   - [ ] Verify all monitoring is active
   - [ ] Confirm on-call team availability
   - [ ] Review rollback procedure

2. **Launch (T-0)**
   - [ ] Deploy to production
   - [ ] Run smoke tests
   - [ ] Verify health checks
   - [ ] Monitor error rates

3. **Post-Launch (T+1 hour)**
   - [ ] Review metrics dashboards
   - [ ] Check error tracking
   - [ ] Verify external integrations
   - [ ] Test critical user flows

4. **Stabilization (T+24 hours)**
   - [ ] Monitor for issues
   - [ ] Address any P0/P1 bugs
   - [ ] Gather user feedback
   - [ ] Performance baseline established

**Acceptance Criteria:**
- Application accessible to users
- No critical errors
- Performance meets targets
- Team can respond to issues

---

## 📈 Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **P95 Latency** | < 200ms | Prometheus |
| **Error Rate** | < 0.1% | Prometheus |
| **Uptime** | 99.9% | External monitoring |
| **Cache Hit Rate** | > 80% | Redis metrics |
| **Security Scan** | 0 critical/high | Dependency check |
| **Load Capacity** | 500 concurrent users | k6 tests |

---

## 🎯 Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Performance degradation | Load testing, caching, query optimization |
| Security vulnerability | OWASP review, pentesting, dependency scanning |
| Data loss | Backups, replication, disaster recovery |
| Deployment failure | Blue-green deployment, automatic rollback |
| Monitoring blind spots | Comprehensive observability, tracing |

---

## 📅 Timeline Summary

| Week | Focus | Key Deliverables |
|------|-------|------------------|
| Week 19 (Days 1-3) | Performance | Load tests, query optimization, caching |
| Week 19 (Days 4-5) | Security | OWASP review, headers, dependency scan |
| Week 20 (Days 1-2) | Security | Penetration testing, audit logging |
| Week 20 (Days 3-4) | Observability | Tracing, metrics, dashboards |
| Week 20 (Day 5) | Go-Live | Production deployment, monitoring |

---

*This plan should be executed with daily standups and immediate escalation of blockers.*
