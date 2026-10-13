import type { Page } from "@playwright/test";
import { clickSelectRow } from "../utils/table.ts";

export async function goToRealmEventsTab(page: Page) {
  await page.getByTestId("rs-realm-events-tab").click();
}

export async function goToEventsTab(page: Page) {
  await page.getByTestId("rs-events-tab").click();
}

export async function clickSaveEventsConfig(page: Page) {
  await page.getByTestId("save-user").click();
}

export async function clickClearEvents(page: Page) {
  await page.getByTestId("clear-user-events").click();
}

export async function addSavedEventTypes(page: Page, eventTypes: string[]) {
  await page.getByTestId("addTypes").click();
  for (const eventType of eventTypes) {
    await clickSelectRow(page, "Add types", eventType);
  }
  await page.getByTestId("addEventTypeConfirm").click();
}

export async function fillEventListener(page: Page, listener: string) {
  const eventListener = page.getByRole("combobox", { name: "Type to filter" });
  await eventListener.click();
  await eventListener.fill(listener);
  await page.getByRole("option", { name: listener }).click();
  await page.keyboard.press("Escape");
}

export async function clickRevertButton(page: Page) {
  await page.getByTestId("revertEventListenerBtn").click();
}

export async function clickSaveEventsListener(page: Page) {
  await page.getByTestId("saveEventListenerBtn").click();
}

export async function clickRemoveListener(page: Page, listener: string) {
  await page.getByRole("button", { name: `close ${listener}` }).click();
}
