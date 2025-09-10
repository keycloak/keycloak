import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { chooseFile } from "../utils/file-chooser.ts";
import { selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { selectActionToggleItem } from "../utils/masthead.ts";
import { cancelModal, confirmModal } from "../utils/modal.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertClearButtonDisabled,
  assertClientVisible,
  assertGroupVisible,
  assertImportButtonDisabled,
  assertResultTable,
  assertTextAreaToHaveText,
  assertTextContent,
  assertUserVisible,
  clickClearButton,
  clickClearConfirmButton,
  closeModal,
  fillTextarea,
  toggleClients,
  toggleGroups,
  toggleUsers,
} from "./import.ts";

test.describe.serial("Partial import test", () => {
  const testRealm = `Partial-import-${uuid()}`;
  const testRealm2 = `Partial-import-2-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(testRealm);
    await adminClient.createRealm(testRealm2);
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(testRealm);
    await adminClient.deleteRealm(testRealm2);
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, testRealm);
    await goToRealmSettings(page);
    await selectActionToggleItem(page, "Partial import");
  });

  test("Opens and closes partial import dialog", async ({ page }) => {
    await expect(page.getByTestId("confirm")).toBeDisabled();
    await cancelModal(page);
    await expect(page.getByTestId("confirm")).toBeHidden();
  });

  test("Import button only enabled if JSON has something to import", async ({
    page,
  }) => {
    await fillTextarea(page, "{}");
    await assertImportButtonDisabled(page);
    await cancelModal(page);
  });

  test("Displays user options after multi-realm import", async ({ page }) => {
    await chooseFile(page, "../utils/files/multi-realm.json");

    await assertImportButtonDisabled(page);

    await toggleUsers(page);
    await assertImportButtonDisabled(page, false);

    await toggleGroups(page);
    await assertImportButtonDisabled(page, false);

    await toggleGroups(page, false);
    await toggleUsers(page, false);
    await assertImportButtonDisabled(page);

    await assertTextContent(page, "1 Users");
    await assertTextContent(page, "1 Groups");
    await assertTextContent(page, "1 Clients");
    await assertTextContent(page, "1 Identity providers");
    await assertTextContent(page, "2 Realm roles");
    await assertTextContent(page, "1 Client roles");

    await toggleGroups(page);
    await selectItem(page, page.locator("#realm-selector"), "realm2");
    await assertImportButtonDisabled(page);

    await assertTextContent(page, "2 Clients");

    await toggleClients(page);
    await assertImportButtonDisabled(page, false);
    await confirmModal(page);

    await assertTextContent(page, "2 records added");
    await assertResultTable(page, "customer-portal");
    await assertResultTable(page, "customer-portal2");
    await closeModal(page);
  });

  test("Displays user options after realm-less import and does the import", async ({
    page,
  }) => {
    await cancelModal(page);
    await goToRealm(page, testRealm2);
    await goToRealmSettings(page);
    await selectActionToggleItem(page, "Partial import");

    await chooseFile(page, "../utils/files/client-only.json");

    await expect(page.locator("select")).toBeHidden();

    await assertTextContent(page, "1 Clients");

    await assertClientVisible(page);
    await assertUserVisible(page, false);
    await assertGroupVisible(page, false);

    await toggleClients(page);
    await confirmModal(page);

    await assertTextContent(page, "One record added");
    await assertResultTable(page, "customer-portal3");
  });

  test("Should clear the input with the button", async ({ page }) => {
    await assertClearButtonDisabled(page);
    await assertTextAreaToHaveText(page, "");
    await fillTextarea(page, "{}");
    await assertTextAreaToHaveText(page, "{}");
    await assertClearButtonDisabled(page, false);
    await clickClearButton(page);
    await clickClearConfirmButton(page);
    await assertTextAreaToHaveText(page, "");
  });
});
