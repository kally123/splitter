import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Soak test configuration - sustained load over extended period
export const options = {
  stages: [
    { duration: '5m', target: 50 },    // Ramp up
    { duration: '4h', target: 50 },    // Stay at 50 users for 4 hours
    { duration: '5m', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const errorRate = new Rate('errors');
const memoryLeakIndicator = new Trend('response_time_over_time');

export default function () {
  const userIndex = (__VU % 10) + 1;
  const email = `soak${userIndex}@example.com`;
  const password = 'Password123!';

  // Login
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: email,
    password: password,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginRes.status !== 200) {
    // Register if not exists
    http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({
      email: email,
      password: password,
      displayName: `Soak Test User ${userIndex}`,
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    return;
  }

  const token = loginRes.json('accessToken');
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // Typical user session
  // 1. Check dashboard
  const dashboardRes = http.get(`${BASE_URL}/api/v1/balances`, { headers });
  memoryLeakIndicator.add(dashboardRes.timings.duration);
  check(dashboardRes, { 'dashboard ok': (r) => r.status === 200 }) || errorRate.add(1);

  sleep(2);

  // 2. View groups
  const groupsRes = http.get(`${BASE_URL}/api/v1/groups`, { headers });
  memoryLeakIndicator.add(groupsRes.timings.duration);
  check(groupsRes, { 'groups ok': (r) => r.status === 200 }) || errorRate.add(1);

  sleep(3);

  // 3. View expenses
  const expensesRes = http.get(`${BASE_URL}/api/v1/expenses`, { headers });
  memoryLeakIndicator.add(expensesRes.timings.duration);
  check(expensesRes, { 'expenses ok': (r) => r.status === 200 }) || errorRate.add(1);

  sleep(2);

  // 4. Check notifications
  const notificationsRes = http.get(`${BASE_URL}/api/v1/notifications`, { headers });
  memoryLeakIndicator.add(notificationsRes.timings.duration);
  check(notificationsRes, { 'notifications ok': (r) => r.status === 200 }) || errorRate.add(1);

  sleep(5);
}
