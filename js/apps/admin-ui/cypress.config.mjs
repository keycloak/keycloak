import { defineConfig } from "cypress";

export default defineConfig({
  projectId: "j4yhox",
  chromeWebSecurity: false,
  viewportWidth: 1360,
  viewportHeight: 768,
  defaultCommandTimeout: 30000,
  experimentalMemoryManagement: true,
  e2e: {
    baseUrl: "http://localhost:8080",
    slowTestThreshold: 30000,
    specPattern: "cypress/e2e/**/*.{js,jsx,ts,tsx}",
  },
});
