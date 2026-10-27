import { expect, test, type Page } from '@playwright/test';

async function playE4(page: Page) {
  await page.locator('[data-square="e2"]').tap();
  await page.locator('[data-square="e4"]').tap();
  await expect(page.getByTestId('move-1')).toContainText('e4');
}

/** Liegt das Element vollständig im sichtbaren Bereich und nicht unter der Aktionsleiste? */
async function fullyVisibleAboveBar(page: Page, testId: string) {
  const box = await page.getByTestId(testId).boundingBox();
  const bar = await page.getByTestId('bottom-bar').boundingBox();
  expect(box, testId).toBeTruthy();
  expect(bar).toBeTruthy();
  expect(box!.y, `${testId} oben sichtbar`).toBeGreaterThanOrEqual(0);
  expect(box!.y + box!.height, `${testId} nicht unter der Aktionsleiste`).toBeLessThanOrEqual(bar!.y + 1);
}

async function noHorizontalScroll(page: Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
  expect(overflow).toBeLessThanOrEqual(1);
}

test('mobile Darstellung: Brett und Analyse untereinander, ohne horizontales Scrollen', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('[data-square="e2"].movable')).toBeVisible();
  const board = await page.getByTestId('chess-board').boundingBox();
  const summary = await page.getByTestId('coaching-summary').boundingBox();
  expect(board && summary).toBeTruthy();
  expect(summary!.y).toBeGreaterThan(board!.y + board!.height - 1);
  await noHorizontalScroll(page);
  // Touch-Bedienung: Auswahl und Zielfeld antippen.
  await playE4(page);
});

test('Handy hochkant: nach dem Zug sind Brett, Bewertung und Coach ohne Scrollen sichtbar', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('[data-square="e2"].movable')).toBeVisible();
  await expect(page.getByTestId('bottom-bar')).toBeVisible();
  await playE4(page);
  await expect(page.getByTestId('coach-peek')).toContainText(/Bestzug|Sehr gut|Gut|Ungenau|Fehler|Erzwungen|Unsicher/, { timeout: 60_000 });

  await fullyVisibleAboveBar(page, 'chess-board');
  await fullyVisibleAboveBar(page, 'evaluation-bar');
  await fullyVisibleAboveBar(page, 'coach-peek');
  await noHorizontalScroll(page);

  // Antippen der Vorschau springt zum ausführlichen Coaching, der Knopf führt zurück zum Brett.
  await page.getByTestId('coach-peek').tap();
  await expect(page.getByTestId('coaching-summary')).toBeInViewport();
  await page.getByTestId('btn-to-board').tap();
  await expect(page.getByTestId('chess-board')).toBeInViewport({ ratio: 0.9 });
});

test('Handy hochkant: seltene Aktionen im Menü „Mehr“', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('[data-square="e2"].movable')).toBeVisible();
  await page.getByTestId('btn-more').tap();
  await page.getByTestId('btn-settings').tap();
  await expect(page.getByTestId('settings-dialog')).toBeVisible();
});

test('Handy: Wischen über leere Felder scrollt die Seite, ziehbare Figuren bleiben ziehbar', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('[data-square="e2"].movable')).toBeVisible();
  const touchAction = (selector: string) => page.locator(selector).evaluate((el) => getComputedStyle(el).touchAction);
  // touch-action wird nicht vererbt: Das Brett erlaubt Scrollen, nur ziehbare Figuren fangen die Wischgeste ab.
  expect(await touchAction('[data-testid="chess-board"]')).toBe('manipulation');
  expect(await touchAction('[data-square="e4"]')).not.toBe('none');
  expect(await touchAction('[data-square="e2"]')).toBe('none');
});

test.describe('kleines Handy (iPhone SE)', () => {
  test.use({ viewport: { width: 320, height: 568 } });

  test('Brett, Bewertung und Coach-Vorschau passen auf einen Bildschirm', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('[data-square="e2"].movable')).toBeVisible();
    await playE4(page);
    await expect(page.getByTestId('coach-peek')).toBeVisible({ timeout: 60_000 });
    await fullyVisibleAboveBar(page, 'chess-board');
    await fullyVisibleAboveBar(page, 'evaluation-bar');
    await fullyVisibleAboveBar(page, 'coach-peek');
    await noHorizontalScroll(page);
  });
});

test.describe('Handy quer', () => {
  test.use({ viewport: { width: 915, height: 412 } });

  test('Brett und Coaching nebeneinander, ohne Kopfzeile', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('[data-square="e2"].movable')).toBeVisible();
    await expect(page.locator('header')).toHaveCount(0);
    await playE4(page);
    const board = await page.getByTestId('chess-board').boundingBox();
    const summary = await page.getByTestId('coaching-summary').boundingBox();
    expect(summary!.x).toBeGreaterThan(board!.x + board!.width - 1);
    await fullyVisibleAboveBar(page, 'chess-board');
    await fullyVisibleAboveBar(page, 'evaluation-bar');
    await noHorizontalScroll(page);
  });
});
