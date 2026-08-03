import { expect, Locator, Page } from "@playwright/test";
import { clickSelectRow } from "./table.ts";

function isPageClosedError(error: unknown): boolean {
  return (
    error instanceof Error &&
    /Target page, context or browser has been closed/i.test(error.message)
  );
}

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
  await expect(element).toBeVisible();
  await expect(element).toBeEnabled();
  try {
    await element.click({ timeout: 3_000 });
  } catch (error) {
    if (isPageClosedError(error)) {
      throw error;
    }
    await element.click({ force: true, timeout: 3_000 });
  }
  await page.getByRole("option", { name: value, exact: true }).click();
}

export async function assertSelectValue(field: Locator, value: string) {
  const text = field;
  await expect(text).toHaveText(value);
}

export async function switchOn(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await setSwitchState(switchElement, true);
}

export async function switchOff(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await expect(switchElement).toBeChecked();
  await setSwitchState(switchElement, false);
}

export async function ensureSwitchOff(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await setSwitchState(switchElement, false);
}

export async function switchToggle(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await setSwitchState(switchElement, !(await switchElement.isChecked()));
}

export async function clickSwitch(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await clickSwitchElement(switchElement);
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

async function clickSwitchElement(switchElement: Locator) {
  await expect(switchElement).toBeVisible();

  const switchId = await switchElement.getAttribute("id");
  const label = switchId
    ? switchElement.page().locator(`label[for="${switchId}"]`).first()
    : undefined;

  try {
    await switchElement.click({ timeout: 3_000 });
    return;
  } catch (error) {
    if (isPageClosedError(error)) {
      throw error;
    }
  }

  if (label && (await label.count()) > 0) {
    try {
      await label.click({ force: true, timeout: 3_000 });
      return;
    } catch (error) {
      if (isPageClosedError(error)) {
        throw error;
      }
    }
  }

  await switchElement.click({ force: true, timeout: 3_000 });
}

async function setSwitchState(switchElement: Locator, checked: boolean) {
  for (let attempt = 0; attempt < 3; attempt++) {
    await expect(switchElement).toBeVisible();

    if ((await switchElement.isChecked()) === checked) {
      return;
    }

    try {
      if (checked) {
        await switchElement.check({ force: true, timeout: 3_000 });
      } else {
        await switchElement.uncheck({ force: true, timeout: 3_000 });
      }
    } catch (error) {
      if (isPageClosedError(error)) {
        throw error;
      }
    }

    if (await waitForSwitchState(switchElement, checked)) {
      return;
    }

    await clickSwitchElement(switchElement);
    if (await waitForSwitchState(switchElement, checked)) {
      return;
    }

    // Some switches only respond to keyboard interactions after focus.
    try {
      await switchElement.focus({ timeout: 2_000 });
      await switchElement.page().keyboard.press("Space");
    } catch (error) {
      if (isPageClosedError(error)) {
        throw error;
      }
    }

    if (await waitForSwitchState(switchElement, checked)) {
      return;
    }
  }

  if (checked) {
    await expect(switchElement).toBeChecked();
  } else {
    await expect(switchElement).not.toBeChecked();
  }
}

async function waitForSwitchState(switchElement: Locator, checked: boolean) {
  try {
    await expect
      .poll(async () => await switchElement.isChecked(), { timeout: 2_000 })
      .toBe(checked);
    return true;
  } catch {
    return false;
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
