import { type Page, expect } from "@playwright/test";
import { selectItem, switchOn } from "../utils/form.ts";

export async function goToEventsConfig(page: Page) {
  await page.getByRole("link", { name: "Event configs" }).click();
}

async function removeLastAlert(page: Page) {
  await page.getByTestId("last-alert").locator("button").click();
}

async function turnOnUserEvents(page: Page) {
  await page.getByTestId("rs-events-tab").click();
  await switchOn(page, "[data-testid='eventsEnabled']");
  await page.getByTestId("save-user").click();
  await removeLastAlert(page);
}

async function turnOnAdminEvents(page: Page) {
  await page.getByTestId("rs-admin-events-tab").click();
  await switchOn(page, "[data-testid='adminEventsEnabled']");
  await page.getByTestId("save-admin").click();
  await removeLastAlert(page);
}

export async function enableSaveEvents(page: Page) {
  await turnOnUserEvents(page);
  await turnOnAdminEvents(page);
}

export async function clickSearchPanel(page: Page) {
  await page.getByTestId("dropdown-panel-btn").click();
}

type SearchParam = {
  userId?: string;
  client?: string;
  eventType?: string;
};

export async function fillSearchPanel(
  page: Page,
  { userId, client, eventType }: SearchParam,
) {
  if (userId) await page.getByTestId("userId-searchField").fill(userId);
  if (client) await page.getByTestId("client-searchField").fill(client);
  if (eventType) await selectItem(page, page.getByLabel("Select"), eventType);
}

export async function assertSearchButtonDisabled(page: Page, disabled = true) {
  if (disabled) {
    await expect(page.getByTestId("search-events-btn")).toBeDisabled();
  } else {
    await expect(page.getByTestId("search-events-btn")).toBeEnabled();
  }
}

export async function clickSearchButton(page: Page) {
  await page.getByTestId("search-events-btn").click();
}

export async function assertSearchChipGroupItemExist(
  page: Page,
  itemName: string,
  exist = true,
) {
  const locator = page.getByRole("group", { name: "User ID" });
  if (exist) {
    await expect(locator).toHaveText(`User ID${itemName}`);
  } else {
    await expect(locator).toBeHidden();
  }
}

export async function goToAdminEventsTab(page: Page) {
  await page.getByTestId("admin-events-tab").click();
}
