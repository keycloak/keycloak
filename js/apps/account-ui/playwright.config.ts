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
      name: "import realm",
      testMatch: /import\.setup\.ts/,
      teardown: "del realm",
    },
    {
      name: "del realm",
      testMatch: /import\.teardown\.ts/,
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
      name: "photoz realm chromium",
      use: { ...devices["Desktop Chrome"] },
      dependencies: ["import realm"],
      testMatch: ["**/*my-resources.spec.ts"],
    },
  ],
});
