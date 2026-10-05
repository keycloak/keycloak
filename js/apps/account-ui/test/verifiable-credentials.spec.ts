import { expect, type Page, test } from "@playwright/test";
import { login } from "./support/actions.ts";
import { adminClient, findUserByUsername } from "./support/admin-client.ts";
import { createTestBed } from "./support/testbed.ts";
import { skipIfOID4VCIFeatureDisabled } from "./support/oid4vci.ts";

const CREDENTIAL_CONFIGURATION_ID = "employee-credential-configuration";
const CREDENTIAL_SCOPE_NAME = "employee-credential";
const realm = {
  verifiableCredentialsEnabled: true,
};

async function createCredential(realm: string) {
  await adminClient.clientScopes.create({
    realm,
    name: CREDENTIAL_SCOPE_NAME,
    protocol: "oid4vc",
    attributes: {
      "vc.credential_configuration_id": CREDENTIAL_CONFIGURATION_ID,
    },
  });
  const user = await findUserByUsername(realm, "jdoe");

  await adminClient.users.createVerifiableCredential(
    { realm, id: user.id! },
    { credentialScopeName: CREDENTIAL_SCOPE_NAME },
  );
}

async function openVerifiableCredentials(page: Page, realmName: string) {
  await login(page, realmName);
  await page.getByTestId("verifiable-credentials").click();
  await expect(
    page.locator(`#credential-${CREDENTIAL_SCOPE_NAME}`),
  ).toBeVisible();
}

async function revokeCredential(page: Page) {
  await page.getByRole("button", { name: "Revoke", exact: true }).click();
  const dialog = page.getByRole("dialog", {
    name: "Revoke verifiable credential",
  });
  await expect(dialog).toContainText(CREDENTIAL_SCOPE_NAME);
  await dialog.getByRole("button", { name: "Revoke", exact: true }).click();
}

test.describe("Verifiable Credentials", () => {
  test.beforeEach(async () => {
    await skipIfOID4VCIFeatureDisabled();
  });

  test("opens issued credential client links without granting window.opener access", async ({
    page,
  }) => {
    await using testBed = await createTestBed({
      verifiableCredentialsEnabled: true,
    });

    const clientBaseUrl = "https://example.com/wallet";
    const credentialScopeName = "TestCredential";

    await page.route("**/account/verifiable-credentials", async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            credentialScopeName,
            credentialConfigurationId: "test-config",
          },
        ]),
      });
    });

    await page.route(
      "**/account/issued-verifiable-credentials",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([
            {
              id: "issued-1",
              credentialType: credentialScopeName,
              clientId: "wallet-client",
              clientName: "Wallet Client",
              clientBaseUrl,
              issuedAt: Date.now(),
            },
          ]),
        });
      },
    );

    await login(page, testBed.realm);

    const navItem = page.getByTestId("verifiable-credentials");
    await expect(navItem).toBeVisible();
    await navItem.click();
    await page
      .locator(`#credential-${credentialScopeName}-view-issued`)
      .click();

    const link = page.getByRole("dialog").getByRole("link", {
      name: /Wallet Client/,
    });
    await expect(link).toHaveAttribute("href", clientBaseUrl);
    await expect(link).toHaveAttribute("target", "_blank");
    await expect(link).toHaveAttribute("rel", "noreferrer noopener");
  });

  test("lists and revokes a credential", async ({ page }) => {
    await using testBed = await createTestBed(realm);
    await createCredential(testBed.realm);
    await openVerifiableCredentials(page, testBed.realm);

    await revokeCredential(page);

    await expect(page.getByTestId("last-alert")).toHaveText(
      "Verifiable credential deleted successfully",
    );
    await expect(
      page.locator(`#credential-${CREDENTIAL_SCOPE_NAME}`),
    ).toHaveCount(0);
  });

  test("starts wallet issuance with the credential configuration", async ({
    page,
  }) => {
    await using testBed = await createTestBed(realm);
    await createCredential(testBed.realm);
    await openVerifiableCredentials(page, testBed.realm);

    await page.route(
      "**/protocol/openid-connect/auth**kc_action=**",
      async (route) => route.abort(),
    );
    const authorizationRequest = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return (
        url.pathname.endsWith("/protocol/openid-connect/auth") &&
        url.searchParams.has("kc_action")
      );
    });
    await page
      .getByRole("button", { name: "Issue to Wallet", exact: true })
      .click();

    const url = new URL((await authorizationRequest).url());
    const action = url.searchParams.get("kc_action");
    expect(action, "Authorization request is missing kc_action").toBeTruthy();
    const [actionName, encodedConfig] = (action ?? "").split(":", 2);
    expect(
      encodedConfig,
      "Credential offer action is missing its configuration",
    ).toBeTruthy();
    expect(actionName).toBe("verifiable_credential_offer");
    expect(JSON.parse(atob(encodedConfig))).toEqual({
      credentialConfigurationId: CREDENTIAL_CONFIGURATION_ID,
      preAuthorized: false,
    });
  });
});
