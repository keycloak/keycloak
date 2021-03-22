/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oidc;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AddressClaimSet;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * Test for OAuth2 'scope' parameter and for some other aspects of client scopes
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class OIDCScopeTest extends AbstractOIDCScopeTest {

    private static String userId = KeycloakModelUtils.generateId();

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = UserBuilder.create()
                .id(userId)
                .username("john")
                .enabled(true)
                .email("john@email.cz")
                .firstName("John")
                .lastName("Doe")
                .password("password")
                .role("account", "manage-account")
                .role("account", "view-profile")
                .addRoles("role-1", "role-2")
                .build();

        user.setEmailVerified(true);
        MultivaluedHashMap<String, String> attrs = new MultivaluedHashMap<>();
        attrs.add("street", "Elm 5");
        attrs.add("phoneNumber", "111-222-333");
        attrs.add("phoneNumberVerified", "true");
        user.setAttributes(attrs);

        testRealm.getUsers().add(user);


        // Add sample realm roles
        RoleRepresentation role1 = new RoleRepresentation();
        role1.setName("role-1");
        testRealm.getRoles().getRealm().add(role1);
        RoleRepresentation role2 = new RoleRepresentation();
        role2.setName("role-2");
        testRealm.getRoles().getRealm().add(role2);

        RoleRepresentation roleParent = RoleBuilder.create()
                .name("role-parent")
                .realmComposite("role-1")
                .build();
        testRealm.getRoles().getRealm().add(roleParent);

        // Add sample group
        GroupRepresentation group = new GroupRepresentation();
        group.setName("group-role-1");
        group.setRealmRoles(Collections.singletonList("role-1"));
        testRealm.getGroups().add(group);

        // Add more sample users
        user = UserBuilder.create()
                .username("role-1-user")
                .enabled(true)
                .password("password")
                .addRoles("role-1")
                .build();
        testRealm.getUsers().add(user);

        user = UserBuilder.create()
                .username("role-2-user")
                .enabled(true)
                .password("password")
                .addRoles("role-2")
                .build();
        testRealm.getUsers().add(user);

        user = UserBuilder.create()
                .username("role-parent-user")
                .enabled(true)
                .password("password")
                .addRoles("role-parent")
                .build();
        testRealm.getUsers().add(user);

        user = UserBuilder.create()
                .username("group-role-1-user")
                .enabled(true)
                .password("password")
                .addGroups("group-role-1")
                .build();
        testRealm.getUsers().add(user);
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        oauth.clientId("test-app");
        oauth.scope(null);
        oauth.maxAge(null);
    }

    @After
    public void removePersistentConsentFromUser() {
        try {
            adminClient.realm("test").users().get(userId).revokeConsent("third-party");
        } catch (NotFoundException nfe) {
            // Ignore if consent not present
        }
    }


    @Test
    public void testBuiltinOptionalScopes() throws Exception {
        // Login. Assert that just 'profile' and 'email' data are there. 'Address' and 'phone' not
        oauth.doLogin("john", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid email profile", "test-app");
        IDToken idToken = tokens.idToken;

        assertProfile(idToken, true);
        assertEmail(idToken, true);
        assertAddress(idToken, false);
        assertPhone(idToken, false);
        // check both idtoken and access token for microprofile claims.
        assertMicroprofile(idToken, false);
        assertMicroprofile(tokens.accessToken, false);

        // Logout
        oauth.doLogout(tokens.refreshToken, "password");
        events.expectLogout(idToken.getSessionState())
                .client("test-app")
                .user(userId)
                .removeDetail(Details.REDIRECT_URI).assertEvent();

        // Login with optional scopes. Assert that everything is there
        oauth.scope("openid address phone microprofile-jwt");
        oauth.doLogin("john", "password");
        loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();
        tokens = sendTokenRequest(loginEvent, userId,"openid email profile address phone microprofile-jwt", "test-app");
        idToken = tokens.idToken;

        assertProfile(idToken, true);
        assertEmail(idToken, true);
        assertAddress(idToken, true);
        assertPhone(idToken, true);
        assertMicroprofile(idToken, true);
        assertMicroprofile(tokens.accessToken, true);
    }


    private void assertProfile(IDToken idToken, boolean claimsIn) {
        if (claimsIn) {
            Assert.assertEquals("john", idToken.getPreferredUsername());
            Assert.assertEquals("John", idToken.getGivenName());
            Assert.assertEquals("Doe", idToken.getFamilyName());
            Assert.assertEquals("John Doe", idToken.getName());
        } else {
            Assert.assertNull(idToken.getPreferredUsername());
            Assert.assertNull(idToken.getGivenName());
            Assert.assertNull(idToken.getFamilyName());
            Assert.assertNull(idToken.getName());
        }
    }


    private void assertEmail(IDToken idToken, boolean claimsIn) {
        if (claimsIn) {
            Assert.assertEquals("john@email.cz", idToken.getEmail());
            Assert.assertEquals(true, idToken.getEmailVerified());
        } else {
            Assert.assertNull(idToken.getEmail());
            Assert.assertNull(idToken.getEmailVerified());
        }
    }


    private void assertAddress(IDToken idToken, boolean claimsIn) {
        AddressClaimSet address = idToken.getAddress();
        if (claimsIn) {
            Assert.assertNotNull(address);
            Assert.assertEquals("Elm 5", address.getStreetAddress());
        } else {
            Assert.assertNull(address);
        }
    }


    private void assertPhone(IDToken idToken, boolean claimsIn) {
        if (claimsIn) {
            Assert.assertEquals("111-222-333", idToken.getPhoneNumber());
            Assert.assertEquals(true, idToken.getPhoneNumberVerified());
        } else {
            Assert.assertNull(idToken.getPhoneNumber());
            Assert.assertNull(idToken.getPhoneNumberVerified());
        }
    }


    private void assertMicroprofile(IDToken idToken, boolean claimsIn) {
        if (claimsIn) {
            Assert.assertTrue(idToken.getOtherClaims().containsKey("upn"));
            Assert.assertEquals("john", idToken.getOtherClaims().get("upn"));
            Assert.assertTrue(idToken.getOtherClaims().containsKey("groups"));
            List<String> groups = (List<String>) idToken.getOtherClaims().get("groups");
            Assert.assertNotNull(groups);
            Assert.assertTrue(groups.containsAll(Arrays.asList("role-1", "role-2")));
        } else {
            Assert.assertFalse(idToken.getOtherClaims().containsKey("upn"));
            Assert.assertFalse(idToken.getOtherClaims().containsKey("groups"));
        }
    }


    @Test
    public void testRemoveScopes() throws Exception {
        // Add 'profile' as optional scope. Remove 'email' scope entirely
        String profileScopeId = ApiUtil.findClientScopeByName(testRealm(), "profile").toRepresentation().getId();
        String emailScopeId = ApiUtil.findClientScopeByName(testRealm(), "email").toRepresentation().getId();

        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        testApp.removeDefaultClientScope(profileScopeId);
        testApp.removeDefaultClientScope(emailScopeId);
        testApp.addOptionalClientScope(profileScopeId);

        // Login without scope parameter. Assert 'profile' and 'email' info not there
        oauth.doLogin("john", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId,"openid", "test-app");
        IDToken idToken = tokens.idToken;

        assertProfile(idToken, false);
        assertEmail(idToken, false);
        assertAddress(idToken, false);
        assertPhone(idToken, false);

        // Logout
        oauth.doLogout(tokens.refreshToken, "password");
        events.expectLogout(idToken.getSessionState())
                .client("test-app")
                .user(userId)
                .removeDetail(Details.REDIRECT_URI).assertEvent();

        // Login with scope parameter. Just 'profile' is there
        oauth.scope("openid profile");
        oauth.doLogin("john", "password");
        loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();
        tokens = sendTokenRequest(loginEvent, userId,"openid profile", "test-app");
        idToken = tokens.idToken;

        assertProfile(idToken, true);
        assertEmail(idToken, false);
        assertAddress(idToken, false);
        assertPhone(idToken, false);

        // Revert
        testApp.removeOptionalClientScope(profileScopeId);
        testApp.addDefaultClientScope(profileScopeId);
        testApp.addDefaultClientScope(emailScopeId);
    }


    @Test
    public void testOptionalScopesWithConsentRequired() throws Exception {
        // Remove "displayOnConsentScreen" from address
        ClientScopeResource addressScope = ApiUtil.findClientScopeByName(testRealm(), "address");
        ClientScopeRepresentation addressScopeRep = addressScope.toRepresentation();
        addressScopeRep.getAttributes().put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "false");
        addressScope.update(addressScopeRep);

        oauth.clientId("third-party");
        oauth.doLoginGrant("john", "password");

        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId,"openid email profile", "third-party");
        IDToken idToken = tokens.idToken;

        assertProfile(idToken, true);
        assertEmail(idToken, true);
        assertAddress(idToken, false);
        assertPhone(idToken, false);

        // Logout
        oauth.doLogout(tokens.refreshToken, "password");
        events.expectLogout(idToken.getSessionState())
                .client("third-party")
                .user(userId)
                .removeDetail(Details.REDIRECT_URI).assertEvent();

        // Login with optional scopes. Grant screen should have just "phone"
        oauth.scope("openid address phone");
        oauth.doLoginGrant("john", "password");

        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PHONE_CONSENT_TEXT);
        grantPage.accept();

        loginEvent = events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .user(userId)
                .assertEvent();
        tokens = sendTokenRequest(loginEvent, userId,"openid email profile address phone", "third-party");
        idToken = tokens.idToken;

        assertProfile(idToken, true);
        assertEmail(idToken, true);
        assertAddress(idToken, true);
        assertPhone(idToken, true);

        // Revert
        addressScopeRep.getAttributes().put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "true");
        addressScope.update(addressScopeRep);
    }


    @Test
    public void testClientDisplayedOnConsentScreen() throws Exception {
        // Add "displayOnConsentScreen" to client
        ClientResource thirdParty = ApiUtil.findClientByClientId(testRealm(), "third-party");
        ClientRepresentation thirdPartyRep = thirdParty.toRepresentation();
        thirdPartyRep.getAttributes().put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "true");
        thirdPartyRep.getAttributes().put(ClientScopeModel.CONSENT_SCREEN_TEXT, "ThirdParty permissions");
        thirdParty.update(thirdPartyRep);

        // Login. Client should be displayed on consent screen
        oauth.clientId("third-party");
        oauth.doLoginGrant("john", "password");

        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT, "ThirdParty permissions");
        grantPage.accept();

        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId,"openid email profile", "third-party");
        IDToken idToken = tokens.idToken;

        assertProfile(idToken, true);
        assertEmail(idToken, true);
        assertAddress(idToken, false);
        assertPhone(idToken, false);

        // Revert
        thirdPartyRep.getAttributes().put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "false");
        thirdParty.update(thirdPartyRep);
    }


    // KEYCLOAK-7855
    @Test
    public void testClientDisplayedOnConsentScreenWithEmptyConsentText() throws Exception {
        // Add "displayOnConsentScreen" to client
        ClientResource thirdParty = ApiUtil.findClientByClientId(testRealm(), "third-party");
        ClientRepresentation thirdPartyRep = thirdParty.toRepresentation();
        thirdPartyRep.getAttributes().put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "true");
        thirdPartyRep.getAttributes().put(ClientScopeModel.CONSENT_SCREEN_TEXT, "");
        thirdParty.update(thirdPartyRep);

        // Change consent text on profile scope
        ClientScopeResource profileScope = ApiUtil.findClientScopeByName(testRealm(), OAuth2Constants.SCOPE_PROFILE);
        ClientScopeRepresentation profileScopeRep = profileScope.toRepresentation();
        profileScopeRep.getAttributes().put(ClientScopeModel.CONSENT_SCREEN_TEXT, " ");
        profileScope.update(profileScopeRep);

        // Login. ConsentTexts are empty for the client and for the "profile" scope, so it should fallback to name/clientId
        oauth.clientId("third-party");
        oauth.doLoginGrant("john", "password");

        grantPage.assertCurrent();
        grantPage.assertGrants("profile", OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT, "third-party");
        grantPage.accept();

        // Revert
        profileScopeRep.getAttributes().put(ClientScopeModel.CONSENT_SCREEN_TEXT, OIDCLoginProtocolFactory.PROFILE_SCOPE_CONSENT_TEXT);
        profileScope.update(profileScopeRep);

        thirdPartyRep.getAttributes().put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "false");
        thirdParty.update(thirdPartyRep);
    }


    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void testRefreshTokenWithConsentRequired() {
        // Login with consentRequired
        oauth.clientId("third-party");
        oauth.doLoginGrant("john", "password");

        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId,"openid email profile", "third-party");
        IDToken idToken = tokens.idToken;
        RefreshToken refreshToken1 = oauth.parseRefreshToken(tokens.refreshToken);

        assertProfile(idToken, true);
        assertEmail(idToken, true);
        assertAddress(idToken, false);
        assertPhone(idToken, false);

        // Ensure that I can refresh token
        OAuthClient.AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(tokens.refreshToken, "password");
        Assert.assertEquals(200, refreshResponse.getStatusCode());
        idToken = oauth.verifyIDToken(refreshResponse.getIdToken());

        assertProfile(idToken, true);
        assertEmail(idToken, true);
        assertAddress(idToken, false);
        assertPhone(idToken, false);

        events.expectRefresh(refreshToken1.getId(), idToken.getSessionState())
                .user(userId)
                .client("third-party")
                .assertEvent();

        // Go to applications in account mgmt and revoke consent
        accountAppsPage.open();
        events.clear();
        accountAppsPage.revokeGrant("third-party");
        events.expect(EventType.REVOKE_GRANT)
                .client("account")
                .user(userId)
                .detail(Details.REVOKED_CLIENT, "third-party")
                .assertEvent();

        // Ensure I can't refresh anymore
        refreshResponse = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken(), "password");
        assertEquals(400, refreshResponse.getStatusCode());
        events.expectRefresh(refreshToken1.getId(), idToken.getSessionState())
                .client("third-party")
                .user(userId)
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_ID)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .error("invalid_token").assertEvent();
    }


    // KEYCLOAK-6170
    @Test
    public void testTwoRefreshTokensWithDifferentScopes() {
        // Add 2 client scopes. Each with scope to 1 realm role
        ClientScopeRepresentation clientScope1 = new ClientScopeRepresentation();
        clientScope1.setName("scope-role-1");
        clientScope1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = testRealm().clientScopes().create(clientScope1);
        String scope1Id = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scope1Id);
        response.close();

        ClientScopeRepresentation clientScope2 = new ClientScopeRepresentation();
        clientScope2.setName("scope-role-2");
        clientScope2.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        response = testRealm().clientScopes().create(clientScope2);
        String scope2Id = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scope2Id);
        response.close();

        RoleRepresentation role1 = testRealm().roles().get("role-1").toRepresentation();
        testRealm().clientScopes().get(scope1Id).getScopeMappings().realmLevel().add(Arrays.asList(role1));

        RoleRepresentation role2 = testRealm().roles().get("role-2").toRepresentation();
        testRealm().clientScopes().get(scope2Id).getScopeMappings().realmLevel().add(Arrays.asList(role2));

        // Add client scopes to our client. Disable fullScopeAllowed
        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testAppRep.setFullScopeAllowed(false);
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scope1Id);
        testApp.addOptionalClientScope(scope2Id);

        // Login with scope-role-1. Save refresh token
        oauth.scope("scope-role-1");
        oauth.doLogin("john", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();

        Tokens tokens1 = sendTokenRequest(loginEvent, userId,"openid email profile scope-role-1", "test-app");
        Assert.assertTrue(tokens1.accessToken.getRealmAccess().isUserInRole("role-1"));
        Assert.assertFalse(tokens1.accessToken.getRealmAccess().isUserInRole("role-2"));

        //SSO login with scope-role-2. Save refresh token
        oauth.scope("scope-role-2");
        oauth.openLoginForm();
        loginEvent = events.expectLogin().user(userId).removeDetail(Details.USERNAME).client("test-app").assertEvent();
        Tokens tokens2 = sendTokenRequest(loginEvent, userId,"openid email profile scope-role-2", "test-app");
        Assert.assertFalse(tokens2.accessToken.getRealmAccess().isUserInRole("role-1"));
        Assert.assertTrue(tokens2.accessToken.getRealmAccess().isUserInRole("role-2"));

        // Ensure I can refresh refreshToken1. Just role1 is present
        OAuthClient.AccessTokenResponse refreshResponse1 = oauth.doRefreshTokenRequest(tokens1.refreshToken, "password");
        Assert.assertEquals(200, refreshResponse1.getStatusCode());
        AccessToken accessToken1 = oauth.verifyToken(refreshResponse1.getAccessToken());
        Assert.assertTrue(accessToken1.getRealmAccess().isUserInRole("role-1"));
        Assert.assertFalse(accessToken1.getRealmAccess().isUserInRole("role-2"));

        // Ensure I can refresh refreshToken2. Just role2 is present
        OAuthClient.AccessTokenResponse refreshResponse2 = oauth.doRefreshTokenRequest(tokens2.refreshToken, "password");
        Assert.assertEquals(200, refreshResponse2.getStatusCode());
        AccessToken accessToken2 = oauth.verifyToken(refreshResponse2.getAccessToken());
        Assert.assertFalse(accessToken2.getRealmAccess().isUserInRole("role-1"));
        Assert.assertTrue(accessToken2.getRealmAccess().isUserInRole("role-2"));

        // Revert
        testAppRep.setFullScopeAllowed(true);
        testApp.update(testAppRep);
        testApp.removeOptionalClientScope(scope1Id);
        testApp.removeOptionalClientScope(scope2Id);
    }


    // Test that clientScope is NOT applied in case that user is not member of any role scoped to the clientScope (including composite roles)
    @Test
    public void testClientScopesPermissions() {
        // Add 2 client scopes. Each with scope to 1 realm role
        ClientScopeRepresentation clientScope1 = new ClientScopeRepresentation();
        clientScope1.setName("scope-role-1");
        clientScope1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = testRealm().clientScopes().create(clientScope1);
        String scope1Id = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scope1Id);
        response.close();

        ClientScopeRepresentation clientScopeParent = new ClientScopeRepresentation();
        clientScopeParent.setName("scope-role-parent");
        clientScopeParent.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        response = testRealm().clientScopes().create(clientScopeParent);
        String scopeParentId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scopeParentId);
        response.close();

        RoleRepresentation role1 = testRealm().roles().get("role-1").toRepresentation();
        testRealm().clientScopes().get(scope1Id).getScopeMappings().realmLevel().add(Arrays.asList(role1));

        RoleRepresentation roleParent = testRealm().roles().get("role-parent").toRepresentation();
        testRealm().clientScopes().get(scopeParentId).getScopeMappings().realmLevel().add(Arrays.asList(roleParent));

        // Add client scopes to our client
        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addDefaultClientScope(scope1Id);
        testApp.addDefaultClientScope(scopeParentId);

        // role-1-user will have clientScope "scope-role-1" and also "scope-role-parent" due the composite role
        testLoginAndClientScopesPermissions("role-1-user", "scope-role-1 scope-role-parent", "role-1");

        // role-2-user won't have any of the "scope-role-1" or "scope-role-parent" applied as he is not member of "role-1" nor "role-parent"
        testLoginAndClientScopesPermissions("role-2-user", "", "role-2");

        // role-parent-user will have clientScope "scope-role-1" (due the composite role) and also "scope-role-parent"
        testLoginAndClientScopesPermissions("role-parent-user", "scope-role-1 scope-role-parent", "role-1", "role-parent");

        // group-role-1-user will have clientScope "scope-role-1" and also "scope-role-parent" due the composite role and due the fact that he is member of group
        testLoginAndClientScopesPermissions("group-role-1-user", "scope-role-1 scope-role-parent", "role-1");


        // Revert
        testApp.removeOptionalClientScope(scope1Id);
        testApp.removeOptionalClientScope(scopeParentId);
    }


    private void testLoginAndClientScopesPermissions(String username, String expectedRoleScopes, String... expectedRoles) {
        String userId = ApiUtil.findUserByUsername(testRealm(), username).getId();

        oauth.openLoginForm();
        oauth.doLogin(username, "password");
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId,"openid email profile " + expectedRoleScopes, "test-app");
        Assert.assertNames(tokens.accessToken.getRealmAccess().getRoles(), expectedRoles);

        oauth.doLogout(tokens.refreshToken, "password");
        events.expectLogout(tokens.idToken.getSessionState())
                .client("test-app")
                .user(userId)
                .removeDetail(Details.REDIRECT_URI).assertEvent();
    }



}
