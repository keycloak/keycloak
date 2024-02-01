import react from "@vitejs/plugin-react-swc";
import path from "path";
import { defineConfig, loadEnv } from "vite";
import { checker } from "vite-plugin-checker";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const external = ["react", "react/jsx-runtime", "react-dom"];
  if (env.LIB) external.push("react-router-dom");
  const lib = env.LIB
    ? {
        lib: {
          entry: path.resolve(__dirname, "src/index.ts"),
          formats: ["es"],
        },
      }
    : undefined;
  return {
    base: "",
    server: {
      port: 8080,
    },
    build: {
      ...lib,
      sourcemap: true,
      target: "esnext",
      modulePreload: false,
      cssMinify: "lightningcss",
      rollupOptions: {
        external: external,
      },
    },
    plugins: [react(), checker({ typescript: true })],
  };
});
