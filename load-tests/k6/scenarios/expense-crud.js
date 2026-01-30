import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, THRESHOLDS, getStages } from '../config.js';
import { login, getToken, getHeaders, createExpense, getExpenses, getExpense, updateExpense, deleteExpense } from '../utils/api-client.js';
import { generateExpenseData, randomDelay } from '../utils/data-generators.js';
import { randomItem, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Custom metrics
const expenseCreationDuration = new Trend('expense_creation_duration');
const expenseQueryDuration = new Trend('expense_query_duration');
const expenseUpdateDuration = new Trend('expense_update_duration');
const expenseDeleteDuration = new Trend('expense_delete_duration');
const expenseCreationErrors = new Counter('expense_creation_errors');

export const options = {
    stages: getStages(__ENV.TEST_TYPE || 'load'),
    thresholds: {
        ...THRESHOLDS,
        'expense_creation_duration': ['p(95)<300'],
        'expense_query_duration': ['p(95)<150'],
        'expense_update_duration': ['p(95)<300'],
        'expense_delete_duration': ['p(95)<200'],
    },
};

// Shared test data
let testGroups = [];
let testUserIds = [];

export function setup() {
    // Login as first user to get test data
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

    // Get test user IDs
    TEST_USERS.forEach((u, i) => {
        testUserIds.push(`user-${i + 1}`); // Placeholder, would be actual IDs
    });

    console.log(`Setup complete. Found ${testGroups.length} groups.`);
    return { groups: testGroups, userIds: testUserIds };
}

export default function (data) {
    const user = randomItem(TEST_USERS);
    
    // Login
    const token = login(user.email, user.password);
    if (!token) {
        console.error('Login failed, skipping iteration');
        return;
    }

    sleep(randomDelay(1, 2));

    group('Create Expense', function () {
        if (data.groups.length === 0) {
            console.log('No groups available for expense creation');
            return;
        }

        const selectedGroup = randomItem(data.groups);
        const expenseData = generateExpenseData(
            selectedGroup.id,
            data.userIds.slice(0, randomIntBetween(2, 4))
        );

        const startTime = Date.now();
        const res = createExpense(expenseData);
        expenseCreationDuration.add(Date.now() - startTime);

        const success = check(res, {
            'expense created (201)': (r) => r.status === 201,
            'expense has id': (r) => r.json('id') !== undefined,
            'expense amount matches': (r) => parseFloat(r.json('amount')) === parseFloat(expenseData.amount),
        });

        if (!success) {
            expenseCreationErrors.add(1);
            console.error(`Expense creation failed: ${res.status} - ${res.body}`);
        } else {
            // Store expense ID for later operations
            __ENV.LAST_EXPENSE_ID = res.json('id');
        }
    });

    sleep(randomDelay(1, 2));

    group('Query Expenses', function () {
        // Get all expenses
        const startTime = Date.now();
        const res = getExpenses(null, 0, 20);
        expenseQueryDuration.add(Date.now() - startTime);

        check(res, {
            'expenses list successful': (r) => r.status === 200,
            'expenses is array': (r) => Array.isArray(r.json('content') || r.json()),
        });
    });

    sleep(randomDelay(1, 2));

    group('Get Single Expense', function () {
        const expenseId = __ENV.LAST_EXPENSE_ID;
        if (!expenseId) return;

        const startTime = Date.now();
        const res = getExpense(expenseId);
        expenseQueryDuration.add(Date.now() - startTime);

        check(res, {
            'get expense successful': (r) => r.status === 200,
            'expense id matches': (r) => r.json('id') === expenseId,
        });
    });

    sleep(randomDelay(1, 2));

    group('Update Expense', function () {
        const expenseId = __ENV.LAST_EXPENSE_ID;
        if (!expenseId) return;

        const updateData = {
            description: `Updated expense ${Date.now()}`,
            amount: randomIntBetween(10, 200).toFixed(2),
        };

        const startTime = Date.now();
        const res = updateExpense(expenseId, updateData);
        expenseUpdateDuration.add(Date.now() - startTime);

        check(res, {
            'expense updated': (r) => r.status === 200,
            'description updated': (r) => r.json('description') === updateData.description,
        });
    });

    sleep(randomDelay(1, 2));

    // Delete expense (50% chance to clean up)
    if (Math.random() > 0.5) {
        group('Delete Expense', function () {
            const expenseId = __ENV.LAST_EXPENSE_ID;
            if (!expenseId) return;

            const startTime = Date.now();
            const res = deleteExpense(expenseId);
            expenseDeleteDuration.add(Date.now() - startTime);

            check(res, {
                'expense deleted': (r) => r.status === 204 || r.status === 200,
            });
        });
    }

    sleep(randomDelay(2, 4));
}

export function teardown(data) {
    console.log('Expense CRUD test completed');
}
