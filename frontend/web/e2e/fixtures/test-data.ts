import { v4 as uuidv4 } from 'uuid';

/**
 * Test data generators for E2E tests
 */

export const generateTestEmail = () => `test-${uuidv4().slice(0, 8)}@example.com`;

export const generateTestGroup = (overrides = {}) => ({
  name: `Test Group ${Date.now()}`,
  description: 'A test group for E2E testing',
  type: 'OTHER' as const,
  currency: 'USD',
  ...overrides,
});

export const generateTestExpense = (overrides = {}) => ({
  description: `Test Expense ${Date.now()}`,
  amount: Math.floor(Math.random() * 100) + 10,
  currency: 'USD',
  category: 'OTHER' as const,
  splitType: 'EQUAL' as const,
  date: new Date().toISOString(),
  ...overrides,
});

export const generateTestSettlement = (overrides = {}) => ({
  amount: Math.floor(Math.random() * 50) + 5,
  paymentMethod: 'CASH' as const,
  note: 'Test settlement payment',
  ...overrides,
});

// Static test data for predictable tests
export const STATIC_TEST_DATA = {
  groups: {
    tripGroup: {
      name: 'Summer Vacation 2026',
      description: 'Beach trip with friends',
      type: 'TRIP' as const,
      currency: 'USD',
    },
    homeGroup: {
      name: 'Apartment 42B',
      description: 'Shared apartment expenses',
      type: 'HOME' as const,
      currency: 'USD',
    },
    coupleGroup: {
      name: 'Us',
      description: 'Shared expenses',
      type: 'COUPLE' as const,
      currency: 'USD',
    },
  },
  expenses: {
    dinner: {
      description: 'Dinner at Italian Restaurant',
      amount: 120.50,
      category: 'FOOD' as const,
    },
    groceries: {
      description: 'Weekly Groceries',
      amount: 85.25,
      category: 'GROCERIES' as const,
    },
    utilities: {
      description: 'Electricity Bill - January',
      amount: 150.00,
      category: 'UTILITIES' as const,
    },
    entertainment: {
      description: 'Movie Night',
      amount: 45.00,
      category: 'ENTERTAINMENT' as const,
    },
    transport: {
      description: 'Uber to Airport',
      amount: 35.00,
      category: 'TRANSPORT' as const,
    },
  },
  categories: [
    'FOOD',
    'GROCERIES',
    'TRANSPORT',
    'ENTERTAINMENT',
    'UTILITIES',
    'RENT',
    'HEALTHCARE',
    'SHOPPING',
    'OTHER',
  ],
  splitTypes: ['EQUAL', 'EXACT', 'PERCENTAGE', 'SHARES'],
  paymentMethods: ['CASH', 'BANK_TRANSFER', 'VENMO', 'PAYPAL', 'ZELLE', 'OTHER'],
};

// Seed data for database setup
export const SEED_DATA = {
  users: [
    {
      email: 'alice@example.com',
      password: 'Password123!',
      displayName: 'Alice Smith',
    },
    {
      email: 'bob@example.com',
      password: 'Password123!',
      displayName: 'Bob Johnson',
    },
    {
      email: 'charlie@example.com',
      password: 'Password123!',
      displayName: 'Charlie Brown',
    },
  ],
};
