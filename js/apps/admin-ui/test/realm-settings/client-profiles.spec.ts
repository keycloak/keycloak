import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import {
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertExecutorInList,
  assertIntentClient,
  cancelClientProfileCreation,
  checkElementNotInList,
  clickAddExecutor,
  clickDeleteExecutor,
  clickSaveClientProfile,
  createClientProfile,
  goToClientPoliciesTab,
  goToClientProfilesList,
  saveClientProfileCreation,
  searchClientProfile,
  searchNonExistingClientProfile,
  selectExecutorType,
} from "./client-profiles.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { confirmModal } from "../utils/modal.ts";

test.describe.serial("Realm settings client profiles tab tests", () => {
  const profileName = "Test";
  const realmName = `client-profiles-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToClientPoliciesTab(page);
    await goToClientProfilesList(page);
  });

  test("Complete new client form and cancel", async ({ page }) => {
    await createClientProfile(page, profileName, "Test Description");
    await cancelClientProfileCreation(page);
    await checkElementNotInList(page, profileName);
  });

  test("Complete new client form and submit", async ({ page }) => {
    await createClientProfile(page, profileName, "Test Description");
    await saveClientProfileCreation(page);
    await assertNotificationMessage(page, "New client profile created");
  });

  test("Should perform client profile search by profile name", async ({
    page,
  }) => {
    await searchClientProfile(page, "fapi-ciba");
  });

  test("Should search non-existent client profile", async ({ page }) => {
    await searchNonExistingClientProfile(page, "nonExistentProfile");
  });

  test.describe.serial("Edit client profile", () => {
    const editedProfileName = "Edit";
    test.beforeAll(() =>
      adminClient.createClientProfile(
        editedProfileName,
        "Edit Description",
        realmName,
      ),
    );

    test.beforeEach(async ({ page }) => {
      await searchClientProfile(page, editedProfileName);
      await clickTableRowItem(page, editedProfileName);
    });

    test("Should add executor to client profile", async ({ page }) => {
      const executorType = "intent-client-bind-checker";
      await clickAddExecutor(page);
      await selectExecutorType(page, executorType);
      await assertIntentClient(
        page,
        "https://rs.keycloak-fapi.org/check-intent-client-bound",
      );

      await clickSaveClientProfile(page);
      await assertNotificationMessage(
        page,
        "Success! Executor created successfully",
      );

      await assertExecutorInList(page, executorType);
    });

    test("Should delete executor from a client profile", async ({ page }) => {
      // Create a new executor
      const executorType = "confidential-client";
      await clickAddExecutor(page);
      await selectExecutorType(page, executorType);
      await clickSaveClientProfile(page);

      // Delete the executor
      await clickDeleteExecutor(page, executorType);
      await assertNotificationMessage(
        page,
        "Success! The executor was deleted.",
      );
      await checkElementNotInList(page, executorType);
    });

    test("Check deleting the client profile", async ({ page }) => {
      await selectActionToggleItem(page, "Delete this client profile");
      await confirmModal(page);
      await assertNotificationMessage(page, "Client profile deleted");
    });
  });
});
