// vite.config.ts
import react from "file:///home/edewit/workspace/keycloak/keycloak/node_modules/.pnpm/@vitejs+plugin-react-swc@3.7.0_vite@5.2.11_@types+node@20.12.12_lightningcss@1.25.1_terser@5.31.0_/node_modules/@vitejs/plugin-react-swc/index.mjs";
import path from "path";
import {
  defineConfig,
  loadEnv,
} from "file:///home/edewit/workspace/keycloak/keycloak/node_modules/.pnpm/vite@5.2.11_@types+node@20.12.12_lightningcss@1.25.1_terser@5.31.0/node_modules/vite/dist/node/index.js";
import { checker } from "file:///home/edewit/workspace/keycloak/keycloak/node_modules/.pnpm/vite-plugin-checker@0.6.4_eslint@8.57.0_optionator@0.9.4_typescript@5.4.5_vite@5.2.11_@types+_kd72m4cjuawx7a6x2mjml4g3zm/node_modules/vite-plugin-checker/dist/esm/main.js";
import dts from "file:///home/edewit/workspace/keycloak/keycloak/node_modules/.pnpm/vite-plugin-dts@3.9.1_@types+node@20.12.12_rollup@4.18.0_typescript@5.4.5_vite@5.2.11_@types+_o675aa6yhw7sef6hwvuvop4g7m/node_modules/vite-plugin-dts/dist/index.mjs";

// src/utils/getRootPath.ts
import { generatePath } from "file:///home/edewit/workspace/keycloak/keycloak/node_modules/.pnpm/react-router-dom@6.23.1_react-dom@18.3.1_react@18.3.1__react@18.3.1/node_modules/react-router-dom/dist/main.js";

// src/constants.ts
var DEFAULT_REALM = "master";
var ROOT_PATH = "/realms/:realm/account";

// src/utils/getRootPath.ts
var getRootPath = (realm = DEFAULT_REALM) => generatePath(ROOT_PATH, { realm });

// vite.config.ts
var __vite_injected_original_dirname =
  "/home/edewit/workspace/keycloak/keycloak/js/apps/account-ui";
var vite_config_default = defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const external = ["react", "react/jsx-runtime", "react-dom"];
  const plugins = [react(), checker({ typescript: true })];
  if (env.LIB) {
    external.push("react-router-dom");
    external.push("react-i18next");
    plugins.push(dts({ insertTypesEntry: true }));
  }
  const lib = env.LIB
    ? {
        outDir: "lib",
        lib: {
          entry: path.resolve(__vite_injected_original_dirname, "src/index.ts"),
          formats: ["es"],
        },
      }
    : void 0;
  return {
    base: "",
    server: {
      port: 8080,
      open: getRootPath(),
    },
    build: {
      ...lib,
      sourcemap: true,
      target: "esnext",
      modulePreload: false,
      cssMinify: "lightningcss",
      rollupOptions: {
        external,
      },
    },
    plugins,
  };
});
export { vite_config_default as default };
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiLCAic3JjL3V0aWxzL2dldFJvb3RQYXRoLnRzIiwgInNyYy9jb25zdGFudHMudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvaG9tZS9lZGV3aXQvd29ya3NwYWNlL2tleWNsb2FrL2tleWNsb2FrL2pzL2FwcHMvYWNjb3VudC11aVwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL2hvbWUvZWRld2l0L3dvcmtzcGFjZS9rZXljbG9hay9rZXljbG9hay9qcy9hcHBzL2FjY291bnQtdWkvdml0ZS5jb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL2hvbWUvZWRld2l0L3dvcmtzcGFjZS9rZXljbG9hay9rZXljbG9hay9qcy9hcHBzL2FjY291bnQtdWkvdml0ZS5jb25maWcudHNcIjtpbXBvcnQgcmVhY3QgZnJvbSBcIkB2aXRlanMvcGx1Z2luLXJlYWN0LXN3Y1wiO1xuaW1wb3J0IHBhdGggZnJvbSBcInBhdGhcIjtcbmltcG9ydCB7IGRlZmluZUNvbmZpZywgbG9hZEVudiB9IGZyb20gXCJ2aXRlXCI7XG5pbXBvcnQgeyBjaGVja2VyIH0gZnJvbSBcInZpdGUtcGx1Z2luLWNoZWNrZXJcIjtcbmltcG9ydCBkdHMgZnJvbSBcInZpdGUtcGx1Z2luLWR0c1wiO1xuXG5pbXBvcnQgeyBnZXRSb290UGF0aCB9IGZyb20gXCIuL3NyYy91dGlscy9nZXRSb290UGF0aFwiO1xuXG4vLyBodHRwczovL3ZpdGVqcy5kZXYvY29uZmlnL1xuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKCh7IG1vZGUgfSkgPT4ge1xuICBjb25zdCBlbnYgPSBsb2FkRW52KG1vZGUsIHByb2Nlc3MuY3dkKCksIFwiXCIpO1xuICBjb25zdCBleHRlcm5hbCA9IFtcInJlYWN0XCIsIFwicmVhY3QvanN4LXJ1bnRpbWVcIiwgXCJyZWFjdC1kb21cIl07XG4gIGNvbnN0IHBsdWdpbnMgPSBbcmVhY3QoKSwgY2hlY2tlcih7IHR5cGVzY3JpcHQ6IHRydWUgfSldO1xuICBpZiAoZW52LkxJQikge1xuICAgIGV4dGVybmFsLnB1c2goXCJyZWFjdC1yb3V0ZXItZG9tXCIpO1xuICAgIGV4dGVybmFsLnB1c2goXCJyZWFjdC1pMThuZXh0XCIpO1xuICAgIHBsdWdpbnMucHVzaChkdHMoeyBpbnNlcnRUeXBlc0VudHJ5OiB0cnVlIH0pKTtcbiAgfVxuICBjb25zdCBsaWIgPSBlbnYuTElCXG4gICAgPyB7XG4gICAgICAgIG91dERpcjogXCJsaWJcIixcbiAgICAgICAgbGliOiB7XG4gICAgICAgICAgZW50cnk6IHBhdGgucmVzb2x2ZShfX2Rpcm5hbWUsIFwic3JjL2luZGV4LnRzXCIpLFxuICAgICAgICAgIGZvcm1hdHM6IFtcImVzXCJdLFxuICAgICAgICB9LFxuICAgICAgfVxuICAgIDogdW5kZWZpbmVkO1xuICByZXR1cm4ge1xuICAgIGJhc2U6IFwiXCIsXG4gICAgc2VydmVyOiB7XG4gICAgICBwb3J0OiA4MDgwLFxuICAgICAgb3BlbjogZ2V0Um9vdFBhdGgoKSxcbiAgICB9LFxuICAgIGJ1aWxkOiB7XG4gICAgICAuLi5saWIsXG4gICAgICBzb3VyY2VtYXA6IHRydWUsXG4gICAgICB0YXJnZXQ6IFwiZXNuZXh0XCIsXG4gICAgICBtb2R1bGVQcmVsb2FkOiBmYWxzZSxcbiAgICAgIGNzc01pbmlmeTogXCJsaWdodG5pbmdjc3NcIixcbiAgICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgICAgZXh0ZXJuYWwsXG4gICAgICB9LFxuICAgIH0sXG4gICAgcGx1Z2lucyxcbiAgfTtcbn0pO1xuIiwgImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvaG9tZS9lZGV3aXQvd29ya3NwYWNlL2tleWNsb2FrL2tleWNsb2FrL2pzL2FwcHMvYWNjb3VudC11aS9zcmMvdXRpbHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9ob21lL2VkZXdpdC93b3Jrc3BhY2Uva2V5Y2xvYWsva2V5Y2xvYWsvanMvYXBwcy9hY2NvdW50LXVpL3NyYy91dGlscy9nZXRSb290UGF0aC50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vaG9tZS9lZGV3aXQvd29ya3NwYWNlL2tleWNsb2FrL2tleWNsb2FrL2pzL2FwcHMvYWNjb3VudC11aS9zcmMvdXRpbHMvZ2V0Um9vdFBhdGgudHNcIjtpbXBvcnQgeyBnZW5lcmF0ZVBhdGggfSBmcm9tIFwicmVhY3Qtcm91dGVyLWRvbVwiO1xuaW1wb3J0IHsgREVGQVVMVF9SRUFMTSwgUk9PVF9QQVRIIH0gZnJvbSBcIi4uL2NvbnN0YW50c1wiO1xuXG5leHBvcnQgY29uc3QgZ2V0Um9vdFBhdGggPSAocmVhbG0gPSBERUZBVUxUX1JFQUxNKSA9PlxuICBnZW5lcmF0ZVBhdGgoUk9PVF9QQVRILCB7IHJlYWxtIH0pO1xuIiwgImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvaG9tZS9lZGV3aXQvd29ya3NwYWNlL2tleWNsb2FrL2tleWNsb2FrL2pzL2FwcHMvYWNjb3VudC11aS9zcmNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9ob21lL2VkZXdpdC93b3Jrc3BhY2Uva2V5Y2xvYWsva2V5Y2xvYWsvanMvYXBwcy9hY2NvdW50LXVpL3NyYy9jb25zdGFudHMudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL2hvbWUvZWRld2l0L3dvcmtzcGFjZS9rZXljbG9hay9rZXljbG9hay9qcy9hcHBzL2FjY291bnQtdWkvc3JjL2NvbnN0YW50cy50c1wiO2V4cG9ydCBjb25zdCBERUZBVUxUX1JFQUxNID0gXCJtYXN0ZXJcIjtcbmV4cG9ydCBjb25zdCBST09UX1BBVEggPSBcIi9yZWFsbXMvOnJlYWxtL2FjY291bnRcIjtcbmV4cG9ydCBjb25zdCBBRE1JTl9VU0VSID0gXCJhZG1pblwiO1xuZXhwb3J0IGNvbnN0IEFETUlOX1BBU1NXT1JEID0gXCJhZG1pblwiO1xuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUFtVyxPQUFPLFdBQVc7QUFDclgsT0FBTyxVQUFVO0FBQ2pCLFNBQVMsY0FBYyxlQUFlO0FBQ3RDLFNBQVMsZUFBZTtBQUN4QixPQUFPLFNBQVM7OztBQ0ppWCxTQUFTLG9CQUFvQjs7O0FDQTVDLElBQU0sZ0JBQWdCO0FBQ2pZLElBQU0sWUFBWTs7O0FERWxCLElBQU0sY0FBYyxDQUFDLFFBQVEsa0JBQ2xDLGFBQWEsV0FBVyxFQUFFLE1BQU0sQ0FBQzs7O0FESm5DLElBQU0sbUNBQW1DO0FBU3pDLElBQU8sc0JBQVEsYUFBYSxDQUFDLEVBQUUsS0FBSyxNQUFNO0FBQ3hDLFFBQU0sTUFBTSxRQUFRLE1BQU0sUUFBUSxJQUFJLEdBQUcsRUFBRTtBQUMzQyxRQUFNLFdBQVcsQ0FBQyxTQUFTLHFCQUFxQixXQUFXO0FBQzNELFFBQU0sVUFBVSxDQUFDLE1BQU0sR0FBRyxRQUFRLEVBQUUsWUFBWSxLQUFLLENBQUMsQ0FBQztBQUN2RCxNQUFJLElBQUksS0FBSztBQUNYLGFBQVMsS0FBSyxrQkFBa0I7QUFDaEMsYUFBUyxLQUFLLGVBQWU7QUFDN0IsWUFBUSxLQUFLLElBQUksRUFBRSxrQkFBa0IsS0FBSyxDQUFDLENBQUM7QUFBQSxFQUM5QztBQUNBLFFBQU0sTUFBTSxJQUFJLE1BQ1o7QUFBQSxJQUNFLFFBQVE7QUFBQSxJQUNSLEtBQUs7QUFBQSxNQUNILE9BQU8sS0FBSyxRQUFRLGtDQUFXLGNBQWM7QUFBQSxNQUM3QyxTQUFTLENBQUMsSUFBSTtBQUFBLElBQ2hCO0FBQUEsRUFDRixJQUNBO0FBQ0osU0FBTztBQUFBLElBQ0wsTUFBTTtBQUFBLElBQ04sUUFBUTtBQUFBLE1BQ04sTUFBTTtBQUFBLE1BQ04sTUFBTSxZQUFZO0FBQUEsSUFDcEI7QUFBQSxJQUNBLE9BQU87QUFBQSxNQUNMLEdBQUc7QUFBQSxNQUNILFdBQVc7QUFBQSxNQUNYLFFBQVE7QUFBQSxNQUNSLGVBQWU7QUFBQSxNQUNmLFdBQVc7QUFBQSxNQUNYLGVBQWU7QUFBQSxRQUNiO0FBQUEsTUFDRjtBQUFBLElBQ0Y7QUFBQSxJQUNBO0FBQUEsRUFDRjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
