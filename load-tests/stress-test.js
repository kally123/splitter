import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Stress test configuration - push the system to its limits
export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '5m', target: 100 },   // Stay at 100 users
    { duration: '2m', target: 200 },   // Ramp up to 200 users
    { duration: '5m', target: 200 },   // Stay at 200 users
    { duration: '2m', target: 300 },   // Ramp up to 300 users
    { duration: '5m', target: 300 },   // Stay at 300 users
    { duration: '2m', target: 400 },   // Push to 400 users
    { duration: '5m', target: 400 },   // Stay at 400 users
    { duration: '5m', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // 95% < 2s under stress
    http_req_failed: ['rate<0.1'],       // Error rate < 10% under extreme load
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

export default function () {
  const email = `stress${__VU}@example.com`;
  const password = 'Password123!';

  // Try to login or register
  let loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: email,
    password: password,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginRes.status === 401) {
    // Register new user
    const registerRes = http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({
      email: email,
      password: password,
      displayName: `Stress Test User ${__VU}`,
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    loginRes = registerRes;
  }

  responseTime.add(loginRes.timings.duration);
  
  const success = check(loginRes, {
    'auth successful': (r) => r.status === 200 || r.status === 201,
  });

  if (!success) {
    errorRate.add(1);
    return;
  }

  const token = loginRes.json('accessToken');
  if (!token) return;

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // Simulate heavy read operations
  for (let i = 0; i < 5; i++) {
    const groupsRes = http.get(`${BASE_URL}/api/v1/groups`, { headers });
    responseTime.add(groupsRes.timings.duration);
    check(groupsRes, { 'groups ok': (r) => r.status === 200 }) || errorRate.add(1);

    const balancesRes = http.get(`${BASE_URL}/api/v1/balances`, { headers });
    responseTime.add(balancesRes.timings.duration);
    check(balancesRes, { 'balances ok': (r) => r.status === 200 }) || errorRate.add(1);

    sleep(0.5);
  }

  // Simulate write operations
  const expenseRes = http.post(`${BASE_URL}/api/v1/expenses`, JSON.stringify({
    description: `Stress test expense ${Date.now()}`,
    amount: Math.random() * 100,
    currency: 'USD',
    category: 'OTHER',
    splitType: 'EQUAL',
  }), { headers });

  responseTime.add(expenseRes.timings.duration);
  check(expenseRes, { 'expense created': (r) => r.status === 201 || r.status === 400 }) || errorRate.add(1);

  sleep(1);
}
