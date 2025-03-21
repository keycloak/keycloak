import { Page } from "@playwright/test";

export async function clickCreateRealm(page: Page) {
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
