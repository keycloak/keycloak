import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertSaveButtonIsDisabled, clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import {
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToOrganizations, goToRealm } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  clickTableRowItem,
} from "../utils/table.ts";
import {
  fillCreatePage,
  fillNameField,
  getNameField,
  goToCreate,
} from "./main.ts";

test.describe.serial("Organization CRUD", () => {
  const realmName = `organization-${uuid()}`;

  test.beforeAll(() =>
    adminClient.createRealm(realmName, { organizationsEnabled: true }),
  );
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToOrganizations(page);
  });

  test("should create new organization", async ({ page }) => {
    await goToCreate(page);
    await assertSaveButtonIsDisabled(page);
    await fillCreatePage(page, { name: "orgName" });
    await fillCreatePage(page, {
      name: "orgName",
      domain: ["ame.org", "test.nl"],
      description: "some description",
    });
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Organization successfully saved.");
  });

  test.describe.serial("Existing organization", () => {
    const orgName = `org-edit-${uuid()}`;
    const delOrgName = `org-del-${uuid()}`;
    const delOrgName2 = `org-del-${uuid()}`;

    test.beforeAll(async () => {
      await adminClient.createOrganization({
        realm: realmName,
        name: orgName,
        domains: [{ name: orgName, verified: false }],
      });
      await adminClient.createOrganization({
        realm: realmName,
        name: delOrgName,
        domains: [{ name: delOrgName, verified: false }],
      });
      await adminClient.createOrganization({
        realm: realmName,
        name: delOrgName2,
        domains: [{ name: delOrgName2, verified: false }],
      });
    });

    test.afterAll(async () => {
      await adminClient.deleteOrganization(orgName, realmName);
    });

    test("should modify existing organization", async ({ page }) => {
      await clickTableRowItem(page, orgName);

      // This waits for the field to be filled before we clear and fill it with a new value
      await expect(getNameField(page)).toHaveValue(orgName);

      const newValue = "newName";
      await fillNameField(page, newValue);
      await expect(getNameField(page)).toHaveValue(newValue);
      await clickSaveButton(page);
      await assertNotificationMessage(page, "Organization successfully saved.");
      await goToOrganizations(page);
      await assertRowExists(page, newValue);
    });

    test("should delete from list", async ({ page }) => {
      await clickRowKebabItem(page, delOrgName, "Delete");
      await confirmModal(page);
      await assertNotificationMessage(
        page,
        "The organization has been deleted",
      );
    });

    test("should delete from details page", async ({ page }) => {
      await clickTableRowItem(page, delOrgName2);
      await selectActionToggleItem(page, "Delete");
      await confirmModal(page);
      await assertNotificationMessage(
        page,
        "The organization has been deleted",
      );
    });
  });
});
