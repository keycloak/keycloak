import { expect, test } from "@playwright/test";
import { login } from "../support/actions.ts";
import { adminClient, findUserByUsername } from "../support/admin-client.ts";
import { DEFAULT_USER } from "../support/common.ts";
import { createTestBed } from "../support/testbed.ts";

test.describe("Signing in", () => {
  test("shows password and OTP credentials", async ({ page }) => {
    await using testBed = await createTestBed();

    // Log in and navigate to the signing in section.
    await login(page, testBed.realm);
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/signing-in").click();

    // Verify the password credential is configured, and it is not possible to create a new one.
    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toContainText("My password");
    await expect(page.getByTestId("password/create")).toBeHidden();

    // Verify the OTP credential not configured, and it is possible to create a new one.
    await expect(
      page.getByTestId("otp/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("otp/credential-list").getByRole("listitem"),
    ).toContainText("Authenticator application is not set up.");
    await page.getByTestId("otp/create").click();
    await expect(page.locator("#kc-page-title")).toContainText(
      "Mobile Authenticator Setup",
    );
  });

  test("allows setting a password credential if none exists", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    const user = await findUserByUsername(testBed.realm, DEFAULT_USER.username);

    // Log in and delete the password credential of the user.
    await login(page, testBed.realm);
    const credentials = await adminClient.users.getCredentials({
      realm: testBed.realm,
      id: user.id as string,
    });
    await adminClient.users.deleteCredential({
      realm: testBed.realm,
      id: user.id as string,
      credentialId: credentials[0].id as string,
    });

    // Navigate to the signing in section.
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/signing-in").click();

    // Verify the password credential is not configured, and it is possible to create a new one.
    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toContainText("not set up");
    await page.getByTestId("password/create").click();
    await expect(page.locator("#kc-page-title")).toContainText(
      "Update password",
    );
  });
});
