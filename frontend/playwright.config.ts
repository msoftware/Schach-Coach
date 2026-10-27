import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-End-Tests gegen das echte Backend mit der mitgelieferten Engine.
 * Voraussetzung: Backend-JAR gebaut (cd backend && ./gradlew bootJar).
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 120_000,
  expect: { timeout: 30_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    locale: 'de-DE',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] }, testIgnore: /responsive/ },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] }, testIgnore: /responsive/ },
    { name: 'mobile', use: { ...devices['Pixel 7'] }, testMatch: /responsive/ },
    // iPhone (WebKit/Safari): Touch verhält sich dort anders als in Chromium, z. B. beim Antippen von Figuren.
    { name: 'iphone', use: { ...devices['iPhone 13'] }, testMatch: /responsive/ },
  ],
  webServer: [
    {
      command: 'java -jar ../backend/build/libs/chess-coach-backend.jar --chesscoach.engine.calvin.jar-path=../backend/engine/calvin-chess-engine-6.1.1.jar',
      url: 'http://localhost:8080/api/health',
      reuseExistingServer: true,
      timeout: 120_000,
    },
    {
      command: 'npx vite --port 5173 --strictPort',
      url: 'http://localhost:5173',
      reuseExistingServer: true,
      timeout: 60_000,
    },
  ],
});
