import { type Page, expect } from "@playwright/test";
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
  await page.getByTestId("tab-ciba-policy").click();
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
