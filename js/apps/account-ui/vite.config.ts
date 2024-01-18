import react from "@vitejs/plugin-react-swc";
import path from "path";
import { defineConfig } from "vite";
import { checker } from "vite-plugin-checker";

// https://vitejs.dev/config/
export default defineConfig({
  base: "",
  server: {
    port: 8080,
  },
  build: {
    lib: {
      entry: path.resolve(__dirname, "src/index.ts"),
      formats: ["es"],
    },
    sourcemap: true,
    target: "esnext",
    modulePreload: false,
    cssMinify: "lightningcss",
    rollupOptions: {
      external: ["react", "react/jsx-runtime", "react-dom", "react-router-dom"],
    },
  },
  plugins: [react(), checker({ typescript: true })],
});
