import fs from 'fs';
import path from 'path';
import got from 'got';
import cachedir from 'cachedir';
export const PIKA_CDN = `https://cdn.pika.dev`;
export const CACHE_DIR = cachedir('snowpack');
export const RESOURCE_CACHE = path.join(CACHE_DIR, 'pkg-cache-1.4');
export const HAS_CDN_HASH_REGEX = /\-[a-zA-Z0-9]{16,}/;
export async function readLockfile(cwd) {
    try {
        var lockfileContents = fs.readFileSync(path.join(cwd, 'snowpack.lock.json'), {
            encoding: 'utf8',
        });
    }
    catch (err) {
        // no lockfile found, ignore and continue
        return null;
    }
    // If this fails, we actually do want to alert the user by throwing
    return JSON.parse(lockfileContents);
}
export async function writeLockfile(loc, importMap) {
    const sortedImportMap = { imports: {} };
    for (const key of Object.keys(importMap.imports).sort()) {
        sortedImportMap.imports[key] = importMap.imports[key];
    }
    fs.writeFileSync(loc, JSON.stringify(sortedImportMap, undefined, 2), { encoding: 'utf8' });
}
export function fetchCDNResource(resourceUrl) {
    if (!resourceUrl.startsWith(PIKA_CDN)) {
        resourceUrl = PIKA_CDN + resourceUrl;
    }
    return got(resourceUrl, {
        headers: { 'user-agent': `snowpack/v1.4 (https://snowpack.dev)` },
        throwHttpErrors: false,
    });
}
export function isTruthy(item) {
    return Boolean(item);
}
/**
 * Given a package name, look for that package's package.json manifest.
 * Return both the manifest location (if believed to exist) and the
 * manifest itself (if found).
 *
 * NOTE: You used to be able to require() a package.json file directly,
 * but now with export map support in Node v13 that's no longer possible.
 */
export function resolveDependencyManifest(dep, cwd) {
    // Attempt #1: Resolve the dependency manifest normally. This works for most
    // packages, but fails when the package defines an export map that doesn't
    // include a package.json. If we detect that to be the reason for failure,
    // move on to our custom implementation.
    try {
        const depManifest = require.resolve(`${dep}/package.json`, { paths: [cwd] });
        return [depManifest, require(depManifest)];
    }
    catch (err) {
        if ((err.code === 'MODULE_NOT_FOUND' || err.code === 'ERR_PACKAGE_PATH_NOT_EXPORTED') &&
            err.message.includes(`'./package.json'`)) {
            // its most likely an export map issue, move on to our manual resolver.
            // TODO(03-2020): Remove MODULE_NOT_FOUND check (only needed for Node v13.1-v13.9).
        }
        else {
            throw err;
        }
    }
    // Attempt #2: Resolve the dependency manifest manually. This involves resolving
    // the dep itself to find the entrypoint file, and then haphazardly replacing the
    // file path within the package with a "./package.json" instead. It's not as
    // thorough as Attempt #1, but it should work well until export maps become more
    // established & move out of experimental mode.
    let result = [null, null];
    try {
        const fullPath = require.resolve(dep, { paths: [cwd] });
        // Strip everything after the package name to get the package root path
        // NOTE: This find-replace is very gross, replace with something like upath.
        const searchPath = `${path.sep}node_modules${path.sep}${dep.replace('/', path.sep)}`;
        const indexOfSearch = fullPath.lastIndexOf(searchPath);
        if (indexOfSearch >= 0) {
            const manifestPath = fullPath.substring(0, indexOfSearch + searchPath.length + 1) + 'package.json';
            result[0] = manifestPath;
            const manifestStr = fs.readFileSync(manifestPath, { encoding: 'utf8' });
            result[1] = JSON.parse(manifestStr);
        }
    }
    catch (err) {
        // ignore
    }
    finally {
        return result;
    }
}
/**
 * If Rollup erred parsing a particular file, show suggestions based on its
 * file extension (note: lowercase is fine).
 */
export const MISSING_PLUGIN_SUGGESTIONS = {
    '.css': 'Try installing rollup-plugin-postcss and adding it to Snowpack (https://www.snowpack.dev/#custom-rollup-plugins)',
    '.svelte': 'Try installing rollup-plugin-svelte and adding it to Snowpack (https://www.snowpack.dev/#custom-rollup-plugins)',
    '.vue': 'Try installing rollup-plugin-vue and adding it to Snowpack (https://www.snowpack.dev/#custom-rollup-plugins)',
};
