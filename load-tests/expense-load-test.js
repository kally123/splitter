import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const loginDuration = new Trend('login_duration');
const createExpenseDuration = new Trend('create_expense_duration');
const getBalancesDuration = new Trend('get_balances_duration');
const requestsPerSecond = new Counter('requests_per_second');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up to 10 users
    { duration: '1m', target: 50 },    // Stay at 50 users
    { duration: '30s', target: 100 },  // Spike to 100 users
    { duration: '1m', target: 100 },   // Stay at 100 users
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95% < 500ms, 99% < 1s
    errors: ['rate<0.01'],                            // Error rate < 1%
    login_duration: ['p(95)<1000'],                   // Login < 1s
    create_expense_duration: ['p(95)<500'],           // Create expense < 500ms
    get_balances_duration: ['p(95)<300'],             // Get balances < 300ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test data
const testUsers = [
  { email: 'loadtest1@example.com', password: 'Password123!' },
  { email: 'loadtest2@example.com', password: 'Password123!' },
  { email: 'loadtest3@example.com', password: 'Password123!' },
];

// Helper function to get random test user
function getRandomUser() {
  return testUsers[Math.floor(Math.random() * testUsers.length)];
}

// Authentication helper
function login(email, password) {
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: email,
    password: password,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });

  loginDuration.add(loginRes.timings.duration);

  check(loginRes, {
    'login successful': (r) => r.status === 200,
    'has access token': (r) => r.json('accessToken') !== undefined,
  }) || errorRate.add(1);

  return loginRes.json('accessToken');
}

// Main test scenario
export default function () {
  const user = getRandomUser();
  let token;

  group('Authentication', function () {
    token = login(user.email, user.password);
    requestsPerSecond.add(1);
  });

  if (!token) {
    console.log('Login failed, skipping remaining tests');
    return;
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  sleep(1);

  group('Dashboard', function () {
    // Get user profile
    const profileRes = http.get(`${BASE_URL}/api/v1/users/me`, { headers, tags: { name: 'get_profile' } });
    check(profileRes, { 'profile retrieved': (r) => r.status === 200 }) || errorRate.add(1);
    requestsPerSecond.add(1);

    // Get groups
    const groupsRes = http.get(`${BASE_URL}/api/v1/groups`, { headers, tags: { name: 'get_groups' } });
    check(groupsRes, { 'groups retrieved': (r) => r.status === 200 }) || errorRate.add(1);
    requestsPerSecond.add(1);

    // Get overall balances
    const balancesRes = http.get(`${BASE_URL}/api/v1/balances`, { headers, tags: { name: 'get_balances' } });
    getBalancesDuration.add(balancesRes.timings.duration);
    check(balancesRes, { 'balances retrieved': (r) => r.status === 200 }) || errorRate.add(1);
    requestsPerSecond.add(1);
  });

  sleep(1);

  group('Expenses Flow', function () {
    // Get groups first
    const groupsRes = http.get(`${BASE_URL}/api/v1/groups`, { headers, tags: { name: 'get_groups' } });
    const groups = groupsRes.json();

    if (groups && groups.length > 0) {
      const groupId = groups[0].id;

      // Get expenses for group
      const expensesRes = http.get(`${BASE_URL}/api/v1/expenses?groupId=${groupId}`, { 
        headers, 
        tags: { name: 'get_expenses' } 
      });
      check(expensesRes, { 'expenses retrieved': (r) => r.status === 200 }) || errorRate.add(1);
      requestsPerSecond.add(1);

      // Create new expense (with 50% probability to reduce write load)
      if (Math.random() < 0.5) {
        const createExpenseRes = http.post(`${BASE_URL}/api/v1/expenses`, JSON.stringify({
          groupId: groupId,
          description: `Load test expense ${Date.now()}`,
          amount: Math.floor(Math.random() * 100) + 1,
          currency: 'USD',
          category: 'OTHER',
          splitType: 'EQUAL',
          date: new Date().toISOString(),
        }), { headers, tags: { name: 'create_expense' } });

        createExpenseDuration.add(createExpenseRes.timings.duration);
        check(createExpenseRes, {
          'expense created': (r) => r.status === 201,
        }) || errorRate.add(1);
        requestsPerSecond.add(1);
      }

      // Get group balances
      const groupBalancesRes = http.get(`${BASE_URL}/api/v1/balances?groupId=${groupId}`, { 
        headers, 
        tags: { name: 'get_group_balances' } 
      });
      check(groupBalancesRes, { 'group balances retrieved': (r) => r.status === 200 }) || errorRate.add(1);
      requestsPerSecond.add(1);
    }
  });

  sleep(1);

  group('Notifications', function () {
    const notificationsRes = http.get(`${BASE_URL}/api/v1/notifications`, { 
      headers, 
      tags: { name: 'get_notifications' } 
    });
    check(notificationsRes, { 'notifications retrieved': (r) => r.status === 200 }) || errorRate.add(1);
    requestsPerSecond.add(1);
  });

  sleep(2);
}

// Setup - Create test users if they don't exist
export function setup() {
  console.log('Setting up load test...');
  
  for (const user of testUsers) {
    const registerRes = http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({
      email: user.email,
      password: user.password,
      displayName: `Load Test User ${user.email.split('@')[0]}`,
    }), {
      headers: { 'Content-Type': 'application/json' },
    });

    if (registerRes.status === 201) {
      console.log(`Created user: ${user.email}`);
      
      // Create a test group for the user
      const token = registerRes.json('accessToken');
      if (token) {
        http.post(`${BASE_URL}/api/v1/groups`, JSON.stringify({
          name: `Load Test Group - ${user.email}`,
          description: 'Group for load testing',
          type: 'OTHER',
          currency: 'USD',
        }), {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
          },
        });
      }
    }
  }

  console.log('Setup complete');
}

// Teardown
export function teardown(data) {
  console.log('Load test complete');
}
