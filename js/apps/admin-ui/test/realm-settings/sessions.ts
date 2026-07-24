import { expect, type Page } from "@playwright/test";
import { changeTimeUnit } from "../utils/form.ts";

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

export async function populateSessionsPageRememberMeDisabled(page: Page) {
  await page.getByTestId("sso-session-idle-input").fill("5");
  await changeTimeUnit(page, "Hours", "#kc-sso-session-idle-select-menu");

  await page.getByTestId("sso-session-max-input").fill("2");
  await changeTimeUnit(page, "Hours", "#kc-sso-session-max-select-menu");

  await page.getByTestId("client-session-idle-input").fill("4");
  await changeTimeUnit(page, "Hours", "#kc-client-session-idle-select-menu");

  await page.getByTestId("client-session-max-input").fill("1");
  await changeTimeUnit(page, "Hours", "#kc-client-session-max-select-menu");
}

export async function populateSessionsPageRememberMeEnabled(page: Page) {
  await populateSessionsPageRememberMeDisabled(page);
  await page.getByTestId("sso-session-idle-remember-me-input").fill("3");
  await changeTimeUnit(
    page,
    "Days",
    "#kc-sso-session-idle-remember-me-select-menu",
  );

  await page.getByTestId("sso-session-max-remember-me-input").fill("4");
  await changeTimeUnit(
    page,
    "Minutes",
    "#kc-sso-session-max-remember-me-select-menu",
  );
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
