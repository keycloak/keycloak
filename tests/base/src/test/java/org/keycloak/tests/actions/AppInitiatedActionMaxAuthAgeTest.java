/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.actions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.Constants;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.StickySessionEncoderProvider;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.PasswordGenerateUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;

@KeycloakIntegrationTest
public class AppInitiatedActionMaxAuthAgeTest {

    private static final String PASSWORD = PasswordGenerateUtil.generatePassword();

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = TestUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser testUser;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginPasswordUpdatePage changePasswordPage;

    @InjectPage
    LoginUpdateProfilePage updateProfilePage;

    @InjectPage
    LoginConfigTotpPage configTotpPage;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectMailServer
    MailServer mailServer;

    @AfterEach
    public void resetMaxAuthAge() {
        timeOffSet.set(0);
        // The realm is shared by all tests of this class, so undo whatever a test changed on the required actions
        for (RequiredActionProviderRepresentation a : realm.admin().flows().getRequiredActions()) {
            if (a.getConfig() != null && a.getConfig().remove(Constants.MAX_AUTH_AGE_KEY) != null) {
                realm.admin().flows().updateRequiredAction(a.getAlias(), a);
            }
        }
    }

    @Test
    public void staleActionWindowTriggersReAuthEvenWithFreshSession() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        changePasswordPage.assertCurrent();
        timeOffSet.set(35);

        changePasswordPage.changePassword("New-password1!", "New-password1!");

        loginPage.assertCurrent();

        // Recover: re-authenticate and complete
        timeOffSet.set(0);
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void ssoEntryLateInWindowDoesNotExtendMaxAuthAgeAtCommit() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        // Still within the 30s window, so the AIA is entered with the SSO cookie only (no re-authentication)
        timeOffSet.set(25);
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        changePasswordPage.assertCurrent();

        // The real authentication is now older than max_auth_age. Entering the AIA via SSO must not have restarted the clock
        timeOffSet.set(35);
        changePasswordPage.changePassword("New-password1!", "New-password1!");

        loginPage.assertCurrent();

        // Recover: re-authenticate and complete
        timeOffSet.set(0);
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void ssoEntryWithDifferentRootAuthSessionIdDoesNotLoopOnCommit() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        useRootAuthSessionWithDifferentIdThanUserSession();

        // SSO entry (cookie only). The user session ID differs from the root authentication session ID
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        changePasswordPage.assertCurrent();

        timeOffSet.set(10);

        // The commit check must use the same user session as the entry check (identity cookie), so there is no re-authentication
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void ssoEntryWithDifferentRootAuthSessionIdStillEnforcesMaxAuthAgeOnCommit() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        useRootAuthSessionWithDifferentIdThanUserSession();

        timeOffSet.set(25);
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        changePasswordPage.assertCurrent();

        // The real authentication is now older than max_auth_age
        timeOffSet.set(35);
        changePasswordPage.changePassword("New-password1!", "New-password1!");

        loginPage.assertCurrent();
    }

    @Test
    public void freshWindowAllowsActionCommitWithoutReAuth() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();

        // Advance by only 10s - within the 30s window
        timeOffSet.set(10);

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void maxAuthAgeZeroAlwaysRequiresReAuth() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 0);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        timeOffSet.set(1);

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();

        loginPage.assertCurrent();
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void maxAuthAgeZeroRequiresReAuthInSameSecondAsLogin() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 0);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        // No time offset: the action is started within the same second as the login
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();

        loginPage.assertCurrent();
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void maxAuthAgeZeroCommitSucceedsRegardlessOfElapsedTime() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 0);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();

        // Re-authentication forced on entry
        loginPage.assertCurrent();
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        changePasswordPage.assertCurrent();

        // Even after significant time, the commit succeeds because the user re-authenticated in this flow
        timeOffSet.set(120);

        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void actionNotCommittedWhenUserSessionRemovedAndMaxAuthAgeExpired() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        String sessionId = pollLoginSuccess().getEvent().getSessionId();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        changePasswordPage.assertCurrent();

        // The max auth age elapses and the user session disappears while the AIA form is open.
        // Only the user session is removed, the authentication session of the open form stays.
        timeOffSet.set(35);
        runOnServer.run(session -> {
            UserSessionModel userSession = session.sessions().getUserSession(session.getContext().getRealm(), sessionId);
            if (userSession != null) {
                session.sessions().removeUserSession(session.getContext().getRealm(), userSession);
            }
        });

        changePasswordPage.changePassword("New-password1!", "New-password1!");

        // The action must not be committed. The user is sent to authenticate instead.
        loginPage.assertCurrent();

        // Recover: the original password still works, which proves that the password was not changed
        timeOffSet.set(0);
        loginPage.fillLogin(testUser.getUsername(), PASSWORD);
        loginPage.submit();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void actionWithDefaultMaxAuthAgeWithinWindowIsUninterrupted() {
        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        timeOffSet.set(200);

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void updateProfileNotCommittedWhenMaxAuthAgeElapsed() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PROFILE.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PROFILE.name()).open();
        updateProfilePage.assertCurrent();
        timeOffSet.set(35);

        updateProfilePage.update("Changed", "Name");

        // The action must not be committed. The user is sent to authenticate instead.
        loginPage.assertCurrent();
        Assertions.assertEquals("Test", testUser.admin().toRepresentation().getFirstName());

        // Recover: re-authenticate and complete
        timeOffSet.set(0);
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        updateProfilePage.assertCurrent();
        updateProfilePage.update("Changed", "Name");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertEquals("Changed", testUser.admin().toRepresentation().getFirstName());
    }

    @Test
    public void updateProfileCommittedWithinMaxAuthAge() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PROFILE.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PROFILE.name()).open();
        updateProfilePage.assertCurrent();
        timeOffSet.set(10);

        updateProfilePage.update("Changed", "Name");

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertEquals("Changed", testUser.admin().toRepresentation().getFirstName());
    }

    @Test
    public void configureTotpNotCommittedWhenMaxAuthAgeElapsed() {
        setMaxAuthAge(UserModel.RequiredAction.CONFIGURE_TOTP.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.CONFIGURE_TOTP.name()).open();
        configTotpPage.assertCurrent();
        String staleSecret = configTotpPage.getTotpSecret();
        timeOffSet.set(35);

        configTotpPage.configure(new TimeBasedOTP().generateTOTP(staleSecret));

        // The action must not be committed. The user is sent to authenticate instead.
        loginPage.assertCurrent();
        Assertions.assertEquals(0, countOtpCredentials());

        // Recover: re-authenticate and configure with the secret of the new form
        timeOffSet.set(0);
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        configTotpPage.assertCurrent();
        configTotpPage.configure(new TimeBasedOTP().generateTOTP(configTotpPage.getTotpSecret()));
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertEquals(1, countOtpCredentials());
    }

    @Test
    public void configureTotpCommittedWithinMaxAuthAge() {
        setMaxAuthAge(UserModel.RequiredAction.CONFIGURE_TOTP.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.CONFIGURE_TOTP.name()).open();
        configTotpPage.assertCurrent();
        timeOffSet.set(10);

        configTotpPage.configure(new TimeBasedOTP().generateTOTP(configTotpPage.getTotpSecret()));

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertEquals(1, countOtpCredentials());
    }

    @Test
    public void passwordPolicyMaxAuthAgeIsEnforcedAtCommit() {
        setMaxAuthAgePasswordPolicy(30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        changePasswordPage.assertCurrent();
        timeOffSet.set(35);

        changePasswordPage.changePassword("New-password1!", "New-password1!");

        loginPage.assertCurrent();

        // Recover: re-authenticate and complete
        timeOffSet.set(0);
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void passwordPolicyMaxAuthAgeTakesPrecedenceOverActionConfigAtCommit() {
        setMaxAuthAgePasswordPolicy(600);
        // Would force re-authentication after 35s if it was used
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        changePasswordPage.assertCurrent();
        timeOffSet.set(35);

        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void updateEmailPostWithoutSessionCodeRequiresReAuthWhenMaxAuthAgeElapsed() {
        enableUpdateEmailAction();
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_EMAIL.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).open();
        assertUpdateEmailPageCurrent();
        timeOffSet.set(35);

        // Dropping the session_code makes the request look like a page refresh, which must not skip the max_auth_age check
        submitUpdateEmailWithoutSessionCode("new-email@localhost");

        // The user is sent to authenticate and the confirmation mail to the submitted address is not sent
        loginPage.assertCurrent();
        Assertions.assertEquals(0, mailServer.getReceivedMessages().length);
    }

    @Test
    public void updateEmailPostWithoutSessionCodeWithinMaxAuthAgeIsNotInterrupted() {
        enableUpdateEmailAction();
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_EMAIL.name(), 30);

        oauth.loginForm().doLogin(testUser.getUsername(), PASSWORD);
        pollLoginSuccess();

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).open();
        assertUpdateEmailPageCurrent();

        submitUpdateEmailWithoutSessionCode("new-email@localhost");

        // Within the allowed window the request is handled as before
        Assertions.assertTrue(mailServer.waitForIncomingEmail(5000, 1));
    }

    private void enableUpdateEmailAction() {
        RequiredActionProviderRepresentation action = realm.admin().flows().getRequiredActions().stream()
                .filter(a -> UserModel.RequiredAction.UPDATE_EMAIL.name().equals(a.getProviderId()))
                .findFirst()
                .orElseThrow();
        // The realm is shared by all tests of this class, so disable the action again afterwards
        realm.cleanup().add(r -> r.flows().getRequiredActions().stream()
                .filter(a -> UserModel.RequiredAction.UPDATE_EMAIL.name().equals(a.getProviderId()))
                .findFirst()
                .ifPresent(a -> {
                    a.setEnabled(false);
                    r.flows().updateRequiredAction(a.getAlias(), a);
                }));
        action.setEnabled(true);
        realm.admin().flows().updateRequiredAction(action.getAlias(), action);
    }

    private void assertUpdateEmailPageCurrent() {
        driver.waiting().until(d -> "login-update-email".equals(driver.page().getCurrentPageId()));
    }

    /**
     * Submits the update email form like the browser would, but with the session_code removed from the form action.
     */
    private void submitUpdateEmailWithoutSessionCode(String email) {
        ((JavascriptExecutor) driver.driver()).executeScript(
                "var form = document.getElementById('kc-update-email-form');"
                        + "form.action = form.action.replace(/([?&])session_code=[^&]*&?/, '$1');"
                        + "form.querySelector('input[name=email]').value = arguments[0];"
                        + "form.submit();", email);
    }

    /**
     * Returns the LOGIN event of the test user. Other events (e.g. late events of a previous test) are skipped, so the tests
     * do not depend on the order in which they run or on the timing of events being persisted.
     */
    private EventAssertion pollLoginSuccess() {
        EventRepresentation event;
        do {
            event = events.poll();
        } while (event != null && !(EventType.LOGIN.name().equals(event.getType()) && testUser.getId().equals(event.getUserId())));
        return EventAssertion.expectLoginSuccess(event);
    }

    private long countOtpCredentials() {
        return testUser.admin().credentials().stream()
                .filter(c -> OTPCredentialModel.TYPE.equals(c.getType()))
                .count();
    }

    /**
     * Points the AUTH_SESSION_ID cookie to a root authentication session with its own random ID, as it is the case for restarted
     * logins or action token roots. The existing SSO user session keeps its ID, so the two IDs differ.
     */
    private void useRootAuthSessionWithDifferentIdThanUserSession() {
        String realmName = realm.getName();
        // Returns "<AUTH_SESSION_ID cookie value>|<KC_AUTH_SESSION_HASH cookie value>"
        String cookieValues = runOnServer.fetchString(session -> {
            RealmModel serverRealm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(serverRealm);
            String rootAuthSessionId = session.authenticationSessions().createRootAuthenticationSession(serverRealm).getId();
            String signed = new AuthenticationSessionManager(session).signAndEncodeToBase64AuthSessionId(rootAuthSessionId);
            String cookie = session.getProvider(StickySessionEncoderProvider.class).encodeSessionId(signed, rootAuthSessionId);
            String hash = Base64.getEncoder().withoutPadding()
                    .encodeToString(HashUtils.hash(JavaAlgorithm.SHA384, rootAuthSessionId.getBytes(StandardCharsets.UTF_8)));
            return cookie + "|" + hash;
        });
        String[] values = cookieValues.split("\\|");

        // The cookies can only be set on an HTML page of the Keycloak origin. prompt=login renders the login form without ending the SSO session.
        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        loginPage.assertCurrent();
        replaceCookie(CookieType.AUTH_SESSION_ID, values[0], "/realms/" + realmName + "/");
        replaceCookie(CookieType.AUTH_SESSION_ID_HASH, values[1], "/realms/" + realmName + "/");
    }

    private void replaceCookie(CookieType type, String value, String defaultPath) {
        String path = defaultPath;
        Cookie existing = driver.cookies().get(type);
        if (existing != null) {
            path = existing.getPath();
            driver.driver().manage().deleteCookie(existing);
        }
        driver.cookies().add(new Cookie.Builder(type.getName(), value).path(path).build());
    }

    private void setMaxAuthAgePasswordPolicy(int maxAuthAge) {
        // The realm is shared by all tests of this class and the password policy of UPDATE_PASSWORD takes precedence over the action config.
        // Reset with an empty string: realm.updateWithCleanup restores a null policy, which the realm update ignores, so the policy would leak.
        realm.cleanup().add(r -> {
            RealmRepresentation rep = r.toRepresentation();
            rep.setPasswordPolicy("");
            r.update(rep);
        });
        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setPasswordPolicy(PasswordPolicy.MAX_AUTH_AGE_ID + "(" + maxAuthAge + ")");
        realm.admin().update(rep);
    }

    private void setMaxAuthAge(String providerId, int maxAuthAge) {
        Optional<RequiredActionProviderRepresentation> action = realm.admin().flows().getRequiredActions()
                .stream()
                .filter(a -> a.getProviderId().equals(providerId))
                .findFirst();
        action.ifPresent(a -> {
            Map<String, String> config = a.getConfig() == null ? new HashMap<>() : new HashMap<>(a.getConfig());
            config.put(Constants.MAX_AUTH_AGE_KEY, String.valueOf(maxAuthAge));
            a.setConfig(config);
            realm.admin().flows().updateRequiredAction(a.getAlias(), a);
        });
    }

    public static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.resetPasswordAllowed(true);
        }
    }

    public static class TestUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("strategy-test-user")
                    .email("strategy-test-user@localhost")
                    .name("Test", "User")
                    .password(PASSWORD)
                    .emailVerified(true);
        }
    }
}
