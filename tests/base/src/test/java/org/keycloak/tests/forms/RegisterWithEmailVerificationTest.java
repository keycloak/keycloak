package org.keycloak.tests.forms;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.forms.RegistrationPassword;
import org.keycloak.authentication.requiredactions.VerifyEmail;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
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
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.testframework.ui.page.ProceedPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.page.VerifyEmailPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.keycloak.authentication.forms.RegistrationPassword.ALWAYS_SET_PASSWORD_ON_REGISTER_FORM;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;
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

    @InjectPage(ref = "loginUsernamePage", webDriverRef = "driver")
    LoginUsernamePage loginUsernamePage;

    @InjectPage(ref = "passwordPage", webDriverRef = "driver")
    PasswordPage passwordPage;

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
            assertTrue(mailServer.waitForIncomingEmail(2));
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
            oauth2.openLoginForm();
            loginPage2.fillLogin("john", password);
            loginPage2.submit();

            String code = oauth2.parseLoginResponse().getCode();
            assertNotNull(code);

            // Browser 1 - refresh. Should be still on verifyEmail
            driver.navigate().refresh();
            loginPage.assertCurrent();
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

    enum RegistrationEntryPoint {
        REGISTRATIONS_ENDPOINT {
            @Override
            void open(OAuthClient oauth, PkceGenerator pkce, LoginPage loginPage) {
                oauth.registrationForm().codeChallenge(pkce).open();
            }
        },
        PROMPT_CREATE {
            @Override
            void open(OAuthClient oauth, PkceGenerator pkce, LoginPage loginPage) {
                oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_CREATE).codeChallenge(pkce).open();
            }
        },
        LOGIN_PAGE_REGISTER_LINK {
            @Override
            void open(OAuthClient oauth, PkceGenerator pkce, LoginPage loginPage) {
                oauth.loginForm().codeChallenge(pkce).open();
                loginPage.clickRegister();
            }
        };

        abstract void open(OAuthClient oauth, PkceGenerator pkce, LoginPage loginPage);
    }

    // Issue 55287 - Email verified in another browser than the one used for registration.
    // The original browser can't continue the deleted authentication session, but restarts to the login page within the
    // client's original authorization request, with the registered username pre-filled. With the VerifyEmail
    // "continue as registered user" option, the login restarts with the registered user already chosen.
    @ParameterizedTest
    @CsvSource({
            "REGISTRATIONS_ENDPOINT, false, COMBINED, false",
            "REGISTRATIONS_ENDPOINT, true, COMBINED, false",
            "PROMPT_CREATE, false, COMBINED, false",
            "PROMPT_CREATE, true, COMBINED, false",
            "LOGIN_PAGE_REGISTER_LINK, false, COMBINED, false",
            "LOGIN_PAGE_REGISTER_LINK, true, COMBINED, false",
            "REGISTRATIONS_ENDPOINT, false, COMBINED, true",
            "REGISTRATIONS_ENDPOINT, true, COMBINED, true",
            "LOGIN_PAGE_REGISTER_LINK, false, COMBINED, true",
            "REGISTRATIONS_ENDPOINT, false, IDENTITY_FIRST, false",
            "REGISTRATIONS_ENDPOINT, false, IDENTITY_FIRST, true",
            "PROMPT_CREATE, true, IDENTITY_FIRST, true"
    })
    public void registerFromClientAndVerifyEmailInOtherBrowser(RegistrationEntryPoint entryPoint, boolean alwaysSetPasswordOnRegisterForm,
                                                               LoginFlow loginFlow, boolean continueAsRegisteredUser) throws Exception {
        String authConfigId = alwaysSetPasswordOnRegisterForm ? enableAlwaysSetPasswordOnRegisterForm() : null;
        realm.updateWithCleanup((realmm) -> realmm.verifyEmail(true));
        loginFlow.configure(runOnServer, realm);
        if (continueAsRegisteredUser) {
            setVerifyEmailConfig(Map.of(VerifyEmail.CONTINUE_AS_REGISTERED_USER, "true"));
        }
        String username = "crossbrowser-" + entryPoint.name().toLowerCase() + "-" + alwaysSetPasswordOnRegisterForm
                + "-" + loginFlow.name().toLowerCase() + "-" + continueAsRegisteredUser;
        String email = username + "@email.cz";
        String password = generatePassword();
        PkceGenerator pkce = PkceGenerator.s256();
        String userId = null;
        try {
            deleteRealmCookies(driver, oauth);
            deleteRealmCookies(driver2, oauth2);

            // Browser 1 - registration
            entryPoint.open(oauth, pkce, loginPage);
            registerPage.assertCurrent();
            if (alwaysSetPasswordOnRegisterForm) {
                registerPage.register("firstName", "lastName", email, username, password);
            } else {
                registerPage.registerWithoutPassword("firstName", "lastName", email, username);
            }
            verifyEmailPage.assertCurrent();

            EventRepresentation registerEvent = events.poll();
            EventAssertion.assertSuccess(registerEvent).type(EventType.REGISTER);
            userId = registerEvent.getUserId();

            // Browser 2 - verify email
            assertTrue(mailServer.waitForIncomingEmail(1));
            String link = MailUtils.getPasswordResetEmailLink(mailServer.getLastReceivedMessage());
            driver2.open(link);
            proceedPage2.assertCurrent();
            proceedPage2.clickProceedLink();
            if (!alwaysSetPasswordOnRegisterForm) {
                changePasswordPage2.assertCurrent();
                changePasswordPage2.changePassword(password, password);
                infoPage2.assertCurrent();
                assertThat(infoPage2.getInfo(), is("Your account has been updated."));
            } else {
                infoPage2.assertCurrent();
                assertThat(infoPage2.getInfo(), is("Your email address has been verified."));
            }

            // Browser 1 - refresh. Authentication session was removed, so the login restarts from the beginning,
            // telling the user that their email is verified rather than that the login timed out
            driver.navigate().refresh();
            events.clear();
            loginAfterRegistrationRestart(loginFlow, continueAsRegisteredUser, username, password);
            String code = oauth.parseLoginResponse().getCode();
            assertNotNull(code);
            EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN).userId(userId);

            AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code).codeVerifier(pkce).send();
            assertTrue(tokenResponse.isSuccess());
            assertThat(oauth.parseToken(tokenResponse.getAccessToken(), AccessToken.class).getSubject(), is(userId));
        } finally {
            if (continueAsRegisteredUser) {
                realm.admin().flows().removeRequiredActionConfig(UserModel.RequiredAction.VERIFY_EMAIL.name());
            }
            if (authConfigId != null) {
                disableAlwaysSetPasswordOnRegisterForm(authConfigId);
            }
            if (userId != null) {
                realm.admin().users().delete(userId).close();
            }
        }
    }

    enum LoginFlow {
        // Default browser flow: username and password on one form
        COMBINED {
            @Override
            void configure(RunOnServerClient runOnServer, ManagedRealm realm) {
            }
        },
        // Username, then password, on separate forms
        IDENTITY_FIRST {
            @Override
            void configure(RunOnServerClient runOnServer, ManagedRealm realm) {
                realm.dirty();
                runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                        .copyBrowserFlow("identity-first-browser")
                        .inForms(forms -> forms
                                .clear()
                                .addAuthenticatorExecution(REQUIRED, UsernameFormFactory.PROVIDER_ID)
                                .addAuthenticatorExecution(REQUIRED, PasswordFormFactory.PROVIDER_ID))
                        .defineAsBrowserFlow());
            }
        };

        abstract void configure(RunOnServerClient runOnServer, ManagedRealm realm);
    }

    /**
     * Browser 1 after the registration restarted following email verification in browser 2: shows that the email is
     * verified, then logs in as the registered user. With continueAsRegisteredUser the user is already chosen, so just
     * the password is asked for. Otherwise the username is asked for first, pre-filled.
     */
    private void loginAfterRegistrationRestart(LoginFlow loginFlow, boolean continueAsRegisteredUser, String username, String password) {
        String emailVerified = "Your email address has been verified.";
        if (loginFlow == LoginFlow.COMBINED) {
            loginPage.assertCurrent();
            assertThat(loginPage.getSuccessMessage().orElse(null), is(emailVerified));
            assertTrue(loginPage.getErrorMessage().isEmpty());
            if (continueAsRegisteredUser) {
                assertFalse(loginPage.isUsernameInputPresent());
                assertThat(loginPage.getAttemptedUsername(), is(username));
                loginPage.fillPassword(password);
            } else {
                assertThat(loginPage.getUsername(), is(username));
                loginPage.fillLogin(username, password);
            }
            loginPage.submit();
        } else {
            if (continueAsRegisteredUser) {
                passwordPage.assertCurrent();
                assertThat(passwordPage.getSuccessMessage().orElse(null), is(emailVerified));
            } else {
                loginUsernamePage.assertCurrent();
                assertThat(loginUsernamePage.getSuccessMessage().orElse(null), is(emailVerified));
                assertThat(loginUsernamePage.getUsername(), is(username));
                loginUsernamePage.submit();
                passwordPage.assertCurrent();
            }
            assertThat(passwordPage.getAttemptedUsername(), is(username));
            passwordPage.fillPassword(password);
            passwordPage.submit();
        }
    }

    private void setVerifyEmailConfig(Map<String, String> config) {
        RequiredActionConfigRepresentation rep = new RequiredActionConfigRepresentation();
        rep.setConfig(config);
        realm.admin().flows().updateRequiredActionConfig(UserModel.RequiredAction.VERIFY_EMAIL.name(), rep);
    }

    // Issue 55287 - Registration started by the client, and authentication session expired before the form was submitted.
    // Restarts to the registration form, like a restart of a live authentication session.
    @ParameterizedTest
    @EnumSource(value = RegistrationEntryPoint.class, names = {"REGISTRATIONS_ENDPOINT", "PROMPT_CREATE"})
    public void registerFromClientRestartsToRegistrationAfterAuthSessionRemoved(RegistrationEntryPoint entryPoint) {
        String username = "expired-" + entryPoint.name().toLowerCase();
        deleteRealmCookies(driver, oauth);
        entryPoint.open(oauth, PkceGenerator.s256(), loginPage);
        registerPage.assertCurrent();

        removeRootAuthenticationSession();

        registerPage.registerWithoutPassword("firstName", "lastName", username + "@email.cz", username);
        registerPage.assertCurrent();
        assertThat(registerPage.getErrorMessage().orElse(null), is("Your login attempt timed out. Login will start from the beginning."));
        assertTrue(realm.admin().users().searchByUsername(username, true).isEmpty());
    }

    // Issue 55287 - Authentication session of the registration lost (expired, or Keycloak restarted) before the email was
    // verified. The restart is a plain timeout, as there is no verified email to report.
    @ParameterizedTest
    @EnumSource(RegistrationEntryPoint.class)
    public void registerFromClientAndLoseAuthSessionBeforeVerifyingEmail(RegistrationEntryPoint entryPoint) {
        realm.updateWithCleanup((realmm) -> realmm.verifyEmail(true));
        String username = "unverified-" + entryPoint.name().toLowerCase();
        try {
            deleteRealmCookies(driver, oauth);
            entryPoint.open(oauth, PkceGenerator.s256(), loginPage);
            registerPage.assertCurrent();
            registerPage.registerWithoutPassword("firstName", "lastName", username + "@email.cz", username);
            verifyEmailPage.assertCurrent();

            removeRootAuthenticationSession();

            driver.navigate().refresh();
            loginPage.assertCurrent();
            assertThat(loginPage.getErrorMessage().orElse(null), is("Your login attempt timed out. Login will start from the beginning."));
            assertTrue(loginPage.getSuccessMessage().isEmpty());
            assertThat(loginPage.getUsername(), is(username));
        } finally {
            realm.admin().users().searchByUsername(username, true)
                    .forEach(user -> realm.admin().users().delete(user.getId()).close());
        }
    }

    /**
     * Deletes the cookies left by earlier tests (e.g. KC_RESTART). WebDriver only deletes cookies visible to the current
     * page, so open a page under the realm path first.
     */
    private void deleteRealmCookies(ManagedWebDriver webDriver, OAuthClient oauthClient) {
        webDriver.open(oauthClient.getEndpoints().getOpenIDConfiguration());
        webDriver.cookies().deleteAll();
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
