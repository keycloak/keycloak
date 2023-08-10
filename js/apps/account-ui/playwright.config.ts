import { defineConfig, devices } from "@playwright/test";

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./test",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: "html",
  use: {
    baseURL: "http://localhost:8080/",
    trace: "on-first-retry",
  },

  /* Configure projects for major browsers */
  projects: [
    { name: "setup", testMatch: /.auth\.setup\.ts/ },
    {
      name: "import test realm",
      testMatch: /test-realm\.setup\.ts/,
      teardown: "del test realm",
    },
    {
      name: "del test realm",
      testMatch: /test-realm\.teardown\.ts/,
    },
    {
      name: "import groups realm",
      testMatch: /groups-realm\.setup\.ts/,
      teardown: "del groups realm",
    },
    {
      name: "del groups realm",
      testMatch: /groups-realm\.teardown\.ts/,
    },
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        storageState: ".auth/user.json",
      },
      dependencies: ["setup"],
      testIgnore: ["**/*my-resources.spec.ts"],
    },

    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        storageState: ".auth/user.json",
      },
      dependencies: ["setup"],
      testIgnore: ["**/*my-resources.spec.ts"],
    },

    {
      name: "resources",
      use: { ...devices["Desktop Chrome"] },
      dependencies: ["import test realm"],
      testMatch: ["**/*my-resources.spec.ts"],
    },
    {
      name: "groups",
      use: { ...devices["Desktop Chrome"] },
      dependencies: ["import groups realm"],
      testMatch: ["**/*groups.spec.ts"],
    },
  ],
});
