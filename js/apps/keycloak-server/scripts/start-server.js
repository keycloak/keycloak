#!/usr/bin/env node
import { Octokit } from "@octokit/rest";
import gunzip from "gunzip-maybe";
import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { pipeline } from "node:stream/promises";
import { fileURLToPath } from "node:url";
import { extract } from "tar-fs";
import { parseArgs } from "node:util";

const DIR_NAME = path.dirname(fileURLToPath(import.meta.url));
const SERVER_DIR = path.resolve(DIR_NAME, "../server");
const LOCAL_QUARKUS = path.resolve(DIR_NAME, "../../../../quarkus/dist/target");
const LOCAL_DIST_NAME = "keycloak-999.0.0-SNAPSHOT.tar.gz";
const SCRIPT_EXTENSION = process.platform === "win32" ? ".bat" : ".sh";
const ADMIN_USERNAME = "admin";
const ADMIN_PASSWORD = "admin";
const CLIENT_ID = "temporary-admin-service";
const CLIENT_SECRET = "temporary-admin-service";

const options = {
  local: {
    type: "boolean",
  },
  "account-dev": {
    type: "boolean",
  },
  "admin-dev": {
    type: "boolean",
  },
};

await startServer();

async function startServer() {
  let { scriptArgs, keycloakArgs } = handleArgs(process.argv.slice(2));

  await downloadServer(scriptArgs.local);

  const env = {
    KC_BOOTSTRAP_ADMIN_USERNAME: ADMIN_USERNAME,
    KC_BOOTSTRAP_ADMIN_PASSWORD: ADMIN_PASSWORD,
    KC_BOOTSTRAP_ADMIN_CLIENT_ID: CLIENT_ID,
    KC_BOOTSTRAP_ADMIN_CLIENT_SECRET: CLIENT_SECRET,
    ...process.env,
  };

  if (scriptArgs["account-dev"]) {
    env.KC_ACCOUNT_VITE_URL = "http://localhost:5173";
  }

  if (scriptArgs["admin-dev"]) {
    env.KC_ADMIN_VITE_URL = "http://localhost:5174";
  }

  console.info("Starting server…");

  const child = spawn(
    path.join(SERVER_DIR, `bin/kc${SCRIPT_EXTENSION}`),
    [
      "start-dev",
      `--features="login:v2,account:v3,admin-fine-grained-authz:v2,transient-users,oid4vc-vci,organization,declarative-ui,quick-theme,spiffe,kubernetes-service-accounts,workflows"`,
      ...keycloakArgs,
    ],
    {
      shell: true,
      env,
    },
  );

  child.stdout.pipe(process.stdout);
  child.stderr.pipe(process.stderr);
}

function handleArgs(args) {
  const { values, tokens } = parseArgs({
    args,
    options,
    strict: false,
    tokens: true,
  });
  // we need to remove the args that belong to the script so that we can pass the rest through to keycloak
  tokens
    .filter((token) => Object.hasOwn(options, token.name))
    .forEach((token) => {
      let tokenRaw = token.rawName;
      if (token.value) {
        tokenRaw += `=${token.value}`;
      }
      args.splice(args.indexOf(tokenRaw), 1);
    });
  return { scriptArgs: values, keycloakArgs: args };
}

async function downloadServer(local) {
  const directoryExists = fs.existsSync(SERVER_DIR);

  if (directoryExists) {
    console.info("Server installation found, skipping download.");
    return;
  }

  let assetStream;
  if (local) {
    console.info(`Looking for ${LOCAL_DIST_NAME} at ${LOCAL_QUARKUS}`);
    assetStream = fs.createReadStream(
      path.join(LOCAL_QUARKUS, LOCAL_DIST_NAME),
    );
  } else {
    console.info("Downloading and extracting server…");
    const nightlyAsset = await getNightlyAsset();
    assetStream = await getAssetAsStream(nightlyAsset);
  }
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
    ({ name }) => name === "keycloak-999.0.0-SNAPSHOT.tar.gz",
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
  return pipeline(stream, gunzip(), extract(path, options));
}
