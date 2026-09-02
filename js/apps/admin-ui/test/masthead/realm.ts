import { type Page, expect } from "@playwright/test";
import { goToRealm, goToRealms } from "../utils/sidebar.ts";

function getCurrentRealmItem(page: Page) {
  return page.getByTestId("currentRealm");
}

export async function goToRealmSection(page: Page) {
  const realmName = await getCurrentRealmItem(page).textContent();
  await goToRealm(page, realmName!);
}

export async function clickCreateRealm(page: Page) {
  await goToRealms(page);
  await page.getByTestId("add-realm").click();
}

export async function fillRealmName(page: Page, realmName: string) {
  await page.getByTestId("realm").fill(realmName);
}

export async function clickCreateRealmForm(page: Page) {
  await page.getByTestId("create").click();
}

export async function clickClearResourceFile(page: Page) {
  await page.getByRole("button", { name: "Clear" }).click();
}

export async function clickConfirmClear(page: Page) {
  await page.getByTestId("clear-button").click();
}

export async function assertCurrentRealm(
  page: Page,
  realmName: string,
  not = false,
) {
  if (!not) {
    await expect(getCurrentRealmItem(page)).toContainText(realmName);
  } else {
    await expect(getCurrentRealmItem(page)).not.toContainText(realmName);
  }
}

export function getTextArea(page: Page) {
  return page.getByRole("textbox", { name: "File content" });
}

export async function assertTextAreaContains(page: Page, content: string) {
  await expect(getTextArea(page)).toContainText(content);
}
