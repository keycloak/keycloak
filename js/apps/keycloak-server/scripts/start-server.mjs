#!/usr/bin/env node
// @ts-check
import { Octokit } from "@octokit/rest";
import gunzip from "gunzip-maybe";
import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { pipeline } from "node:stream/promises";
import { fileURLToPath } from "node:url";
import tar from "tar-fs";

const DIR_NAME = path.dirname(fileURLToPath(import.meta.url));
const SERVER_DIR = path.resolve(DIR_NAME, "../server");
const SCRIPT_EXTENSION = process.platform === "win32" ? ".bat" : ".sh";

await startServer();

async function startServer() {
  await downloadServer();

  console.info("Starting server…");

  const args = process.argv.slice(2);
  const child = spawn(
    path.join(SERVER_DIR, `bin/kc${SCRIPT_EXTENSION}`),
    [
      "start-dev",
      "--http-port=8180",
      "--features=admin2,admin-fine-grained-authz,declarative-user-profile",
      ...args,
    ],
    {
      env: {
        KEYCLOAK_ADMIN: "admin",
        KEYCLOAK_ADMIN_PASSWORD: "admin",
        ...process.env,
      },
    }
  );

  child.stdout.pipe(process.stdout);
  child.stderr.pipe(process.stderr);
}

async function downloadServer() {
  const directoryExists = fs.existsSync(SERVER_DIR);

  if (directoryExists) {
    console.info("Server installation found, skipping download.");
    return;
  }

  console.info("Downloading and extracting server…");

  const nightlyAsset = await getNightlyAsset();
  const assetStream = await getAssetAsStream(nightlyAsset);

  await extractTarball(assetStream, SERVER_DIR, { strip: 1 });
}

async function getNightlyAsset() {
  const api = new Octokit();
  const release = await api.repos.getReleaseByTag({
    owner: "keycloak",
    repo: "keycloak",
    tag: "nightly",
  });

  return release.data.assets.find(
    ({ name }) => name === "keycloak-999.0.0-SNAPSHOT.tar.gz"
  );
}

async function getAssetAsStream(asset) {
  const response = await fetch(asset.browser_download_url);

  if (!response.ok) {
    throw new Error("Something went wrong requesting the nightly release.");
  }

  return response.body;
}

function extractTarball(stream, path, options) {
  return pipeline(stream, gunzip(), tar.extract(path, options));
}
