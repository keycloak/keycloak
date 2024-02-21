import { defineConfig } from "cypress";
import cypressSplit from "cypress-split";

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
  experimentalMemoryManagement: true,

  retries: {
    runMode: 3,
  },

  e2e: {
    baseUrl: "http://localhost:8080",
    slowTestThreshold: 30000,
    specPattern: "cypress/e2e/**/*.{js,jsx,ts,tsx}",
    setupNodeEvents(on, config) {
      cypressSplit(on, config);
      return config;
    },
  },
});
