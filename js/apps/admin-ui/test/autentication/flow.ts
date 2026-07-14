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

type DragTargetPosition = {
  /** Horizontal offset within the target row, as a fraction of row width. */
  xRatio?: number;
  /** Vertical offset within the target row, as a fraction of row height. */
  yRatio?: number;
  /** Minimum vertical offset in pixels (applied after yRatio). */
  minYOffset?: number;
};

/** Drags a flow row grip handle onto another row. Re-measures the drop target after drag
 *  activation because drag start collapses expanded subflows and shifts row positions. */
export async function dragExecutionToRow(
  page: Page,
  sourceRow: Locator,
  targetRow: Locator,
  targetPosition: DragTargetPosition = {},
) {
  const { xRatio = 0.5, yRatio = 0.5, minYOffset } = targetPosition;
  const sourceHandle = sourceRow.locator(
    ".keycloak__authentication__drag-handle",
  );
  const sourceBox = await sourceHandle.boundingBox();
  expect(sourceBox).not.toBeNull();

  const startX = sourceBox!.x + sourceBox!.width / 2;
  const startY = sourceBox!.y + sourceBox!.height / 2;

  await page.mouse.move(startX, startY);
  await page.mouse.down();
  // Satisfy dnd-kit PointerSensor activation distance (5px).
  await page.mouse.move(startX + 10, startY, { steps: 5 });
  // Drag start collapses expanded subflows asynchronously; wait for layout to settle.
  await page.waitForTimeout(200);

  const targetBox = await targetRow.boundingBox();
  expect(targetBox).not.toBeNull();

  const targetY =
    minYOffset !== undefined
      ? Math.max(minYOffset, targetBox!.height * yRatio)
      : targetBox!.height * yRatio;

  await page.mouse.move(
    targetBox!.x + targetBox!.width * xRatio,
    targetBox!.y + targetY,
    { steps: 25 },
  );
  await page.mouse.up();
}

export async function expandFlowRow(page: Page, displayName: string) {
  const row = page.getByRole("row").filter({
    has: page.getByTestId(displayName),
  });
  if ((await row.getAttribute("aria-expanded")) === "false") {
    await row.getByRole("button").first().click();
  }
}

export async function assertExecutionRequirement(
  page: Page,
  executionName: string | RegExp,
  requirement: string,
) {
  const row = page.getByRole("row", { name: executionName }).first();
  await expect(
    row.locator(".keycloak__authentication__requirement-dropdown"),
  ).toHaveText(requirement);
}

export async function assertExecutionLevel(
  page: Page,
  executionName: string | RegExp,
  level: number,
) {
  const row = page
    .locator("tr[data-execution-id]")
    .filter({
      has:
        typeof executionName === "string"
          ? page.getByTestId(executionName)
          : page.getByRole("row", { name: executionName }),
    })
    .first();
  await expect(row).toHaveAttribute("data-level", String(level));
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
  await getEnabledSwitch(page, policyName).click({ force: true });
}

export async function clickDefaultSwitchPolicy(page: Page, policyName: string) {
  await getDefaultSwitch(page, policyName).click({ force: true });
}

export async function assertSwitchPolicyChecked(
  page: Page,
  policyName: string,
) {
  await expect(getEnabledSwitch(page, policyName)).toBeChecked();
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
