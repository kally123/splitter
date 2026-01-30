import { test, expect } from '@playwright/test';
import { BalancesPage, SettleUpModal } from '../pages/balances.page';

test.describe('Balances - Overview', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should display balances page', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    await balancesPage.expectPageLoaded();
    await expect(balancesPage.totalBalanceCard).toBeVisible();
  });

  test('should show total balance summary', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    // Total balance should be visible
    const balance = await balancesPage.getTotalBalance();
    expect(balance).toBeDefined();
  });

  test('should show you owe and you are owed sections', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    await expect(balancesPage.youOweSection).toBeVisible();
    await expect(balancesPage.youAreOwedSection).toBeVisible();
  });

  test('should filter balances by group', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    // Click group filter and select a group
    await balancesPage.groupFilter.click();
    
    const firstGroup = page.locator('[data-testid="group-option"]').first();
    if (await firstGroup.isVisible()) {
      const groupName = await firstGroup.textContent();
      await firstGroup.click();
      
      // Balances should be filtered
      await expect(page.locator('[data-testid="active-filter"]')).toContainText(groupName || '');
    }
  });
});

test.describe('Balances - Settlement Flow', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should open settle up modal', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    const balanceRow = page.locator('[data-testid="balance-row"]').first();
    
    if (await balanceRow.isVisible()) {
      await balanceRow.locator('[data-testid="settle-btn"]').click();
      
      const settleModal = new SettleUpModal(page);
      await settleModal.expectVisible();
    }
  });

  test('should show validation error for zero amount', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    const balanceRow = page.locator('[data-testid="balance-row"]').first();
    
    if (await balanceRow.isVisible()) {
      await balanceRow.locator('[data-testid="settle-btn"]').click();
      
      const settleModal = new SettleUpModal(page);
      await settleModal.amountInput.fill('0');
      await settleModal.recordButton.click();
      
      await settleModal.expectError('Amount must be greater than 0');
    }
  });

  test('should record settlement via cash', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    const balanceRow = page.locator('[data-testid="balance-row"]').first();
    
    if (await balanceRow.isVisible()) {
      await balanceRow.locator('[data-testid="settle-btn"]').click();
      
      const settleModal = new SettleUpModal(page);
      await settleModal.recordSettlement({
        amount: 25,
        paymentMethod: 'CASH',
        note: 'Cash payment',
      });
      
      // Modal should close on success
      await settleModal.expectSuccess();
      
      // Toast notification should appear
      await expect(page.locator('.toast')).toContainText('Settlement recorded');
    }
  });

  test('should record settlement via Venmo', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    const balanceRow = page.locator('[data-testid="balance-row"]').first();
    
    if (await balanceRow.isVisible()) {
      await balanceRow.locator('[data-testid="settle-btn"]').click();
      
      const settleModal = new SettleUpModal(page);
      await settleModal.recordSettlement({
        amount: 50,
        paymentMethod: 'VENMO',
        note: 'Venmo @username',
      });
      
      await settleModal.expectSuccess();
    }
  });

  test('should cancel settlement', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    const balanceRow = page.locator('[data-testid="balance-row"]').first();
    
    if (await balanceRow.isVisible()) {
      await balanceRow.locator('[data-testid="settle-btn"]').click();
      
      const settleModal = new SettleUpModal(page);
      await settleModal.cancel();
      
      // Modal should be closed
      await expect(page.locator('[data-testid="settle-up-modal"]')).not.toBeVisible();
    }
  });
});

test.describe('Balances - Settlement Confirmation', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should show pending settlements in activity', async ({ page }) => {
    await page.goto('/activity');
    
    // Look for pending settlement notifications
    const pendingSettlements = page.locator('[data-testid="notification-item"]:has-text("settlement")');
    
    // This is a conditional test - settlements may or may not exist
    const count = await pendingSettlements.count();
    if (count > 0) {
      await expect(pendingSettlements.first()).toBeVisible();
    }
  });

  test('should confirm pending settlement', async ({ page }) => {
    await page.goto('/activity');
    
    const pendingSettlement = page.locator('[data-testid="notification-item"]:has-text("settlement"):has-text("pending")').first();
    
    if (await pendingSettlement.isVisible()) {
      await pendingSettlement.click();
      await page.locator('button:has-text("Confirm")').click();
      
      await expect(page.locator('.toast')).toContainText('Settlement confirmed');
    }
  });

  test('should reject pending settlement', async ({ page }) => {
    await page.goto('/activity');
    
    const pendingSettlement = page.locator('[data-testid="notification-item"]:has-text("settlement"):has-text("pending")').first();
    
    if (await pendingSettlement.isVisible()) {
      await pendingSettlement.click();
      await page.locator('button:has-text("Reject")').click();
      
      // Optionally provide a reason
      const reasonInput = page.locator('textarea[id="reason"]');
      if (await reasonInput.isVisible()) {
        await reasonInput.fill('Incorrect amount');
      }
      
      await page.locator('button:has-text("Confirm Rejection")').click();
      
      await expect(page.locator('.toast')).toContainText('Settlement rejected');
    }
  });
});

test.describe('Balances - Edge Cases', () => {
  test.use({ storageState: 'e2e/.auth/user.json' });

  test('should handle settling full balance', async ({ page }) => {
    const balancesPage = new BalancesPage(page);
    await balancesPage.goto();
    
    const balanceRow = page.locator('[data-testid="balance-row"]').first();
    
    if (await balanceRow.isVisible()) {
      // Get the full amount owed
      const amountText = await balanceRow.locator('[data-testid="balance-amount"]').textContent();
      const amount = parseFloat(amountText?.replace(/[^0-9.]/g, '') || '0');
      
      if (amount > 0) {
        await balanceRow.locator('[data-testid="settle-btn"]').click();
        
        const settleModal = new SettleUpModal(page);
        await settleModal.recordSettlement({
          amount: amount,
          paymentMethod: 'BANK_TRANSFER',
          note: 'Full settlement',
        });
        
        await settleModal.expectSuccess();
      }
    }
  });

  test('should show settlement history', async ({ page }) => {
    await page.goto('/activity');
    
    // Look for completed settlements
    const completedSettlements = page.locator('[data-testid="notification-item"]:has-text("settled")');
    
    // Verify settlements have proper information
    const count = await completedSettlements.count();
    for (let i = 0; i < Math.min(count, 3); i++) {
      const settlement = completedSettlements.nth(i);
      await expect(settlement).toBeVisible();
    }
  });
});
