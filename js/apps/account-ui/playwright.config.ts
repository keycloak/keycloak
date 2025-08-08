import { type ViewportSize, defineConfig, devices } from "@playwright/test";

import { getAccountUrl } from "./test/utils";

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
  reporter: process.env.CI ? [["github"], ["html"]] : "list",

  use: {
    baseURL: getAccountUrl(),
    trace: "retain-on-failure",
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: "import realms",
      testMatch: /realm\.setup\.ts/,
      teardown: "del realms",
    },
    {
      name: "del realms",
      testMatch: /realm\.teardown\.ts/,
    },
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        viewport,
      },
      dependencies: ["import realms"],
    },
    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        viewport,
      },
      dependencies: ["import realms"],
    },
  ],
});
