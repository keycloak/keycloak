import { defineConfig } from "cypress";
import cypressSplit from "cypress-split";
import fs from "node:fs";
import { isAsyncFunction } from "node:util/types";

const isCI = process.env.CI === "true";
const retryCount = parseInt(process.env.RETRY_COUNT || "0");

export default defineConfig({
  video: isCI,
  projectId: "j4yhox",
  chromeWebSecurity: false,
  viewportWidth: 1920,
  viewportHeight: 1200,
  defaultCommandTimeout: 30000,
  numTestsKeptInMemory: 30,
  experimentalMemoryManagement: true,

  retries: {
    runMode: retryCount,
  },

  e2e: {
    baseUrl: "http://localhost:8080",
    slowTestThreshold: 30000,
    specPattern: "cypress/e2e/**/*.{js,jsx,ts,tsx}",
    setupNodeEvents(on, config) {
      on("before:browser:launch", (browser, launchOptions) => {
        if (browser.family === "firefox") {
          // launchOptions.preferences is a map of preference names to values
          // login is not working in firefox when testing_localhost_is_secure_when_hijacked is false
          launchOptions.preferences[
            "network.proxy.testing_localhost_is_secure_when_hijacked"
          ] = true;
        }

        return launchOptions;
      });
      // after:spec collides with cypressSplit function below and is overridden there

      function afterSpecRemoveSuccessfulVideos(spec, results) {
        if (results?.video) {
          // Do we have failures for any retry attempts?
          const failures = results.tests.some((test) =>
            test.attempts.some((attempt) => attempt.state === "failed"),
          );

          if (!failures) {
            // delete the video if the spec passed and no tests retried
            fs.rmSync(results.video, { force: true });
          }
        }
      }

      function chainedOn(event, callback) {
        if (event === "after:spec") {
          if (isAsyncFunction(callback)) {
            on(event, async (spec, results) => {
              afterSpecRemoveSuccessfulVideos(spec, results);
              await callback(spec, results);
            });
          } else {
            on(event, (spec, results) => {
              afterSpecRemoveSuccessfulVideos(spec, results);
              callback(spec, results);
            });
          }
        } else {
          on(event, callback);
        }
      }

      cypressSplit(chainedOn, config);

      return config;
    },
  },
});
