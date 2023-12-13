import { defineConfig } from "vite";
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
    target: "esnext",
    modulePreload: false,
    cssMinify: "lightningcss",
    rollupOptions: {
      external: ["react", "react/jsx-runtime", "react-dom"],
    },
  },
  plugins: [react(), checker({ typescript: true })],
});
