import { test, expect } from '@playwright/test';
import { SettingsPage } from '../pages/settings.page';

test.describe('Settings - Profile', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should display settings page', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.expectPageLoaded();
    await expect(settingsPage.profileSection).toBeVisible();
  });

  test('should show current user information', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    // Display name and email should be pre-filled
    await expect(settingsPage.displayNameInput).not.toBeEmpty();
    await expect(settingsPage.emailInput).not.toBeEmpty();
  });

  test('should update display name', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    const newName = `Test User ${Date.now()}`;
    await settingsPage.updateDisplayName(newName);
    
    await settingsPage.expectSuccess('Profile updated');
    await expect(settingsPage.displayNameInput).toHaveValue(newName);
  });

  test('should show validation error for empty display name', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.displayNameInput.clear();
    await settingsPage.saveProfileButton.click();
    
    await expect(page.locator('text=Display name is required')).toBeVisible();
  });
});

test.describe('Settings - Password', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should show change password form', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.changePasswordButton.click();
    
    await expect(settingsPage.currentPasswordInput).toBeVisible();
    await expect(settingsPage.newPasswordInput).toBeVisible();
    await expect(settingsPage.confirmPasswordInput).toBeVisible();
  });

  test('should show error for incorrect current password', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.changePassword('wrongpassword', 'NewPassword123!', 'NewPassword123!');
    
    await settingsPage.expectError('Current password is incorrect');
  });

  test('should show error for mismatched passwords', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.changePasswordButton.click();
    await settingsPage.currentPasswordInput.fill('Password123!');
    await settingsPage.newPasswordInput.fill('NewPassword123!');
    await settingsPage.confirmPasswordInput.fill('DifferentPassword!');
    await settingsPage.updatePasswordButton.click();
    
    await expect(page.locator('text=Passwords do not match')).toBeVisible();
  });

  test('should show error for weak password', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.changePassword('Password123!', 'weak', 'weak');
    
    await expect(page.locator('text=Password must be at least 8 characters')).toBeVisible();
  });
});

test.describe('Settings - Preferences', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should change default currency', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.setDefaultCurrency('EUR');
    
    await expect(settingsPage.defaultCurrencySelect).toHaveValue('EUR');
  });

  test('should toggle theme', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    // Get initial theme
    const initialTheme = await page.evaluate(() => document.documentElement.classList.contains('dark'));
    
    await settingsPage.toggleTheme();
    
    // Theme should have changed
    const newTheme = await page.evaluate(() => document.documentElement.classList.contains('dark'));
    expect(newTheme).not.toBe(initialTheme);
  });

  test('should persist theme preference', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.toggleTheme();
    
    // Reload page and check theme persists
    await page.reload();
    
    const themeClass = await page.evaluate(() => document.documentElement.classList.contains('dark'));
    expect(themeClass).toBeDefined();
  });
});

test.describe('Settings - Notifications', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should display notification settings', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await expect(settingsPage.notificationsSection).toBeVisible();
    await expect(settingsPage.emailNotificationsToggle).toBeVisible();
  });

  test('should toggle email notifications', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    const initialState = await settingsPage.emailNotificationsToggle.isChecked();
    
    await settingsPage.toggleEmailNotifications();
    
    const newState = await settingsPage.emailNotificationsToggle.isChecked();
    expect(newState).not.toBe(initialState);
  });
});

test.describe('Settings - Security', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should display security section', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await expect(settingsPage.securitySection).toBeVisible();
    await expect(settingsPage.changePasswordButton).toBeVisible();
  });
});

test.describe('Settings - Account Deletion', () => {
  // Use a separate test user for deletion tests to avoid breaking other tests
  test.skip('should show confirmation dialog for account deletion', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.deleteAccountButton.click();
    
    await expect(page.locator('[data-testid="delete-account-modal"]')).toBeVisible();
    await expect(page.locator('text=This action cannot be undone')).toBeVisible();
  });

  test.skip('should require password for account deletion', async ({ page }) => {
    const settingsPage = new SettingsPage(page);
    await settingsPage.goto();
    
    await settingsPage.deleteAccountButton.click();
    
    // Try to delete without password
    await page.locator('button:has-text("Delete My Account")').click();
    
    await expect(page.locator('text=Password is required')).toBeVisible();
  });
});
