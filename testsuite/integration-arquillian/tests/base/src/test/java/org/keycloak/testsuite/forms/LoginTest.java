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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;

import static org.keycloak.common.Profile.Feature.DYNAMIC_SCOPES;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.oauth.OAuthClient.SERVER_ROOT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginTest extends AbstractChangeImportedUserPasswordsTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        UserRepresentation user = UserBuilder.create()
                                             .username("login-test")
                                             .email("login@test.com")
                                             .enabled(true)
                                             .password(generatePassword("login-test"))
                                             .build();

        UserRepresentation user2 = UserBuilder.create()
                                              .username("login-test2")
                                              .email("login2@test.com")
                                              .enabled(true)
                                              .password(generatePassword("login2-test"))
                                              .build();

        UserRepresentation admin = UserBuilder.create()
                .username("admin")
                .password(generatePassword("admin"))
                .enabled(true)
                .build();
        HashMap<String, List<String>> clientRoles = new HashMap<>();
        clientRoles.put("realm-management", Arrays.asList("realm-admin"));
        admin.setClientRoles(clientRoles);

        RealmBuilder.edit(testRealm)
                    .user(user)
                    .user(user2)
                    .user(admin);
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected LoginConfigTotpPage configTotpPage;

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    private static String userId;

    private static String user2Id;

    @Override
    public void importTestRealms() {
        super.importTestRealms();
        userId = testRealm().users().search("login-test", Boolean.TRUE).get(0).getId();
        user2Id = testRealm().users().search("login-test2", Boolean.TRUE).get(0).getId();
    }

    @Test
    public void testBrowserSecurityHeaders() {
        Client client = AdminClientUtil.createResteasyClient();
        Response response = client.target(oauth.loginForm().build()).request().get();
        assertThat(response.getStatus(), is(equalTo(200)));
        for (BrowserSecurityHeaders header : BrowserSecurityHeaders.values()) {
            String headerValue = response.getHeaderString(header.getHeaderName());
            String expectedValue = header.getDefaultValue();
            if (expectedValue.isEmpty()) {
                assertNull(headerValue);
            } else {
                Assert.assertNotNull(headerValue);
                assertThat(headerValue, is(equalTo(expectedValue)));
            }
        }
        response.close();
        client.close();
    }

    @Test
    public void testContentSecurityPolicyReportOnlyBrowserSecurityHeader() {
        final String expectedCspReportOnlyValue = "default-src 'none'";
        final String cspReportOnlyAttr = "contentSecurityPolicyReportOnly";
        final String cspReportOnlyHeader = "Content-Security-Policy-Report-Only";

        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        final String defaultContentSecurityPolicyReportOnly = realmRep.getBrowserSecurityHeaders().get(cspReportOnlyAttr);
        realmRep.getBrowserSecurityHeaders().put(cspReportOnlyAttr, expectedCspReportOnlyValue);
        adminClient.realm("test").update(realmRep);

        try {
            Client client = AdminClientUtil.createResteasyClient();
            Response response = client.target(oauth.loginForm().build()).request().get();
            String headerValue = response.getHeaderString(cspReportOnlyHeader);
            assertThat(headerValue, is(equalTo(expectedCspReportOnlyValue)));
            response.close();
            client.close();
        } finally {
            realmRep.getBrowserSecurityHeaders().put(cspReportOnlyAttr, defaultContentSecurityPolicyReportOnly);
            adminClient.realm("test").update(realmRep);
        }
    }

    //KEYCLOAK-5556
    @Test
    public void testPOSTAuthenticationRequest() {
        Client client = AdminClientUtil.createResteasyClient();

        Form form = new Form()
                .param(OAuth2Constants.SCOPE, "openid")
                .param(OAuth2Constants.CLIENT_ID, oauth.getClientId())
                .param(OAuth2Constants.RESPONSE_TYPE, "code")
                .param(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri())
                .param(OAuth2Constants.STATE, "123456");

        //POST request to http://localhost:8180/auth/realms/test/protocol/openid-connect/auth;
        Response response = client.target(oauth.getEndpoints().getAuthorization()).request().post(Entity.form(form));

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));

        response.close();
        client.close();
    }

    @Test
    public void loginWithLongRedirectUri() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(adminClient.realm("test"))
                .updateWith(r -> r.setEventsEnabled(true)).update()) {
            String randomLongString = RandomStringUtils.random(2500, true, true);
            String longRedirectUri = oauth.getRedirectUri() + "?longQueryParameterValue=" + randomLongString;
            oauth.loginForm().param(OAuth2Constants.REDIRECT_URI, longRedirectUri).open();

            loginPage.assertCurrent();
            loginPage.login("login-test", getPassword("login-test"));

            events.expectLogin().user(userId).detail(OAuth2Constants.REDIRECT_URI, longRedirectUri).assertEvent();
        }
    }

    @Test
    public void loginChangeUserAfterInvalidPassword() {
        loginPage.open();
        loginPage.login("login-test2", "invalid");

        loginPage.assertCurrent();

        Assert.assertEquals("login-test2", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertNull(loginPage.getPasswordInputError());

        events.expectLogin().user(user2Id).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test2")
                .removeDetail(Details.CONSENT)
                .assertEvent();

        loginPage.login("login-test", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginInvalidPassword() {
        loginPage.open();
        loginPage.login("login-test", "invalid");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("login-test", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertNull(loginPage.getPasswordInputError());

        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginMissingPassword() {
        loginPage.open();
        loginPage.missingPassword("login-test");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("login-test", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertNull(loginPage.getPasswordInputError());

        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    private void setUserEnabled(String id, boolean enabled) {
        UserRepresentation rep = adminClient.realm("test").users().get(id).toRepresentation();
        rep.setEnabled(enabled);
        adminClient.realm("test").users().get(id).update(rep);
    }

    @Test
    public void loginInvalidPasswordDisabledUser() {
        setUserEnabled(userId, false);

        try {
            loginPage.open();
            loginPage.login("login-test", "invalid");

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("login-test", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

            events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                    .detail(Details.USERNAME, "login-test")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            setUserEnabled(userId, true);
        }
    }

    @Test
    public void loginDisabledUser() {
        setUserEnabled(userId, false);

        try {
            loginPage.open();
            loginPage.login("login-test", getPassword("login-test"));

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("login-test", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assert.assertEquals("Account is disabled, contact your administrator.", loginPage.getError());

            events.expectLogin().user(userId).session((String) null).error("user_disabled")
                    .detail(Details.USERNAME, "login-test")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            setUserEnabled(userId, true);
        }
    }

    @Test
    public void loginDifferentUserAfterDisabledUserThrownOut() {
        String userId = AccountHelper.getUserRepresentation(adminClient.realm("test"), "test-user@localhost").getId();

        try {
            loginPage.open();
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            appPage.assertCurrent();
            appPage.openAccount();

            setUserEnabled(userId, false);

            loginPage.open();
            loginPage.assertCurrent();

            // try to log in as different user
            loginPage.login("keycloak-user@localhost", getPassword("keycloak-user@localhost"));

            appPage.assertCurrent();
        } finally {
            setUserEnabled(userId, true);
        }
    }

    @Test
    public void loginInvalidUsername() {
        loginPage.open();
        loginPage.login("invalid", "invalid");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("invalid", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        events.expectLogin().user((String) null).session((String) null).error("user_not_found")
                .detail(Details.USERNAME, "invalid")
                .removeDetail(Details.CONSENT)
                .assertEvent();

        loginPage.login("login-test", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginMissingUsername() {
        loginPage.open();
        loginPage.missingUsername();

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        events.expectLogin().user((String) null).session((String) null).error("user_not_found")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    // KEYCLOAK-2557
    public void loginUserWithEmailAsUsername() {
        loginPage.open();
        loginPage.login("login@test.com", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login@test.com").assertEvent();
    }

    @Test
    public void loginSuccess() {
        loginPage.open();
        loginPage.login("login-test", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginSuccessRealmSigningAlgorithms() throws JWSInputException {
        ContainerAssume.assumeAuthServerSSL();

        loginPage.open();
        loginPage.login("login-test", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        driver.navigate().to(getAuthServerContextRoot() + "/auth/realms/test/");
        String keycloakIdentity = driver.manage().getCookieNamed("KEYCLOAK_IDENTITY").getValue();

        // Check identity cookie is signed with HS256
        String algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
        assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, algorithm);

        try {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.ES256);

            oauth.openLoginForm();
            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            driver.navigate().to(getAuthServerContextRoot() + "/auth/realms/test/");
            keycloakIdentity = driver.manage().getCookieNamed("KEYCLOAK_IDENTITY").getValue();

            // Check identity cookie is still signed with HS256
            algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
            assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, algorithm);

            // Check identity cookie still works
            oauth.openLoginForm();
            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
        }
    }

    @Test
    public void loginWithWhitespaceSuccess() {
        loginPage.open();
        loginPage.login(" login-test \t ", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginWithEmailWhitespaceSuccess() {
        loginPage.open();
        loginPage.login("    login@test.com    ", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).assertEvent();
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        realmRep.setPasswordPolicy(policy);
        adminClient.realm("test").update(realmRep);
    }

    @Test
    public void loginWithForcePasswordChangePolicy() {
        setPasswordPolicy("forceExpiredPasswordChange(1)");

        try {
            // Setting offset to more than one day to force password update
            // elapsedTime > timeToExpire
            setTimeOffset(86405);

            loginPage.open();

            loginPage.login("login-test", getPassword("login-test"));

            updatePasswordPage.assertCurrent();

            final String newPwd = generatePassword("login-test");
            updatePasswordPage.changePassword(newPwd, newPwd);

            setTimeOffset(0);

            events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).detail(Details.USERNAME, "login-test").assertEvent();
            events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).detail(Details.USERNAME, "login-test").assertEvent();

            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();
            assertEquals("bad expectation, on page: " + currentUrl, RequestType.AUTH_RESPONSE, appPage.getRequestType());

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        } finally {
            setPasswordPolicy(null);
        }
    }

    @Test
    public void loginWithoutForcePasswordChangePolicy() {
        setPasswordPolicy("forceExpiredPasswordChange(1)");

        try {
            // Setting offset to less than one day to avoid forced password update
            // elapsedTime < timeToExpire
            setTimeOffset(86205);

            loginPage.open();

            loginPage.login("login-test", getPassword("login-test"));

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());

            setTimeOffset(0);

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
        } finally {
            setPasswordPolicy(null);
        }
    }

    @Test
    public void loginNoTimeoutWithLongWait() {
        loginPage.open();

        setTimeOffset(1700);

        loginPage.login("login-test", getPassword("login-test"));

        setTimeOffset(0);

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
    }

    @Test
    public void loginLoginHint() {
        oauth.loginForm().param("login_hint", "login-test").open();

        Assert.assertEquals("login-test", loginPage.getUsername());
        loginPage.login(getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginWithEmailSuccess() {
        loginPage.open();
        loginPage.login("login@test.com", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).assertEvent();
    }

    private void setRememberMe(boolean enabled) {
        this.setRememberMe(enabled, null, null);
    }

    private void setRememberMe(boolean enabled, Integer idleTimeout, Integer maxLifespan) {
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        rep.setRememberMe(enabled);
        rep.setSsoSessionIdleTimeoutRememberMe(idleTimeout);
        rep.setSsoSessionMaxLifespanRememberMe(maxLifespan);
        adminClient.realm("test").update(rep);
    }

    @Test
    public void loginWithRememberMe() {
        setRememberMe(true);

        try {
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", getPassword("login-test"));

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            Assert.assertEquals("login-test", loginPage.getUsername());

            loginPage.setRememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }

    @Test
    public void loginWithRememberMeNotSet() {
        loginPage.open();
        assertFalse(loginPage.isRememberMeCheckboxPresent());
        // fake create the rememberme checkbox
        ((JavascriptExecutor) driver).executeScript(
          "var checkbox = document.createElement('input');" +
          "checkbox.type = 'checkbox';" +
          "checkbox.id = 'rememberMe';" +
          "checkbox.name = 'rememberMe';" +
          "document.getElementsByTagName('form')[0].appendChild(checkbox);");

        assertTrue(loginPage.isRememberMeCheckboxPresent());
        loginPage.setRememberMe(true);
        loginPage.login("login-test", getPassword("login-test"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());
        EventRepresentation loginEvent = events.expectLogin().user(userId)
                .detail(Details.USERNAME, "login-test")
                .assertEvent();
        // check remember me is not set although it was sent in the form data
        assertNull(loginEvent.getDetails().get(Details.REMEMBER_ME));
    }

    //KEYCLOAK-2741
    @Test
    public void loginAgainWithoutRememberMe() {
        setRememberMe(true);

        try {
            //login with remember me
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", getPassword("login-test"));

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            Assert.assertEquals("login-test", loginPage.getUsername());

            //login without remember me
            loginPage.setRememberMe(false);
            loginPage.login("login-test", getPassword("login-test"));

            // Expire session
            loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .assertEvent();
            sessionId = loginEvent.getSessionId();
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe not checked nor username/email prefilled
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            assertNotEquals("login-test", loginPage.getUsername());
        } finally {
            setRememberMe(false);
        }
    }

    @Test
    // KEYCLOAK-3181
    public void loginWithEmailUserAndRememberMe() {
        setRememberMe(true);

        try {
            loginPage.open();
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login@test.com", getPassword("login-test"));

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login@test.com")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());

            Assert.assertEquals("login@test.com", loginPage.getUsername());

            loginPage.setRememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }

    @Test
    public void testLoginAfterDisablingRememberMeInRealmSettings() {
        setRememberMe(true);

        try {
            //login with remember me
            loginPage.open();
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login@test.com", getPassword("login-test"));

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());
            events.expectLogin().user(userId)
                    .detail(Details.USERNAME, "login@test.com")
                    .detail(Details.REMEMBER_ME, "true")
                    .assertEvent();

            AccessTokenResponse response = oauth.accessTokenRequest(oauth.parseLoginResponse().getCode()).send();

            setRememberMe(false);

            //refresh fail
            response = oauth.refreshRequest(response.getRefreshToken()).send();
            assertNull(response.getAccessToken());
            assertNotNull(response.getError());
            assertEquals("Session not active", response.getErrorDescription());

            // Assert session removed
            loginPage.open();
            assertFalse(loginPage.isRememberMeCheckboxPresent());
            assertNotEquals("login-test", loginPage.getUsername());
        } finally {
            setRememberMe(false);
        }
    }

    // Login timeout scenarios
    // KEYCLOAK-1037
    @Test
    public void loginExpiredCode() {
        loginPage.open();
        // authSession expired and removed from the storage
        setTimeOffset(5000);

        loginPage.login("login@test.com", getPassword("login-test"));
        loginPage.assertCurrent();

        Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
        setTimeOffset(0);

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .assertEvent();
    }

    // KEYCLOAK-1037
    @Test
    public void loginExpiredCodeWithExplicitRemoveExpired() {
        loginPage.open();
        setTimeOffset(5000);

        loginPage.login("login@test.com", getPassword("login-test"));

        loginPage.assertCurrent();

        Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        setTimeOffset(0);

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .detail(Details.RESTART_AFTER_TIMEOUT, "true")
                .assertEvent();
    }

    @Test
    public void loginAfterExpiredTimeout() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(adminClient.realm("test"))
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(5);
                })
                .update()) {

            loginPage.open();
            loginPage.login("login@test.com", getPassword("login-test"));

            events.expectLogin().user(userId).assertEvent();

            // wait for a timeout
            setTimeOffset(6);

            loginPage.open();
            loginPage.login("login@test.com", getPassword("login-test"));

            events.expectLogin().user(userId).assertEvent();
        }
    }


    @Test
    public void loginExpiredCodeAndExpiredCookies() {
        loginPage.open();

        driver.manage().deleteAllCookies();

        // Cookies are expired including KC_RESTART. No way to continue login. Error page must be shown with the "back to application" link
        loginPage.login("login@test.com", getPassword("login-test"));
        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();

        ClientRepresentation thirdParty = findClientByClientId(adminClient.realm("test"), "third-party").toRepresentation();
        Assert.assertNotNull(link, thirdParty.getBaseUrl());
    }

    @Test
    public void loginWithDisabledCookies() {
        String userId = adminClient.realm("test").users().search("test-user@localhost").get(0).getId();
        oauth.clientId("test-app");
        oauth.openLoginForm();

        driver.manage().deleteAllCookies();


        // Cookie has been deleted or disabled, the error shown in the UI should be Errors.COOKIE_NOT_FOUND
        loginPage.login("login@test.com", getPassword("login-test"));

        events.expect(EventType.LOGIN_ERROR)
                .user(new UserRepresentation())
                .error(Errors.COOKIE_NOT_FOUND)
                .assertEvent();

        errorPage.assertCurrent();
    }

    @Test
    public void openLoginFormWithDifferentApplication() throws Exception {
        oauth.clientId("root-url-client");
        oauth.redirectUri(SERVER_ROOT + "/foo/bar/");
        oauth.openLoginForm();

        // Login form shown after redirect from app
        oauth.clientId("test-app");
        oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
        oauth.openLoginForm();

        assertTrue(loginPage.isCurrent());
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void openLoginFormAfterExpiredCode() throws Exception {
        oauth.openLoginForm();

        setTimeOffset(5000);

        oauth.openLoginForm();

        loginPage.assertCurrent();
        assertNull("Not expected to have error on loginForm.", loginPage.getError());

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void testAuthenticationSessionExpiresEarlyAfterAuthentication() throws Exception {
        // Open login form and refresh right after. This simulates creating another "tab" in rootAuthenticationSession
        oauth.openLoginForm();
        driver.navigate().refresh();

        // Assert authenticationSession in cache with 2 tabs
        String authSessionId = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName()).getValue();
        Assert.assertEquals((Integer) 2, getTestingClient().testing().getAuthenticationSessionTabsCount("test", authSessionId));

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        // authentication session should still exists with remaining browser tab
        Assert.assertEquals((Integer) 1, getTestingClient().testing().getAuthenticationSessionTabsCount("test", authSessionId));

        // authentication session should be expired after 1 minute
        setTimeOffset(300);
        Assert.assertEquals((Integer) 0, getTestingClient().testing().getAuthenticationSessionTabsCount("test", authSessionId));
    }


    @Test
    public void loginRememberMeExpiredIdle() throws Exception {
        try (Closeable c = new RealmAttributeUpdater(adminClient.realm("test"))
          .setSsoSessionIdleTimeoutRememberMe(1)
          .setSsoSessionIdleTimeout(1) // max of both values
          .setRememberMe(true)
          .update()) {
            // login form shown after redirect from app
            oauth.clientId("test-app");
            oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());
            loginPage.setRememberMe(true);
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            // sucessful login - app page should be on display.
            events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
            appPage.assertCurrent();

            // expire idle timeout using the timeout window.
            setTimeOffset(2 + (ProfileAssume.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));

            // trying to open the account page with an expired idle timeout should redirect back to the login page.
            loginPage.open();
            loginPage.assertCurrent();
        }
    }

    @Test
    public void loginRememberMeExpiredMaxLifespan() throws Exception {
        try (Closeable c = new RealmAttributeUpdater(adminClient.realm("test"))
          .setSsoSessionMaxLifespanRememberMe(1)
          .setRememberMe(true)
          .update()) {
            // login form shown after redirect from app
            oauth.clientId("test-app");
            oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());
            loginPage.setRememberMe(true);
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            // sucessful login - app page should be on display.
            events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
            appPage.assertCurrent();

            // expire the max lifespan.
            setTimeOffset(2);

            // trying to open the account page with an expired lifespan should redirect back to the login page.
            loginPage.open();
            loginPage.assertCurrent();
        }
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void loginSuccessfulWithDynamicScope() {
        ProfileAssume.assumeFeatureEnabled(DYNAMIC_SCOPES);
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("dynamic");
        clientScope.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic:*");
        }});
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = testRealm().clientScopes().create(clientScope);
        String scopeId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scopeId);
        response.close();

        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        oauth.doLogin("login@test.com", getPassword("login-test"));
        events.expectLogin().user(userId).assertEvent();
    }

    @Test
    public void loginSuccessfulWithoutWebAuthn() {
        testingClient.disableFeature(Profile.Feature.WEB_AUTHN);
        try {
            loginPage.open();
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            events.expectLogin().assertEvent();
        } finally {
            testingClient.enableFeature(Profile.Feature.WEB_AUTHN);
        }
    }

    @Test
    public void testExecuteActionIfSessionExists() {
        loginPage.open();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();

        UsersResource users = adminClient.realm("test").users();
        UserRepresentation user = users.search("test-user@localhost").get(0);

        user.setRequiredActions(List.of(RequiredAction.CONFIGURE_TOTP.name()));

        try {
            users.get(user.getId()).update(user);

            oauth.openLoginForm();

            // make sure the authentication session is no longer available
            for (Cookie cookie : driver.manage().getCookies()) {
                if (cookie.getName().startsWith(CookieType.AUTH_SESSION_ID.getName())) {
                    driver.manage().deleteCookie(cookie);
                }
            }

            oauth.openLoginForm();
            configTotpPage.assertCurrent();
        } finally {
            user.setRequiredActions(List.of());
            users.get(user.getId()).update(user);
        }

    }

    @Test
    public void testAuthSessionIdCookieFormat(){
        oauth.openLoginForm();
        String encodedBase64AuthSessionId = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName()).getValue();
        String decodedAuthSessionId = new String(Base64Url.decode(encodedBase64AuthSessionId), StandardCharsets.UTF_8);
        Assert.assertTrue(decodedAuthSessionId.contains("."));
        String authSessionId = decodedAuthSessionId.substring(0, decodedAuthSessionId.indexOf("."));
        String signature = decodedAuthSessionId.substring(decodedAuthSessionId.indexOf(".") + 1);
        Assert.assertNotNull(authSessionId);
        MatcherAssert.assertThat(authSessionId, AssertEvents.isSessionId());
        Assert.assertNotNull(signature);

        testingClient.server().run(session-> {
           Assert.assertNotNull(session.authenticationSessions().getRootAuthenticationSession(session.getContext().getRealm(), authSessionId));
        });
   }
}
