import { type Page, expect } from "@playwright/test";
import { changeTimeUnit, switchOn } from "../utils/form.ts";

export async function goToTokensTab(page: Page) {
  await page.getByTestId("rs-tokens-tab").click();
}

function getAccessTokenLifespan(page: Page) {
  return page.getByTestId("access-token-lifespan-input");
}

function getParRequestUriLifespan(page: Page) {
  return page.getByTestId("par-request-uri-lifespan-input");
}

function getAccessTokenLifespanImplicitInput(page: Page) {
  return page.getByTestId("access-token-lifespan-implicit-input");
}

export async function populateTokensPage(page: Page) {
  await switchOn(page, page.getByTestId("revoke-refresh-token-switch"));

  await getAccessTokenLifespan(page).fill("1");
  await changeTimeUnit(page, "Days", "#kc-access-token-lifespan-select-menu");

  await getParRequestUriLifespan(page).fill("2");
  await changeTimeUnit(
    page,
    "Minutes",
    "#kc-access-token-lifespan-implicit-select-menu",
  );

  await getAccessTokenLifespanImplicitInput(page).fill("2");
  await changeTimeUnit(page, "Hours", "#par-request-uri-lifespan-select-menu");

  await page.getByTestId("client-login-timeout-input").fill("3");
  await changeTimeUnit(page, "Hours", "#kc-client-login-timeout-select-menu");

  await page.getByTestId("user-initiated-action-lifespan").fill("4");
  await changeTimeUnit(
    page,
    "Minutes",
    "#kc-user-initiated-action-lifespan-select-menu",
  );

  await page.getByTestId("default-admin-initated-input").fill("5");
  await changeTimeUnit(page, "Days", "#kc-default-admin-initiated-select-menu");

  await page.getByTestId("email-verification-input").fill("6");
  await changeTimeUnit(page, "Days", "#kc-email-verification-select-menu");

  await page.getByTestId("idp-email-verification-input").fill("7");
  await changeTimeUnit(page, "Days", "#kc-idp-email-verification-select-menu");

  await page.getByTestId("forgot-pw-input").fill("8");
  await changeTimeUnit(page, "Days", "#kc-forgot-pw-select-menu");

  await page.getByTestId("execute-actions-input").fill("9");
  await changeTimeUnit(page, "Days", "#kc-execute-actions-select-menu");
}

export async function clickSaveSessionsButton(page: Page) {
  await page.getByTestId("tokens-tab-save").click();
}

export async function assertAccessTokenLifespan(page: Page, value: string) {
  await expect(getAccessTokenLifespan(page)).toHaveValue(value);
}

export async function assertParRequestUriLifespan(page: Page, value: string) {
  await expect(getParRequestUriLifespan(page)).toHaveValue(value);
}

export async function assertAccessTokenLifespanImplicitInput(
  page: Page,
  value: string,
) {
  await expect(getAccessTokenLifespanImplicitInput(page)).toHaveValue(value);
}
