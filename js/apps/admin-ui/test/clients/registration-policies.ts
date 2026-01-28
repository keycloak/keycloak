import type { Page } from "@playwright/test";

export async function goToClientRegistrationTab(page: Page) {
  await page.getByTestId("registration").click();
}

export async function goToAuthenticatedSubTab(page: Page) {
  await page.getByTestId("authenticated").click();
}

export async function clickCreateAnonymousPolicy(page: Page) {
  await page.getByTestId("createPolicy-anonymous").click();
}

export async function clickCreateAuthenticatedPolicy(page: Page) {
  await page.getByTestId("createPolicy-authenticated").click();
}

type Policy = { name: string };

export async function createPolicy(page: Page, type: string, data: Policy) {
  await page.getByTestId(type).getByText(type, { exact: true }).click();
  await fillPolicyForm(page, data);
}

export async function fillPolicyForm(page: Page, data: Policy) {
  await page.getByTestId("name").fill(data.name);
}
