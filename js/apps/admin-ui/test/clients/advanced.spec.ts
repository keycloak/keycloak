import { expect, test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import { assertEmptyTable, clickTableRowItem } from "../utils/table.ts";
import {
  assertAccessTokenSignatureAlgorithm,
  assertAdvancedSwitchesOn,
  assertBrowserFlowInput,
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
  switchOffExcludeSessionStateSwitch,
  saveAuthFlowOverride,
  revertAuthFlowOverride,
  saveOid4vci,
  revertOid4vci,
  assertOid4vciEnabled,
  switchOid4vciEnabled,
} from "./advanced.ts";

test.describe.serial("Advanced tab test", () => {
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
    await saveAdvanced(page);
    await assertAdvancedSwitchesOn(page);
    await clickAdvancedSwitches(page, false);
    await revertAdvanced(page);
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

test.describe.serial("Client Offline Session Max", () => {
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

test.describe.serial("OpenID for Verifiable Credentials", () => {
  const realmName = `oid4vci-test-${uuidv4()}`;
  const clientIdOpenIdConnect = `client-oidc-${uuidv4()}`;
  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, {});
    await adminClient.createClient({
      clientId: clientIdOpenIdConnect,
      realm: realmName,
      protocol: "openid-connect",
    });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.describe.serial("with protocol openid-connect", () => {
    test.beforeEach(async ({ page }) => {
      await login(page);
      await goToRealm(page, realmName);
      await goToClients(page);
      await clickTableRowItem(page, clientIdOpenIdConnect);

      await page.waitForSelector('[data-testid="advancedTab"]', {
        state: "visible",
        timeout: 10000,
      });
      await page.getByTestId("advancedTab").click();
    });

    test("should handle OID4VC section visibility based on feature flag", async ({
      page,
    }) => {
      const toggleSwitch = page.locator("#attributes\\.oid4vci🍺enabled");

      const isVisible = await toggleSwitch.isVisible();

      if (isVisible) {
        await toggleSwitch.scrollIntoViewIfNeeded();
        await assertOid4vciEnabled(page, false);
        await switchOid4vciEnabled(page, true);
        await saveOid4vci(page);
        await assertOid4vciEnabled(page, true);
        await switchOid4vciEnabled(page, false);
        await assertOid4vciEnabled(page, false);
        await revertOid4vci(page);
        await assertOid4vciEnabled(page, true);
      } else {
        await expect(toggleSwitch).toBeHidden();
      }
    });
  });
});
