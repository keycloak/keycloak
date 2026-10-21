import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation.js";
import type { Page } from "@playwright/test";

export async function clickAddProvider(page: Page, provider: string) {
  await page.getByTestId(`${provider}-card`).click();
}

export async function fillKerberosForm(
  page: Page,
  data: IdentityProviderRepresentation,
) {
  await page.getByTestId("name").fill(data.alias!);
  await page
    .getByTestId("config.kerberosRealm.0")
    .fill(data.config!["kerberosRealm"]!);
  await page
    .getByTestId("config.serverPrincipal.0")
    .fill(data.config!["serverPrincipal"]!);
  await page.getByTestId("config.keyTab.0").fill(data.config!["keyTab"]!);
}

export async function clickSave(page: Page, provider: string) {
  await page.getByTestId(`${provider}-save`).click();
}

export async function clickUserFederationCard(page: Page, name: string) {
  await page.getByRole("link", { name }).click();
}
