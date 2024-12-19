import { Page, expect } from "@playwright/test";
import { confirmModal } from "../utils/modal";

export async function fillDuplicateFlowModal(
  page: Page,
  alias: string,
  description?: string,
) {
  await page.getByTestId("alias").fill(alias);
  await page.getByTestId("description").fill(description || "");
  await confirmModal(page);
}

export async function addExecution(
  page: Page,
  subFlowName: string,
  executionTestId: string,
) {
  await page.getByTestId(`${subFlowName}-edit-dropdown`).click();
  await page.getByRole("menuitem", { name: "Add step" }).click();
  await page.getByTestId(executionTestId).click();
  await page.getByTestId("modal-add").click();
}

export async function assertExecutionExists(page: Page, name: string) {
  expect(page.getByTestId(name)).toBeVisible();
}

export async function goToRequiredActions(page: Page) {
  await page.getByTestId("requiredActions").click();
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
  expect(getEnabledSwitch(page, policyName)).toBeChecked();
}

export async function assertDefaultSwitchPolicyEnabled(
  page: Page,
  policyName: string,
) {
  expect(getDefaultSwitch(page, policyName)).toBeEnabled();
}
