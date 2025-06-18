import { defineConfig, devices } from "@playwright/test";

const retryCount = parseInt(process.env.RETRY_COUNT || "0");

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./test",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: retryCount,
  workers: 1,
  timeout: 60 * 1000,
  reporter: process.env.CI ? [["github"], ["html"]] : "list",

  use: {
    baseURL: "http://localhost:8080",
    trace: "on-first-retry",
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        viewport: { width: 1920, height: 1200 },
      },
    },
    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        viewport: { width: 1920, height: 1200 },
      },
    },
  ],
});
