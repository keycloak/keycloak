#!/usr/bin/env node
import KcAdminClient from "@keycloak/keycloak-admin-client";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const DIR_NAME = path.dirname(fileURLToPath(import.meta.url));
const ADMIN_USERNAME = "admin";
const ADMIN_PASSWORD = "admin";

await importClient();

async function importClient() {
  const adminClient = new KcAdminClient({
    baseUrl: "http://127.0.0.1:8180",
    realmName: "master",
  });

  await adminClient.auth({
    username: ADMIN_USERNAME,
    password: ADMIN_PASSWORD,
    grantType: "password",
    clientId: "admin-cli",
  });

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
