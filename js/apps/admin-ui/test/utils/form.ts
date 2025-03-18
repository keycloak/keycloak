import { expect, Locator, Page } from "@playwright/test";

export async function assertRequiredFieldError(page: Page, field: string) {
  await expect(page.getByTestId(field + "-helper")).toHaveText(/required/i);
}

export async function assertFieldError(
  page: Page,
  field: string,
  text: string,
) {
  await expect(page.getByTestId(field + "-helper")).toHaveText(text);
}

export async function selectItem(
  page: Page,
  field: Locator | string,
  value: string,
) {
  const element = typeof field === "string" ? page.locator(field) : field;
  await element.click();
  await page.getByRole("option", { name: value, exact: true }).click();
}

export async function assertSelectValue(field: Locator, value: string) {
  const text = field;
  await expect(text).toHaveText(value);
}

export async function switchOn(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await switchElement.click({ force: true });
  await expect(switchElement).toBeChecked();
}

export async function switchOff(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await expect(switchElement).toBeChecked();
  await switchElement.click({ force: true });
}

export async function switchToggle(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await switchElement.click({ force: true });
}

export async function assertSwitchIsChecked(
  page: Page,
  id: string,
  not = false,
) {
  if (not) {
    await expect(page.locator(id)).not.toBeChecked();
  } else {
    await expect(page.locator(id)).toBeChecked();
  }
}

function getSaveButton(page: Page) {
  return page.getByTestId("save");
}

export async function clickSaveButton(page: Page) {
  await getSaveButton(page).click();
}

export async function assertSaveButtonIsDisabled(page: Page) {
  await expect(getSaveButton(page)).toBeDisabled();
}

export async function clickCancelButton(page: Page) {
  await page.getByTestId("cancel").click();
}

async function clickOption(page: Page, option: string) {
  await page.getByRole("option", { name: option }).click();
}

export async function changeTimeUnit(
  page: Page,
  unit: "Minutes" | "Hours" | "Days",
  inputType: string,
) {
  await page.locator(inputType).click();
  await clickOption(page, unit);
}
