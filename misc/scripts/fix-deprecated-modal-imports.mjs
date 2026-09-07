#!/usr/bin/env node

import { readdir, readFile, writeFile, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url));
const SRC_DIR = path.resolve(SCRIPT_DIR, "..", "..", "js", "apps", "admin-ui", "src");

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await walk(full)));
    } else if (/\.(ts|tsx)$/.test(entry.name)) {
      files.push(full);
    }
  }
  return files;
}

const DEPRECATED_IMPORT_RE =
  /import\s*\{([^}]*)\}\s*from\s*['"]@patternfly\/react-core\/deprecated['"];?\s*\n/g;
const CORE_IMPORT_RE =
  /import\s*\{([^}]*)\}\s*from\s*['"]@patternfly\/react-core['"];?\s*\n/;

const MODAL_SYMBOLS = new Set(["Modal", "ModalVariant"]);

function parseImports(importBody) {
  return importBody
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean)
    .map((entry) => {
      const match = entry.match(/^(\w+)$/);
      return match ? { name: match[1], raw: entry } : null;
    })
    .filter(Boolean);
}

function formatImport(symbols) {
  const lines = symbols.map((s) => `\t${s}`);
  return `import {\n${lines.join(",\n")}\n} from '@patternfly/react-core';\n`;
}

async function processFile(filePath) {
  let content = await readFile(filePath, "utf8");
  if (!content.includes("@patternfly/react-core/deprecated")) {
    return false;
  }

  const deprecatedMatches = [...content.matchAll(DEPRECATED_IMPORT_RE)];
  if (deprecatedMatches.length === 0) {
    return false;
  }

  let modalSymbols = [];
  let otherDeprecatedSymbols = [];

  for (const match of deprecatedMatches) {
    const symbols = parseImports(match[1]);
    for (const sym of symbols) {
      if (MODAL_SYMBOLS.has(sym.name)) {
        modalSymbols.push(sym.name);
      } else {
        otherDeprecatedSymbols.push(sym);
      }
    }
  }

  if (modalSymbols.length === 0) {
    return false;
  }

  const coreMatch = content.match(CORE_IMPORT_RE);
  if (!coreMatch) {
    return false;
  }

  const existingCore = parseImports(coreMatch[1]).map((s) => s.name);
  const mergedCore = [...new Set([...existingCore, ...modalSymbols])].sort((a, b) =>
    a.localeCompare(b),
  );

  content = content.replace(CORE_IMPORT_RE, formatImport(mergedCore));

  for (const match of deprecatedMatches) {
    const symbols = parseImports(match[1]);
    const remaining = symbols.filter((s) => !MODAL_SYMBOLS.has(s.name));
    if (remaining.length === 0) {
      content = content.replace(match[0], "");
    } else {
      const lines = remaining.map((s) => `\t${s.raw}`);
      const replacement = `import {\n${lines.join(",\n")}\n} from '@patternfly/react-core/deprecated';\n`;
      content = content.replace(match[0], replacement);
    }
  }

  await writeFile(filePath, content);
  return true;
}

const files = await walk(SRC_DIR);
let fixed = 0;
for (const file of files) {
  if (await processFile(file)) {
    fixed++;
    console.log(`Fixed: ${path.relative(SRC_DIR, file)}`);
  }
}
console.log(`\nFixed ${fixed} files.`);
