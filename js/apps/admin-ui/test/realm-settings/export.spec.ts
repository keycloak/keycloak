import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { selectActionToggleItem } from "../utils/masthead.ts";
import { cancelModal, confirmModal } from "../utils/modal.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertDialogClosed,
  assertWarningMessage,
  toggleIncludeClients,
  toggleIncludeGroupsAndRoles,
} from "./export.ts";

test.describe.serial("Partial realm export", () => {
  const REALM_NAME = `partial-export-test-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(REALM_NAME);
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(REALM_NAME);
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, REALM_NAME);
    await goToRealmSettings(page);
    await selectActionToggleItem(page, "Partial export");
  });

  test("Closes the dialog", async ({ page }) => {
    await cancelModal(page);
    await assertDialogClosed(page);
  });

  test("Shows a warning message", async ({ page }) => {
    await assertWarningMessage(page);

    await toggleIncludeGroupsAndRoles(page);
    await assertWarningMessage(page, true);
    await toggleIncludeGroupsAndRoles(page, false);

    await toggleIncludeClients(page);
    await assertWarningMessage(page, true);
    await toggleIncludeClients(page, false);
    await assertWarningMessage(page);
  });

  test("Exports the realm", async ({ page }) => {
    await toggleIncludeGroupsAndRoles(page);
    await toggleIncludeClients(page);
    await confirmModal(page);

    const download = await page.waitForEvent("download");
    expect(download.suggestedFilename()).toBeDefined();
  });
});
