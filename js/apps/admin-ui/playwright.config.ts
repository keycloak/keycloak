import { type ViewportSize, defineConfig, devices } from "@playwright/test";

const viewport: ViewportSize = { width: 1920, height: 1080 };

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./test",
  fullyParallel: true,
  // The admin console tests are not optimized for parallel execution, long-term
  // this should be addressed and 'workers' should be returned to the default value.
  workers: 1,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI ? [["github"], ["html"]] : "list",

  use: {
    trace: "retain-on-failure",
  },

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
