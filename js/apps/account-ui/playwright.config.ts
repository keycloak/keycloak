import { defineConfig, devices } from "@playwright/test";

import { getAccountUrl } from "./test/utils";

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./test",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: process.env.CI ? [["github"], ["html"]] : "list",
  expect: {
    timeout: 20 * 1000,
  },

  use: {
    baseURL: getAccountUrl(),
    trace: "on-first-retry",
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
        viewport: { width: 1920, height: 1200 },
      },
      dependencies: ["import realms"],
    },
  ],
});
