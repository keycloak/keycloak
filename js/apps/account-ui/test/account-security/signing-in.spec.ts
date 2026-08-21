import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation.js";
import { expect, test, type Page } from "@playwright/test";
import { createHash } from "node:crypto";
import { assertLastAlert, login } from "../support/actions.ts";
import { adminClient, findUserByUsername } from "../support/admin-client.ts";
import { DEFAULT_USER } from "../support/common.ts";
import { createTestBed, type TestBed } from "../support/testbed.ts";

const RAW_RECOVERY_CODE = "ABCDEFGHIJKL";

// Two-factor credentials, mirroring how this was tested manually: one OTP credential and one
// recovery (backup) codes credential. Login authenticates with the recovery code rather than a
// live-computed OTP code, which sidesteps TOTP algorithm/timing concerns entirely for this test.
//
// A full 12-code set is required (not just 1): Keycloak's RecoveryAuthnCodesCredentialProvider
// fires a blocking "configure new recovery codes" required action post-login whenever the
// remaining count is below its warning threshold, which would hijack the login flow.
async function setupUserWithOtpAndRecoveryCodes(page: Page): Promise<TestBed> {
  const recoveryCodes = Array.from({ length: 12 }, (_, i) => ({
    number: i + 1,
    encodedHashedValue: createHash("sha512")
      .update(i === 0 ? RAW_RECOVERY_CODE : `${RAW_RECOVERY_CODE}-${i}`, "utf8")
      .digest("base64"),
  }));

  const userWithOtpAndRecovery: UserRepresentation = {
    ...DEFAULT_USER,
    credentials: [
      ...DEFAULT_USER.credentials,
      {
        userLabel: "Authenticator application",
        type: "otp",
        secretData: JSON.stringify({ value: "DJmQfC73VGFhw7D4QJ8A" }),
        credentialData: JSON.stringify({
          subType: "totp",
          digits: 6,
          counter: 0,
          period: 30,
          algorithm: "HmacSHA1",
        }),
      },
      {
        userLabel: "Backup codes",
        type: "recovery-authn-codes",
        secretData: JSON.stringify({ codes: recoveryCodes }),
        credentialData: JSON.stringify({
          hashIterations: null,
          algorithm: "SHA-512",
          remaining: 12,
          total: 12,
        }),
      },
    ],
  };
  const testBed = await createTestBed({ users: [userWithOtpAndRecovery] });

  // Recovery codes are DISABLED in the browser flow by default; enable them so login can use one.
  const executions = await adminClient.authenticationManagement.getExecutions({
    flow: "browser",
    realm: testBed.realm,
  });
  const recoveryExecution = executions.find(
    (execution) => execution.providerId === "auth-recovery-authn-code-form",
  );
  expect(
    recoveryExecution,
    "auth-recovery-authn-code-form execution not found in the browser flow",
  ).toBeDefined();
  await adminClient.authenticationManagement.updateExecution(
    { flow: "browser", realm: testBed.realm },
    { ...recoveryExecution, requirement: "ALTERNATIVE" },
  );

  await login(page, testBed.realm);
  await page.getByRole("link", { name: "Try Another Way" }).click();
  await page
    .getByRole("heading", { name: "Recovery Authentication Code" })
    .click();
  await page
    .getByRole("textbox", { name: /Recovery code/ })
    .fill(RAW_RECOVERY_CODE);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();

  await page.getByTestId("accountSecurity").click();
  await page.getByTestId("account-security/signing-in").click();

  return testBed;
}

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
    await expect(
      page.getByTestId("basic-authentication/default-select"),
    ).toHaveCount(0);

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

  test("allows choosing the default two-factor credential", async ({
    page,
  }) => {
    await using testBed = await setupUserWithOtpAndRecoveryCodes(page);

    const defaultSelect = page.getByTestId("two-factor/default-select");
    await expect(defaultSelect).toBeVisible();
    // The OTP credential was inserted first, so it has the lowest priority and is preselected.
    await expect(defaultSelect.locator("option:checked")).toHaveText(
      "Authenticator application",
    );

    await defaultSelect.selectOption({ label: "Backup codes" });
    await assertLastAlert(page, "Successfully updated the default credential.");
    await expect(defaultSelect.locator("option:checked")).toHaveText(
      "Backup codes",
    );

    // Reload to prove the change was persisted server-side via moveToFirst,
    // not just kept in local component state.
    await page.reload();
    await expect(
      page.getByTestId("two-factor/default-select").locator("option:checked"),
    ).toHaveText("Backup codes");

    // Prove the feature's actual effect, not just that the dropdown/API round-trips the priority
    // value: a fresh login should now try the recovery code form directly, since it's the credential
    // the user just made preferred, instead of defaulting to the OTP form as before.
    await page.getByTestId("options-toggle").click();
    await page.getByRole("menuitem", { name: "Sign out", exact: true }).click();
    await login(page, testBed.realm);
    await expect(
      page.getByRole("heading", {
        name: "Login with a recovery authentication code",
      }),
    ).toBeVisible();
  });

  test("shows an error when the reorder request fails", async ({ page }) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars -- only needed for realm cleanup on disposal
    await using testBed = await setupUserWithOtpAndRecoveryCodes(page);

    const defaultSelect = page.getByTestId("two-factor/default-select");
    await expect(defaultSelect).toBeVisible();
    await expect(defaultSelect.locator("option:checked")).toHaveText(
      "Authenticator application",
    );

    await page.route("**/account/credentials/*/moveToFirst", (route) =>
      route.fulfill({
        status: 400,
        contentType: "application/json",
        body: JSON.stringify({ error: "invalid_request" }),
      }),
    );

    await defaultSelect.selectOption({ label: "Backup codes" });
    await assertLastAlert(
      page,
      "Could not update the default credential: invalid_request",
    );

    // The selection reverts since local state is only updated on success.
    await expect(defaultSelect.locator("option:checked")).toHaveText(
      "Authenticator application",
    );
    // The pending flag was cleared in `finally`, so the dropdown is usable again, not stuck disabled.
    await expect(defaultSelect).toBeEnabled();
  });
});
