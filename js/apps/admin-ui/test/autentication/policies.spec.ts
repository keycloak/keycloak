import { test } from "@playwright/test";
import { toAuthentication } from "../../src/authentication/routes/Authentication.tsx";
import { createTestBed } from "../support/testbed.ts";
import { clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import {
  assertSupportedApplications,
  fillSelects,
  goToOTPPolicyTab,
  goToWebauthnPage,
  goToWebauthnPasswordlessPage,
  increaseInitialCounter,
  setPolicyType,
  setWebAuthnPolicyCreateTimeout,
} from "./policies.ts";

test.describe("OTP policies tab", () => {
  test("changes to hotp", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, {
      to: toAuthentication({ realm: testBed.realm, tab: "policies" }),
    });
    await goToOTPPolicyTab(page);
    // Check initial supported applications
    await assertSupportedApplications(page, [
      "FreeOTP",
      "Google Authenticator",
      "Microsoft Authenticator",
    ]);

    // Change policy type and save
    await setPolicyType(page, "hotp");
    await increaseInitialCounter(page);
    await clickSaveButton(page);

    // // Verify notification and updated supported applications
    await assertNotificationMessage(page, "OTP policy successfully updated");
    await assertSupportedApplications(page, [
      "FreeOTP",
      "Google Authenticator",
    ]);
  });
});

test.describe("Webauthn policies tabs", () => {
  test("fills webauthn settings", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, {
      to: toAuthentication({ realm: testBed.realm, tab: "policies" }),
    });
    await goToWebauthnPage(page);

    await fillSelects(page, {
      webAuthnPolicyAttestationConveyancePreference: "Indirect",
      webAuthnPolicyRequireResidentKey: "Yes",
      webAuthnPolicyUserVerificationRequirement: "Preferred",
    });

    await setWebAuthnPolicyCreateTimeout(page, 30);
    await clickSaveButton(page);

    await assertNotificationMessage(
      page,
      "Updated WebAuthn policies successfully",
    );
  });

  test("fills webauthn passwordless settings", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, {
      to: toAuthentication({ realm: testBed.realm, tab: "policies" }),
    });
    await goToWebauthnPasswordlessPage(page);

    await fillSelects(page, {
      webAuthnPolicyPasswordlessAttestationConveyancePreference: "Indirect",
      webAuthnPolicyPasswordlessRequireResidentKey: "Yes",
      webAuthnPolicyPasswordlessUserVerificationRequirement: "Preferred",
    });

    await clickSaveButton(page);

    await assertNotificationMessage(
      page,
      "Updated WebAuthn policies successfully",
    );
  });
});
