/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.forms;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.authenticators.resetcred.ResetCredentialEmail;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.federation.kerberos.AbstractKerberosTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.BrowserTabUtil;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.KerberosUtils;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.TestAppHelper;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.UserActionTokenBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ResetPasswordTest extends AbstractTestRealmKeycloakTest {

    private String userId;
    private String password;
    private UserRepresentation defaultUser;

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Drone
    @SecondBrowser
    protected WebDriver driver2;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        RealmBuilder.edit(testRealm)
                .client(org.keycloak.testsuite.util.ClientBuilder.create().clientId("client-user").serviceAccount());
    }

    @Before
    public void setup() {
        log.info("Adding login-test user");
        defaultUser = UserBuilder.create()
                .username("login-test")
                .email("login@test.com")
                .enabled(true)
                .build();

        password = generatePassword();
        userId = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), defaultUser, password);
        defaultUser.setId(userId);
        expectedMessagesCount = 0;
        getCleanup().addUserId(userId);
    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected InfoPage infoPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private int expectedMessagesCount;

    @Test
    public void resetPasswordLink() throws IOException, MessagingException {
        String username = "login-test";
        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";

        openResetPasswordUrlAndDoFlow(resetUri, "account", oauth.AUTH_SERVER_ROOT + "/realms/test/account/", false);

        AccountHelper.logout(testRealm(), username);
        WaitUtils.waitForPageToLoad();

        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);
        testAppHelper.login(username, "resetPassword");

        appPage.assertCurrent();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void resetPasswordLoggedUser() throws IOException {
        String username = "login-test";
        loginPage.open();
        loginPage.login(username, password);

        events.expectLogin().user(userId).detail(Details.USERNAME, username).assertEvent();

        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";

        openResetPasswordUrlAndDoFlow(resetUri, "account", oauth.AUTH_SERVER_ROOT + "/realms/test/account/", true);

        AccountHelper.logout(testRealm(), username);
        WaitUtils.waitForPageToLoad();

        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);
        testAppHelper.login(username, "resetPassword");

        appPage.assertCurrent();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    // Starts by opening "reset-password-url". Then go through the successful reset-password flow for the particular user. After user confirms new password, this method ends.
    private void openResetPasswordUrlAndDoFlow(String resetUri, String expectedClientId, String expectedRedirectUri, boolean userAuthenticated) throws IOException {
        String username = "login-test";
        driver.navigate().to(resetUri);

        if (!userAuthenticated) {
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword(username);
        }

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        AssertEvents.ExpectedEvent event = events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .user(userId)
                .client(expectedClientId)
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, "login@test.com")
                .session((String)null);
        if (expectedRedirectUri != null) {
            event.detail(Details.REDIRECT_URI,  expectedRedirectUri);
        } else {
            event.removeDetail(Details.REDIRECT_URI);
        }
        event.assertEvent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        if(userAuthenticated) {
            assertFalse("Logout other sessions was ticked", updatePasswordPage.isLogoutSessionsChecked());
        } else {
            updatePasswordPage.checkLogoutSessions();
        }

        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        event = events.expectRequiredAction(EventType.UPDATE_PASSWORD)
                .detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .client(expectedClientId)
                .user(userId).detail(Details.USERNAME, username);
        if (expectedRedirectUri != null) {
            event.detail(Details.REDIRECT_URI,  expectedRedirectUri);
        } else {
            event.removeDetail(Details.REDIRECT_URI);
        }
        event.assertEvent();

        event = events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .client(expectedClientId)
                .user(userId).detail(Details.USERNAME, username);
        if (expectedRedirectUri != null) {
            event.detail(Details.REDIRECT_URI,  expectedRedirectUri);
        } else {
            event.removeDetail(Details.REDIRECT_URI);
        }
        event.assertEvent();
    }

    @Test
    public void resetPasswordLinkTestAppWithoutRedirectUriParam() throws IOException {
        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials?client_id=test-app";

        openResetPasswordUrlAndDoFlow(resetUri, "test-app", null, false);

        // Link "Back to application" with the baseUrl of client "test-app"
        infoPage.assertCurrent();
        assertEquals("Your account has been updated.", infoPage.getInfo());

        infoPage.clickBackToApplicationLink();
        WaitUtils.waitForPageToLoad();
        MatcherAssert.assertThat(driver.getCurrentUrl(), endsWith("/app/auth"));
    }

    @Test
    public void resetPasswordLinkTestAppWithRedirectUriParam() throws IOException {
        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials?client_id=test-app&redirect_uri=" + oauth.getRedirectUri();

        openResetPasswordUrlAndDoFlow(resetUri, "test-app", oauth.getRedirectUri(), false);

        // Should be directly redirected to "application because of "redirect_uri" parameter
        appPage.assertCurrent();
    }

    @Test
    public void resetPasswordLinkErrorFlows() throws IOException {
        // Client not found
        driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials?client_id=not_found");
        errorPage.assertCurrent();
        Assert.assertEquals("Client not found.", errorPage.getError());

        // Redirect_uri without client
        driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials?redirect_uri=https://foo/bar/");
        errorPage.assertCurrent();
        Assert.assertEquals("Missing parameters: client_id", errorPage.getError());

        // Incorrect redirect_uri
        driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials?client_id=test-app&redirect_uri=https://foo/bar/");
        errorPage.assertCurrent();
        Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        // Client disabled
        ClientResource client = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation clientRep = client.toRepresentation();
        clientRep.setEnabled(false);
        client.update(clientRep);
        try {
            driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials?client_id=test-app");
            Assert.assertEquals("Client disabled.", errorPage.getError());
        } finally {
            clientRep.setEnabled(true);
            client.update(clientRep);
        }
    }


    @Test
    public void resetPassword() throws IOException, MessagingException {
        resetPassword("login-test");
    }

    @Test
    public void resetPasswordTwice() throws IOException, MessagingException {
        String changePasswordUrl = resetPassword("login-test");
        events.clear();

        assertSecondPasswordResetFails(changePasswordUrl, oauth.getClientId()); // KC_RESTART doesn't exist, it was deleted after first successful reset-password flow was finished
    }

    @Test
    public void resetPasswordForceLogin() throws IOException, MessagingException {
        // add the force login option in the reset-credential-email authenticator
        configureForceLogin(Boolean.TRUE.toString());

        resetPassword("login-test", "resetPassword", true);
    }

    @Test
    public void resetPasswordForceLoginFederatedUser() throws IOException, MessagingException {
        // create the example map storage user federation
        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName(UserStorageProvider.class.getName());
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));
        memProvider.getConfig().putSingle(UserStorageProviderModel.IMPORT_ENABLED, Boolean.toString(false));
        String componentId = ApiUtil.getCreatedId(testRealm().components().add(memProvider));
        getCleanup().addComponentId(componentId);

        // remove the test user and create it but federated
        testRealm().users().get(userId).remove();
        UserRepresentation user = new UserRepresentation();
        user.setUsername("login-test");
        user.setEmail("login@test.com");
        user.setFederationLink(componentId);
        this.userId = ApiUtil.getCreatedId(testRealm().users().create(user));
        Assert.assertFalse(StorageId.isLocalStorage(userId));

        // by default federated users are force to re-login
        resetPassword("login-test", "resetPassword", true);

        // check with false the session is maintained
        configureForceLogin(Boolean.FALSE.toString());
        resetPassword("login-test", "resetPassword", false);
    }

    @Test
    public void resetPasswordTwiceInNewBrowser() throws IOException, MessagingException {
        String changePasswordUrl = resetPassword("login-test");
        events.clear();

        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
        driver.navigate().to(resetUri); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
        driver.manage().deleteAllCookies();

        assertSecondPasswordResetFails(changePasswordUrl, oauth.getClientId());
    }

    public void assertSecondPasswordResetFails(String changePasswordUrl, String clientId) {
        driver.navigate().to(changePasswordUrl.trim());

        errorPage.assertCurrent();
        assertEquals("Action expired. Please continue with login now.", errorPage.getError());

        events.expect(EventType.RESET_PASSWORD)
          .client(clientId)
          .session((String) null)
          .user(userId)
          .error(Errors.EXPIRED_CODE)
          .assertEvent();
    }

    @Test
    public void resetPasswordWithSpacesInUsername() throws IOException, MessagingException {
        resetPassword(" login-test ");
    }

    @Test
    public void resetPasswordCancelChangeUser() throws IOException, MessagingException {
        initiateResetPasswordFromResetPasswordPage("test-user@localhost");

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).detail(Details.USERNAME, "test-user@localhost")
                .session((String) null)
                .detail(Details.EMAIL, "test-user@localhost").assertEvent();

        loginPage.login("login@test.com", password);

        EventRepresentation loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "login@test.com").assertEvent();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        assertEquals(200, tokenResponse.getStatusCode());
        assertEquals(userId, oauth.verifyToken(tokenResponse.getAccessToken()).getSubject());

        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId()).user(userId).assertEvent();
    }

    @Test
    public void resetPasswordByEmail() throws IOException, MessagingException {
        resetPassword("login@test.com");
    }

    @Test
    public void resetPasswordBackButton() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("login@test.com", "wrongpassword");
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        driver.navigate().back();
        loginPage.assertCurrent();
    }

    private void configureForceLogin(String value) {
        AuthenticationExecutionInfoRepresentation sendEmailExec = testRealm()
                .flows()
                .getExecutions(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW)
                .stream()
                .filter(e -> ResetCredentialEmail.PROVIDER_ID.equals(e.getProviderId()))
                .findAny().orElseThrow();
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias("reset-password-config");
        config.getConfig().put(ResetCredentialEmail.FORCE_LOGIN, value);
        String configId = ApiUtil.getCreatedId(testRealm().flows().newExecutionConfig(sendEmailExec.getId(), config));
        getCleanup().addAuthenticationConfigId(configId);
    }

    private String resetPassword(String username) throws IOException, MessagingException {
        return resetPassword(username, "resetPassword", false);
    }

    private String resetPassword(String username, String password) throws IOException, MessagingException {
        return resetPassword(username, password, false);
    }

    private String resetPassword(String username, String password, boolean relogin) throws IOException, MessagingException {
        initiateResetPasswordFromResetPasswordPage(username);

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .user(userId)
                .detail(Details.USERNAME, username.trim())
                .detail(Details.EMAIL, "login@test.com")
                .session((String)null)
                .assertEvent();

        assertEquals(expectedMessagesCount, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        assertEquals("You need to change your password.", updatePasswordPage.getFeedbackMessage());

        updatePasswordPage.changePassword(password, password);

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).detail(Details.USERNAME, username.trim()).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).detail(Details.USERNAME, username.trim()).assertEvent();

        if (relogin) {
            // relogin is forced therefore the info page should be displayed
            Assert.assertEquals("Your account has been updated.", infoPage.getInfo());
            String backToAppLink = infoPage.getBackToApplicationLink();
            ClientRepresentation client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app").toRepresentation();
            Assert.assertEquals(backToAppLink, client.getBaseUrl());
            loginPage.open();
            loginPage.assertCurrent();
        } else {
            // continue to app because it is the same browser and auth session exists
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            EventRepresentation loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, username.trim()).assertEvent();
            String sessionId = loginEvent.getSessionId();

            AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
            oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).withRedirect().open();

            events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();

            loginPage.open();

            loginPage.login("login-test", password);

            loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
            sessionId = loginEvent.getSessionId();

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
            oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).withRedirect().open();

            events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();
        }

        return changePasswordUrl;
    }

    private void resetPasswordInvalidPassword(String username, String password, String error) throws IOException, MessagingException {

        initiateResetPasswordFromResetPasswordPage(username);

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).session((String) null)
                .detail(Details.USERNAME, username).detail(Details.EMAIL, "login@test.com").assertEvent();

        assertEquals(expectedMessagesCount, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());


        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword(password, password);

        updatePasswordPage.assertCurrent();
        assertEquals(error, updatePasswordPage.getError());
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL_ERROR).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).error(Errors.PASSWORD_REJECTED).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
        events.expectRequiredAction(EventType.UPDATE_PASSWORD_ERROR).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).error(Errors.PASSWORD_REJECTED).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
    }

    private void initiateResetPasswordFromResetPasswordPage(String username) {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
        expectedMessagesCount++;
    }

    @Test
    public void resetPasswordWrongEmail() throws IOException, MessagingException, InterruptedException {
        initiateResetPasswordFromResetPasswordPage("invalid");

        assertEquals(0, greenMail.getReceivedMessages().length);

        events.expectRequiredAction(EventType.RESET_PASSWORD).user((String) null).session((String) null).detail(Details.USERNAME, "invalid").removeDetail(Details.EMAIL).removeDetail(Details.CODE_ID).error("user_not_found").assertEvent();
    }

    @Test
    public void resetPasswordMissingUsername() throws IOException, MessagingException, InterruptedException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("");

        resetPasswordPage.assertCurrent();

        assertEquals("Please specify username.", resetPasswordPage.getUsernameError());

        assertEquals(0, greenMail.getReceivedMessages().length);

        events.expectRequiredAction(EventType.RESET_PASSWORD).user((String) null).session((String) null).clearDetails().error("username_missing").assertEvent();

    }

    @Test
    public void resetPasswordExpiredCode() throws IOException, MessagingException, InterruptedException {
        initiateResetPasswordFromResetPasswordPage("login-test");

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .session((String)null)
                .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        try {
            setTimeOffset(360);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void resetPasswordExpiredCodeShort() throws IOException, MessagingException, InterruptedException {
        final AtomicInteger originalValue = new AtomicInteger();

        RealmRepresentation realmRep = testRealm().toRepresentation();
        originalValue.set(realmRep.getActionTokenGeneratedByUserLifespan());
        realmRep.setActionTokenGeneratedByUserLifespan(60);
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setActionTokenGeneratedByUserLifespan(originalValue.get());
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeShortPerActionLifespan() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).build());
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeShortPerActionMultipleTimeouts() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        //Make sure that one attribute settings won't affect the other
        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).verifyEmailLifespan(300).build());

        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    // KEYCLOAK-4016
    @Test
    public void resetPasswordExpiredCodeAndAuthSession() throws IOException, MessagingException, InterruptedException {
        final AtomicInteger originalValue = new AtomicInteger();

        RealmRepresentation realmRep = testRealm().toRepresentation();
        originalValue.set(realmRep.getActionTokenGeneratedByUserLifespan());
        realmRep.setActionTokenGeneratedByUserLifespan(60);
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message).replace("&amp;", "&");

            log.debug("Removing cookies."); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
            driver.manage().deleteAllCookies();

            setTimeOffset(70);

            log.debug("Going to reset password URI.");
            driver.navigate().to(changePasswordUrl.trim());

            errorPage.assertCurrent();
            Assert.assertEquals("Action expired.", errorPage.getError());
            String backToAppLink = errorPage.getBackToApplicationLink();
            Assert.assertTrue(backToAppLink.endsWith("/app/auth"));

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setActionTokenGeneratedByUserLifespan(originalValue.get());
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeAndAuthSessionPerActionLifespan() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).build());
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message).replace("&amp;", "&");

            log.debug("Removing cookies."); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
            driver.manage().deleteAllCookies();

            setTimeOffset(70);

            log.debug("Going to reset password URI.");
            URLUtils.navigateToUri(changePasswordUrl.trim());

            errorPage.assertCurrent();
            Assert.assertEquals("Action expired.", errorPage.getError());
            String backToAppLink = errorPage.getBackToApplicationLink();
            Assert.assertTrue(backToAppLink.endsWith("/app/auth"));

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeAndAuthSessionPerActionMultipleTimeouts() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        //Make sure that one attribute settings won't affect the other
        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).verifyEmailLifespan(300).build());
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message).replace("&amp;", "&");

            log.debug("Removing cookies."); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
            driver.manage().deleteAllCookies();

            setTimeOffset(70);

            log.debug("Going to reset password URI.");
            driver.navigate().to(changePasswordUrl.trim());

            errorPage.assertCurrent();
            Assert.assertEquals("Action expired.", errorPage.getError());
            String backToAppLink = errorPage.getBackToApplicationLink();
            Assert.assertTrue(backToAppLink.endsWith("/app/auth"));

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    // KEYCLOAK-5061
    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlow() throws IOException, MessagingException, InterruptedException {
        final AtomicInteger originalValue = new AtomicInteger();

        RealmRepresentation realmRep = testRealm().toRepresentation();
        originalValue.set(realmRep.getActionTokenGeneratedByUserLifespan());
        realmRep.setActionTokenGeneratedByUserLifespan(60);
        testRealm().update(realmRep);

        try {
            // Redirect directly to KC "forgot password" endpoint instead of "authenticate" endpoint
            String loginUrl = oauth.loginForm().build();
            String forgotPasswordUrl = loginUrl.replace("/auth?", "/forgot-credentials?"); // Workaround, but works

            driver.navigate().to(forgotPasswordUrl);
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            expectedMessagesCount++;

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            resetPasswordPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setActionTokenGeneratedByUserLifespan(originalValue.get());
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlowPerActionLifespan() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).build());
        testRealm().update(realmRep);

        try {
            // Redirect directly to KC "forgot password" endpoint instead of "authenticate" endpoint
            String loginUrl = oauth.loginForm().build();
            String forgotPasswordUrl = loginUrl.replace("/auth?", "/forgot-credentials?"); // Workaround, but works

            driver.navigate().to(forgotPasswordUrl);
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            expectedMessagesCount++;

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            resetPasswordPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlowPerActionMultipleTimeouts() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        //Make sure that one attribute settings won't affect the other
        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).verifyEmailLifespan(300).build());
        testRealm().update(realmRep);

        try {
            // Redirect directly to KC "forgot password" endpoint instead of "authenticate" endpoint
            String loginUrl = oauth.loginForm().build();
            String forgotPasswordUrl = loginUrl.replace("/auth?", "/forgot-credentials?"); // Workaround, but works

            driver.navigate().to(forgotPasswordUrl);
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            expectedMessagesCount++;

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            resetPasswordPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordDisabledUser() throws IOException, MessagingException, InterruptedException {
        UserRepresentation user = findUser("login-test");
        try {
            user.setEnabled(false);
            updateUser(user);

            initiateResetPasswordFromResetPasswordPage("login-test");

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.RESET_PASSWORD).session((String) null).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error("user_disabled").assertEvent();
        } finally {
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void resetPasswordNoEmail() throws IOException, MessagingException, InterruptedException {
        final String email;

        UserRepresentation user = findUser("login-test");
        email = user.getEmail();

        try {
            user.setEmail("");
            updateUser(user);

            initiateResetPasswordFromResetPasswordPage("login-test");

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.RESET_PASSWORD_ERROR).session((String) null).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error("invalid_email").assertEvent();
        } finally {
            user.setEmail(email);
            updateUser(user);
        }
    }

    @Test
    public void resetPasswordWrongSmtp() throws IOException, MessagingException, InterruptedException {
        final String[] host = new String[1];

        Map<String, String> smtpConfig = new HashMap<>();
        smtpConfig.putAll(testRealm().toRepresentation().getSmtpServer());
        host[0] =  smtpConfig.get("host");
        smtpConfig.put("host", "invalid_host");
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> oldSmtp = realmRep.getSmtpServer();

        try {
            realmRep.setSmtpServer(smtpConfig);
            testRealm().update(realmRep);

            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();

            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD_ERROR).user(userId)
                    .session((String)null)
                    .detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error(Errors.EMAIL_SEND_FAILED).assertEvent();
        } finally {
            // Revert SMTP back
            realmRep.setSmtpServer(oldSmtp);
            testRealm().update(realmRep);
        }
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setPasswordPolicy(policy);
        testRealm().update(realmRep);
    }

    @Test
    public void resetPasswordWithLengthPasswordPolicy() throws IOException, MessagingException {
        setPasswordPolicy("length");

        initiateResetPasswordFromResetPasswordPage("login-test");

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).session((String)null).user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("invalid", "invalid");

        assertEquals("Invalid password: minimum length 8.", resetPasswordPage.getErrorMessage());

        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL_ERROR).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).error(Errors.PASSWORD_REJECTED).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
        events.expectRequiredAction(EventType.UPDATE_PASSWORD_ERROR).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).error(Errors.PASSWORD_REJECTED).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();

        updatePasswordPage.changePassword("resetPasswordWithPasswordPolicy", "resetPasswordWithPasswordPolicy");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());


        EventRepresentation loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
        String sessionId = loginEvent.getSessionId();

        AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).withRedirect().open();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();

        loginPage.open();

        loginPage.login("login-test", "resetPasswordWithPasswordPolicy");

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void resetPasswordBeforeUserIsDisabled() throws IOException, MessagingException {
        initiateResetPasswordFromResetPasswordPage("login-test");

        assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).session((String)null).user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

        UserRepresentation user = findUser("login-test");
        user.setEnabled(false);
        updateUser(user);

        driver.navigate().to(changePasswordUrl.trim());

        errorPage.assertCurrent();
        assertEquals("Account is disabled, contact your administrator.", errorPage.getError());
    }

    @Test
    public void resetPasswordWithPasswordHistoryPolicy() throws IOException, MessagingException {
        //Block passwords that are equal to previous passwords. Default value is 3.
        setPasswordPolicy("passwordHistory");

        try {
            setTimeOffset(2000000);
            resetPassword("login-test", "password1");

            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");

            setTimeOffset(4000000);
            resetPassword("login-test", "password2");

            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password2", "Invalid password: must not be equal to any of last 3 passwords.");

            setTimeOffset(6000000);
            resetPassword("login-test", "password3");

            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password2", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password3", "Invalid password: must not be equal to any of last 3 passwords.");

            setTimeOffset(8000000);
            resetPassword("login-test", password);
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void resetPasswordLinkOpenedInNewBrowser() throws IOException, MessagingException {
        resetPasswordLinkOpenedInNewBrowser(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
    }


    private void resetPasswordLinkOpenedInNewBrowser(String expectedSystemClientId) throws IOException, MessagingException {
        String username = "login-test";
        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
        driver.navigate().to(resetUri);

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        log.info("Should be at login page again.");
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .user(userId)
                .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                .client(expectedSystemClientId)
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, "login@test.com")
                .session((String)null)
                .assertEvent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        log.debug("Going to reset password URI.");
        driver.navigate().to(resetUri); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
        log.debug("Removing cookies.");
        driver.manage().deleteAllCookies();
        log.debug("Going to URI from e-mail.");
        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        infoPage.assertCurrent();
        assertEquals("Your account has been updated.", infoPage.getInfo());

        // Link "back to application" not present due the fact we use system client
        assertThat(driver.getPageSource(), Matchers.not(Matchers.containsString("Back to Application")));
    }


    // KEYCLOAK-5982
    @Test
    public void resetPasswordLinkOpenedInNewBrowserAndAccountClientRenamed() throws IOException, MessagingException {
        // Temporarily rename client "account" . Revert it back after the test
        try (Closeable accountClientUpdater = ClientAttributeUpdater.forClient(adminClient, "test", Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .setClientId("account-changed")
                .update()) {

            // Assert resetPassword link opened in new browser works even if client "account" not available
            resetPasswordLinkOpenedInNewBrowser(SystemClientUtil.SYSTEM_CLIENT_ID);

        }
    }

    @Test
    public void resetPasswordLinkNewBrowserSessionPreserveClient() throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("login-test");

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver2.navigate().to(changePasswordUrl.trim());

        changePasswordOnUpdatePage(driver2);

        assertThat(driver2.getCurrentUrl(), Matchers.containsString("client_id=test-app"));

        assertThat(driver2.getPageSource(), Matchers.containsString("Your account has been updated."));
        assertThat(driver2.getPageSource(), Matchers.containsString("Back to Application"));
    }


    // KEYCLOAK-15239
    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // TODO: https://github.com/keycloak/keycloak/issues/20526
    public void resetPasswordWithSpnegoEnabled() throws IOException, MessagingException {
        KerberosUtils.assumeKerberosSupportExpected();

        // Just switch SPNEGO authenticator requirement to alternative. No real usage of SPNEGO needed for this test
        AuthenticationExecutionModel.Requirement origRequirement = AbstractKerberosTest.updateKerberosAuthExecutionRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE, testRealm());

        try {
            resetPassword("login-test");
        } finally {
            // Revert
            AbstractKerberosTest.updateKerberosAuthExecutionRequirement(origRequirement, testRealm());
        }
    }


    @Test
    public void failResetPasswordServiceAccount() {
        String username = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "client-user";
        UserRepresentation serviceAccount = testRealm().users()
                .search(username).get(0);

        serviceAccount.toString();

        UserResource serviceAccount1 = testRealm().users().get(serviceAccount.getId());

        serviceAccount = serviceAccount1.toRepresentation();
        serviceAccount.setEmail("client-user@test.com");
        serviceAccount1.update(serviceAccount);

        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
        driver.navigate().to(resetUri);

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("Invalid username or password.", errorPage.getError());
    }

    @Test
    public void resetPasswordLinkNewTabAndProperRedirectClient() throws IOException {
        final String REDIRECT_URI = getAuthServerRoot() + "realms/master/app/auth";
        final String CLIENT_ID = "test-app";

        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver);
             ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(getAdminClient(), TEST_REALM_NAME, CLIENT_ID)
                     .filterRedirectUris(uri -> uri.contains(REDIRECT_URI))
                     .update()) {

            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));

            loginPage.open();
            resetPasswordInNewTab(defaultUser, CLIENT_ID, REDIRECT_URI);
            assertThat(driver.getCurrentUrl(), Matchers.containsString(REDIRECT_URI));

            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            loginPage.open();
            resetPasswordInNewTab(defaultUser, CLIENT_ID, REDIRECT_URI);
            assertThat(driver.getCurrentUrl(), Matchers.containsString(REDIRECT_URI));
        }
    }

    @Test
    public void resetPasswordInfoMessageWithDuplicateEmailsAllowed() throws IOException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Boolean originalLoginWithEmailAllowed = realmRep.isLoginWithEmailAllowed();
        Boolean originalDuplicateEmailsAllowed = realmRep.isDuplicateEmailsAllowed();

        try {
            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            assertEquals("Enter your username or email address and we will send you instructions on how to create a new password.", resetPasswordPage.getInfoMessage());

            realmRep.setLoginWithEmailAllowed(false);
            realmRep.setDuplicateEmailsAllowed(true);
            testRealm().update(realmRep);

            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            assertEquals("Enter your username and we will send you instructions on how to create a new password.", resetPasswordPage.getInfoMessage());
        } finally {
            realmRep.setLoginWithEmailAllowed(originalLoginWithEmailAllowed);
            realmRep.setDuplicateEmailsAllowed(originalDuplicateEmailsAllowed);
            testRealm().update(realmRep);
        }

    }

    // KEYCLOAK-15170
    @Test
    public void changeEmailAddressAfterSendingEmail() throws IOException {
        initiateResetPasswordFromResetPasswordPage(defaultUser.getUsername());

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        UserResource user = testRealm().users().get(defaultUser.getId());
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEmail("vmuzikar@redhat.com");
        user.update(userRep);

        driver.navigate().to(changePasswordUrl.trim());
        errorPage.assertCurrent();
        assertEquals("Invalid email address.", errorPage.getError());
    }

    @Test
    public void resetPasswordWithLoginHint() throws IOException {
        // Redirect directly to KC "forgot password" endpoint instead of "authenticate" endpoint
        String loginUrl = oauth.loginForm().build();
        String forgotPasswordUrl = loginUrl.replace("/auth?", "/forgot-credentials?"); // Workaround, but works

        driver.navigate().to(forgotPasswordUrl + "&login_hint=test");
        resetPasswordPage.assertCurrent();

        assertEquals("test", resetPasswordPage.getUsername());
    }

    private void changePasswordOnUpdatePage(WebDriver driver) {
        assertThat(driver.getPageSource(), Matchers.containsString("You need to change your password."));

        final WebElement newPassword = driver.findElement(By.id("password-new"));
        newPassword.sendKeys("resetPassword");
        final WebElement confirmPassword = driver.findElement(By.id("password-confirm"));
        confirmPassword.sendKeys("resetPassword");
        final WebElement submit = driver.findElement(By.cssSelector("button[type=\"submit\"]"));

        UIUtils.clickLink(submit);
    }

    private void resetPasswordInNewTab(UserRepresentation user, String clientId, String redirectUri) throws IOException {
        try (BrowserTabUtil browserTabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            events.clear();

            final int emailCount = greenMail.getReceivedMessages().length;

            // In tab1 start "Forget password" flow and make sure the email is sent
            loginPage.assertCurrent();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword(user.getUsername());
            WaitUtils.waitForPageToLoad();

            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

            String tab1Url = driver.getCurrentUrl();

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .user(user.getId())
                    .client(clientId)
                    .detail(Details.REDIRECT_URI, redirectUri)
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(Details.EMAIL, user.getEmail())
                    .session((String) null)
                    .assertEvent();

            assertEquals(emailCount + 1, greenMail.getReceivedMessages().length);

            final MimeMessage message = greenMail.getReceivedMessages()[emailCount];
            final String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            // Open link from email in the new tab
            browserTabUtil.newTab(changePasswordUrl.trim());

            // Change password in tab2
            changePasswordOnUpdatePage(driver);

            events.expectRequiredAction(EventType.UPDATE_PASSWORD)
                    .detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                    .detail(Details.REDIRECT_URI, redirectUri)
                    .client(clientId)
                    .user(user.getId()).detail(Details.USERNAME, user.getUsername()).assertEvent();
            events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                    .detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                    .detail(Details.REDIRECT_URI, redirectUri)
                    .client(clientId)
                    .user(user.getId()).detail(Details.USERNAME, user.getUsername()).assertEvent();

            // User should be authenticated in current tab (tab2)
            WaitUtils.waitUntilElement(appPage.getAccountLink()).is().clickable();
            appPage.assertCurrent();
            assertThat(driver.getCurrentUrl(), Matchers.containsString(redirectUri));

            // Close tab2
            assertThat(browserTabUtil.getCountOfTabs(), Matchers.equalTo(2));
            browserTabUtil.closeTab(1);
            assertThat(browserTabUtil.getCountOfTabs(), Matchers.equalTo(1));

            if (driver instanceof HtmlUnitDriver) {
                // With HtmlUnit, authChecker javascript doesn't work. Hence need to manually trigger "reset flow" endpoint
                KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(tab1Url);
                String resetFlowPath = builder
                        .replacePath(builder.getPath().substring(0, builder.getPath().lastIndexOf('/') + 1) + LoginActionsService.RESTART_PATH)
                        .queryParam(Constants.SKIP_LOGOUT, "true")
                        .build().toString();
                driver.navigate().to(resetFlowPath);
            }

            // User should be automatically authenticated in tab1 as well (due authChecker.js on real browsers like FF or Chrome)
            WaitUtils.waitUntilElement(appPage.getAccountLink()).is().clickable();
            appPage.assertCurrent();
        }
    }
}
