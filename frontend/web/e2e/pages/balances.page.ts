import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Balances Page
 */
export class BalancesPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly totalBalanceCard: Locator;
  readonly youOweSection: Locator;
  readonly youAreOwedSection: Locator;
  readonly balancesList: Locator;
  readonly balanceRows: Locator;
  readonly groupFilter: Locator;
  readonly settleUpButton: Locator;
  readonly emptyState: Locator;
  readonly loadingSpinner: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h1');
    this.totalBalanceCard = page.locator('[data-testid="total-balance"]');
    this.youOweSection = page.locator('[data-testid="you-owe-section"]');
    this.youAreOwedSection = page.locator('[data-testid="you-are-owed-section"]');
    this.balancesList = page.locator('[data-testid="balances-list"]');
    this.balanceRows = page.locator('[data-testid="balance-row"]');
    this.groupFilter = page.locator('[data-testid="group-filter"]');
    this.settleUpButton = page.locator('[data-testid="settle-up-btn"]');
    this.emptyState = page.locator('[data-testid="empty-state"]');
    this.loadingSpinner = page.locator('[data-testid="loading-spinner"]');
  }

  async goto() {
    await this.page.goto('/balances');
    await expect(this.pageTitle).toBeVisible();
  }

  async expectPageLoaded() {
    await expect(this.pageTitle).toContainText('Balances');
  }

  async getTotalBalance(): Promise<string> {
    return await this.totalBalanceCard.locator('.balance-amount').textContent() || '0';
  }

  async getBalancesCount(): Promise<number> {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
    return await this.balanceRows.count();
  }

  async filterByGroup(groupName: string) {
    await this.groupFilter.click();
    await this.page.locator(`[data-testid="group-option"]:has-text("${groupName}")`).click();
  }

  async clickSettleForUser(userName: string) {
    const balanceRow = this.balanceRows.filter({ hasText: userName }).first();
    await balanceRow.locator('[data-testid="settle-btn"]').click();
  }

  async expectBalanceWithUser(userName: string, expectedAmount?: string) {
    const balanceRow = this.balanceRows.filter({ hasText: userName });
    await expect(balanceRow).toBeVisible();
    
    if (expectedAmount) {
      await expect(balanceRow).toContainText(expectedAmount);
    }
  }

  async expectNoBalances() {
    await expect(this.emptyState).toBeVisible();
  }
}

/**
 * Page Object Model for Settle Up Modal
 */
export class SettleUpModal {
  readonly page: Page;
  readonly modal: Locator;
  readonly amountInput: Locator;
  readonly paymentMethodSelect: Locator;
  readonly noteInput: Locator;
  readonly recordButton: Locator;
  readonly cancelButton: Locator;
  readonly errorMessage: Locator;
  readonly successMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.modal = page.locator('[data-testid="settle-up-modal"]');
    this.amountInput = page.locator('input[id="amount"]');
    this.paymentMethodSelect = page.locator('select[id="paymentMethod"]');
    this.noteInput = page.locator('textarea[id="note"]');
    this.recordButton = page.locator('button:has-text("Record Payment")');
    this.cancelButton = page.locator('button:has-text("Cancel")');
    this.errorMessage = page.locator('[role="alert"]');
    this.successMessage = page.locator('[data-testid="success-message"]');
  }

  async expectVisible() {
    await expect(this.modal).toBeVisible();
  }

  async recordSettlement(options: {
    amount: number;
    paymentMethod?: string;
    note?: string;
  }) {
    await this.amountInput.fill(options.amount.toString());
    
    if (options.paymentMethod) {
      await this.paymentMethodSelect.selectOption(options.paymentMethod);
    }
    
    if (options.note) {
      await this.noteInput.fill(options.note);
    }
    
    await this.recordButton.click();
  }

  async expectError(message: string) {
    await expect(this.errorMessage).toContainText(message);
  }

  async expectSuccess() {
    await expect(this.modal).not.toBeVisible();
  }

  async cancel() {
    await this.cancelButton.click();
    await expect(this.modal).not.toBeVisible();
  }
}
