import { expect, test, type Page } from '@playwright/test';

async function freshStart(page: Page) {
  await page.goto('/');
  await page.evaluate(async () => {
    localStorage.clear();
    await new Promise<void>((resolve) => {
      const req = indexedDB.deleteDatabase('chess-coach');
      req.onsuccess = req.onerror = req.onblocked = () => resolve();
    });
  });
  await page.reload();
  await expect(page.locator('[data-square="e2"].movable')).toBeVisible();
}

async function playUserMove(page: Page, from: string, to: string) {
  await page.locator(`[data-square="${from}"]`).click();
  await page.locator(`[data-square="${to}"]`).click();
}

test('Partie spielen: Zug, Coaching, Variante, Computerzug, Undo und Wiederherstellung', async ({ page }) => {
  // 1. Anwendung öffnen und neue Partie starten.
  await freshStart(page);
  await expect(page.getByTestId('game-status')).toContainText('Weiß ist am Zug');
  await expect(page.locator('img[alt="weißer König"]')).toHaveCount(1);

  // 2. Einen legalen Zug spielen (Auswahl zeigt legale Zielfelder).
  await page.locator('[data-square="e2"]').click();
  await expect(page.locator('[data-square="e4"] .target')).toBeVisible();
  await page.locator('[data-square="e4"]').click();
  await expect(page.getByTestId('move-1')).toContainText('e4');

  // 3. Auf das Coaching-Ergebnis warten.
  await expect(page.getByTestId('classification')).toBeVisible({ timeout: 60_000 });
  await expect(page.getByTestId('main-reason')).not.toBeEmpty();

  // 5. Der Computer antwortet automatisch mit einem legalen Zug.
  await expect(page.getByTestId('move-2')).toBeVisible({ timeout: 60_000 });
  await expect(page.locator('[data-square="d2"].movable')).toBeVisible();

  // 4. Erklärung öffnen und eine Kandidatenvariante abspielen.
  await page.getByTestId('btn-details').click();
  await expect(page.getByTestId('coaching-details')).toContainText('Was war gut, was war schlecht?');
  const boardBefore = await page.getByTestId('chess-board').innerHTML();
  await page.getByTestId('tab-candidates').click();
  await page.locator('[data-testid^="candidate-"]').first().click();
  await expect(page.getByTestId('variation-viewer')).toContainText('nicht Teil der Partie');
  await page.getByTestId('variation-forward').click();
  await page.getByTestId('variation-back').click();
  await page.getByTestId('variation-exit').click();
  await expect(page.getByTestId('variation-viewer')).toHaveCount(0);
  expect(await page.getByTestId('chess-board').innerHTML()).toBe(boardBefore);

  // 6. Den letzten Zug zurücknehmen.
  await page.getByTestId('btn-undo').click();
  await expect(page.getByTestId('move-1')).toHaveCount(0);
  await expect(page.locator('[data-square="e2"] img')).toHaveAttribute('alt', 'weißer Bauer');

  // Erneut spielen, dann 7. Seite neu laden und Partie wiederherstellen.
  await playUserMove(page, 'd2', 'd4');
  await expect(page.getByTestId('move-2')).toBeVisible({ timeout: 90_000 });
  const fenSquares = await page.locator('[data-square] img').evaluateAll((imgs) => imgs.map((i) => i.getAttribute('alt')).join(','));
  await page.reload();
  await expect(page.getByTestId('move-2')).toBeVisible();
  await expect(page.getByTestId('move-1')).toContainText('d4');
  const restored = await page.locator('[data-square] img').evaluateAll((imgs) => imgs.map((i) => i.getAttribute('alt')).join(','));
  expect(restored).toBe(fenSquares);
});

test('Partie bis zum Matt spielen und Ergebnis prüfen', async ({ page }) => {
  await freshStart(page);
  // Gespeicherte Partie vor dem Schäfermatt (wird über die Zughistorie vom Backend rekonstruiert).
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const req = indexedDB.open('chess-coach', 1);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction('games', 'readwrite');
      tx.objectStore('games').put({
        id: sessionStorage.getItem('chess-coach-window-slot'), gameId: 'e2e', initialFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
        moves: ['e2e4', 'e7e5', 'f1c4', 'b8c6', 'd1h5', 'g8f6'], sanMoves: [],
        fen: 'r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4', playerColor: 'WHITE',
        difficulty: 'BEGINNER', analysisProfile: 'FAST', status: 'IN_PROGRESS', result: '*', savedAt: new Date().toISOString(), finished: false,
      });
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
    db.close();
  });
  await page.reload();
  await expect(page.getByTestId('move-6')).toBeVisible();
  await playUserMove(page, 'h5', 'f7');
  await expect(page.getByTestId('game-status')).toContainText('Schachmatt – Weiß gewinnt');
  await expect(page.getByTestId('game-result')).toContainText('1-0');
  await expect(page.locator('[data-square].movable')).toHaveCount(0);
  // Die Analyse bleibt nach Partieende verfügbar.
  await expect(page.getByTestId('classification')).toBeVisible({ timeout: 60_000 });
});

test('Bauernumwandlung über die Auswahl', async ({ page }) => {
  await freshStart(page);
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((resolve) => {
      const req = indexedDB.open('chess-coach', 1);
      req.onsuccess = () => resolve(req.result);
    });
    await new Promise<void>((resolve) => {
      const tx = db.transaction('games', 'readwrite');
      tx.objectStore('games').put({
        id: sessionStorage.getItem('chess-coach-window-slot'), gameId: 'e2e', initialFen: '8/4P3/8/8/8/k7/8/4K3 w - - 0 1', moves: [], sanMoves: [],
        fen: '8/4P3/8/8/8/k7/8/4K3 w - - 0 1', playerColor: 'WHITE', difficulty: 'BEGINNER', analysisProfile: 'FAST',
        status: 'IN_PROGRESS', result: '*', savedAt: new Date().toISOString(), finished: false,
      });
      tx.oncomplete = () => resolve();
    });
    db.close();
  });
  await page.reload();
  await expect(page.locator('[data-square="e7"].movable')).toBeVisible();
  await playUserMove(page, 'e7', 'e8');
  await expect(page.getByTestId('promotion-dialog')).toBeVisible();
  await page.getByTestId('promote-n').click();
  await expect(page.getByTestId('move-1')).toContainText('e8=N');
  await expect(page.locator('[data-square="e8"] img')).toHaveAttribute('alt', 'weißer Springer');
});

test('Zwei Fenster desselben Browsers spielen unabhängige Partien', async ({ page, context }) => {
  await freshStart(page);
  const second = await context.newPage();
  await second.goto('/');
  await expect(second.locator('[data-square="e2"].movable')).toBeVisible();

  await playUserMove(page, 'e2', 'e4');
  await expect(page.getByTestId('move-1')).toContainText('e4');
  await playUserMove(second, 'd2', 'd4');
  await expect(second.getByTestId('move-1')).toContainText('d4');

  // Nach dem Neuladen behält jedes Fenster seine eigene Partie.
  await page.reload();
  await second.reload();
  await expect(page.getByTestId('move-1')).toContainText('e4');
  await expect(second.getByTestId('move-1')).toContainText('d4');
});

test('Zwei Personen spielen gleichzeitig in getrennten Browsern', async ({ page, browser }) => {
  await freshStart(page);
  const otherPerson = await browser.newContext({ locale: 'de-DE' });
  const other = await otherPerson.newPage();
  await other.goto('/');
  await expect(other.locator('[data-square="e2"].movable')).toBeVisible();

  // Beide ziehen praktisch gleichzeitig; Coaching und Computerzüge laufen parallel.
  await Promise.all([playUserMove(page, 'e2', 'e4'), playUserMove(other, 'c2', 'c4')]);
  await Promise.all([
    expect(page.getByTestId('move-2')).toBeVisible({ timeout: 90_000 }),
    expect(other.getByTestId('move-2')).toBeVisible({ timeout: 90_000 }),
  ]);
  await expect(page.getByTestId('move-1')).toContainText('e4');
  await expect(other.getByTestId('move-1')).toContainText('c4');

  // Eine Person nimmt zurück – die andere Partie und ihr Coaching bleiben unberührt.
  await page.getByTestId('btn-undo').click();
  await expect(page.getByTestId('move-1')).toHaveCount(0);
  await expect(other.getByTestId('move-1')).toContainText('c4');
  await expect(other.getByTestId('move-2')).toBeVisible();
  await expect(other.getByTestId('classification')).toBeVisible({ timeout: 60_000 });

  await otherPerson.close();
});
