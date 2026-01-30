import { test as setup, expect } from '@playwright/test';
import { TEST_USER } from '../fixtures/auth.fixture';
import path from 'path';

const authFile = path.join(__dirname, '../.auth/user.json');

/**
 * Authentication setup - runs before all tests
 * Creates a shared authentication state that other tests can reuse
 */
setup('authenticate', async ({ page, request }) => {
  // First, try to register the test user (in case it doesn't exist)
  try {
    const registerResponse = await request.post('/api/v1/auth/register', {
      data: {
        email: TEST_USER.email,
        password: TEST_USER.password,
        displayName: TEST_USER.displayName,
      },
    });
    
    // If registration succeeds, we have a new user
    if (registerResponse.ok()) {
      console.log('Test user created successfully');
    }
  } catch (error) {
    // User might already exist, continue to login
    console.log('Test user may already exist, attempting login...');
  }

  // Navigate to login page
  await page.goto('/auth/login');
  
  // Fill in login form
  await page.locator('input[id="email"]').fill(TEST_USER.email);
  await page.locator('input[id="password"]').fill(TEST_USER.password);
  
  // Submit login form
  await page.locator('button[type="submit"]').click();
  
  // Wait for successful login - should redirect to dashboard
  await expect(page).toHaveURL('/dashboard', { timeout: 15000 });
  
  // Verify we're logged in by checking for user-specific content
  await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
  
  // Save authentication state
  await page.context().storageState({ path: authFile });
  
  console.log('Authentication state saved successfully');
});
