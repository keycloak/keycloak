import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertFieldError } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import {
  assertModalMessage,
  assertModalTitle,
  cancelModal,
  confirmModal,
} from "../utils/modal.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertRowExists,
  clickTableRowItem,
  searchItem,
} from "../utils/table.ts";
import {
  addClientRolesCondition,
  addClientScopeCondition,
  addClientUpdaterSourceHost,
  assertExists,
  checkNewClientPolicyForm as assertNewClientPolicyForm,
  assertRoles,
  clickAddCondition,
  clickCancelClientPolicyCreation,
  clickSaveClientPolicy,
  clickSaveConditionButton,
  createNewClientPolicyFromEmptyState,
  deleteClientPolicyFromDetails,
  deleteClientPolicyItemFromTable,
  deleteCondition,
  fillClientPolicyForm,
  fillClientRolesCondition,
  goBackToPolicies,
  goToClientPoliciesList,
  goToClientPoliciesTab,
  selectCondition,
  shouldCancelAddingCondition,
  shouldNotHaveConditionsConfigured,
  shouldReloadJSONPolicies,
} from "./client-policies.ts";

test.describe.serial("Realm settings client policies tab tests", () => {
  const realmName = `realm-settings-client-policies_${uuid()}`;
  const placeHolder = "Search client policy";

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToClientPoliciesTab(page);
  });

  test("Complete new client form and cancel", async ({ page }) => {
    await goToClientPoliciesList(page);

    await createNewClientPolicyFromEmptyState(page, "Test", "Test Description");
    await assertNewClientPolicyForm(page);
    await clickCancelClientPolicyCreation(page);
    await assertEmptyTable(page);
  });

  test("Complete new client form and submit", async ({ page }) => {
    await goToClientPoliciesList(page);

    await createNewClientPolicyFromEmptyState(page, "New", "New Description");
    await clickSaveClientPolicy(page);
    await assertNotificationMessage(page, "New policy created");
  });

  test.describe.serial("Editing client policy", () => {
    test.beforeAll(() =>
      adminClient.createClientPolicy("Test", "Test Description", realmName),
    );

    test.afterAll(() => adminClient.deleteClientPolicy("Test", realmName));
    test.beforeEach(async ({ page }) => {
      await goToClientPoliciesList(page);
      await clickTableRowItem(page, "Test");
    });

    test("Should perform client profile search by profile name", async ({
      page,
    }) => {
      await page.getByRole("link", { name: "Client policies" }).click();
      await goToClientPoliciesList(page);
      await searchItem(page, placeHolder, "Test");
      await assertRowExists(page, "Test");
    });

    test("Should not have conditions configured by default", async ({
      page,
    }) => {
      await shouldNotHaveConditionsConfigured(page);
    });

    test("Should cancel adding a new condition to a client profile", async ({
      page,
    }) => {
      await clickAddCondition(page);
      await shouldCancelAddingCondition(page);
    });

    test("Should add a new client-roles condition to a client profile", async ({
      page,
    }) => {
      await clickAddCondition(page);
      await addClientScopeCondition(page);
      await clickSaveConditionButton(page);
      await assertNotificationMessage(page, "Condition created successfully.");
      await assertExists(page, "client-scopes-condition-link");
    });

    test("Should edit the client-roles condition of a client profile", async ({
      page,
    }) => {
      const role = "manage-realm";
      // Add a new client-roles condition
      await clickAddCondition(page);
      await addClientRolesCondition(page, role);
      await clickSaveConditionButton(page);

      // Edit the client-roles condition
      await selectCondition(page, "client-roles");
      await assertRoles(page, role);
      await fillClientRolesCondition(page, "admin");
      await clickSaveConditionButton(page);
      await assertNotificationMessage(page, "Condition updated successfully.");
    });

    test("Should cancel deleting condition from a client profile", async ({
      page,
    }) => {
      // Add a new condition
      const condition = "client-updater-source-host";
      const conditionLink = `${condition}-condition-link`;
      await clickAddCondition(page);
      await addClientUpdaterSourceHost(page);
      await clickSaveConditionButton(page);

      await deleteCondition(page, condition);
      await assertModalTitle(page, "Delete condition?");
      await assertModalMessage(
        page,
        `This action will permanently delete ${condition}. This cannot be undone.`,
      );
      await cancelModal(page);
      await assertExists(page, conditionLink);

      await deleteCondition(page, condition);
      await confirmModal(page);
      await assertExists(page, conditionLink, false);
    });

    test("Should not create duplicate client profile", async ({ page }) => {
      await goBackToPolicies(page);
      await goToClientPoliciesList(page);
      await page.getByTestId("createPolicy").click();
      await fillClientPolicyForm(page, "Test", "Test Again Description");
      await assertFieldError(
        page,
        "name",
        "The name must be unique within the realm",
      );
    });
  });

  test("Check reloading JSON policies", async ({ page }) => {
    await shouldReloadJSONPolicies(page);
  });

  test.describe.serial("Delete client policy", () => {
    const testPolicy = "DeletablePolicy";
    test.beforeEach(() =>
      adminClient.createClientPolicy(testPolicy, "Test Description", realmName),
    );
    test.beforeEach(async ({ page }) => {
      await goToClientPoliciesList(page);
    });

    test("Check deleting the client policy", async ({ page }) => {
      await deleteClientPolicyItemFromTable(page, testPolicy);
      await confirmModal(page);
      await assertNotificationMessage(page, "Client policy deleted");
      await assertRowExists(page, testPolicy, false);
    });

    test("Check deleting newly created client policy from create view via dropdown", async ({
      page,
    }) => {
      await deleteClientPolicyFromDetails(page, testPolicy);
      await confirmModal(page);
      await assertNotificationMessage(page, "Client policy deleted");
      await assertRowExists(page, testPolicy, false);
    });
  });
});
