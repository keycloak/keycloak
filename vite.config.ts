import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    // Resolve the 'module' entrypoint at all times (not the default due to Node.js compatibility issues).
    mainFields: ["module"],
  },
  plugins: [react({ jsxRuntime: "classic" })],
  test: {
    setupFiles: "vitest.setup.ts",
    watch: false,
    deps: {
      // Ensure '.mjs' files are used for '@patternfly/react-styles'.
      inline: [/@patternfly\/react-styles/],
    },
  },
});
