import react from "@vitejs/plugin-react-swc";
import path from "node:path";
import peerDepsExternal from "rollup-plugin-peer-deps-external";
import { defineConfig } from "vite";
import { checker } from "vite-plugin-checker";
import dts from "vite-plugin-dts";
import { libInjectCss } from "vite-plugin-lib-inject-css";

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    target: "esnext",
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
    libInjectCss(),
    checker({ typescript: true }),
    dts({ insertTypesEntry: true }),
  ],
});
