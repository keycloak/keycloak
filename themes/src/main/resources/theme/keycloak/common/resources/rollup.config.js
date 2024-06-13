import { defineConfig } from "rollup";
import { nodeResolve } from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";
import replace from "@rollup/plugin-replace";
import terser from "@rollup/plugin-terser";

const plugins = [
  nodeResolve(),
  commonjs(),
  replace({
    preventAssignment: true,
    // React depends on process.env.NODE_ENV to determine which code to include for production.
    // This ensures that no additional code meant for development is included in the build.
    "process.env.NODE_ENV": JSON.stringify("production"),
  }),
  terser(),
];

export default defineConfig([
  {
    input: [
      "node_modules/react/cjs/react.production.min.js",
      "node_modules/react/cjs/react-jsx-runtime.production.min.js",
    ],
    output: {
      dir: "vendor/react",
      format: "es",
    },
    plugins,
  },
  {
    input: "node_modules/react-dom/cjs/react-dom.production.min.js",
    output: {
      dir: "vendor/react-dom",
      format: "es",
    },
    external: ["react"],
    plugins,
  },
]);
