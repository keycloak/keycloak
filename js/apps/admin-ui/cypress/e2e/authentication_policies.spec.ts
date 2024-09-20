import { keycloakBefore } from "../support/util/keycloak_hooks";
import Masthead from "../support/pages/admin-ui/Masthead";
import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import OTPPolicies from "../support/pages/admin-ui/manage/authentication/OTPPolicies";
import WebAuthnPolicies from "../support/pages/admin-ui/manage/authentication/WebAuthnPolicies";

describe("Policies", () => {
  const masthead = new Masthead();
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();

  describe("OTP policies tab", () => {
    const otpPoliciesPage = new OTPPolicies();

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToAuthentication();
      otpPoliciesPage.goToTab();
    });

    it("should change to hotp", () => {
      otpPoliciesPage.checkSupportedApplications(
        "FreeOTP",
        "Google Authenticator",
        "Microsoft Authenticator",
      );
      otpPoliciesPage.setPolicyType("hotp").increaseInitialCounter().save();
      masthead.checkNotificationMessage("OTP policy successfully updated");
      otpPoliciesPage.checkSupportedApplications(
        "FreeOTP",
        "Google Authenticator",
      );
    });
  });

  describe("Webauthn policies tabs", () => {
    const webauthnPage = new WebAuthnPolicies();

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToAuthentication();
    });

    it("should fill webauthn settings", () => {
      webauthnPage.goToTab();
      webauthnPage.fillSelects({
        webAuthnPolicyAttestationConveyancePreference: "Indirect",
        webAuthnPolicyRequireResidentKey: "Yes",
        webAuthnPolicyUserVerificationRequirement: "Preferred",
      });
      webauthnPage.webAuthnPolicyCreateTimeout(30).save();
      masthead.checkNotificationMessage(
        "Updated webauthn policies successfully",
      );
    });

    it("should fill webauthn passwordless settings", () => {
      webauthnPage.goToPasswordlessTab();
      webauthnPage
        .fillSelects(
          {
            webAuthnPolicyAttestationConveyancePreference: "Indirect",
            webAuthnPolicyRequireResidentKey: "Yes",
            webAuthnPolicyUserVerificationRequirement: "Preferred",
          },
          true,
        )
        .save();
      masthead.checkNotificationMessage(
        "Updated webauthn policies successfully",
      );
    });
  });
});
