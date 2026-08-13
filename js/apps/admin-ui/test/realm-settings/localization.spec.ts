import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  searchItem,
} from "../utils/table.ts";
import {
  addBundle,
  clickConfirmEditButton,
  clickCreateButton,
  clicksSaveLocalization,
  editBundle,
  goToLocalizationTab,
  goToRealmOverridesSubTab,
  selectLocale,
  switchInternationalization,
} from "./localization.ts";

test.describe.serial("Go to localization tab", () => {
  const realmName = `localization-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToLocalizationTab(page);
  });

  test("Locales tab - Add locale", async ({ page }) => {
    await switchInternationalization(page);
    await selectLocale(page, "Danish");

    await clicksSaveLocalization(page);
    await assertNotificationMessage(page, "Realm successfully updated");
  });

  test.describe.serial("Locales tab - CRUD bundle", () => {
    test.beforeAll(() =>
      adminClient.updateRealm(realmName, { internationalizationEnabled: true }),
    );

    test.beforeEach(async ({ page }) => {
      await goToRealmOverridesSubTab(page);
    });

    test("Realm Overrides - Search function", async ({ page }) => {
      await addBundle(page, "search", "321");
      await clickCreateButton(page);

      await searchItem(page, "Search for translation", "321");
      await assertRowExists(page, "search");

      await searchItem(page, "Search for translation", "not-found");
      await assertRowExists(page, "not-found", false);
    });

    test("Realm Overrides - Add and delete bundle", async ({ page }) => {
      await addBundle(page, "bar", "123");
      await clickCreateButton(page);
      await addBundle(page, "foo", "abc");
      await clickCreateButton(page);

      await assertNotificationMessage(
        page,
        "Success! The translation has been added.",
      );

      await assertRowExists(page, "bar");
      await clickRowKebabItem(page, "bar", "Delete");
      await confirmModal(page);
      await assertNotificationMessage(
        page,
        "Successfully removed translation(s).",
      );
    });

    test("Realm Overrides - Edit and cancel edit message bundle", async ({
      page,
    }) => {
      await goToRealmOverridesSubTab(page);

      const key = "edit";
      await addBundle(page, key, "123");
      await clickCreateButton(page);
      await addBundle(page, "foo", "abc");
      await clickCreateButton(page);

      await assertNotificationMessage(
        page,
        "Success! The translation has been added.",
      );

      await editBundle(page, 0, "def");
      await clickConfirmEditButton(page, 0);
      await assertNotificationMessage(page, "Success! Translation updated.");
      await assertRowExists(page, "def");
    });
  });
});
