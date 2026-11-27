package org.keycloak.tests.i18n;

import org.keycloak.admin.client.resource.RealmLocalizationResource;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.page.TermsAndConditionsPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.CustomProvidersServerConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the page-specific login page titles, set with the {@code loginTitle.<template>} message keys.
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class LoginPageTitleTest {

    private static final String REALM_DISPLAY_NAME = "Title Test";
    private static final String OTP_SECRET = "abcdefghijklmnopqrstuvwxyz0123456789";

    @InjectRealm(config = TitleRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(ref = "totp-setup", config = TotpSetupUserConfig.class)
    ManagedUser totpSetupUser;

    @InjectUser(ref = "otp", config = OtpUserConfig.class)
    ManagedUser otpUser;

    @InjectUser(ref = "terms", config = TermsUserConfig.class)
    ManagedUser termsUser;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    LoginConfigTotpPage configTotpPage;

    @InjectPage
    LoginTotpPage loginTotpPage;

    @InjectPage
    LoginPasswordResetPage resetPasswordPage;

    @InjectPage
    LogoutConfirmPage logoutConfirmPage;

    @InjectPage
    TermsAndConditionsPage termsPage;

    @BeforeEach
    public void resetBrowser() {
        // Remove the locale cookie and any session left by an earlier test. otpPageUsesPageTitle signs in.
        realm.admin().logoutAll();
        driver.cookies().deleteAll();
    }

    @Test
    public void pageWithoutPageTitleUsesLoginTitle() {
        oauth.openLoginForm();
        loginPage.assertCurrent();

        assertEquals("Sign in to " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void otpSetupPageUsesPageTitle() {
        setText("en", "loginTitle.login-config-totp", "Set up two-factor authentication - {0}");

        oauth.openLoginForm();
        loginPage.fillLogin(totpSetupUser.getUsername(), "password");
        loginPage.submit();
        configTotpPage.assertCurrent();

        assertEquals("Set up two-factor authentication - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void otpPageUsesPageTitle() {
        setText("en", "loginTitle.login-otp", "Enter your one-time code - {0}");

        oauth.openLoginForm();
        loginPage.fillLogin(otpUser.getUsername(), "password");
        loginPage.submit();
        loginTotpPage.assertCurrent();

        assertEquals("Enter your one-time code - " + REALM_DISPLAY_NAME, driver.driver().getTitle());

        // The page still works with the new title.
        loginTotpPage.login(new TimeBasedOTP().generateTOTP(OTP_SECRET));
        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void resetPasswordPageUsesPageTitle() {
        setText("en", "loginTitle.login-reset-password", "Reset password - {0}");

        openResetPasswordPage();

        assertEquals("Reset password - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void pageCreatedWithCreateFormUsesPageTitle() {
        // The Terms and Conditions required action renders terms.ftl with LoginFormsProvider.createForm().
        enableRequiredAction(RequiredAction.TERMS_AND_CONDITIONS.name());
        setText("en", "loginTitle.terms", "Accept the terms - {0}");

        oauth.openLoginForm();
        loginPage.fillLogin(termsUser.getUsername(), "password");
        loginPage.submit();
        termsPage.assertCurrent();

        assertEquals("Accept the terms - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void notFoundPageUsesErrorPageTitle() {
        setText("en", "loginTitle.error", "Error - {0}");

        driver.open(keycloakUrls.getBaseBuilder().path("/realms/" + realm.getName() + "/nosuch").build().toString());
        errorPage.assertCurrent();

        assertEquals("Error - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void invalidPageTitleKeepsLoginTitle() {
        setText("en", "loginTitle.login-reset-password", "Reset password - {0");

        openResetPasswordPage();

        assertEquals("Sign in to " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void invalidErrorPageTitleKeepsErrorPage() {
        setText("en", "loginTitle.error", "Error - {0");

        driver.open(keycloakUrls.getBaseBuilder().path("/realms/" + realm.getName() + "/nosuch").build().toString());
        errorPage.assertCurrent();

        assertEquals("", driver.driver().getTitle());
    }

    @Test
    public void logoutConfirmPageKeepsItsTitleWithoutPageTitle() {
        oauth.logoutForm().open();
        logoutConfirmPage.assertCurrent();

        assertEquals("Logging out", driver.driver().getTitle());
    }

    @Test
    public void logoutConfirmPageTitleCanBeReplaced() {
        setText("en", "loginTitle.logout-confirm", "Sign out of {0}");

        oauth.logoutForm().open();
        logoutConfirmPage.assertCurrent();

        assertEquals("Sign out of " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void pageTitleIsHtmlEscaped() {
        setText("en", "loginTitle.login-reset-password", "<b>Reset</b> & go - {0}");

        openResetPasswordPage();

        assertEquals("<b>Reset</b> & go - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
        assertThat(driver.driver().getPageSource(), containsString("&lt;b&gt;Reset&lt;/b&gt; &amp; go"));
    }

    @Test
    public void pageTitleUsesSelectedLocale() {
        setText("en", "loginTitle.login-reset-password", "Reset password - {0}");
        setText("de", "loginTitle.login-reset-password", "Passwort zurücksetzen - {0}");

        openResetPasswordPage();
        resetPasswordPage.selectLanguage("Deutsch");
        assertEquals("Deutsch", resetPasswordPage.getSelectedLanguage());

        assertEquals("Passwort zurücksetzen - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void pageTitleFallsBackToEnglishLikeOtherMessages() {
        setText("en", "loginTitle.login-reset-password", "Reset password - {0}");

        openResetPasswordPage();
        resetPasswordPage.selectLanguage("Deutsch");
        assertEquals("Deutsch", resetPasswordPage.getSelectedLanguage());

        assertEquals("Reset password - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void pageTitleCanComeFromTheme() {
        // The page-titles theme in keycloak-tests-custom-providers defines loginTitle.login-reset-password.
        realm.updateWithCleanup(r -> r.loginTheme("page-titles"));

        openResetPasswordPage();

        assertEquals("Theme reset password - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    @Test
    public void pageTitleUsesRealmNameWithoutDisplayName() {
        realm.updateWithCleanup(r -> r.displayName(""));
        setText("en", "loginTitle.login-reset-password", "Reset password - {0}");

        openResetPasswordPage();

        assertEquals("Reset password - " + realm.getName(), driver.driver().getTitle());
    }

    @Test
    public void pageTitleResolvesLocalizedRealmDisplayName() {
        realm.updateWithCleanup(r -> r.displayName("${titleTestRealmName}"));
        setText("en", "titleTestRealmName", "Localized Title Test");
        setText("en", "loginTitle.login-reset-password", "Reset password - {0}");

        openResetPasswordPage();

        assertEquals("Reset password - Localized Title Test", driver.driver().getTitle());
    }

    @Test
    public void pageTitleResolvesReferencesToOtherMessages() {
        setText("en", "titleTestResetPassword", "Reset password");
        setText("en", "loginTitle.login-reset-password", "${titleTestResetPassword} - {0}");

        openResetPasswordPage();

        assertEquals("Reset password - " + REALM_DISPLAY_NAME, driver.driver().getTitle());
    }

    private void openResetPasswordPage() {
        oauth.openLoginForm();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
    }

    private void setText(String locale, String key, String text) {
        RealmLocalizationResource localization = realm.admin().localization();
        localization.saveRealmLocalizationText(locale, key, text);
        realm.cleanup().add(r -> r.localization().deleteRealmLocalizationText(locale, key));
    }

    private void enableRequiredAction(String alias) {
        RequiredActionProviderRepresentation action = realm.admin().flows().getRequiredAction(alias);
        boolean enabled = action.isEnabled();
        action.setEnabled(true);
        realm.admin().flows().updateRequiredAction(alias, action);
        realm.cleanup().add(r -> {
            RequiredActionProviderRepresentation original = r.flows().getRequiredAction(alias);
            original.setEnabled(enabled);
            r.flows().updateRequiredAction(alias, original);
        });
    }

    public static class TitleRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.displayName(REALM_DISPLAY_NAME)
                    .resetPasswordAllowed(true)
                    .internationalizationEnabled(true)
                    .supportedLocales("en", "de")
                    .defaultLocale("en");
        }
    }

    public static class TotpSetupUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("totp-setup-user").password("password").email("totp-setup-user@localhost")
                    .name("Totp", "Setup").requiredActions(RequiredAction.CONFIGURE_TOTP.name());
        }
    }

    public static class OtpUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("otp-user").password("password").email("otp-user@localhost")
                    .name("Otp", "User").totpSecret(OTP_SECRET);
        }
    }

    public static class TermsUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("terms-user").password("password").email("terms-user@localhost")
                    .name("Terms", "User").requiredActions(RequiredAction.TERMS_AND_CONDITIONS.name());
        }
    }
}
