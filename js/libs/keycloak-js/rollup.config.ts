import terser from "@rollup/plugin-terser";
import path from "node:path";
import type { OutputOptions, RollupOptions } from "rollup";
import { defineConfig } from "rollup";

interface DefineOptionsArgs {
  file: string;
  name: string;
  amdId: string;
}

const sourceFile = (file: string) => path.join("src", file);
const targetFile = (file: string) => path.join("dist", file);

function defineOptions({
  file,
  name,
  amdId,
}: DefineOptionsArgs): RollupOptions[] {
  const input = sourceFile(`${file}.js`);
  const umdOutput: OutputOptions = {
    format: "umd",
    name,
    amd: { id: amdId },
  };

  return [
    // Modern ES module variant.
    {
      input,
      output: [
        {
          file: targetFile(`${file}.mjs`),
        },
      ],
    },
    // Legacy Universal Module Definition, or “UMD”.
    {
      input,
      output: [
        {
          ...umdOutput,
          file: targetFile(`${file}.js`),
        },
        {
          ...umdOutput,
          file: targetFile(`${file}.min.js`),
          sourcemap: true,
          sourcemapExcludeSources: true,
          plugins: [terser()],
        },
      ],
    },
  ];
}

export default defineConfig([
  ...defineOptions({
    file: "keycloak",
    name: "Keycloak",
    amdId: "keycloak",
  }),
  ...defineOptions({
    file: "keycloak-authz",
    name: "KeycloakAuthorization",
    amdId: "keycloak-authorization",
  }),
]);
