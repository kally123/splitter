import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Settings Page
 */
export class SettingsPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  
  // Profile section
  readonly profileSection: Locator;
  readonly displayNameInput: Locator;
  readonly emailInput: Locator;
  readonly avatarUpload: Locator;
  readonly saveProfileButton: Locator;
  
  // Preferences section
  readonly preferencesSection: Locator;
  readonly defaultCurrencySelect: Locator;
  readonly themeToggle: Locator;
  readonly languageSelect: Locator;
  
  // Notifications section
  readonly notificationsSection: Locator;
  readonly emailNotificationsToggle: Locator;
  readonly pushNotificationsToggle: Locator;
  readonly expenseNotificationsToggle: Locator;
  readonly settlementNotificationsToggle: Locator;
  
  // Security section
  readonly securitySection: Locator;
  readonly changePasswordButton: Locator;
  readonly currentPasswordInput: Locator;
  readonly newPasswordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly updatePasswordButton: Locator;
  
  // Danger zone
  readonly dangerZoneSection: Locator;
  readonly deleteAccountButton: Locator;
  
  // Messages
  readonly successMessage: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h1');
    
    // Profile
    this.profileSection = page.locator('[data-testid="profile-section"]');
    this.displayNameInput = page.locator('input[id="displayName"]');
    this.emailInput = page.locator('input[id="email"]');
    this.avatarUpload = page.locator('input[type="file"]');
    this.saveProfileButton = page.locator('[data-testid="save-profile-btn"]');
    
    // Preferences
    this.preferencesSection = page.locator('[data-testid="preferences-section"]');
    this.defaultCurrencySelect = page.locator('select[id="defaultCurrency"]');
    this.themeToggle = page.locator('[data-testid="theme-toggle"]');
    this.languageSelect = page.locator('select[id="language"]');
    
    // Notifications
    this.notificationsSection = page.locator('[data-testid="notifications-section"]');
    this.emailNotificationsToggle = page.locator('[data-testid="email-notifications-toggle"]');
    this.pushNotificationsToggle = page.locator('[data-testid="push-notifications-toggle"]');
    this.expenseNotificationsToggle = page.locator('[data-testid="expense-notifications-toggle"]');
    this.settlementNotificationsToggle = page.locator('[data-testid="settlement-notifications-toggle"]');
    
    // Security
    this.securitySection = page.locator('[data-testid="security-section"]');
    this.changePasswordButton = page.locator('[data-testid="change-password-btn"]');
    this.currentPasswordInput = page.locator('input[id="currentPassword"]');
    this.newPasswordInput = page.locator('input[id="newPassword"]');
    this.confirmPasswordInput = page.locator('input[id="confirmNewPassword"]');
    this.updatePasswordButton = page.locator('[data-testid="update-password-btn"]');
    
    // Danger zone
    this.dangerZoneSection = page.locator('[data-testid="danger-zone"]');
    this.deleteAccountButton = page.locator('[data-testid="delete-account-btn"]');
    
    // Messages
    this.successMessage = page.locator('[data-testid="success-toast"]');
    this.errorMessage = page.locator('[role="alert"]');
  }

  async goto() {
    await this.page.goto('/settings');
    await expect(this.pageTitle).toBeVisible();
  }

  async expectPageLoaded() {
    await expect(this.pageTitle).toContainText('Settings');
  }

  async updateDisplayName(name: string) {
    await this.displayNameInput.clear();
    await this.displayNameInput.fill(name);
    await this.saveProfileButton.click();
  }

  async changePassword(currentPassword: string, newPassword: string, confirmPassword?: string) {
    await this.changePasswordButton.click();
    await this.currentPasswordInput.fill(currentPassword);
    await this.newPasswordInput.fill(newPassword);
    await this.confirmPasswordInput.fill(confirmPassword || newPassword);
    await this.updatePasswordButton.click();
  }

  async setDefaultCurrency(currency: string) {
    await this.defaultCurrencySelect.selectOption(currency);
  }

  async toggleTheme() {
    await this.themeToggle.click();
  }

  async toggleEmailNotifications() {
    await this.emailNotificationsToggle.click();
  }

  async expectSuccess(message?: string) {
    await expect(this.successMessage).toBeVisible();
    if (message) {
      await expect(this.successMessage).toContainText(message);
    }
  }

  async expectError(message: string) {
    await expect(this.errorMessage).toContainText(message);
  }

  async deleteAccount(confirmPassword: string) {
    await this.deleteAccountButton.click();
    // Confirmation modal
    await this.page.locator('[data-testid="confirm-password-input"]').fill(confirmPassword);
    await this.page.locator('button:has-text("Delete My Account")').click();
  }
}
