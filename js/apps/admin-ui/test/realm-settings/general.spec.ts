import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { switchOff, switchOn } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToClients, goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertDisplayName,
  assertFrontendURL,
  assertRequireSSL,
  clickRevertButton,
  clickSaveRealm,
  fillDisplayName,
  fillFrontendURL,
  fillRequireSSL,
} from "./general.ts";
import { SERVER_URL } from "../utils/constants.ts";

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
    const locator = page.getByRole("link", {
      name: "OpenID Endpoint Configuration",
    });

    await expect(locator).toHaveAttribute(
      "href",
      `${SERVER_URL}/realms/${realmName}/.well-known/openid-configuration`,
    );
    await expect(locator).toHaveAttribute("target", "_blank");
    await expect(locator).toHaveAttribute("rel", "noreferrer noopener");

    const link = await page
      .getByRole("link", { name: "OpenID Endpoint Configuration" })
      .getAttribute("href");
    const response = await page.request.get(link!);
    expect(response.status()).toBe(200);
  });

  test("all general tab switches", async ({ page }) => {
    await switchOn(page, "#userManagedAccessAllowed");
    await clickSaveRealm(page);
    await assertNotificationMessage(page, "Realm successfully updated");

    await switchOff(page, "#userManagedAccessAllowed");
    await clickSaveRealm(page);
    await assertNotificationMessage(page, "Realm successfully updated");
  });

  test("realm enable/disable switch", async ({ page }) => {
    // Enable realm
    await switchOn(page, `#${realmName}-switch`);
    await assertNotificationMessage(page, "Realm successfully updated");

    // Disable realm
    await switchOff(page, `#${realmName}-switch`);
    await confirmModal(page);
    await assertNotificationMessage(page, "Realm successfully updated");
  });

  test("Fail to set Realm ID to empty", async ({ page }) => {
    await page.getByRole("textbox", { name: "Copyable input" }).fill("");
    await clickSaveRealm(page);
    await expect(page.getByText("Required field")).toBeVisible();
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
