import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation.js";
import type { Page } from "@playwright/test";

export async function createClient(
  page: Page,
  client?: ClientRepresentation,
  action?: () => Promise<void>,
) {
  await page.getByTestId("createClient").click();
  await page.getByTestId("clientId").fill(client?.clientId || "");
  await page.getByTestId("name").fill(client?.name || "");
  await page.getByTestId("description").fill(client?.description || "");
  await page.getByLabel("Client type", { exact: true }).click();
  await page
    .getByRole("option", { name: client?.protocol || "OpenID Connect" })
    .click();

  if (client?.alwaysDisplayInConsole)
    await page.getByText("Always display in UI").click();

  if (action) {
    await action();
  } else {
    await continueNext(page);
  }
}

export async function clientCapabilityConfig(
  page: Page,
  client: ClientRepresentation,
) {
  if (client.publicClient)
    await page.getByText("Client authentication", { exact: true }).click();

  if (client.directAccessGrantsEnabled)
    await page.getByTestId("direct").click();

  if (client.implicitFlowEnabled) await page.getByTestId("implicit").click();

  if (client.serviceAccountsEnabled)
    await page.getByTestId("service-account").click();

  if (client.standardFlowEnabled) await page.getByTestId("standard").click();

  if (
    client.attributes?.["oauth2.device.authorization.grant.enabled"] === "true"
  )
    await page.getByTestId("oauth-device-authorization-grant").click();

  if (client.attributes?.["oidc.ciba.grant.enabled"] === "true")
    await page.getByTestId("oidc-ciba-grant").click();
}

export async function continueNext(page: Page) {
  await page.getByRole("button", { name: "Next" }).click();
}

export async function save(page: Page) {
  await page.getByRole("button", { name: "Save" }).click();
}

export async function cancel(page: Page) {
  await page.getByRole("button", { name: "Cancel" }).click();
}
