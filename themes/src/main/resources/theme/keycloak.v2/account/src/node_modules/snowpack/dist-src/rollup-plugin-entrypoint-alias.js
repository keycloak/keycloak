import path from 'path';
import { resolveDependencyManifest } from './util';
const IS_DEEP_PACKAGE_IMPORT = /^(@[\w-]+\/)?([\w-]+)\/(.*)/;
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
export function rollupPluginEntrypointAlias({ cwd }) {
    return {
        name: 'snowpack:rollup-plugin-entrypoint-alias',
        resolveId(source, importer) {
            if (!IS_DEEP_PACKAGE_IMPORT.test(source)) {
                return null;
            }
            const [, packageScope, packageName] = source.match(IS_DEEP_PACKAGE_IMPORT);
            const packageFullName = packageScope ? `${packageScope}${packageName}` : packageName;
            const [, manifest] = resolveDependencyManifest(packageFullName, cwd);
            if (!manifest) {
                return null;
            }
            let needsAlias = (typeof manifest.module === 'string' &&
                source === path.posix.join(packageFullName, manifest.module)) ||
                (typeof manifest.browser === 'string' &&
                    source === path.posix.join(packageFullName, manifest.browser)) ||
                (typeof manifest.main === 'string' &&
                    source === path.posix.join(packageFullName, manifest.main));
            if (!needsAlias) {
                return null;
            }
            return this.resolve(packageFullName, importer, { skipSelf: true }).then((resolved) => {
                return resolved || null;
            });
        },
    };
}
