import { type Locator, type Page, expect } from "@playwright/test";
import { clickSelectRow } from "./table.ts";

function isPageClosedError(error: unknown): boolean {
  return (
    error instanceof Error &&
    /Target page, context or browser has been closed/i.test(error.message)
  );
}

function toErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message.replace(/\s+/g, " ").trim();
  }
  return String(error);
}

async function getSwitchDebugName(switchElement: Locator): Promise<string> {
  return (
    (await switchElement.getAttribute("data-testid")) ??
    (await switchElement.getAttribute("id")) ??
    "unknown-switch"
  );
}

async function logSwitchDebug(switchElement: Locator, messageParts: string[]) {
  if (messageParts.length === 0) {
    return;
  }
  const switchName = await getSwitchDebugName(switchElement);
  console.info(`[switch:${switchName}] ${messageParts.join(" | ")}`);
}

export async function assertRequiredFieldError(page: Page, field: string) {
  await expect(page.getByTestId(field + "-helper")).toContainText(/required/i);
}

export async function assertFieldError(
  page: Page,
  field: string,
  text: string,
) {
  await expect(page.getByTestId(field + "-helper")).toContainText(text);
}

/**
 * Click a dropdown trigger and pick an option from a dropdown that closes after
 * selection, then wait for the close to complete.
 *
 * Waiting prevents race conditions where the dropdown-close re-render clobbers
 * a subsequent {@link Page.fill} on a controlled input.
 *
 * Use this for all {@link KeycloakSelect} dropdowns — including
 * `typeaheadMulti` variants — because `TypeaheadSelect`'s `onClick={toggle}`
 * on the PF6 `Select` wrapper closes the dropdown on every click.
 *
 * Use {@link selectMultiItem} only for dropdowns that genuinely stay open after
 * selection: `SelectControl` (which calls `event.stopPropagation()` in
 * `onSelect`) and `UserSelect` (which has its own open-state management).
 */
export async function selectItem(
  page: Page,
  field: Locator | string,
  value: string | Locator,
) {
  await openDropdown(page, field);
  const option = toOptionLocator(page, value);
  await option.click();
  await expect(
    option,
    "selectItem: dropdown stayed open after selection — " +
      "use selectMultiItem() for multi-select (typeaheadMulti) dropdowns",
  ).toBeHidden();
}

/**
 * Click a dropdown trigger and pick one or more options from a multi-select
 * dropdown that stays open after each selection, then click the toggle to close.
 *
 * Only needed for components whose `onSelect` does **not** close the dropdown:
 * - `SelectControl` / `TypeaheadSelectControl` with `typeaheadMulti`
 * - `UserSelect` with `typeaheadMulti`
 *
 * For `KeycloakSelect` (including `typeaheadMulti`), use {@link selectItem} —
 * it always closes after selection.
 *
 * ```ts
 * await selectMultiItem(page, "#supportedLocales", "Danish", "German");
 * ```
 */
export async function selectMultiItem(
  page: Page,
  field: Locator | string,
  ...values: (string | Locator)[]
) {
  const element = typeof field === "string" ? page.locator(field) : field;
  await openDropdown(page, element);
  for (const value of values) {
    await toOptionLocator(page, value).click();
  }
  const expandable = findExpandable(element);
  await expect(
    expandable,
    "selectMultiItem: dropdown closed after selection — " +
      "use selectItem() for single-select dropdowns",
  ).toHaveAttribute("aria-expanded", "true");
  // Click the toggle to close instead of pressing Escape, which could bubble
  // up and close a parent modal dialog if focus is not on the typeahead input.
  await element.click();
  await expect(expandable).not.toHaveAttribute("aria-expanded", "true");
}

// The field element may be a combobox input (has aria-expanded itself) or a
// container div wrapping a PF6 MenuToggle (aria-expanded is on a child button).
// Use .first() so the union never resolves to two elements (e.g. a MenuToggle
// wrapper that itself has aria-expanded AND contains a child with it).
function findExpandable(element: Locator): Locator {
  const self = element.and(element.page().locator("[aria-expanded]"));
  return self.or(element.locator("[aria-expanded]").first()).first();
}

async function openDropdown(page: Page, field: Locator | string) {
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
}

function toOptionLocator(page: Page, value: string | Locator) {
  return typeof value === "string"
    ? page.getByRole("option", { name: value, exact: true })
    : value;
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

type SwitchClickResult = {
  strategy: "direct-click" | "label-force-click" | "force-click";
  failures: string[];
};

async function clickSwitchElement(
  switchElement: Locator,
): Promise<SwitchClickResult> {
  await expect(switchElement).toBeVisible();
  const failures: string[] = [];

  const switchId = await switchElement.getAttribute("id");
  const label = switchId
    ? switchElement.page().locator(`label[for="${switchId}"]`).first()
    : undefined;

  try {
    await switchElement.click({ timeout: 3_000 });
    return { strategy: "direct-click", failures };
  } catch (error) {
    if (isPageClosedError(error)) {
      throw error;
    }
    failures.push(`direct-click failed: ${toErrorMessage(error)}`);
  }

  if (label && (await label.count()) > 0) {
    try {
      await label.click({ force: true, timeout: 3_000 });
      return { strategy: "label-force-click", failures };
    } catch (error) {
      if (isPageClosedError(error)) {
        throw error;
      }
      failures.push(`label-force-click failed: ${toErrorMessage(error)}`);
    }
  }

  try {
    await switchElement.click({ force: true, timeout: 3_000 });
    return { strategy: "force-click", failures };
  } catch (error) {
    if (isPageClosedError(error)) {
      throw error;
    }
    failures.push(`force-click failed: ${toErrorMessage(error)}`);
    throw error;
  }
}

async function setSwitchState(switchElement: Locator, checked: boolean) {
  const debugEvents: string[] = [];
  const targetState = checked ? "checked" : "unchecked";

  for (let attempt = 0; attempt < 3; attempt++) {
    const attemptNumber = attempt + 1;
    await expect(switchElement).toBeVisible();

    if ((await switchElement.isChecked()) === checked) {
      if (debugEvents.length > 0) {
        await logSwitchDebug(switchElement, [
          ...debugEvents,
          `state already ${targetState} on attempt ${attemptNumber}`,
        ]);
      }
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
      debugEvents.push(
        `attempt ${attemptNumber}: ${
          checked ? "check" : "uncheck"
        } failed (${toErrorMessage(error)})`,
      );
    }

    if (await waitForSwitchState(switchElement, checked)) {
      if (debugEvents.length > 0) {
        await logSwitchDebug(switchElement, [
          ...debugEvents,
          `state reached via ${checked ? "check" : "uncheck"} on attempt ${attemptNumber}`,
        ]);
      }
      return;
    }

    try {
      const clickResult = await clickSwitchElement(switchElement);
      if (
        clickResult.strategy !== "direct-click" ||
        clickResult.failures.length > 0
      ) {
        debugEvents.push(
          `attempt ${attemptNumber}: click strategy ${clickResult.strategy}` +
            (clickResult.failures.length > 0
              ? ` after ${clickResult.failures.join("; ")}`
              : ""),
        );
      }
    } catch (error) {
      if (isPageClosedError(error)) {
        throw error;
      }
      debugEvents.push(
        `attempt ${attemptNumber}: click strategy failed (${toErrorMessage(error)})`,
      );
    }

    if (await waitForSwitchState(switchElement, checked)) {
      if (debugEvents.length > 0) {
        await logSwitchDebug(switchElement, [
          ...debugEvents,
          `state reached after click on attempt ${attemptNumber}`,
        ]);
      }
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
      debugEvents.push(
        `attempt ${attemptNumber}: keyboard toggle failed (${toErrorMessage(error)})`,
      );
    }

    if (await waitForSwitchState(switchElement, checked)) {
      if (debugEvents.length > 0) {
        await logSwitchDebug(switchElement, [
          ...debugEvents,
          `state reached after keyboard toggle on attempt ${attemptNumber}`,
        ]);
      }
      return;
    }

    debugEvents.push(
      `attempt ${attemptNumber}: state still not ${targetState} after all strategies`,
    );
  }

  await logSwitchDebug(switchElement, [
    ...debugEvents,
    `failed to set state to ${targetState} after 3 attempts`,
  ]);
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
  await selectItem(page, inputType, unit);
}
