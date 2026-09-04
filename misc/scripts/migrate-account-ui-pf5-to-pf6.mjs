#!/usr/bin/env node

import { spawn } from "node:child_process";
import { mkdtemp, mkdir, readFile, writeFile, cp, rm, access, readdir } from "node:fs/promises";
import { constants as fsConstants } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(SCRIPT_DIR, "..", "..");
const DEFAULT_APP_PATH = path.join(REPO_ROOT, "js", "apps", "account-ui");
const DEFAULT_REPORT_DIR = path.join(REPO_ROOT, "misc", "scripts", "reports");
const DEFAULT_BACKUP_DIR = path.join(DEFAULT_REPORT_DIR, "backups");
const PF5_PATTERN = /pf-v5-|patternfly-v5|--pf-v5-|--pf-t--temp--dev--tbd|t_temp_dev_tbd/;

function usage() {
  console.log(`Usage:
  node misc/scripts/migrate-account-ui-pf5-to-pf6.mjs [options]

Options:
  --app-path <path>          Account UI app root (default: js/apps/account-ui)
  --apply                    Apply changes to the source app (default: dry-run)
  --report-dir <path>        Directory for markdown/json reports
  --backup-dir <path>        Directory for apply-mode backups
  --fail-on-unresolved       Exit non-zero if PF5/temp tokens remain
  --skip-pf-codemods         Skip @patternfly/pf-codemods v5->v6 step
  --skip-css-vars-updater    Skip @patternfly/css-vars-updater step
  --skip-class-updater       Skip @patternfly/class-name-updater step
  -h, --help                 Show this help
`);
}

function parseArgs(argv) {
  const options = {
    appPath: DEFAULT_APP_PATH,
    apply: false,
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
      case "--app-path":
        options.appPath = path.resolve(argv[++i]);
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

async function resolveAppDir(appPath) {
  const absolute = path.resolve(appPath);
  const packageJson = path.join(absolute, "package.json");

  if (!(await pathExists(packageJson))) {
    throw new Error(`Could not find account-ui app at '${absolute}'. Expected package.json.`);
  }

  return absolute;
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
        if (entry.name === "node_modules" || entry.name === "target" || entry.name === "lib") {
          continue;
        }
        stack.push(fullPath);
      } else if (entry.isFile()) {
        files.push(fullPath);
      }
    }
  }

  return files;
}

async function scanResiduals(appDir) {
  const filePaths = await listFilesRecursive(appDir);
  const residuals = [];

  for (const filePath of filePaths) {
    const ext = path.extname(filePath).toLowerCase();
    if (![".css", ".scss", ".less", ".js", ".jsx", ".ts", ".tsx", ".md", ".json", ".ftl", ".properties"].includes(ext)) {
      continue;
    }

    const content = await readFile(filePath, "utf8");
    const lines = content.split("\n");
    for (let i = 0; i < lines.length; i++) {
      if (PF5_PATTERN.test(lines[i])) {
        residuals.push({
          file: path.relative(appDir, filePath),
          line: i + 1,
          text: lines[i],
        });
      }
    }
  }

  return residuals;
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

  return `# Account UI PF5 -> PF6 Migration Report

- Mode: **${report.mode}**
- App path: \`${report.appPath}\`
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

  const appDir = await resolveAppDir(options.appPath);
  const runTimestamp = formatTimestamp();
  const tempRoot = await mkdtemp(path.join(tmpdir(), "kc-pf6-account-migrate-"));
  const stagedAppDir = path.join(tempRoot, "account-ui");
  await cp(appDir, stagedAppDir, { recursive: true });

  const steps = [];
  const stagedAppDirRelative = "account-ui";
  const stagedSrcDir = path.join(stagedAppDir, "src");
  const stagedTestDir = path.join(stagedAppDir, "test");

  if (!options.skipPfCodemods && (await pathExists(stagedSrcDir))) {
    steps.push(
      await runCommand(
        "npx",
        [
          "@patternfly/pf-codemods@latest",
          "--v6",
          "--fix",
          path.join(stagedAppDirRelative, "src"),
        ],
        tempRoot,
      ),
    );

    if (await pathExists(stagedTestDir)) {
      steps.push(
        await runCommand(
          "npx",
          [
            "@patternfly/pf-codemods@latest",
            "--v6",
            "--fix",
            path.join(stagedAppDirRelative, "test"),
          ],
          tempRoot,
        ),
      );
    }
  }

  if (!options.skipCssVarsUpdater) {
    const cssTargets = [];
    if (await pathExists(stagedSrcDir)) {
      cssTargets.push(path.join(stagedAppDirRelative, "src"));
    }
    if (await pathExists(stagedTestDir)) {
      cssTargets.push(path.join(stagedAppDirRelative, "test"));
    }

    if (cssTargets.length > 0) {
      steps.push(
        await runCommand(
          "npx",
          ["@patternfly/css-vars-updater@latest", ...cssTargets, "--fix"],
          tempRoot,
        ),
      );
    }
  }

  if (!options.skipClassUpdater) {
    steps.push(
      await runCommand(
        "npx",
        [
          "@patternfly/class-name-updater@latest",
          "--v6",
          "--extensions",
          "css,scss,less,html,js,jsx,ts,tsx,md,ftl,properties",
          stagedAppDirRelative,
          "--fix",
        ],
        tempRoot,
      ),
    );
  }

  const residuals = await scanResiduals(stagedAppDir);
  const failedStep = steps.find((step) => step.exitCode !== 0);
  const hasUnresolvedResiduals =
    options.failOnUnresolved && residuals.length > 0;

  let backupPath;
  let applied = false;

  if (!failedStep && options.apply && !hasUnresolvedResiduals) {
    await mkdir(options.backupDir, { recursive: true });
    backupPath = path.join(options.backupDir, `account-ui-${runTimestamp}`);
    await cp(appDir, backupPath, { recursive: true });

    const entries = await readdir(appDir, { withFileTypes: true });
    for (const entry of entries) {
      if (entry.name === "node_modules" || entry.name === "target" || entry.name === "lib") {
        continue;
      }
      await rm(path.join(appDir, entry.name), { recursive: true, force: true });
    }

    const stagedEntries = await readdir(stagedAppDir, { withFileTypes: true });
    for (const entry of stagedEntries) {
      if (entry.name === "node_modules" || entry.name === "target" || entry.name === "lib") {
        continue;
      }
      await cp(path.join(stagedAppDir, entry.name), path.join(appDir, entry.name), {
        recursive: true,
      });
    }
    applied = true;
  }

  const report = {
    timestamp: new Date().toISOString(),
    mode: options.apply ? "apply" : "dry-run",
    appPath: appDir,
    backupPath,
    steps,
    residuals,
    applied,
    success: !failedStep && (!options.failOnUnresolved || residuals.length === 0),
  };

  await mkdir(options.reportDir, { recursive: true });
  const reportPrefix = `account-ui-pf5-to-pf6-${runTimestamp}`;
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
