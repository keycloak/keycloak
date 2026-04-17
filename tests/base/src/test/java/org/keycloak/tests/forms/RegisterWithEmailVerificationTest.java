package org.keycloak.tests.forms;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.forms.RegistrationPassword;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.page.VerifyEmailPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.keycloak.authentication.forms.RegistrationPassword.ALWAYS_SET_PASSWORD_ON_REGISTER_FORM;
import static org.keycloak.tests.admin.authentication.AbstractAuthenticationTest.findExecutionByProvider;
import static org.keycloak.tests.utils.PasswordGenerateUtil.generatePassword;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class RegisterWithEmailVerificationTest {

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectRealm(config = RegisterTestRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectMailServer
    MailServer mailServer;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    RegisterPage registerPage;

    @InjectPage
    protected LoginPasswordUpdatePage changePasswordPage;

    @InjectPage
    VerifyEmailPage verifyEmailPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

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
                .clientId("test-app")
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
                    .clientId("test-app")
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
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.registrationAllowed(true);
            return realm;
        }
    }

}
