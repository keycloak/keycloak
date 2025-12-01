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

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.undertow.lb.SimpleUndertowLoadBalancer;
import org.keycloak.testsuite.oidc.AbstractOIDCScopeTest;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.BrowserTabUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.UserManager;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.BasicAuthHelper;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.infinispan.Cache;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Cookie;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.protocol.oidc.OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT;
import static org.keycloak.protocol.oidc.OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN;
import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.Assert.assertExpiration;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getHttpAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RefreshTokenTest extends AbstractKeycloakTest {

    public static final int ALLOWED_CLOCK_SKEW = 3;

    @Page
    protected LoginPage loginPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        realmRepresentation.getClients().add(org.keycloak.testsuite.util.ClientBuilder.create()
                .clientId("service-account-app")
                .serviceAccount()
                .attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "true")
                .secret("secret")
                .build());

        RealmBuilder realm = RealmBuilder.edit(realmRepresentation)
                .testEventListener();

        testRealms.add(realm.build());

    }


    /**
     * KEYCLOAK-547
     *
     */
    @Test
    public void nullRefreshToken() {
        Client client = AdminClientUtil.createResteasyClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget target = client.target(uri);

        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
        assertEquals(400, response.getStatus());
        response.close();
        events.clear();
    }

    @Test
    public void invalidRefreshToken() {
        AccessTokenResponse response = oauth.doRefreshTokenRequest("invalid");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        events.clear();
    }

    @Test
    public void refreshTokenStructure() {
        oauth.loginForm().nonce("123456").doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        assertNull(token.getNonce());

        IDToken idToken = oauth.verifyToken(tokenResponse.getIdToken());
        assertEquals("123456", idToken.getNonce());

        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        assertNotNull(refreshTokenString);

        assertNull(refreshToken.getNonce());
        assertNull("RealmAccess should be null for RefreshTokens", refreshToken.getRealmAccess());
        assertTrue("ResourceAccess should be null for RefreshTokens", refreshToken.getResourceAccess().isEmpty());
    }

    @Test
    public void refreshTokenRequest() {
        oauth.loginForm().nonce("123456").doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        assertNull(token.getNonce());

        IDToken idToken = oauth.verifyToken(tokenResponse.getIdToken(), IDToken.class);
        assertEquals("123456", idToken.getNonce());

        assertNotNull(tokenResponse.getRefreshToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(tokenResponse.getRefreshToken());

        EventRepresentation tokenEvent = events.expectCodeToToken(codeId, sessionId).assertEvent();

        assertEquals("Bearer", tokenResponse.getTokenType());

        assertThat(token.getExp() - getCurrentTime(), allOf(greaterThanOrEqualTo(200L), lessThanOrEqualTo(350L)));
        long actual = refreshToken.getExp() - getCurrentTime();
        assertThat(actual, allOf(greaterThanOrEqualTo(1799L - ALLOWED_CLOCK_SKEW), lessThanOrEqualTo(1800L + ALLOWED_CLOCK_SKEW)));

        assertEquals(sessionId, refreshToken.getSessionId());
        assertNull(refreshToken.getNonce());

        AccessTokenResponse response = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        assertEquals(200, response.getStatusCode());

        assertEquals(sessionId, refreshedToken.getSessionId());
        assertEquals(sessionId, refreshedRefreshToken.getSessionId());

        assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        assertThat(refreshedToken.getExp() - getCurrentTime(), allOf(greaterThanOrEqualTo(250L - ALLOWED_CLOCK_SKEW), lessThanOrEqualTo(300L + ALLOWED_CLOCK_SKEW)));

        assertThat(refreshedToken.getExp() - token.getExp(), allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(10L)));
        assertThat(refreshedRefreshToken.getExp() - refreshToken.getExp(), allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(10L)));

        // "test-app" should not be an audience in the refresh token
        assertEquals("test-app", refreshedRefreshToken.getIssuedFor());
        Assert.assertFalse(refreshedRefreshToken.hasAudience("test-app"));

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("Bearer", response.getTokenType());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), refreshedToken.getSubject());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        assertEquals(1, refreshedToken.getResourceAccess(oauth.getClientId()).getRoles().size());
        assertTrue(refreshedToken.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).assertEvent();
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));

        assertNull(refreshedToken.getNonce());

        idToken =  oauth.verifyToken(response.getIdToken(), IDToken.class);
        assertNull(idToken.getNonce()); // null after refresh as recommended by spec

        assertNotNull(response.getRefreshToken());
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertEquals(sessionId, refreshToken.getSessionId());
        assertNull(refreshToken.getNonce());
    }

    @Test
    public void refreshTokenWithDifferentIssuer() {
        final String proxyHost = "localhost";
        final int httpPort = 8666;
        final int httpsPort = 8667;

        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String refreshTokenString = response.getRefreshToken();

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        SimpleUndertowLoadBalancer proxy = new SimpleUndertowLoadBalancer(proxyHost, httpPort, httpsPort, "node1=" + getHttpAuthServerContextRoot() + "/auth");
        proxy.start();

        try {
            oauth.baseUrl(String.format("http://%s:%s", proxyHost, httpPort));

            response = oauth.doRefreshTokenRequest(refreshTokenString);

            Assert.assertEquals(400, response.getStatusCode());
            Assert.assertEquals("invalid_grant", response.getError());
            assertThat(response.getErrorDescription(), Matchers.startsWith("Invalid token issuer."));
            events.expect(EventType.REFRESH_TOKEN).error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
        } finally {
            proxy.stop();
            oauth.baseUrl(AUTH_SERVER_ROOT);
        }
    }


    @Test
    public void refreshingTokenLoadsSessionIntoCache() {

        ProfileAssume.assumeFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS);

        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String refreshTokenString = response.getRefreshToken();

        // Test when neither client nor user session is in the cache
        testingClient.server().run(session -> {
            session.getProvider(InfinispanConnectionProvider.class).getCache(USER_SESSION_CACHE_NAME).clear();
            session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME).clear();
        });

        response = oauth.doRefreshTokenRequest(refreshTokenString);
        Assert.assertEquals(200, response.getStatusCode());

        testingClient.server().run(session -> {
            assertThat(session.getProvider(InfinispanConnectionProvider.class).getCache(USER_SESSION_CACHE_NAME).size(),
                    greaterThan(0));
            assertThat(session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME).size(),
                    greaterThan(0));
        });

        // Test is only the client session is missing
        testingClient.server().run(session -> session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME).clear());

        response = oauth.doRefreshTokenRequest(refreshTokenString);
        Assert.assertEquals(200, response.getStatusCode());

        testingClient.server().run(session -> {
            assertThat(session.getProvider(InfinispanConnectionProvider.class).getCache(USER_SESSION_CACHE_NAME).size(),
                    greaterThan(0));
            assertThat(session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME).size(),
                    greaterThan(0));
        });

    }

    @Test
    public void refreshTokenWithAccessToken() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String accessTokenString = tokenResponse.getAccessToken();

        AccessTokenResponse response = oauth.doRefreshTokenRequest(accessTokenString);
        Assert.assertNotEquals(200, response.getStatusCode());
    }

    @Test
    public void testDoNotResolveOfflineUserSessionIfAuthenticationSessionIsInvalidated() {
        oauth.scope("offline_access");
        testDoNotResolveUserSessionIfAuthenticationSessionIsInvalidated();
    }

    @Test
    public void testDoNotResolveUserSessionIfAuthenticationSessionIsInvalidated() {
        String realmName = KeycloakModelUtils.generateId();
        RealmsResource realmsResource = realmsResouce();
        realmsResource.create(RealmBuilder.create().name(realmName).build());
        RealmResource realmResource = realmsResource.realm(realmName);
        RealmRepresentation realm = realmResource.toRepresentation();

        try {
            realm.setSsoSessionMaxLifespan((int) TimeUnit.MINUTES.toSeconds(2));
            realm.setSsoSessionIdleTimeout((int) TimeUnit.MINUTES.toSeconds(2));
            realm.setAccessTokenLifespan((int) TimeUnit.MINUTES.toSeconds(1));
            realmResource.update(realm);

            realmResource.clients().create(org.keycloak.testsuite.util.ClientBuilder.create()
                    .clientId("public-client")
                    .redirectUris("*")
                    .publicClient()
                    .build()).close();

            realmResource.users()
                    .create(UserBuilder.create().username("alice")
                            .firstName("alice")
                            .lastName("alice")
                            .email("alice@keycloak.org")
                            .password("alice").addRoles("offline_access").build()).close();
            realmResource.users()
                    .create(UserBuilder.create().username("bob")
                            .firstName("bob")
                            .lastName("bob")
                            .email("bob@keycloak.org")
                            .password("bob").addRoles("offline_access").build()).close();

            oauth.realm(realmName);
            oauth.client("public-client");

            oauth.doLogin("alice", "alice");
            String aliceCode = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(aliceCode);
            AccessToken aliceAt = oauth.verifyToken(tokenResponse.getAccessToken());

            setTimeOffset((int) TimeUnit.MINUTES.toSeconds(2));

            oauth.doLogin("bob", "bob");
            String bobCode = oauth.parseLoginResponse().getCode();

            assertNotEquals(aliceCode, bobCode);

            tokenResponse = oauth.doAccessTokenRequest(bobCode);
            String refreshToken = tokenResponse.getRefreshToken();
            tokenResponse = oauth.doRefreshTokenRequest(refreshToken);
            AccessToken bobAt = oauth.verifyToken(tokenResponse.getAccessToken());

            assertNotEquals(aliceAt.getSessionId(), bobAt.getSessionId());
            assertEquals("bob", bobAt.getPreferredUsername());
        } finally {
            setTimeOffset(0);

            realm.setSsoSessionMaxLifespan(null);
            realm.setSsoSessionIdleTimeout(null);
            realm.setAccessTokenLifespan(null);
            realmResource.update(realm);
        }
    }

    @Test
    public void testTimeoutWhenReUsingPreviousAuthenticationSession() {
        String realmName = KeycloakModelUtils.generateId();
        RealmsResource realmsResource = realmsResouce();
        realmsResource.create(RealmBuilder.create().name(realmName).build());
        RealmResource realmResource = realmsResource.realm(realmName);
        RealmRepresentation realm = realmResource.toRepresentation();

        try {
            realm.setSsoSessionMaxLifespan((int) TimeUnit.MINUTES.toSeconds(2));
            realm.setSsoSessionIdleTimeout((int) TimeUnit.MINUTES.toSeconds(2));
            realm.setAccessTokenLifespan((int) TimeUnit.MINUTES.toSeconds(1));
            realmResource.update(realm);

            realmResource.clients().create(org.keycloak.testsuite.util.ClientBuilder.create()
                    .clientId("public-client")
                    .redirectUris("*")
                    .publicClient()
                    .build()).close();

            realmResource.users()
                    .create(UserBuilder.create().username("alice").password("alice").addRoles("offline_access").build()).close();
            realmResource.users()
                    .create(UserBuilder.create().username("bob").password("bob").addRoles("offline_access").build()).close();

            oauth.realm(realmName);
            oauth.client("public-client");

            oauth.openLoginForm();

            Cookie authSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());

            oauth.fillLoginForm("alice", "alice");

            oauth.parseLoginResponse().getCode();
//            WebClient webClient = DroneHtmlUnitDriver.class.cast(driver).getWebClient();
//            webClient.getCookieManager().clearCookies();
            driver.manage().deleteAllCookies();
            oauth.openLoginForm();
            driver.manage().addCookie(authSessionCookie);
            oauth.fillLoginForm("bob", "bob");
            assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
        } finally {
            setTimeOffset(0);

            realm.setSsoSessionMaxLifespan(null);
            realm.setSsoSessionIdleTimeout(null);
            realm.setAccessTokenLifespan(null);
            realmResource.update(realm);
        }
    }

    /**
     * KEYCLOAK-15437
     */
    @Test
    public void tokenRefreshWithAccessTokenShouldReturnIdTokenWithAccessTokenHash() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String refreshToken = tokenResponse.getRefreshToken();

        AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshToken);
        Assert.assertEquals(200, response.getStatusCode());
        IDToken idToken = oauth.verifyToken(response.getIdToken());
        Assert.assertNotNull("AccessTokenHash should not be null after token refresh", idToken.getAccessTokenHash());
    }

    @Test
    public void refreshTokenReuseTokenWithoutRefreshTokensRevoked() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response1 = oauth.doAccessTokenRequest(code);
        RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
        assertEquals(200, response2.getStatusCode());

        events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();

        AccessTokenResponse response3 = oauth.doRefreshTokenRequest(response1.getRefreshToken());

        assertEquals(200, response3.getStatusCode());

        events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();
    }


    @Test
    public void refreshTokenReuseTokenWithoutRefreshTokensRevokedWithLessScopes() {
        //add phone,address as optional scope and request them
        ClientScopeRepresentation phoneScope = adminClient.realm("test").clientScopes().findAll().stream().filter((ClientScopeRepresentation clientScope) ->"phone".equals(clientScope.getName())).findFirst().get();
        ClientScopeRepresentation addressScope = adminClient.realm("test").clientScopes().findAll().stream().filter((ClientScopeRepresentation clientScope) ->"address".equals(clientScope.getName())).findFirst().get();
        ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).addClientScope(phoneScope.getId(),false);
        ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).addClientScope(addressScope.getId(),false);

        try {
            oauth.doLogin("test-user@localhost", "password");

            oauth.parseLoginResponse().getCode();

            String optionalScope = "phone address";
            oauth.scope(optionalScope);
            AccessTokenResponse response1 = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());
            AbstractOIDCScopeTest.assertScopes("openid basic email roles web-origins acr profile address phone",  refreshToken1.getScope());

            setTimeOffset(2);

            String scope = "email phone";
            oauth.scope(scope);
            AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            assertEquals(200, response2.getStatusCode());
            AbstractOIDCScopeTest.assertScopes("openid email phone profile",  response2.getScope());
            RefreshToken refreshToken2 = oauth.parseRefreshToken(response2.getRefreshToken());
            assertNotNull(refreshToken2);
            AbstractOIDCScopeTest.assertScopes("openid acr roles phone address email profile basic web-origins",  refreshToken2.getScope());

        } finally {
            setTimeOffset(0);
            oauth.scope(null);
        }
    }

    @Test
    public void refreshTokenReuseTokenScopeParameterNotInRefreshToken() {
        try {
            //scope parameter consists scope that is not part of scope refresh token => error thrown
            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response1 = oauth.doAccessTokenRequest(code);
            RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());
            AbstractOIDCScopeTest.assertScopes("openid basic email roles web-origins acr profile",  refreshToken1.getScope());

            setTimeOffset(2);

            String scope = "openid email ssh_public_key";
            oauth.scope(scope);
            AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            assertEquals(400, response2.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_SCOPE, response2.getError());

        } finally {
            setTimeOffset(0);
            oauth.scope(null);
        }
    }

    @Test
    public void refreshWithOptionalClientScopeWithIncludeInTokenScopeDisabled() {
        //set roles client scope as optional
        ClientScopeRepresentation rolesScope = ApiUtil.findClientScopeByName(adminClient.realm("test"), OIDCLoginProtocolFactory.ROLES_SCOPE).toRepresentation();
        ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).removeClientScope(rolesScope.getId(),true);
        ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).addClientScope(rolesScope.getId(),false);

        try {
            oauth.scope("roles");
            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

            AbstractOIDCScopeTest.assertScopes("openid email profile",  accessToken.getScope());
            AbstractOIDCScopeTest.assertScopes("openid basic email roles web-origins acr profile",  refreshToken.getScope());

            Assert.assertNotNull(accessToken.getRealmAccess());
            Assert.assertNotNull(accessToken.getResourceAccess());

            oauth.scope(null);

            response = oauth.doRefreshTokenRequest(response.getRefreshToken());

            accessToken = oauth.verifyToken(response.getAccessToken());
            refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

            AbstractOIDCScopeTest.assertScopes("openid email profile",  accessToken.getScope());
            AbstractOIDCScopeTest.assertScopes("openid basic email roles web-origins acr profile",  refreshToken.getScope());

            Assert.assertNotNull(accessToken.getRealmAccess());
            Assert.assertNotNull(accessToken.getResourceAccess());

        } finally {
            ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).removeClientScope(rolesScope.getId(),false);
            ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).addClientScope(rolesScope.getId(),true);
        }
    }

    @Test
    public void refreshTokenReuseTokenWithRefreshTokensRevoked() {
        try {

            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response1 = oauth.doAccessTokenRequest(code);
            RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            RefreshToken refreshToken2 = oauth.parseRefreshToken(response2.getRefreshToken());

            assertEquals(200, response2.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();

            AccessTokenResponse response3 = oauth.doRefreshTokenRequest(response1.getRefreshToken());

            assertEquals(400, response3.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).user((String) null).removeDetail(Details.TOKEN_ID).removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            // Client session invalidated hence old refresh token not valid anymore
            AccessTokenResponse response4 = oauth.doRefreshTokenRequest(response2.getRefreshToken());
            assertEquals(400, response4.getStatusCode());
            events.expectRefresh(refreshToken2.getId(), sessionId).user((String) null).removeDetail(Details.TOKEN_ID).removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();
        } finally {
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);
        }
    }

    @Test
    public void refreshTokenReuseOnDifferentTab() {
        try {

            BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver);
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true);

            //login with tab 1
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response1 = oauth.doAccessTokenRequest(code);
            RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());
            events.expectCodeToToken(codeId, sessionId).assertEvent();
            assertNotNull(refreshToken1.getOtherClaims().get(Constants.REUSE_ID));
            assertNotEquals(refreshToken1.getOtherClaims().get(Constants.REUSE_ID), refreshToken1.getId());

            //login with tab 2
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));

            loginEvent = events.expectLogin().assertEvent();
            sessionId = loginEvent.getSessionId();
            codeId = loginEvent.getDetails().get(Details.CODE_ID);
            code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse responseNew = oauth.doAccessTokenRequest(code);
            RefreshToken refreshTokenNew = oauth.parseRefreshToken(responseNew.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();
            assertNotNull(refreshToken1.getOtherClaims().get(Constants.REUSE_ID));
            assertNotEquals(refreshTokenNew.getOtherClaims().get(Constants.REUSE_ID), refreshTokenNew.getId());
            assertNotEquals(refreshToken1.getOtherClaims().get(Constants.REUSE_ID), refreshTokenNew.getOtherClaims().get(Constants.REUSE_ID));

            setTimeOffset(10);

            //refresh with token from tab 1
            AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            assertEquals(200, response2.getStatusCode());
            RefreshToken refreshToken2 = oauth.parseRefreshToken(response2.getRefreshToken());
            events.expectRefresh(refreshToken2.getId(), sessionId);
            assertNotEquals(refreshToken2.getOtherClaims().get(Constants.REUSE_ID), refreshToken2.getId());
            assertEquals(refreshToken1.getOtherClaims().get(Constants.REUSE_ID), refreshToken2.getOtherClaims().get(Constants.REUSE_ID));

            //refresh with token from tab 2
            AccessTokenResponse responseNew1 = oauth.doRefreshTokenRequest(responseNew.getRefreshToken());
            assertEquals(200, responseNew1.getStatusCode());
            RefreshToken refreshTokenNew1 = oauth.parseRefreshToken(responseNew1.getRefreshToken());
            events.expectRefresh(refreshTokenNew1.getId(), sessionId);
            assertNotEquals(refreshTokenNew1.getOtherClaims().get(Constants.REUSE_ID), refreshTokenNew1.getId());
            assertEquals(refreshTokenNew.getOtherClaims().get(Constants.REUSE_ID), refreshTokenNew1.getOtherClaims().get(Constants.REUSE_ID));

            //try refresh token reuse with token from tab 2
            responseNew1 = oauth.doRefreshTokenRequest(responseNew.getRefreshToken());
            assertEquals(400, responseNew1.getStatusCode());


        } finally {
            resetTimeOffset();
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);
        }
    }

    @Test
    public void refreshTokenReuseTokenWithRefreshTokensRevokedAfterSingleReuse() {
        try {
            RealmManager.realm(adminClient.realm("test"))
                    .revokeRefreshToken(true)
                    .refreshTokenMaxReuse(1);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse initialResponse = oauth.doAccessTokenRequest(code);
            RefreshToken initialRefreshToken = oauth.parseRefreshToken(initialResponse.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            // Initial refresh.
            AccessTokenResponse responseFirstUse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());
            RefreshToken newTokenFirstUse = oauth.parseRefreshToken(responseFirstUse.getRefreshToken());

            assertEquals(200, responseFirstUse.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).assertEvent();

            // Second refresh (allowed).
            AccessTokenResponse responseFirstReuse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());
            RefreshToken newTokenFirstReuse = oauth.parseRefreshToken(responseFirstReuse.getRefreshToken());
            String userId = newTokenFirstReuse.getSubject();

            assertEquals(200, responseFirstReuse.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).detail(Details.REFRESH_TOKEN_SUB, userId).assertEvent();

            // Token reused twice, became invalid.
            AccessTokenResponse responseSecondReuse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());

            assertEquals(400, responseSecondReuse.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).user((String) null).detail(Details.REFRESH_TOKEN_SUB, userId).removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            // Refresh token from first use became invalid.
            AccessTokenResponse responseUseOfInvalidatedRefreshToken =
                    oauth.doRefreshTokenRequest(responseFirstUse.getRefreshToken());

            assertEquals(400, responseUseOfInvalidatedRefreshToken.getStatusCode());

            events.expectRefresh(newTokenFirstUse.getId(), sessionId).user((String) null).removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            // Refresh token from reuse is not valid. Client session was invalidated
            AccessTokenResponse responseUseOfValidRefreshToken =
                    oauth.doRefreshTokenRequest(responseFirstReuse.getRefreshToken());

            assertEquals(400, responseUseOfValidRefreshToken.getStatusCode());

            events.expectRefresh(newTokenFirstReuse.getId(), sessionId).user((String) null).removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();
        } finally {
            RealmManager.realm(adminClient.realm("test"))
                    .refreshTokenMaxReuse(0)
                    .revokeRefreshToken(false);
        }
    }

    @Test
    public void refreshTokenReuseOfExistingTokenAfterEnablingReuseRevokation() {
        try {
            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse initialResponse = oauth.doAccessTokenRequest(code);
            RefreshToken initialRefreshToken = oauth.parseRefreshToken(initialResponse.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            // Infinite reuse allowed
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());

            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true).refreshTokenMaxReuse(1);

            // Config changed, we start tracking reuse.
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());

            AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());

            assertEquals(400, responseReuseExceeded.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).user((String) null).removeDetail(Details.TOKEN_ID).removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();
        } finally {
            RealmManager.realm(adminClient.realm("test"))
                    .refreshTokenMaxReuse(0)
                    .revokeRefreshToken(false);
        }
    }

    @Test
    public void refreshTokenReuseOfExistingTokenAfterDisablingReuseRevokation() {
        try {
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true).refreshTokenMaxReuse(1);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse initialResponse = oauth.doAccessTokenRequest(code);
            RefreshToken initialRefreshToken = oauth.parseRefreshToken(initialResponse.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            // Single reuse authorized.
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());

            AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());

            assertEquals(400, responseReuseExceeded.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).user((String) null).removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);

            // Config changed, token cannot be used again at this point due the client session invalidated
            AccessTokenResponse responseReuseExceeded2 = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());
            assertEquals(400, responseReuseExceeded2.getStatusCode());
            events.expectRefresh(initialRefreshToken.getId(), sessionId).user((String) null).removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();
        } finally {
            RealmManager.realm(adminClient.realm("test"))
                    .refreshTokenMaxReuse(0)
                    .revokeRefreshToken(false);
        }
    }

    // Doublecheck that with "revokeRefreshToken" and revoked tokens, the SSO re-authentication won't cause old tokens to be valid again
    @Test
    public void refreshTokenReuseTokenWithRefreshTokensRevokedAndSSOReauthentication() throws Exception {
        try {
            // Initial login
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response1 = oauth.doAccessTokenRequest(code);
            RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            // Refresh token for the first time - should pass

            AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            RefreshToken refreshToken2 = oauth.parseRefreshToken(response2.getRefreshToken());

            assertEquals(200, response2.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();

            // Client sessions is available now
            Assert.assertTrue(hasClientSessionForTestApp());

            // Refresh token for the second time - should fail and invalidate client session

            AccessTokenResponse response3 = oauth.doRefreshTokenRequest(response1.getRefreshToken());

            assertEquals(400, response3.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).removeDetail(Details.TOKEN_ID).user((String) null).removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            // No client sessions available after revoke
            Assert.assertFalse(hasClientSessionForTestApp());

            // Introspection with the accessToken from the first authentication. This should fail
            JsonNode jsonNode = oauth.doIntrospectionAccessTokenRequest(response1.getAccessToken()).asJsonNode();
            Assert.assertFalse(jsonNode.get("active").asBoolean());
            events.clear();

            // SSO re-authentication
            setTimeOffset(2);
            oauth.openLoginForm();

            loginEvent = events.expectLogin().assertEvent();
            sessionId = loginEvent.getSessionId();
            codeId = loginEvent.getDetails().get(Details.CODE_ID);
            code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response4 = oauth.doAccessTokenRequest(code);
            oauth.parseRefreshToken(response4.getRefreshToken());
            events.expectCodeToToken(codeId, sessionId).assertEvent();

            // Client sessions should be available again now after re-authentication
            Assert.assertTrue(hasClientSessionForTestApp());

            // Introspection again with the accessToken from the very first authentication. This should fail as the access token was obtained for the old client session before SSO re-authentication
            jsonNode = oauth.doIntrospectionAccessTokenRequest(response1.getAccessToken()).asJsonNode();
            Assert.assertFalse(jsonNode.get("active").asBoolean());

            // Try userInfo with the same old access token. Should fail as well
//            UserInfo userInfo = oauth.doUserInfoRequest(response1.getAccessToken());
            Client client = AdminClientUtil.createResteasyClient();
            Response userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, response1.getAccessToken());
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), userInfoResponse.getStatus());
            String wwwAuthHeader = userInfoResponse.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            events.clear();

            // Try to refresh with one of the old refresh tokens before SSO re-authentication - should fail
            AccessTokenResponse response5 = oauth.doRefreshTokenRequest(response2.getRefreshToken());
            assertEquals(400, response5.getStatusCode());
            events.expectRefresh(refreshToken2.getId(), sessionId).user((String) null).removeDetail(Details.TOKEN_ID).removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();
        } finally {
            resetTimeOffset();
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);
        }
    }

    // Returns true if "test-user@localhost" has any user session with client session for "test-app"
    private boolean hasClientSessionForTestApp() {
        List<UserSessionRepresentation> userSessions = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "test-user@localhost").getUserSessions();
        return userSessions.stream()
                .anyMatch(userSession -> userSession.getClients().containsValue("test-app"));
    }

    private void processExpectedValidRefresh(String sessionId, RefreshToken requestToken, String refreshToken) {
        AccessTokenResponse response2 = oauth.doRefreshTokenRequest(refreshToken);

        assertEquals(200, response2.getStatusCode());

        events.expectRefresh(requestToken.getId(), sessionId).assertEvent();
    }


    @Test
    public void refreshTokenClientDisabled() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        try {
            ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).enabled(false);

            response = oauth.doRefreshTokenRequest(refreshTokenString);

            assertEquals(401, response.getStatusCode());
            assertEquals("invalid_client", response.getError());

            events.expectRefresh(refreshToken.getId(), sessionId).user((String) null).session((String) null).clearDetails().error(Errors.CLIENT_DISABLED).assertEvent();
        } finally {
            ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).enabled(true);
        }
    }

    @Test
    public void refreshTokenUserSessionRemoved() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        events.poll();

        String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();

        testingClient.testing().removeUserSession("test", sessionId);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        events.clear();
    }

    @Test
    public void refreshTokenAfterUserLogoutAndLoginAgain() {
        String refreshToken1 = loginAndForceNewLoginPage();

        oauth.doLogout(refreshToken1);
        events.clear();

        try {
            // Continue with login
            setTimeOffset(2);
            driver.navigate().refresh();
            oauth.fillLoginForm("test-user@localhost", "password");

            assertFalse(loginPage.isCurrent());

            AccessTokenResponse tokenResponse2;
            String code = oauth.parseLoginResponse().getCode();
            tokenResponse2 = oauth.doAccessTokenRequest(code);

            // Now try refresh with the original refreshToken1 created in logged-out userSession. It should fail
            AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(refreshToken1);
            assertEquals(400, responseReuseExceeded.getStatusCode());

            // Finally try with valid refresh token
            responseReuseExceeded = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken());
            assertEquals(200, responseReuseExceeded.getStatusCode());
        } finally {
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenAfterAdminLogoutAllAndLoginAgain() {
        String refreshToken1 = loginAndForceNewLoginPage();

        adminClient.realm("test").logoutAll();
        // Must wait for server to execute the request. Sometimes, there is issue with the execution and another tests failed, because of this.
        WaitUtils.pause(500);

        events.clear();

        try {
            // Continue with login
            setTimeOffset(2);
            driver.navigate().refresh();
            oauth.fillLoginForm("test-user@localhost", "password");

            assertFalse(loginPage.isCurrent());

            AccessTokenResponse tokenResponse2;
            String code = oauth.parseLoginResponse().getCode();
            tokenResponse2 = oauth.doAccessTokenRequest(code);

            // Now try refresh with the original refreshToken1 created in logged-out userSession. It should fail
            AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(refreshToken1);
            assertEquals(400, responseReuseExceeded.getStatusCode());

            // Finally try with valid refresh token
            responseReuseExceeded = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken());
            assertEquals(200, responseReuseExceeded.getStatusCode());
        } finally {
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenAfterUserAdminLogoutEndpointAndLoginAgain() {
        try {
            String refreshToken1 = loginAndForceNewLoginPage();

            RefreshToken refreshTokenParsed1 = oauth.parseRefreshToken(refreshToken1);
            String userId = refreshTokenParsed1.getSubject();
            UserResource user = adminClient.realm("test").users().get(userId);
            user.logout();

            // Continue with login
            setTimeOffset(2);
            driver.navigate().refresh();
            oauth.fillLoginForm("test-user@localhost", "password");

            assertFalse(loginPage.isCurrent());

            AccessTokenResponse tokenResponse2;
            String code = oauth.parseLoginResponse().getCode();
            tokenResponse2 = oauth.doAccessTokenRequest(code);

            // Now try refresh with the original refreshToken1 created in logged-out userSession. It should fail
            AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(refreshToken1);
            assertEquals(400, responseReuseExceeded.getStatusCode());

            // Finally try with valid refresh token
            responseReuseExceeded = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken());
            assertEquals(200, responseReuseExceeded.getStatusCode());
        } finally {
            resetTimeOffset();
            // Need to reset not-before of user, which was updated during user.logout()
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername(realm, "test-user@localhost");
                session.users().setNotBeforeForUser(realm, user, 0);
            });
        }
    }

    @Test
    public void testUserSessionRefreshAndIdle() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        events.poll();

        String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();

        int last = testingClient.testing().getLastSessionRefresh("test", sessionId, false);

        setTimeOffset(2);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

        oauth.verifyToken(tokenResponse.getAccessToken());
        oauth.parseRefreshToken(tokenResponse.getRefreshToken());

        assertEquals(200, tokenResponse.getStatusCode());

        int next = testingClient.testing().getLastSessionRefresh("test", sessionId, false);

        Assert.assertNotEquals(last, next);

        RealmResource realmResource = adminClient.realm("test");
        int lastAccessTokenLifespan = realmResource.toRepresentation().getAccessTokenLifespan();
        int originalIdle = realmResource.toRepresentation().getSsoSessionIdleTimeout();

        try {
            RealmManager.realm(realmResource).accessTokenLifespan(100000);

            setTimeOffset(4);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            next = testingClient.testing().getLastSessionRefresh("test", sessionId, false);

            // lastSEssionRefresh should be updated because access code lifespan is higher than sso idle timeout
            assertThat(next, allOf(greaterThan(last), lessThan(last + 50)));

            RealmManager.realm(realmResource).ssoSessionIdleTimeout(1);

            events.clear();
            // Needs to add some additional time due the tollerance allowed by IDLE_TIMEOUT_WINDOW_SECONDS
            setTimeOffset(6 + (ProfileAssume.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            // test idle timeout
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        } finally {
            RealmManager.realm(realmResource).ssoSessionIdleTimeout(originalIdle).accessTokenLifespan(lastAccessTokenLifespan);
            events.clear();
            resetTimeOffset();
        }

    }

    @Test
    public void testUserSessionRefreshAndIdleRememberMe() throws Exception {
        RealmResource testRealm = adminClient.realm("test");

        try (Closeable ignored = new RealmAttributeUpdater(testRealm)
                .updateWith(r -> {
                    r.setRememberMe(true);
                    r.setSsoSessionIdleTimeoutRememberMe(500);
                    r.setSsoSessionIdleTimeout(100);
                }).update()) {
            oauth.openLoginForm();
            loginPage.setRememberMe(true);
            loginPage.login("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            events.poll();

            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            int last = testingClient.testing().getLastSessionRefresh("test", sessionId, false);

            setTimeOffset(110 + (ProfileAssume.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            oauth.verifyToken(tokenResponse.getAccessToken());
            oauth.parseRefreshToken(tokenResponse.getRefreshToken());
            assertEquals(200, tokenResponse.getStatusCode());

            int next = testingClient.testing().getLastSessionRefresh("test", sessionId, false);
            Assert.assertNotEquals(last, next);

            events.clear();
            // Needs to add some additional time due the tollerance allowed by IDLE_TIMEOUT_WINDOW_SECONDS
            setTimeOffset(620 + 2 * (ProfileAssume.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            // test idle remember me timeout
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);
            events.clear();

        } finally {
            resetTimeOffset();
        }
    }

    private String getClientSessionUuid(final String userSessionId, String clientId) {
        return testingClient.server().fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName("test");
            ClientModel clientModel = realmModel.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());
            return clientSession.getId();
        }, String.class);
    }

    private int checkIfUserAndClientSessionExist(final String userSessionId, final String clientId, final String clientSessionId) {
        return testingClient.server().fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName("test");
            ClientModel clientModel = realmModel.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            if (userSession != null) {
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());
                return clientSession != null && clientSessionId.equals(clientSession.getId())? 2 : 1;
            }
            return 0;
        }, Integer.class);
    }

    @Test
    public void refreshTokenUserSessionMaxLifespan() throws Exception {
        RealmResource realmResource = adminClient.realm("test");
        getTestingClient().testing().setTestingInfinispanTimeService();
        try (Closeable ignored = new RealmAttributeUpdater(realmResource)
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(3600);
                    r.setSsoSessionIdleTimeout(7200);
                }).update()) {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3600);
            final String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            setTimeOffset(1800);

            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1800);
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
            events.expectRefresh(refreshId, sessionId).assertEvent();

            setTimeOffset(3700);
            oauth.parseRefreshToken(tokenResponse.getRefreshToken());
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenUserClientMaxLifespanSmallerThanSession() throws Exception {
        RealmResource realmResource = adminClient.realm("test");
        getTestingClient().testing().setTestingInfinispanTimeService();
        try (Closeable ignored = new RealmAttributeUpdater(realmResource)
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(3600);
                    r.setSsoSessionIdleTimeout(7200);
                    r.setClientSessionMaxLifespan(1000);
                    r.setClientSessionIdleTimeout(7200);
                }).update()) {

            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1000);
            String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            setTimeOffset(600);
            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 400);
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
            events.expectRefresh(refreshId, sessionId).assertEvent();

            setTimeOffset(1100);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
            assertEquals(1, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            setTimeOffset(1600);
            oauth.openLoginForm();
            loginEvent = events.expectLogin().assertEvent();
            sessionId = loginEvent.getSessionId();
            code = oauth.parseLoginResponse().getCode();
            tokenResponse = oauth.doAccessTokenRequest(code);
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1000);
            events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), sessionId).assertEvent();

            clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            setTimeOffset(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenUserClientMaxLifespanGreaterThanSession() throws Exception {
        RealmResource realmResource = adminClient.realm("test");
        getTestingClient().testing().setTestingInfinispanTimeService();
        try (Closeable ignored = new RealmAttributeUpdater(realmResource)
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(3600);
                    r.setSsoSessionIdleTimeout(7200);
                    r.setClientSessionMaxLifespan(5000);
                    r.setClientSessionIdleTimeout(7200);
                }).update()) {

            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3600);
            String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            setTimeOffset(1800);
            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1800);
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
            events.expectRefresh(refreshId, sessionId).assertEvent();

            setTimeOffset(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenUserSessionMaxLifespanModifiedAfterTokenRefresh() throws Exception {
        RealmResource realmResource = adminClient.realm("test");
        getTestingClient().testing().setTestingInfinispanTimeService();

        try (Closeable ignored = new RealmAttributeUpdater(realmResource)
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(7200);
                    r.setSsoSessionIdleTimeout(7200);
                    r.setClientSessionMaxLifespan(7200);
                    r.setClientSessionIdleTimeout(7200);
                }).update()) {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200);
            final String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            RealmRepresentation rep = realmResource.toRepresentation();
            rep.setSsoSessionMaxLifespan(3600);
            realmResource.update(rep);

            setTimeOffset(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.assertRefreshTokenErrorAndMaybeSessionExpired(sessionId, loginEvent.getUserId(), loginEvent.getClientId());
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenClientSessionMaxLifespanModifiedAfterTokenRefresh() throws Exception {
        RealmResource realmResource = adminClient.realm("test");
        getTestingClient().testing().setTestingInfinispanTimeService();

        try (Closeable ignored = new RealmAttributeUpdater(realmResource)
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(7200);
                    r.setSsoSessionIdleTimeout(7200);
                    r.setClientSessionMaxLifespan(7200);
                    r.setClientSessionIdleTimeout(7200);
                }).update()) {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(200, tokenResponse.getStatusCode());
            assertTrue("Invalid ExpiresIn: " + tokenResponse.getRefreshExpiresIn(), 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200);
            String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            RealmRepresentation rep = realmResource.toRepresentation();
            rep.setClientSessionMaxLifespan(3600);
            realmResource.update(rep);

            setTimeOffset(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).error(Errors.INVALID_TOKEN).session(sessionId).user((String) null).assertEvent();
            assertEquals(1, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            setTimeOffset(4200);
            oauth.openLoginForm();
            loginEvent = events.expectLogin().assertEvent();
            sessionId = loginEvent.getSessionId();
            code = oauth.parseLoginResponse().getCode();
            tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(200, tokenResponse.getStatusCode());
            assertTrue("Invalid ExpiresIn: " + tokenResponse.getRefreshExpiresIn(), 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3000);
            events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), sessionId).assertEvent();

            clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            setTimeOffset(7300);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void silentLoginClientSessionMaxLifespanModifiedAfterTokenRefresh() throws Exception {
        RealmResource realmResource = adminClient.realm("test");
        getTestingClient().testing().setTestingInfinispanTimeService();

        try (Closeable ignored = new RealmAttributeUpdater(realmResource)
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(7200);
                    r.setSsoSessionIdleTimeout(7200);
                    r.setClientSessionMaxLifespan(7200);
                    r.setClientSessionIdleTimeout(7200);
                }).update()) {

            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200);
            String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            RealmRepresentation rep = realmResource.toRepresentation();
            rep.setClientSessionMaxLifespan(3600);
            realmResource.update(rep);

            setTimeOffset(4200);
            oauth.openLoginForm();
            loginEvent = events.expectLogin().assertEvent();
            sessionId = loginEvent.getSessionId();
            code = oauth.parseLoginResponse().getCode();
            tokenResponse = oauth.doAccessTokenRequest(code);
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3000);
            events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), sessionId).assertEvent();

            clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            setTimeOffset(7300);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    /**
     * KEYCLOAK-1267
     * @throws Exception
     */
    @Test
    public void refreshTokenUserSessionMaxLifespanWithRememberMe() throws Exception {

        RealmResource testRealm = adminClient.realm("test");

        try (Closeable ignored = new RealmAttributeUpdater(testRealm)
                .updateWith(r -> {
                    r.setRememberMe(true);
                    r.setSsoSessionMaxLifespanRememberMe(100);
                    r.setSsoSessionMaxLifespan(50);
                }).update()) {

            oauth.openLoginForm();
            loginPage.setRememberMe(true);
            loginPage.login("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            events.poll();

            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();

            setTimeOffset(110);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);
            events.clear();

        } finally {
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenClientSessionMaxLifespan() {
        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();
        Integer originalSsoSessionMaxLifespan = rep.getSsoSessionMaxLifespan();

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        getTestingClient().testing().setTestingInfinispanTimeService();

        try {
            rep.setSsoSessionMaxLifespan(1000);
            realm.update(rep);

            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN, "500");
            client.update(clientRepresentation);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();
            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            events.poll();

            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertTrue("Invalid RefreshExpiresIn" + tokenResponse.getRefreshExpiresIn(), 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 500);

            setTimeOffset(100);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertTrue("Invalid RefreshExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 400);

            setTimeOffset(600);

            oauth.openLoginForm();
            code = oauth.parseLoginResponse().getCode();

            tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(200, tokenResponse.getStatusCode());
            assertTrue("Invalid RefreshExpiresIn" + tokenResponse.getRefreshExpiresIn(), 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 400);

            setTimeOffset(700);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(200, tokenResponse.getStatusCode());
            assertTrue("Invalid RefreshExpiresIn" + tokenResponse.getRefreshExpiresIn(), 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 300);

            setTimeOffset(1100);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);
        } finally {
            rep.setSsoSessionMaxLifespan(originalSsoSessionMaxLifespan);
            realm.update(rep);
            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN, null);
            client.update(clientRepresentation);

            events.clear();
            resetTimeOffset();
            getTestingClient().testing().revertTestingInfinispanTimeService();
        }
    }

    /**
     * This is a very esoteric test specific to bug <a href="https://github.com/keycloak/keycloak/issues/38591">#38591</a>.
     * Consider removing or rewriting the test if the loading of sessions from the database has changed and no longer
     * updates the client session timestamp. It is also specific to the case when the idle timeout of a client is reduced
     * while some client sessions already exist.
     */
    @Test
    public void refreshTokenClientSessionIdleTimeoutTwoClientsWithReloadingFromDatabase() {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS);

        RealmResource realm = adminClient.realm("test");

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        // Duplicate the primary client to have two clients to test with
        ClientRepresentation clientRepresentation2 = client.toRepresentation();
        clientRepresentation2.setClientId("test-app2");
        clientRepresentation2.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, "500");
        clientRepresentation2.setId(null);
        try (Response resp = realm.clients().create(clientRepresentation2)) {
            String clientUUID = ApiUtil.getCreatedId(resp);
            getCleanup().addClientUuid(clientUUID);
        }

        getTestingClient().testing().setTestingInfinispanTimeService();

        try {
            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();
            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            // Reduce the idle time so that the originally issued refresh token is valid, but it will be considered invalid due to the client configuration
            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, "500");
            client.update(clientRepresentation);

            oauth.client("test-app2", "password");

            // We are already logged in due to the token
            oauth.openLoginForm();

            String code2 = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(code2);

            assertThat(sessionId, Matchers.equalTo(tokenResponse2.getSessionState()));

            setTimeOffset(100);

            tokenResponse2 = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken());
            assertEquals(200, tokenResponse2.getStatusCode());
            assertTrue("Invalid RefreshExpiresIn: " + tokenResponse2.getRefreshExpiresIn(), 0 < tokenResponse2.getRefreshExpiresIn() && tokenResponse2.getRefreshExpiresIn() <= 500);

            // Clear all entries from the cache to enforce re-loading the data from the database
            testingClient.server("test").run(session -> {
                InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                if (connections != null) {
                    Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache = connections.getCache(USER_SESSION_CACHE_NAME);
                    Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = connections.getCache(CLIENT_SESSION_CACHE_NAME);
                    if (sessionCache != null) {
                        sessionCache.clear();
                    }
                    if (clientSessionCache != null) {
                        clientSessionCache.clear();
                    }
                }
            });

            setTimeOffset(550);
            oauth.client("test-app", "password");
            events.poll();

            // The client session of the first client should have expired by now
            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        } finally {
            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, null);
            client.update(clientRepresentation);

            events.clear();
            resetTimeOffset();
            getTestingClient().testing().revertTestingInfinispanTimeService();
        }
    }

    @Test
    public void testCheckSsl() {
        try (Client client = AdminClientUtil.createResteasyClient()) {
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);
            builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget refreshTarget = client.target(uri);

            String refreshToken;
            {
                Response response = executeGrantAccessTokenRequest(grantTarget);
                assertEquals(200, response.getStatus());
                org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
                refreshToken = tokenResponse.getRefreshToken();
                response.close();
            }

            {
                Response response = executeRefreshToken(refreshTarget, refreshToken);
                assertEquals(200, response.getStatus());
                org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
                refreshToken = tokenResponse.getRefreshToken();
                response.close();
            }

            if (!AUTH_SERVER_SSL_REQUIRED) {   // test checkSsl
                RealmResource realmResource = adminClient.realm("test");
                {
                    RealmManager.realm(realmResource).sslRequired(SslRequired.ALL.toString());
                }

                Response response = executeRefreshToken(refreshTarget, refreshToken);
                assertEquals(403, response.getStatus());
                response.close();

                {
                    RealmManager.realm(realmResource).sslRequired(SslRequired.EXTERNAL.toString());
                }
            }

            Response response = executeRefreshToken(refreshTarget, refreshToken);
            assertEquals(200, response.getStatus());
            response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            response.close();
        } finally {
            events.clear();
        }

    }

    @Test
    public void refreshTokenUserDisabled() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        try {
            UserManager.realm(adminClient.realm("test")).username("test-user@localhost").enabled(false);
            response = oauth.doRefreshTokenRequest(refreshTokenString);
            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_grant", response.getError());

            events.expectRefresh(refreshToken.getId(), sessionId).user((String) null).clearDetails().error(Errors.INVALID_TOKEN).assertEvent();
        } finally {
            UserManager.realm(adminClient.realm("test")).username("test-user@localhost").enabled(true);
        }
    }

    @Test
    public void refreshTokenUserDeleted() {
        String userId = createUser("test", "temp-user@localhost", "password");
        oauth.doLogin("temp-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().user(userId).assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).user(userId).assertEvent();

        adminClient.realm("test").users().delete(userId).close();

        response = oauth.doRefreshTokenRequest(refreshTokenString);
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(refreshToken.getId(), sessionId).user((String) null).clearDetails().error(Errors.INVALID_TOKEN).assertEvent();
    }

    @Test
    public void refreshTokenServiceAccount() {
        AccessTokenResponse response = oauth.client("service-account-app", "secret").doClientCredentialsGrantAccessTokenRequest();

        assertNotNull(response.getRefreshToken());

        response = oauth.doRefreshTokenRequest(response.getRefreshToken());

        assertNotNull(response.getRefreshToken());
    }

    @Test
    public void testClientSessionMaxLifespan() {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();
        Integer originalSsoSessionMaxLifespan = rep.getSsoSessionMaxLifespan();
        int ssoSessionMaxLifespan = rep.getSsoSessionIdleTimeout() - 100;
        Integer originalClientSessionMaxLifespan = rep.getClientSessionMaxLifespan();

        try {
            rep.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);
            realm.update(rep);

            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), ssoSessionMaxLifespan);

            rep.setClientSessionMaxLifespan(ssoSessionMaxLifespan - 100);
            realm.update(rep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), ssoSessionMaxLifespan - 100);

            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN,
                    Integer.toString(ssoSessionMaxLifespan - 200));
            client.update(clientRepresentation);

            refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), ssoSessionMaxLifespan - 200);
        } finally {
            rep.setSsoSessionMaxLifespan(originalSsoSessionMaxLifespan);
            rep.setClientSessionMaxLifespan(originalClientSessionMaxLifespan);
            realm.update(rep);
            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN, null);
            client.update(clientRepresentation);
        }
    }

    @Test
    public void testClientSessionIdleTimeout() {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();
        int ssoSessionIdleTimeout = rep.getSsoSessionIdleTimeout();
        Integer originalClientSessionIdleTimeout = rep.getClientSessionIdleTimeout();

        try {
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), ssoSessionIdleTimeout);

            rep.setClientSessionIdleTimeout(ssoSessionIdleTimeout - 100);
            realm.update(rep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), ssoSessionIdleTimeout - 100);

            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT,
                    Integer.toString(ssoSessionIdleTimeout - 200));
            client.update(clientRepresentation);

            refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), ssoSessionIdleTimeout - 200);
        } finally {
            rep.setClientSessionIdleTimeout(originalClientSessionIdleTimeout);
            realm.update(rep);
            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, null);
            client.update(clientRepresentation);
        }
    }

    @Test // KEYCLOAK-17323
    public void testRefreshTokenWhenClientSessionTimeoutPassedButRealmDidNot() {
        //noinspection resource
        getCleanup()
                .addCleanup(new RealmAttributeUpdater(adminClient.realm("test"))
                        .setSsoSessionIdleTimeout(2592000) // 30 Days
                        .setSsoSessionMaxLifespan(86313600) // 999 Days
                        .update()
                )
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, "test", "test-app")
                        .setAttribute(CLIENT_SESSION_IDLE_TIMEOUT, "60") // 1 minute
                        .setAttribute(CLIENT_SESSION_MAX_LIFESPAN, "65") // 1 minute 5 seconds
                        .update()
                );

        getTestingClient().testing().setTestingInfinispanTimeService();
        try {
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getExpiresIn(), 65);

            setTimeOffset(70);

            oauth.openLoginForm();
            code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response2 = oauth.doAccessTokenRequest(code);
            assertExpiration(response2.getExpiresIn(), 65);
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenRequestNoRefreshToken() {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        String refreshTokenString = tokenResponse.getRefreshToken();

        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);
        AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshTokenString);

        assertNotNull(response.getAccessToken());
        assertNull(response.getRefreshToken());

        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "true");
        client.update(clientRepresentation);
    }

    @Test
    public void tokenRefreshRequest_ClientRS384_RealmRS384() throws Exception {
        conductTokenRefreshRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.RS384, Algorithm.RS384);
    }

    @Test
    public void tokenRefreshRequest_ClientRS512_RealmRS256() throws Exception {
        conductTokenRefreshRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.RS512, Algorithm.RS256);
    }

    @Test
    public void tokenRefreshRequest_ClientES256_RealmRS256() throws Exception {
        conductTokenRefreshRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.ES256, Algorithm.RS256);
    }

    @Test
    public void tokenRefreshRequest_ClientES384_RealmES384() throws Exception {
        conductTokenRefreshRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.ES384, Algorithm.ES384);
    }

    @Test
    public void tokenRefreshRequest_ClientES512_RealmRS256() throws Exception {
        conductTokenRefreshRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.ES512, Algorithm.RS256);
    }

    @Test
    public void tokenRefreshRequest_ClientPS256_RealmRS256() throws Exception {
        conductTokenRefreshRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.PS256, Algorithm.RS256);
    }

    @Test
    public void tokenRefreshRequest_ClientPS384_RealmES384() throws Exception {
        conductTokenRefreshRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.PS384, Algorithm.ES384);
    }

    @Test
    public void tokenRefreshRequest_ClientPS512_RealmPS256() throws Exception {
        conductTokenRefreshRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.PS512, Algorithm.PS256);
    }

    protected Response executeRefreshToken(WebTarget refreshTarget, String refreshToken) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
        form.param("refresh_token", refreshToken);
        return refreshTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }

    protected Response executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "test-user@localhost")
                .param("password", "password");
        return grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }

    private void conductTokenRefreshRequest(String                                                                                                                              expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        try {
            // Realm setting is used for ID Token signature algorithm
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, expectedIdTokenAlg);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), expectedAccessAlg);
            refreshToken(expectedRefreshAlg, expectedAccessAlg, expectedIdTokenAlg);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), Algorithm.RS256);
        }
    }

    private void refreshToken(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        JWSHeader header = new JWSInput(tokenResponse.getAccessToken()).getHeader();
        assertEquals(expectedAccessAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        header = new JWSInput(tokenResponse.getIdToken()).getHeader();
        assertEquals(expectedIdTokenAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        header = new JWSInput(tokenResponse.getRefreshToken()).getHeader();
        assertEquals(expectedRefreshAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        EventRepresentation tokenEvent = events.expectCodeToToken(codeId, sessionId).assertEvent();

        assertNotNull(refreshTokenString);

        assertEquals("Bearer", tokenResponse.getTokenType());

        assertEquals(sessionId, refreshToken.getSessionId());

        AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshTokenString);
        if (response.getError() != null || response.getErrorDescription() != null) {
            log.debugf("Refresh token error: %s, error description: %s", response.getError(), response.getErrorDescription());
        }

        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        assertEquals(200, response.getStatusCode());

        assertEquals(sessionId, refreshedToken.getSessionId());
        assertEquals(sessionId, refreshedRefreshToken.getSessionId());

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("Bearer", response.getTokenType());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), refreshedToken.getSubject());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        EventRepresentation refreshEvent = events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).assertEvent();
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));
    }

    private String loginAndForceNewLoginPage() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        events.poll();

        // Assert refresh successful
        String refreshToken = tokenResponse.getRefreshToken();
        RefreshToken refreshTokenParsed1 = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        processExpectedValidRefresh(sessionId, refreshTokenParsed1, refreshToken);

        // Open the tab with prompt=login. AuthenticationSession will be created with same ID like userSession
        String loginFormUri = oauth.loginForm()
                .param(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                .build();
        driver.navigate().to(loginFormUri);

        loginPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());

        return refreshToken;
    }
}
