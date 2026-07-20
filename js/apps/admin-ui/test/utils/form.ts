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
  await expect(element).toBeVisible();
  await expect(element).toBeEnabled();
  try {
    await element.click({ timeout: 3_000 });
  } catch (error) {
    if (
      error instanceof Error &&
      /Target page, context or browser has been closed/i.test(error.message)
    ) {
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
  await setSwitchState(switchElement, false);
}

export async function switchToggle(page: Page, id: string | Locator) {
  const switchElement = typeof id === "string" ? page.locator(id) : id;
  await setSwitchState(switchElement, !(await switchElement.isChecked()));
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

async function setSwitchState(switchElement: Locator, checked: boolean) {
  await expect(switchElement).toBeVisible();
  await expect(switchElement).toBeEnabled();

  for (let attempt = 0; attempt < 3; attempt++) {
    if ((await switchElement.isChecked()) === checked) {
      return;
    }

    try {
      if (checked) {
        await switchElement.check({ timeout: 3_000 });
      } else {
        await switchElement.uncheck({ timeout: 3_000 });
      }
    } catch (error) {
      if (
        error instanceof Error &&
        /Target page, context or browser has been closed/i.test(error.message)
      ) {
        throw error;
      }
      await clickSwitch(switchElement);
    }

    try {
      await expect
        .poll(async () => await switchElement.isChecked(), { timeout: 1_000 })
        .toBe(checked);
      return;
    } catch {
      // Retry a few times for transient UI states before failing.
    }

    const switchId = await switchElement.getAttribute("id");
    if (switchId) {
      const label = switchElement.page().locator(`label[for="${switchId}"]`);
      if ((await label.count()) > 0) {
        try {
          await label.first().click({ timeout: 3_000 });
          await expect
            .poll(async () => await switchElement.isChecked(), {
              timeout: 1_000,
            })
            .toBe(checked);
          return;
        } catch {
          // Continue to next retry if label click did not stabilize state.
        }
      }
    }
  }

  if (checked) {
    await expect(switchElement).toBeChecked();
  } else {
    await expect(switchElement).not.toBeChecked();
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
