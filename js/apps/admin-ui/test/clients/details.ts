import type { Page } from "@playwright/test";
import { selectItem, assertSelectValue } from "../utils/form.ts";

function getPkceEnabledSwitch(page: Page) {
  return page.locator("#kc-pkce-enabled-switch");
}

function getKeyForCodeExchangeInput(page: Page) {
  return page.locator("#keyForCodeExchange");
}

export async function requirePkce(page: Page, required: boolean) {
  const pkceSwitch = getPkceEnabledSwitch(page);
  const isChecked = await pkceSwitch.isChecked();
  if (isChecked !== required) {
    await pkceSwitch.click();
  }
}

export async function selectKeyForCodeExchangeInput(page: Page, value: string) {
  await requirePkce(page, true);
  // Then select the method
  await selectItem(page, getKeyForCodeExchangeInput(page), value);
}

export async function assertKeyForCodeExchangeInput(page: Page, value: string) {
  await assertSelectValue(getKeyForCodeExchangeInput(page), value);
}
