import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, API_VERSION } from '../config.js';

const API_BASE = `${BASE_URL}/api/${API_VERSION}`;

// Store tokens per VU
const tokens = {};

export function getHeaders(token = null) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

export function login(email, password) {
    const response = http.post(
        `${API_BASE}/auth/login`,
        JSON.stringify({ email, password }),
        { headers: getHeaders() }
    );

    const success = check(response, {
        'login successful': (r) => r.status === 200,
        'has access token': (r) => r.json('accessToken') !== undefined,
    });

    if (success) {
        tokens[__VU] = response.json('accessToken');
        return response.json('accessToken');
    }
    return null;
}

export function getToken() {
    return tokens[__VU];
}

// User APIs
export function getProfile() {
    return http.get(`${API_BASE}/users/me`, {
        headers: getHeaders(getToken()),
    });
}

export function searchUsers(query) {
    return http.get(`${API_BASE}/users/search?q=${encodeURIComponent(query)}`, {
        headers: getHeaders(getToken()),
    });
}

// Group APIs
export function createGroup(name, description = '') {
    return http.post(
        `${API_BASE}/groups`,
        JSON.stringify({ name, description }),
        { headers: getHeaders(getToken()) }
    );
}

export function getGroups() {
    return http.get(`${API_BASE}/groups`, {
        headers: getHeaders(getToken()),
    });
}

export function getGroup(groupId) {
    return http.get(`${API_BASE}/groups/${groupId}`, {
        headers: getHeaders(getToken()),
    });
}

export function addMember(groupId, userId) {
    return http.post(
        `${API_BASE}/groups/${groupId}/members`,
        JSON.stringify({ userId }),
        { headers: getHeaders(getToken()) }
    );
}

// Expense APIs
export function createExpense(data) {
    return http.post(
        `${API_BASE}/expenses`,
        JSON.stringify(data),
        { headers: getHeaders(getToken()) }
    );
}

export function getExpenses(groupId = null, page = 0, size = 20) {
    let url = `${API_BASE}/expenses?page=${page}&size=${size}`;
    if (groupId) {
        url += `&groupId=${groupId}`;
    }
    return http.get(url, { headers: getHeaders(getToken()) });
}

export function getExpense(expenseId) {
    return http.get(`${API_BASE}/expenses/${expenseId}`, {
        headers: getHeaders(getToken()),
    });
}

export function updateExpense(expenseId, data) {
    return http.put(
        `${API_BASE}/expenses/${expenseId}`,
        JSON.stringify(data),
        { headers: getHeaders(getToken()) }
    );
}

export function deleteExpense(expenseId) {
    return http.del(`${API_BASE}/expenses/${expenseId}`, null, {
        headers: getHeaders(getToken()),
    });
}

// Balance APIs
export function getUserBalances() {
    return http.get(`${API_BASE}/balances/me`, {
        headers: getHeaders(getToken()),
    });
}

export function getGroupBalances(groupId) {
    return http.get(`${API_BASE}/balances/group/${groupId}`, {
        headers: getHeaders(getToken()),
    });
}

export function getSimplifiedDebts(groupId) {
    return http.get(`${API_BASE}/balances/simplified/${groupId}`, {
        headers: getHeaders(getToken()),
    });
}

// Settlement APIs
export function recordSettlement(data) {
    return http.post(
        `${API_BASE}/settlements`,
        JSON.stringify(data),
        { headers: getHeaders(getToken()) }
    );
}

export function getSettlements() {
    return http.get(`${API_BASE}/settlements`, {
        headers: getHeaders(getToken()),
    });
}

// Health check
export function healthCheck() {
    return http.get(`${BASE_URL}/actuator/health`);
}
