import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, THRESHOLDS, getStages } from '../config.js';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Custom metrics
const loginDuration = new Trend('login_duration');
const loginFailures = new Counter('login_failures');
const tokenRefreshDuration = new Trend('token_refresh_duration');

export const options = {
    stages: getStages(__ENV.TEST_TYPE || 'load'),
    thresholds: {
        ...THRESHOLDS,
        'login_duration': ['p(95)<500'],
        'login_failures': ['count<10'],
    },
};

export function setup() {
    // Verify API is accessible
    const healthRes = http.get(`${BASE_URL}/actuator/health`);
    if (healthRes.status !== 200) {
        throw new Error('API is not healthy');
    }
    console.log('API health check passed');
    return { users: TEST_USERS };
}

export default function (data) {
    const user = randomItem(data.users);
    
    // Test: Login
    const loginStart = Date.now();
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email: user.email,
            password: user.password,
        }),
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );
    loginDuration.add(Date.now() - loginStart);

    const loginSuccess = check(loginRes, {
        'login status is 200': (r) => r.status === 200,
        'login response has accessToken': (r) => r.json('accessToken') !== undefined,
        'login response has refreshToken': (r) => r.json('refreshToken') !== undefined,
    });

    if (!loginSuccess) {
        loginFailures.add(1);
        console.error(`Login failed for ${user.email}: ${loginRes.status} - ${loginRes.body}`);
        return;
    }

    const accessToken = loginRes.json('accessToken');
    const refreshToken = loginRes.json('refreshToken');

    sleep(1);

    // Test: Access protected resource
    const profileRes = http.get(`${BASE_URL}/api/v1/users/me`, {
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json',
        },
    });

    check(profileRes, {
        'profile access successful': (r) => r.status === 200,
        'profile has user id': (r) => r.json('id') !== undefined,
    });

    sleep(1);

    // Test: Token refresh
    const refreshStart = Date.now();
    const refreshRes = http.post(
        `${BASE_URL}/api/v1/auth/refresh`,
        JSON.stringify({ refreshToken }),
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );
    tokenRefreshDuration.add(Date.now() - refreshStart);

    check(refreshRes, {
        'token refresh successful': (r) => r.status === 200,
        'new accessToken received': (r) => r.json('accessToken') !== undefined,
    });

    sleep(1);

    // Test: Access with invalid token
    const invalidTokenRes = http.get(`${BASE_URL}/api/v1/users/me`, {
        headers: {
            'Authorization': 'Bearer invalid_token_12345',
            'Content-Type': 'application/json',
        },
    });

    check(invalidTokenRes, {
        'invalid token rejected': (r) => r.status === 401,
    });

    sleep(2);
}

export function teardown(data) {
    console.log('Auth flow test completed');
}
