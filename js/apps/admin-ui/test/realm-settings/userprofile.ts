import type { Page } from "@playwright/test";

export async function goToUserProfileTab(page: Page) {
  await page.getByTestId("rs-user-profile-tab").click();
}

export async function goToAttributesTab(page: Page) {
  await page.getByTestId("attributesTab").click();
}

export async function clickCreateAttribute(page: Page) {
  await page.getByTestId("createAttributeBtn").click();
}

type AttributeForm = {
  name?: string;
  displayName?: string;
};

export async function fillAttributeForm(
  page: Page,
  { name, displayName }: AttributeForm,
) {
  if (name) await page.getByTestId("name").fill(name);
  if (displayName)
    await page.getByTestId("attributes-displayName").fill(displayName);
}

export async function clickCancelAttribute(page: Page) {
  await page.getByTestId("attribute-cancel").click();
}

export async function clickSaveAttribute(page: Page) {
  await page.getByTestId("attribute-create").click();
}

export async function clickAddValidator(page: Page) {
  await page.getByTestId("addValidator").click();
}

export async function clickSaveValidator(page: Page) {
  await page.getByTestId("save-validator-role-button").click();
}

export async function goToAttributeGroupsTab(page: Page) {
  await page.getByTestId("attributesGroupTab").click();
}

export async function switchOffIfOn(page: Page, selector: string) {
  if (await page.isChecked(selector)) {
    await page.locator(selector).click({ force: true });
  }
}
