// vite.config.ts
import react from "file:///Users/agnieszkagancarczyk/redhat/projects/my/keycloak/js/node_modules/.pnpm/@vitejs+plugin-react-swc@3.3.2_vite@4.4.7/node_modules/@vitejs/plugin-react-swc/index.mjs";
import path from "node:path";
import peerDepsExternal from "file:///Users/agnieszkagancarczyk/redhat/projects/my/keycloak/js/node_modules/.pnpm/rollup-plugin-peer-deps-external@2.2.4_rollup@3.26.3/node_modules/rollup-plugin-peer-deps-external/dist/rollup-plugin-peer-deps-external.js";
import { defineConfig } from "file:///Users/agnieszkagancarczyk/redhat/projects/my/keycloak/js/node_modules/.pnpm/vite@4.4.7_@types+node@20.4.5/node_modules/vite/dist/node/index.js";
import { checker } from "file:///Users/agnieszkagancarczyk/redhat/projects/my/keycloak/js/node_modules/.pnpm/vite-plugin-checker@0.6.1_eslint@8.45.0_typescript@5.1.6_vite@4.4.7/node_modules/vite-plugin-checker/dist/esm/main.js";
import dts from "file:///Users/agnieszkagancarczyk/redhat/projects/my/keycloak/js/node_modules/.pnpm/vite-plugin-dts@3.3.1_@types+node@20.4.5_rollup@3.26.3_typescript@5.1.6_vite@4.4.7/node_modules/vite-plugin-dts/dist/index.mjs";
var __vite_injected_original_dirname = "/Users/agnieszkagancarczyk/redhat/projects/my/keycloak/js/libs/keycloak-masthead";
var vite_config_default = defineConfig({
  resolve: {
    mainFields: ["module"]
  },
  build: {
    target: "ES2022",
    lib: {
      entry: path.resolve(__vite_injected_original_dirname, "src/main.ts"),
      formats: ["es"]
    },
    rollupOptions: {
      plugins: [
        peerDepsExternal({
          includeDependencies: true
        })
      ]
    }
  },
  plugins: [
    react(),
    checker({ typescript: true }),
    dts({ insertTypesEntry: true })
  ]
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvVXNlcnMvYWduaWVzemthZ2FuY2FyY3p5ay9yZWRoYXQvcHJvamVjdHMvbXkva2V5Y2xvYWsvanMvbGlicy9rZXljbG9hay1tYXN0aGVhZFwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL1VzZXJzL2Fnbmllc3prYWdhbmNhcmN6eWsvcmVkaGF0L3Byb2plY3RzL215L2tleWNsb2FrL2pzL2xpYnMva2V5Y2xvYWstbWFzdGhlYWQvdml0ZS5jb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL1VzZXJzL2Fnbmllc3prYWdhbmNhcmN6eWsvcmVkaGF0L3Byb2plY3RzL215L2tleWNsb2FrL2pzL2xpYnMva2V5Y2xvYWstbWFzdGhlYWQvdml0ZS5jb25maWcudHNcIjtpbXBvcnQgcmVhY3QgZnJvbSBcIkB2aXRlanMvcGx1Z2luLXJlYWN0LXN3Y1wiO1xuaW1wb3J0IHBhdGggZnJvbSBcIm5vZGU6cGF0aFwiO1xuaW1wb3J0IHBlZXJEZXBzRXh0ZXJuYWwgZnJvbSBcInJvbGx1cC1wbHVnaW4tcGVlci1kZXBzLWV4dGVybmFsXCI7XG5pbXBvcnQgeyBkZWZpbmVDb25maWcgfSBmcm9tIFwidml0ZVwiO1xuaW1wb3J0IHsgY2hlY2tlciB9IGZyb20gXCJ2aXRlLXBsdWdpbi1jaGVja2VyXCI7XG5pbXBvcnQgZHRzIGZyb20gXCJ2aXRlLXBsdWdpbi1kdHNcIjtcblxuLy8gaHR0cHM6Ly92aXRlanMuZGV2L2NvbmZpZy9cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XG4gIHJlc29sdmU6IHtcbiAgICBtYWluRmllbGRzOiBbXCJtb2R1bGVcIl0sXG4gIH0sXG4gIGJ1aWxkOiB7XG4gICAgdGFyZ2V0OiBcIkVTMjAyMlwiLFxuICAgIGxpYjoge1xuICAgICAgZW50cnk6IHBhdGgucmVzb2x2ZShfX2Rpcm5hbWUsIFwic3JjL21haW4udHNcIiksXG4gICAgICBmb3JtYXRzOiBbXCJlc1wiXSxcbiAgICB9LFxuICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgIHBsdWdpbnM6IFtcbiAgICAgICAgcGVlckRlcHNFeHRlcm5hbCh7XG4gICAgICAgICAgaW5jbHVkZURlcGVuZGVuY2llczogdHJ1ZSxcbiAgICAgICAgfSksXG4gICAgICBdLFxuICAgIH0sXG4gIH0sXG4gIHBsdWdpbnM6IFtcbiAgICByZWFjdCgpLFxuICAgIGNoZWNrZXIoeyB0eXBlc2NyaXB0OiB0cnVlIH0pLFxuICAgIGR0cyh7IGluc2VydFR5cGVzRW50cnk6IHRydWUgfSksXG4gIF0sXG59KTtcbiJdLAogICJtYXBwaW5ncyI6ICI7QUFBa2EsT0FBTyxXQUFXO0FBQ3BiLE9BQU8sVUFBVTtBQUNqQixPQUFPLHNCQUFzQjtBQUM3QixTQUFTLG9CQUFvQjtBQUM3QixTQUFTLGVBQWU7QUFDeEIsT0FBTyxTQUFTO0FBTGhCLElBQU0sbUNBQW1DO0FBUXpDLElBQU8sc0JBQVEsYUFBYTtBQUFBLEVBQzFCLFNBQVM7QUFBQSxJQUNQLFlBQVksQ0FBQyxRQUFRO0FBQUEsRUFDdkI7QUFBQSxFQUNBLE9BQU87QUFBQSxJQUNMLFFBQVE7QUFBQSxJQUNSLEtBQUs7QUFBQSxNQUNILE9BQU8sS0FBSyxRQUFRLGtDQUFXLGFBQWE7QUFBQSxNQUM1QyxTQUFTLENBQUMsSUFBSTtBQUFBLElBQ2hCO0FBQUEsSUFDQSxlQUFlO0FBQUEsTUFDYixTQUFTO0FBQUEsUUFDUCxpQkFBaUI7QUFBQSxVQUNmLHFCQUFxQjtBQUFBLFFBQ3ZCLENBQUM7QUFBQSxNQUNIO0FBQUEsSUFDRjtBQUFBLEVBQ0Y7QUFBQSxFQUNBLFNBQVM7QUFBQSxJQUNQLE1BQU07QUFBQSxJQUNOLFFBQVEsRUFBRSxZQUFZLEtBQUssQ0FBQztBQUFBLElBQzVCLElBQUksRUFBRSxrQkFBa0IsS0FBSyxDQUFDO0FBQUEsRUFDaEM7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=
