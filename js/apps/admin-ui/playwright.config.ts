import { type ViewportSize, defineConfig, devices } from "@playwright/test";
import { sharedConfig } from "../../playwright.shared";

const viewport: ViewportSize = { width: 1920, height: 1080 };

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./test",
  // Tests within a file run in order, as many files share state in 'beforeAll'.
  fullyParallel: false,
  ...sharedConfig,

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
