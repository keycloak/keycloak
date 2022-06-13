import webpackPreprocessor from "@cypress/webpack-batteries-included-preprocessor";
import { defineConfig } from "cypress";
import del from "del";
import ForkTsCheckerWebpackPlugin from "fork-ts-checker-webpack-plugin";
import path from "node:path";

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
  retries: {
    runMode: 3,
  },
  e2e: {
    setupNodeEvents(on) {
      // Use a Webpack based TypeScript pre-processor to ensure types are correct.
      on(
        "file:preprocessor",
        webpackPreprocessor({
          typescript: require.resolve("typescript"),
          webpackOptions: {
            ...webpackPreprocessor.defaultOptions.webpackOptions,
            context: path.resolve(__dirname, "cypress"),
            plugins: [
              new ForkTsCheckerWebpackPlugin({
                async: false,
              }),
            ],
          },
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
          await del(results.video);
        }
      });
    },
    baseUrl: "http://localhost:8080",
    slowTestThreshold: 30000,
    specPattern: "cypress/e2e/**/*.{js,jsx,ts,tsx}",
  },
});
