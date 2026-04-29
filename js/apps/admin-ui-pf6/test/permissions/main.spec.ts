import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { clickSaveButton, selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToRealm } from "../utils/sidebar.ts";
import { assertRowExists } from "../utils/table.ts";
import {
  clickCreateNewPolicy,
  clickCreatePermission,
  clickCreatePolicySaveButton,
  clickSearchButton,
  deletePermission,
  fillPermissionForm,
  goToEvaluation,
  goToPermissions,
  openSearchPanel,
  pickGroup,
  removeGroup,
  selectResource,
} from "./main.ts";
import { fillPolicyForm, goToPolicies } from "./policy.ts";

test.describe.serial("Permissions section tests", () => {
  const realmName = `permissions-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { adminPermissionsEnabled: true });
    await adminClient.createUser({
      realm: realmName,
      username: "test-user",
      enabled: true,
    });
    await adminClient.createGroup("one", realmName);
    await adminClient.createGroup("two", realmName);
  });
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToPermissions(page);
  });

  test("should create permission", async ({ page }) => {
    await clickCreatePermission(page);
    await selectResource(page, "Users");
    await fillPermissionForm(page, {
      name: "test-permission",
      description: "test-description",
      scopes: ["view"],
    });
    await clickCreateNewPolicy(page);
    await fillPolicyForm(
      page,
      {
        name: "test-policy",
        description: "test-description",
        type: "User",
        user: "test-user",
      },
      true,
    );
    await clickCreatePolicySaveButton(page);
    await assertNotificationMessage(page, "Successfully created the policy");

    await expect(
      page.getByRole("gridcell", { name: "test-policy" }),
    ).toBeVisible();

    await clickSaveButton(page);
    await assertNotificationMessage(
      page,
      "Successfully created the permission",
    );

    await goToPermissions(page);
    await assertRowExists(page, "test-permission");
    await deletePermission(page, "test-permission");
    await goToPolicies(page);
    await assertRowExists(page, "test-policy");
  });

  test("should edit group permission", async ({ page }) => {
    await clickCreatePermission(page);
    await selectResource(page, "Groups");
    await fillPermissionForm(page, {
      name: "test-group-permission",
      scopes: ["view"],
      enforcementMode: "specificResources",
    });
    await pickGroup(page, "one");
    await pickGroup(page, "two");

    await clickCreateNewPolicy(page);
    await fillPolicyForm(
      page,
      {
        name: "test-policy2",
        description: "test-description",
        type: "User",
        user: "test-user",
      },
      true,
    );

    await clickCreatePolicySaveButton(page);
    await assertNotificationMessage(page, "Successfully created the policy");
    await clickSaveButton(page);
    await removeGroup(page, "one");
    await clickSaveButton(page);
    await assertNotificationMessage(
      page,
      "Successfully updated the permission",
    );
  });

  test.describe.serial("evaluate permissions", () => {
    test.beforeAll(async () => {
      await adminClient.createUser({
        realm: realmName,
        username: "other-user",
        enabled: true,
      });
      await adminClient.createUser({
        realm: realmName,
        username: "user1",
        enabled: true,
      });
      const { id } = await adminClient.createUserPolicy({
        realm: realmName,
        name: "other-policy",
        description: "other-description",
        type: "user",
        username: "other-user",
      });
      await adminClient.createPermission({
        realm: realmName,
        name: "client-permission",
        description: "",
        policies: [id!],
        resources: [],
        resourceType: "Clients",
        scopes: ["view"],
      });
    });

    test("should evaluate permissions success", async ({ page }) => {
      await goToEvaluation(page);
      await selectItem(page, page.getByTestId("user"), "other-user");
      await selectItem(page, "#resourceType", "Clients");
      await selectItem(page, "#clients", "account");
      await selectItem(page, "#authScopes", "view");
      await page.getByTestId("permission-eval").click();

      await expect(
        page.getByRole("heading", { name: "Success alert: account with" }),
      ).toBeVisible();
    });

    test("should evaluate permissions denied", async ({ page }) => {
      await goToEvaluation(page);
      await selectItem(page, page.getByTestId("user"), "user1");
      await selectItem(page, "#resourceType", "Clients");
      await selectItem(page, "#clients", "account");
      await selectItem(page, "#authScopes", "view");
      await page.getByTestId("permission-eval").click();

      await expect(
        page.getByRole("heading", { name: "Warning alert: account with" }),
      ).toBeVisible();
    });
  });

  test.describe.serial("permission search", () => {
    test.beforeAll(async () => {
      for (let i = 0; i < 5; i++) {
        await adminClient.createPermission({
          realm: realmName,
          name: `permission-${i}`,
          description: "",
          policies: [],
          resources: [],
          resourceType: i % 2 ? "Clients" : "Users",
          scopes: ["view"],
        });
      }
    });

    test("should search permission", async ({ page }) => {
      await openSearchPanel(page);
      await page.getByTestId("name").fill("permission-1");
      await clickSearchButton(page);

      await assertRowExists(page, "permission-1");
      await assertRowExists(page, "permission-2", false);
    });

    test("should search permission filter clients", async ({ page }) => {
      await openSearchPanel(page);
      await selectItem(page, "#resourceType", "Clients");
      await clickSearchButton(page);

      await assertRowExists(page, "permission-1");
      await assertRowExists(page, "permission-2", false);
      await assertRowExists(page, "permission-3");
    });
  });
});
