import webpackPreprocessor from "@cypress/webpack-batteries-included-preprocessor";
import { defineConfig } from "cypress";
import { deleteAsync } from "del";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);

export default defineConfig({
  projectId: "j4yhox",
  screenshotsFolder: "assets/screenshots",
  videosFolder: "assets/videos",
  chromeWebSecurity: false,
  viewportWidth: 1360,
  viewportHeight: 768,
  defaultCommandTimeout: 30000,
  videoCompression: false,
  numTestsKeptInMemory: 30,
  videoUploadOnPasses: false,

  e2e: {
    setupNodeEvents(on) {
      on(
        "file:preprocessor",
        webpackPreprocessor({
          typescript: require.resolve("typescript"),
        })
      );

      on("after:spec", async (spec, results) => {
        if (!results.video) {
          return;
        }

        // Do we have failures for any retry attempts?
        const failures = results.tests.some(({ attempts }) =>
          attempts.some(({ state }) => state === "failed")
        );

        // Delete the video if the spec passed and no tests were retried.
        if (!failures) {
          await deleteAsync(results.video);
        }
      });
    },
    baseUrl: "http://localhost:8080",
    slowTestThreshold: 30000,
    specPattern: "cypress/e2e/**/*.{js,jsx,ts,tsx}",
  },

  component: {
    devServer: {
      framework: "react",
      bundler: "vite",
    },
  },
});
