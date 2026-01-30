import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { BASE_URL, TEST_USERS, THRESHOLDS, getStages } from '../config.js';
import { 
    login, getGroups, getExpenses, getUserBalances, 
    createExpense, recordSettlement, getGroupBalances 
} from '../utils/api-client.js';
import { generateExpenseData, randomDelay } from '../utils/data-generators.js';
import { randomItem, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Custom metrics
const pageLoadDuration = new Trend('page_load_duration');
const transactionDuration = new Trend('transaction_duration');
const userJourneyErrors = new Counter('user_journey_errors');

export const options = {
    stages: getStages(__ENV.TEST_TYPE || 'load'),
    thresholds: {
        ...THRESHOLDS,
        'page_load_duration': ['p(95)<500'],
        'transaction_duration': ['p(95)<1000'],
    },
};

let testGroups = [];
let testUserIds = [];

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

    const groupsRes = http.get(`${BASE_URL}/api/v1/groups`, {
        headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
    });

    if (groupsRes.status === 200) {
        testGroups = groupsRes.json('content') || groupsRes.json() || [];
    }

    return { groups: testGroups };
}

export default function (data) {
    const user = randomItem(TEST_USERS);
    
    const token = login(user.email, user.password);
    if (!token) {
        userJourneyErrors.add(1);
        return;
    }

    // Simulate realistic user behavior with different scenarios
    const scenario = randomIntBetween(1, 4);

    switch (scenario) {
        case 1:
            browseAndViewScenario(data);
            break;
        case 2:
            addExpenseScenario(data);
            break;
        case 3:
            settleUpScenario(data);
            break;
        case 4:
            dashboardRefreshScenario(data);
            break;
    }
}

function browseAndViewScenario(data) {
    group('Browse and View Journey', function () {
        // Load dashboard
        let startTime = Date.now();
        
        const [balanceRes, groupsRes] = [
            getUserBalances(),
            getGroups(),
        ];
        pageLoadDuration.add(Date.now() - startTime);

        check(balanceRes, { 'dashboard balance loaded': (r) => r.status === 200 });
        check(groupsRes, { 'dashboard groups loaded': (r) => r.status === 200 });

        sleep(randomDelay(2, 4)); // User reads dashboard

        // Click into a group
        if (data.groups.length > 0) {
            const selectedGroup = randomItem(data.groups);
            
            startTime = Date.now();
            const [expensesRes, groupBalanceRes] = [
                getExpenses(selectedGroup.id, 0, 20),
                getGroupBalances(selectedGroup.id),
            ];
            pageLoadDuration.add(Date.now() - startTime);

            check(expensesRes, { 'group expenses loaded': (r) => r.status === 200 });
            check(groupBalanceRes, { 'group balance loaded': (r) => r.status === 200 });

            sleep(randomDelay(3, 6)); // User browses expenses

            // Scroll/paginate through expenses
            if (Math.random() > 0.5) {
                startTime = Date.now();
                const page2Res = getExpenses(selectedGroup.id, 1, 20);
                pageLoadDuration.add(Date.now() - startTime);

                check(page2Res, { 'pagination works': (r) => r.status === 200 });
                sleep(randomDelay(2, 4));
            }
        }
    });
}

function addExpenseScenario(data) {
    group('Add Expense Journey', function () {
        // Load dashboard first
        getUserBalances();
        getGroups();
        sleep(randomDelay(1, 2));

        if (data.groups.length === 0) {
            console.log('No groups for expense');
            return;
        }

        // Select group and load it
        const selectedGroup = randomItem(data.groups);
        getGroupBalances(selectedGroup.id);
        sleep(randomDelay(1, 2));

        // Create expense
        const expenseData = generateExpenseData(
            selectedGroup.id,
            [`user-1`, `user-2`, `user-3`].slice(0, randomIntBetween(2, 3))
        );

        const startTime = Date.now();
        const createRes = createExpense(expenseData);
        transactionDuration.add(Date.now() - startTime);

        const success = check(createRes, {
            'expense created': (r) => r.status === 201,
        });

        if (!success) {
            userJourneyErrors.add(1);
            return;
        }

        sleep(randomDelay(1, 2));

        // View updated balance
        const balanceRes = getUserBalances();
        check(balanceRes, { 'balance updated': (r) => r.status === 200 });

        sleep(randomDelay(2, 4));
    });
}

function settleUpScenario(data) {
    group('Settle Up Journey', function () {
        // Load dashboard
        const balanceRes = getUserBalances();
        sleep(randomDelay(1, 2));

        if (data.groups.length === 0) return;

        // Select group and view balances
        const selectedGroup = randomItem(data.groups);
        const groupBalanceRes = getGroupBalances(selectedGroup.id);
        
        check(groupBalanceRes, { 'group balance loaded': (r) => r.status === 200 });
        sleep(randomDelay(2, 4)); // User reviews who owes what

        // Record a settlement
        const settlementData = {
            groupId: selectedGroup.id,
            toUserId: 'user-2', // Would be actual user ID
            amount: randomIntBetween(10, 100),
            paymentMethod: 'VENMO',
        };

        const startTime = Date.now();
        const settleRes = recordSettlement(settlementData);
        transactionDuration.add(Date.now() - startTime);

        // Settlement might fail if no balance owed - that's OK
        if (settleRes.status === 201 || settleRes.status === 200) {
            sleep(randomDelay(1, 2));
            
            // Verify balance updated
            getUserBalances();
        }

        sleep(randomDelay(2, 4));
    });
}

function dashboardRefreshScenario(data) {
    group('Dashboard Refresh Journey', function () {
        // Simulate user repeatedly checking dashboard (common pattern)
        for (let i = 0; i < 3; i++) {
            const startTime = Date.now();
            const balanceRes = getUserBalances();
            pageLoadDuration.add(Date.now() - startTime);

            check(balanceRes, { 'dashboard refresh successful': (r) => r.status === 200 });

            // Wait like a user would
            sleep(randomDelay(10, 30));
        }
    });
}

export function teardown(data) {
    console.log('Mixed workload test completed');
}
