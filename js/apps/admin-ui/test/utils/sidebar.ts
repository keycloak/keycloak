import { Page } from "@playwright/test";

export async function goToRealm(page: Page, realmName: string) {
  await page.getByTestId("realmSelector").click();
  await page.getByRole("menuitem", { name: realmName }).click();
}

export async function goToClients(page: Page) {
  await page.getByTestId("nav-item-clients").click();
}

export async function goToAuthentication(page: Page) {
  await page.getByTestId("nav-item-authentication").click();
}
