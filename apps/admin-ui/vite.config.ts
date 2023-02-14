import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react-swc";
import { checker } from "vite-plugin-checker";

// https://vitejs.dev/config/
export default defineConfig({
  base: "",
  server: {
    port: 8080,
  },
  build: {
    sourcemap: true,
    target: "ES2022",
    // Code splitting results in broken CSS for production builds.
    // This is due to an unknown bug, presumably in Rollup.
    // TODO: Revisit if we can re-enable this in the future.
    cssCodeSplit: false,
  },
  resolve: {
    // Resolve the 'module' entrypoint at all times (not the default due to Node.js compatibility issues).
    mainFields: ["module"],
  },
  plugins: [react(), checker({ typescript: true })],
  test: {
    setupFiles: "vitest.setup.ts",
    watch: false,
    deps: {
      // Ensure '.mjs' files are used for '@patternfly/react-styles'.
      inline: [/@patternfly\/react-styles/],
    },
  },
});
