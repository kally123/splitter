import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Dashboard Page
 */
export class DashboardPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly welcomeMessage: Locator;
  readonly totalBalanceCard: Locator;
  readonly youOweCard: Locator;
  readonly youAreOwedCard: Locator;
  readonly recentActivitySection: Locator;
  readonly recentExpensesList: Locator;
  readonly groupsSummarySection: Locator;
  readonly quickActionsSection: Locator;
  readonly addExpenseButton: Locator;
  readonly createGroupButton: Locator;
  readonly settleUpButton: Locator;
  readonly notificationBell: Locator;
  readonly userMenu: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h1');
    this.welcomeMessage = page.locator('[data-testid="welcome-message"]');
    this.totalBalanceCard = page.locator('[data-testid="total-balance-card"]');
    this.youOweCard = page.locator('[data-testid="you-owe-card"]');
    this.youAreOwedCard = page.locator('[data-testid="you-are-owed-card"]');
    this.recentActivitySection = page.locator('[data-testid="recent-activity"]');
    this.recentExpensesList = page.locator('[data-testid="recent-expenses-list"]');
    this.groupsSummarySection = page.locator('[data-testid="groups-summary"]');
    this.quickActionsSection = page.locator('[data-testid="quick-actions"]');
    this.addExpenseButton = page.locator('[data-testid="add-expense-btn"]');
    this.createGroupButton = page.locator('[data-testid="create-group-btn"]');
    this.settleUpButton = page.locator('[data-testid="settle-up-btn"]');
    this.notificationBell = page.locator('[data-testid="notification-bell"]');
    this.userMenu = page.locator('[data-testid="user-menu"]');
  }

  async goto() {
    await this.page.goto('/dashboard');
    await expect(this.pageTitle).toBeVisible();
  }

  async expectPageLoaded() {
    await expect(this.pageTitle).toContainText('Dashboard');
    await expect(this.totalBalanceCard).toBeVisible();
  }

  async getTotalBalance(): Promise<string> {
    return await this.totalBalanceCard.locator('.balance-amount').textContent() || '0';
  }

  async getYouOweAmount(): Promise<string> {
    return await this.youOweCard.locator('.balance-amount').textContent() || '0';
  }

  async getYouAreOwedAmount(): Promise<string> {
    return await this.youAreOwedCard.locator('.balance-amount').textContent() || '0';
  }

  async clickAddExpense() {
    await this.addExpenseButton.click();
  }

  async clickCreateGroup() {
    await this.createGroupButton.click();
  }

  async clickSettleUp() {
    await this.settleUpButton.click();
  }

  async openNotifications() {
    await this.notificationBell.click();
  }

  async openUserMenu() {
    await this.userMenu.click();
  }

  async logout() {
    await this.openUserMenu();
    await this.page.locator('text=Log out').click();
    await expect(this.page).toHaveURL('/auth/login');
  }

  async getRecentExpensesCount(): Promise<number> {
    return await this.recentExpensesList.locator('.expense-item').count();
  }

  async navigateToGroups() {
    await this.page.locator('nav a[href="/groups"]').click();
    await expect(this.page).toHaveURL('/groups');
  }

  async navigateToExpenses() {
    await this.page.locator('nav a[href="/expenses"]').click();
    await expect(this.page).toHaveURL('/expenses');
  }

  async navigateToBalances() {
    await this.page.locator('nav a[href="/balances"]').click();
    await expect(this.page).toHaveURL('/balances');
  }

  async navigateToActivity() {
    await this.page.locator('nav a[href="/activity"]').click();
    await expect(this.page).toHaveURL('/activity');
  }

  async navigateToSettings() {
    await this.page.locator('nav a[href="/settings"]').click();
    await expect(this.page).toHaveURL('/settings');
  }
}
