// K6 Load Test Configuration
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
export const API_VERSION = 'v1';

// Test user credentials (created during setup)
export const TEST_USERS = [
    { email: 'loadtest1@splitter.test', password: 'LoadTest123!' },
    { email: 'loadtest2@splitter.test', password: 'LoadTest123!' },
    { email: 'loadtest3@splitter.test', password: 'LoadTest123!' },
    { email: 'loadtest4@splitter.test', password: 'LoadTest123!' },
    { email: 'loadtest5@splitter.test', password: 'LoadTest123!' },
];

// Thresholds for different scenarios
export const THRESHOLDS = {
    // HTTP request duration thresholds
    http_req_duration: ['p(95)<200', 'p(99)<500'],
    // HTTP request failure rate
    http_req_failed: ['rate<0.01'],
    // Custom metrics
    'expense_creation_duration': ['p(95)<300'],
    'balance_query_duration': ['p(95)<100'],
    'group_operations_duration': ['p(95)<200'],
};

// Load stages for different test types
export const LOAD_STAGES = {
    smoke: [
        { duration: '1m', target: 5 },
        { duration: '1m', target: 0 },
    ],
    load: [
        { duration: '2m', target: 50 },
        { duration: '5m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 0 },
    ],
    stress: [
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '5m', target: 200 },
        { duration: '2m', target: 300 },
        { duration: '5m', target: 300 },
        { duration: '5m', target: 0 },
    ],
    spike: [
        { duration: '1m', target: 50 },
        { duration: '30s', target: 500 },
        { duration: '1m', target: 500 },
        { duration: '30s', target: 50 },
        { duration: '2m', target: 0 },
    ],
    soak: [
        { duration: '5m', target: 100 },
        { duration: '4h', target: 100 },
        { duration: '5m', target: 0 },
    ],
};

// Get stage configuration by test type
export function getStages(testType = 'load') {
    return LOAD_STAGES[testType] || LOAD_STAGES.load;
}
