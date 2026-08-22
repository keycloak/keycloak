package org.keycloak.tests.forms;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.forms.RegistrationPassword;
import org.keycloak.authentication.requiredactions.VerifyEmailSuccessToken;
import org.keycloak.common.util.Time;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.ProceedPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.page.VerifyEmailPage;
import org.keycloak.testframework.ui.page.VerifyEmailSuccessPage;
import org.keycloak.testframework.ui.webdriver.BrowserTabUtils;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.keycloak.authentication.forms.RegistrationPassword.ALWAYS_SET_PASSWORD_ON_REGISTER_FORM;
import static org.keycloak.tests.admin.authentication.AbstractAuthenticationTest.findExecutionByProvider;
import static org.keycloak.tests.utils.PasswordGenerateUtil.generatePassword;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class RegisterWithEmailVerificationTest {

    @InjectWebDriver(ref = "driver", lifecycle = LifeCycle.CLASS)
    ManagedWebDriver driver;

    @InjectRealm(config = RegisterTestRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(ref = "oauth", webDriverRef = "driver")
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectMailServer
    MailServer mailServer;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage(ref = "loginPage", webDriverRef = "driver")
    LoginPage loginPage;

    @InjectPage(ref = "registerPage", webDriverRef = "driver")
    RegisterPage registerPage;

    @InjectPage(ref = "changePasswordPage", webDriverRef = "driver")
    protected LoginPasswordUpdatePage changePasswordPage;

    @InjectPage(ref = "verifyEmailPage", webDriverRef = "driver")
    VerifyEmailPage verifyEmailPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectWebDriver(ref = "driver2", lifecycle = LifeCycle.CLASS)
    ManagedWebDriver driver2;

    @InjectOAuthClient(ref = "oauth2", webDriverRef = "driver2")
    OAuthClient oauth2;

    @InjectPage(ref = "loginPage2", webDriverRef = "driver2")
    LoginPage loginPage2;

    @InjectPage(ref = "resetPasswordPage2", webDriverRef = "driver2")
    LoginPasswordResetPage resetPasswordPage2;

    @InjectPage(ref = "verifyEmailPage2", webDriverRef = "driver2")
    VerifyEmailPage verifyEmailPage2;

    @InjectPage(ref = "verifyEmailSuccessPage", webDriverRef = "driver")
    VerifyEmailSuccessPage verifyEmailSuccessPage;

    @InjectPage(ref = "changePasswordPage2", webDriverRef = "driver2")
    protected LoginPasswordUpdatePage changePasswordPage2;

    @InjectPage(ref = "proceedPage2", webDriverRef = "driver2")
    ProceedPage proceedPage2;

    @InjectPage(ref = "infoPage2", webDriverRef = "driver2")
    InfoPage infoPage2;

    @Test
    public void registerUserSuccessWithEmailVerification() {
        realm.updateWithCleanup((realmm) -> realmm.verifyEmail(true));

        registerUserSuccessWithEmailVerification(userId -> {
            try {
                MimeMessage message = mailServer.getReceivedMessages()[0];
                return MailUtils.getPasswordResetEmailLink(message);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });
    }

    @Test
    public void registerUserSuccessWithEmailVerification_emailVerifyDefaultAction() {
        // Don't enable "Verify email" realm switch, but rather switch VERIFY_EMAIL as a default required action
        AuthenticationManagementResource authMgmt = realm.admin().flows();
        RequiredActionProviderRepresentation reqAction = authMgmt.getRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name());
        reqAction.setDefaultAction(true);
        authMgmt.updateRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name(), reqAction);

        try {
            registerUserSuccessWithEmailVerification(userId -> {
                try {
                    MimeMessage message = mailServer.getReceivedMessages()[0];
                    return MailUtils.getPasswordResetEmailLink(message);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            });
        } finally {
            reqAction.setDefaultAction(false);
            authMgmt.updateRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name(), reqAction);
        }
    }

    @Test
    public void registerUserSuccessWithEmailVerificationWithResend() {
        realm.updateWithCleanup((realmm) -> realmm.verifyEmail(true));

        registerUserSuccessWithEmailVerification(userId -> {
            try {
                timeOffSet.set(40);

                // Re-send email verification link
                verifyEmailPage.clickResendEmail();
                verifyEmailPage.assertCurrent();

                EventRepresentation sendVerifyEmailEvent = events.poll();
                EventAssertion.assertSuccess(sendVerifyEmailEvent)
                        .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                        .userId(userId)
                        .type(EventType.SEND_VERIFY_EMAIL);

                // Get the last email
                MimeMessage message = mailServer.getLastReceivedMessage();
                return MailUtils.getPasswordResetEmailLink(message);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });
    }

    @Test
    public void registerUserSuccessWithEmailVerificationWithForgetPassword() throws Exception {
        realm.updateWithCleanup((realmm) -> {
            realmm.verifyEmail(true);
            realmm.resetPasswordAllowed(true);
            return realmm;
        });

        registerUserSuccessWithEmailVerificationWithForgetPasswordImpl();
    }

    @Test
    public void registerUserSuccessWithEmailVerificationWithForgetPassword_emailVerifyDefaultAction() throws Exception {
        realm.updateWithCleanup((realmm) -> {
            // Don't enable "Verify email" realm switch, but rather switch VERIFY_EMAIL as a default required action
            AuthenticationManagementResource authMgmt = realm.admin().flows();
            RequiredActionProviderRepresentation reqAction = authMgmt.getRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name());
            reqAction.setDefaultAction(true);
            authMgmt.updateRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name(), reqAction);

            realmm.resetPasswordAllowed(true);
            return realmm;
        });

        registerUserSuccessWithEmailVerificationWithForgetPasswordImpl();
    }

    // Issue 48206
    private void registerUserSuccessWithEmailVerificationWithForgetPasswordImpl() throws Exception {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // Password not shown initially on the registration page since verify-email is required
        Assert.assertFalse(registerPage.isPasswordPresent());
        registerPage.registerWithoutPassword("firstName", "lastName", "john@email.cz", "john");
        verifyEmailPage.assertCurrent();

        EventRepresentation registerEvent = events.poll();
        EventAssertion.assertSuccess(registerEvent)
                .clientId("test-app-oauth")
                .details(Details.USERNAME, "john")
                .details(Details.EMAIL, "john@email.cz")
                .details(Details.REGISTER_METHOD, "form")
                .type(EventType.REGISTER);
        String userId = registerEvent.getUserId();

        try {
            EventRepresentation sendVerifyEmailEvent = events.poll();
            EventAssertion.assertSuccess(sendVerifyEmailEvent)
                    .details(Details.EMAIL, "john@email.cz")
                    .userId(userId)
                    .type(EventType.SEND_VERIFY_EMAIL);

            // Browser2 - open login, click "Forget password" and fill username
            oauth2.openLoginForm();
            loginPage2.resetPassword();
            resetPasswordPage2.assertCurrent();
            resetPasswordPage2.changePassword("john@email.cz");

            // Receive the email and click it on browser2
            MimeMessage message = mailServer.getLastReceivedMessage();
            String forgetPasswordEmailLink = MailUtils.getPasswordResetEmailLink(message);
            driver2.open(forgetPasswordEmailLink);

            // Need to verify email now
            verifyEmailPage2.assertCurrent();
            message = mailServer.getLastReceivedMessage();
            String verifyEmailLink = MailUtils.getPasswordResetEmailLink(message);
            driver2.open(verifyEmailLink);

            // Browser 2 - update password and authenticate
            changePasswordPage2.assertCurrent();
            String password = generatePassword();
            changePasswordPage2.changePassword(password, password);

            String code = oauth2.parseLoginResponse().getCode();
            assertNotNull(code);

            // Browser 1 - refresh. Email was verified in browser 2, so we show the success page.
            driver.navigate().refresh();
            assertVerifyEmailSuccessPage();
        } finally {
            realm.admin().users().delete(userId).close();
        }
    }

    /**
     * @param receiveEmailFunction Income is userId. Outcome is link to password reset
     * @throws Exception
     */
    private void registerUserSuccessWithEmailVerification(Function<String, String> receiveEmailFunction) {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // Password not shown initially on the registration page since verify-email is required
        Assert.assertFalse(registerPage.isPasswordPresent());
        registerPage.registerWithoutPassword("firstName", "lastName", "registerUserSuccessWithEmailVerification@email", "registerUserSuccessWithEmailVerification");
        verifyEmailPage.assertCurrent();

        EventRepresentation registerEvent = events.poll();
        EventAssertion.assertSuccess(registerEvent)
                .clientId("test-app-oauth")
                .details(Details.USERNAME, "registerUserSuccessWithEmailVerification")
                .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email")
                .details(Details.REGISTER_METHOD, "form")
                .type(EventType.REGISTER);
        String userId = registerEvent.getUserId();

        try {
            EventRepresentation sendVerifyEmailEvent = events.poll();
            EventAssertion.assertSuccess(sendVerifyEmailEvent)
                    .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                    .userId(userId)
                    .type(EventType.SEND_VERIFY_EMAIL);

            String link = receiveEmailFunction.apply(userId);

            driver.open(link);

            EventRepresentation reqActionEmailEvent = events.poll();
            EventAssertion.assertSuccess(reqActionEmailEvent)
                    .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                    .userId(userId)
                    .type(EventType.VERIFY_EMAIL);

            // User is required to update password as a next step after email is verified
            updatePasswordOnChangePasswordPage(userId);

            assertUserRegistered(userId, "registerUserSuccessWithEmailVerification", "registerUserSuccessWithEmailVerification@email");

            String code = oauth.parseLoginResponse().getCode();
            assertNotNull(code);
        } finally {
            realm.admin().users().delete(userId).close();
        }
    }

    @Test
    public void registerUserSuccessWithEmailVerification_passwordOnRegisterForm() throws Exception {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((realmm) -> realmm.verifyEmail(true));
        String userId = null;
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserSuccessWithEmailVerification@email", "registerUserSuccessWithEmailVerification", generatePassword());
            verifyEmailPage.assertCurrent();

            EventRepresentation registerEvent = events.poll();
            EventAssertion.assertSuccess(registerEvent)
                    .clientId("test-app-oauth")
                    .details(Details.USERNAME, "registerUserSuccessWithEmailVerification")
                    .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email")
                    .details(Details.REGISTER_METHOD, "form")
                    .type(EventType.REGISTER);
            userId = registerEvent.getUserId();

            EventRepresentation sendVerifyEmailEvent = events.poll();
            EventAssertion.assertSuccess(sendVerifyEmailEvent)
                    .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                    .userId(userId)
                    .type(EventType.SEND_VERIFY_EMAIL);

            MimeMessage message = mailServer.getReceivedMessages()[0];
            String link = MailUtils.getPasswordResetEmailLink(message);

            driver.open(link);

            EventRepresentation reqActionEmailEvent = events.poll();
            EventAssertion.assertSuccess(reqActionEmailEvent)
                    .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                    .userId(userId)
                    .type(EventType.VERIFY_EMAIL);

            assertUserRegistered(userId, "registerUserSuccessWithEmailVerification", "registerUserSuccessWithEmailVerification@email");

            String code = oauth.parseLoginResponse().getCode();
            assertNotNull(code);
        } finally {
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    // Issue 51088 - Verify email link opened in a fresh browser (e.g. incognito tab) after Keycloak was restarted,
    // so the original root authentication session no longer exists.
    @Test
    public void registerUserSuccessWithEmailVerificationInFreshBrowserAfterRestart() throws Exception {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((realmm) -> realmm.verifyEmail(true));
        String userId = null;
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserSuccessWithEmailVerification@email", "registerUserSuccessWithEmailVerification", generatePassword());
            verifyEmailPage.assertCurrent();

            EventRepresentation registerEvent = events.poll();
            EventAssertion.assertSuccess(registerEvent)
                    .clientId("test-app-oauth")
                    .details(Details.USERNAME, "registerUserSuccessWithEmailVerification")
                    .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email")
                    .details(Details.REGISTER_METHOD, "form")
                    .type(EventType.REGISTER);
            userId = registerEvent.getUserId();

            EventRepresentation sendVerifyEmailEvent = events.poll();
            EventAssertion.assertSuccess(sendVerifyEmailEvent)
                    .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                    .userId(userId)
                    .type(EventType.SEND_VERIFY_EMAIL);

            MimeMessage message = mailServer.getReceivedMessages()[0];
            String link = MailUtils.getPasswordResetEmailLink(message);

            // Simulate a Keycloak restart before the user clicks the link: the in-memory root authentication
            // session created during registration is lost.
            removeRootAuthenticationSession();

            // Open the verification link in a fresh browser (e.g. an incognito tab) without any auth session cookie.
            // As the original authentication session is gone, a confirmation page asking to proceed is shown.
            driver2.open(link);
            proceedPage2.assertCurrent();
            proceedPage2.clickProceedLink();

            // Before the fix this failed with a NullPointerException because the original root authentication
            // session referenced by the token no longer existed. Now the email is verified successfully.
            infoPage2.assertCurrent();
            assertThat(infoPage2.getInfo(), is("Your email address has been verified."));

            EventRepresentation reqActionEmailEvent = events.poll();
            EventAssertion.assertSuccess(reqActionEmailEvent)
                    .details(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                    .userId(userId)
                    .type(EventType.VERIFY_EMAIL);

            assertTrue(getUser(userId).isEmailVerified());
        } finally {
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    // Issue 43896 - verify email original tab should not cause RESTART_AUTHENTICATION_ERROR
    @Test
    public void registerWithEmailVerification_originalTabShouldNotErrorAfterVerificationInNewTab() throws IOException {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((r) -> r.verifyEmail(true));
        String userId = null;
        try {
            // ── Browser 1 (original tab): register, land on verify-email page ──
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.register("originalFirstName", "originalLastName", "originalTabShouldNotErrorAfterVerificationInNewTab@email", "registerWithEmailVerification_originalTabShouldNotErrorAfterVerificationInNewTab", generatePassword());
            verifyEmailPage.assertCurrent();

            // Consume the REGISTER and SEND_VERIFY_EMAIL events
            userId = events.poll().getUserId();
            events.poll(); // SEND_VERIFY_EMAIL

            // ── Browser 2 (new tab): open the verification link ──
            MimeMessage message = mailServer.getLastReceivedMessage();
            String verifyLink = MailUtils.getPasswordResetEmailLink(message);
            driver2.open(verifyLink);

            // Browser 2 opens the link in a fresh auth session, so Keycloak shows a
            // confirmation page first (VerifyEmailActionTokenHandler.handleToken, fresh-session branch).
            // Click the confirm button to actually verify the email. This is the step that:
            //   (a) marks the user's email as verified
            //   (b) would previously have removed the authentication session browser 1 is holding, which is
            //       what left that tab failing with an expired code. It is now kept, because the session
            //       belongs to a registration.
            driver2.driver().findElement(By.cssSelector("#kc-info-message a")).click();

            // Consume VERIFY_EMAIL success event from browser 2
            EventRepresentation verifyEmailEvent = events.poll();
            EventAssertion.assertSuccess(verifyEmailEvent)
                    .type(EventType.VERIFY_EMAIL);

            // ── Browser 1 (original tab): both routes to the confirmation ──
            String verifyEmailUrl = driver.driver().getCurrentUrl();
            String pollTarget = sessionPollingRedirectUrl(driver.driver().getPageSource());

            // Route 1: the verify-email-success endpoint, which is what authChecker.js navigates to once a
            // session cookie appears. The email really is verified now, so it must confirm.
            driver.open(pollTarget);
            assertVerifyEmailSuccessPage();

            // Route 2: re-entering the required action. VerifyEmail.process() detects isEmailVerified()==true
            // with NEW_USER_REGISTERED set and renders the success page rather than erroring.
            driver.open(verifyEmailUrl);
            assertVerifyEmailSuccessPage();

            // This route renders through context.form(), which attaches the authentication session, so the
            // success page must suppress both session scripts itself rather than relying on being detached.
            assertNoSessionScripts(driver.driver().getPageSource());
        } finally {
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    /**
     * Removes the root authentication session held by the first browser on the server, simulating the loss of
     * in-memory authentication sessions that happens when Keycloak is restarted.
     */
    private void removeRootAuthenticationSession() {
        String encodedAuthSessionId = driver.driver().manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName()).getValue();
        String realmId = realm.getId();
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealm(realmId);
            session.getContext().setRealm(realmModel);

            AuthenticationSessionManager authenticationSessionManager = new AuthenticationSessionManager(session);
            String rootAuthSessionId = authenticationSessionManager.decodeBase64AndValidateSignature(encodedAuthSessionId);
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realmModel, rootAuthSessionId);
            if (rootAuthSession != null) {
                session.authenticationSessions().removeRootAuthenticationSession(realmModel, rootAuthSession);
            }
        });
    }

    /**
     * Issue 43896 in its reported form: the verification link is opened in a second tab of the SAME browser, so
     * both tabs share one authentication session. Completing the login in the second tab consumes that session,
     * which is why the confirmation cannot be keyed on it. This also never reaches the fresh-session branch of
     * VerifyEmailActionTokenHandler, unlike the separate-browser case above.
     *
     * The polling navigation itself is simulated, because the default HtmlUnit driver does not execute the ES
     * module that authChecker.js is loaded as.
     */
    @Test
    public void registerWithEmailVerification_originalTabIsConfirmedAfterVerificationInSameBrowserTab() throws Exception {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((r) -> r.verifyEmail(true));
        String userId = null;
        BrowserTabUtils tabs = driver.tabs();
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.register("sameBrowserFirstName", "sameBrowserLastName", "sameBrowserTab@email",
                    "registerWithEmailVerification_originalTabIsConfirmedAfterVerificationInSameBrowserTab", generatePassword());
            verifyEmailPage.assertCurrent();

            userId = events.poll().getUserId();
            events.poll(); // SEND_VERIFY_EMAIL

            String pollTarget = sessionPollingRedirectUrl(driver.driver().getPageSource());

            MimeMessage message = mailServer.getLastReceivedMessage();
            String verifyLink = MailUtils.getPasswordResetEmailLink(message);

            // Second tab of the same browser - shares the AUTH_SESSION_ID cookie with the original tab.
            tabs.newTab(verifyLink);

            EventRepresentation verifyEmailEvent = events.poll();
            EventAssertion.assertSuccess(verifyEmailEvent).type(EventType.VERIFY_EMAIL);

            // Back on the original tab, following what the polling would navigate to.
            tabs.switchToTab(0);
            driver.open(pollTarget);

            assertVerifyEmailSuccessPage();
        } finally {
            tabs.closeTabs();
            // Unlike the separate-browser case, the second tab here goes on to complete the login. Both the
            // session it leaves on this shared driver and the events it emits would otherwise be picked up by
            // whichever test runs next.
            driver.cookies().deleteAll();
            events.skipAll();
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    /**
     * The polling token is bounded by the lifespan of the verification email it accompanies, so re-rendering the
     * page must reuse the expiry rather than recompute it - otherwise refreshing repeatedly would keep pushing
     * the token out past the link it belongs to. The expiry is the only varying input, so an unchanged token
     * string is what shows it was reused.
     */
    @Test
    public void refreshingVerifyEmailPageDoesNotExtendPollingToken() throws IOException {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((r) -> r.verifyEmail(true));

        String userId = null;
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.register("refreshFirstName", "refreshLastName", "refreshPollingToken@email",
                    "refreshingVerifyEmailPageDoesNotExtendPollingToken", generatePassword());
            verifyEmailPage.assertCurrent();

            userId = events.poll().getUserId();
            events.poll(); // SEND_VERIFY_EMAIL

            String firstPollTarget = sessionPollingRedirectUrl(driver.driver().getPageSource());

            timeOffSet.set(30);
            driver.navigate().refresh();
            verifyEmailPage.assertCurrent();

            assertThat(sessionPollingRedirectUrl(driver.driver().getPageSource()), is(firstPollTarget));
        } finally {
            timeOffSet.set(0);
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    /**
     * Changing the email while the page is open makes the next render send a fresh verification link. The
     * polling token has to be re-based on that link, rather than keeping the expiry of the email it replaced -
     * otherwise polling can stop working while the link the user actually received is still valid.
     */
    @Test
    public void changingEmailReissuesPollingTokenExpiry() throws IOException {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((r) -> r.verifyEmail(true));

        String userId = null;
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.register("changedFirstName", "changedLastName", "beforeChange@email",
                    "changingEmailReissuesPollingTokenExpiry", generatePassword());
            verifyEmailPage.assertCurrent();

            userId = events.poll().getUserId();
            events.poll(); // SEND_VERIFY_EMAIL

            String beforeChange = sessionPollingRedirectUrl(driver.driver().getPageSource());

            UserRepresentation user = realm.admin().users().get(userId).toRepresentation();
            user.setEmail("afterchange@email");
            realm.admin().users().get(userId).update(user);

            // Offset the clock so a re-based expiry is distinguishable from the retained one.
            timeOffSet.set(30);
            driver.navigate().refresh();
            verifyEmailPage.assertCurrent();

            assertThat("Polling token must be re-based on the newly sent verification link",
                    sessionPollingRedirectUrl(driver.driver().getPageSource()), Matchers.not(is(beforeChange)));
        } finally {
            timeOffSet.set(0);
            events.skipAll();
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    /**
     * Clicking resend inside the cooldown window re-renders the verify-email page from a separate request, so it
     * builds its own form. It must still point polling at the verify-email-success endpoint - falling back to
     * the default scripts would reintroduce the redirect this fixes.
     */
    @Test
    public void resendCooldownPageKeepsPollingToVerifyEmailSuccessEndpoint() throws IOException {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((r) -> r.verifyEmail(true));

        String userId = null;
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.register("cooldownFirstName", "cooldownLastName", "resendCooldown@email",
                    "resendCooldownPageKeepsPollingToVerifyEmailSuccessEndpoint", generatePassword());
            verifyEmailPage.assertCurrent();

            userId = events.poll().getUserId();
            events.poll(); // SEND_VERIFY_EMAIL

            // No time offset, so the cooldown from the first send is still in force and this hits the retry page.
            verifyEmailPage.clickResendEmail();
            verifyEmailPage.assertCurrent();

            // Confirms the retry branch was actually taken, rather than a normal re-render that would configure
            // the polling anyway and make the assertions below vacuous.
            assertThat(verifyEmailPage.getErrorMessage().orElse(null), notNullValue());

            String pageSource = driver.driver().getPageSource();
            assertTrue("Resend cooldown page must keep polling the verify-email-success endpoint: " + pageSource,
                    pageSource.contains("/login-actions/verify-email-success"));
            assertFalse("Resend cooldown page must not re-enable checkAuthSession: " + pageSource,
                    pageSource.contains("checkAuthSession"));
        } finally {
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    /**
     * The confirmation is detached, so the signed token in the query string is the only thing identifying the
     * registration. The language links must carry it, otherwise switching language lands on an endpoint that can
     * no longer confirm anything.
     */
    @Test
    public void verifyEmailSuccessPageKeepsTokenWhenSwitchingLanguage() throws Exception {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((r) -> {
            r.verifyEmail(true);
            r.internationalizationEnabled(true);
            r.supportedLocales("en", "de");
            r.defaultLocale("en");
            return r;
        });

        String userId = null;
        BrowserTabUtils tabs = driver.tabs();
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.register("localeFirstName", "localeLastName", "localeSwitch@email",
                    "verifyEmailSuccessPageKeepsTokenWhenSwitchingLanguage", generatePassword());
            verifyEmailPage.assertCurrent();

            userId = events.poll().getUserId();
            events.poll(); // SEND_VERIFY_EMAIL

            String pollTarget = sessionPollingRedirectUrl(driver.driver().getPageSource());

            MimeMessage message = mailServer.getLastReceivedMessage();
            tabs.newTab(MailUtils.getPasswordResetEmailLink(message));

            EventAssertion.assertSuccess(events.poll()).type(EventType.VERIFY_EMAIL);

            tabs.switchToTab(0);
            driver.open(pollTarget);
            assertVerifyEmailSuccessPage();

            String localeUrl = localeSwitchUrl(driver.driver().getPageSource());
            assertTrue("Language switch URL must keep the signed token: " + localeUrl,
                    localeUrl.contains(Constants.KEY + "="));

            // Following it must still confirm; the text is no longer English, so only the page is checked.
            driver.open(localeUrl);
            verifyEmailSuccessPage.assertCurrent();
        } finally {
            tabs.closeTabs();
            driver.cookies().deleteAll();
            events.skipAll();
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    /**
     * A language link from the rendered page. Values are HTML attributes, so ampersands come back escaped.
     */
    private String localeSwitchUrl(String pageSource) {
        Matcher matcher = Pattern.compile("\"([^\"]*kc_locale=[^\"]*)\"").matcher(pageSource);
        assertTrue("Expected a language switch URL in the page source: " + pageSource, matcher.find());

        return matcher.group(1).replace("&amp;", "&");
    }

    /**
     * Decoding only verifies the signature, so any token the realm signed deserializes into this class, with
     * whatever claims it happens to share. One without a user id must take the fallback path rather than being
     * looked up - resolving a null id fails inside StorageId and would surface as a 500.
     */
    @Test
    public void verifyEmailSuccessEndpointRejectsSignedTokenWithoutUserId() {
        String realmId = realm.getId();
        String tokenString = runOnServer.fetchString(session -> {
            RealmModel realmModel = session.realms().getRealm(realmId);
            session.getContext().setRealm(realmModel);

            return session.tokens().encode(new VerifyEmailSuccessToken(null, Time.currentTime() + 60));
        });

        driver.open(realm.getBaseUrl() + "/login-actions/verify-email-success?" + Constants.KEY + "=" + tokenString);

        assertNotVerifyEmailSuccessPage();
    }

    /**
     * The endpoint is reachable by anyone. Without a valid signed token naming a registration it must not claim
     * that an email was verified.
     */
    @Test
    public void verifyEmailSuccessEndpointDoesNotConfirmWithoutRegistrationSession() {
        driver.open(realm.getBaseUrl() + "/login-actions/verify-email-success");

        assertNotVerifyEmailSuccessPage();
    }

    /**
     * Session polling navigates here as soon as any session cookie appears, which also happens when an unrelated
     * account logs in elsewhere - not only when this registration's email was verified. The endpoint must
     * re-check server side, so a pending registration whose email is still unverified gets no confirmation.
     */
    @Test
    public void verifyEmailSuccessEndpointDoesNotConfirmWhileEmailUnverified() throws IOException {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((r) -> r.verifyEmail(true));

        String userId = null;
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.register("unverifiedFirstName", "unverifiedLastName", "pollWhileUnverified@email",
                    "verifyEmailSuccessEndpointDoesNotConfirmWhileEmailUnverified", generatePassword());
            verifyEmailPage.assertCurrent();

            userId = events.poll().getUserId();
            events.poll(); // SEND_VERIFY_EMAIL

            // Follow the polling target exactly as authChecker.js would, but without ever verifying the email.
            driver.open(sessionPollingRedirectUrl(driver.driver().getPageSource()));

            assertNotVerifyEmailSuccessPage();
        } finally {
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    /**
     * The URL authChecker.js would navigate to once a session appears. It carries a signed token, so it has to
     * be read back out of the rendered page rather than reconstructed here.
     */
    private String sessionPollingRedirectUrl(String pageSource) {
        Matcher matcher = Pattern.compile("startSessionPolling\\(\\s*\"([^\"]+)\"\\s*\\)").matcher(pageSource);
        assertTrue("Could not find a session polling URL in the page source: " + pageSource, matcher.find());

        return unescapeJavaScript(matcher.group(1));
    }

    private String unescapeJavaScript(String value) {
        StringBuilder unescaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '\\' || i + 1 >= value.length()) {
                unescaped.append(current);
                continue;
            }

            char next = value.charAt(i + 1);
            if (next == 'u' && i + 5 < value.length()) {
                unescaped.append((char) Integer.parseInt(value.substring(i + 2, i + 6), 16));
                i += 5;
            } else {
                unescaped.append(next);
                i++;
            }
        }
        return unescaped.toString();
    }

    /**
     * Checks the message resolved as well as the page. A missing message key renders as the key name itself,
     * which assertCurrent() alone would not notice - the page id comes from the template name, not its content.
     */
    private void assertVerifyEmailSuccessPage() {
        verifyEmailSuccessPage.assertCurrent();
        assertThat(verifyEmailSuccessPage.getInstruction(), is("Your email address has been verified."));
    }

    private void assertNotVerifyEmailSuccessPage() {
        assertFalse("Endpoint must not confirm verification: " + driver.driver().getCurrentUrl(),
                driver.driver().getPageSource().contains("data-page-id=\"login-login-verify-email-success\""));
    }

    /**
     * The success page is terminal. Both authChecker entry points navigate back into the authentication flow,
     * which - with the email already verified - completes and redirects to the client, so neither may appear.
     */
    private void assertNoSessionScripts(String pageSource) {
        assertFalse("verify-email-success page must not start session polling, but page source was: " + pageSource,
                pageSource.contains("startSessionPolling"));
        assertFalse("verify-email-success page must not run checkAuthSession, but page source was: " + pageSource,
                pageSource.contains("checkAuthSession"));
    }

    /**
     * The verify-email page shown during registration must point its session polling at the standalone
     * success endpoint. Polling the default ssoLoginInOtherTabsUrl re-enters the authentication flow, which
     * completes and redirects to the client once the email is verified, so the user never sees a confirmation.
     */
    @Test
    public void registrationVerifyEmailPagePollsToVerifyEmailSuccessEndpoint() throws IOException {
        String authConfigId = enableAlwaysSetPasswordOnRegisterForm();
        realm.updateWithCleanup((r) -> r.verifyEmail(true));

        String userId = null;
        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.register("pollFirstName", "pollLastName", "pollToVerifyEmailSuccess@email",
                    "registrationVerifyEmailPagePollsToVerifyEmailSuccessEndpoint", generatePassword());
            verifyEmailPage.assertCurrent();

            userId = events.poll().getUserId();
            events.poll(); // SEND_VERIFY_EMAIL

            String pageSource = driver.driver().getPageSource();
            assertTrue("Expected session polling to target the verify-email-success endpoint, but page source was: " + pageSource,
                    pageSource.contains("/login-actions/verify-email-success"));
        } finally {
            disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    private void updatePasswordOnChangePasswordPage(String userId) {
        changePasswordPage.assertCurrent();
        String password = generatePassword();
        changePasswordPage.changePassword(password, password);

        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .userId(userId)
                .type(EventType.UPDATE_PASSWORD);
        event = events.poll();
        EventAssertion.assertSuccess(event)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .userId(userId)
                .type(EventType.UPDATE_CREDENTIAL);
    }

    private UserRepresentation assertUserRegistered(String userId, String username, String email) {
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .details("username", username.toLowerCase())
                .type(EventType.LOGIN);

        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);
        assertUserBasicRegisterAttributes(userId, username, email, "firstName", "lastName");
        return user;
    }

    private UserRepresentation getUser(String userId) {
        return realm.admin().users().get(userId).toRepresentation();
    }

    private void assertUserBasicRegisterAttributes(String userId, String username, String email, String firstName, String lastName) {
        UserRepresentation user = getUser(userId);
        assertThat(user, notNullValue());

        if (username != null) {
            assertThat(username, Matchers.equalToIgnoringCase(user.getUsername()));
        }
        assertThat(email.toLowerCase(), is(user.getEmail()));
        assertThat(firstName, is(user.getFirstName()));
        assertThat(lastName, is(user.getLastName()));
    }

    private String enableAlwaysSetPasswordOnRegisterForm() {
        AuthenticatorConfigRepresentation cfg = new AuthenticatorConfigRepresentation();
        cfg.setAlias("reg-password");
        Map<String, String> cfgMap = Map.of(ALWAYS_SET_PASSWORD_ON_REGISTER_FORM, "true");
        cfg.setConfig(cfgMap);

        AuthenticationManagementResource authMgmtResource = realm.admin().flows();
        AuthenticationExecutionInfoRepresentation authExecution = findExecutionByProvider(RegistrationPassword.PROVIDER_ID, authMgmtResource.getExecutions(DefaultAuthenticationFlows.REGISTRATION_FLOW));
        Response resp = authMgmtResource.newExecutionConfig(authExecution.getId(), cfg);
        resp.close();
        return ApiUtil.getCreatedId(resp);
    }

    private void disableAlwaysSetPasswordOnRegisterForm(String configId) {
        AuthenticationManagementResource authMgmtResource = realm.admin().flows();
        AuthenticatorConfigRepresentation cfg = authMgmtResource.getAuthenticatorConfig(configId);
        cfg.getConfig().put(ALWAYS_SET_PASSWORD_ON_REGISTER_FORM, "false");
        authMgmtResource.updateAuthenticatorConfig(configId, cfg);
    }

    public static class RegisterTestRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.registrationAllowed(true);
            return realm;
        }
    }

}
