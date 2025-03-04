import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { clickSaveButton } from "../utils/form";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { goToAuthentication, goToRealm } from "../utils/sidebar";
import {
  assertSupportedApplications,
  fillSelects,
  goToOTPPolicyTab,
  goToWebauthnPage,
  goToWebauthnPasswordlessPage,
  increaseInitialCounter,
  setPolicyType,
  setWebAuthnPolicyCreateTimeout,
} from "./policies";

test.describe("OTP policies tab", () => {
  const realmName = `policies-otp-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToAuthentication(page);
    await goToOTPPolicyTab(page);
  });

  test("should change to hotp", async ({ page }) => {
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
  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToAuthentication(page);
  });

  test("should fill webauthn settings", async ({ page }) => {
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
      "Updated webauthn policies successfully",
    );
  });

  test("should fill webauthn passwordless settings", async ({ page }) => {
    await goToWebauthnPasswordlessPage(page);

    await fillSelects(page, {
      webAuthnPolicyPasswordlessAttestationConveyancePreference: "Indirect",
      webAuthnPolicyPasswordlessRequireResidentKey: "Yes",
      webAuthnPolicyPasswordlessUserVerificationRequirement: "Preferred",
    });

    await clickSaveButton(page);

    await assertNotificationMessage(
      page,
      "Updated webauthn policies successfully",
    );
  });
});
