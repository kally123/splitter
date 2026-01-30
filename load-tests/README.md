# Splitter Load Testing

This directory contains load testing infrastructure using [k6](https://k6.io/).

## Quick Start

### Prerequisites
- [k6](https://k6.io/docs/getting-started/installation/) installed locally
- Docker & Docker Compose (for metrics visualization)
- Splitter API running locally or accessible

### Running Tests Locally

```bash
# Smoke test (quick validation)
k6 run --env TEST_TYPE=smoke k6/scenarios/mixed-workload.js

# Load test (standard load)
k6 run --env TEST_TYPE=load k6/scenarios/expense-crud.js

# Stress test (find breaking point)
k6 run --env TEST_TYPE=stress k6/scenarios/balance-queries.js

# Against a specific environment
k6 run --env BASE_URL=https://api.staging.splitter.app k6/scenarios/auth-flow.js
```

### Running with Metrics Visualization

1. Start InfluxDB and Grafana:
```bash
docker-compose -f docker-compose.loadtest.yml up -d influxdb grafana
```

2. Run k6 with InfluxDB output:
```bash
k6 run --out influxdb=http://localhost:8086/k6 k6/scenarios/mixed-workload.js
```

3. Open Grafana at http://localhost:3001 to view results

### Running Containerized Tests

```bash
# Run full load test suite
docker-compose -f docker-compose.loadtest.yml --profile run up k6
```

## Test Scenarios

| Scenario | Description | Key Metrics |
|----------|-------------|-------------|
| `auth-flow.js` | Login, token refresh, protected access | Login latency, token errors |
| `expense-crud.js` | Create, read, update, delete expenses | CRUD latency, error rates |
| `balance-queries.js` | Balance calculations, simplified debts | Query latency, cache effectiveness |
| `mixed-workload.js` | Realistic user behavior mix | Page load times, transaction success |

## Test Types

| Type | Purpose | Duration | Max VUs |
|------|---------|----------|---------|
| `smoke` | Sanity check | 2 min | 5 |
| `load` | Normal load | 16 min | 100 |
| `stress` | Find limits | 26 min | 300 |
| `spike` | Sudden traffic | 5 min | 500 |
| `soak` | Long duration | 4+ hours | 100 |

## Thresholds

Default pass/fail thresholds:
- **p95 latency**: < 200ms
- **p99 latency**: < 500ms
- **Error rate**: < 1%
- **Balance queries**: < 100ms p95
- **Expense creation**: < 300ms p95

## Test Data Setup

Before running tests, ensure test users exist:

```sql
-- Create test users in user-service database
INSERT INTO users (email, password_hash, display_name) VALUES
('loadtest1@splitter.test', '$2a$10$...', 'Load Test User 1'),
('loadtest2@splitter.test', '$2a$10$...', 'Load Test User 2'),
-- ... etc
```

## CI Integration

Add to GitHub Actions:
```yaml
- name: Run Load Tests
  run: |
    k6 run --env BASE_URL=${{ secrets.STAGING_API_URL }} \
           --env TEST_TYPE=load \
           --out json=results.json \
           load-tests/k6/scenarios/mixed-workload.js
```

## Interpreting Results

### Good Results
- All thresholds pass (✓)
- Consistent latency across the test
- Error rate stays near 0%

### Warning Signs
- Latency increases over time (memory leak?)
- Error rate spikes at higher load
- Timeouts during peak load

### Action Items
- If balance queries slow: Check Redis cache, add indexes
- If expense creation slow: Check split calculation, database locks
- If auth slow: Check token generation, session storage
