import react from "@vitejs/plugin-react";
import path from "node:path";
import peerDepsExternal from "rollup-plugin-peer-deps-external";
import { defineConfig } from "vite";
import { checker } from "vite-plugin-checker";
import dts from "vite-plugin-dts";

// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    mainFields: ["module"],
  },
  build: {
    target: "ES2022",
    lib: {
      entry: path.resolve(__dirname, "src/main.ts"),
      formats: ["es"],
    },
    rollupOptions: {
      plugins: [
        peerDepsExternal({
          includeDependencies: true,
        }),
      ],
    },
  },
  plugins: [
    react(),
    checker({ typescript: true }),
    dts({ insertTypesEntry: true }),
  ],
});
