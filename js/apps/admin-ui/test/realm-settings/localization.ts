import type { Locator, Page } from "@playwright/test";
import { selectMultiItem, switchOff, switchOn } from "../utils/form.ts";

export async function goToLocalizationTab(page: Page) {
  await page.getByTestId("rs-localization-tab").click();
}

export async function goToLocalesSubTab(page: Page) {
  await page.getByTestId("rs-localization-locales-sub-tab").click();
}

export async function goToRealmOverridesSubTab(page: Page) {
  await page.getByTestId("rs-localization-realm-overrides-tab").click();
}

export async function switchInternationalization(
  page: Page,
  on: boolean = true,
) {
  const localeSwitch = page.getByTestId("internationalizationEnabled");
  if (on) {
    await switchOn(page, localeSwitch);
  } else {
    await switchOff(page, localeSwitch);
  }
}

export async function selectLocale(page: Page, locale: string) {
  await selectMultiItem(page, "#supportedLocales", locale);
}

export async function clicksSaveLocalization(page: Page) {
  await page.getByTestId("localization-tab-save").click();
}

export async function addBundle(page: Page, key: string, value: string) {
  await page.getByTestId("add-translationBtn").click();
  await page.getByTestId("key").fill(key);
  await page.getByTestId("value").fill(value);
}

export async function clickCreateButton(page: Page) {
  await page.getByTestId("add-translation-confirm-button").click();
}

export async function editBundle(row: Locator, value: string) {
  await row.getByRole("button", { name: "Edit button", exact: true }).click();
  await row
    .getByRole("textbox", { name: "Edit translation value" })
    .fill(value);
}

export async function clickConfirmEditButton(row: Locator) {
  await row.getByRole("button", { name: "Accept editing button" }).click();
}
