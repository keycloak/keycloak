#!/usr/bin/env node
/**
 * Check whether a package@version is in the pnpm lockfile and/or production bundles.
 *
 * Usage:
 *   node scripts/check-bundle-dependency.mjs <package> <version> [options]
 *   pnpm check-bundle <package> <version>
 *
 * Requires `pnpm install` from js/. Bundle analysis builds in memory (slow).
 * Lockfile presence does not imply the package is shipped (see lodash CVE case).
 *
 * @see js/scripts/README.md
 */

import { createRequire } from "node:module";
import { readFileSync, readdirSync, statSync, existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { spawnSync } from "node:child_process";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const JS_ROOT = path.resolve(__dirname, "..");
const LOCKFILE = path.join(JS_ROOT, "pnpm-lock.yaml");

const TARGETS = {
  "admin-ui": {
    dir: "apps/admin-ui",
    type: "vite",
    configFile: "vite.config.ts",
  },
  "account-ui": {
    dir: "apps/account-ui",
    type: "vite",
    configFile: "vite.config.ts",
  },
  "themes-vendor": {
    dir: "themes-vendor",
    type: "rollup",
    configFile: "rollup.config.js",
  },
};

const RELATED_PACKAGES = {
  lodash: ["lodash", "lodash-es"],
  "lodash-es": ["lodash", "lodash-es"],
};

const HELP = `Usage: check-bundle-dependency.mjs <package> <version> [options]

Check if a dependency is in the pnpm lockfile and/or production JS bundles.

Arguments:
  package    npm package name (e.g. lodash, lodash-es)
  version    exact version (4.17.23) or range with --semver (e.g. 4.17.x, ^4.18.0)

Options:
  --app <name>       admin-ui | account-ui | themes-vendor | all (default: all)
  --lockfile-only    Skip bundle analysis
  --skip-build       Scan existing target/**/*.map instead of in-memory build
  --semver           Treat version as a semver range
  --strict-name      Do not match related packages (lodash / lodash-es)
  --json             Machine-readable JSON output
  --verbose          Print matching module paths
  -h, --help         Show this help

Exit codes:
  0  Not present in any analyzed production bundle
  1  Present in at least one production bundle
  2  Error (invalid args, build failure, etc.)

Examples:
  node scripts/check-bundle-dependency.mjs lodash 4.17.23
  node scripts/check-bundle-dependency.mjs lodash-es 4.18.1 --app admin-ui
  pnpm check-bundle react 18.3.1 --app admin-ui
`;

/** @typedef {{ name: string, version: string, paths: string[] }} PackageHit */

/**
 * @param {string} preferDir
 * @param {string} moduleName
 */
function resolveFromWorkspace(preferDir, moduleName) {
  const candidates = [
    preferDir,
    "apps/admin-ui",
    "apps/account-ui",
    "themes-vendor",
    ".",
  ];
  for (const dir of candidates) {
    const pkgJson = path.join(JS_ROOT, dir, "package.json");
    if (!existsSync(pkgJson)) continue;
    try {
      const req = createRequire(pkgJson);
      return req.resolve(moduleName);
    } catch {
      // try next
    }
  }
  throw new Error(
    `Cannot resolve "${moduleName}" from workspace. Run pnpm install in js/.`,
  );
}

/**
 * @param {string} userVersion
 * @param {boolean} useSemver
 */
function versionMatches(resolved, userVersion, useSemver) {
  if (!useSemver) {
    return resolved === userVersion;
  }
  return satisfiesSemver(resolved, userVersion);
}

/**
 * Minimal semver satisfies (no extra dependency).
 * @param {string} resolved
 * @param {string} range
 */
function satisfiesSemver(resolved, range) {
  const r = parseVersion(resolved);
  if (!r) return false;

  let expr = range.trim();
  if (/^\d+\.\d+\.\d+$/.test(expr)) {
    return resolved === expr;
  }
  if (/^\d+\.\d+$/.test(expr)) {
    expr = `${expr}.*`;
  }
  if (/^\d+\.\d+\.x$/i.test(expr) || /^\d+\.\d+\.\*$/i.test(expr)) {
    const [a, b] = expr.split(".").map((x) => parseInt(x, 10));
    return r.major === a && r.minor === b;
  }
  if (/^\d+\.x$/i.test(expr) || /^\d+\.\*$/i.test(expr)) {
    const a = parseInt(expr.split(".")[0], 10);
    return r.major === a;
  }
  if (expr.startsWith("^")) {
    const base = parseVersion(expr.slice(1));
    if (!base) return false;
    if (base.major > 0) {
      return (
        r.major === base.major &&
        (r.minor > base.minor ||
          (r.minor === base.minor && r.patch >= base.patch))
      );
    }
    if (base.minor > 0) {
      return r.major === 0 && r.minor === base.minor && r.patch >= base.patch;
    }
    return r.major === 0 && r.minor === 0 && r.patch >= base.patch;
  }
  if (expr.startsWith("~")) {
    const base = parseVersion(expr.slice(1));
    if (!base) return false;
    return (
      r.major === base.major && r.minor === base.minor && r.patch >= base.patch
    );
  }
  if (expr === "*" || expr === "x" || expr === "X") return true;
  return resolved === expr;
}

/** @param {string} v */
function parseVersion(v) {
  const m = /^(\d+)\.(\d+)\.(\d+)/.exec(v);
  if (!m) return null;
  return {
    major: parseInt(m[1], 10),
    minor: parseInt(m[2], 10),
    patch: parseInt(m[3], 10),
  };
}

/**
 * @param {string} packageName
 * @param {boolean} strictName
 */
function packageNamesToMatch(packageName, strictName) {
  if (strictName) return [packageName];
  return RELATED_PACKAGES[packageName] ?? [packageName];
}

/**
 * @param {string} modulePath
 * @returns {PackageHit | null}
 */
function parseModulePath(modulePath) {
  const pnpm =
    /node_modules\/\.pnpm\/((?:@[^@/]+[+/])?[^@/+]+)@([^/_]+).*?\/node_modules\/((?:@[^/]+\/)?[^/]+)/.exec(
      modulePath,
    );
  if (pnpm) {
    const folderName = pnpm[1];
    const version = pnpm[2];
    const logicalName = pnpm[3];
    const name = logicalName.startsWith("@")
      ? logicalName
      : folderName.startsWith("@")
        ? folderName
        : logicalName;
    return { name, version, paths: [modulePath] };
  }
  const flat = /node_modules\/((?:@[^/]+\/)?[^/]+)(?:\/|$)/.exec(modulePath);
  if (flat) {
    return { name: flat[1], version: "", paths: [modulePath] };
  }
  return null;
}

/**
 * @param {string[]} modulePaths
 * @param {string[]} names
 * @param {string} userVersion
 * @param {boolean} useSemver
 */
function findHitsInModulePaths(modulePaths, names, userVersion, useSemver) {
  /** @type {Map<string, PackageHit>} */
  const hits = new Map();
  for (const modulePath of modulePaths) {
    const parsed = parseModulePath(modulePath);
    if (!parsed) continue;
    if (!names.includes(parsed.name)) continue;
    if (
      parsed.version &&
      !versionMatches(parsed.version, userVersion, useSemver)
    ) {
      continue;
    }
    const key = `${parsed.name}@${parsed.version || "unknown"}`;
    const existing = hits.get(key);
    if (existing) {
      existing.paths.push(modulePath);
    } else {
      hits.set(key, { ...parsed, paths: [modulePath] });
    }
  }
  return [...hits.values()];
}

/**
 * @param {string} lockfileContent
 * @param {string[]} names
 * @param {string} userVersion
 * @param {boolean} useSemver
 */
function scanLockfile(lockfileContent, names, userVersion, useSemver) {
  /** @type {{ packageKey: string, version: string, dependents: string[] }[]} */
  const matches = [];
  const packageKeyRe = new RegExp(
    `^  '?(${names.map(escapeRegExp).join("|")})@([^':]+)'?:`,
    "m",
  );

  for (const line of lockfileContent.split("\n")) {
    const m = line.match(packageKeyRe);
    if (!m) continue;
    if (line.includes(": {}")) continue;
    if (!names.includes(m[1])) continue;
    const version = m[2];
    if (!versionMatches(version, userVersion, useSemver)) continue;
    const packageKey = `${m[1]}@${version}`;
    if (!matches.some((entry) => entry.packageKey === packageKey)) {
      matches.push({
        packageKey,
        version,
        dependents: [],
      });
    }
  }

  if (matches.length === 0) {
    return { inLockfile: false, matches: [], dependents: [] };
  }

  const dependents = findLockfileDependents(
    lockfileContent,
    matches.map((x) => x.version),
    names,
    userVersion,
    useSemver,
  );

  for (const match of matches) {
    match.dependents = dependents.get(match.packageKey) ?? [];
  }

  return {
    inLockfile: true,
    matches,
    dependents: [...dependents.values()].flat(),
  };
}

/**
 * @param {string} content
 * @param {string[]} versions
 * @param {string[]} names
 * @param {string} userVersion
 * @param {boolean} useSemver
 */
function findLockfileDependents(
  content,
  versions,
  names,
  userVersion,
  useSemver,
) {
  /** @type {Map<string, string[]>} */
  const result = new Map();
  const lines = content.split("\n");
  let currentBlock = "";

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const blockMatch = /^ {2}([^:]+):$/.exec(line);
    if (blockMatch && !line.startsWith("    ")) {
      currentBlock = blockMatch[1];
    }
    const depMatch = /^ {6}(${names.map(escapeRegExp).join("|")}): (.+)$/.exec(
      line,
    );
    if (!depMatch || !currentBlock) continue;
    const depVersion = depMatch[2].trim();
    const matched = versions.some(
      (v) =>
        depVersion === v ||
        (useSemver && versionMatches(depVersion, userVersion, useSemver)),
    );
    if (!matched && !versions.includes(depVersion)) {
      if (!versionMatches(depVersion, userVersion, useSemver)) continue;
    }
    for (const name of names) {
      const key = `${name}@${depVersion}`;
      if (!result.has(key)) result.set(key, []);
      const list = result.get(key);
      if (!list.includes(currentBlock)) list.push(currentBlock);
    }
  }
  return result;
}

/** @param {string} s */
function escapeRegExp(s) {
  return s.replace(/[.*+?^${}()|[\]\\/]/g, "\\$&");
}

const stripCheckerPlugin = {
  name: "strip-checker-for-bundle-audit",
  configResolved(config) {
    config.plugins = config.plugins.filter(
      (p) => p.name !== "vite-plugin-checker",
    );
  },
};

/**
 * @param {typeof TARGETS['admin-ui']} target
 */
async function analyzeViteBundle(target) {
  const appRoot = path.join(JS_ROOT, target.dir);
  const configFile = path.join(appRoot, target.configFile);
  const viteResolved = resolveFromWorkspace(target.dir, "vite");
  const { createBuilder } = await import(viteResolved);

  const originalCwd = process.cwd();
  process.chdir(appRoot);
  try {
    const builder = await createBuilder({
      configFile,
      root: appRoot,
      logLevel: "error",
      plugins: [stripCheckerPlugin],
    });
    await builder.buildApp();
    return sourceMapPathsForTarget(target);
  } finally {
    process.chdir(originalCwd);
  }
}

/**
 * @param {typeof TARGETS['themes-vendor']} target
 */
async function analyzeRollupBundle(target) {
  const appRoot = path.join(JS_ROOT, target.dir);
  const configFile = path.join(appRoot, target.configFile);
  const rollupResolved = resolveFromWorkspace(target.dir, "rollup");
  const { rollup } = await import(rollupResolved);

  const originalCwd = process.cwd();
  process.chdir(appRoot);
  try {
    const configUrl = pathToFileURL(configFile).href;
    const configModule = await import(`${configUrl}?t=${Date.now()}`);
    const configs = configModule.default;
    const configList = Array.isArray(configs) ? configs : [configs];
    /** @type {string[]} */
    const modulePaths = [];

    for (const rollupConfig of configList) {
      const auditConfig = {
        ...rollupConfig,
        plugins: rollupConfig.plugins.filter(
          (p) => !String(p.name ?? "").includes("terser"),
        ),
      };
      const bundle = await rollup(auditConfig);
      modulePaths.push(...bundle.watchFiles);
      await bundle.close();
    }
    return modulePaths;
  } finally {
    process.chdir(originalCwd);
  }
}

/** @param {string} dir */
function collectSourceMapPaths(dir) {
  /** @type {string[]} */
  const sources = [];
  if (!existsSync(dir)) return sources;

  /** @param {string} current */
  function walk(current) {
    for (const entry of readdirSync(current)) {
      const full = path.join(current, entry);
      const st = statSync(full);
      if (st.isDirectory()) {
        walk(full);
        continue;
      }
      if (!entry.endsWith(".map")) continue;
      try {
        const map = JSON.parse(readFileSync(full, "utf8"));
        if (Array.isArray(map.sources)) {
          sources.push(...map.sources);
        }
      } catch {
        // ignore invalid maps
      }
    }
  }
  walk(dir);
  return sources;
}

/**
 * @param {typeof TARGETS['admin-ui']} target
 */
function sourceMapPathsForTarget(target) {
  const targetDir = path.join(JS_ROOT, target.dir, "target");
  return collectSourceMapPaths(targetDir);
}

/**
 * @param {string} packageName
 */
function runPnpmWhy(packageName) {
  const result = spawnSync("pnpm", ["why", packageName, "-r", "--json"], {
    cwd: JS_ROOT,
    encoding: "utf8",
    maxBuffer: 10 * 1024 * 1024,
  });
  if (result.status !== 0 || !result.stdout) return null;
  try {
    return JSON.parse(result.stdout);
  } catch {
    return null;
  }
}

/**
 * @param {ReturnType<typeof parseArgs>} options
 * @param {string[]} names
 */
async function run(options, names) {
  const lockfileContent = readFileSync(LOCKFILE, "utf8");
  const lock = scanLockfile(
    lockfileContent,
    names,
    options.version,
    options.useSemver,
  );

  /** @type {Record<string, { inBundle: boolean, hits: PackageHit[], moduleCount: number }>} */
  const bundles = {};
  let inAnyBundle = false;

  const appKeys = options.app === "all" ? Object.keys(TARGETS) : [options.app];

  if (!options.lockfileOnly) {
    for (const appKey of appKeys) {
      const target = TARGETS[appKey];
      let modulePaths;
      try {
        if (options.skipBuild) {
          modulePaths = sourceMapPathsForTarget(target);
          if (modulePaths.length === 0) {
            throw new Error(
              `No source maps under ${target.dir}/target. Run a production build first or omit --skip-build.`,
            );
          }
        } else if (target.type === "vite") {
          await ensureViteDepsBuilt(target.dir);
          modulePaths = await analyzeViteBundle(target);
        } else {
          modulePaths = await analyzeRollupBundle(target);
        }
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        throw new Error(`${appKey}: ${message}`);
      }

      const hits = findHitsInModulePaths(
        modulePaths,
        names,
        options.version,
        options.useSemver,
      );
      const inBundle = hits.length > 0;
      if (inBundle) inAnyBundle = true;
      bundles[appKey] = {
        inBundle,
        hits,
        moduleCount: modulePaths.length,
      };
    }
  }

  const pnpmWhy = options.verbose ? runPnpmWhy(names[0]) : null;

  return {
    package: options.packageName,
    version: options.version,
    names,
    lockfile: lock,
    bundles,
    inAnyBundle,
    pnpmWhy,
  };
}

async function ensureViteDepsBuilt() {
  const uiSharedLib = path.join(JS_ROOT, "libs/ui-shared/lib");
  if (!existsSync(uiSharedLib)) {
    const r = spawnSync(
      "pnpm",
      ["--filter", "@keycloak/keycloak-ui-shared", "build"],
      {
        cwd: JS_ROOT,
        stdio: "pipe",
        encoding: "utf8",
      },
    );
    if (r.status !== 0) {
      throw new Error(
        `Failed to build @keycloak/keycloak-ui-shared:\n${r.stderr || r.stdout}`,
      );
    }
  }
  const adminClientLib = path.join(JS_ROOT, "libs/keycloak-admin-client/lib");
  if (!existsSync(adminClientLib)) {
    const r = spawnSync(
      "pnpm",
      ["--filter", "@keycloak/keycloak-admin-client", "build"],
      {
        cwd: JS_ROOT,
        stdio: "pipe",
        encoding: "utf8",
      },
    );
    if (r.status !== 0) {
      throw new Error(
        `Failed to build @keycloak/keycloak-admin-client:\n${r.stderr || r.stdout}`,
      );
    }
  }
}

/**
 * @param {string[]} argv
 */
function parseArgs(argv) {
  const options = {
    packageName: "",
    version: "",
    app: "all",
    lockfileOnly: false,
    skipBuild: false,
    useSemver: false,
    strictName: false,
    json: false,
    verbose: false,
  };

  const positional = [];
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === "-h" || arg === "--help") {
      return { help: true };
    }
    if (arg === "--lockfile-only") {
      options.lockfileOnly = true;
      continue;
    }
    if (arg === "--skip-build") {
      options.skipBuild = true;
      continue;
    }
    if (arg === "--semver") {
      options.useSemver = true;
      continue;
    }
    if (arg === "--strict-name") {
      options.strictName = true;
      continue;
    }
    if (arg === "--json") {
      options.json = true;
      continue;
    }
    if (arg === "--verbose") {
      options.verbose = true;
      continue;
    }
    if (arg === "--app") {
      const value = argv[++i];
      if (!value || (!(value in TARGETS) && value !== "all")) {
        throw new Error(
          `Invalid --app value: ${value}. Use admin-ui, account-ui, themes-vendor, or all.`,
        );
      }
      options.app = value;
      continue;
    }
    if (arg.startsWith("--")) {
      throw new Error(`Unknown option: ${arg}`);
    }
    positional.push(arg);
  }

  if (positional.length < 2) {
    throw new Error("Missing required arguments: <package> <version>");
  }

  options.packageName = positional[0];
  options.version = positional[1];
  return options;
}

/**
 * @param {Awaited<ReturnType<run>>} result
 * @param {ReturnType<parseArgs>} options
 */
function printHuman(result, options) {
  console.log(`Package: ${result.package}@${result.version}`);
  if (result.names.length > 1) {
    console.log(
      `Also matching: ${result.names.filter((n) => n !== result.package).join(", ")}`,
    );
  }
  console.log();

  if (result.lockfile.inLockfile) {
    console.log("Lockfile: YES");
    for (const m of result.lockfile.matches) {
      const deps = m.dependents.length
        ? ` (via ${m.dependents.slice(0, 5).join(", ")}${m.dependents.length > 5 ? ", ..." : ""})`
        : "";
      console.log(`  - ${m.packageKey}${deps}`);
    }
  } else {
    console.log("Lockfile: NO");
  }

  if (!options.lockfileOnly) {
    console.log("\nBundle:");
    for (const [app, data] of Object.entries(result.bundles)) {
      console.log(`  ${app}: ${data.inBundle ? "YES" : "NO"}`);
      if (data.inBundle && options.verbose) {
        for (const hit of data.hits) {
          console.log(`    ${hit.name}@${hit.version}`);
          for (const p of hit.paths.slice(0, 5)) {
            console.log(`      ${p}`);
          }
          if (hit.paths.length > 5) {
            console.log(`      ... and ${hit.paths.length - 5} more`);
          }
        }
      }
    }
  }

  console.log();
  if (options.lockfileOnly) {
    console.log(
      result.lockfile.inLockfile
        ? "Summary: present in lockfile (bundle analysis skipped)."
        : "Summary: not found in lockfile.",
    );
  } else if (result.inAnyBundle) {
    console.log("Summary: shipped in at least one production bundle.");
  } else if (result.lockfile.inLockfile) {
    console.log(
      "Summary: present in lockfile but not shipped in analyzed production bundles.",
    );
  } else {
    console.log("Summary: not in lockfile or analyzed production bundles.");
  }
}

async function main() {
  let options;
  try {
    options = parseArgs(process.argv.slice(2));
  } catch (err) {
    console.error(err instanceof Error ? err.message : err);
    console.error();
    console.error(HELP);
    process.exit(2);
  }

  if (options.help) {
    console.log(HELP);
    process.exit(0);
  }

  const names = packageNamesToMatch(options.packageName, options.strictName);

  try {
    const result = await run(options, names);
    if (options.json) {
      console.log(JSON.stringify(result, null, 2));
    } else {
      printHuman(result, options);
    }
    process.exit(result.inAnyBundle ? 1 : 0);
  } catch (err) {
    console.error(err instanceof Error ? err.message : err);
    process.exit(2);
  }
}

await main();
