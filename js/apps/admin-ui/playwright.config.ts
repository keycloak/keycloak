import { type ViewportSize, defineConfig, devices } from "@playwright/test";

const retryCount = parseInt(process.env.RETRY_COUNT || "0");
console.log("----------------------------");
console.log("Playwright retries = " + retryCount);
console.log("----------------------------");

const viewport: ViewportSize = { width: 1920, height: 1080 };

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./test",
  forbidOnly: !!process.env.CI,
  retries: retryCount,
  workers: process.env.CI ? 1 : undefined,
  timeout: 60_000,
  reporter: process.env.CI ? [["github"], ["html"]] : "list",

  use: {
    baseURL: "http://localhost:8080",
    trace: "retain-on-failure",
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        viewport,
      },
    },
    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        viewport,
      },
    },
  ],
});
