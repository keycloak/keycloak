import { expect, test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import { assertEmptyTable, clickTableRowItem } from "../utils/table.ts";
import { createDefaultTrustProvider } from "../identity-providers/main.ts";
import {
  assertAccessTokenSignatureAlgorithm,
  assertAdvancedSwitchesOn,
  assertBrowserFlowInput,
  assertOnExcludeSessionStateSwitch,
  assertTestClusterAvailability,
  assertTokenLifespanClientOfflineSessionMaxVisible,
  assertDirectGrantInput,
  assertRefreshTokenMaxReuse,
  assertRefreshTokenMaxReuseVisible,
  assertRevokeRefreshToken,
  fillRefreshTokenMaxReuse,
  selectRevokeRefreshToken,
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
  selectOid4vciAttesterTrustIdps,
  getOid4vciAttesterTrustIdpsValues,
  getOid4vciAttesterTrustIdpsSelect,
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

  test("Refresh token revocation override", async ({ page }) => {
    const inherited = "Inherits from realm settings (Disabled)";
    const getAttributes = async () =>
      (await adminClient.getClient(clientId))?.attributes ?? {};

    // Default: inherits from realm (master realm has revocation disabled), no max reuse field
    await assertRevokeRefreshToken(page, inherited);
    await assertRefreshTokenMaxReuseVisible(page, false);

    // Enable for the client: max reuse field appears, preset with the realm value
    await selectRevokeRefreshToken(page, "Enabled");
    await assertRefreshTokenMaxReuseVisible(page, true);
    await assertRefreshTokenMaxReuse(page, "0");
    await fillRefreshTokenMaxReuse(page, "2");
    await saveAdvanced(page);
    await assertNotificationMessage(page, "Client successfully updated");

    let attributes = await getAttributes();
    expect(attributes["revoke.refresh.token"]).toBe("true");
    expect(attributes["refresh.token.max.reuse"]).toBe("2");

    // Persisted values are loaded again after reload
    await page.reload();
    await goToAdvancedTab(page);
    await assertRevokeRefreshToken(page, "Enabled");
    await assertRefreshTokenMaxReuse(page, "2");

    // Disabling hides the max reuse field, revert restores the saved state
    await selectRevokeRefreshToken(page, "Disabled");
    await assertRefreshTokenMaxReuseVisible(page, false);
    await revertAdvanced(page);
    await assertRevokeRefreshToken(page, "Enabled");
    await assertRefreshTokenMaxReuse(page, "2");

    // Non-canonical but valid values set through the admin API are shown as the matching option
    await adminClient.updateClient(clientId, {
      attributes: { "revoke.refresh.token": " TRUE " },
    });
    await page.reload();
    await goToAdvancedTab(page);
    await assertRevokeRefreshToken(page, "Enabled");
    await assertRefreshTokenMaxReuseVisible(page, true);
    await assertRefreshTokenMaxReuse(page, "2");

    // Disable for the client and save: the max reuse override is cleared as well
    await selectRevokeRefreshToken(page, "Disabled");
    await assertRefreshTokenMaxReuseVisible(page, false);
    await saveAdvanced(page);
    await assertNotificationMessage(page, "Client successfully updated");
    attributes = await getAttributes();
    expect(attributes["revoke.refresh.token"]).toBe("false");
    expect(attributes["refresh.token.max.reuse"] ?? "").toBe("");

    // Back to inheriting from the realm
    await selectRevokeRefreshToken(page, inherited);
    await assertRefreshTokenMaxReuseVisible(page, false);
    await saveAdvanced(page);
    await assertNotificationMessage(page, "Client successfully updated");
    attributes = await getAttributes();
    expect(attributes["revoke.refresh.token"] ?? "").toBe("");
    expect(attributes["refresh.token.max.reuse"] ?? "").toBe("");
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
    await adminClient.createRealm(realmName, {
      verifiableCredentialsEnabled: true,
    });
    await adminClient.createClient({
      clientId: clientIdOpenIdConnect,
      realm: realmName,
      protocol: "openid-connect",
    });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  const getTestClient = async () => {
    return await adminClient.getClient(clientIdOpenIdConnect, realmName);
  };

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

    test("should hide OID4VC section when verifiable credentials are disabled for the realm", async ({
      page,
    }) => {
      await adminClient.updateRealm(realmName, {
        verifiableCredentialsEnabled: false,
      });

      await page.reload();
      await page.waitForSelector('[data-testid="advancedTab"]', {
        state: "visible",
        timeout: 10000,
      });
      await page.getByTestId("advancedTab").click();

      const toggleSwitch = page.locator("#attributes\\.oid4vci🍺enabled");
      await expect(toggleSwitch).toBeHidden();

      await adminClient.updateRealm(realmName, {
        verifiableCredentialsEnabled: true,
      });
    });

    test("should show OID4VCI Attester Trust IdPs field only when OID4VCI is enabled", async ({
      page,
    }) => {
      const attesterTrustIdpsSelect = getOid4vciAttesterTrustIdpsSelect(page);

      await switchOid4vciEnabled(page, true);
      await expect(attesterTrustIdpsSelect).toBeVisible();

      await switchOid4vciEnabled(page, false);
      await expect(attesterTrustIdpsSelect).toBeHidden();
    });

    test("should persist OID4VCI Attester Trust IdPs values", async ({
      page,
    }) => {
      const aliases = ["trust-idp-alias-1", "trust-idp-alias-2"];
      for (const alias of aliases) {
        const jwksUrl = `https://jwks.io/v1/${uuidv4()}`;
        await createDefaultTrustProvider(page, alias, jwksUrl);
      }

      await goToClients(page);
      await clickTableRowItem(page, clientIdOpenIdConnect);
      await goToAdvancedTab(page);

      await switchOid4vciEnabled(page, true);
      await selectOid4vciAttesterTrustIdps(page, aliases);
      await saveOid4vci(page);

      // Verify chips for the selected aliases are persisted
      expect(await getOid4vciAttesterTrustIdpsValues(page)).toEqual(aliases);
      const client = await getTestClient();
      expect(client?.attributes?.["oid4vci.attester_trust_idps"]).toBe(
        aliases.join(","),
      );

      // Change the value and test reverting
      await selectOid4vciAttesterTrustIdps(page, [aliases[0]]);
      await revertOid4vci(page);
      expect(await getOid4vciAttesterTrustIdpsValues(page)).toEqual(aliases);
    });
  });
});
