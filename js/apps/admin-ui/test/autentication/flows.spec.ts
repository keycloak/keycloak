import { expect, test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertRequiredFieldError, clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import {
  assertAxeViolations,
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToAuthentication, goToRealm } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  clickRowKebabItem,
  clickTableRowItem,
  getRowByCellText,
  searchItem,
} from "../utils/table.ts";
import {
  addCondition,
  addExecution,
  addPolicy,
  addSubFlow,
  assertDefaultSwitchPolicyEnabled,
  assertRowExists,
  assertSwitchPolicyChecked,
  clickDefaultSwitchPolicy,
  clickDeleteRow,
  clickSwitchPolicy,
  fillBindFlowModal,
  fillCreateForm,
  fillDuplicateFlowModal,
  goToCIBAPolicyTab,
  goToCreateItem,
  goToOTPPolicyTab,
  goToPoliciesTab,
  goToRequiredActions,
  goToWebAuthnTab,
} from "./flow.ts";

test.describe.serial("Authentication test", () => {
  const realmName = `authentication-flow-${uuidv4()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToAuthentication(page);
  });

  test("authentication search flow", async ({ page }) => {
    const itemId = "browser";
    await searchItem(page, "Search for flow", itemId);
    await expect(getRowByCellText(page, itemId)).toBeVisible();
    await expect(getRowByCellText(page, "clients")).toBeHidden();
  });

  test("should create duplicate of existing flow", async ({ page }) => {
    await clickRowKebabItem(page, "browser", "Duplicate");
    await fillDuplicateFlowModal(page, "Copy of browser");

    await assertNotificationMessage(page, "Flow successfully duplicated");
    await expect(page.locator('text="Copy of browser"')).toBeVisible();
  });

  test("Should fail duplicate with empty flow name", async ({ page }) => {
    await clickRowKebabItem(page, "Direct grant", "Duplicate");
    await fillDuplicateFlowModal(page, "");

    await assertRequiredFieldError(page, "alias");
  });

  test("Should fail duplicate with duplicated name", async ({ page }) => {
    await clickRowKebabItem(page, "Direct grant", "Duplicate");
    await fillDuplicateFlowModal(page, "browser");

    await assertNotificationMessage(
      page,
      "Could not duplicate flow: New flow alias name already exists",
    );
  });

  test.describe.serial("Flow details", () => {
    const flowName = "Copy of browser test";

    test.beforeEach(async ({ page }) => {
      await adminClient.copyFlow("browser", flowName, realmName);
      await page.getByTestId("refresh").click();
    });

    test.afterEach(() => adminClient.deleteFlow(flowName, realmName));

    test("Should add a execution", async ({ page }) => {
      await clickTableRowItem(page, flowName);
      await addExecution(
        page,
        flowName + " forms",
        "reset-credentials-choose-user",
      );

      await assertNotificationMessage(page, "Flow successfully updated");
      await assertRowExists(page, "Choose User");
    });

    test("should add a condition", async ({ page }) => {
      await clickTableRowItem(page, flowName);

      await addCondition(
        page,
        flowName + " Browser - Conditional 2FA",
        "conditional-user-role",
      );

      await assertNotificationMessage(page, "Flow successfully updated");
    });

    test("Should add a sub-flow", async ({ page }) => {
      await clickTableRowItem(page, flowName);

      const name = "SubFlow";
      await addSubFlow(page, flowName + " Browser - Conditional 2FA", name);

      await assertNotificationMessage(page, "Flow successfully updated");
      await assertRowExists(page, name);
    });

    test("Should remove an execution", async ({ page }) => {
      await clickTableRowItem(page, flowName);

      const name = "Cookie";
      await assertRowExists(page, name);

      await clickDeleteRow(page, name);
      await confirmModal(page);
      await assertRowExists(page, "Cookie", false);
    });

    test("Should set as default in action menu", async ({ page }) => {
      await clickTableRowItem(page, flowName);
      await selectActionToggleItem(page, "Bind flow");

      // set as default
      await fillBindFlowModal(page, "Direct grant flow");
      await clickSaveButton(page);
      await assertNotificationMessage(page, "Flow successfully updated");
      await expect(page.getByText("Default")).toBeVisible();

      // unset as default
      await goToAuthentication(page);
      await clickTableRowItem(page, "direct grant");
      await selectActionToggleItem(page, "Bind flow");
      await fillBindFlowModal(page, "Direct grant flow");
      await clickSaveButton(page);
    });

    test("Drag and drop execution", async ({ page }) => {
      await clickTableRowItem(page, flowName);
      const source = page.getByText("Identity Provider Redirector");
      const target = page.getByText("Kerberos");

      // execute mouse movement twice to trigger dragover event
      await source.hover();
      await source.hover();

      await page.mouse.down();
      await target.hover();
      await target.hover();

      await page.mouse.up();

      await assertNotificationMessage(page, "Flow successfully updated");
    });

    test("Should edit flow details", async ({ page }) => {
      await clickTableRowItem(page, flowName);

      await selectActionToggleItem(page, "Edit info");

      const newName = "New flow name";
      await fillDuplicateFlowModal(page, newName, "Other description");
      await assertNotificationMessage(page, "Flow successfully updated");
      await expect(page.locator(`text="${newName}"`)).toBeVisible();
    });
  });
});

test.describe.serial("Required actions", () => {
  const realmName = `test-${uuidv4()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToAuthentication(page);
    await goToRequiredActions(page);
  });

  test("should enable delete account", async ({ page }) => {
    const action = "Delete Account";
    await clickSwitchPolicy(page, action);
    await assertNotificationMessage(
      page,
      "Updated required action successfully",
    );
    await assertSwitchPolicyChecked(page, action);
  });

  test("should register an unregistered action", async ({ page }) => {
    const action = "Verify Profile";
    await assertSwitchPolicyChecked(page, action);
    await assertDefaultSwitchPolicyEnabled(page, action);
    await clickSwitchPolicy(page, action);
    await assertNotificationMessage(
      page,
      "Updated required action successfully",
    );
    await clickSwitchPolicy(page, action);
    await assertNotificationMessage(
      page,
      "Updated required action successfully",
    );
    await assertSwitchPolicyChecked(page, action);
    await assertDefaultSwitchPolicyEnabled(page, action);
  });

  test("should set action as default", async ({ page }) => {
    const action = "Configure OTP";
    await clickDefaultSwitchPolicy(page, action);
    await assertNotificationMessage(
      page,
      "Updated required action successfully",
    );
    await assertSwitchPolicyChecked(page, action);
  });
});

test.describe.serial("Password policies tab", () => {
  const realmName = `policies-password-${uuidv4()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToAuthentication(page);
    await goToPoliciesTab(page);
  });

  test("should add password policies", async ({ page }) => {
    await assertEmptyTable(page);
    await addPolicy(page, "Not Recently Used");
    await clickSaveButton(page);
    await assertNotificationMessage(
      page,
      "Password policies successfully updated",
    );
  });
});

test.describe.serial("Accessibility tests for authentication", () => {
  const realmName = "a11y-realm";
  const flowName = `Flow-${uuidv4()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToAuthentication(page);
  });

  test("should pass accessibility checks on main page", async ({ page }) => {
    await assertAxeViolations(page);
  });

  test("Check a11y violations on load/ authentication tab/ flows sub tab/ creating flow form", async ({
    page,
  }) => {
    await goToCreateItem(page);

    await assertAxeViolations(page);
    await page.getByTestId("cancel").click();
  });

  test("Check a11y violations on load/ authentication tab/ flows sub tab/ creating flow", async ({
    page,
  }) => {
    await goToCreateItem(page);
    await fillCreateForm(
      page,
      flowName,
      "Some nice description about what this flow does",
      "Client flow",
    );
    await assertAxeViolations(page);
  });

  test("Check a11y violations on load/ authentication tab/ flows sub tab/ creating", async ({
    page,
  }) => {
    await clickTableRowItem(page, "reset credentials");
    await assertAxeViolations(page);
  });

  test("Check a11y violations on load/ authentication tab/ required actions sub tab", async ({
    page,
  }) => {
    await goToRequiredActions(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on load/ policies tab/ password policy sub tab", async ({
    page,
  }) => {
    await goToPoliciesTab(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on load/ authentication tab/ policies sub tab/ adding policy", async ({
    page,
  }) => {
    await goToPoliciesTab(page);
    await addPolicy(page, "Not Recently Used");
    await assertAxeViolations(page);
  });

  test("Check a11y violations on load/ policies tab/ otp policy sub tab", async ({
    page,
  }) => {
    await goToOTPPolicyTab(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on load/ policies tab/ WebAuthn Policies sub tab", async ({
    page,
  }) => {
    await goToWebAuthnTab(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on load/ policies tab/ CIBA Policy sub tab", async ({
    page,
  }) => {
    await goToCIBAPolicyTab(page);
    await assertAxeViolations(page);
  });
});
