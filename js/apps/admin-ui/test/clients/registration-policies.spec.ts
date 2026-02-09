import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { clickCancelButton, clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import {
  assertAxeViolations,
  assertNotificationMessage,
} from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  clickTableRowItem,
  getTableData,
} from "../utils/table.ts";
import {
  clickCreateAnonymousPolicy,
  clickCreateAuthenticatedPolicy,
  createPolicy,
  fillPolicyForm,
  goToAuthenticatedSubTab,
  goToClientRegistrationTab,
} from "./registration-policies.ts";

test.describe.serial("Client registration policies tab", () => {
  const tabName = "Client registration";
  const realmName = `clients-details-realm-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
    await goToClientRegistrationTab(page);
  });

  test.describe.serial("Anonymous client policies subtab", () => {
    const policyName = "newAnonymPolicy1";
    const policyNameUpdated = "policy2";

    test("check anonymous clients list is not empty", async ({ page }) => {
      const rows = await getTableData(page, tabName);
      expect(rows.length).toBeGreaterThan(1);
    });

    test("add anonymous client registration policy", async ({ page }) => {
      await clickCreateAnonymousPolicy(page);
      await createPolicy(page, "max-clients", { name: policyName });
      await clickSaveButton(page);

      await assertNotificationMessage(
        page,
        "New client policy created successfully",
      );
      await clickCancelButton(page);

      await assertRowExists(page, policyName);
    });

    test("edit anonymous client registration policy", async ({ page }) => {
      await clickTableRowItem(page, "Consent Required");
      await fillPolicyForm(page, { name: policyNameUpdated });
      await clickSaveButton(page);

      await assertNotificationMessage(
        page,
        "Client policy updated successfully",
      );
      await clickCancelButton(page);

      await assertRowExists(page, policyNameUpdated);
    });

    test("delete anonymous client registration policy", async ({ page }) => {
      await clickRowKebabItem(page, "Full Scope Disabled", "Delete");
      await confirmModal(page);

      await assertNotificationMessage(
        page,
        "Client registration policy deleted successfully",
      );
    });
  });

  test.describe.serial("Authenticated client policies subtab", () => {
    const policyName = "newAuthPolicy1";
    const policyNameUpdated = "policy3";

    test.beforeEach(async ({ page }) => {
      await goToAuthenticatedSubTab(page);
    });

    test("check authenticated clients list is not empty", async ({ page }) => {
      const rows = await getTableData(page, tabName);
      expect(rows.length).toBeGreaterThan(1);
    });

    test("add authenticated client registration policy", async ({ page }) => {
      await clickCreateAuthenticatedPolicy(page);
      await createPolicy(page, "scope", { name: policyName });
      await clickSaveButton(page);

      await assertNotificationMessage(
        page,
        "New client policy created successfully",
      );
      await clickCancelButton(page);

      await assertRowExists(page, policyName);
    });

    test("edit authenticated client registration policy", async ({ page }) => {
      await clickTableRowItem(page, "Allowed Protocol Mapper Types");
      await fillPolicyForm(page, { name: policyNameUpdated });
      await clickSaveButton(page);

      await assertNotificationMessage(
        page,
        "Client policy updated successfully",
      );
      await clickCancelButton(page);
      await assertRowExists(page, policyNameUpdated);
    });

    test("delete authenticated client registration policy", async ({
      page,
    }) => {
      await clickRowKebabItem(page, "Allowed Client Scopes", "Delete");
      await confirmModal(page);

      await assertNotificationMessage(
        page,
        "Client registration policy deleted successfully",
      );
    });
  });
});

test.describe
  .serial("Accessibility tests for client registration policies", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToClients(page);
    await goToClientRegistrationTab(page);
  });

  test("Check accessibility violations", async ({ page }) => {
    await assertAxeViolations(page);
  });
});
