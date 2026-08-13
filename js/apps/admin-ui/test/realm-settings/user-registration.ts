import type { Page } from "@playwright/test";

export async function goToUserRegistrationTab(page: Page) {
  await page.getByTestId("rs-userRegistration-tab").click();
}

export async function goToDefaultGroupTab(page: Page) {
  await page.getByTestId("default-groups-tab").click();
}
