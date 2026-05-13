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
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.EventMatchers;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.AdminApiUtil;
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
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.runonserver.RunHelpers;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;

import static org.keycloak.common.Profile.Feature.DYNAMIC_SCOPES;
import static org.keycloak.testsuite.admin.AdminApiUtil.findClientByClientId;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.oauth.OAuthClient.SERVER_ROOT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        RealmBuilder.update(testRealm)
                    .users(user)
                    .users(user2)
                    .users(admin);
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
        userId = managedRealm.admin().users().search("login-test", Boolean.TRUE).get(0).getId();
        user2Id = managedRealm.admin().users().search("login-test2", Boolean.TRUE).get(0).getId();
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
                Assertions.assertNotNull(headerValue);
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

            EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.REDIRECT_URI, longRedirectUri);
        }
    }

    @Test
    public void loginChangeUserAfterInvalidPassword() {
        oauth.openLoginForm();
        loginPage.login("login-test2", "invalid");

        loginPage.assertCurrent();

        Assertions.assertEquals("login-test2", loginPage.getUsername());
        Assertions.assertEquals("", loginPage.getPassword());

        Assertions.assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertNull(loginPage.getPasswordInputError());

        EventAssertion.expectLoginError(events.poll()).userId(user2Id).sessionId(null).error("invalid_user_credentials")
                .details(Details.USERNAME, "login-test2")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .withoutDetails(Details.CONSENT);

        loginPage.login("login-test", getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginInvalidPassword() {
        oauth.openLoginForm();
        loginPage.login("login-test", "invalid");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assertions.assertEquals("login-test", loginPage.getUsername());
        Assertions.assertEquals("", loginPage.getPassword());

        Assertions.assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertNull(loginPage.getPasswordInputError());

        EventAssertion.expectLoginError(events.poll()).userId(userId).sessionId(null).error("invalid_user_credentials")
                .details(Details.USERNAME, "login-test")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .withoutDetails(Details.CONSENT);
    }

    @Test
    public void loginMissingPassword() {
        oauth.openLoginForm();
        loginPage.missingPassword("login-test");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assertions.assertEquals("login-test", loginPage.getUsername());
        Assertions.assertEquals("", loginPage.getPassword());

        Assertions.assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertNull(loginPage.getPasswordInputError());

        EventAssertion.expectLoginError(events.poll()).userId(userId).sessionId(null).error("invalid_user_credentials")
                .details(Details.USERNAME, "login-test")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .withoutDetails(Details.CONSENT);
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
            oauth.openLoginForm();
            loginPage.login("login-test", "invalid");

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assertions.assertEquals("login-test", loginPage.getUsername());
            Assertions.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());

            EventAssertion.expectLoginError(events.poll()).userId(userId).sessionId(null).error("invalid_user_credentials")
                    .details(Details.USERNAME, "login-test")
                    .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                    .withoutDetails(Details.CONSENT);
        } finally {
            setUserEnabled(userId, true);
        }
    }

    @Test
    public void loginDisabledUser() {
        setUserEnabled(userId, false);

        try {
            oauth.openLoginForm();
            loginPage.login("login-test", getPassword("login-test"));

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assertions.assertEquals("login-test", loginPage.getUsername());
            Assertions.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assertions.assertEquals("Account is disabled, contact your administrator.", loginPage.getError());

            EventAssertion.expectLoginError(events.poll()).userId(userId).sessionId(null).error("user_disabled")
                    .details(Details.USERNAME, "login-test")
                    .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                    .withoutDetails(Details.CONSENT);
        } finally {
            setUserEnabled(userId, true);
        }
    }

    @Test
    public void loginDifferentUserAfterDisabledUserThrownOut() {
        String userId = AccountHelper.getUserRepresentation(adminClient.realm("test"), "test-user@localhost").getId();

        try {
            oauth.openLoginForm();
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            appPage.assertCurrent();
            appPage.openAccount();

            setUserEnabled(userId, false);

            oauth.openLoginForm();
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
        oauth.openLoginForm();
        loginPage.login("invalid", "invalid");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assertions.assertEquals("invalid", loginPage.getUsername());
        Assertions.assertEquals("", loginPage.getPassword());

        Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());

        EventAssertion.expectLoginError(events.poll()).userId(null).sessionId(null).error("user_not_found")
                .details(Details.USERNAME, "invalid")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .withoutDetails(Details.CONSENT);

        loginPage.login("login-test", getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginMissingUsername() {
        oauth.openLoginForm();
        loginPage.missingUsername();

        loginPage.assertCurrent();

        Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());

        EventAssertion.expectLoginError(events.poll()).userId(null).sessionId(null).error("user_not_found")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .withoutDetails(Details.CONSENT);
    }

    @Test
    // KEYCLOAK-2557
    public void loginUserWithEmailAsUsername() {
        oauth.openLoginForm();
        loginPage.login("login@test.com", getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login@test.com");
    }

    @Test
    public void loginSuccess() {
        oauth.openLoginForm();
        loginPage.login("login-test", getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginSuccessRealmSigningAlgorithms() throws JWSInputException {
        ContainerAssume.assumeAuthServerSSL();

        oauth.openLoginForm();
        loginPage.login("login-test", getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");

        driver.navigate().to(getAuthServerContextRoot() + "/auth/realms/test/");
        String keycloakIdentity = driver.manage().getCookieNamed("KEYCLOAK_IDENTITY").getValue();

        // Check identity cookie is signed with HS256
        String algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
        assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, algorithm);

        try {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.ES256);

            oauth.openLoginForm();
            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            driver.navigate().to(getAuthServerContextRoot() + "/auth/realms/test/");
            keycloakIdentity = driver.manage().getCookieNamed("KEYCLOAK_IDENTITY").getValue();

            // Check identity cookie is still signed with HS256
            algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
            assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, algorithm);

            // Check identity cookie still works
            oauth.openLoginForm();
            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
        }
    }

    @Test
    public void loginWithWhitespaceSuccess() {
        oauth.openLoginForm();
        loginPage.login(" login-test \t ", getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginWithEmailWhitespaceSuccess() {
        oauth.openLoginForm();
        loginPage.login("    login@test.com    ", getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId);
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
            timeOffSet.set(86405);

            oauth.openLoginForm();

            loginPage.login("login-test", getPassword("login-test"));

            updatePasswordPage.assertCurrent();

            final String newPwd = generatePassword("login-test");
            updatePasswordPage.changePassword(newPwd, newPwd);

            timeOffSet.set(0);

            events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).detail(Details.USERNAME, "login-test").assertEvent();
            events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).detail(Details.USERNAME, "login-test").assertEvent();

            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType(), "bad expectation, on page: " + currentUrl);

            EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");

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
            timeOffSet.set(86205);

            oauth.openLoginForm();

            loginPage.login("login-test", getPassword("login-test"));

            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

            timeOffSet.set(0);

            EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");
        } finally {
            setPasswordPolicy(null);
        }
    }

    @Test
    public void loginNoTimeoutWithLongWait() {
        oauth.openLoginForm();

        timeOffSet.set(1700);

        loginPage.login("login-test", getPassword("login-test"));

        timeOffSet.set(0);

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginLoginHint() {
        oauth.loginForm().param("login_hint", "login-test").open();

        Assertions.assertEquals("login-test", loginPage.getUsername());
        loginPage.login(getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginWithEmailSuccess() {
        oauth.openLoginForm();
        loginPage.login("login@test.com", getPassword("login-test"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectLoginSuccess(events.poll()).userId(userId);
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
            oauth.openLoginForm();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", getPassword("login-test"));

            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assertions.assertNotNull(oauth.parseLoginResponse().getCode());
            EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll()).userId(userId)
                                                   .details(Details.USERNAME, "login-test")
                                                   .details(Details.REMEMBER_ME, "true").getEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            runOnServer.run(RunHelpers.removeUserSession("test", sessionId));

            // Assert rememberMe checked and username/email prefilled
            oauth.openLoginForm();
            assertTrue(loginPage.isRememberMeChecked());
            Assertions.assertEquals("login-test", loginPage.getUsername());

            loginPage.setRememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }

    @Test
    public void loginWithRememberMeNotSet() {
        oauth.openLoginForm();
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

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());
        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll()).userId(userId)
                .details(Details.USERNAME, "login-test").getEvent();
        // check remember me is not set although it was sent in the form data
        assertNull(loginEvent.getDetails().get(Details.REMEMBER_ME));
    }

    //KEYCLOAK-2741
    @Test
    public void loginAgainWithoutRememberMe() {
        setRememberMe(true);

        try {
            //login with remember me
            oauth.openLoginForm();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", getPassword("login-test"));

            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assertions.assertNotNull(oauth.parseLoginResponse().getCode());
            EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll()).userId(userId)
                                                   .details(Details.USERNAME, "login-test")
                                                   .details(Details.REMEMBER_ME, "true").getEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            runOnServer.run(RunHelpers.removeUserSession("test", sessionId));

            // Assert rememberMe checked and username/email prefilled
            oauth.openLoginForm();
            assertTrue(loginPage.isRememberMeChecked());
            Assertions.assertEquals("login-test", loginPage.getUsername());

            //login without remember me
            loginPage.setRememberMe(false);
            loginPage.login("login-test", getPassword("login-test"));

            // Expire session
            loginEvent = EventAssertion.expectLoginSuccess(events.poll()).userId(userId)
                                                   .details(Details.USERNAME, "login-test").getEvent();
            sessionId = loginEvent.getSessionId();
            runOnServer.run(RunHelpers.removeUserSession("test", sessionId));

            // Assert rememberMe not checked nor username/email prefilled
            oauth.openLoginForm();
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
            oauth.openLoginForm();
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login@test.com", getPassword("login-test"));

            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assertions.assertNotNull(oauth.parseLoginResponse().getCode());
            EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll()).userId(userId)
                                                   .details(Details.USERNAME, "login@test.com")
                                                   .details(Details.REMEMBER_ME, "true").getEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            runOnServer.run(RunHelpers.removeUserSession("test", sessionId));

            // Assert rememberMe checked and username/email prefilled
            oauth.openLoginForm();
            assertTrue(loginPage.isRememberMeChecked());

            Assertions.assertEquals("login@test.com", loginPage.getUsername());

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
            oauth.openLoginForm();
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login@test.com", getPassword("login-test"));

            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assertions.assertNotNull(oauth.parseLoginResponse().getCode());
            EventAssertion.expectLoginSuccess(events.poll()).userId(userId)
                    .details(Details.USERNAME, "login@test.com")
                    .details(Details.REMEMBER_ME, "true");

            AccessTokenResponse response = oauth.accessTokenRequest(oauth.parseLoginResponse().getCode()).send();

            setRememberMe(false);

            //refresh fail
            response = oauth.refreshRequest(response.getRefreshToken()).send();
            assertNull(response.getAccessToken());
            assertNotNull(response.getError());
            assertEquals("Session not active", response.getErrorDescription());

            // Assert session removed
            oauth.openLoginForm();
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
        oauth.openLoginForm();
        // authSession expired and removed from the storage
        timeOffSet.set(5000);

        loginPage.login("login@test.com", getPassword("login-test"));
        loginPage.assertCurrent();

        Assertions.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
        timeOffSet.set(0);

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).userId(null).sessionId(null).error(Errors.EXPIRED_CODE);
    }

    // KEYCLOAK-1037
    @Test
    public void loginExpiredCodeWithExplicitRemoveExpired() {
        oauth.openLoginForm();
        timeOffSet.set(5000);

        loginPage.login("login@test.com", getPassword("login-test"));

        loginPage.assertCurrent();

        Assertions.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        timeOffSet.set(0);

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).userId(null).sessionId(null).error(Errors.EXPIRED_CODE)
                .details(Details.RESTART_AFTER_TIMEOUT, "true");
    }

    @Test
    public void loginAfterExpiredTimeout() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(adminClient.realm("test"))
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(5);
                })
                .update()) {

            oauth.openLoginForm();
            loginPage.login("login@test.com", getPassword("login-test"));

            EventAssertion.expectLoginSuccess(events.poll()).userId(userId);

            // wait for a timeout
            timeOffSet.set(6);

            oauth.openLoginForm();
            loginPage.login("login@test.com", getPassword("login-test"));

            EventAssertion.expectLoginSuccess(events.poll()).userId(userId);
        }
    }


    @Test
    public void loginExpiredCodeAndExpiredCookies() {
        oauth.openLoginForm();

        driver.manage().deleteAllCookies();

        // Cookies are expired including KC_RESTART. No way to continue login. Error page must be shown with the "back to application" link
        loginPage.login("login@test.com", getPassword("login-test"));
        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();

        ClientRepresentation thirdParty = findClientByClientId(adminClient.realm("test"), "third-party").toRepresentation();
        Assertions.assertNotNull(thirdParty.getBaseUrl(), link);
    }

    @Test
    public void loginWithDisabledCookies() {
        String userId = adminClient.realm("test").users().search("test-user@localhost").get(0).getId();
        oauth.client("test-app", "password");
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
    public void loginWithClientDisabledInActiveAuthenticationSession() {
        ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        boolean wasEnabled = clientRepresentation.isEnabled();

        try {
            oauth.client("test-app", "password");
            oauth.openLoginForm();
            loginPage.assertCurrent();

            clientRepresentation.setEnabled(false);
            clientResource.update(clientRepresentation);

            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            errorPage.assertCurrent();
            assertEquals("Login requester not enabled", errorPage.getError());
            events.expect(EventType.LOGIN)
                    .client("test-app")
                    .user((String) null)
                    .session((String) null)
                    .error(Errors.CLIENT_DISABLED)
                    .assertEvent();
        } finally {
            clientRepresentation.setEnabled(wasEnabled);
            clientResource.update(clientRepresentation);
        }
    }

    @Test
    public void openLoginFormWithDifferentApplication() throws Exception {
        oauth.client("root-url-client");
        oauth.redirectUri(SERVER_ROOT + "/foo/bar/");
        oauth.openLoginForm();

        // Login form shown after redirect from app
        oauth.client("test-app", "password");
        oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
        oauth.openLoginForm();

        assertTrue(loginPage.isCurrent());
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, "test-user@localhost");
    }

    @Test
    public void openLoginFormAfterExpiredCode() throws Exception {
        oauth.openLoginForm();

        timeOffSet.set(5000);

        oauth.openLoginForm();

        loginPage.assertCurrent();
        assertNull(loginPage.getError(), "Not expected to have error on loginForm.");

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, "test-user@localhost");
    }

    @Test
    public void testAuthenticationSessionExpiresEarlyAfterAuthentication() throws Exception {
        // Open login form and refresh right after. This simulates creating another "tab" in rootAuthenticationSession
        oauth.openLoginForm();
        driver.navigate().refresh();

        // Assert authenticationSession in cache with 2 tabs
        String authSessionId = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName()).getValue();
        Assertions.assertEquals((Integer) 2, getTestingClient().testing().getAuthenticationSessionTabsCount("test", authSessionId));

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        // authentication session should still exists with remaining browser tab
        Assertions.assertEquals((Integer) 1, getTestingClient().testing().getAuthenticationSessionTabsCount("test", authSessionId));

        // authentication session should be expired after 1 minute
        timeOffSet.set(300);
        Assertions.assertEquals((Integer) 0, getTestingClient().testing().getAuthenticationSessionTabsCount("test", authSessionId));
    }


    @Test
    public void loginRememberMeExpiredIdle() throws Exception {
        try (Closeable c = new RealmAttributeUpdater(adminClient.realm("test"))
          .setSsoSessionIdleTimeoutRememberMe(1)
          .setSsoSessionIdleTimeout(1) // max of both values
          .setRememberMe(true)
          .update()) {
            // login form shown after redirect from app
            oauth.client("test-app", "password");
            oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());
            loginPage.setRememberMe(true);
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            // sucessful login - app page should be on display.
            EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, "test-user@localhost");
            appPage.assertCurrent();

            // expire idle timeout using the timeout window.
            timeOffSet.set(2 + (ProfileAssume.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));

            // trying to open the account page with an expired idle timeout should redirect back to the login page.
            oauth.openLoginForm();
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
            oauth.client("test-app", "password");
            oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());
            loginPage.setRememberMe(true);
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            // sucessful login - app page should be on display.
            EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, "test-user@localhost");
            appPage.assertCurrent();

            // expire the max lifespan.
            timeOffSet.set(2);

            // trying to open the account page with an expired lifespan should redirect back to the login page.
            oauth.openLoginForm();
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
        Response response = managedRealm.admin().clientScopes().create(clientScope);
        String scopeId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scopeId);
        response.close();

        ClientResource testApp = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        oauth.doLogin("login@test.com", getPassword("login-test"));
        EventAssertion.expectLoginSuccess(events.poll()).userId(userId);
    }

    @Test
    public void loginSuccessfulWithoutWebAuthn() {
        testingClient.disableFeature(Profile.Feature.WEB_AUTHN);
        try {
            oauth.openLoginForm();
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            EventAssertion.expectLoginSuccess(events.poll());
        } finally {
            testingClient.enableFeature(Profile.Feature.WEB_AUTHN);
        }
    }

    @Test
    public void testExecuteActionIfSessionExists() {
        oauth.openLoginForm();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        EventAssertion.expectLoginSuccess(events.poll());

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
        Assertions.assertTrue(decodedAuthSessionId.contains("."));
        String authSessionId = decodedAuthSessionId.substring(0, decodedAuthSessionId.indexOf("."));
        String signature = decodedAuthSessionId.substring(decodedAuthSessionId.indexOf(".") + 1);
        Assertions.assertNotNull(authSessionId);
        MatcherAssert.assertThat(authSessionId, EventMatchers.isSessionId());
        Assertions.assertNotNull(signature);

        testingClient.server().run(session-> {
           Assertions.assertNotNull(session.authenticationSessions().getRootAuthenticationSession(session.getContext().getRealm(), authSessionId));
        });
   }
}
