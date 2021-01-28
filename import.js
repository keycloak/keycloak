#!/usr/bin/env node
const KcAdminClient = require('keycloak-admin').default;
const consoleClientConfig = require("./security-admin-console-v2.json");


const adminClient = new KcAdminClient({
  baseUrl: "http://localhost:8180/auth",
  realmName: 'master',
});

(async () => {
  await adminClient.auth({
    username: 'admin',
    password: 'admin',
    grantType: 'password',
    clientId: 'admin-cli',
  });

  const adminConsoleClient = await adminClient.clients.find({clientId: "security-admin-console-v2"});
  if (adminConsoleClient.length === 0) {
    adminClient.clients.create(consoleClientConfig);
  }
})();