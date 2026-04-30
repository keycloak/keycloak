import { defineConfig } from "rollup";
import { nodeResolve } from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";
import replace from "@rollup/plugin-replace";
import terser from "@rollup/plugin-terser";
import path from "node:path";

const plugins = [
  nodeResolve(),
  commonjs({
    strictRequires: "auto",
  }),
  replace({
    preventAssignment: true,
    "process.env.NODE_ENV": '"production"',
  }),
  terser(),
];

const litPlugins = [nodeResolve(), terser()];

const targetDir = "target/classes/theme/keycloak/common/resources/vendor";

/** @type{import("rollup").WarningHandlerWithDefault} */
function onwarn(warning, defaultHandler) {
  if (warning.code === "UNRESOLVED_IMPORT") {
    throw new Error(`Unresolved import: ${warning.exporter}`);
  }

  defaultHandler(warning);
}

export default defineConfig([
  // React
  {
    input: [
      "node_modules/react/cjs/react.production.min.js",
      "node_modules/react/cjs/react-jsx-runtime.production.min.js",
    ],
    output: {
      dir: path.join(targetDir, "react"),
      format: "es",
    },
    plugins,
    onwarn,
  },
  {
    input: "node_modules/react-dom/cjs/react-dom.production.min.js",
    output: {
      dir: path.join(targetDir, "react-dom"),
      format: "es",
    },
    external: ["react"],
    plugins,
    onwarn,
  },
  // Web Crypto Shim
  {
    input: "src/main/js/web-crypto-shim.js",
    output: {
      dir: path.join(targetDir, "web-crypto-shim"),
      format: "es",
    },
    plugins,
    onwarn,
  },
  // Lit - bundled with all dependencies
  {
    input: "src/main/js/lit-bundle.js",
    output: {
      file: path.join(targetDir, "lit/lit.js"),
      format: "es",
    },
    plugins: litPlugins,
    onwarn,
  },
  // @lit/context
  {
    input: "src/main/js/lit-context.js",
    output: {
      file: path.join(targetDir, "lit-context/context.js"),
      format: "es",
    },
    external: ["lit"],
    plugins: litPlugins,
    onwarn,
  },
  // keycloak-js
  {
    input: "src/main/js/keycloak-js-bundle.js",
    output: {
      file: path.join(targetDir, "keycloak-js/keycloak.js"),
      format: "es",
    },
    plugins: litPlugins,
    onwarn,
  },
  // i18next
  {
    input: "src/main/js/i18next-bundle.js",
    output: {
      file: path.join(targetDir, "i18next/i18next.js"),
      format: "es",
    },
    plugins: litPlugins,
    onwarn,
  },
]);
