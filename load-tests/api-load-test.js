import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// API-specific load test for individual endpoints
export const options = {
  scenarios: {
    auth_endpoints: {
      executor: 'constant-arrival-rate',
      rate: 10,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 20,
      maxVUs: 50,
      exec: 'authEndpoints',
      tags: { scenario: 'auth' },
    },
    group_endpoints: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 30,
      maxVUs: 100,
      exec: 'groupEndpoints',
      startTime: '30s',
      tags: { scenario: 'groups' },
    },
    expense_endpoints: {
      executor: 'constant-arrival-rate',
      rate: 30,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 50,
      maxVUs: 150,
      exec: 'expenseEndpoints',
      startTime: '1m',
      tags: { scenario: 'expenses' },
    },
    balance_endpoints: {
      executor: 'constant-arrival-rate',
      rate: 40,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 60,
      maxVUs: 200,
      exec: 'balanceEndpoints',
      startTime: '1m30s',
      tags: { scenario: 'balances' },
    },
  },
  thresholds: {
    'http_req_duration{scenario:auth}': ['p(95)<1000'],
    'http_req_duration{scenario:groups}': ['p(95)<500'],
    'http_req_duration{scenario:expenses}': ['p(95)<500'],
    'http_req_duration{scenario:balances}': ['p(95)<300'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const errorRate = new Rate('errors');

// Shared token cache
let cachedToken = null;

function getToken() {
  if (cachedToken) return cachedToken;
  
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: 'apitest@example.com',
    password: 'Password123!',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginRes.status === 200) {
    cachedToken = loginRes.json('accessToken');
  } else {
    // Register
    const registerRes = http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({
      email: 'apitest@example.com',
      password: 'Password123!',
      displayName: 'API Test User',
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (registerRes.status === 201) {
      cachedToken = registerRes.json('accessToken');
    }
  }

  return cachedToken;
}

// Auth endpoints scenario
export function authEndpoints() {
  const timestamp = Date.now();
  const email = `auth${__VU}_${timestamp}@example.com`;

  // Register
  const registerRes = http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({
    email: email,
    password: 'Password123!',
    displayName: `Auth Test ${__VU}`,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'register' },
  });

  check(registerRes, {
    'register: status 201 or 409': (r) => r.status === 201 || r.status === 409,
  }) || errorRate.add(1);

  // Login
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: email,
    password: 'Password123!',
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'login' },
  });

  check(loginRes, {
    'login: status 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  if (loginRes.status === 200) {
    const token = loginRes.json('accessToken');
    
    // Refresh token
    const refreshRes = http.post(`${BASE_URL}/api/v1/auth/refresh`, JSON.stringify({
      refreshToken: loginRes.json('refreshToken'),
    }), {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'refresh' },
    });

    check(refreshRes, {
      'refresh: status 200': (r) => r.status === 200,
    });
  }

  sleep(1);
}

// Group endpoints scenario
export function groupEndpoints() {
  const token = getToken();
  if (!token) return;

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // List groups
  const listRes = http.get(`${BASE_URL}/api/v1/groups`, { 
    headers, 
    tags: { endpoint: 'list_groups' } 
  });
  check(listRes, { 'list groups: 200': (r) => r.status === 200 }) || errorRate.add(1);

  // Create group (10% of requests)
  if (Math.random() < 0.1) {
    const createRes = http.post(`${BASE_URL}/api/v1/groups`, JSON.stringify({
      name: `Load Test Group ${Date.now()}`,
      description: 'Created during load test',
      type: 'OTHER',
      currency: 'USD',
    }), { headers, tags: { endpoint: 'create_group' } });

    check(createRes, { 'create group: 201': (r) => r.status === 201 }) || errorRate.add(1);
  }

  // Get specific group
  const groups = listRes.json();
  if (groups && groups.length > 0) {
    const groupId = groups[Math.floor(Math.random() * groups.length)].id;
    
    const getRes = http.get(`${BASE_URL}/api/v1/groups/${groupId}`, { 
      headers, 
      tags: { endpoint: 'get_group' } 
    });
    check(getRes, { 'get group: 200': (r) => r.status === 200 }) || errorRate.add(1);
  }

  sleep(0.5);
}

// Expense endpoints scenario
export function expenseEndpoints() {
  const token = getToken();
  if (!token) return;

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // List expenses
  const listRes = http.get(`${BASE_URL}/api/v1/expenses`, { 
    headers, 
    tags: { endpoint: 'list_expenses' } 
  });
  check(listRes, { 'list expenses: 200': (r) => r.status === 200 }) || errorRate.add(1);

  // Create expense (20% of requests)
  if (Math.random() < 0.2) {
    const createRes = http.post(`${BASE_URL}/api/v1/expenses`, JSON.stringify({
      description: `Load test expense ${Date.now()}`,
      amount: Math.floor(Math.random() * 100) + 1,
      currency: 'USD',
      category: 'OTHER',
      splitType: 'EQUAL',
    }), { headers, tags: { endpoint: 'create_expense' } });

    check(createRes, { 'create expense: 201 or 400': (r) => r.status === 201 || r.status === 400 });
  }

  sleep(0.3);
}

// Balance endpoints scenario
export function balanceEndpoints() {
  const token = getToken();
  if (!token) return;

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // Get overall balances
  const balancesRes = http.get(`${BASE_URL}/api/v1/balances`, { 
    headers, 
    tags: { endpoint: 'get_balances' } 
  });
  check(balancesRes, { 'get balances: 200': (r) => r.status === 200 }) || errorRate.add(1);

  // Get group-specific balances
  const groupsRes = http.get(`${BASE_URL}/api/v1/groups`, { headers });
  const groups = groupsRes.json();
  
  if (groups && groups.length > 0) {
    const groupId = groups[Math.floor(Math.random() * groups.length)].id;
    
    const groupBalancesRes = http.get(`${BASE_URL}/api/v1/balances?groupId=${groupId}`, { 
      headers, 
      tags: { endpoint: 'get_group_balances' } 
    });
    check(groupBalancesRes, { 'get group balances: 200': (r) => r.status === 200 }) || errorRate.add(1);
  }

  sleep(0.2);
}
