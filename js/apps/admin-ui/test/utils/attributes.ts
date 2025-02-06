import { Page, expect } from "@playwright/test";

export async function goToAttributesTab(page: Page) {
  await page.getByTestId("attributesTab").click();
}

function getAttribute(page: Page, index: number, key: string) {
  return page.locator(`input[name="attributes\\.${index}\\.${key}"]`);
}

export async function fillAttributeData(
  page: Page,
  key: string,
  value: string,
  index: number = 0,
) {
  await page.getByTestId("attributes-add-row").click();

  await getAttribute(page, index, "key").fill(key);
  await getAttribute(page, index, "value").fill(value);
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

export async function assertAttribute(
  page: Page,
  key: string,
  value: string,
  index: number = 0,
) {
  await expect(getAttribute(page, index, "key")).toHaveValue(key);
  await expect(getAttribute(page, index, "value")).toHaveValue(value);
}
