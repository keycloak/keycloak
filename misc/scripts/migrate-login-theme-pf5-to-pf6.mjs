#!/usr/bin/env node

import { spawn } from "node:child_process";
import { mkdtemp, mkdir, readFile, writeFile, cp, rm, access, readdir } from "node:fs/promises";
import { constants as fsConstants } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(SCRIPT_DIR, "..", "..");
const DEFAULT_REPORT_DIR = path.join(REPO_ROOT, "misc", "scripts", "reports");
const DEFAULT_BACKUP_DIR = path.join(DEFAULT_REPORT_DIR, "backups");
const PF5_PATTERN = /pf-v5-|patternfly-v5|--pf-v5-|--pf-t--temp--dev--tbd|t_temp_dev_tbd/;

function usage() {
  console.log(`Usage:
  node misc/scripts/migrate-login-theme-pf5-to-pf6.mjs --theme-path <path> [options]

Required:
  --theme-path <path>        Custom theme root or login directory

Options:
  --apply                    Apply changes to the source theme (default: dry-run)
  --report-dir <path>        Directory for markdown/json reports
  --backup-dir <path>        Directory for apply-mode backups
  --allow-builtin-theme      Allow running on built-in keycloak themes
  --fail-on-unresolved       Exit non-zero if PF5/temp tokens remain
  --skip-pf-codemods         Skip @patternfly/pf-codemods tokens-update step
  --skip-css-vars-updater    Skip @patternfly/css-vars-updater step
  --skip-class-updater       Skip @patternfly/class-name-updater step
  -h, --help                 Show this help
`);
}

function parseArgs(argv) {
  const options = {
    apply: false,
    allowBuiltinTheme: false,
    failOnUnresolved: false,
    skipPfCodemods: false,
    skipCssVarsUpdater: false,
    skipClassUpdater: false,
    reportDir: DEFAULT_REPORT_DIR,
    backupDir: DEFAULT_BACKUP_DIR,
  };

  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    switch (arg) {
      case "--theme-path":
        options.themePath = argv[++i];
        break;
      case "--report-dir":
        options.reportDir = path.resolve(argv[++i]);
        break;
      case "--backup-dir":
        options.backupDir = path.resolve(argv[++i]);
        break;
      case "--apply":
        options.apply = true;
        break;
      case "--allow-builtin-theme":
        options.allowBuiltinTheme = true;
        break;
      case "--fail-on-unresolved":
        options.failOnUnresolved = true;
        break;
      case "--skip-pf-codemods":
        options.skipPfCodemods = true;
        break;
      case "--skip-css-vars-updater":
        options.skipCssVarsUpdater = true;
        break;
      case "--skip-class-updater":
        options.skipClassUpdater = true;
        break;
      case "-h":
      case "--help":
        options.help = true;
        break;
      default:
        throw new Error(`Unknown option: ${arg}`);
    }
  }

  return options;
}

async function pathExists(targetPath) {
  try {
    await access(targetPath, fsConstants.F_OK);
    return true;
  } catch {
    return false;
  }
}

async function resolveLoginDir(themePath) {
  const absolute = path.resolve(themePath);
  const directThemeProps = path.join(absolute, "theme.properties");
  const nestedThemeProps = path.join(absolute, "login", "theme.properties");

  if (await pathExists(directThemeProps) && path.basename(absolute) === "login") {
    return absolute;
  }

  if (await pathExists(nestedThemeProps)) {
    return path.join(absolute, "login");
  }

  throw new Error(
    `Could not find login theme at '${absolute}'. Expected either <theme>/login/theme.properties or <login>/theme.properties.`,
  );
}

function isBuiltinTheme(loginDir) {
  const normalized = path.resolve(loginDir);
  const builtinRoots = [
    path.resolve(REPO_ROOT, "themes", "src", "main", "resources", "theme", "keycloak"),
    path.resolve(REPO_ROOT, "themes", "src", "main", "resources", "theme", "keycloak.v3"),
    path.resolve(REPO_ROOT, "themes", "src", "main", "resources-community", "theme", "keycloak"),
    path.resolve(REPO_ROOT, "themes", "src", "main", "resources-community", "theme", "keycloak.v3"),
  ];

  return builtinRoots.some((root) => normalized === path.join(root, "login"));
}

async function runCommand(command, args, cwd) {
  const startedAt = Date.now();
  const env = { ...process.env, NODE_OPTIONS: "--max-old-space-size=4096" };

  return await new Promise((resolve) => {
    const child = spawn(command, args, { cwd, env });
    let stdout = "";
    let stderr = "";

    child.stdout.on("data", (data) => {
      stdout += data.toString();
    });
    child.stderr.on("data", (data) => {
      stderr += data.toString();
    });

    child.on("close", (code) => {
      resolve({
        command: [command, ...args].join(" "),
        exitCode: code ?? 1,
        stdout,
        stderr,
        elapsedMs: Date.now() - startedAt,
      });
    });
  });
}

async function listFilesRecursive(rootPath) {
  const files = [];
  const stack = [rootPath];

  while (stack.length > 0) {
    const current = stack.pop();
    const entries = await readdir(current, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(current, entry.name);
      if (entry.isDirectory()) {
        stack.push(fullPath);
      } else if (entry.isFile()) {
        files.push(fullPath);
      }
    }
  }

  return files;
}

async function scanResiduals(loginDir) {
  const filePaths = await listFilesRecursive(loginDir);
  const residuals = [];

  for (const filePath of filePaths) {
    const ext = path.extname(filePath).toLowerCase();
    if (![".ftl", ".properties", ".css", ".scss", ".less", ".js", ".jsx", ".ts", ".tsx", ".md"].includes(ext)) {
      continue;
    }

    const content = await readFile(filePath, "utf8");
    const lines = content.split("\n");
    for (let i = 0; i < lines.length; i++) {
      if (PF5_PATTERN.test(lines[i])) {
        residuals.push({
          file: path.relative(loginDir, filePath),
          line: i + 1,
          text: lines[i],
        });
      }
    }
  }

  return residuals;
}

async function rewriteThemeProperties(themePropertiesPath) {
  let content = await readFile(themePropertiesPath, "utf8");
  content = content
    .replaceAll("vendor/patternfly-v5/", "vendor/patternfly-v6/")
    .replaceAll("pf-v5-theme-dark", "pf-v6-theme-dark");
  await writeFile(themePropertiesPath, content, "utf8");
}

function formatTimestamp(date = new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}-${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`;
}

function toMarkdownReport(report) {
  const stepLines = report.steps
    .map((step) => {
      const state = step.exitCode === 0 ? "OK" : "FAILED";
      return `- ${state} \`${step.command}\` (${step.elapsedMs} ms)`;
    })
    .join("\n");

  const residualSection =
    report.residuals.length === 0
      ? "No unresolved PF5 markers or temporary PF6 tokens found."
      : report.residuals
          .slice(0, 200)
          .map(
            (item) =>
              `- \`${item.file}:${item.line}\` ${item.text.trim()}`,
          )
          .join("\n");

  return `# Login Theme PF5 -> PF6 Migration Report

- Mode: **${report.mode}**
- Theme path: \`${report.themePath}\`
- Login path: \`${report.loginPath}\`
- Timestamp: \`${report.timestamp}\`

## Steps
${stepLines || "- No steps were executed."}

## Residual Findings
${residualSection}

## Result
- Success: **${report.success}**
- Applied: **${report.applied}**
${report.backupPath ? `- Backup: \`${report.backupPath}\`` : ""}
`;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    usage();
    return;
  }

  if (!options.themePath) {
    throw new Error("Missing required option: --theme-path");
  }

  const loginDir = await resolveLoginDir(options.themePath);
  if (!options.allowBuiltinTheme && isBuiltinTheme(loginDir)) {
    throw new Error(
      `Refusing to run on built-in theme '${loginDir}'. Pass --allow-builtin-theme if this is intentional.`,
    );
  }

  const themeName = path.basename(path.dirname(loginDir));
  const runTimestamp = formatTimestamp();
  const tempRoot = await mkdtemp(path.join(tmpdir(), "kc-pf6-login-migrate-"));
  const stagedLoginDir = path.join(tempRoot, "login");
  await cp(loginDir, stagedLoginDir, { recursive: true });

  const steps = [];
  const stagedLoginDirRelative = "login";

  const stagedJsDir = path.join(stagedLoginDir, "resources", "js");
  if (!options.skipPfCodemods && (await pathExists(stagedJsDir))) {
    steps.push(
      await runCommand(
        "npx",
        [
          "@patternfly/pf-codemods@latest",
          "--v6",
          "--only",
          "tokens-update",
          path.join(stagedLoginDirRelative, "resources", "js"),
          "--fix",
        ],
        tempRoot,
      ),
    );
  }

  const stagedCssDir = path.join(stagedLoginDir, "resources", "css");
  if (!options.skipCssVarsUpdater && (await pathExists(stagedCssDir))) {
    steps.push(
      await runCommand(
        "npx",
        [
          "@patternfly/css-vars-updater@latest",
          path.join(stagedLoginDirRelative, "resources", "css"),
          "--fix",
        ],
        tempRoot,
      ),
    );
  }

  if (!options.skipClassUpdater) {
    steps.push(
      await runCommand(
        "npx",
        [
          "@patternfly/class-name-updater@latest",
          "--v6",
          "--extensions",
          "ftl,properties,css,scss,less,html,js,jsx,ts,tsx,md",
          stagedLoginDirRelative,
          "--fix",
        ],
        tempRoot,
      ),
    );
  }

  await rewriteThemeProperties(path.join(stagedLoginDir, "theme.properties"));
  const residuals = await scanResiduals(stagedLoginDir);
  const failedStep = steps.find((step) => step.exitCode !== 0);

  let backupPath;
  let applied = false;

  if (!failedStep && options.apply) {
    await mkdir(options.backupDir, { recursive: true });
    backupPath = path.join(options.backupDir, `${themeName}-login-${runTimestamp}`);
    await cp(loginDir, backupPath, { recursive: true });

    await rm(loginDir, { recursive: true, force: true });
    await cp(stagedLoginDir, loginDir, { recursive: true });
    applied = true;
  }

  const report = {
    timestamp: new Date().toISOString(),
    mode: options.apply ? "apply" : "dry-run",
    themePath: path.resolve(options.themePath),
    loginPath: loginDir,
    backupPath,
    steps,
    residuals,
    applied,
    success: !failedStep && (!options.failOnUnresolved || residuals.length === 0),
  };

  await mkdir(options.reportDir, { recursive: true });
  const reportPrefix = `login-theme-pf5-to-pf6-${themeName}-${runTimestamp}`;
  const reportJsonPath = path.join(options.reportDir, `${reportPrefix}.json`);
  const reportMdPath = path.join(options.reportDir, `${reportPrefix}.md`);

  await writeFile(reportJsonPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  await writeFile(reportMdPath, toMarkdownReport(report), "utf8");

  if (failedStep) {
    console.error(`Migration step failed: ${failedStep.command}`);
    console.error(`Report: ${reportMdPath}`);
    console.error(`REPORT_READY:${reportMdPath}`);
    process.exit(1);
  }

  if (options.failOnUnresolved && residuals.length > 0) {
    console.error(
      `Found ${residuals.length} unresolved PF5/PF6-temp markers (use report for details): ${reportMdPath}`,
    );
    console.error(`REPORT_READY:${reportMdPath}`);
    process.exit(2);
  }

  console.log(`Migration ${options.apply ? "applied" : "analyzed"} successfully.`);
  console.log(`Residual findings: ${residuals.length}`);
  console.log(`Report: ${reportMdPath}`);
  console.log(`REPORT_READY:${reportMdPath}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
