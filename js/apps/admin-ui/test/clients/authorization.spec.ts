import { test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";
import { clickSaveButton } from "../utils/form.ts";
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
  searchItem,
} from "../utils/table.ts";
import {
  assertClipboardHasText,
  assertDownload,
  assertEmptyStateNotVisible,
  assertTableIsPopulated,
  assertResource,
  assertRowNotVisible,
  clickAuthenticationSaveButton,
  clickCopyButton,
  clickNextPage,
  createAuthorizationScope,
  createPermission,
  createPolicy,
  createResource,
  deletePolicy,
  fillForm,
  goToAuthorizationTab,
  goToExportSubTab,
  goToPermissionsSubTab,
  goToPoliciesSubTab,
  goToResourcesSubTab,
  goToScopesSubTab,
  inputClient,
  selectResource,
  setPolicy,
} from "./authorization.ts";

test.describe.serial("Client authentication subtab", () => {
  const clientId = `client-authentication-${crypto.randomUUID()}`;

  test.beforeAll(async () => {
    await adminClient.createClient({
      protocol: "openid-connect",
      clientId,
      publicClient: false,
      authorizationServicesEnabled: true,
      serviceAccountsEnabled: true,
      standardFlowEnabled: true,
    });
  });

  test.afterAll(async () => {
    await adminClient.deleteClient(clientId);
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await goToAuthorizationTab(page);
  });

  test("Should update the resource server settings", async ({ page }) => {
    await setPolicy(page, "DISABLED");
    await clickAuthenticationSaveButton(page);
    await assertNotificationMessage(page, "Resource successfully updated");
  });

  test("Should create a resource", async ({ page }) => {
    await goToResourcesSubTab(page);
    await createResource(page, {
      name: "Test Resource",
      displayName: "The display name",
      type: "type",
      uris: ["one", "two"],
    });

    await clickSaveButton(page);
    await assertNotificationMessage(page, "Resource created successfully");
  });

  test("Edit a resource", async ({ page }) => {
    await goToResourcesSubTab(page);
    await clickTableRowItem(page, "Test Resource");

    await fillForm(page, { displayName: "updated" });
    await clickSaveButton(page);

    await assertNotificationMessage(page, "Resource successfully updated");
  });

  test("Should navigate to previous page when last resource on current page is deleted", async ({
    page,
  }) => {
    for (let i = 0; i < 15; i++) {
      const paddedIndex = i.toString().padStart(2, "0");
      await adminClient.createResource(clientId, {
        name: `Z Paginated Resource ${paddedIndex}`,
      });
    }

    try {
      await goToResourcesSubTab(page);
      await assertTableIsPopulated(page);

      await clickNextPage(page);
      await assertResource(page, "Z Paginated Resource 10");

      for (let i = 10; i < 15; i++) {
        const paddedIndex = i.toString().padStart(2, "0");
        const resourceName = `Z Paginated Resource ${paddedIndex}`;

        await clickRowKebabItem(page, resourceName, "Delete");
        await confirmModal(page);
        await assertRowNotVisible(page, resourceName);
      }

      await assertEmptyStateNotVisible(page);
      await assertTableIsPopulated(page);
    } finally {
      for (let i = 0; i < 15; i++) {
        const paddedIndex = i.toString().padStart(2, "0");
        await adminClient.deleteResource(clientId, {
          name: `Z Paginated Resource ${paddedIndex}`,
        });
      }
    }
  });

  test("Should create a scope", async ({ page }) => {
    await goToScopesSubTab(page);
    await createAuthorizationScope(page, {
      name: "The scope",
      displayName: "Display something",
      iconUri: "res://something",
    });
    await clickSaveButton(page);

    await assertNotificationMessage(
      page,
      "Authorization scope created successfully",
    );
    await goToScopesSubTab(page);
    await assertRowExists(page, "The scope");
  });

  test("Should create a permission", async ({ page }) => {
    await goToPermissionsSubTab(page);

    await createPermission(page, "resource", {
      name: "Permission name",
      description: "Something describing this permission",
    });
    await selectResource(page, "Test Resource");

    await clickSaveButton(page);
    await assertNotificationMessage(
      page,
      "Successfully created the permission",
    );
  });

  test("Should create a policy", async ({ page }) => {
    await goToPoliciesSubTab(page);
    await createPolicy(page, "Regex", {
      name: "Regex policy",
      description: "Policy for regex",
      targetClaim: "I don't know",
      pattern: ".*?",
    });
    await clickSaveButton(page);

    await assertNotificationMessage(page, "Successfully created the policy");
  });

  test("Should delete a policy", async ({ page }) => {
    await goToPoliciesSubTab(page);
    await deletePolicy(page, "Regex Policy");

    await assertNotificationMessage(page, "The Policy successfully deleted");
  });

  test("Should create a client policy", async ({ page }) => {
    await goToPoliciesSubTab(page);
    await createPolicy(page, "Client", {
      name: "Client policy",
      description: "Extra client field",
    });

    await inputClient(page, "master-realm");
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Successfully created the policy");
  });

  test("Should copy auth details", async ({ page, context, browserName }) => {
    test.skip(browserName === "firefox", "Still working on it");
    await context.grantPermissions(["clipboard-write", "clipboard-read"]);
    await goToExportSubTab(page);
    await clickCopyButton(page);
    await assertNotificationMessage(page, "Authorization details copied.");
    await assertClipboardHasText(page);
  });

  test("Should export auth details", async ({ page }) => {
    await goToExportSubTab(page);

    await assertDownload(page);
  });
});

test.describe
  .serial("Client authorization tab access for view-realm-authorization", () => {
  const clientId = `realm-view-authz-client-${crypto.randomUUID()}`;
  const resourceName = `test-resource-${crypto.randomUUID()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm("realm-view-authz");
    const testUser = await adminClient.createUser({
      // Create user in master realm
      username: "test-view-authz-user",
      enabled: true,
      credentials: [{ type: "password", value: "password" }],
    });

    await adminClient.addClientRoleToUser(
      testUser.id!,
      "realm-view-authz-realm",
      ["view-realm", "view-users", "view-authorization", "view-clients"],
    );
    await adminClient.createClient({
      realm: "realm-view-authz",
      clientId,
      authorizationServicesEnabled: true,
      serviceAccountsEnabled: true,
      standardFlowEnabled: true,
    });
    await adminClient.createResource(clientId, {
      realm: "realm-view-authz",
      name: resourceName,
    });
  });

  test.afterAll(async () => {
    await adminClient.deleteUser("test-view-authz-user");
    await adminClient.deleteRealm("realm-view-authz");
  });

  test("Should view authorization tab", async ({ page }) => {
    await login(page, {
      username: "test-view-authz-user",
      password: "password",
    });

    await goToRealm(page, "realm-view-authz");
    await page.reload();
    await goToClients(page);

    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await goToAuthorizationTab(page);

    await goToResourcesSubTab(page);
    await clickTableRowItem(page, resourceName);
    await page.goBack();

    await goToScopesSubTab(page);
    await goToPoliciesSubTab(page);
    await goToPermissionsSubTab(page);
  });
});

test.describe.serial("Accessibility tests for client authorization", () => {
  const clientId = `realm-view-authz-client-${crypto.randomUUID()}`;
  test.beforeAll(() =>
    adminClient.createClient({
      protocol: "openid-connect",
      clientId,
      publicClient: false,
      authorizationServicesEnabled: true,
      serviceAccountsEnabled: true,
      standardFlowEnabled: true,
    }),
  );

  test.afterAll(() => adminClient.deleteClient(clientId));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await goToAuthorizationTab(page);
  });

  test("Check a11y violations on load/ client authorization", async ({
    page,
  }) => {
    await assertAxeViolations(page);
  });
});
