import ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { expect, test } from "@playwright/test";
import { randomUUID } from "node:crypto";

import {
  createClient,
  createIdentityProvider,
  createRandomUserWithPassword,
  deleteClient,
  deleteIdentityProvider,
  deleteUser,
  findClientByClientId,
  inRealm,
} from "../admin-client";
import { SERVER_URL } from "../constants";
import groupsIdPClient from "../realms/groups-idp.json" assert { type: "json" };

const realm = "groups";

test.describe("Account linking", () => {
  let groupIdPClientId: string;
  let user: string;
  // Tests for keycloak account console, section Account linking in Account security
  test.beforeAll(async () => {
    user = await createRandomUserWithPassword("user-" + randomUUID(), "pwd");

    const kcGroupsIdpId = await findClientByClientId("groups-idp");
    if (kcGroupsIdpId) {
      await deleteClient(kcGroupsIdpId);
    }
    groupIdPClientId = await createClient(
      groupsIdPClient as ClientRepresentation,
    );
    const idp: IdentityProviderRepresentation = {
      alias: "master-idp",
      providerId: "oidc",
      enabled: true,
      config: {
        clientId: "groups-idp",
        clientSecret: "H0JaTc7VBu3HJR26vrzMxgidfJmgI5Dw",
        validateSignature: "false",
        tokenUrl: `${SERVER_URL}/realms/master/protocol/openid-connect/token`,
        jwksUrl: `${SERVER_URL}/realms/master/protocol/openid-connect/certs`,
        issuer: `${SERVER_URL}/realms/master`,
        authorizationUrl: `${SERVER_URL}/realms/master/protocol/openid-connect/auth`,
        logoutUrl: `${SERVER_URL}/realms/master/protocol/openid-connect/logout`,
        userInfoUrl: `${SERVER_URL}/realms/master/protocol/openid-connect/userinfo`,
      },
    };

    await inRealm(realm, () => createIdentityProvider(idp));
  });

  test.afterAll(async () => {
    await deleteUser(user);
  });
  test.afterAll(async () => {
    await deleteClient(groupIdPClientId);
  });
  test.afterAll(async () => {
    await inRealm(realm, () => deleteIdentityProvider("master-idp"));
  });

  test("Linking", async ({ page }) => {
    // If refactoring this, consider introduction of helper functions for individual pages - login, update profile etc.
    await page.goto(`/realms/${realm}/account`);

    // Click the login via master-idp provider button
    await loginWithIdp(page, "master-idp");

    // Now the login at the master-idp should be visible
    await loginWithUsernamePassword(page, "admin", "admin");

    // Now the update-profile page should be visible
    await updateProfile(page, "test", "user", "testuser@keycloak.org");

    // Now the account console should be visible
    await page.getByTestId("accountSecurity").click();
    await expect(
      page.getByTestId("account-security/linked-accounts"),
    ).toBeVisible();
    await page.getByTestId("account-security/linked-accounts").click();
    await expect(
      page
        .getByTestId("linked-accounts/master-idp")
        .getByRole("button", { name: "Unlink account" }),
    ).toBeVisible();
    await page
      .getByTestId("linked-accounts/master-idp")
      .getByRole("button", { name: "Unlink account" })
      .click();

    // Expect an error shown that the account cannot be unlinked
    await expect(page.getByTestId("alerts")).toBeVisible();
  });
});

async function updateProfile(page, firstName, lastName, email) {
  await expect(
    page.getByRole("heading", { name: "Update Account Information" }),
  ).toBeVisible();
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("First name", { exact: true }).fill(firstName);
  await page.getByLabel("Last name", { exact: true }).fill(lastName);
  await page.getByRole("button", { name: "Submit" }).click();
}

async function loginWithUsernamePassword(page, username, password) {
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
}

async function loginWithIdp(page, idpAlias: string) {
  await page.getByRole("link", { name: idpAlias }).click();
}
