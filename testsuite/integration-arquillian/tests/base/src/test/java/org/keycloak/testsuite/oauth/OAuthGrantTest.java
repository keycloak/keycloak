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
package org.keycloak.testsuite.oauth;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantTest extends AbstractKeycloakTest {

    public static final String THIRD_PARTY_APP = "third-party";
    public static final String REALM_NAME = "test";
    private final String DEFAULT_USERNAME = "test-user@localhost";
    private final String DEFAULT_PASSWORD = "password";
    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Page
    protected AppPage appPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    private static String ROLE_USER = "Have User privileges";
    private static String ROLE_CUSTOMER = "Have Customer User privileges";

    @Test
    public void oauthGrantAcceptTest() {
        oauth.client(THIRD_PARTY_APP, "password");
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);

        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);

        grantPage.accept();

        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        EventRepresentation loginEvent = events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String sessionId = loginEvent.getSessionId();

        AccessTokenResponse accessToken = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());

        String tokenString = accessToken.getAccessToken();
        Assert.assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);
        assertEquals(sessionId, token.getSessionState());

        AccessToken.Access realmAccess = token.getRealmAccess();
        assertEquals(1, realmAccess.getRoles().size());
        Assert.assertTrue(realmAccess.isUserInRole("user"));

        Map<String, AccessToken.Access> resourceAccess = token.getResourceAccess();
        assertEquals(1, resourceAccess.size());
        assertEquals(1, resourceAccess.get("test-app").getRoles().size());
        Assert.assertTrue(resourceAccess.get("test-app").isUserInRole("customer-user"));

        events.expectCodeToToken(codeId, loginEvent.getSessionId()).client(THIRD_PARTY_APP).assertEvent();

        AccountHelper.revokeConsents(adminClient.realm(TEST), DEFAULT_USERNAME, THIRD_PARTY_APP);

        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
        Assert.assertEquals(userConsents.size(), 0);

        assertEquals(0, driver.findElements(By.id("revoke-third-party")).size());
    }

    @Test
    public void oauthGrantCancelTest() {
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);

        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);

        grantPage.cancel();

        assertEquals("access_denied", oauth.parseLoginResponse().getError());

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .error("rejected_by_user")
                .removeDetail(Details.CONSENT)
                .session(Matchers.nullValue(String.class))
                .assertEvent();
    }

    @Test
    public void oauthGrantNotShownWhenAlreadyGranted() throws IOException {
        // Grant permissions on grant screen
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);

        grantPage.assertCurrent();
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Assert permissions granted on Account mgmt.
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);

        Assert.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("profile")));
        Assert.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("email")));

        // Open login form and assert grantPage not shown
        oauth.openLoginForm();
        appPage.assertCurrent();
        events.expectLogin()
                .detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT)
                .removeDetail(Details.USERNAME)
                .client(THIRD_PARTY_APP).assertEvent();

        // Revoke grant in account mgmt.
        AccountHelper.revokeConsents(adminClient.realm(TEST), DEFAULT_USERNAME, THIRD_PARTY_APP);

        userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
        Assert.assertEquals(userConsents.size(), 0);

        // Open login form again and assert grant Page is shown
        oauth.openLoginForm();
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
    }

    @Test
    public void oauthGrantAddAnotherScope() throws IOException {
        // Grant permissions on grant screen
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);

        // Create new clientScope and add to client
        RealmResource appRealm = adminClient.realm(REALM_NAME);
        ClientScopeRepresentation scope1 = new ClientScopeRepresentation();
        scope1.setName("foo-scope");
        scope1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = appRealm.clientScopes().create(scope1);
        String fooScopeId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addClientScopeId(fooScopeId);

        // Add clientScope to client
        ClientResource thirdParty = findClientByClientId(appRealm, THIRD_PARTY_APP);
        thirdParty.addDefaultClientScope(fooScopeId);

        // Confirm grant page
        grantPage.assertCurrent();
        grantPage.accept();
        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Assert new clientScope not yet in account mgmt
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
        Assert.assertFalse(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("foo-scope")));

        // Show grant page another time. Just new clientScope is on the page
        oauth.openLoginForm();
        grantPage.assertCurrent();
        grantPage.assertGrants("foo-scope");

        grantPage.accept();
        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Go to account mgmt. Everything is granted now
        userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
        Assert.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("foo-scope")));

        // Revoke
        AccountHelper.revokeConsents(adminClient.realm(TEST), DEFAULT_USERNAME, THIRD_PARTY_APP);

        userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
        Assert.assertEquals(userConsents.size(), 0);

        // Cleanup
        thirdParty.removeDefaultClientScope(fooScopeId);
    }

    @Test
    public void oauthGrantScopeParamRequired() throws Exception {
        RealmResource appRealm = adminClient.realm(REALM_NAME);
        ClientResource thirdParty = findClientByClientId(appRealm, THIRD_PARTY_APP);

        // Create clientScope
        ClientScopeRepresentation scope1 = new ClientScopeRepresentation();
        scope1.setName("foo-scope");
        scope1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = appRealm.clientScopes().create(scope1);
        String fooScopeId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addClientScopeId(fooScopeId);

        // Add clientScope as optional to client
        thirdParty.addOptionalClientScope(fooScopeId);

        // Assert clientScope not on grant screen when not requested
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assert.assertFalse(grants.contains("foo-scope"));
        grantPage.cancel();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .error("rejected_by_user")
                .removeDetail(Details.CONSENT)
                .session(Matchers.nullValue(String.class))
                .assertEvent();

        oauth.scope("foo-scope");
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        grants = grantPage.getDisplayedGrants();
        Assert.assertTrue(grants.contains("foo-scope"));
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Revoke
        AccountHelper.revokeConsents(adminClient.realm(TEST), DEFAULT_USERNAME, THIRD_PARTY_APP);

        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
        Assert.assertEquals(userConsents.size(), 0);

        // cleanup
        oauth.scope(null);
        thirdParty.removeOptionalClientScope(fooScopeId);
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void oauthGrantDynamicScopeParamRequired() throws IOException {
        RealmResource appRealm = adminClient.realm(REALM_NAME);
        ClientResource thirdParty = findClientByClientId(appRealm, THIRD_PARTY_APP);

        // Create clientScope
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName("foo-dynamic-scope");
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        scope.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "foo-dynamic-scope:*");
        }});
        Response response = appRealm.clientScopes().create(scope);
        String dynamicFooScopeId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addClientScopeId(dynamicFooScopeId);

        // Add clientScope as optional to client
        thirdParty.addOptionalClientScope(dynamicFooScopeId);

        // Assert clientScope not on grant screen when not requested
        oauth.clientId(THIRD_PARTY_APP);
        oauth.scope("foo-dynamic-scope:withparam");
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assert.assertTrue(grants.contains("foo-dynamic-scope: withparam"));
        grantPage.accept();

        EventRepresentation loginEvent = events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);

        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client(THIRD_PARTY_APP)
                .assertEvent();

        oauth.logoutForm().idTokenHint(res.getIdToken()).open();

        events.expectLogout(loginEvent.getSessionId()).client(THIRD_PARTY_APP).removeDetail(Details.REDIRECT_URI).assertEvent();

        // login again to check whether the Dynamic scope and only the dynamic scope is requested again
        oauth.scope("foo-dynamic-scope:withparam");
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        grants = grantPage.getDisplayedGrants();
        Assert.assertEquals(1, grants.size());
        Assert.assertTrue(grants.contains("foo-dynamic-scope: withparam"));
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Revoke
        AccountHelper.revokeConsents(adminClient.realm(TEST), DEFAULT_USERNAME, THIRD_PARTY_APP);

        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
        Assert.assertEquals(userConsents.size(), 0);

        // cleanup
        oauth.scope(null);
        thirdParty.removeOptionalClientScope(dynamicFooScopeId);
    }


    // KEYCLOAK-4326
    @Test
    public void oauthGrantClientScopeMappers() throws Exception {
        // Add client scope with some protocol mapper
        RealmResource appRealm = adminClient.realm(REALM_NAME);

        ClientScopeRepresentation scope1 = new ClientScopeRepresentation();
        scope1.setName("foo-addr");
        scope1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = appRealm.clientScopes().create(scope1);
        String fooScopeId = ApiUtil.getCreatedId(response);
        response.close();

        ProtocolMapperRepresentation protocolMapper = ProtocolMapperUtil.createAddressMapper(true, true, true, true);
        response = appRealm.clientScopes().get(fooScopeId).getProtocolMappers().createMapper(protocolMapper);
        response.close();

        // Add clientScope to client
        ClientResource thirdParty = findClientByClientId(appRealm, THIRD_PARTY_APP);
        thirdParty.addDefaultClientScope(fooScopeId);
        getCleanup().addClientScopeId(fooScopeId);

        // Login
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT, "foo-addr");
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Go to user's application screen
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);

        Assert.assertEquals("third-party", userConsents.get(0).get("clientId"));
        Assert.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("foo-addr")));

        // Login as admin and see the consent screen of particular user
        UserResource user = ApiUtil.findUserByUsernameId(appRealm, "test-user@localhost");
        List<Map<String, Object>> consents = user.getConsents();
        Assert.assertEquals(1, consents.size());

        // Assert automatically logged another time
        oauth.openLoginForm();
        appPage.assertCurrent();
        events.expectLogin()
                .detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT)
                .removeDetail(Details.USERNAME)
                .client(THIRD_PARTY_APP).assertEvent();

        // Revoke
        AccountHelper.revokeConsents(adminClient.realm(TEST), DEFAULT_USERNAME, THIRD_PARTY_APP);

        userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
        Assert.assertEquals(userConsents.size(), 0);

        // Cleanup
        thirdParty.removeDefaultClientScope(fooScopeId);
    }

    @Test
    public void oauthGrantExpiredAuthSession() throws Exception {
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);

        grantPage.assertCurrent();

        // Expire cookies
        driver.manage().deleteAllCookies();

        grantPage.accept();

        // Assert link "back to application" present
        errorPage.assertCurrent();
        String backToAppLink = errorPage.getBackToApplicationLink();
        ClientRepresentation thirdParty = findClientByClientId(adminClient.realm(REALM_NAME), THIRD_PARTY_APP).toRepresentation();
        Assert.assertEquals(backToAppLink, thirdParty.getBaseUrl());
    }

    // KEYCLOAK-7470
    @Test
    public void oauthGrantOrderedClientScopes() throws Exception {
        // Add GUI Order to client scopes --- email=1, profile=2
        RealmResource appRealm = adminClient.realm(REALM_NAME);

        ClientScopeResource emailScope = ApiUtil.findClientScopeByName(appRealm, "email");
        ClientScopeRepresentation emailRep = emailScope.toRepresentation();
        emailRep.getAttributes().put(ClientScopeModel.GUI_ORDER, "1");
        emailScope.update(emailRep);
        Assert.assertEquals("1", emailRep.getAttributes().get(ClientScopeModel.GUI_ORDER));

        ClientScopeResource profileScope = ApiUtil.findClientScopeByName(appRealm, "profile");
        ClientScopeRepresentation profileRep = profileScope.toRepresentation();
        profileRep.getAttributes().put(ClientScopeModel.GUI_ORDER, "2");
        profileScope.update(profileRep);
        Assert.assertEquals("2", profileRep.getAttributes().get(ClientScopeModel.GUI_ORDER));

        // Display consent screen --- assert email, then profile
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);

        grantPage.assertCurrent();
        List<String> displayedScopes = grantPage.getDisplayedGrants();
        Assert.assertEquals("Email address", displayedScopes.get(0));
        Assert.assertEquals("User profile", displayedScopes.get(1));
        grantPage.accept();

        // Update GUI Order --- email=3
        emailRep = emailScope.toRepresentation();
        emailRep.getAttributes().put(ClientScopeModel.GUI_ORDER, "3");
        emailScope.update(emailRep);
        Assert.assertEquals("3", emailRep.getAttributes().get(ClientScopeModel.GUI_ORDER));

        // Revoke grant and display consent screen --- assert profile, then email
        AccountHelper.revokeConsents(adminClient.realm(TEST), DEFAULT_USERNAME, THIRD_PARTY_APP);

        oauth.openLoginForm();
        grantPage.assertCurrent();
        displayedScopes = grantPage.getDisplayedGrants();
        Assert.assertEquals("User profile", displayedScopes.get(0));
        Assert.assertEquals("Email address", displayedScopes.get(1));
    }


    // KEYCLOAK-16006 - tests that after revoke consent from single client, the SSO session is still valid and not automatically logged-out
    @Test
    public void oauthGrantUserNotLoggedOutAfterConsentRevoke() throws Exception {
        // Login
        oauth.clientId(THIRD_PARTY_APP);
        oauth.doLogin(DEFAULT_USERNAME, DEFAULT_PASSWORD);

        // Confirm consent screen
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        EventRepresentation loginEvent = events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();
        String sessionId = loginEvent.getSessionId();

        // Revoke consent with admin REST API
        adminClient.realm(REALM_NAME).users().get(loginEvent.getUserId()).revokeConsent(THIRD_PARTY_APP);

        // Make sure that after refresh, consent page is displayed and user doesn't need to re-authenticate. Just accept consent screen again
        oauth.openLoginForm();

        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        loginEvent = events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        //String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String sessionId2 = loginEvent.getSessionId();
        Assert.assertEquals(sessionId, sessionId2);

        // Revert consent
        adminClient.realm(REALM_NAME).users().get(loginEvent.getUserId()).revokeConsent(THIRD_PARTY_APP);
    }

}
