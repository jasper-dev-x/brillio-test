import { defineConfig, devices } from '@playwright/test'

const API_PORT = Number(process.env.BACKEND_PORT ?? 8080)
const WEB_PORT = Number(process.env.FRONTEND_PORT ?? 5173)

/**
 * Full-stack end-to-end tests: real browser -> Vite dev server -> Spring Boot API -> sample data.
 * Playwright starts both servers (or reuses ones already running locally, e.g. from ./dev.sh).
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : [['list'], ['html', { open: 'never' }]],
  timeout: 30_000,
  use: {
    baseURL: `http://localhost:${WEB_PORT}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Optional: point at a preinstalled Chromium instead of `npx playwright install`.
        launchOptions: process.env.PW_CHROMIUM_PATH ? { executablePath: process.env.PW_CHROMIUM_PATH } : {},
      },
    },
  ],
  webServer: [
    {
      name: 'api',
      command: `./mvnw -q spring-boot:run -Dspring-boot.run.arguments=--server.port=${API_PORT}`,
      cwd: '../backend',
      url: `http://localhost:${API_PORT}/actuator/health`,
      reuseExistingServer: !process.env.CI,
      timeout: 180_000, // first run downloads Maven dependencies
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      name: 'web',
      command: `npx vite --port ${WEB_PORT} --strictPort`,
      url: `http://localhost:${WEB_PORT}`,
      env: { API_URL: `http://localhost:${API_PORT}` },
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
})
