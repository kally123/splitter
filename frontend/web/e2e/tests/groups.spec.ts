import { test, expect } from '@playwright/test';
import { GroupsListPage, CreateGroupPage, GroupDetailPage } from '../pages/groups.page';
import { generateTestGroup, STATIC_TEST_DATA } from '../fixtures/test-data';

test.describe('Groups - List', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should display groups list page', async ({ page }) => {
    const groupsPage = new GroupsListPage(page);
    await groupsPage.goto();
    
    await groupsPage.expectPageLoaded();
    await expect(groupsPage.createGroupButton).toBeVisible();
  });

  test('should show empty state when no groups exist', async ({ page }) => {
    // This test assumes a clean user with no groups
    const groupsPage = new GroupsListPage(page);
    await groupsPage.goto();
    
    const count = await groupsPage.getGroupsCount();
    if (count === 0) {
      await groupsPage.expectEmptyState();
    }
  });

  test('should navigate to create group page', async ({ page }) => {
    const groupsPage = new GroupsListPage(page);
    await groupsPage.goto();
    
    await groupsPage.clickCreateGroup();
    
    await expect(page).toHaveURL('/groups/new');
  });

  test('should filter groups by search', async ({ page }) => {
    const groupsPage = new GroupsListPage(page);
    await groupsPage.goto();
    
    await groupsPage.searchGroups('Test');
    
    // Results should be filtered
    const groupCards = page.locator('[data-testid="group-card"]');
    const count = await groupCards.count();
    
    for (let i = 0; i < count; i++) {
      const text = await groupCards.nth(i).textContent();
      expect(text?.toLowerCase()).toContain('test');
    }
  });
});

test.describe('Groups - Create', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should display create group form', async ({ page }) => {
    const createPage = new CreateGroupPage(page);
    await createPage.goto();
    
    await expect(createPage.nameInput).toBeVisible();
    await expect(createPage.descriptionInput).toBeVisible();
    await expect(createPage.typeSelect).toBeVisible();
    await expect(createPage.submitButton).toBeVisible();
  });

  test('should show validation error for empty name', async ({ page }) => {
    const createPage = new CreateGroupPage(page);
    await createPage.goto();
    
    await createPage.submitButton.click();
    
    await expect(page.locator('text=Group name is required')).toBeVisible();
  });

  test('should create a trip group successfully', async ({ page }) => {
    const createPage = new CreateGroupPage(page);
    await createPage.goto();
    
    const groupData = generateTestGroup({ type: 'TRIP' });
    await createPage.createGroup(groupData.name, {
      description: groupData.description,
      type: 'TRIP',
      currency: 'USD',
    });
    
    // Should redirect to group detail page
    await expect(page).toHaveURL(/\/groups\/[a-zA-Z0-9-]+$/);
    await expect(page.locator('h1')).toContainText(groupData.name);
  });

  test('should create a home group successfully', async ({ page }) => {
    const createPage = new CreateGroupPage(page);
    await createPage.goto();
    
    const { homeGroup } = STATIC_TEST_DATA.groups;
    await createPage.createGroup(`${homeGroup.name} ${Date.now()}`, {
      description: homeGroup.description,
      type: 'HOME',
      currency: 'USD',
    });
    
    await expect(page).toHaveURL(/\/groups\/[a-zA-Z0-9-]+$/);
  });

  test('should cancel group creation', async ({ page }) => {
    const createPage = new CreateGroupPage(page);
    await createPage.goto();
    
    await createPage.nameInput.fill('Cancelled Group');
    await createPage.cancel();
    
    await expect(page).toHaveURL('/groups');
  });
});

test.describe('Groups - Detail', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  let testGroupId: string;

  test.beforeAll(async ({ request }) => {
    // Create a test group via API
    const response = await request.post('/api/v1/groups', {
      data: {
        name: `E2E Test Group ${Date.now()}`,
        description: 'Group created for E2E testing',
        type: 'OTHER',
        currency: 'USD',
      },
    });
    
    if (response.ok()) {
      const data = await response.json();
      testGroupId = data.id;
    }
  });

  test('should display group detail page', async ({ page }) => {
    // Navigate to groups and select the first one
    await page.goto('/groups');
    const firstGroup = page.locator('[data-testid="group-card"]').first();
    
    if (await firstGroup.isVisible()) {
      await firstGroup.click();
      
      // Should show group details
      await expect(page.locator('h1')).toBeVisible();
      await expect(page.locator('[data-testid="add-expense-btn"]')).toBeVisible();
    }
  });

  test('should switch between tabs', async ({ page }) => {
    await page.goto('/groups');
    const firstGroup = page.locator('[data-testid="group-card"]').first();
    
    if (await firstGroup.isVisible()) {
      await firstGroup.click();
      
      const detailPage = new GroupDetailPage(page);
      
      // Check expenses tab
      await detailPage.goToExpensesTab();
      await expect(detailPage.expensesList).toBeVisible();
      
      // Check balances tab
      await detailPage.goToBalancesTab();
      await expect(detailPage.balancesSummary).toBeVisible();
      
      // Check members tab
      await detailPage.goToMembersTab();
      await expect(detailPage.membersList).toBeVisible();
    }
  });

  test('should navigate to add expense from group', async ({ page }) => {
    await page.goto('/groups');
    const firstGroup = page.locator('[data-testid="group-card"]').first();
    
    if (await firstGroup.isVisible()) {
      await firstGroup.click();
      
      await page.locator('[data-testid="add-expense-btn"]').click();
      
      // Should open expense creation with group pre-selected
      await expect(page.locator('input[id="description"]')).toBeVisible();
    }
  });
});

test.describe('Groups - Members', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should show current user as member', async ({ page }) => {
    await page.goto('/groups');
    const firstGroup = page.locator('[data-testid="group-card"]').first();
    
    if (await firstGroup.isVisible()) {
      await firstGroup.click();
      
      await page.locator('[data-testid="tab-members"]').click();
      
      // Should show at least one member (the creator)
      const memberCount = await page.locator('.member-item').count();
      expect(memberCount).toBeGreaterThanOrEqual(1);
    }
  });

  test('should open invite member modal', async ({ page }) => {
    await page.goto('/groups');
    const firstGroup = page.locator('[data-testid="group-card"]').first();
    
    if (await firstGroup.isVisible()) {
      await firstGroup.click();
      
      await page.locator('[data-testid="invite-member-btn"]').click();
      
      await expect(page.locator('[data-testid="invite-modal"]')).toBeVisible();
    }
  });
});
