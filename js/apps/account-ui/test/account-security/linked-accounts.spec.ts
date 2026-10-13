import { type Page, expect, test } from "@playwright/test";
import groupsIdPClient from "../realms/groups-idp.json" with { type: "json" };
import userProfileRealm from "../realms/user-profile-realm.json" with { type: "json" };
import { assertLastAlert, login } from "../support/actions.ts";
import { adminClient } from "../support/admin-client.ts";
import { DEFAULT_USER, getAccountUrl, SERVER_URL } from "../support/common.ts";
import { createTestBed } from "../support/testbed.ts";

const EXTERNAL_USERNAME = "external-user";
const EXTERNAL_PASSWORD = "external-user";
const EXTERNAL_EMAIL = "external-user@keycloak.org";

test.describe("Linked accounts", () => {
  test("shows linked accounts", async ({ page }) => {
    await using testBed = await createTestBed(userProfileRealm);

    // Log in and navigate to the linked accounts section.
    await login(page, testBed.realm);
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/linked-accounts").click();
    await expect(page.getByTestId("page-heading")).toHaveText(
      "Linked accounts",
    );
  });

  test("cannot remove the last federated identity", async ({ page }) => {
    // Create an 'external' realm with a user that will be used for linking.
    await using externalTestBed = await createTestBed({
      users: [
        {
          ...DEFAULT_USER,
          username: EXTERNAL_USERNAME,
          email: EXTERNAL_EMAIL,
          credentials: [
            {
              type: "password",
              value: EXTERNAL_PASSWORD,
            },
          ],
        },
      ],
    });

    await adminClient.clients.create({
      realm: externalTestBed.realm,
      ...groupsIdPClient,
    });

    // Create a realm that links to the external realm as an identity provider.
    await using testBed = await createTestBed();

    await adminClient.identityProviders.create({
      realm: testBed.realm,
      alias: "master-idp",
      providerId: "oidc",
      enabled: true,
      config: {
        clientId: "groups-idp",
        clientSecret: "H0JaTc7VBu3HJR26vrzMxgidfJmgI5Dw",
        validateSignature: "false",
        tokenUrl: `${SERVER_URL}/realms/${externalTestBed.realm}/protocol/openid-connect/token`,
        jwksUrl: `${SERVER_URL}/realms/${externalTestBed.realm}/protocol/openid-connect/certs`,
        issuer: `${SERVER_URL}/realms/${externalTestBed.realm}`,
        authorizationUrl: `${SERVER_URL}/realms/${externalTestBed.realm}/protocol/openid-connect/auth`,
        logoutUrl: `${SERVER_URL}/realms/${externalTestBed.realm}/protocol/openid-connect/logout`,
        userInfoUrl: `${SERVER_URL}/realms/${externalTestBed.realm}/protocol/openid-connect/userinfo`,
      },
    });

    await page.goto(getAccountUrl(testBed.realm).toString());

    // Click the login via master-idp provider button
    await loginWithIdp(page, "master-idp");

    // Now the login at the master-idp should be visible
    await loginWithUsernamePassword(page, EXTERNAL_USERNAME, EXTERNAL_PASSWORD);

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
    await assertLastAlert(
      page,
      "Could not unlink due to: You can not remove last federated identity as you do not have a password.",
    );
  });
});

async function loginWithUsernamePassword(
  page: Page,
  username: string,
  password: string,
) {
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
}

async function loginWithIdp(page: Page, idpAlias: string) {
  await page.getByRole("link", { name: idpAlias }).click();
}
