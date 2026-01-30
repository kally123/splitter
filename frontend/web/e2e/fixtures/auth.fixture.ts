import { test as base, expect } from '@playwright/test';
import path from 'path';

// Test user credentials
export const TEST_USER = {
  email: 'test@example.com',
  password: 'Password123!',
  displayName: 'Test User',
};

export const TEST_USER_2 = {
  email: 'test2@example.com',
  password: 'Password123!',
  displayName: 'Test User 2',
};

// Extend base test with authentication
export const test = base.extend<{
  authenticatedPage: typeof base;
}>({
  authenticatedPage: async ({ page }, use) => {
    // Load authentication state from file
    const authFile = path.join(__dirname, '../.auth/user.json');
    await page.context().storageState({ path: authFile });
    await use(page as any);
  },
});

export { expect };

/**
 * Helper to login programmatically via API
 */
export async function loginViaApi(request: any, email: string, password: string): Promise<string> {
  const response = await request.post('/api/v1/auth/login', {
    data: { email, password },
  });
  
  const data = await response.json();
  return data.accessToken;
}

/**
 * Helper to register a new user via API
 */
export async function registerViaApi(
  request: any,
  email: string,
  password: string,
  displayName: string
): Promise<{ userId: string; accessToken: string }> {
  const response = await request.post('/api/v1/auth/register', {
    data: { email, password, displayName },
  });
  
  const data = await response.json();
  return {
    userId: data.userId,
    accessToken: data.accessToken,
  };
}

/**
 * Helper to create authenticated API context
 */
export async function createAuthenticatedContext(request: any, token: string) {
  return request.newContext({
    extraHTTPHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}
