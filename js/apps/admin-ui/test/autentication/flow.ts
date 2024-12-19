import { Page, expect } from "@playwright/test";
import { confirmModal } from "../utils/modal";
import AxeBuilder from "@axe-core/playwright";
import { selectItem } from "../utils/form";

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

export async function addPolicy(page: Page, value) {
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

export async function assertAxeViolations(page: Page) {
  const { violations } = await new AxeBuilder({ page }).analyze();
  expect(violations.length, violations.map((v) => v.help).join("\n")).toBe(0);
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
