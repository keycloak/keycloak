import { Page, expect } from "@playwright/test";

function getCurrentRealmItem(page: Page) {
  return page.getByTestId("nav-item-realms");
}

export async function goToRealmSection(page: Page) {
  await getCurrentRealmItem(page).click();
}

export async function clickCreateRealm(page: Page) {
  await getCurrentRealmItem(page).click();
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
