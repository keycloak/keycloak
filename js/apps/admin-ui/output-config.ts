import { defineConfig } from "@playwright/test";

// used by GHA to generate the report
export default defineConfig({
  reporter: [["html", { outputFile: "playwright-report", open: "never" }]],
});
