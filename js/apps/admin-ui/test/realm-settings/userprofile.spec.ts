import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toUserProfile } from "../../src/realm-settings/routes/UserProfile.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import { assertFieldError, selectItem, switchOn } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToRealm, goToRealmSettings, goToUsers } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  clickTableRowItem,
  getRowByCellText,
} from "../utils/table.ts";
import { goToLoginTab } from "./login.ts";
import {
  clickAddValidator,
  clickCancelAttribute,
  clickCreateUser,
  clickCreateAttribute,
  fillEmailAndOptionalUsername,
  clickSaveAttribute,
  clickSaveValidator,
  fillAttributeForm,
  goToAttributeGroupsTab,
  goToAttributesTab,
  goToUserProfileTab,
  switchOffIfOn,
} from "./userprofile.ts";

test("Reorders filtered attributes and preserves the positions of hidden attributes", async ({
  page,
}) => {
  await using testBed = await createTestBed();
  const { realm } = testBed;
  const group = "filtered-group";
  const otherGroup = "other-group";
  const first = "first-filtered";
  const second = "second-filtered";
  const before = "hidden-before";
  const between = "hidden-between";
  const after = "hidden-after";
  await adminClient.addUserProfile(realm, {
    attributes: [
      { name: before },
      { name: first, group },
      { name: between, group: otherGroup },
      { name: second, group },
      { name: after },
    ],
    groups: [{ name: group }, { name: otherGroup }],
  });
  await login(page, { to: toUserProfile({ realm, tab: "attributes" }) });

  const table = page.getByRole("grid", { name: "Draggable table" });
  const attributeNames = table.getByRole("link");
  await expect(attributeNames.nth(4)).toHaveText(after);
  const originalOrder = await attributeNames.allTextContents();

  await selectItem(
    page,
    page.getByRole("button", { name: "All groups" }),
    group,
  );
  await expect(attributeNames).toHaveText([first, second]);
  await getRowByCellText(page, first).dragTo(getRowByCellText(page, second));
  await assertNotificationMessage(
    page,
    "User Profile configuration has been saved",
  );
  await expect(attributeNames).toHaveText([second, first]);

  const { attributes } = await adminClient.getUserProfile(realm);
  expect(attributes?.map((attribute) => attribute.name)).toEqual([
    before,
    second,
    between,
    first,
    ...originalOrder.slice(4),
  ]);
});

test("Preserves the saved order when deleting an attribute after a delayed reorder", async ({
  page,
}) => {
  await using testBed = await createTestBed();
  const { realm } = testBed;
  const group = "filtered-group";
  const first = "first-filtered";
  const second = "second-filtered";
  const removed = "attribute-to-delete";
  const added = "attribute-added-during-reorder";
  await adminClient.addUserProfile(realm, {
    attributes: [
      { name: first, group },
      { name: second, group },
      { name: removed, group },
    ],
    groups: [{ name: group }],
  });
  await login(page, { to: toUserProfile({ realm, tab: "attributes" }) });
  await selectItem(
    page,
    page.getByRole("button", { name: "All groups" }),
    group,
  );

  const attributeNames = page
    .getByRole("grid", { name: "Draggable table" })
    .getByRole("link");
  await expect(attributeNames).toHaveText([first, second, removed]);

  const reorderSaved = Promise.withResolvers<boolean>();
  const releaseReorderResponse = Promise.withResolvers<void>();
  let holdReorderResponse = true;
  await page.route(`**/admin/realms/${realm}/users/profile`, async (route) => {
    if (holdReorderResponse && route.request().method() === "PUT") {
      holdReorderResponse = false;
      const response = await route.fetch();
      reorderSaved.resolve(response.ok());
      await releaseReorderResponse.promise;
      await route.fulfill({ response });
    } else {
      await route.continue();
    }
  });

  try {
    await getRowByCellText(page, first).dragTo(getRowByCellText(page, second));
    expect(await reorderSaved.promise).toBe(true);

    const rowActions = getRowByCellText(page, removed).getByRole("button", {
      name: "Kebab toggle",
    });
    await rowActions.click();
    await expect(page.getByRole("menuitem", { name: "Delete" })).toBeDisabled();
    await rowActions.click();

    // A separate save reaches the server before the reorder response arrives.
    await adminClient.addUserProfile(realm, {
      attributes: [{ name: added, group }],
    });

    releaseReorderResponse.resolve();
    await assertNotificationMessage(
      page,
      "User Profile configuration has been saved",
    );
    await expect(attributeNames).toHaveText([added, second, first, removed]);

    await clickRowKebabItem(page, removed, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, "Attribute deleted");
    await expect(attributeNames).toHaveText([added, second, first]);

    const { attributes } = await adminClient.getUserProfile(realm);
    expect(
      attributes
        ?.filter((attribute) => attribute.group === group)
        .map(({ name }) => name),
    ).toEqual([added, second, first]);
  } finally {
    releaseReorderResponse.resolve();
  }
});

test.describe.serial("User profile tabs", () => {
  const name = "Test";
  const displayName = "Test display name";
  const modifyName = "ModifyTest";
  const group = `realm-settings-group-${uuid()}`;

  const realmName = `sessions-realm-user-profile-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.addUserProfile(realmName, {
      attributes: [
        {
          name: modifyName,
          displayName,
        },
      ],
      groups: [
        {
          name: group,
        },
      ],
    });
  });
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToUserProfileTab(page);
  });

  test.afterEach(async () => {
    await adminClient.deleteUser("testuser7", realmName, true);
    await adminClient.deleteUser("testuser8", realmName, true);
    await adminClient.deleteUser("testuser8@gmail.com", realmName, true);
    await adminClient.deleteUser("testuser9@gmail.com", realmName, true);
    await adminClient.deleteUser("testuser10", realmName, true);
    await adminClient.deleteUser("testuser11", realmName, true);
  });

  test.describe.serial("Attributes sub tab tests", () => {
    test("Completes new attribute form and performs cancel", async ({
      page,
    }) => {
      const uniqueName = "UniqueName";
      await clickCreateAttribute(page);
      await fillAttributeForm(page, { name: uniqueName, displayName });
      await clickCancelAttribute(page);
      await assertRowExists(page, uniqueName, false);
      await assertRowExists(page, "firstName", true);
    });

    test("Completes new attribute form and performs submit", async ({
      page,
    }) => {
      await clickCreateAttribute(page);
      await fillAttributeForm(page, { name, displayName });
      await clickSaveAttribute(page);
      await assertNotificationMessage(
        page,
        "Success! User Profile configuration has been saved.",
      );
    });

    test("Modifies existing attribute and performs save", async ({ page }) => {
      const displayName = "Edited display name";
      await clickTableRowItem(page, modifyName);
      await fillAttributeForm(page, { displayName });
      await clickSaveAttribute(page);
      await assertNotificationMessage(
        page,
        "Success! User Profile configuration has been saved.",
      );
      await assertRowExists(page, displayName, true);
    });

    test("Adds and removes validator to/from existing attribute and performs save", async ({
      page,
    }) => {
      await clickTableRowItem(page, modifyName);
      await clickAddValidator(page);
      await selectItem(
        page,
        page.getByRole("button", { name: "Select an option" }),
        "email Email format validator",
      );
      await clickSaveValidator(page);
      await expect(
        page.locator('tbody [data-label="Validator name"]'),
      ).toContainText("email");

      await page.getByTestId("deleteValidator").click();
      await confirmModal(page);
      await expect(page.locator(".kc-emptyValidators")).toContainText(
        "No validators.",
      );
    });

    test("Keeps realm overrides when deleting an attribute without a translation key", async ({
      page,
    }) => {
      const attributeWithoutDisplayName = "NoTranslationKey";
      await adminClient.addLocalizationText(
        "en",
        "loginAccountTitle",
        "My Custom Login",
        realmName,
      );

      await clickCreateAttribute(page);
      await fillAttributeForm(page, { name: attributeWithoutDisplayName });
      await clickSaveAttribute(page);
      await assertNotificationMessage(
        page,
        "Success! User Profile configuration has been saved.",
      );

      await clickRowKebabItem(page, attributeWithoutDisplayName, "Delete");
      await confirmModal(page);
      await assertNotificationMessage(page, "Attribute deleted");

      expect(await adminClient.getLocalizationTexts("en", realmName)).toEqual({
        loginAccountTitle: "My Custom Login",
      });
    });
  });

  test("Deletes an attributes group", async ({ page }) => {
    await goToAttributeGroupsTab(page);
    await clickRowKebabItem(page, group, "Delete");
    await confirmModal(page);
    await assertRowExists(page, group, false);
  });

  test("Checks that not required attribute is not present when user is created with email as username and edit username set to disabled", async ({
    page,
  }) => {
    const attrName = "newAttribute1";

    await clickCreateAttribute(page);
    await fillAttributeForm(page, { name: attrName, displayName: attrName });
    await page.getByTestId("admin-edit").uncheck();
    await clickSaveAttribute(page);

    await goToRealmSettings(page);
    await goToLoginTab(page);
    await switchOn(page, "#kc-edit-username-switch");

    await goToUsers(page);
    await clickCreateUser(page);
    await expect(page.getByTestId(attrName)).toBeHidden();
    await page.getByTestId("username").fill("testuser7");
    await page.getByTestId("user-creation-save").click();
    await assertNotificationMessage(page, "The user has been created");
  });

  test("Checks that not required attribute is not present when user is created/edited with email as username enabled", async ({
    page,
  }) => {
    const attrName = "newAttribute2";

    await clickCreateAttribute(page);
    await fillAttributeForm(page, { name: attrName, displayName: attrName });
    await page.getByTestId("admin-edit").uncheck();
    await clickSaveAttribute(page);

    await goToRealmSettings(page);
    await goToLoginTab(page);
    await switchOn(page, "#kc-email-as-username-switch");

    await goToUsers(page);
    await clickCreateUser(page);
    await fillEmailAndOptionalUsername(
      page,
      "testuser8@gmail.com",
      "testuser8",
    );
    await expect(page.getByTestId(attrName)).toBeHidden();
    await page.getByTestId("user-creation-save").click();
    await assertNotificationMessage(page, "The user has been created");

    await page.getByTestId("email").fill(`testuser9@gmail.com`);
    await page.getByTestId("user-creation-save").click();
    await assertNotificationMessage(page, "The user has been saved");
  });

  test("Checks that not required attribute with permissions to view/edit is present when user is created", async ({
    page,
  }) => {
    const attrName = "newAttribute3";

    await clickCreateAttribute(page);
    await fillAttributeForm(page, { name: attrName, displayName: attrName });
    await page.getByTestId("user-edit").check();
    await page.getByTestId("user-view").check();
    await page.getByTestId("admin-view").check();
    await clickSaveAttribute(page);

    await goToRealmSettings(page);
    await goToLoginTab(page);
    await switchOffIfOn(page, "#kc-email-as-username-switch");

    await goToUsers(page);
    await clickCreateUser(page);
    await expect(page.getByTestId(attrName)).toBeVisible();
    await page.getByTestId("username").fill("testuser10");
    await page.getByTestId("user-creation-save").click();
    await assertNotificationMessage(page, "The user has been created");
    await expect(page.getByTestId(attrName)).toBeVisible();
  });

  test("Checks that required attribute with permissions to view/edit is present and required when user is created", async ({
    page,
  }) => {
    const attrName = "newAttribute4";

    await goToUserProfileTab(page);
    await goToAttributesTab(page);
    await clickCreateAttribute(page);
    await page.getByTestId("name").fill(attrName);
    await page.getByTestId("attributes-displayName").fill(attrName);
    await page.getByTestId("user-edit").check();
    await page.getByTestId("user-view").check();
    await page.getByTestId("admin-view").check();
    await switchOn(page, "#kc-required");
    await clickSaveAttribute(page);

    await goToUsers(page);
    await clickCreateUser(page);
    await expect(page.getByTestId(attrName)).toBeVisible();
    await page.getByTestId("username").fill("testuser11");
    await page.getByTestId("user-creation-save").click();
    await assertFieldError(page, attrName, `Please specify '${attrName}'.`);

    await page.getByTestId(attrName).fill("MyAttribute");
    await page.getByTestId("user-creation-save").click();
    await assertNotificationMessage(page, "The user has been created");
  });

  test("Checks that attribute group is visible when user with existing attribute is created", async ({
    page,
  }) => {
    const group = "personalInfo";

    await goToUserProfileTab(page);
    await goToAttributeGroupsTab(page);
    await page.getByTestId("create-attributes-groups-action").click();
    await page.getByTestId("name").fill(group);
    await page.getByTestId("attributes-displayHeader").fill(group);
    await page.getByTestId("saveGroupBtn").click();
    await goToAttributesTab(page);
    await clickTableRowItem(page, modifyName);
    await selectItem(page, "#group", group);
    await page.getByTestId("admin-edit").check();

    await clickSaveAttribute(page);
    await assertNotificationMessage(
      page,
      "Success! User Profile configuration has been saved.",
    );

    await goToUsers(page);
    await clickCreateUser(page);

    await expect(page.getByRole("heading", { name: group })).toBeVisible();
  });
});
