import { defineConfig, devices } from "@playwright/test";
import { getRootPath } from "./src/utils/getRootPath";

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
  use: {
    baseURL: `http://localhost:8080${getRootPath()}`,
    trace: "on-first-retry",
    viewport: { width: 1920, height: 1080 },
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
      },
      dependencies: ["import realms"],
    },
  ],
});
