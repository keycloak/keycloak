import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToClients, goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertDisplayName,
  assertFrontendURL,
  assertOpenIdEndpointConfigurationLink,
  assertOpenIdEndpointConfigurationReachable,
  assertRequiredFieldMessage,
  assertRequireSSL,
  clearRealmId,
  clickRevertButton,
  clickSaveRealm,
  disableRealm,
  disableUserManagedAccess,
  enableRealm,
  enableUserManagedAccess,
  fillDisplayName,
  fillFrontendURL,
  fillRequireSSL,
} from "./general.ts";

test.describe.serial("Realm settings general tab tests", () => {
  const realmName = `general-realm-settings-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
  });

  test("Check Access Endpoints OpenID Endpoint Configuration link", async ({
    page,
  }) => {
    await assertOpenIdEndpointConfigurationLink(page, realmName);
    await assertOpenIdEndpointConfigurationReachable(page);
  });

  test("all general tab switches", async ({ page }) => {
    await enableUserManagedAccess(page);
    await clickSaveRealm(page);
    await assertNotificationMessage(page, "Realm successfully updated");

    await disableUserManagedAccess(page);
    await clickSaveRealm(page);
    await assertNotificationMessage(page, "Realm successfully updated");
  });

  test("realm enable/disable switch", async ({ page }) => {
    await enableRealm(page, realmName);
    await assertNotificationMessage(page, "Realm successfully updated");

    await disableRealm(page, realmName);
    await assertNotificationMessage(page, "Realm successfully updated");
  });

  test("Fail to set Realm ID to empty", async ({ page }) => {
    await clearRealmId(page);
    await clickSaveRealm(page);
    await assertRequiredFieldMessage(page);
  });

  test("Modify Display name", async ({ page }) => {
    const name = "display_name";
    await fillDisplayName(page, name);
    await clickSaveRealm(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await goToClients(page);
    await goToRealmSettings(page);
    await assertDisplayName(page, name);
  });

  test("Modify front end URL", async ({ page }) => {
    const frontendUrl = "www.example.com";
    await goToRealmSettings(page);
    await fillFrontendURL(page, frontendUrl);

    await clickSaveRealm(page);
    await assertNotificationMessage(page, "Realm successfully updated");

    await goToClients(page);
    await goToRealmSettings(page);
    await assertFrontendURL(page, frontendUrl);
  });

  test("Select SSL all requests", async ({ page }) => {
    await goToRealmSettings(page);
    await fillRequireSSL(page, "All requests");
    await clickSaveRealm(page);
    await assertNotificationMessage(page, "Realm successfully updated");

    await goToClients(page);
    await goToRealmSettings(page);
    await assertRequireSSL(page, "All requests");
  });

  test("Verify 'Revert' button works", async ({ page }) => {
    await fillDisplayName(page, "display_name");
    await clickSaveRealm(page);

    await fillDisplayName(page, "should_be_reverted");
    await clickRevertButton(page);
    await assertDisplayName(page, "display_name");
  });
});
