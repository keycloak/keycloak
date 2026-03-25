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
package org.keycloak.tests.oauth;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.tests.utils.Assert;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.events.Errors.INVALID_REQUEST;
import static org.keycloak.tests.oauth.RefreshTokenTimeoutsTest.isPersistentSessionsFeatureEnabled;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class RefreshTokenTest {

    public static final int ALLOWED_CLOCK_SKEW = 3;

    private static final Logger log = Logger.getLogger(RefreshTokenTest.class);

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectEvents
    Events events;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectPage
    LoginPage loginPage;

    @InjectRealm(config = RefreshTokenTestRealmConfig.class)
    protected ManagedRealm realm;

    @InjectUser(config = TestRealmUserConfig.class)
    protected ManagedUser user;

    @InjectClient(attachTo = "test-app")
    ManagedClient managedClient;

    public static class RefreshTokenTestRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("service-account-app")
                    .serviceAccountsEnabled(true)
                    .attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "true")
                    .secret("secret");
            realm.addRole("user");
            return realm;
        }
    }

    @BeforeEach
    public void before() {
        enableRefreshTokenEvents(realm);
        AccountHelper.logout(realm.admin(), user.getUsername());
    }

    public static void enableRefreshTokenEvents(ManagedRealm realm) {
        RealmEventsConfigRepresentation realmEventsConfig = realm.admin().getRealmEventsConfig();
        List<String> enabledEventTypes = realmEventsConfig.getEnabledEventTypes();
        if (!enabledEventTypes.contains(EventType.REFRESH_TOKEN.name())) {
            enabledEventTypes.addAll(List.of(EventType.REFRESH_TOKEN.name(), EventType.REFRESH_TOKEN_ERROR.name()));
            realm.admin().updateRealmEventsConfig(realmEventsConfig);
        }
    }

    /**
     * KEYCLOAK-547
     *
     */
    @Test
    public void nullRefreshToken() {
        class RefreshRequestWithoutRefreshTokenParameter extends AbstractHttpPostRequest<RefreshRequestWithoutRefreshTokenParameter, AccessTokenResponse> {

            RefreshRequestWithoutRefreshTokenParameter(AbstractOAuthClient<?> client) {
                super(client);
            }

            @Override
            protected String getEndpoint() {
                return client.getEndpoints().getToken();
            }

            protected void initRequest() {
                parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
                scope(false);
            }

            @Override
            protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
                return new AccessTokenResponse(response);
            }

        }
        AccessTokenResponse response = new RefreshRequestWithoutRefreshTokenParameter(oauth).send();
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
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

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        assertNull(token.getNonce());

        IDToken idToken = oauth.verifyToken(tokenResponse.getIdToken());
        assertEquals("123456", idToken.getNonce());

        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.CODE_TO_TOKEN);

        assertNotNull(refreshTokenString);

        assertNull(refreshToken.getNonce());
        assertNull(refreshToken.getRealmAccess(), "RealmAccess should be null for RefreshTokens");
        assertTrue(refreshToken.getResourceAccess().isEmpty(), "ResourceAccess should be null for RefreshTokens");
    }

    @Test
    public void refreshTokenRequest() {
        RoleRepresentation userRole = this.realm.admin().roles().get("user").toRepresentation();
        this.user.admin().roles().realmLevel().add(List.of(userRole));

        oauth.loginForm().nonce("123456").doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .userId(user.getId())
                .clientId("test-app")
                .hasSessionId()
                .type(EventType.LOGIN);

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        assertNull(token.getNonce());

        IDToken idToken = oauth.verifyToken(tokenResponse.getIdToken(), IDToken.class);
        assertEquals("123456", idToken.getNonce());

        assertNotNull(tokenResponse.getRefreshToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(tokenResponse.getRefreshToken());

        EventRepresentation tokenEvent = events.poll();
        EventAssertion.assertSuccess(tokenEvent)
                .userId(user.getId())
                .sessionId(sessionId)
                .isCodeId()
                .clientId("test-app")
                .type(EventType.CODE_TO_TOKEN);

        assertEquals("Bearer", tokenResponse.getTokenType());

        assertThat(token.getExp() - Time.currentTime(), allOf(greaterThanOrEqualTo(200L), lessThanOrEqualTo(350L)));
        long actual = refreshToken.getExp() - Time.currentTime();
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
        assertThat(refreshedToken.getExp() - Time.currentTime(), allOf(greaterThanOrEqualTo(250L - ALLOWED_CLOCK_SKEW), lessThanOrEqualTo(300L + ALLOWED_CLOCK_SKEW)));

        assertThat(refreshedToken.getExp() - token.getExp(), allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(10L)));
        assertThat(refreshedRefreshToken.getExp() - refreshToken.getExp(), allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(10L)));

        // "test-app" should not be an audience in the refresh token
        assertEquals("test-app", refreshedRefreshToken.getIssuedFor());
        assertFalse(refreshedRefreshToken.hasAudience("test-app"));

        assertNotEquals(token.getId(), refreshedToken.getId());
        assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("Bearer", response.getTokenType());

        Assert.assertEquals(user.getId(), refreshedToken.getSubject());
        assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        assertTrue(refreshedToken.getResourceAccess(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).isUserInRole(AccountRoles.MANAGE_ACCOUNT));

        EventRepresentation refreshEvent = events.poll();
        EventAssertion.assertSuccess(refreshEvent)
                .userId(user.getId())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .clientId("test-app")
                .type(EventType.REFRESH_TOKEN);
        assertNotEquals(tokenEvent.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        assertNotEquals(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));

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
        oauth.doLogin("test-user@localhost", "password");

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String refreshTokenString = response.getRefreshToken();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.CODE_TO_TOKEN);

        String invalidIssuerRefreshToken = encodeRefreshToken(refreshTokenString);
        response = oauth.doRefreshTokenRequest(invalidIssuerRefreshToken);

        Assert.assertEquals(400, response.getStatusCode());
        Assert.assertEquals("invalid_grant", response.getError());
        assertThat(response.getErrorDescription(), Matchers.startsWith("Invalid token issuer."));
        EventAssertion.assertError(events.poll())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error(Errors.INVALID_TOKEN);
    }

    private String encodeRefreshToken(String encodedRefreshToken) {
        return runOnServer.fetchString(session -> {
            try {
                JWSInput input = new JWSInput(encodedRefreshToken);
                RefreshToken refreshToken = input.readJsonContent(RefreshToken.class);

                refreshToken.issuer("https://fake-issuer");
                return session.tokens().encode(refreshToken);
            } catch (JWSInputException ioe) {
                throw new RuntimeException("Failed to encode token: " + encodedRefreshToken);
            }
        });
    }


    @Test
    public void refreshingTokenLoadsSessionIntoCache() {
        Assumptions.assumeTrue(isPersistentSessionsFeatureEnabled(adminClient), "Skip as persistent_user_sessions feature is disabled");

        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String refreshTokenString = response.getRefreshToken();

        // Test when neither client nor user session is in the cache
        runOnServer.run(session -> {
            session.getProvider(InfinispanConnectionProvider.class).getCache(USER_SESSION_CACHE_NAME).clear();
            session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME).clear();
        });

        response = oauth.doRefreshTokenRequest(refreshTokenString);
        Assert.assertEquals(200, response.getStatusCode());

        runOnServer.run(session -> {
            MatcherAssert.assertThat(session.getProvider(InfinispanConnectionProvider.class).getCache(USER_SESSION_CACHE_NAME).size(),
                    greaterThan(0));
            MatcherAssert.assertThat(session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME).size(),
                    greaterThan(0));
        });

        // Test is only the client session is missing
        runOnServer.run(session -> session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME).clear());

        response = oauth.doRefreshTokenRequest(refreshTokenString);
        Assert.assertEquals(200, response.getStatusCode());

        runOnServer.run(session -> {
            MatcherAssert.assertThat(session.getProvider(InfinispanConnectionProvider.class).getCache(USER_SESSION_CACHE_NAME).size(),
                    greaterThan(0));
            MatcherAssert.assertThat(session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME).size(),
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
        try {
            testDoNotResolveUserSessionIfAuthenticationSessionIsInvalidated();
        } finally {
            oauth.scope(null);
        }
    }

    @Test
    public void testDoNotResolveUserSessionIfAuthenticationSessionIsInvalidated() {
        String realmName = KeycloakModelUtils.generateId();
        RealmsResource realmsResource = adminClient.realms();
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);
        realmsResource.create(realm);
        RealmResource realmResource = realmsResource.realm(realmName);
        realm = realmResource.toRepresentation();

        String origRealm = oauth.getRealm();
        String origClientId = oauth.getClientId();
        String origClientSecret = oauth.config().getClientSecret();

        try {
            realm.setSsoSessionMaxLifespan((int) TimeUnit.MINUTES.toSeconds(2));
            realm.setSsoSessionIdleTimeout((int) TimeUnit.MINUTES.toSeconds(2));
            realm.setAccessTokenLifespan((int) TimeUnit.MINUTES.toSeconds(1));
            realmResource.update(realm);

            realmResource.clients().create(ClientConfigBuilder.create()
                    .clientId("public-client")
                    .redirectUris("*")
                    .publicClient(true)
                    .build()).close();

            realmResource.users()
                    .create(UserConfigBuilder.create().username("alice")
                            .firstName("alice")
                            .lastName("alice")
                            .email("alice@keycloak.org")
                            .password("alice").roles("offline_access").build()).close();
            realmResource.users()
                    .create(UserConfigBuilder.create().username("bob")
                            .firstName("bob")
                            .lastName("bob")
                            .email("bob@keycloak.org")
                            .password("bob").roles("offline_access").build()).close();

            oauth.realm(realmName);
            oauth.client("public-client");

            oauth.doLogin("alice", "alice");
            String aliceCode = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(aliceCode);
            AccessToken aliceAt = oauth.verifyToken(tokenResponse.getAccessToken());

            timeOffSet.set((int) TimeUnit.MINUTES.toSeconds(2));

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
            realmResource.remove();
            oauth.realm(origRealm);
            oauth.client(origClientId, origClientSecret);
        }
    }

    @Test
    public void testTimeoutWhenReUsingPreviousAuthenticationSession() {
        String realmName = KeycloakModelUtils.generateId();
        RealmsResource realmsResource = adminClient.realms();
        realmsResource.create(RealmConfigBuilder.create().name(realmName).build());
        RealmResource realmResource = realmsResource.realm(realmName);
        RealmRepresentation realm = realmResource.toRepresentation();

        String origRealm = oauth.getRealm();
        String origClientId = oauth.getClientId();
        String origClientSecret = oauth.config().getClientSecret();

        try {
            realm.setSsoSessionMaxLifespan((int) TimeUnit.MINUTES.toSeconds(2));
            realm.setSsoSessionIdleTimeout((int) TimeUnit.MINUTES.toSeconds(2));
            realm.setAccessTokenLifespan((int) TimeUnit.MINUTES.toSeconds(1));
            realmResource.update(realm);

            realmResource.clients().create(ClientConfigBuilder.create()
                    .clientId("public-client")
                    .redirectUris("*")
                    .publicClient(true)
                    .build()).close();

            realmResource.users()
                    .create(UserConfigBuilder.create().username("alice")
                            .firstName("alice")
                            .lastName("alice")
                            .email("alice@keycloak.org")
                            .password("alice").roles("offline_access").build()).close();
            realmResource.users()
                    .create(UserConfigBuilder.create().username("bob")
                            .firstName("bob")
                            .lastName("bob")
                            .email("bob@keycloak.org")
                            .password("bob").roles("offline_access").build()).close();

            oauth.realm(realmName);
            oauth.client("public-client");

            oauth.openLoginForm();

            Cookie authSessionCookie = driver.cookies().get(CookieType.AUTH_SESSION_ID.getName());

            oauth.fillLoginForm("alice", "alice");

            oauth.parseLoginResponse().getCode();
            driver.cookies().deleteAll();

            // Enforce login page to be able to delete cookies here (as appPage is on different domain)
            oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
            driver.cookies().deleteAll();

            oauth.openLoginForm();
            driver.cookies().add(authSessionCookie);
            oauth.fillLoginForm("bob", "bob");
            Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
        } finally {
            realmResource.remove();
            oauth.realm(origRealm);
            oauth.client(origClientId, origClientSecret);
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
        Assert.assertNotNull(idToken.getAccessTokenHash(), "AccessTokenHash should not be null after token refresh");
    }

    public static void assertScopes(String expectedScope, String receivedScope) {
        Collection<String> expectedScopes = Arrays.stream(expectedScope.split(" ")).sorted().toList();
        Collection<String> receivedScopes = Arrays.stream(receivedScope.split(" ")).sorted().toList();
        assertTrue(expectedScopes.containsAll(receivedScopes) && receivedScopes.containsAll(expectedScopes),
                "Not matched. Expected scopes: " + expectedScopes + ", Received scopes: " + receivedScopes);
    }

    @Test
    public void refreshTokenReuseTokenWithoutRefreshTokensRevokedWithLessScopes() {
        //add phone,address as optional scope and request them
        ClientScopeRepresentation phoneScope = findClientScopeByName("phone");
        ClientScopeRepresentation addressScope = findClientScopeByName("address");

        managedClient.admin().addOptionalClientScope(phoneScope.getId());
        managedClient.admin().addOptionalClientScope(addressScope.getId());

        try {
            oauth.doLogin("test-user@localhost", "password");

            oauth.parseLoginResponse().getCode();

            String optionalScope = "phone address";
            oauth.scope(optionalScope);
            AccessTokenResponse response1 = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());
            assertScopes("openid basic email roles service_account web-origins acr profile address phone",  refreshToken1.getScope());

            timeOffSet.set(2);

            String scope = "email phone";
            oauth.scope(scope);
            AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            assertEquals(200, response2.getStatusCode());
            assertScopes("openid email phone profile",  response2.getScope());
            RefreshToken refreshToken2 = oauth.parseRefreshToken(response2.getRefreshToken());
            assertNotNull(refreshToken2);
            assertScopes("openid acr roles phone address email profile basic service_account web-origins",  refreshToken2.getScope());

        } finally {
            oauth.scope(null);
            managedClient.admin().removeOptionalClientScope(phoneScope.getId());
            managedClient.admin().removeOptionalClientScope(addressScope.getId());
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
            assertScopes("openid basic email roles service_account web-origins acr profile",  refreshToken1.getScope());

            timeOffSet.set(2);

            String scope = "openid email ssh_public_key";
            oauth.scope(scope);
            AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            assertEquals(400, response2.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_SCOPE, response2.getError());

        } finally {
            oauth.scope(null);
        }
    }

    private ClientScopeRepresentation findClientScopeByName(String name) {
        return realm.admin().clientScopes().findAll().stream()
                .filter((ClientScopeRepresentation clientScope) -> name.equals(clientScope.getName()))
                .findFirst().get();
    }

    @Test
    public void refreshWithOptionalClientScopeWithIncludeInTokenScopeDisabled() {
        //set roles client scope as optional
        ClientScopeRepresentation rolesScope = findClientScopeByName(OIDCLoginProtocolFactory.ROLES_SCOPE);
        managedClient.admin().removeDefaultClientScope(rolesScope.getId());
        managedClient.admin().addOptionalClientScope(rolesScope.getId());

        try {
            oauth.scope("roles");
            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

            assertScopes("openid email profile",  accessToken.getScope());
            assertScopes("openid basic email roles service_account web-origins acr profile",  refreshToken.getScope());

            Assert.assertNotNull(accessToken.getRealmAccess());
            Assert.assertNotNull(accessToken.getResourceAccess());

            oauth.scope(null);

            response = oauth.doRefreshTokenRequest(response.getRefreshToken());

            accessToken = oauth.verifyToken(response.getAccessToken());
            refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

            assertScopes("openid email profile",  accessToken.getScope());
            assertScopes("openid basic email roles service_account web-origins acr profile",  refreshToken.getScope());

            Assert.assertNotNull(accessToken.getRealmAccess());
            Assert.assertNotNull(accessToken.getResourceAccess());

        } finally {
            managedClient.admin().removeOptionalClientScope(rolesScope.getId());
            managedClient.admin().addDefaultClientScope(rolesScope.getId());
        }
    }

    private void processExpectedValidRefresh(String sessionId, RefreshToken requestToken, String refreshToken) {
        AccessTokenResponse response2 = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, response2.getStatusCode());
        EventAssertion.assertSuccess(events.poll())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, requestToken.getId())
                .type(EventType.REFRESH_TOKEN);
    }


    @Test
    public void refreshTokenClientDisabled() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String refreshTokenString = response.getRefreshToken();
        events.clear();

        managedClient.updateWithCleanup(c -> c.enabled(false));

        response = oauth.doRefreshTokenRequest(refreshTokenString);

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_client", response.getError());

        EventAssertion.assertError(events.poll())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error(Errors.CLIENT_DISABLED);
        managedClient.updateWithCleanup(c -> c.enabled(true));
    }

    @Test
    public void refreshTokenUserSessionRemoved() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String sessionId = tokenResponse.getSessionState();

        events.clear();

        realm.admin().deleteSession(sessionId, false);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        EventAssertion.assertError(events.poll())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error(Errors.INVALID_TOKEN);

        events.clear();
    }

    @Test
    public void refreshTokenAfterUserLogoutAndLoginAgain() {
        String refreshToken1 = loginAndForceNewLoginPage();

        oauth.doLogout(refreshToken1);
        events.clear();


        // Continue with login
        timeOffSet.set(2);
        driver.navigate().refresh();
        oauth.fillLoginForm("test-user@localhost", "password");

        AccessTokenResponse tokenResponse2;
        String code = oauth.parseLoginResponse().getCode();
        tokenResponse2 = oauth.doAccessTokenRequest(code);

        // Now try refresh with the original refreshToken1 created in logged-out userSession. It should fail
        AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(refreshToken1);
        assertEquals(400, responseReuseExceeded.getStatusCode());

        // Finally try with valid refresh token
        responseReuseExceeded = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken());
        assertEquals(200, responseReuseExceeded.getStatusCode());
    }

    @Test
    public void refreshTokenAfterAdminLogoutAllAndLoginAgain() {
        String refreshToken1 = loginAndForceNewLoginPage();

        realm.admin().logoutAll();
        events.clear();

        // Continue with login
        timeOffSet.set(2);
        driver.navigate().refresh();
        oauth.fillLoginForm("test-user@localhost", "password");

        AccessTokenResponse tokenResponse2;
        String code = oauth.parseLoginResponse().getCode();
        tokenResponse2 = oauth.doAccessTokenRequest(code);

        // Now try refresh with the original refreshToken1 created in logged-out userSession. It should fail
        AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(refreshToken1);
        assertEquals(400, responseReuseExceeded.getStatusCode());

        // Finally try with valid refresh token
        responseReuseExceeded = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken());
        assertEquals(200, responseReuseExceeded.getStatusCode());
    }

    @Test
    public void refreshTokenAfterUserAdminLogoutEndpointAndLoginAgain() {
        String refreshToken1 = loginAndForceNewLoginPage();
        user.admin().logout();

        try {
            // Continue with login
            timeOffSet.set(2);
            driver.navigate().refresh();
            oauth.fillLoginForm("test-user@localhost", "password");

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
            // Need to reset not-before of user, which was updated during user.logout()
            UserRepresentation userRep = user.admin().toRepresentation();
            userRep.setNotBefore(0);
            user.admin().update(userRep);
        }
    }


    @Test
    public void testCheckSsl() {
        try {
            AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            String refreshToken = tokenResponse.getRefreshToken();
            assertNotNull(refreshToken);

            if (!oauth.getEndpoints().getIssuer().startsWith("https://")) {   // test checkSsl
                RealmRepresentation realmRep = realm.admin().toRepresentation();
                String origSslRequired = realmRep.getSslRequired();
                realmRep.setSslRequired(SslRequired.ALL.toString());
                realm.admin().update(realmRep);

                try {
                    AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshToken);
                    assertEquals(403, response.getStatusCode());
                    assertEquals(INVALID_REQUEST, response.getError());
                    assertEquals("HTTPS required", response.getErrorDescription());
                } finally {
                    realmRep.setSslRequired(origSslRequired);
                    realm.admin().update(realmRep);
                }
            }

            AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getRefreshToken());
        } finally {
            events.clear();
        }

    }

    @Test
    public void refreshTokenUserDisabled() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String sessionId = response.getSessionState();

        String refreshTokenString = response.getRefreshToken();
        assertNotNull(refreshTokenString);
        events.clear();

        UserRepresentation userRep = user.admin().toRepresentation();

        try {
            userRep.setEnabled(false);
            user.admin().update(userRep);

            response = oauth.doRefreshTokenRequest(refreshTokenString);
            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_grant", response.getError());

            EventAssertion.assertError(events.poll())
                    .type(EventType.REFRESH_TOKEN_ERROR)
                    .sessionId(sessionId)
                    .error(Errors.INVALID_TOKEN);
        } finally {
            userRep.setEnabled(true);
            user.admin().update(userRep);
        }
    }

    @Test
    public void refreshTokenUserDeleted() {
        UserConfigBuilder newUser = UserConfigBuilder.create()
                .username("temp-user@localhost")
                .password("password")
                .name("First", "Last")
                .email("temp-user@localhost")
                .enabled(true);
        UserRepresentation rep = newUser.build();
        String userId = ApiUtil.getCreatedId(realm.admin().users().create(rep));

        oauth.doLogin("temp-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        String sessionId = response.getSessionState();

        String refreshTokenString = response.getRefreshToken();
        assertNotNull(refreshTokenString);
        events.clear();

        realm.admin().users().delete(userId).close();

        response = oauth.doRefreshTokenRequest(refreshTokenString);
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        EventAssertion.assertError(events.poll())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .userId(null)
                .sessionId(sessionId)
                .error(Errors.INVALID_TOKEN);
    }

    @Test
    public void refreshTokenServiceAccount() {
        String origClientId = oauth.config().getClientId();
        String origClientSecret = oauth.config().getClientSecret();
        try {
            AccessTokenResponse response = oauth.client("service-account-app", "secret").doClientCredentialsGrantAccessTokenRequest();
            assertNotNull(response.getRefreshToken());
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertNotNull(response.getRefreshToken());
        } finally {
            oauth.client(origClientId, origClientSecret);
        }
    }

    @Test
    public void refreshTokenRequestNoRefreshToken() {
        ClientRepresentation client = managedClient.admin().toRepresentation();
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        String refreshTokenString = tokenResponse.getRefreshToken();

        client.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        managedClient.admin().update(client);

        try {

            AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshTokenString);

            assertNotNull(response.getAccessToken());
            assertNull(response.getRefreshToken());
        } finally {
            client.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "true");
            managedClient.admin().update(client);
        }
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

    private void conductTokenRefreshRequest(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        try {
            // Realm setting is used for ID Token signature algorithm
            changeRealmTokenSignatureProvider(expectedIdTokenAlg);
            changeClientAccessTokenSignatureProvider(expectedAccessAlg);
            refreshToken(expectedRefreshAlg, expectedAccessAlg, expectedIdTokenAlg);
        } finally {
            changeRealmTokenSignatureProvider(Constants.DEFAULT_SIGNATURE_ALGORITHM);
            changeClientAccessTokenSignatureProvider(Constants.DEFAULT_SIGNATURE_ALGORITHM);
        }
    }

    private void changeRealmTokenSignatureProvider(String toSigAlgName) {
        RealmRepresentation rep = realm.admin().toRepresentation();
        log.tracef("change realm test signature algorithm from %s to %s", rep.getDefaultSignatureAlgorithm(), toSigAlgName);
        rep.setDefaultSignatureAlgorithm(toSigAlgName);
        realm.admin().update(rep);
    }

    private void changeClientAccessTokenSignatureProvider(String toSigAlgName) {
        ClientRepresentation clientRep = managedClient.admin().toRepresentation();
        log.tracef("change client %s access token signature algorithm from %s to %s", clientRep.getClientId(), clientRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG), toSigAlgName);
        clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, toSigAlgName);
        managedClient.admin().update(clientRep);
    }

    private void refreshToken(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .userId(user.getId())
                .clientId("test-app")
                .hasSessionId()
                .type(EventType.LOGIN);

        String sessionId = loginEvent.getSessionId();

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

        EventRepresentation tokenEvent = events.poll();
        EventAssertion.assertSuccess(tokenEvent)
                .userId(user.getId())
                .sessionId(sessionId)
                .isCodeId()
                .clientId("test-app")
                .type(EventType.CODE_TO_TOKEN);

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

        Assert.assertEquals(user.getId(), refreshedToken.getSubject());

        EventRepresentation refreshEvent = events.poll();
        EventAssertion.assertSuccess(refreshEvent)
                .userId(user.getId())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .details(Details.UPDATED_REFRESH_TOKEN_ID, refreshedRefreshToken.getId())
                .clientId("test-app")
                .type(EventType.REFRESH_TOKEN);
    }

    private String loginAndForceNewLoginPage() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .userId(user.getId())
                .clientId("test-app")
                .hasSessionId()
                .type(EventType.LOGIN);

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.CODE_TO_TOKEN);

        // Assert refresh successful
        String refreshToken = tokenResponse.getRefreshToken();
        RefreshToken refreshTokenParsed1 = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        processExpectedValidRefresh(sessionId, refreshTokenParsed1, refreshToken);

        // Open the tab with prompt=login. AuthenticationSession will be created with same ID like userSession
        oauth.loginForm()
                .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                .open();

        loginPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());

        return refreshToken;
    }
}
