import { test, expect } from '@playwright/test';
import { ExpensesListPage, CreateExpensePage, ExpenseDetailPage } from '../pages/expenses.page';
import { generateTestExpense, STATIC_TEST_DATA } from '../fixtures/test-data';

test.describe('Expenses - List', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should display expenses list page', async ({ page }) => {
    const expensesPage = new ExpensesListPage(page);
    await expensesPage.goto();
    
    await expensesPage.expectPageLoaded();
    await expect(expensesPage.addExpenseButton).toBeVisible();
  });

  test('should navigate to create expense page', async ({ page }) => {
    const expensesPage = new ExpensesListPage(page);
    await expensesPage.goto();
    
    await expensesPage.clickAddExpense();
    
    await expect(page).toHaveURL('/expenses/new');
  });

  test('should filter expenses by search', async ({ page }) => {
    const expensesPage = new ExpensesListPage(page);
    await expensesPage.goto();
    
    await expensesPage.searchExpenses('Dinner');
    
    // Wait for search results
    await page.waitForTimeout(500);
    
    const expenseCards = page.locator('[data-testid="expense-card"]');
    const count = await expenseCards.count();
    
    for (let i = 0; i < count; i++) {
      const text = await expenseCards.nth(i).textContent();
      expect(text?.toLowerCase()).toContain('dinner');
    }
  });

  test('should filter expenses by category', async ({ page }) => {
    const expensesPage = new ExpensesListPage(page);
    await expensesPage.goto();
    
    await expensesPage.filterByCategory('FOOD');
    
    // Expenses should be filtered by category
    const expenseCards = page.locator('[data-testid="expense-card"]');
    const count = await expenseCards.count();
    
    for (let i = 0; i < count; i++) {
      await expect(expenseCards.nth(i).locator('[data-testid="expense-category"]')).toContainText('Food');
    }
  });
});

test.describe('Expenses - Create', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should display create expense form', async ({ page }) => {
    const createPage = new CreateExpensePage(page);
    await createPage.goto();
    
    await expect(createPage.descriptionInput).toBeVisible();
    await expect(createPage.amountInput).toBeVisible();
    await expect(createPage.categorySelect).toBeVisible();
    await expect(createPage.splitTypeSelect).toBeVisible();
    await expect(createPage.submitButton).toBeVisible();
  });

  test('should show validation error for empty description', async ({ page }) => {
    const createPage = new CreateExpensePage(page);
    await createPage.goto();
    
    await createPage.amountInput.fill('50');
    await createPage.submitButton.click();
    
    await expect(page.locator('text=Description is required')).toBeVisible();
  });

  test('should show validation error for zero amount', async ({ page }) => {
    const createPage = new CreateExpensePage(page);
    await createPage.goto();
    
    await createPage.descriptionInput.fill('Test Expense');
    await createPage.amountInput.fill('0');
    await createPage.submitButton.click();
    
    await expect(page.locator('text=Amount must be greater than 0')).toBeVisible();
  });

  test('should create expense with equal split', async ({ page }) => {
    // First ensure we have a group
    await page.goto('/groups');
    const firstGroup = page.locator('[data-testid="group-card"]').first();
    
    if (await firstGroup.isVisible()) {
      const groupName = await firstGroup.locator('[data-testid="group-name"]').textContent();
      
      const createPage = new CreateExpensePage(page);
      await createPage.goto();
      
      const expenseData = generateTestExpense();
      await createPage.createExpense({
        description: expenseData.description,
        amount: expenseData.amount,
        category: 'FOOD',
        splitType: 'EQUAL',
      });
      
      await createPage.expectSuccess();
    }
  });

  test('should create expense with percentage split', async ({ page }) => {
    await page.goto('/groups');
    const firstGroup = page.locator('[data-testid="group-card"]').first();
    
    if (await firstGroup.isVisible()) {
      const createPage = new CreateExpensePage(page);
      await createPage.goto();
      
      await createPage.descriptionInput.fill('Percentage Split Test');
      await createPage.amountInput.fill('100');
      await createPage.categorySelect.selectOption('OTHER');
      await createPage.splitTypeSelect.selectOption('PERCENTAGE');
      
      // Set percentages for members (if applicable)
      // This assumes split details are visible for percentage split
      
      await createPage.submitButton.click();
    }
  });

  test('should cancel expense creation', async ({ page }) => {
    const createPage = new CreateExpensePage(page);
    await createPage.goto();
    
    await createPage.descriptionInput.fill('Cancelled Expense');
    await createPage.cancel();
    
    await expect(page).not.toHaveURL('/expenses/new');
  });
});

test.describe('Expenses - Edit', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should edit expense description', async ({ page }) => {
    const expensesPage = new ExpensesListPage(page);
    await expensesPage.goto();
    
    const firstExpense = page.locator('[data-testid="expense-card"]').first();
    
    if (await firstExpense.isVisible()) {
      await firstExpense.locator('[data-testid="expense-menu"]').click();
      await page.locator('text=Edit').click();
      
      // Edit form should appear
      await expect(page.locator('input[id="description"]')).toBeVisible();
      
      // Update description
      await page.locator('input[id="description"]').fill('Updated Expense Description');
      await page.locator('button[type="submit"]').click();
      
      // Verify update
      await expect(page.locator('[data-testid="expense-card"]').first()).toContainText('Updated Expense Description');
    }
  });

  test('should edit expense amount', async ({ page }) => {
    const expensesPage = new ExpensesListPage(page);
    await expensesPage.goto();
    
    const firstExpense = page.locator('[data-testid="expense-card"]').first();
    
    if (await firstExpense.isVisible()) {
      await firstExpense.locator('[data-testid="expense-menu"]').click();
      await page.locator('text=Edit').click();
      
      await page.locator('input[id="amount"]').fill('150');
      await page.locator('button[type="submit"]').click();
      
      // Verify update
      await expect(page.locator('[data-testid="expense-card"]').first()).toContainText('$150');
    }
  });
});

test.describe('Expenses - Delete', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should delete expense with confirmation', async ({ page }) => {
    // First create an expense to delete
    const createPage = new CreateExpensePage(page);
    await createPage.goto();
    
    const expenseToDelete = `Delete Me ${Date.now()}`;
    await createPage.createExpense({
      description: expenseToDelete,
      amount: 25,
      category: 'OTHER',
    });
    
    // Navigate to expenses and delete
    const expensesPage = new ExpensesListPage(page);
    await expensesPage.goto();
    
    const initialCount = await expensesPage.getExpensesCount();
    
    await expensesPage.deleteExpense(expenseToDelete);
    
    // Verify deletion
    const newCount = await expensesPage.getExpensesCount();
    expect(newCount).toBe(initialCount - 1);
  });

  test('should cancel expense deletion', async ({ page }) => {
    const expensesPage = new ExpensesListPage(page);
    await expensesPage.goto();
    
    const firstExpense = page.locator('[data-testid="expense-card"]').first();
    
    if (await firstExpense.isVisible()) {
      const initialCount = await expensesPage.getExpensesCount();
      
      await firstExpense.locator('[data-testid="expense-menu"]').click();
      await page.locator('text=Delete').click();
      await page.locator('button:has-text("Cancel")').click();
      
      // Count should remain the same
      const newCount = await expensesPage.getExpensesCount();
      expect(newCount).toBe(initialCount);
    }
  });
});

test.describe('Expenses - Categories', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  const categories = STATIC_TEST_DATA.categories;

  for (const category of categories) {
    test(`should create expense with ${category} category`, async ({ page }) => {
      const createPage = new CreateExpensePage(page);
      await createPage.goto();
      
      await createPage.createExpense({
        description: `${category} Test Expense`,
        amount: 50,
        category: category,
        splitType: 'EQUAL',
      });
      
      // Should succeed without error
      await expect(page.locator('[role="alert"]')).not.toBeVisible();
    });
  }
});
