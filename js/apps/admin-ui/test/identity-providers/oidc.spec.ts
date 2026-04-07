import { test, expect } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { switchOn } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import {
  addMapper,
  assertAuthorizationUrl,
  assertFieldEditable,
  assertFieldReadOnly,
  assertInvalidUrlNotification,
  assertJwksUrlExists,
  assertOnMappingPage,
  assertPkceMethodExists,
  assertReloadButtonVisible,
  captureIdpSavePayload,
  clickCancelMapper,
  fillOIDCProviderForm,
  goToAddOidcProvider,
  toggleSyncField,
  clickReloadNow,
  clickRevertButton,
  clickSaveButton,
  clickSaveMapper,
  createOIDCProvider,
  enableKeepSynced,
  goToMappersTab,
  setUrl,
} from "./main.ts";

test.describe.serial("OIDC identity provider test", () => {
  const oidcProviderName = "oidc";
  const secret = "123";

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToIdentityProviders(page);
  });

  test.afterAll(() => adminClient.deleteIdentityProvider(oidcProviderName));

  test("should create an OIDC provider using discovery url", async ({
    page,
  }) => {
    await createOIDCProvider(page, oidcProviderName, secret);
    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );
    await assertAuthorizationUrl(page);

    await setUrl(page, "authorization", "invalid");
    await clickSaveButton(page);
    await assertInvalidUrlNotification(page, "authorization");
    await clickRevertButton(page);

    await setUrl(page, "token", "invalid");
    await clickSaveButton(page);
    await assertInvalidUrlNotification(page, "token");
    await clickRevertButton(page);

    await setUrl(page, "tokenIntrospection", "invalid");
    await clickSaveButton(page);
    await assertInvalidUrlNotification(page, "tokenIntrospection");
    await clickRevertButton(page);

    await assertJwksUrlExists(page);
    await page.getByText("Use JWKS URL").click();
    await assertJwksUrlExists(page, false);

    await assertPkceMethodExists(page, false);
    await switchOn(page, "#config\\.pkceEnabled");
    await assertPkceMethodExists(page);

    await clickSaveButton(page);
    await expect(page.getByText("Required field")).toBeVisible();

    await switchOn(page, "#config\\.useJwksUrl");
    await assertJwksUrlExists(page, true);
    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");
  });
});

test.describe.serial("Edit OIDC Provider", () => {
  const oidcProviderName = "OpenID Connect v1.0";
  const alias = `edit-oidc-${uuid()}`;

  test.beforeEach(async ({ page }) => {
    await adminClient.createIdentityProvider(oidcProviderName, alias);
    await login(page);
    await goToIdentityProviders(page);
    await clickTableRowItem(page, oidcProviderName);
  });

  test.afterEach(() => adminClient.deleteIdentityProvider(alias));

  test("should add OIDC mapper of type Attribute Importer", async ({
    page,
  }) => {
    await goToMappersTab(page);
    await addMapper(page, "oidc-user-attribute", "OIDC Attribute Importer");
    await clickSaveMapper(page);
    await assertNotificationMessage(page, "Mapper created successfully.");
  });

  test("should add OIDC mapper of type Claim To Role", async ({ page }) => {
    await goToMappersTab(page);
    await addMapper(page, "oidc-role", "OIDC Claim to Role");
    await clickSaveMapper(page);
    await assertNotificationMessage(page, "Mapper created successfully.");
  });

  test("should cancel the addition of the OIDC mapper", async ({ page }) => {
    await goToMappersTab(page);
    await addMapper(page, "oidc-role", "OIDC Claim to Role");
    await clickCancelMapper(page);
    await assertOnMappingPage(page);
  });
});

test.describe.serial("OIDC well-known sync UI", () => {
  const oidcProviderName = "OpenID Connect v1.0";
  const alias = `wellknown-sync-${uuid()}`;
  const secret = "test-secret";
  // All 7 syncable fields, ##-delimited
  const ALL_SYNC_FIELDS =
    "authorizationUrl##tokenUrl##logoutUrl##userInfoUrl##tokenIntrospectionUrl##issuer##jwksUrl";

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToIdentityProviders(page);
  });

  // Reset IDP sync config to a clean baseline before each test that needs it.
  // Skip silently if the IDP doesn't exist yet (i.e. before the creation test).
  async function resetSyncConfig() {
    try {
      await adminClient.updateIdentityProviderConfig(alias, {
        reloadEnabled: "false",
        includedWellKnownFields: ALL_SYNC_FIELDS,
      });
    } catch {
      // IDP not yet created - creation test will set it up
    }
  }

  test.afterAll(() => adminClient.deleteIdentityProvider(alias));

  test("create OIDC provider via UI with discovery URL", async ({ page }) => {
    await goToAddOidcProvider(page);
    await fillOIDCProviderForm(page, alias, oidcProviderName, alias, secret);
    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );
  });

  test("fields are editable before enabling Keep synced", async ({ page }) => {
    await resetSyncConfig();
    await clickTableRowItem(page, oidcProviderName);
    await assertFieldEditable(page, "authorizationUrl");
    await assertFieldEditable(page, "tokenUrl");
    await assertFieldEditable(page, "logoutUrl");
  });

  test("enabling Keep synced makes synced fields read-only", async ({
    page,
  }) => {
    await resetSyncConfig();
    await clickTableRowItem(page, oidcProviderName);
    await enableKeepSynced(page);
    await assertFieldReadOnly(page, "authorizationUrl");
    await assertFieldReadOnly(page, "tokenUrl");
    await assertFieldReadOnly(page, "logoutUrl");
    await assertReloadButtonVisible(page);
  });

  test("deselecting and re-selecting a field toggles its editability", async ({
    page,
  }) => {
    await resetSyncConfig();
    await clickTableRowItem(page, oidcProviderName);
    await enableKeepSynced(page);
    // Deselect: logoutUrl should become editable, others stay read-only
    await toggleSyncField(page, "logoutUrl");
    await assertFieldEditable(page, "logoutUrl");
    await assertFieldReadOnly(page, "authorizationUrl");
    // Re-select: logoutUrl should go read-only again
    await toggleSyncField(page, "logoutUrl");
    await assertFieldReadOnly(page, "logoutUrl");
  });

  test("saving with all fields synced sends full ## list in payload", async ({
    page,
  }) => {
    await resetSyncConfig();
    await clickTableRowItem(page, oidcProviderName);
    await enableKeepSynced(page);
    const payloadPromise = captureIdpSavePayload(page, alias);
    await clickSaveButton(page);
    const payload = await payloadPromise;
    const config = payload.config as Record<string, string>;
    expect(config.reloadEnabled).toBe("true");
    expect(config.includedWellKnownFields).toBe(ALL_SYNC_FIELDS);
  });

  test("saving with no fields synced sends empty includedWellKnownFields in payload", async ({
    page,
  }) => {
    await resetSyncConfig();
    await clickTableRowItem(page, oidcProviderName);
    await enableKeepSynced(page);
    for (const field of ALL_SYNC_FIELDS.split("##")) {
      await toggleSyncField(page, field);
    }
    const payloadPromise = captureIdpSavePayload(page, alias);
    await clickSaveButton(page);
    const payload = await payloadPromise;
    const config = payload.config as Record<string, string>;
    expect(config.reloadEnabled).toBe("true");
    // Empty or absent both mean "sync nothing"
    const syncedFields = (config.includedWellKnownFields ?? "")
      .split("##")
      .filter(Boolean);
    expect(syncedFields).toHaveLength(0);
  });

  test("saving with a subset of fields sends only those fields in payload", async ({
    page,
  }) => {
    await resetSyncConfig();
    await clickTableRowItem(page, oidcProviderName);
    await enableKeepSynced(page);
    // Deselect all except authorizationUrl and tokenUrl
    for (const field of [
      "logoutUrl",
      "userInfoUrl",
      "tokenIntrospectionUrl",
      "issuer",
      "jwksUrl",
    ]) {
      await toggleSyncField(page, field);
    }
    const payloadPromise = captureIdpSavePayload(page, alias);
    await clickSaveButton(page);
    const payload = await payloadPromise;
    const config = payload.config as Record<string, string>;
    expect(config.reloadEnabled).toBe("true");
    const syncedFields = config.includedWellKnownFields.split("##");
    expect(syncedFields).toContain("authorizationUrl");
    expect(syncedFields).toContain("tokenUrl");
    expect(syncedFields).not.toContain("logoutUrl");
    expect(syncedFields).not.toContain("issuer");
  });

  test("Reload now button calls backend and updates timestamps", async ({
    page,
  }) => {
    await resetSyncConfig();
    await clickTableRowItem(page, oidcProviderName);
    await enableKeepSynced(page);
    await assertReloadButtonVisible(page);
    await clickReloadNow(page);
    await expect(page.getByTestId("well-known-last-sync")).not.toHaveText(
      "Never synced",
    );
  });
});
