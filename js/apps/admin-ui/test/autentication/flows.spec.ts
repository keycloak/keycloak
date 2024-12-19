import { expect, test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../../cypress/support/util/AdminClient";
import { assertRequiredFieldError } from "../utils/form";
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
  assertDefaultSwitchPolicyEnabled,
  assertExecutionExists,
  assertSwitchPolicyChecked,
  clickDefaultSwitchPolicy,
  clickSwitchPolicy,
  fillDuplicateFlowModal,
  goToRequiredActions,
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
    await selectRowKebab(page, "Browser");
    await clickRowKebabItem(page, "Duplicate");
    await fillDuplicateFlowModal(page, "");

    await assertRequiredFieldError(page, "alias");
  });

  test("Should fail duplicate with duplicated name", async ({ page }) => {
    await selectRowKebab(page, "Browser");
    await clickRowKebabItem(page, "Duplicate");
    await fillDuplicateFlowModal(page, "browser");

    await assertNotificationMessage(
      page,
      "Could not duplicate flow: New flow alias name already exists",
    );
  });

  test.describe("Flow details", () => {
    let flowId: string | undefined;
    test.beforeAll(() =>
      adminClient.inRealm(realmName, async () => {
        await adminClient.copyFlow("browser", "Copy of browser");
        flowId = (await adminClient.getFlow("Copy of browser"))!.id!;
      }),
    );
    test.afterAll(() =>
      adminClient.inRealm(realmName, () => adminClient.deleteFlow(flowId!)),
    );

    test("Should add a execution", async ({ page }) => {
      await clickTableRowItem(page, "Copy of browser");
      await addExecution(
        page,
        "Copy of browser forms",
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
    await page.click("text=Password Policy");
  });

  test("should add password policies", async ({ page }) => {
    await expect(page.locator('[data-testid="empty-state"]')).toBeVisible();
    await page.selectOption(
      '[data-testid="policy-select"]',
      "Not Recently Used",
    );
    await page.click('[data-testid="save"]');
    await assertNotificationMessage(
      page,
      "Password policies successfully updated",
    );
  });

  // Additional password policy tests...
});

// // Note: For accessibility tests, you would use the @axe-core/playwright package
// test.describe("Accessibility tests for authentication", () => {
//   const realmName = "a11y-realm";

//   test.beforeAll(async ({ request }) => {
//     await request.post("/admin/realms", {
//       data: { realm: realmName },
//     });
//   });

//   test.beforeEach(async ({ page }) => {
//     await login(page);
//     await goToRealm(page, realmName);
//     await goToAuthentication(page);
//   });

//   test.afterAll(async ({ request }) => {
//     await request.delete(`/admin/realms/${realmName}`);
//   });

//   test("should pass accessibility checks on main page", async ({ page }) => {
//     // Assuming you've configured axe-core
//     const violations = await page.evaluate(async () => {
//       // @ts-ignore
//       const { axe } = window;
//       const results = await axe.run();
//       return results.violations;
//     });
//     expect(violations.length).toBe(0);
//   });

//   // Additional accessibility tests...
// });
