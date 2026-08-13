import { type Page, expect } from "@playwright/test";
import { switchOn } from "../utils/form.ts";

export async function goToEmailTab(page: Page) {
  await page.getByTestId("rs-email-tab").click();
}

export async function populateEmailPageNoAuth(page: Page) {
  await getFrom(page).fill("test@local.local");
  await getFromDisplayName(page).fill("Tester");
  await getReplyTo(page).fill("replyto-test@local.local");
  await getReplyToDisplayName(page).fill("ReplyTo Tester");
  await getEnvelopeFrom(page).fill("envelope-test@local.local");

  await getHost(page).fill("host.smtp.local");
  await getPort(page).fill("123");

  await getEnableSSL(page).check();
  await getEnableStartTLS(page).check();

  await getDebug(page).check();
}

export async function assertEmailPageNoAuth(page: Page) {
  await expect(getFrom(page)).toHaveValue("test@local.local");

  await expect(getFromDisplayName(page)).toHaveValue("Tester");
  await expect(getReplyTo(page)).toHaveValue("replyto-test@local.local");
  await expect(getReplyToDisplayName(page)).toHaveValue("ReplyTo Tester");
  await expect(getEnvelopeFrom(page)).toHaveValue("envelope-test@local.local");

  await expect(getHost(page)).toHaveValue("host.smtp.local");
  await expect(getPort(page)).toHaveValue("123");

  await expect(getEnableSSL(page)).toBeChecked();
  await expect(getEnableStartTLS(page)).toBeChecked();

  await expect(getDebug(page)).toBeChecked();
}

export async function populateEmailPageWithPasswordAuth(page: Page) {
  await switchOn(page, getAuth(page));
  await getUser(page).fill("user");
  await getAuthTypeBasic(page).check();
  await getPassword(page).fill("password");
}

export async function assertEmailPageWithPasswordAuth(page: Page) {
  await expect(getAuth(page)).toBeChecked();
  await expect(getUser(page)).toHaveValue("user");
  await expect(getAuthTypeBasic(page)).toBeChecked();
  await expect(getPassword(page)).toHaveValue("**********");
}

export async function populateEmailPageWithTokenAuth(page: Page) {
  await getAuthTypeToken(page).check();
  await getAuthTokenUrl(page).fill("https://auth.token.url");
  await getAuthTokenScope(page).fill("scope");
  await getAuthTokenClientId(page).fill("client-id");
  await getAuthTokenClientSecret(page).fill("client-secret");
}

export async function assertEmailPageWithTokenAuth(page: Page) {
  await expect(getAuthTypeToken(page)).toBeChecked();
  await expect(getAuthTokenUrl(page)).toHaveValue("https://auth.token.url");
  await expect(getAuthTokenScope(page)).toHaveValue("scope");
  await expect(getAuthTokenClientId(page)).toHaveValue("client-id");
  await expect(getAuthTokenClientSecret(page)).toHaveValue("**********");
}

function getFrom(page: Page) {
  return page.getByTestId("smtpServer.from");
}

function getFromDisplayName(page: Page) {
  return page.getByTestId("smtpServer.fromDisplayName");
}

function getReplyTo(page: Page) {
  return page.getByTestId("smtpServer.replyTo");
}

function getReplyToDisplayName(page: Page) {
  return page.getByTestId("smtpServer.replyToDisplayName");
}

function getEnvelopeFrom(page: Page) {
  return page.getByTestId("smtpServer.envelopeFrom");
}

function getHost(page: Page) {
  return page.getByTestId("smtpServer.host");
}

function getPort(page: Page) {
  return page.getByTestId("smtpServer.port");
}

function getEnableSSL(page: Page) {
  return page.getByTestId("enable-ssl");
}

function getEnableStartTLS(page: Page) {
  return page.getByTestId("enable-start-tls");
}

function getAuth(page: Page) {
  return page.getByTestId("smtpServer.auth");
}

function getUser(page: Page) {
  return page.getByTestId("smtpServer.user");
}

function getAuthTypeBasic(page: Page) {
  return page.getByTestId("smtpServer.authType.basic");
}

function getAuthTypeToken(page: Page) {
  return page.getByTestId("smtpServer.authType.token");
}

function getPassword(page: Page) {
  return page.getByTestId("smtpServer.password");
}

function getAuthTokenUrl(page: Page) {
  return page.getByTestId("smtpServer.authTokenUrl");
}

function getAuthTokenScope(page: Page) {
  return page.getByTestId("smtpServer.authTokenScope");
}

function getAuthTokenClientId(page: Page) {
  return page.getByTestId("smtpServer.authTokenClientId");
}

function getAuthTokenClientSecret(page: Page) {
  return page.getByTestId("smtpServer.authTokenClientSecret");
}

function getDebug(page: Page) {
  return page.getByTestId("enable-debug");
}

export async function clickSaveEmailButton(page: Page) {
  await page.getByTestId("email-tab-save").click();
}
