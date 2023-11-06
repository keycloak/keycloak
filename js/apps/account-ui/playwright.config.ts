import { defineConfig, devices } from "@playwright/test";

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
    baseURL: process.env.CI
      ? "http://localhost:8080/realms/master/account/"
      : "http://localhost:8080/",
    trace: "on-first-retry",
  },

  /* Configure projects for major browsers */
  projects: [
    { name: "setup", testMatch: /.auth\.setup\.ts/ },
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
        storageState: ".auth/user.json",
      },
      dependencies: ["setup", "import realms"],
      testIgnore: ["**/personal-info.spec.ts"],
    },
    {
      name: "personal-info",
      use: {
        ...devices["Desktop Chrome"],
      },
      dependencies: ["import realms"],
      testMatch: ["**/personal-info.spec.ts"],
    },
  ],
});
