import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import checker from "vite-plugin-checker";

// https://vitejs.dev/config/
export default defineConfig({
  base: "",
  server: {
    port: 8080,
  },
  resolve: {
    // Resolve the 'module' entrypoint at all times (not the default due to Node.js compatibility issues).
    mainFields: ["module"],
    dedupe: ["react", "react-dom"],
  },
  optimizeDeps: {
    // Enable optimization of dependencies using esbuild (see https://vitejs.dev/guide/migration.html#using-esbuild-deps-optimization-at-build-time).
    disabled: false,
  },
  build: {
    commonjsOptions: {
      // Ensure `@rollup/plugin-commonjs` is not loaded, using esbuild instead (see https://vitejs.dev/guide/migration.html#using-esbuild-deps-optimization-at-build-time).
      include: [],
    },
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
