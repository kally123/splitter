import { test, expect } from '@playwright/test';
import { LoginPage, RegisterPage } from '../pages/auth.page';
import { generateTestEmail } from '../fixtures/test-data';

test.describe('Authentication - Login', () => {
  test('should display login page correctly', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    
    await expect(loginPage.emailInput).toBeVisible();
    await expect(loginPage.passwordInput).toBeVisible();
    await expect(loginPage.submitButton).toBeVisible();
    await expect(loginPage.registerLink).toBeVisible();
  });

  test('should show validation errors for empty form', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    
    await loginPage.submitButton.click();
    
    // Should show validation errors
    await expect(page.locator('text=Email is required')).toBeVisible();
  });

  test('should show error for invalid email format', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    
    await loginPage.emailInput.fill('invalid-email');
    await loginPage.passwordInput.fill('password123');
    await loginPage.submitButton.click();
    
    await expect(page.locator('text=Invalid email')).toBeVisible();
  });

  test('should show error for invalid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    
    await loginPage.login('nonexistent@example.com', 'wrongpassword');
    
    await loginPage.expectError('Invalid email or password');
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    
    // Use test user credentials
    await loginPage.login('test@example.com', 'Password123!');
    
    await loginPage.expectSuccess();
    await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
  });

  test('should navigate to register page', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    
    await loginPage.goToRegister();
    
    await expect(page).toHaveURL('/auth/register');
  });

  test('should toggle password visibility', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    
    await loginPage.passwordInput.fill('mypassword');
    
    // Password should be hidden by default
    await expect(loginPage.passwordInput).toHaveAttribute('type', 'password');
    
    // Toggle visibility
    await loginPage.togglePasswordVisibility();
    await expect(loginPage.passwordInput).toHaveAttribute('type', 'text');
    
    // Toggle back
    await loginPage.togglePasswordVisibility();
    await expect(loginPage.passwordInput).toHaveAttribute('type', 'password');
  });
});

test.describe('Authentication - Registration', () => {
  test('should display registration page correctly', async ({ page }) => {
    const registerPage = new RegisterPage(page);
    await registerPage.goto();
    
    await expect(registerPage.displayNameInput).toBeVisible();
    await expect(registerPage.emailInput).toBeVisible();
    await expect(registerPage.passwordInput).toBeVisible();
    await expect(registerPage.confirmPasswordInput).toBeVisible();
    await expect(registerPage.submitButton).toBeVisible();
  });

  test('should show validation errors for empty form', async ({ page }) => {
    const registerPage = new RegisterPage(page);
    await registerPage.goto();
    
    await registerPage.submitButton.click();
    
    await expect(page.locator('text=Display name is required')).toBeVisible();
  });

  test('should show error for password mismatch', async ({ page }) => {
    const registerPage = new RegisterPage(page);
    await registerPage.goto();
    
    await registerPage.register('Test User', 'test@example.com', 'Password123!', 'DifferentPassword!');
    
    await expect(page.locator('text=Passwords do not match')).toBeVisible();
  });

  test('should show error for weak password', async ({ page }) => {
    const registerPage = new RegisterPage(page);
    await registerPage.goto();
    
    await registerPage.register('Test User', 'test@example.com', 'weak', 'weak');
    
    await expect(page.locator('text=Password must be at least 8 characters')).toBeVisible();
  });

  test('should register successfully with valid data', async ({ page }) => {
    const registerPage = new RegisterPage(page);
    await registerPage.goto();
    
    const testEmail = generateTestEmail();
    await registerPage.register('New Test User', testEmail, 'Password123!');
    
    await registerPage.expectSuccess();
    await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
  });

  test('should show error for existing email', async ({ page }) => {
    const registerPage = new RegisterPage(page);
    await registerPage.goto();
    
    // Use existing test user email
    await registerPage.register('Test User', 'test@example.com', 'Password123!');
    
    await registerPage.expectError('Email already exists');
  });

  test('should navigate to login page', async ({ page }) => {
    const registerPage = new RegisterPage(page);
    await registerPage.goto();
    
    await registerPage.goToLogin();
    
    await expect(page).toHaveURL('/auth/login');
  });
});

test.describe('Authentication - Logout', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should logout successfully', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
    
    // Open user menu and logout
    await page.locator('[data-testid="user-menu"]').click();
    await page.locator('text=Log out').click();
    
    await expect(page).toHaveURL('/auth/login');
  });

  test('should redirect to login when accessing protected route after logout', async ({ page, context }) => {
    await page.goto('/dashboard');
    
    // Clear authentication
    await context.clearCookies();
    await page.evaluate(() => localStorage.clear());
    
    // Try to navigate to protected route
    await page.goto('/groups');
    
    await expect(page).toHaveURL('/auth/login');
  });
});

test.describe('Authentication - Protected Routes', () => {
  test('should redirect to login for unauthenticated user', async ({ page }) => {
    // Clear any existing auth state
    await page.context().clearCookies();
    
    await page.goto('/dashboard');
    await expect(page).toHaveURL('/auth/login');
    
    await page.goto('/groups');
    await expect(page).toHaveURL('/auth/login');
    
    await page.goto('/expenses');
    await expect(page).toHaveURL('/auth/login');
    
    await page.goto('/balances');
    await expect(page).toHaveURL('/auth/login');
    
    await page.goto('/settings');
    await expect(page).toHaveURL('/auth/login');
  });
});
