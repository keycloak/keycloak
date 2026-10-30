import { type PlaywrightTestConfig } from "@playwright/test";

/**
 * Settings shared by all Playwright configurations in this workspace.
 */
export const sharedConfig = {
  forbidOnly: !!process.env.CI,

  // In CI, retry a failed test once and fail the build if it passes on retry, so flaky tests are not hidden.
  retries: process.env.CI ? 1 : 0,
  failOnFlakyTests: !!process.env.CI,

  reporter: process.env.CI
    ? [
        ["github"],
        ["list"],
        ["html"],
        ["json", { outputFile: "playwright-results.json" }],
      ]
    : "list",

  use: {
    // CI: record a trace only on the retry to avoid the recording overhead on every test.
    // The first failed attempt has no trace, only the screenshot below.
    // Local: record every test, but keep the trace only on failure (no retries, so 'on-first-retry' would never record).
    // Open traces from the HTML report or with `pnpm exec playwright show-trace <trace.zip>`.
    trace: process.env.CI ? "on-first-retry" : "retain-on-failure",
    // Screenshot of the page state at the end of each failed attempt, including the first one.
    screenshot: "only-on-failure",
  },
} satisfies PlaywrightTestConfig;
