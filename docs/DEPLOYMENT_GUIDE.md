# Phase 5 Production Readiness - Deployment Guide

## Overview

This document provides step-by-step instructions for deploying the Splitter application to production using the infrastructure created in Phase 5.

## Prerequisites

- Kubernetes cluster (EKS, GKE, or AKS)
- `kubectl` configured for cluster access
- Helm 3.x installed
- Docker registry access (GitHub Container Registry)
- AWS/GCP/Azure credentials for cloud resources

## Pre-Deployment Checklist

### 1. Security Configuration

- [ ] Generate production JWT secrets
- [ ] Configure Stripe production keys
- [ ] Set up AWS credentials for Receipt Service
- [ ] Configure SMTP credentials for notifications
- [ ] Review and update `.dependency-check-suppression.xml`

### 2. Database Setup

```bash
# Apply performance indexes
psql -h <db-host> -U postgres -f infrastructure/docker/init-scripts/performance-indexes.sql
```

### 3. Secret Management

Create secrets in AWS Secrets Manager or configure External Secrets Operator:

```bash
# Install External Secrets Operator
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace

# Create ClusterSecretStore
kubectl apply -f infrastructure/kubernetes/base/secrets.yaml
```

## Deployment Steps

### Step 1: Deploy Observability Stack

```bash
cd infrastructure/observability
docker-compose -f docker-compose.observability.yml up -d
```

### Step 2: Deploy to Kubernetes

```bash
# Staging
kubectl apply -k infrastructure/kubernetes/overlays/staging

# Production (after staging validation)
kubectl apply -k infrastructure/kubernetes/overlays/production
```

### Step 3: Verify Deployment

```bash
# Check pod status
kubectl get pods -n splitter

# Check services
kubectl get svc -n splitter

# Check ingress
kubectl get ingress -n splitter

# Verify rollout status
kubectl-argo-rollouts status api-gateway-rollout -n splitter
```

### Step 4: Run Load Tests

```bash
cd load-tests

# Start load test infrastructure
docker-compose -f docker-compose.loadtest.yml up -d

# Run smoke test first
docker-compose -f docker-compose.loadtest.yml --profile run run --rm k6 run /scripts/scenarios/mixed-workload.js --env TEST_TYPE=smoke

# Run full load test
docker-compose -f docker-compose.loadtest.yml --profile run run --rm k6 run /scripts/scenarios/mixed-workload.js --env TEST_TYPE=load
```

## Monitoring & Alerting

### Access Dashboards

| Service | URL | Description |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | Metrics dashboards |
| Jaeger | http://localhost:16686 | Distributed traces |
| Prometheus | http://localhost:9090 | Raw metrics |
| Alertmanager | http://localhost:9093 | Alert management |

### Key Dashboards

1. **Splitter - Overview**: High-level system health
2. **Splitter - Business Metrics**: KPIs and business metrics
3. **JVM Dashboard**: Service memory and GC metrics

### Alert Configuration

Alerts are defined in `infrastructure/prometheus/alert-rules.yml`. Key alerts:

| Alert | Severity | Description |
|-------|----------|-------------|
| ServiceDown | Critical | Service instance unreachable |
| HighErrorRate | Warning | Error rate > 1% |
| CriticalErrorRate | Critical | Error rate > 5% |
| HighLatencyP95 | Warning | P95 latency > 200ms |
| PaymentFailureRateHigh | Critical | Payment failures > 10% |

## Canary Deployment

The API Gateway uses Argo Rollouts for canary deployments:

```bash
# View rollout status
kubectl-argo-rollouts get rollout api-gateway-rollout -n splitter

# Promote canary (manual)
kubectl-argo-rollouts promote api-gateway-rollout -n splitter

# Abort rollout
kubectl-argo-rollouts abort api-gateway-rollout -n splitter
```

### Canary Strategy

1. 5% traffic → Wait 5 min
2. 20% traffic → Wait 5 min → Run analysis
3. 50% traffic → Wait 10 min → Run analysis
4. 100% traffic → Complete

## Troubleshooting

### Common Issues

#### Pods Not Starting

```bash
# Check pod events
kubectl describe pod <pod-name> -n splitter

# Check logs
kubectl logs <pod-name> -n splitter
```

#### Database Connection Issues

```bash
# Verify database connectivity
kubectl exec -it <pod-name> -n splitter -- pg_isready -h <db-host>
```

#### High Latency

1. Check Grafana latency dashboard
2. View traces in Jaeger for slow requests
3. Check database query performance in PostgreSQL
4. Verify Redis cache hit rate

### Rollback Procedure

```bash
# Kubernetes deployment rollback
kubectl rollout undo deployment/<service-name> -n splitter

# Argo Rollouts rollback
kubectl-argo-rollouts abort api-gateway-rollout -n splitter
kubectl-argo-rollouts rollback api-gateway-rollout -n splitter
```

## CI/CD Integration

### GitHub Actions Workflows

1. **ci-cd.yml**: Build, test, and deploy pipeline
2. **security-scanning.yml**: Security scans (SAST, DAST, container scanning)

### Required Secrets

Configure in GitHub repository settings:

```
KUBE_CONFIG_STAGING    - Kubernetes config for staging
KUBE_CONFIG_PRODUCTION - Kubernetes config for production
SLACK_WEBHOOK          - Slack notification webhook
```

## Performance Benchmarks

### Expected Performance

| Metric | Target | Measured |
|--------|--------|----------|
| P95 Latency | < 200ms | - |
| P99 Latency | < 500ms | - |
| Error Rate | < 1% | - |
| Balance Query P95 | < 100ms | - |
| Expense Create P95 | < 300ms | - |

### Scaling Guidelines

| Service | Min Replicas | Max Replicas | CPU Threshold |
|---------|--------------|--------------|---------------|
| api-gateway | 3 | 10 | 70% |
| expense-service | 3 | 10 | 70% |
| balance-service | 3 | 15 | 60% |
| user-service | 2 | 5 | 70% |

## Maintenance

### Daily Tasks

- Review Grafana dashboards for anomalies
- Check Alertmanager for unresolved alerts
- Monitor error logs in Loki

### Weekly Tasks

- Review slow query logs
- Check database index usage
- Review security scan reports

### Monthly Tasks

- Update dependencies (security patches)
- Review and rotate secrets
- Capacity planning review
