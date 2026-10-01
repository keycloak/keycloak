import { expect, test } from "@playwright/test";
import { login } from "./support/actions.ts";
import { createTestBed } from "./support/testbed.ts";

test.describe("Verifiable Credentials", () => {
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
    // OID4VCI must be enabled on the server for this page to be registered.
    // eslint-disable-next-line playwright/no-skipped-test -- Feature-gated page.
    test.skip(
      !(await navItem.isVisible()),
      "OID4VCI is not enabled on the Keycloak server",
    );

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
});
