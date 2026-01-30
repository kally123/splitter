import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Expenses List Page
 */
export class ExpensesListPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly addExpenseButton: Locator;
  readonly expensesList: Locator;
  readonly expenseCards: Locator;
  readonly searchInput: Locator;
  readonly groupFilter: Locator;
  readonly categoryFilter: Locator;
  readonly dateRangeFilter: Locator;
  readonly emptyState: Locator;
  readonly loadingSpinner: Locator;
  readonly totalAmount: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h1');
    this.addExpenseButton = page.locator('[data-testid="add-expense-btn"]');
    this.expensesList = page.locator('[data-testid="expenses-list"]');
    this.expenseCards = page.locator('[data-testid="expense-card"]');
    this.searchInput = page.locator('input[placeholder*="Search"]');
    this.groupFilter = page.locator('[data-testid="group-filter"]');
    this.categoryFilter = page.locator('[data-testid="category-filter"]');
    this.dateRangeFilter = page.locator('[data-testid="date-range-filter"]');
    this.emptyState = page.locator('[data-testid="empty-state"]');
    this.loadingSpinner = page.locator('[data-testid="loading-spinner"]');
    this.totalAmount = page.locator('[data-testid="total-amount"]');
  }

  async goto() {
    await this.page.goto('/expenses');
    await expect(this.pageTitle).toBeVisible();
  }

  async expectPageLoaded() {
    await expect(this.pageTitle).toContainText('Expenses');
  }

  async getExpensesCount(): Promise<number> {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
    return await this.expenseCards.count();
  }

  async clickAddExpense() {
    await this.addExpenseButton.click();
    await expect(this.page).toHaveURL('/expenses/new');
  }

  async searchExpenses(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForTimeout(500);
  }

  async filterByGroup(groupName: string) {
    await this.groupFilter.click();
    await this.page.locator(`[data-testid="group-option"]:has-text("${groupName}")`).click();
  }

  async filterByCategory(category: string) {
    await this.categoryFilter.click();
    await this.page.locator(`[data-testid="category-option"]:has-text("${category}")`).click();
  }

  async selectExpense(description: string) {
    await this.expenseCards.filter({ hasText: description }).first().click();
  }

  async expectExpenseExists(description: string) {
    await expect(this.expenseCards.filter({ hasText: description })).toBeVisible();
  }

  async expectEmptyState() {
    await expect(this.emptyState).toBeVisible();
  }

  async deleteExpense(description: string) {
    const expenseCard = this.expenseCards.filter({ hasText: description }).first();
    await expenseCard.locator('[data-testid="expense-menu"]').click();
    await this.page.locator('text=Delete').click();
    await this.page.locator('button:has-text("Confirm")').click();
  }
}

/**
 * Page Object Model for Create Expense Page
 */
export class CreateExpensePage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly groupSelect: Locator;
  readonly descriptionInput: Locator;
  readonly amountInput: Locator;
  readonly currencySelect: Locator;
  readonly categorySelect: Locator;
  readonly dateInput: Locator;
  readonly splitTypeSelect: Locator;
  readonly splitDetails: Locator;
  readonly paidBySelect: Locator;
  readonly noteInput: Locator;
  readonly submitButton: Locator;
  readonly cancelButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h1');
    this.groupSelect = page.locator('select[id="groupId"]');
    this.descriptionInput = page.locator('input[id="description"]');
    this.amountInput = page.locator('input[id="amount"]');
    this.currencySelect = page.locator('select[id="currency"]');
    this.categorySelect = page.locator('select[id="category"]');
    this.dateInput = page.locator('input[id="date"]');
    this.splitTypeSelect = page.locator('select[id="splitType"]');
    this.splitDetails = page.locator('[data-testid="split-details"]');
    this.paidBySelect = page.locator('select[id="paidBy"]');
    this.noteInput = page.locator('textarea[id="note"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.cancelButton = page.locator('button:has-text("Cancel")');
    this.errorMessage = page.locator('[role="alert"]');
  }

  async goto() {
    await this.page.goto('/expenses/new');
    await expect(this.descriptionInput).toBeVisible();
  }

  async createExpense(options: {
    groupId?: string;
    description: string;
    amount: number;
    category?: string;
    splitType?: string;
    note?: string;
  }) {
    if (options.groupId) {
      await this.groupSelect.selectOption(options.groupId);
    }
    
    await this.descriptionInput.fill(options.description);
    await this.amountInput.fill(options.amount.toString());
    
    if (options.category) {
      await this.categorySelect.selectOption(options.category);
    }
    
    if (options.splitType) {
      await this.splitTypeSelect.selectOption(options.splitType);
    }
    
    if (options.note) {
      await this.noteInput.fill(options.note);
    }
    
    await this.submitButton.click();
  }

  async expectError(message: string) {
    await expect(this.errorMessage).toContainText(message);
  }

  async expectSuccess() {
    // Should redirect to group or expenses page
    await expect(this.page).not.toHaveURL('/expenses/new');
  }

  async cancel() {
    await this.cancelButton.click();
  }

  async setSplitPercentage(memberIndex: number, percentage: number) {
    const percentageInput = this.splitDetails.locator(`input[name="splits.${memberIndex}.percentage"]`);
    await percentageInput.fill(percentage.toString());
  }

  async setSplitAmount(memberIndex: number, amount: number) {
    const amountInput = this.splitDetails.locator(`input[name="splits.${memberIndex}.amount"]`);
    await amountInput.fill(amount.toString());
  }

  async setSplitShares(memberIndex: number, shares: number) {
    const sharesInput = this.splitDetails.locator(`input[name="splits.${memberIndex}.shares"]`);
    await sharesInput.fill(shares.toString());
  }
}

/**
 * Page Object Model for Expense Detail Page
 */
export class ExpenseDetailPage {
  readonly page: Page;
  readonly expenseDescription: Locator;
  readonly expenseAmount: Locator;
  readonly expenseCategory: Locator;
  readonly expenseDate: Locator;
  readonly paidBy: Locator;
  readonly splitsList: Locator;
  readonly editButton: Locator;
  readonly deleteButton: Locator;
  readonly backButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.expenseDescription = page.locator('[data-testid="expense-description"]');
    this.expenseAmount = page.locator('[data-testid="expense-amount"]');
    this.expenseCategory = page.locator('[data-testid="expense-category"]');
    this.expenseDate = page.locator('[data-testid="expense-date"]');
    this.paidBy = page.locator('[data-testid="paid-by"]');
    this.splitsList = page.locator('[data-testid="splits-list"]');
    this.editButton = page.locator('[data-testid="edit-expense-btn"]');
    this.deleteButton = page.locator('[data-testid="delete-expense-btn"]');
    this.backButton = page.locator('[data-testid="back-btn"]');
  }

  async expectLoaded(description: string) {
    await expect(this.expenseDescription).toContainText(description);
  }

  async getAmount(): Promise<string> {
    return await this.expenseAmount.textContent() || '';
  }

  async getSplitsCount(): Promise<number> {
    return await this.splitsList.locator('.split-item').count();
  }

  async clickEdit() {
    await this.editButton.click();
  }

  async clickDelete() {
    await this.deleteButton.click();
    await this.page.locator('button:has-text("Confirm")').click();
  }

  async goBack() {
    await this.backButton.click();
  }
}
