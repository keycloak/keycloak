import { type Page, expect } from "@playwright/test";
import { changeTimeUnit, switchOn } from "../utils/form.ts";

export async function goToSessionsTab(page: Page) {
  await page.getByTestId("rs-sessions-tab").click();
}

function getSsoSessionIdleInput(page: Page) {
  return page.getByTestId("sso-session-idle-input");
}

function getSsoSessionMaxInput(page: Page) {
  return page.getByTestId("sso-session-max-input");
}

function getSsoSessionIdleRememberMe(page: Page) {
  return page.getByTestId("sso-session-idle-remember-me-input");
}

function getSsoSessionMaxRememberMe(page: Page) {
  return page.getByTestId("sso-session-max-remember-me-input");
}

export async function populateSessionsPage(page: Page) {
  await getSsoSessionIdleInput(page).fill("1");
  await changeTimeUnit(page, "Minutes", "#kc-sso-session-idle-select-menu");

  await getSsoSessionMaxInput(page).fill("2");
  await changeTimeUnit(page, "Hours", "#kc-sso-session-max-select-menu");

  await getSsoSessionIdleRememberMe(page).fill("3");
  await changeTimeUnit(
    page,
    "Days",
    "#kc-sso-session-idle-remember-me-select-menu",
  );

  await getSsoSessionMaxRememberMe(page).fill("4");
  await changeTimeUnit(
    page,
    "Minutes",
    "#kc-sso-session-max-remember-me-select-menu",
  );

  await page.getByTestId("client-session-idle-input").fill("5");
  await changeTimeUnit(page, "Hours", "#kc-client-session-idle-select-menu");

  await page.getByTestId("client-session-max-input").fill("6");
  await changeTimeUnit(page, "Days", "#kc-client-session-max-select-menu");

  await page.getByTestId("offline-session-idle-input").fill("7");
  await switchOn(page, "#kc-offline-session-max");

  await page.getByTestId("login-timeout-input").fill("9");
  await changeTimeUnit(page, "Minutes", "#kc-login-timeout-select-menu");

  await page.getByTestId("login-action-timeout-input").fill("10");
  await changeTimeUnit(page, "Days", "#kc-login-action-timeout-select-menu");
}

export async function clickSaveSessionsButton(page: Page) {
  await page.getByTestId("sessions-tab-save").click();
}

export async function assertSsoSessionIdleInput(page: Page, value: string) {
  await expect(getSsoSessionIdleInput(page)).toHaveValue(value);
}

export async function assertSsoSessionMaxInput(page: Page, value: string) {
  await expect(getSsoSessionMaxInput(page)).toHaveValue(value);
}

export async function assertSsoSessionIdleRememberMe(
  page: Page,
  value: string,
) {
  await expect(getSsoSessionIdleRememberMe(page)).toHaveValue(value);
}

export async function assertSsoSessionMaxRememberMe(page: Page, value: string) {
  await expect(getSsoSessionMaxRememberMe(page)).toHaveValue(value);
}
