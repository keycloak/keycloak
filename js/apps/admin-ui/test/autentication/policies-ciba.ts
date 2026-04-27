import { type Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";

export async function goToCIBAPolicyTab(page: Page) {
  await page.getByTestId("tab-ciba-policy").click();
}

export async function assertBackchannelTokenDeliveryMode(
  page: Page,
  deliveryMode: string,
) {
  const deliveryModeSelect = getBackchannelTokenDeliveryModeSelect(page);
  await expect(deliveryModeSelect).toHaveText(deliveryMode);
}

export async function setBackchannelTokenDeliveryMode(
  page: Page,
  deliveryMode: string,
) {
  const deliveryModeSelect = getBackchannelTokenDeliveryModeSelect(page);
  await selectItem(page, deliveryModeSelect, deliveryMode);
}

function getBackchannelTokenDeliveryModeSelect(page: Page) {
  return page.getByLabel("Backchannel Token Delivery");
}

export async function assertExpiresInput(page: Page, value: number) {
  const input = getExpiresInput(page);
  await expect(input).toHaveValue(value.toString());
}

export async function setExpiresInput(page: Page, value: number) {
  const input = getExpiresInput(page);
  await input.fill(value.toString());
}

export async function clearExpiresInput(page: Page) {
  return getExpiresInput(page).clear();
}

export const expiresInput = "attributes.cibaExpiresIn";
function getExpiresInput(page: Page) {
  return page.getByTestId(expiresInput);
}

export async function assertIntervalInput(page: Page, value: number) {
  const input = getIntervalInput(page);
  await expect(input).toHaveValue(value.toString());
}

export async function setIntervalInput(page: Page, value: number) {
  const input = getIntervalInput(page);
  await input.fill(value.toString());
}

export async function clearIntervalInput(page: Page) {
  return getIntervalInput(page).clear();
}

export const intervalInput = "attributes.cibaInterval";
function getIntervalInput(page: Page) {
  return page.getByTestId(intervalInput);
}

export async function assertSaveButtonDisabled(page: Page) {
  const button = getSaveButton(page);
  await expect(button).toBeDisabled();
}

export async function assertSaveButtonEnabled(page: Page) {
  const button = getSaveButton(page);
  await expect(button).toBeEnabled();
}

function getSaveButton(page: Page) {
  return page.getByTestId("save");
}
