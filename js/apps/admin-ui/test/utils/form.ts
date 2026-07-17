import { expect, Locator, Page } from "@playwright/test";
import { clickSelectRow } from "./table.ts";

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
  if (await switchElement.isChecked()) return;
  await clickSwitch(switchElement);
  if (!(await switchElement.isChecked())) {
    await switchElement.click({ force: true, timeout: 3_000 });
  }
}

export async function switchOff(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  if (!(await switchElement.isChecked())) return;
  await clickSwitch(switchElement);
}

export async function switchToggle(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  const wasChecked = await switchElement.isChecked();
  await clickSwitch(switchElement);
  const isChecked = await switchElement.isChecked();
  if (isChecked === wasChecked) {
    await switchElement.click({ force: true, timeout: 3_000 });
  }
  if (wasChecked) {
    await expect(switchElement).not.toBeChecked();
  } else {
    await expect(switchElement).toBeChecked();
  }
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

async function clickSwitch(switchElement: Locator) {
  await expect(switchElement).toBeVisible();
  await expect(switchElement).toBeEnabled();
  try {
    await switchElement.click({ timeout: 3_000 });
  } catch (error) {
    if (
      error instanceof Error &&
      /Target page, context or browser has been closed/i.test(error.message)
    ) {
      throw error;
    }
    // Fallback for transient overlays/animations while preserving deterministic state checks.
    await switchElement.click({ force: true, timeout: 3_000 });
  }
}

export async function selectClient(page: Page, clientName: string) {
  await page.getByTestId("select-client-button").click();
  const modal = page.getByTestId("select-client-modal");
  await modal.locator("table tbody").waitFor();
  await modal.getByPlaceholder("Search for client").fill(clientName);
  await page.keyboard.press("Enter");
  await modal
    .getByRole("gridcell", { name: clientName, exact: true })
    .waitFor();
  await clickSelectRow(page, "Clients", clientName);
  await page.getByTestId("confirm").click();
}

export async function changeTimeUnit(
  page: Page,
  unit: "Seconds" | "Minutes" | "Hours" | "Days",
  inputType: string,
) {
  await page.locator(inputType).click();
  await clickOption(page, unit);
}
