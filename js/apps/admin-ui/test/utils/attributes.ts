import { Page, expect } from "@playwright/test";

export async function goToAttributesTab(page: Page) {
  await page.getByTestId("attributesTab").click();
}

export async function fillAttributeData(
  page: Page,
  key: string,
  value: string,
) {
  await page.getByTestId("attributes-add-row").click();
  await page.getByTestId("attributes-key").fill(key);
  await page.getByTestId("attributes-value").fill(value);
}

export async function deleteAttribute(page: Page, row: number) {
  await page.getByTestId("attributes-remove").nth(row).click();
}

export async function clickAttributeSaveButton(page: Page) {
  await page.getByTestId("attributes-save").click();
}

export async function assertAttributeLength(page: Page, length: number) {
  const rows = await page.getByTestId("attributes-key").all();
  expect(rows.length).toBe(length);
}
