import type { Page } from "@playwright/test";

export async function clickCancelButton(page: Page) {
  await page.getByTestId("user-creation-revert").click();
}

export async function clickSaveButton(page: Page) {
  await page.getByTestId("user-creation-save").click();
}

export async function clickAddUserButton(page: Page) {
  await page.getByTestId("no-users-found-empty-action").click();
}

export async function fillUserForm(
  page: Page,
  user: {
    username?: string;
    email?: string;
    firstName?: string;
    lastName?: string;
  },
) {
  const { username, email, firstName, lastName } = user;
  if (username) await page.getByTestId("username").fill(username);
  if (email) await page.getByTestId("email").fill(email);
  if (firstName) await page.getByTestId("firstName").fill(firstName);
  if (lastName) await page.getByTestId("lastName").fill(lastName);
}

export async function goToGroupTab(page: Page) {
  await page.getByTestId("user-groups-tab").click();
}

export async function joinGroup(page: Page, groups: string[], empty = false) {
  if (empty) {
    await page.getByTestId("no-groups-empty-action").click();
  } else {
    await page.getByTestId("join-groups-button").click();
  }

  for (const group of groups) {
    await page.getByTestId(`${group}-check`).click();
  }

  await page.getByTestId("join-button").click();
}
