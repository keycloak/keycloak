import { type Page, expect } from "@playwright/test";

const SERVER_URL = "http://localhost:8080";
const discoveryUrl = `${SERVER_URL}/realms/master/.well-known/openid-configuration`;
const authorizationUrl = `${SERVER_URL}/realms/master/protocol/openid-connect/auth`;

const samlDiscoveryUrl = `${SERVER_URL}/realms/master/protocol/saml/descriptor`;

async function clickProviderCard(page: Page, providerName: string) {
  await page.getByTestId(`${providerName}-card`).click();
}

async function fillDiscoveryUrl(page: Page, discoveryUrl: string) {
  await page.getByTestId("discoveryEndpoint").fill(discoveryUrl);
  await expect(page.getByTestId("playwright-result")).toHaveText("success");
}

async function clickAddButton(page: Page) {
  await page.getByTestId("createProvider").click();
}

export async function createOIDCProvider(
  page: Page,
  providerName: string,
  secret: string,
) {
  await clickProviderCard(page, providerName);
  await fillDiscoveryUrl(page, discoveryUrl);
  await page.getByTestId("displayName").fill(providerName);
  await page.getByTestId("config.clientId").fill(providerName);
  await page.getByTestId("config.clientSecret").fill(secret);
  await clickAddButton(page);
}

export async function createSAMLProvider(
  page: Page,
  providerName: string,
  displayName: string,
) {
  await clickProviderCard(page, providerName);
  await fillDiscoveryUrl(page, samlDiscoveryUrl);
  await page.getByTestId("displayName").fill(displayName);
  await clickAddButton(page);
}

export async function createSPIFFEProvider(
  page: Page,
  providerName: string,
  trustDomain: string,
  bundleEndpoint: string,
) {
  await clickProviderCard(page, providerName);
  await page.getByTestId("config.trustDomain").fill(trustDomain);
  await page.getByTestId("config.bundleEndpoint").fill(bundleEndpoint);
  await clickAddButton(page);
}

export async function createKubernetesProvider(
  page: Page,
  providerName: string,
  issuerUrl: string,
) {
  await clickProviderCard(page, providerName);
  await page.getByTestId("config.issuer").fill(issuerUrl);
  await clickAddButton(page);
}

export async function assertAuthorizationUrl(page: Page) {
  await expect(page.getByTestId("config.authorizationUrl")).toHaveValue(
    authorizationUrl,
  );
}

type UrlType =
  | "authorization"
  | "token"
  | "tokenIntrospection"
  | "singleSignOnService"
  | "singleLogoutService";

export async function setUrl(page: Page, urlType: UrlType, value: string) {
  await page.getByTestId(`config.${urlType}Url`).fill(value);
}

export async function assertInvalidUrlNotification(
  page: Page,
  urlType: UrlType,
) {
  await expect(page.getByTestId("last-alert")).toHaveText(
    `Could not update the provider The url [${urlType}${urlType.startsWith("single") ? "U" : "_u"}rl] is malformed`,
  );
}

export async function clickSaveButton(page: Page) {
  await page.getByTestId("idp-details-save").click();
}

export async function clickRevertButton(page: Page) {
  await page.getByTestId("idp-details-revert").click();
}

async function assertElementExists(
  page: Page,
  locator: string,
  exist: boolean = true,
) {
  if (exist) {
    await expect(page.locator(locator)).toBeVisible();
  } else {
    await expect(page.locator(locator)).toBeHidden();
  }
}

export async function assertJwksUrlExists(page: Page, exist: boolean = true) {
  await assertElementExists(page, "[data-testid='config.jwksUrl']", exist);
}

export async function assertPkceMethodExists(
  page: Page,
  exist: boolean = true,
) {
  await assertElementExists(page, "#config\\.pkceMethod", exist);
}

export async function goToMappersTab(page: Page) {
  await page.getByTestId("mappers-tab").click();
}

export async function addMapper(
  page: Page,
  mapperType: string,
  mapperName: string,
) {
  await page.getByTestId("no-mappers-empty-action").click();
  await page.locator("#identityProviderMapper").click();
  await page.getByTestId(`${mapperType}-idp-mapper`).click();
  await page.getByTestId("name").fill(mapperName);
}

export async function clickSaveMapper(page: Page) {
  await page.getByTestId("new-mapper-save-button").click();
}

export async function clickCancelMapper(page: Page) {
  await page.getByTestId("new-mapper-cancel-button").click();
}

export async function assertOnMappingPage(page: Page) {
  await expect(page).toHaveURL(/.*mappers$/);
}

export async function addAuthConstraints(
  page: Page,
  classRefName: string,
  declRefName: string,
) {
  await page.getByTestId("classref-field").fill(classRefName);
  await page.getByTestId("config.authnContextClassRefs-addValue").click();
  await page.getByTestId("declref-field").fill(declRefName);
  await page.getByTestId("config.authnContextDeclRefs-addValue").click();
  await page
    .getByTestId("config.singleSignOnServiceUrl")
    .fill(samlDiscoveryUrl);
  await page.getByTestId("idp-details-save").click();
}
