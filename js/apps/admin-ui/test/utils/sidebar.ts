import { Page } from "@playwright/test";

export async function goToRealm(page: Page, realmName: string) {
  await page.getByTestId("realmSelector").click();
  await page.getByRole("menuitem", { name: realmName }).click();
}

export async function goToOrganizations(page: Page) {
  page.getByTestId("nav-item-organizations").click();
}

export async function goToClients(page: Page) {
  await page.getByTestId("nav-item-clients").click();
}

export async function goToClientScopes(page: Page) {
  await page.getByTestId("nav-item-client-scopes").click();
}

export async function goToRealmRoles(page: Page) {
  await page.getByTestId("nav-item-roles").click();
}

export async function goToUsers(page: Page) {
  await page.getByTestId("nav-item-users").click();
}

export async function goToGroups(page: Page) {
  await page.getByTestId("nav-item-groups").click();
}

export async function goToSessions(page: Page) {
  await page.getByTestId("nav-item-sessions").click();
}

export async function goToEvents(page: Page) {
  await page.getByTestId("nav-item-events").click();
}

export async function goToRealmSettings(page: Page) {
  await page.getByTestId("nav-item-realm-settings").click();
}

export async function goToAuthentication(page: Page) {
  await page.getByTestId("nav-item-authentication").click();
}

export async function goToIdentityProviders(page: Page) {
  await page.getByTestId("nav-item-identity-providers").click();
}

export async function goToUserFederation(page: Page) {
  await page.getByTestId("nav-item-user-federation").click();
}
