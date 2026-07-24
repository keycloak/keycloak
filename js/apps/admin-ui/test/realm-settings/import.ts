import { type Locator, type Page, expect } from "@playwright/test";
import { switchOff, switchOn } from "../utils/form.ts";

export async function assertImportButtonDisabled(page: Page, disabled = true) {
  if (disabled) {
    await expect(page.getByTestId("confirm")).toBeDisabled();
  } else {
    await expect(page.getByTestId("confirm")).toBeEnabled();
  }
}

function getUsers(page: Page) {
  return page.getByTestId("users-checkbox");
}

export async function toggleUsers(page: Page, on = true) {
  if (on) {
    await switchOn(page, getUsers(page));
  } else {
    await switchOff(page, getUsers(page));
  }
}

function getGroups(page: Page) {
  return page.getByTestId("groups-checkbox");
}

export async function toggleGroups(page: Page, on = true) {
  if (on) {
    await switchOn(page, getGroups(page));
  } else {
    await switchOff(page, getGroups(page));
  }
}

function getClients(page: Page) {
  return page.getByTestId("clients-checkbox");
}

export async function toggleClients(page: Page, on = true) {
  if (on) {
    await switchOn(page, getClients(page));
  } else {
    await switchOff(page, getClients(page));
  }
}

function assertVisible(page: Page, locator: Locator, visible = true) {
  return expect(locator).toBeVisible({ visible });
}

export async function assertClientVisible(page: Page, visible = true) {
  await assertVisible(page, getClients(page), visible);
}

export async function assertUserVisible(page: Page, visible = true) {
  await assertVisible(page, getUsers(page), visible);
}

export async function assertGroupVisible(page: Page, visible = true) {
  await assertVisible(page, getGroups(page), visible);
}

export async function assertTextContent(page: Page, text: string) {
  await expect(page.locator(`text=${text}`)).toBeVisible();
}

export async function assertResultTable(page: Page, text: string) {
  await expect(
    page.getByRole("gridcell", { name: text, exact: true }),
  ).toBeVisible();
}

export async function closeModal(page: Page) {
  await page.getByTestId("close-button").click();
}

function getClearButton(page: Page) {
  return page.locator("button").filter({ hasText: /^Clear$/ });
}

export async function clickClearButton(page: Page) {
  await getClearButton(page).click();
}

export async function clickClearConfirmButton(page: Page) {
  await page.getByTestId("clear-button").click();
}

export async function assertClearButtonDisabled(page: Page, disabled = true) {
  if (disabled) {
    await expect(getClearButton(page)).toBeDisabled();
  } else {
    await expect(getClearButton(page)).toBeEnabled();
  }
}

function getTextArea(page: Page) {
  return page.getByRole("textbox", { name: "File content" });
}

export async function fillTextarea(page: Page, text: string) {
  await getTextArea(page).fill(text);
}

export async function assertTextAreaToHaveText(page: Page, text: string) {
  await expect(getTextArea(page)).toHaveText(text);
}
