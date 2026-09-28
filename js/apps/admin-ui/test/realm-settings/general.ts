import { type Page, expect } from "@playwright/test";
import { SERVER_URL } from "../utils/constants.ts";
import { clickSwitch, selectItem, switchOff, switchOn } from "../utils/form.ts";
import { confirmModal } from "../utils/modal.ts";

export async function clickSaveRealm(page: Page) {
  await page.getByTestId("realmSettingsGeneralTab-save").click();
}

function getOpenIdEndpointConfigurationLink(page: Page) {
  return page.getByRole("link", {
    name: "OpenID Endpoint Configuration",
  });
}

export async function assertOpenIdEndpointConfigurationLink(
  page: Page,
  realmName: string,
) {
  const openIdEndpointLink = getOpenIdEndpointConfigurationLink(page);
  await expect(openIdEndpointLink).toHaveAttribute(
    "href",
    `${SERVER_URL}/realms/${realmName}/.well-known/openid-configuration`,
  );
  await expect(openIdEndpointLink).toHaveAttribute("target", "_blank");
  await expect(openIdEndpointLink).toHaveAttribute(
    "rel",
    "noreferrer noopener",
  );
}

export async function assertOpenIdEndpointConfigurationReachable(page: Page) {
  const link =
    await getOpenIdEndpointConfigurationLink(page).getAttribute("href");

  if (!link) {
    throw new Error("OpenID Endpoint Configuration link is missing");
  }

  const response = await page.request.get(link);
  expect(response.status()).toBe(200);
}

function getDisplayName(page: Page) {
  return page.getByTestId("displayName");
}

export async function fillDisplayName(page: Page, value: string) {
  await getDisplayName(page).fill(value);
}

export async function assertDisplayName(page: Page, value: string) {
  await expect(getDisplayName(page)).toHaveValue(value);
}

function getFrontendURL(page: Page) {
  return page.getByTestId("attributes.frontendUrl");
}

export async function fillFrontendURL(page: Page, value: string) {
  await getFrontendURL(page).fill(value);
}

export async function assertFrontendURL(page: Page, value: string) {
  await expect(getFrontendURL(page)).toHaveValue(value);
}

export async function fillRequireSSL(page: Page, value: string) {
  await selectItem(page, "#sslRequired", value);
}

export async function assertRequireSSL(page: Page, value: string) {
  await expect(page.locator("#sslRequired")).toHaveText(value);
}

export async function clickRevertButton(page: Page) {
  await page.getByTestId("realmSettingsGeneralTab-revert").click();
}

export async function enableUserManagedAccess(page: Page) {
  await switchOn(page, "#userManagedAccessAllowed");
}

export async function disableUserManagedAccess(page: Page) {
  await switchOff(page, "#userManagedAccessAllowed");
}

function getRealmSwitch(page: Page, realmName: string) {
  return page.locator(`#${realmName}-switch`);
}

export async function enableRealm(page: Page, realmName: string) {
  const realmSwitch = getRealmSwitch(page, realmName);
  await expect(realmSwitch).not.toBeChecked();
  await switchOn(page, realmSwitch);
}

export async function disableRealm(page: Page, realmName: string) {
  await clickSwitch(page, getRealmSwitch(page, realmName));
  await confirmModal(page);
}

export async function clearRealmId(page: Page) {
  await page.getByRole("textbox", { name: "Copyable input" }).fill("");
}

export async function assertRequiredFieldMessage(page: Page) {
  await expect(page.getByText("Required field")).toBeVisible();
}
