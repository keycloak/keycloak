#!/usr/bin/env node
import KcAdminClient from "@keycloak/keycloak-admin-client";
import { Octokit } from "@octokit/rest";
import gunzip from "gunzip-maybe";
import { spawn } from "node:child_process";
import fs from "node:fs";
import { readFile } from "node:fs/promises";
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
const AUTH_DELAY = 10000;
const AUTH_RETRY_LIMIT = 3;

const options = {
  local: {
    type: "boolean",
  },
};

await startServer();

async function startServer() {
  let { scriptArgs, keycloakArgs } = handleArgs(process.argv.slice(2));

  await downloadServer(scriptArgs.local);

  console.info("Starting server…");
  const child = spawn(
    path.join(SERVER_DIR, `bin/kc${SCRIPT_EXTENSION}`),
    [
      "start-dev",
      "--http-port=8180",
      `--features="account3,admin-fine-grained-authz,transient-users"`,
      ...keycloakArgs,
    ],
    {
      shell: true,
      env: {
        KEYCLOAK_ADMIN: ADMIN_USERNAME,
        KEYCLOAK_ADMIN_PASSWORD: ADMIN_PASSWORD,
        ...process.env,
      },
    },
  );

  child.stdout.pipe(process.stdout);
  child.stderr.pipe(process.stderr);

  await wait(AUTH_DELAY);
  await importClient();
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

async function importClient() {
  const adminClient = new KcAdminClient({
    baseUrl: "http://127.0.0.1:8180",
    realmName: "master",
  });

  await authenticateAdminClient(adminClient);

  console.info("Checking if client already exists…");

  const adminConsoleClient = await adminClient.clients.find({
    clientId: "security-admin-console-v2",
  });

  if (adminConsoleClient.length > 0) {
    console.info("Client already exists, skipping import.");
    return;
  }

  console.info("Importing client…");

  const configPath = path.join(DIR_NAME, "security-admin-console-v2.json");
  const config = JSON.parse(await readFile(configPath, "utf-8"));

  await adminClient.clients.create(config);

  console.info("Client imported successfully.");
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

async function authenticateAdminClient(
  adminClient,
  numRetries = AUTH_RETRY_LIMIT,
) {
  console.log("Authenticating admin client…");

  try {
    await adminClient.auth({
      username: ADMIN_USERNAME,
      password: ADMIN_PASSWORD,
      grantType: "password",
      clientId: "admin-cli",
    });
  } catch (error) {
    if (numRetries === 0) {
      throw error;
    }

    console.info(
      `Authentication failed, retrying in ${AUTH_DELAY / 1000} seconds.`,
    );

    await wait(AUTH_DELAY);
    await authenticateAdminClient(adminClient, numRetries - 1);
  }

  console.log("Admin client authenticated successfully.");
}

async function wait(delay) {
  return new Promise((resolve) => setTimeout(() => resolve(), delay));
}
