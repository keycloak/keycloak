import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertSaveButtonIsDisabled, clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToWorkflows, goToRealm } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertRowExists,
  clickRowKebabItem,
  clickTableRowItem,
} from "../utils/table.ts";
import {
  fillCreatePage,
  fillYamlField,
  getYamlField,
  goToCreate,
} from "./main.ts";

function simpleWorkflowStr(name: string): string {
  return `---
    name: ${name}
    on: user_authenticated
    steps:
      - uses: notify-user
        with:
          message: Welcome to the Gold Membership program!
   `;
}

function complexWorkflowStr(name: string): string {
  return `
    name: ${name}
    on: user_authenticated
    if: "!has-role('realm-management/realm-admin')"
    steps:
      - uses: notify-user
        after: "30"
        with:
          custom_message: "Welcome back! Your login has been recorded. If inactive, action may be taken."
      - uses: disable-user
        after: "60"
      - uses: delete-user
        after: "90"
   `;
}

const workflowCreatedMessage = "The workflow has been created.";
const worflowUpdatedMessage = "Workflow updated successfully";
const workflowDeletedMessage = "The workflow has been deleted.";
const workflowDisabledMessage = "Workflow disabled";

const simpleWorkflowName = `workflow-simple-${uuid()}`;
const simpleWorkflowRenamedName = `workflow-simple-rename-${uuid()}`;
const simpleWorkflowNameRenamedCopy = `${simpleWorkflowRenamedName} -- Copy`;
const simpleWorkflow = simpleWorkflowStr(simpleWorkflowName);
const simpleWorkflowRenamed = simpleWorkflowStr(simpleWorkflowRenamedName);

const complexWorkflowName = `workflow-complex-${uuid()}`;
const complexWorkflowNameCopy = `${complexWorkflowName} -- Copy`;
const complexWorkflow = complexWorkflowStr(complexWorkflowName);

test.describe.serial("Workflow CRUD", () => {
  const realmName = `workflow-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToWorkflows(page);
  });

  test("should create simple workflow from empty state", async ({ page }) => {
    await assertEmptyTable(page);
    await goToCreate(page);
    await assertSaveButtonIsDisabled(page);
    await fillCreatePage(page, simpleWorkflow);
    await clickSaveButton(page);
    await assertNotificationMessage(page, workflowCreatedMessage);
    await assertRowExists(page, simpleWorkflowName);
  });

  test("should create complex workflow from create button", async ({
    page,
  }) => {
    await goToCreate(page, false);
    await assertSaveButtonIsDisabled(page);
    await fillCreatePage(page, complexWorkflow);
    await clickSaveButton(page);
    await assertNotificationMessage(page, workflowCreatedMessage);
    await assertRowExists(page, complexWorkflowName);
  });

  test("should modify existing workflow", async ({ page }) => {
    await goToWorkflows(page);
    await clickTableRowItem(page, simpleWorkflowName);

    // This waits for the field to be filled before we clear and fill it with a new value
    await expect(getYamlField(page)).toContainText(simpleWorkflowName);

    await fillYamlField(page, simpleWorkflowRenamed);
    await expect(getYamlField(page)).toContainText(simpleWorkflowRenamedName);
    await clickSaveButton(page);
    await assertNotificationMessage(page, worflowUpdatedMessage);
    await goToWorkflows(page);
    await assertRowExists(page, simpleWorkflowRenamedName);
  });

  test("should disable workflow", async ({ page }) => {
    const toggleLocator = page.locator(
      `[data-testid="toggle-enabled-${simpleWorkflowRenamedName}"]`,
    );

    await expect(toggleLocator).toBeVisible();
    await expect(toggleLocator).toBeEnabled();
    await expect(toggleLocator).toBeChecked();

    // Force click (ignores actionability—best for flaky React toggles)
    // Without this, test fails intermittently waiting for the element to be actionable
    await toggleLocator.click({ force: true, timeout: 5000 });

    await assertNotificationMessage(page, workflowDisabledMessage);
    await expect(toggleLocator).not.toBeChecked();
  });

  test("should copy workflow from list", async ({ page }) => {
    await clickRowKebabItem(page, simpleWorkflowRenamedName, "Copy");
    await clickSaveButton(page);
    await assertNotificationMessage(page, workflowCreatedMessage);
    await goToWorkflows(page);
    await assertRowExists(page, simpleWorkflowNameRenamedCopy);
  });

  test("should copy workflow from edit page", async ({ page }) => {
    await clickTableRowItem(page, complexWorkflowName);
    await page.getByTestId("copy").click();
    await clickSaveButton(page);
    await assertNotificationMessage(page, workflowCreatedMessage);
    await goToWorkflows(page);
    await assertRowExists(page, complexWorkflowNameCopy);
  });

  test("should delete workflow from list", async ({ page }) => {
    console.log("Deleting workflow:", complexWorkflowNameCopy);
    await assertRowExists(page, complexWorkflowNameCopy);
    await clickRowKebabItem(page, complexWorkflowNameCopy, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, workflowDeletedMessage);
    await assertRowExists(page, complexWorkflowNameCopy, false);
  });
});
