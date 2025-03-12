import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { assertFieldError } from "../utils/form";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import {
  assertModalMessage,
  assertModalTitle,
  cancelModal,
  confirmModal,
} from "../utils/modal";
import { goToRealm, goToRealmSettings } from "../utils/sidebar";
import {
  assertEmptyTable,
  assertRowExists,
  clickTableRowItem,
  searchItem,
} from "../utils/table";
import {
  addClientRolesCondition,
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
  deleteClientRolesCondition,
  fillClientPolicyForm,
  fillClientRolesCondition,
  goBackToPolicies,
  goToClientPoliciesList,
  goToClientPoliciesTab,
  selectCondition,
  shouldCancelAddingCondition,
  shouldNotHaveConditionsConfigured,
  shouldReloadJSONPolicies,
} from "./client-policies";

test.describe("Realm settings client policies tab tests", () => {
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

    await createNewClientPolicyFromEmptyState(page, "New", "Test Description");
    await clickSaveClientPolicy(page);
    await assertNotificationMessage(page, "New policy created");
  });

  test.describe("Editing client policy", () => {
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
      await addClientRolesCondition(page, "manage-realm");
      await clickSaveConditionButton(page);
      await assertNotificationMessage(page, "Condition created successfully.");
      await assertExists(page, "client-roles-condition-link");
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
      // Add a new client-roles condition
      await clickAddCondition(page);
      await addClientRolesCondition(page, "client-roles");
      await clickSaveConditionButton(page);

      await deleteClientRolesCondition(page, "client-roles");
      await assertModalTitle(page, "Delete condition?");
      await assertModalMessage(
        page,
        "This action will permanently delete client-roles. This cannot be undone.",
      );
      await cancelModal(page);
      await assertExists(page, "client-roles-condition-link");

      await deleteClientRolesCondition(page, "client-roles");
      await confirmModal(page);
      await assertExists(page, "client-roles-condition-link", false);
    });

    test("Check deleting the client policy", async ({ page }) => {
      await goBackToPolicies(page);
      await goToClientPoliciesList(page);
      await deleteClientPolicyItemFromTable(page, "Test");
      await confirmModal(page);
      await assertNotificationMessage(page, "Client policy deleted");
      await assertEmptyTable(page);
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

    test("Check deleting newly created client policy from create view via dropdown", async ({
      page,
    }) => {
      await goBackToPolicies(page);
      await goToClientPoliciesList(page);

      await deleteClientPolicyFromDetails(page, "Test");
      await confirmModal(page);
      await assertNotificationMessage(page, "Client policy deleted");
      await assertEmptyTable(page);
    });
  });

  test("Check reloading JSON policies", async ({ page }) => {
    await shouldReloadJSONPolicies(page);
  });
});
