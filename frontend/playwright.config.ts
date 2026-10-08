import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests against a running TaskFlow (dev servers or the Docker stack):
 *   E2E_BASE_URL (default http://localhost:5173), E2E_IT_EMAIL and E2E_IT_PASSWORD (an IT Manager account).
 * Every run creates its own users and projects, so nothing needs resetting.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 90_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
