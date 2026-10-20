package org.keycloak.tests.forms;

import java.util.Arrays;

import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.PasswordGenerateUtil.generatePassword;

@KeycloakIntegrationTest
public class ResetCredentialsOTPFlowsTest extends AbstractResetCredentialsAlternativeFlowsTest  {

    @InjectPage
    protected LoginConfigTotpPage totpPage;

    @InjectPage
    protected RegisterPage registerPage;

    @InjectPage
    protected LogoutConfirmPage logoutConfirmPage;

    protected TimeBasedOTP totp = new TimeBasedOTP();

    @Override
    public String getAiaAction() {
        return UserModel.RequiredAction.CONFIGURE_TOTP.name();
    }

    @Test
    public void resetCredentialsVerifyCustomOtpLabelSetProperly() {
        try {
            // Make a copy of the default Reset Credentials flow, but:
            // * Without 'Send Reset Email' authenticator,
            // * Without 'Reset Password' authenticator
            final String newFlowAlias = "resetcred - KEYCLOAK-11753 - test";
            configureResetCredentialsRemoveExecutionsAndBindTheFlow( // TODO
                    newFlowAlias,
                    Arrays.asList("reset-credential-email", "reset-password")
            );

            // Login & set up the initial OTP code for the user
            oauth.openLoginForm();
            loginPage.fillLogin(managedUser.getUsername(), managedUser.getPassword());
            loginPage.submit();
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);

            String customOtpLabel = "my-original-otp-label";

            // Setup OTP
            doAIA();
            totpPage.assertCurrent();
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), customOtpLabel);
            assertKcActionStatus(SUCCESS);
            managedUser.cleanup().add(u -> {
                String totpId = u.credentials().stream().filter(c -> OTPCredentialModel.TYPE.equals(c.getType())).findFirst().get().getId();
                u.removeCredential(totpId);
            });

            // Logout
            oauth.logoutForm().idTokenHint(response.getIdToken()).open();

            // Go to login page & click "Forgot password" link to perform the custom 'Reset Credential' flow
            oauth.openLoginForm();
            loginPage.resetPassword();

            // Should be on reset password page now. Provide email of the user & click Submit button
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword(managedUser.getUsername());

            // Provide updated form of the OTP label, to be used within 'Reset OTP' (next) step
            customOtpLabel = "my-reset-otp-label";

            // Reset OTP label to a custom value as part of Reset Credentials flow
            AccountHelper.updateTotpUserLabel(managedRealm.admin(), managedUser.getUsername(), customOtpLabel);

            // Open OTP Authenticator account page
            // Check if OTP credential is present
            Assertions.assertTrue(AccountHelper.totpUserLabelComparator(managedRealm.admin(), managedUser.getUsername(), customOtpLabel));

            // Undo setup changes performed within the test
        } finally {
            revertResetCredentialsFlows();
        }
    }


    // KEYCLOAK-12168 Verify the 'Device Name' label is optional for the first OTP credential created
    // (either via Account page or by registering new user), but required for each next created OTP credential
    @Test
    public void deviceNameOptionalForFirstOTPCredentialButRequiredForEachNextOne() {
        // Enable 'Default Action' on 'Configure OTP' RA for the 'test' realm
        RequiredActionProviderRepresentation otpRequiredAction = managedRealm.admin().flows().getRequiredAction("CONFIGURE_TOTP");
        otpRequiredAction.setDefaultAction(true);
        managedRealm.admin().flows().updateRequiredAction("CONFIGURE_TOTP", otpRequiredAction);
        otpRequiredAction.setDefaultAction(false);
        managedRealm.cleanup().add(r -> r.flows().updateRequiredAction("CONFIGURE_TOTP", otpRequiredAction));

        try {
            // Make a copy of the default Reset Credentials flow, but:
            // * Without 'Send Reset Email' authenticator,
            // * Without 'Reset Password' authenticator
            final String newFlowAlias = "resetcred - KEYCLOAK-12168 - firstOTP - account - test";
            configureResetCredentialsRemoveExecutionsAndBindTheFlow( // TODO
                    newFlowAlias,
                    Arrays.asList("reset-credential-email", "reset-password")
            );

            /* Verify the 'Device Name' is optional when creating new OTP credential via the Account page */

            // Login & set up the initial OTP code for the user
            oauth.openLoginForm();
            loginPage.fillLogin(managedUser.getUsername(), managedUser.getPassword());
            loginPage.submit();

            // Create OTP credential with empty label
            final String emptyOtpLabel = "";

            // Setup OTP
            doAIA();
            totpPage.assertCurrent();
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), emptyOtpLabel);
            assertKcActionStatus(SUCCESS);

            Assertions.assertTrue(AccountHelper.deleteTotpAuthentication(managedRealm.admin(), managedUser.getUsername()));

            // Logout
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            /* Verify the 'Device Name' is optional when creating the first OTP credential via the login config TOTP page */

            // Register new user
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("Bruce", "Wilson", "bwilson@keycloak.org", "bwilson", generatePassword());
            String bwilsonId = AdminApiUtil.findUserByUsername(managedRealm.admin(), "bwilson").getId();
            managedRealm.cleanup().add(r -> r.users().delete(bwilsonId).close());
            totpPage.assertCurrent();

            // Create OTP credential with empty label

            // Setup OTP
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), "");

            // Assert user authenticated
            Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

            Assertions.assertTrue(AccountHelper.isTotpPresent(managedRealm.admin(), "bwilson"));
            Assertions.assertTrue(AccountHelper.totpUserLabelComparator(managedRealm.admin(), "bwilson", ""));

            // Logout
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            /* Verify the 'Device Name' is required for each next OTP credential created via the login config TOTP page */

            // Click "Forgot password" to define another OTP credential
            oauth.openLoginForm();
            loginPage.resetPassword();

            // Should be on reset password page now. Provide email of previously registered user & click Submit button
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("bwilson@keycloak.org");

            // Try to create another OTP credential with empty label again. This
            // should fail with error since OTP label is required in this case already
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), "");
            Assertions.assertTrue(AccountHelper.totpCountEquals(managedRealm.admin(), "bwilson", 1));
            // Create 2nd OTP credential with valid (non-empty) Device Name label. This should pass
            final String secondOtpLabel = "My 2nd OTP device";
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), secondOtpLabel);

            // Assert user authenticated
            Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

            // Verify 2nd OTP credential was successfully created too
            Assertions.assertTrue(AccountHelper.totpUserLabelComparator(managedRealm.admin(), "bwilson", secondOtpLabel));

            // Logout
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            // Undo setup changes performed within the test
        } finally {
            revertResetCredentialsFlows();
        }
    }

    @Test
    void resetPasswordReAuthForced() {
        // TODO
    }
}
