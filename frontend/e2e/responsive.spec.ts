import { expect, test } from '@playwright/test';

test('mobile Darstellung: Brett und Analyse untereinander, ohne horizontales Scrollen', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('[data-square="e2"].movable')).toBeVisible();
  const board = await page.getByTestId('chess-board').boundingBox();
  const summary = await page.getByTestId('coaching-summary').boundingBox();
  expect(board && summary).toBeTruthy();
  expect(summary!.y).toBeGreaterThan(board!.y + board!.height - 1);
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  // Touch-Bedienung: Auswahl und Zielfeld antippen.
  await page.locator('[data-square="e2"]').tap();
  await page.locator('[data-square="e4"]').tap();
  await expect(page.getByTestId('move-1')).toContainText('e4');
});
