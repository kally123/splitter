import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Groups List Page
 */
export class GroupsListPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly createGroupButton: Locator;
  readonly groupsList: Locator;
  readonly groupCards: Locator;
  readonly searchInput: Locator;
  readonly filterDropdown: Locator;
  readonly emptyState: Locator;
  readonly loadingSpinner: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h1');
    this.createGroupButton = page.locator('[data-testid="create-group-btn"]');
    this.groupsList = page.locator('[data-testid="groups-list"]');
    this.groupCards = page.locator('[data-testid="group-card"]');
    this.searchInput = page.locator('input[placeholder*="Search"]');
    this.filterDropdown = page.locator('[data-testid="filter-dropdown"]');
    this.emptyState = page.locator('[data-testid="empty-state"]');
    this.loadingSpinner = page.locator('[data-testid="loading-spinner"]');
  }

  async goto() {
    await this.page.goto('/groups');
    await expect(this.pageTitle).toBeVisible();
  }

  async expectPageLoaded() {
    await expect(this.pageTitle).toContainText('Groups');
  }

  async getGroupsCount(): Promise<number> {
    // Wait for loading to finish
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
    return await this.groupCards.count();
  }

  async clickCreateGroup() {
    await this.createGroupButton.click();
    await expect(this.page).toHaveURL('/groups/new');
  }

  async searchGroups(query: string) {
    await this.searchInput.fill(query);
    // Wait for debounced search
    await this.page.waitForTimeout(500);
  }

  async selectGroup(groupName: string) {
    await this.groupCards.filter({ hasText: groupName }).first().click();
  }

  async expectGroupExists(groupName: string) {
    await expect(this.groupCards.filter({ hasText: groupName })).toBeVisible();
  }

  async expectEmptyState() {
    await expect(this.emptyState).toBeVisible();
  }
}

/**
 * Page Object Model for Create Group Page
 */
export class CreateGroupPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly nameInput: Locator;
  readonly descriptionInput: Locator;
  readonly typeSelect: Locator;
  readonly currencySelect: Locator;
  readonly submitButton: Locator;
  readonly cancelButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h1');
    this.nameInput = page.locator('input[id="name"]');
    this.descriptionInput = page.locator('textarea[id="description"]');
    this.typeSelect = page.locator('select[id="type"]');
    this.currencySelect = page.locator('select[id="currency"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.cancelButton = page.locator('button:has-text("Cancel")');
    this.errorMessage = page.locator('[role="alert"]');
  }

  async goto() {
    await this.page.goto('/groups/new');
    await expect(this.nameInput).toBeVisible();
  }

  async createGroup(name: string, options: {
    description?: string;
    type?: string;
    currency?: string;
  } = {}) {
    await this.nameInput.fill(name);
    
    if (options.description) {
      await this.descriptionInput.fill(options.description);
    }
    
    if (options.type) {
      await this.typeSelect.selectOption(options.type);
    }
    
    if (options.currency) {
      await this.currencySelect.selectOption(options.currency);
    }
    
    await this.submitButton.click();
  }

  async expectError(message: string) {
    await expect(this.errorMessage).toContainText(message);
  }

  async cancel() {
    await this.cancelButton.click();
    await expect(this.page).toHaveURL('/groups');
  }
}

/**
 * Page Object Model for Group Detail Page
 */
export class GroupDetailPage {
  readonly page: Page;
  readonly groupName: Locator;
  readonly groupDescription: Locator;
  readonly membersList: Locator;
  readonly expensesList: Locator;
  readonly balancesSummary: Locator;
  readonly addExpenseButton: Locator;
  readonly inviteMemberButton: Locator;
  readonly settingsButton: Locator;
  readonly tabExpenses: Locator;
  readonly tabBalances: Locator;
  readonly tabMembers: Locator;
  readonly tabSettings: Locator;

  constructor(page: Page) {
    this.page = page;
    this.groupName = page.locator('h1');
    this.groupDescription = page.locator('[data-testid="group-description"]');
    this.membersList = page.locator('[data-testid="members-list"]');
    this.expensesList = page.locator('[data-testid="expenses-list"]');
    this.balancesSummary = page.locator('[data-testid="balances-summary"]');
    this.addExpenseButton = page.locator('[data-testid="add-expense-btn"]');
    this.inviteMemberButton = page.locator('[data-testid="invite-member-btn"]');
    this.settingsButton = page.locator('[data-testid="group-settings-btn"]');
    this.tabExpenses = page.locator('[data-testid="tab-expenses"]');
    this.tabBalances = page.locator('[data-testid="tab-balances"]');
    this.tabMembers = page.locator('[data-testid="tab-members"]');
    this.tabSettings = page.locator('[data-testid="tab-settings"]');
  }

  async goto(groupId: string) {
    await this.page.goto(`/groups/${groupId}`);
    await expect(this.groupName).toBeVisible();
  }

  async expectPageLoaded(expectedName: string) {
    await expect(this.groupName).toContainText(expectedName);
  }

  async getExpensesCount(): Promise<number> {
    await this.tabExpenses.click();
    return await this.expensesList.locator('.expense-item').count();
  }

  async getMembersCount(): Promise<number> {
    await this.tabMembers.click();
    return await this.membersList.locator('.member-item').count();
  }

  async clickAddExpense() {
    await this.addExpenseButton.click();
  }

  async clickInviteMember() {
    await this.inviteMemberButton.click();
  }

  async goToExpensesTab() {
    await this.tabExpenses.click();
    await expect(this.expensesList).toBeVisible();
  }

  async goToBalancesTab() {
    await this.tabBalances.click();
    await expect(this.balancesSummary).toBeVisible();
  }

  async goToMembersTab() {
    await this.tabMembers.click();
    await expect(this.membersList).toBeVisible();
  }

  async goToSettingsTab() {
    await this.tabSettings.click();
  }
}
