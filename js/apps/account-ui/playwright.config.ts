import { type ViewportSize, defineConfig, devices } from "@playwright/test";

const viewport: ViewportSize = { width: 1920, height: 1080 };

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./test",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI
    ? [
        ["github"],
        ["list"],
        ["html"],
        ["json", { outputFile: "playwright-results.json" }],
      ]
    : "list",

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
