// ESM-HMR Interface: `import.meta.hot`

interface ImportMeta {
  // TODO: Import the exact .d.ts files from "esm-hmr"
  // https://github.com/pikapkg/esm-hmr
  hot: any;
  env: Record<string, any>;
}
