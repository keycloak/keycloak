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
    // React depends on process.env.NODE_ENV to determine which code to include for production.
    // This ensures that no additional code meant for development is included in the build.
    "process.env.NODE_ENV": '"production"',
  }),
  terser(),
];

const targetDir = "target/classes/theme/keycloak/common/resources/vendor";

/** @type{import("rollup").WarningHandlerWithDefault} */
function onwarn(warning, defaultHandler) {
  if (warning.code === "UNRESOLVED_IMPORT") {
    throw new Error(`Unresolved import: ${warning.exporter}`);
  }

  defaultHandler(warning);
}

export default defineConfig([
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
  {
    input: "src/main/js/web-crypto-shim.js",
    output: {
      dir: path.join(targetDir, "web-crypto-shim"),
      format: "es",
    },
    plugins,
    onwarn,
  },
]);
