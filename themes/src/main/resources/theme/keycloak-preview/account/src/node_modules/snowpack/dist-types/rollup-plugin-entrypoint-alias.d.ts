import { Plugin } from 'rollup';
/**
 * rollup-plugin-entrypoint-alias
 *
 * Aliases any deep imports from a package to the package name, so that
 * chunking can happen more accurately.
 *
 * Example: lit-element imports from both 'lit-html' & 'lit-html/lit-html.js'.
 * Even though both eventually resolve to the same place, without this plugin
 * we lose the ability to mark "lit-html" as an external package.
 */
export declare function rollupPluginEntrypointAlias({ cwd }: {
    cwd: string;
}): Plugin;
