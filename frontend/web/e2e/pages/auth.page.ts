import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Login Page
 */
export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMessage: Locator;
  readonly registerLink: Locator;
  readonly forgotPasswordLink: Locator;
  readonly passwordToggle: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.locator('input[id="email"]');
    this.passwordInput = page.locator('input[id="password"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.errorMessage = page.locator('[role="alert"]');
    this.registerLink = page.locator('a[href="/auth/register"]');
    this.forgotPasswordLink = page.locator('a[href="/auth/forgot-password"]');
    this.passwordToggle = page.locator('[data-testid="password-toggle"]');
  }

  async goto() {
    await this.page.goto('/auth/login');
    await expect(this.emailInput).toBeVisible();
  }

  async login(email: string, password: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.submitButton.click();
  }

  async expectError(message: string) {
    await expect(this.errorMessage).toContainText(message);
  }

  async expectSuccess() {
    await expect(this.page).toHaveURL('/dashboard');
  }

  async clearForm() {
    await this.emailInput.clear();
    await this.passwordInput.clear();
  }

  async togglePasswordVisibility() {
    await this.passwordToggle.click();
  }

  async goToRegister() {
    await this.registerLink.click();
    await expect(this.page).toHaveURL('/auth/register');
  }
}

/**
 * Page Object Model for Register Page
 */
export class RegisterPage {
  readonly page: Page;
  readonly displayNameInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMessage: Locator;
  readonly loginLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.displayNameInput = page.locator('input[id="displayName"]');
    this.emailInput = page.locator('input[id="email"]');
    this.passwordInput = page.locator('input[id="password"]');
    this.confirmPasswordInput = page.locator('input[id="confirmPassword"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.errorMessage = page.locator('[role="alert"]');
    this.loginLink = page.locator('a[href="/auth/login"]');
  }

  async goto() {
    await this.page.goto('/auth/register');
    await expect(this.emailInput).toBeVisible();
  }

  async register(displayName: string, email: string, password: string, confirmPassword?: string) {
    await this.displayNameInput.fill(displayName);
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.confirmPasswordInput.fill(confirmPassword || password);
    await this.submitButton.click();
  }

  async expectError(message: string) {
    await expect(this.errorMessage).toContainText(message);
  }

  async expectSuccess() {
    await expect(this.page).toHaveURL('/dashboard');
  }

  async goToLogin() {
    await this.loginLink.click();
    await expect(this.page).toHaveURL('/auth/login');
  }
}
