#!/usr/bin/env node
import { readFile } from "node:fs/promises";
import KcAdminClient from "keycloak-admin";

const consoleClientConfig = JSON.parse(
  await readFile(new URL("./security-admin-console-v2.json", import.meta.url))
);

const adminClient = new KcAdminClient.default({
  baseUrl: "http://localhost:8180/auth",
  realmName: "master",
});

await adminClient.auth({
  username: "admin",
  password: "admin",
  grantType: "password",
  clientId: "admin-cli",
});

const adminConsoleClient = await adminClient.clients.find({
  clientId: "security-admin-console-v2",
});

if (adminConsoleClient.length === 0) {
  adminClient.clients.create(consoleClientConfig);
}
