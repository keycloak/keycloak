import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toRealmSettings } from "../../src/realm-settings/routes/RealmSettings.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import { selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  clickRowKebabItem,
  getRowByCellText,
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
      await expect(getRowByCellText(page, "search")).toBeVisible();

      await adminClient.addLocalizationText(
        "en",
        "search-refresh",
        "321",
        realmName,
      );
      await searchItem(page, "Search for translation", "321");
      await expect(getRowByCellText(page, "search-refresh")).toBeVisible();

      await searchItem(page, "Search for translation", "not-found");
      await expect(getRowByCellText(page, "search")).toHaveCount(0);
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

      const row = getRowByCellText(page, "bar");
      await expect(row).toBeVisible();
      await clickRowKebabItem(page, "bar", "Delete");
      const translationsUrl = `**/admin/realms/${realmName}/localization/en`;
      await page.route(translationsUrl, (route) =>
        route.fulfill({ status: 503 }),
      );
      const failedReload = page.waitForResponse(
        (response) =>
          response
            .url()
            .endsWith(`/admin/realms/${realmName}/localization/en`) &&
          response.status() === 503,
      );
      await confirmModal(page);
      await assertNotificationMessage(
        page,
        "Successfully removed translation(s).",
      );
      await failedReload;
      await expect(row).toHaveCount(0);
      await expect(getRowByCellText(page, "foo")).toBeVisible();
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

      const row = getRowByCellText(page, key);
      await editBundle(row, "discarded");
      await row.getByRole("button", { name: "Cancel editing button" }).click();
      await expect(row.getByText("123", { exact: true })).toBeVisible();

      await editBundle(row, "def");
      await clickConfirmEditButton(row);
      await assertNotificationMessage(page, "Success! Translation updated.");
      await expect(row.getByText("def", { exact: true })).toBeVisible();
    });

    test("Realm Overrides - Keep drafts for two rows independent", async ({
      page,
    }) => {
      const firstKey = "first-draft";
      const secondKey = "second-draft";
      await addBundle(page, firstKey, "First original");
      await clickCreateButton(page);
      await addBundle(page, secondKey, "Second original");
      await clickCreateButton(page);

      const firstRow = getRowByCellText(page, firstKey);
      const secondRow = getRowByCellText(page, secondKey);
      const firstInput = firstRow.getByRole("textbox", {
        name: "Edit translation value",
      });
      const secondInput = secondRow.getByRole("textbox", {
        name: "Edit translation value",
      });

      await editBundle(firstRow, "First edited");
      await editBundle(secondRow, "Second edited");
      await expect(firstInput).toHaveValue("First edited");

      await searchItem(page, "Search for translation", secondKey);
      await expect(firstRow).toHaveCount(0);
      await expect(secondInput).toHaveValue("Second edited");
      await clickConfirmEditButton(secondRow);
      await expect(
        secondRow.getByText("Second edited", { exact: true }),
      ).toBeVisible();

      await searchItem(page, "Search for translation", firstKey);
      await expect(firstInput).toHaveValue("First edited");
      await clickConfirmEditButton(firstRow);
      await expect(
        firstRow.getByText("First edited", { exact: true }),
      ).toBeVisible();

      await expect
        .poll(() => adminClient.getLocalizationTexts("en", realmName))
        .toMatchObject({
          [firstKey]: "First edited",
          [secondKey]: "Second edited",
        });
    });
  });
});

test("Preserves independent drafts through a failed translation save and retry", async ({
  page,
}) => {
  await using testBed = await createTestBed();
  const { realm } = testBed;
  const firstKey = "first-draft";
  const secondKey = "second-draft";
  const firstDraft = "First edited";
  const secondDraft = "Second edited";
  await adminClient.addLocalizationText(
    "en",
    firstKey,
    "First original",
    realm,
  );
  await adminClient.addLocalizationText(
    "en",
    secondKey,
    "Second original",
    realm,
  );
  await login(page, { to: toRealmSettings({ realm, tab: "localization" }) });
  await goToRealmOverridesSubTab(page);

  const firstRow = getRowByCellText(page, firstKey);
  const secondRow = getRowByCellText(page, secondKey);
  const firstInput = firstRow.getByRole("textbox", {
    name: "Edit translation value",
  });
  const secondInput = secondRow.getByRole("textbox", {
    name: "Edit translation value",
  });
  await editBundle(firstRow, firstDraft);
  await editBundle(secondRow, secondDraft);

  await page.route(
    `**/admin/realms/${realm}/localization/en/${firstKey}`,
    (route) => route.fulfill({ status: 503 }),
    { times: 1 },
  );

  await clickConfirmEditButton(firstRow);
  await assertNotificationMessage(page, /Error updating translation/);
  await expect(firstInput).toBeEnabled();
  await expect(firstInput).toHaveValue(firstDraft);
  await expect(secondInput).toBeEnabled();
  await expect(secondInput).toHaveValue(secondDraft);

  await clickConfirmEditButton(firstRow);
  await assertNotificationMessage(page, "Success! Translation updated.");
  await expect(firstRow.getByText(firstDraft, { exact: true })).toBeVisible();
  await expect(secondInput).toHaveValue(secondDraft);
  await expect
    .poll(() => adminClient.getLocalizationTexts("en", realm))
    .toMatchObject({
      [firstKey]: firstDraft,
      [secondKey]: "Second original",
    });
});

test("Keeps translation drafts and selection scoped to each locale", async ({
  page,
}) => {
  await using testBed = await createTestBed({
    internationalizationEnabled: true,
    supportedLocales: ["en", "da"],
    defaultLocale: "en",
  });
  const { realm } = testBed;
  const key = "shared-label";
  await adminClient.addLocalizationText("en", key, "English text", realm);
  await adminClient.addLocalizationText("da", key, "Danish text", realm);

  await login(page);
  await goToRealm(page, realm);
  await goToRealmSettings(page);
  await goToLocalizationTab(page);
  await goToRealmOverridesSubTab(page);

  const row = getRowByCellText(page, key);
  const localeSelect = page.locator(".kc-filter-by-locale-select");
  await editBundle(row, "English draft");
  await row.getByRole("checkbox").check();

  await selectItem(page, localeSelect, "Danish");
  await expect(row.getByText("Danish text", { exact: true })).toBeVisible();
  await expect(row.getByRole("checkbox")).not.toBeChecked();

  await selectItem(
    page,
    localeSelect,
    page.getByRole("option", { name: "English", exact: true }).first(),
  );
  await expect(row.getByRole("textbox")).toHaveValue("English draft");
  await expect(row.getByRole("checkbox")).not.toBeChecked();
});
