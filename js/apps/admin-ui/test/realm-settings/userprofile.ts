import { expect, type Page } from "@playwright/test";
import { switchOff } from "../utils/form.ts";

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
  const switchElement = page.locator(selector);
  if (await switchElement.isChecked()) {
    await switchOff(page, switchElement);
  }
}

export async function clickCreateUser(page: Page) {
  await expect(page.getByTestId("users-table-ready")).toBeVisible({
    timeout: 15_000,
  });
  const emptyAction = page.getByTestId("no-users-found-empty-action");
  const addUser = page.getByTestId("add-user");

  if ((await emptyAction.count()) > 0 && (await emptyAction.isVisible())) {
    await emptyAction.click();
    return;
  }

  await addUser.click();
}

export async function fillEmailAndOptionalUsername(
  page: Page,
  email: string,
  username: string,
) {
  await page.getByTestId("email").fill(email);
  const usernameField = page.getByTestId("username");
  await expect(
    usernameField,
    `Expected username field to be hidden when "Email as username" is enabled (fallback username: "${username}")`,
  ).toBeHidden();
}
