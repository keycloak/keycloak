import { type Locator, type Page, expect } from "@playwright/test";

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

/**
 * Click a dropdown trigger and pick an option from a dropdown that closes after
 * selection, then wait for the close to complete.
 *
 * Waiting prevents race conditions where the dropdown-close re-render clobbers
 * a subsequent {@link Page.fill} on a controlled input.
 *
 * Use this for all {@link KeycloakSelect} dropdowns — including
 * `typeaheadMulti` variants — because `TypeaheadSelect`'s `onClick={toggle}`
 * on the PF5 `Select` wrapper closes the dropdown on every click.
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
// container div wrapping a PF5 MenuToggle (aria-expanded is on a child button).
// Use .first() so the union never resolves to two elements (e.g. a MenuToggle
// wrapper that itself has aria-expanded AND contains a child with it).
function findExpandable(element: Locator): Locator {
  const self = element.and(element.page().locator("[aria-expanded]"));
  return self.or(element.locator("[aria-expanded]").first()).first();
}

async function openDropdown(page: Page, field: Locator | string) {
  const element = typeof field === "string" ? page.locator(field) : field;
  await element.click();
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

export async function changeTimeUnit(
  page: Page,
  unit: "Minutes" | "Hours" | "Days",
  inputType: string,
) {
  await selectItem(page, inputType, unit);
}
