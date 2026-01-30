import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, THRESHOLDS, getStages } from '../config.js';
import { login, getToken, getUserBalances, getGroupBalances, getSimplifiedDebts } from '../utils/api-client.js';
import { randomItem, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Custom metrics
const balanceQueryDuration = new Trend('balance_query_duration');
const simplifiedDebtsDuration = new Trend('simplified_debts_duration');
const balanceQueryErrors = new Counter('balance_query_errors');

export const options = {
    stages: getStages(__ENV.TEST_TYPE || 'load'),
    thresholds: {
        ...THRESHOLDS,
        'balance_query_duration': ['p(95)<100', 'p(99)<200'],
        'simplified_debts_duration': ['p(95)<150'],
    },
};

let testGroups = [];

export function setup() {
    const user = TEST_USERS[0];
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email: user.email, password: user.password }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (loginRes.status !== 200) {
        throw new Error('Setup login failed');
    }

    const token = loginRes.json('accessToken');

    // Get user's groups
    const groupsRes = http.get(`${BASE_URL}/api/v1/groups`, {
        headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
    });

    if (groupsRes.status === 200) {
        testGroups = groupsRes.json('content') || groupsRes.json() || [];
    }

    console.log(`Setup complete. Found ${testGroups.length} groups.`);
    return { groups: testGroups };
}

export default function (data) {
    const user = randomItem(TEST_USERS);
    
    const token = login(user.email, user.password);
    if (!token) {
        console.error('Login failed, skipping iteration');
        return;
    }

    sleep(1);

    group('User Balance Overview', function () {
        const startTime = Date.now();
        const res = getUserBalances();
        balanceQueryDuration.add(Date.now() - startTime);

        const success = check(res, {
            'user balances successful': (r) => r.status === 200,
            'has totalOwed': (r) => r.json('totalOwed') !== undefined,
            'has totalOwedToYou': (r) => r.json('totalOwedToYou') !== undefined,
        });

        if (!success) {
            balanceQueryErrors.add(1);
        }
    });

    sleep(randomIntBetween(1, 2));

    // Query balances for each group
    if (data.groups.length > 0) {
        group('Group Balance Queries', function () {
            // Query 1-3 random groups
            const numGroups = Math.min(data.groups.length, randomIntBetween(1, 3));
            
            for (let i = 0; i < numGroups; i++) {
                const selectedGroup = randomItem(data.groups);
                
                const startTime = Date.now();
                const res = getGroupBalances(selectedGroup.id);
                balanceQueryDuration.add(Date.now() - startTime);

                check(res, {
                    'group balances successful': (r) => r.status === 200,
                    'balances is array': (r) => Array.isArray(r.json()),
                });

                sleep(0.5);
            }
        });
    }

    sleep(randomIntBetween(1, 2));

    // Test simplified debts calculation
    if (data.groups.length > 0) {
        group('Simplified Debts', function () {
            const selectedGroup = randomItem(data.groups);
            
            const startTime = Date.now();
            const res = getSimplifiedDebts(selectedGroup.id);
            simplifiedDebtsDuration.add(Date.now() - startTime);

            check(res, {
                'simplified debts successful': (r) => r.status === 200,
                'result is array': (r) => Array.isArray(r.json()),
            });
        });
    }

    sleep(randomIntBetween(2, 4));

    // Rapid fire balance queries (cache effectiveness test)
    group('Rapid Balance Queries', function () {
        for (let i = 0; i < 5; i++) {
            const startTime = Date.now();
            const res = getUserBalances();
            balanceQueryDuration.add(Date.now() - startTime);

            check(res, {
                'rapid query successful': (r) => r.status === 200,
            });

            sleep(0.2);
        }
    });

    sleep(randomIntBetween(1, 3));
}

export function teardown(data) {
    console.log('Balance queries test completed');
}
