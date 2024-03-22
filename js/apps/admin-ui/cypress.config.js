import { defineConfig } from "cypress";
import cypressSplit from "cypress-split";
import fs from "node:fs";

const isCI = process.env.CI === "true";

export default defineConfig({
  video: isCI,
  projectId: "j4yhox",
  chromeWebSecurity: false,
  viewportWidth: 1360,
  viewportHeight: 768,
  defaultCommandTimeout: 30000,
  numTestsKeptInMemory: 30,
  experimentalMemoryManagement: true,
  e2e: {
    baseUrl: "http://localhost:8080",
    slowTestThreshold: 30000,
    specPattern: "cypress/e2e/**/*.{js,jsx,ts,tsx}",
    setupNodeEvents(on, config) {
      on("after:spec", (spec, results) => {
        if (results.video) {
          // Do we have failures for any retry attempts?
          const failures = results.tests.some((test) =>
            test.attempts.some((attempt) => attempt.state === "failed"),
          );

          if (!failures) {
            // delete the video if the spec passed and no tests retried
            fs.unlinkSync(results.video);
          }
        }
      });

      cypressSplit(on, config);

      return config;
    },
  },
});
