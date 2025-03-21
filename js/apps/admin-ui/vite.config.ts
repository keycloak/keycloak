import react from "@vitejs/plugin-react-swc";
import path from "path";
import { defineConfig, loadEnv } from "vite";
import { checker } from "vite-plugin-checker";
import dts from "vite-plugin-dts";
import { configDefaults } from "vitest/config";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const external = ["react", "react/jsx-runtime", "react-dom"];
  const plugins = [react(), checker({ typescript: true })];
  const input = env.LIB ? undefined : "src/main.tsx";
  if (env.LIB) {
    external.push("react-router-dom");
    external.push("react-i18next");
    plugins.push(dts({ insertTypesEntry: true }));
  }
  const lib = env.LIB
    ? {
        outDir: "lib",
        lib: {
          entry: path.resolve(__dirname, "src/index.ts"),
          formats: ["es"],
        },
      }
    : {
        outDir: "target/classes/theme/keycloak.v2/admin/resources",
        external: ["src/index.ts"],
      };
  return {
    base: "",
    server: {
      origin: "http://localhost:5174",
      port: 5174,
    },
    build: {
      ...lib,
      sourcemap: true,
      target: "esnext",
      modulePreload: false,
      cssMinify: "lightningcss",
      manifest: true,
      rollupOptions: {
        input,
        external,
      },
    },
    plugins,
    test: {
      watch: false,
      environment: "jsdom",
      exclude: [...configDefaults.exclude, "./test/**"],
      server: {
        deps: {
          inline: [/@patternfly\/.*/],
        },
      },
    },
  };
});
