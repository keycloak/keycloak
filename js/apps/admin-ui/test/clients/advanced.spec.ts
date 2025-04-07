import { test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../utils/AdminClient";
import { login } from "../utils/login";
import { goToClients, goToRealm } from "../utils/sidebar";
import { assertEmptyTable, clickTableRowItem } from "../utils/table";
import {
  assertAccessTokenSignatureAlgorithm,
  assertAdvancedSwitchesOn,
  assertBrowserFlowInput,
  assertKeyForCodeExchangeInput,
  assertOnExcludeSessionStateSwitch,
  assertTestClusterAvailability,
  assertTokenLifespanClientOfflineSessionMaxVisible,
  assertDirectGrantInput,
  clickAdvancedSwitches,
  clickAllCompatibilitySwitch,
  deleteClusterNode,
  expandClusterNode,
  goToAdvancedTab,
  registerNodeManually,
  revertAdvanced,
  revertCompatibility,
  revertFineGrain,
  saveAdvanced,
  saveCompatibility,
  saveFineGrain,
  selectAccessTokenSignatureAlgorithm,
  selectBrowserFlowInput,
  selectDirectGrantInput,
  selectKeyForCodeExchangeInput,
  switchOffExcludeSessionStateSwitch,
  saveAuthFlowOverride,
  revertAuthFlowOverride,
} from "./advanced";

test.describe("Advanced tab test", () => {
  const clientId = `advanced-tab-${uuidv4()}`;

  test.beforeAll(() =>
    adminClient.createClient({ clientId, publicClient: true }),
  );

  test.afterAll(() => adminClient.deleteClient(clientId));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToClients(page);
    await clickTableRowItem(page, clientId);
    await goToAdvancedTab(page);
  });

  test("Clustering", async ({ page }) => {
    const host = "localhost";
    await expandClusterNode(page);
    await assertEmptyTable(page);
    await registerNodeManually(page, host);
    await assertTestClusterAvailability(page, true);
    await deleteClusterNode(page, host);
    await assertEmptyTable(page);
  });

  test("Fine grain OpenID connect configuration", async ({ page }) => {
    const algorithm = "ES384";
    await selectAccessTokenSignatureAlgorithm(page, algorithm);
    await saveFineGrain(page);
    await selectAccessTokenSignatureAlgorithm(page, "HS384");
    await revertFineGrain(page);
    await assertAccessTokenSignatureAlgorithm(page, algorithm);
  });

  test("OIDC Compatibility Modes configuration", async ({ page }) => {
    await clickAllCompatibilitySwitch(page);
    await saveCompatibility(page);
    await switchOffExcludeSessionStateSwitch(page);
    await revertCompatibility(page);
    await assertOnExcludeSessionStateSwitch(page);
  });

  test("Client Offline Session Max", async ({ page }) => {
    await assertTokenLifespanClientOfflineSessionMaxVisible(page, false);
  });

  test("Advanced settings", async ({ page }) => {
    await clickAdvancedSwitches(page);
    await selectKeyForCodeExchangeInput(page, "S256");
    await saveAdvanced(page);
    await assertAdvancedSwitchesOn(page);
    await assertKeyForCodeExchangeInput(page, "S256");
    await selectKeyForCodeExchangeInput(page, "plain");
    await assertKeyForCodeExchangeInput(page, "plain");
    await clickAdvancedSwitches(page, false);
    await revertAdvanced(page);
    await assertKeyForCodeExchangeInput(page, "S256");
    await assertAdvancedSwitchesOn(page);
  });

  test("Authentication flow override", async ({ page }) => {
    await selectBrowserFlowInput(page, "browser");
    await selectDirectGrantInput(page, "docker auth");
    await assertBrowserFlowInput(page, "browser");
    await assertDirectGrantInput(page, "docker auth");
    await revertAuthFlowOverride(page);
    await assertBrowserFlowInput(page, "Choose...");
    await assertDirectGrantInput(page, "Choose...");
    await selectBrowserFlowInput(page, "browser");
    await selectDirectGrantInput(page, "docker auth");
    await saveAuthFlowOverride(page);
    await selectBrowserFlowInput(page, "first broker login");
    await selectDirectGrantInput(page, "first broker login");
    await revertAuthFlowOverride(page);
  });
});

test.describe("Client Offline Session Max", () => {
  const realmName = `client-offline-session-${uuidv4()}`;
  const clientId = `clientId-${uuidv4()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, {
      offlineSessionMaxLifespanEnabled: true,
    });
    await adminClient.createClient({ clientId, realm: realmName });
  });
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
    await clickTableRowItem(page, clientId);
    await goToAdvancedTab(page);
  });

  test("Client Offline Session Max", async ({ page }) => {
    await assertTokenLifespanClientOfflineSessionMaxVisible(page, true);
  });
});
