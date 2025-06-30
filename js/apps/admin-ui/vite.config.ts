import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react-swc";
import { checker } from "vite-plugin-checker";

// https://vitejs.dev/config/
export default defineConfig({
  base: "",
  server: {
    origin: "http://localhost:5174",
    port: 5174,
  },
  build: {
    sourcemap: true,
    target: "esnext",
    modulePreload: false,
    cssMinify: "lightningcss",
    manifest: true,
    rollupOptions: {
      input: "src/main.tsx",
      external: ["react", "react/jsx-runtime", "react-dom"],
    },
  },
  plugins: [react(), checker({ typescript: true })],
  test: {
    watch: false,
    environment: "jsdom",
    server: {
      deps: {
        inline: [/@patternfly\/.*/],
      },
    },
  },
});
