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

  test("unconfigured credential cards are disabled in order section", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    await login(page, testBed.realm);
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/signing-in").click();

    const orderSection = page.getByTestId("credential-order-section");
    await expect(orderSection).toBeVisible();

    // password card should not be disabled (it is configured)
    const passwordCard = orderSection.getByTestId("password/order-card");
    await expect(passwordCard).not.toHaveClass(/disabled/);

    // OTP card should be disabled (not configured)
    const otpCard = orderSection.getByTestId("otp/order-card");
    await expect(otpCard).toHaveClass(/disabled/);
  });

  test("arrows disabled when only one credential is configured", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    await login(page, testBed.realm);
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/signing-in").click();

    const orderSection = page.getByTestId("credential-order-section");
    await expect(orderSection).toBeVisible();

    // only password is configured, so all move buttons should be disabled
    await expect(page.getByTestId("password/down")).toBeDisabled();
    await expect(page.getByTestId("otp/up")).toBeDisabled();
  });

  test("move recovery codes credentials up and down", async ({ page }) => {
    await using testBed = await createTestBed();

    // enable recovery codes in the realm
    const executions = await adminClient.authenticationManagement.getExecutions(
      {
        flow: "browser",
        realm: testBed.realm,
      },
    );
    const recoveryCodes = executions.find(
      (e) => e.providerId === "auth-recovery-authn-code-form",
    );
    expect(recoveryCodes).toBeDefined();
    recoveryCodes!.requirement = "ALTERNATIVE";
    await adminClient.authenticationManagement.updateExecution(
      { flow: "browser", realm: testBed.realm },
      recoveryCodes!,
    );

    // login and go to signing-in page
    await login(page, testBed.realm);
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/signing-in").click();

    // Verify the password credential is configured
    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toContainText("My password");
    await expect(page.getByTestId("password/create")).toBeHidden();

    // Verify recovery codes not configured
    await expect(
      page
        .getByTestId("recovery-authn-codes/credential-list")
        .getByRole("listitem"),
    ).toContainText("Recovery authentication codes is not set up.");

    // credential order section should be visible (multiple credential types available)
    const orderSection = page.getByTestId("credential-order-section");
    await expect(orderSection).toBeVisible();

    // check initial order of credentials
    await expect(orderSection.locator("span.cred-title")).toContainText([
      "Password",
      "Recovery authentication codes",
    ]);

    // register recovery codes
    await page.getByTestId("recovery-authn-codes/create").click();
    await expect(page.locator("#kc-page-title")).toContainText(
      "Recovery Authentication Codes",
    );
    await page.locator("#kcRecoveryCodesConfirmationCheck").click();
    await page.locator("#saveRecoveryAuthnCodesBtn").click();

    // expect recovery codes has values
    await expect(
      page
        .getByTestId("recovery-authn-codes/credential-list")
        .getByRole("listitem"),
    ).toContainText("Recovery codes");

    // check order in the credential order section (password, recovery codes, OTP)
    await expect(orderSection.locator("span.cred-title")).toContainText([
      "Password",
      "Recovery authentication codes",
      "Authenticator application",
    ]);

    // password is first so up is disabled, recovery codes is in the middle so both enabled
    await expect(page.getByTestId("password/up")).toBeDisabled();
    await expect(page.getByTestId("password/down")).toBeEnabled();
    await expect(page.getByTestId("recovery-authn-codes/up")).toBeEnabled();
    await expect(page.getByTestId("recovery-authn-codes/down")).toBeEnabled();

    // move recovery codes up
    await page.getByTestId("recovery-authn-codes/up").click();

    // now recovery codes are the first one
    await expect(orderSection.locator("span.cred-title")).toContainText([
      "Recovery authentication codes",
      "Password",
      "Authenticator application",
    ]);

    // recovery codes is first so up is disabled
    await expect(page.getByTestId("recovery-authn-codes/up")).toBeDisabled();
    await expect(page.getByTestId("recovery-authn-codes/down")).toBeEnabled();
    await expect(page.getByTestId("password/up")).toBeEnabled();
    await expect(page.getByTestId("password/down")).toBeEnabled();

    // move recovery codes down again
    await page.getByTestId("recovery-authn-codes/down").click();

    // now password is the first one again
    await expect(orderSection.locator("span.cred-title")).toContainText([
      "Password",
      "Recovery authentication codes",
      "Authenticator application",
    ]);

    // verify buttons back to original state
    await expect(page.getByTestId("password/up")).toBeDisabled();
    await expect(page.getByTestId("password/down")).toBeEnabled();
    await expect(page.getByTestId("recovery-authn-codes/up")).toBeEnabled();
    await expect(page.getByTestId("recovery-authn-codes/down")).toBeEnabled();
  });
});
