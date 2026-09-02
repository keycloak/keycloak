import { type Locator, type Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";
import { confirmModal } from "../utils/modal.ts";

export async function fillDuplicateFlowModal(
  page: Page,
  alias: string,
  description?: string,
) {
  await page.getByTestId("alias").fill(alias);
  await page.getByTestId("description").fill(description || "");
  await confirmModal(page);
}

async function clickEditDropdownForFlow(page: Page, flowName: string) {
  await page.getByTestId(`${flowName}-edit-dropdown`).click();
}

async function selectExecutionTestId(page: Page, executionTestId: string) {
  await page.getByTestId(executionTestId).click();
  await page.getByTestId("modal-add").click();
}

export async function addExecution(
  page: Page,
  subFlowName: string,
  executionTestId: string,
) {
  await clickEditDropdownForFlow(page, subFlowName);
  await page.getByRole("menuitem", { name: "Add execution" }).click();
  await selectExecutionTestId(page, executionTestId);
}

export async function addCondition(
  page: Page,
  subFlowName: string,
  executionTestId: string,
) {
  await clickEditDropdownForFlow(page, subFlowName);
  await page.getByRole("menuitem", { name: "Add condition" }).click();
  await selectExecutionTestId(page, executionTestId);
}

export async function addSubFlow(
  page: Page,
  subFlowName: string,
  name: string,
) {
  await clickEditDropdownForFlow(page, subFlowName);
  await page.getByRole("menuitem", { name: "Add sub-flow" }).click();
  await page.getByTestId("name").fill(name);
  await page.getByTestId("modal-add").click();
}

export async function clickDeleteRow(page: Page, flowName: string) {
  await page.getByTestId(`${flowName}-delete`).click();
}

export async function assertRowExists(page: Page, name: string, exists = true) {
  const locator = page.getByTestId(name);
  if (exists) {
    await expect(locator).toBeVisible();
  } else {
    await expect(locator).toBeHidden();
  }
}

export async function fillBindFlowModal(page: Page, flowName: string) {
  await selectItem(page, page.locator("#chooseBindingType"), flowName);
}

export async function goToRequiredActions(page: Page) {
  await page.getByTestId("requiredActions").click();
}

export async function goToPoliciesTab(page: Page) {
  await page.getByTestId("policies").click();
}

export async function goToOTPPolicyTab(page: Page) {
  await goToPoliciesTab(page);
  await page.getByTestId("otpPolicy").click();
}

export async function goToWebAuthnTab(page: Page) {
  await goToPoliciesTab(page);
  await page.getByTestId("webauthnPolicy").click();
}

export async function goToCIBAPolicyTab(page: Page) {
  await goToPoliciesTab(page);
  await page.getByTestId("cibaPolicy").click();
}

export async function addPolicy(page: Page, value: string) {
  await selectItem(page, page.getByTestId("add-policy"), value);
}

const toKey = (name: string) => {
  return name.replace(/\s/g, "-");
};

const getEnabledSwitch = (page: Page, name: string) => {
  return page.locator(`#enable-${toKey(name)}`);
};

const getDefaultSwitch = (page: Page, name: string) => {
  return page.locator(`#default-${toKey(name)}`);
};

export async function clickSwitchPolicy(page: Page, policyName: string) {
  await getEnabledSwitch(page, policyName).click();
}

export async function clickDefaultSwitchPolicy(page: Page, policyName: string) {
  await getDefaultSwitch(page, policyName).click();
}

export async function assertSwitchPolicyChecked(
  page: Page,
  policyName: string,
  checked = true,
) {
  if (checked) {
    await expect(getEnabledSwitch(page, policyName)).toBeChecked();
  } else {
    await expect(getEnabledSwitch(page, policyName)).not.toBeChecked();
  }
}

export async function assertDefaultSwitchPolicyChecked(
  page: Page,
  policyName: string,
  checked = true,
) {
  if (checked) {
    await expect(getDefaultSwitch(page, policyName)).toBeChecked();
  } else {
    await expect(getDefaultSwitch(page, policyName)).not.toBeChecked();
  }
}

export async function assertSwitchPolicyEnabled(
  page: Page,
  policyName: string,
) {
  await expect(getEnabledSwitch(page, policyName)).toBeEnabled();
}

export async function assertDefaultSwitchPolicyEnabled(
  page: Page,
  policyName: string,
) {
  await expect(getDefaultSwitch(page, policyName)).toBeEnabled();
}

export async function goToCreateItem(page: Page) {
  await page.getByRole("link", { name: "Create flow" }).click();
}

export async function fillCreateForm(
  page: Page,
  name: string,
  description: string,
  type: string,
) {
  await page.getByTestId("alias").fill(name);
  await page.getByTestId("description").fill(description);
  await selectItem(page, page.getByLabel("Flow type"), type);
  await page.getByTestId("create").click();
}

export async function dragExecutionAboveExecution(
  page: Page,
  sourceExecution: string,
  targetExecution: string,
) {
  const treeGrid = page.getByRole("treegrid", { name: "Flows" });
  const sourceRow = treeGrid
    .getByRole("row")
    .filter({ hasText: sourceExecution })
    .first();
  const targetRow = treeGrid
    .getByRole("row")
    .filter({ hasText: targetExecution })
    .first();
  const getDragHandle = (row: Locator) =>
    row.getByRole("button", { name: /draggable row/i }).first();

  await expect(sourceRow).toBeVisible({ timeout: 5_000 });
  await expect(targetRow).toBeVisible({ timeout: 5_000 });

  const sourceHandle = getDragHandle(sourceRow);
  await expect(sourceHandle).toBeVisible({ timeout: 5_000 });
  await sourceHandle.scrollIntoViewIfNeeded();

  const getRowOrder = async () => {
    const rows = await treeGrid.getByRole("row").allInnerTexts();
    return {
      sourceIndex: rows.findIndex((row) => row.includes(sourceExecution)),
      targetIndex: rows.findIndex((row) => row.includes(targetExecution)),
    };
  };

  const initialOrder = await getRowOrder();
  expect(
    initialOrder.sourceIndex,
    `Expected "${sourceExecution}" to start below "${targetExecution}" so drag can be verified`,
  ).toBeGreaterThan(initialOrder.targetIndex);

  const hasMoved = async () => {
    const { sourceIndex, targetIndex } = await getRowOrder();
    return (
      sourceIndex !== -1 &&
      targetIndex !== -1 &&
      sourceIndex < targetIndex &&
      sourceIndex !== initialOrder.sourceIndex
    );
  };

  await sourceHandle.focus();
  await page.keyboard.press("Space");

  const rows = await treeGrid.getByRole("row").allInnerTexts();
  const sourceIndex = rows.findIndex((row) => row.includes(sourceExecution));
  const targetIndex = rows.findIndex((row) => row.includes(targetExecution));
  const steps =
    sourceIndex !== -1 && targetIndex !== -1 && sourceIndex > targetIndex
      ? sourceIndex - targetIndex
      : 1;

  for (let step = 0; step < steps; step++) {
    await page.keyboard.press("ArrowUp");
  }
  await page.keyboard.press("Space");

  await expect.poll(hasMoved, { timeout: 8_000 }).toBe(true);
  await expect(page.getByTestId("flow-order-stable")).toBeVisible();
}
