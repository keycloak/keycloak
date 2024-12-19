import { expect, test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../../cypress/support/util/AdminClient";
import { assertRequiredFieldError, clickSaveButton } from "../utils/form";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { goToAuthentication, goToRealm } from "../utils/sidebar";
import {
  clickRowKebabItem,
  clickTableRowItem,
  getRowByCellText,
  searchItem,
  selectRowKebab,
} from "../utils/table";
import {
  addExecution,
  addPolicy,
  assertAxeViolations,
  assertDefaultSwitchPolicyEnabled,
  assertExecutionExists,
  assertSwitchPolicyChecked,
  clickDefaultSwitchPolicy,
  clickSwitchPolicy,
  fillCreateForm,
  fillDuplicateFlowModal,
  goToCIBAPolicyTab,
  goToCreateItem,
  goToOTPPolicyTab,
  goToPoliciesTab,
  goToRequiredActions,
  goToWebAuthnTab,
} from "./flow";

test.describe("Authentication test", () => {
  const realmName = `test${uuidv4()}`;

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
    await expect(getRowByCellText(page, "clients")).not.toBeVisible();
  });

  test("should create duplicate of existing flow", async ({ page }) => {
    await selectRowKebab(page, "browser");
    await clickRowKebabItem(page, "Duplicate");
    await fillDuplicateFlowModal(page, "Copy of browser");

    await assertNotificationMessage(page, "Flow successfully duplicated");
    await expect(page.locator('text="Copy of browser"')).toBeVisible();
  });

  test("Should fail duplicate with empty flow name", async ({ page }) => {
    await selectRowKebab(page, "Direct grant");
    await clickRowKebabItem(page, "Duplicate");
    await fillDuplicateFlowModal(page, "");

    await assertRequiredFieldError(page, "alias");
  });

  test("Should fail duplicate with duplicated name", async ({ page }) => {
    await selectRowKebab(page, "Direct grant");
    await clickRowKebabItem(page, "Duplicate");
    await fillDuplicateFlowModal(page, "browser");

    await assertNotificationMessage(
      page,
      "Could not duplicate flow: New flow alias name already exists",
    );
  });

  test.describe("Flow details", () => {
    let flowId: string | undefined;
    const flowName = "Copy of browser test";
    test.beforeAll(() =>
      adminClient.inRealm(realmName, async () => {
        await adminClient.copyFlow("browser", flowName);
        flowId = (await adminClient.getFlow(flowName))!.id!;
      }),
    );
    test.afterAll(() =>
      adminClient.inRealm(realmName, () => adminClient.deleteFlow(flowId!)),
    );

    test("Should add a execution", async ({ page }) => {
      await clickTableRowItem(page, flowName);
      await addExecution(
        page,
        flowName + " forms",
        "reset-credentials-choose-user",
      );

      await assertNotificationMessage(page, "Flow successfully updated");
      await assertExecutionExists(page, "Choose User");
    });
  });
});

test.describe("Required actions", () => {
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

test.describe("Password policies tab", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToAuthentication(page);
    await goToPoliciesTab(page);
  });

  test("should add password policies", async ({ page }) => {
    await expect(page.locator('[data-testid="empty-state"]')).toBeVisible();
    await addPolicy(page, "Not Recently Used");
    await clickSaveButton(page);
    await assertNotificationMessage(
      page,
      "Password policies successfully updated",
    );
  });
});

test.describe("Accessibility tests for authentication", () => {
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
