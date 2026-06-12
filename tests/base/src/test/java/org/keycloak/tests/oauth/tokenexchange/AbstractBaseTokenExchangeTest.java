package org.keycloak.tests.oauth.tokenexchange;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ConsentPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.hamcrest.MatcherAssert;

import static org.keycloak.models.Constants.CREATE_DEFAULT_CLIENT_SCOPES;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class AbstractBaseTokenExchangeTest {

    @InjectRealm(config = TokenExchangeRealm.class)
    ManagedRealm realm;

    @InjectEvents
    Events events;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    ConsentPage consentPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    protected static UserRepresentation john;

    protected static UserRepresentation mike;

    protected static UserRepresentation alice;

    protected static ClientRepresentation subjectClient;

    protected static ClientRepresentation requesterClient;

    protected static ClientRepresentation requesterClient2;

    @TestSetup
    public void setup() {
        enableEvents(realm);
        john = AdminApiUtil.findUserByUsername(realm.admin(), "john");
        mike = AdminApiUtil.findUserByUsername(realm.admin(), "mike");
        alice = AdminApiUtil.findUserByUsername(realm.admin(), "alice");

        subjectClient = AdminApiUtil.findClientByClientId(realm.admin(), "subject-client").toRepresentation();
        requesterClient = AdminApiUtil.findClientByClientId(realm.admin(), "requester-client").toRepresentation();
        requesterClient2 = AdminApiUtil.findClientByClientId(realm.admin(), "requester-client-2").toRepresentation();
    }

    private void enableEvents(ManagedRealm realm) {
        RealmEventsConfigRepresentation realmEventsConfig = realm.admin().getRealmEventsConfig();
        List<String> enabledEventTypes = realmEventsConfig.getEnabledEventTypes();
        if (!enabledEventTypes.contains(EventType.REFRESH_TOKEN.name())) {
            enabledEventTypes.add(EventType.REFRESH_TOKEN.name());
            enabledEventTypes.add(EventType.INTROSPECT_TOKEN.name());
            realm.admin().updateRealmEventsConfig(realmEventsConfig);
        }
    }

    public static class TokenExchangeRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {

            realm.eventsEnabled(true);

            realm.attribute(CREATE_DEFAULT_CLIENT_SCOPES, String.valueOf(true));

            // Client Scopes
            realm.clientScopes(createClientScope("default-scope1"));
            realm.clientScopes(createClientScope("optional-scope2"));
            realm.clientScopes(createClientScope("optional-scope3"));
            realm.clientScopes(createClientScope("optional-requester-scope"));

            // Clients
            realm.clients(ClientBuilder.create().clientId("subject-client")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("service_account", "acr", "roles", "profile", "basic", "email")
                    .optionalClientScopes("optional-scope2", "optional-requester-scope", OAuth2Constants.OFFLINE_ACCESS)
                    .protocolMappers(createAudienceMapper("requester-client", "requester-client"), createAudienceMapper("subject-client", "subject-client")));

            realm.users(UserBuilder.create().username(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "subject-client")
                    .serviceAccountId("subject-client")
                    .clientRoles("target-client1", "target-client1-role"));

            realm.clients(ClientBuilder.create().clientId("requester-client")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("service_account", "acr", "default-scope1", "roles", "basic")
                    .optionalClientScopes("optional-scope2", "optional-requester-scope", "offline_access", "profile", "email")
                    .protocolMappers(createAudienceMapper("audience-requester-client", "requester-client")));

            realm.clients(ClientBuilder.create().clientId("requester-client-2")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("service_account", "acr", "default-scope1", "roles", "basic")
                    .optionalClientScopes("optional-scope2", "optional-requester-scope", "offline_access", "profile", "email")
                    .protocolMappers(createAudienceMapper("audience-requester-client-2", "requester-client-2")));

            realm.clients(ClientBuilder.create().clientId("requester-client-public")
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .fullScopeEnabled(true)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("acr", "roles", "basic")
                    .optionalClientScopes("optional-scope2", "optional-requester-scope"));

            realm.clients(ClientBuilder.create().clientId("target-client1")
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .fullScopeEnabled(true)
                    .defaultClientScopes("acr", "roles", "basic"));

            realm.clients(ClientBuilder.create().clientId("target-client2")
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .fullScopeEnabled(true)
                    .defaultClientScopes("acr", "roles", "basic"));

            realm.clients(ClientBuilder.create().clientId("target-client3")
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .fullScopeEnabled(true)
                    .defaultClientScopes("acr", "roles", "basic"));

            realm.clients(ClientBuilder.create().clientId("invalid-requester-client")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(true)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("service_account", "acr", "roles", "basic"));

            realm.clients(ClientBuilder.create().clientId("disabled-requester-client")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(true)
                    .attribute("standard.token.exchange.enabled", "false")
                    .defaultClientScopes("service_account", "acr", "roles", "basic"));

            // Realm roles
            realm.roles("all-target", "offline_access", "uma_authorization");

            realm.realmRoles(RoleBuilder.create().name("default-roles-test")
                    .description("${role_default-roles}")
                    .composite(true)
                    .realmComposite("offline_access")
                    .realmComposite("uma_authorization")
                    .clientComposite("account", "view-profile")
                    .clientComposite("account", "manage-account"));

            // Client roles (must be after clients are created)
            realm.clientRoles("requester-client", "requester-client-role");
            realm.clientRoles("requester-client-2", "requester-client-2-role");
            realm.clientRoles("target-client1", "target-client1-role");
            realm.clientRoles("target-client2", "target-client2-role");
            realm.clientRoles("target-client3", "target-client3-role");

            // Scope mappings: map realm roles to client scopes
            realm.addClientScopeRealmRoleMapping("optional-scope2", "all-target");
            realm.addClientScopeRealmRoleMapping("offline_access", "offline_access");

            // Client scope mappings: map client roles to client scopes
            realm.addClientScopeClientRoleMapping("target-client1", "default-scope1", "target-client1-role");
            realm.addClientScopeClientRoleMapping("target-client2", "optional-scope2", "target-client2-role");
            realm.addClientScopeClientRoleMapping("requester-client", "optional-requester-scope", "requester-client-role");
            realm.addClientScopeClientRoleMapping("requester-client-2", "optional-requester-scope", "requester-client-2-role");

            // Users
            realm.users(UserBuilder.create().username("alice")
                    .name("Alice", "Doe")
                    .email("alice@email.cz")
                    .password("password")
                    .realmRoles("default-roles-test")
                    .clientRoles("requester-client", "requester-client-role")
                    .clientRoles("requester-client-2", "requester-client-2-role"));

            realm.users(UserBuilder.create().username("john")
                    .name("John", "Bar")
                    .email("john@email.cz")
                    .password("password")
                    .realmRoles("default-roles-test")
                    .clientRoles("target-client1", "target-client1-role")
                    .clientRoles("target-client2", "target-client2-role"));

            realm.users(UserBuilder.create().username("mike")
                    .name("Mike", "Bar")
                    .email("mike@email.cz")
                    .password("password")
                    .realmRoles("default-roles-test", "all-target")
                    .clientRoles("target-client1", "target-client1-role"));

            return realm;
        }

        private ClientScopeRepresentation createClientScope(String name) {
            ClientScopeRepresentation cs = new ClientScopeRepresentation();
            cs.setName(name);
            cs.setProtocol("openid-connect");
            Map<String, String> attrs = new HashMap<>();
            attrs.put("include.in.token.scope", "true");
            attrs.put("display.on.consent.screen", "true");
            cs.setAttributes(attrs);
            return cs;
        }

        private ProtocolMapperRepresentation createAudienceMapper(String name, String audience) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-audience-mapper");
            Map<String, String> config = new HashMap<>();
            config.put("included.client.audience", audience);
            config.put("id.token.claim", "false");
            config.put("lightweight.claim", "false");
            config.put("access.token.claim", "true");
            config.put("introspection.token.claim", "true");
            mapper.setConfig(config);
            return mapper;
        }

    }

    protected String getSessionIdFromToken(String accessToken) {
        return verifyAccessToken(accessToken).getSessionId();
    }

    protected AccessTokenResponse resourceOwnerLogin(String username, String password, String clientId, String secret) {
        return resourceOwnerLogin(username, password, clientId, secret, null);
    }

    protected AccessTokenResponse resourceOwnerLogin(String username, String password, String clientId, String secret, String scope) {
        events.clear();
        oauth.client(clientId, secret);
        oauth.scope(scope);
        oauth.openid(false);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken token = verifyAccessToken(response.getAccessToken());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .clientId(clientId)
                .userId(token.getSubject())
                .sessionId(token.getSessionId())
                .details(Details.USERNAME, username);
        return response;
    }

    protected String loginWithConsents(UserRepresentation user, String password, String clientId, String secret) {
        oauth.client(clientId, secret).openLoginForm();
        oauth.fillLoginForm(user.getUsername(), password);
        consentPage.assertCurrent();
        consentPage.confirm();
        assertTrue(oauth.parseLoginResponse().isSuccess());
        AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken token = verifyAccessToken(response.getAccessToken());
        final EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId(clientId)
                .userId(user.getId())
                .sessionId(token.getSessionId())
                .details(Details.USERNAME, user.getUsername())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .hasCodeId();
        final String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.CODE_TO_TOKEN)
                .clientId(clientId)
                .userId(user.getId())
                .sessionId(token.getSessionId())
                .details(Details.CODE_ID, codeId);
        return response.getAccessToken();
    }

    protected AccessTokenResponse tokenExchange(String subjectToken, String clientId, String secret, List<String> audience, String requestedTokenType) {
        return oauth.tokenExchangeRequest(subjectToken).client(clientId, secret).audience(audience).requestedTokenType(requestedTokenType).send();
    }

    protected void isAccessTokenEnabled(String accessToken, String clientId, String secret) {
        oauth.client(clientId, secret);
        TokenMetadataRepresentation rep = introspectToken(accessToken);
        assertTrue(rep.isActive());
        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.INTROSPECT_TOKEN)
                .clientId(clientId);
        assertNotNull(event.getUserId());
        assertNotNull(event.getSessionId());
    }

    protected void isAccessTokenDisabled(String accessTokenString, String clientId, String secret) {
        // Test introspection endpoint not possible
        oauth.client(clientId, secret);
        TokenMetadataRepresentation rep = introspectToken(accessTokenString);
        assertFalse(rep.isActive());
    }

    protected void isTokenEnabled(AccessTokenResponse tokenResponse, String clientId, String secret) {
        isAccessTokenEnabled(tokenResponse.getAccessToken(), clientId, secret);
        AccessTokenResponse tokenRefreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(Response.Status.OK.getStatusCode(), tokenRefreshResponse.getStatusCode());
    }

    protected void isTokenDisabled(AccessTokenResponse tokenResponse, String clientId, String secret) {
        isAccessTokenDisabled(tokenResponse.getAccessToken(), clientId, secret);

        oauth.client(clientId, secret);
        AccessTokenResponse tokenRefreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), tokenRefreshResponse.getStatusCode());
    }

    protected void assertAudiences(AccessToken token, List<String> expectedAudiences) {
        MatcherAssert.assertThat("Incompatible audiences", token.getAudience() == null ? List.of() : List.of(token.getAudience()), containsInAnyOrder(expectedAudiences.toArray()));
        java.util.List<String> audsWithoutAzp = new java.util.ArrayList<>(expectedAudiences);
        audsWithoutAzp.remove(token.getIssuedFor());
        MatcherAssert.assertThat("Incompatible resource access", token.getResourceAccess().keySet(), containsInAnyOrder(audsWithoutAzp.toArray()));
    }

    protected void assertScopes(AccessToken token, List<String> expectedScopes) {
        MatcherAssert.assertThat("Incompatible scopes", token.getScope().isEmpty() ? List.of() : List.of(token.getScope().split(" ")), containsInAnyOrder(expectedScopes.toArray()));
    }

    protected AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, List<String> expectedAudiences, List<String> expectedScopes) {
        assertEquals(Response.Status.OK.getStatusCode(), tokenExchangeResponse.getStatusCode());
        AccessToken token = verifyAccessToken(tokenExchangeResponse.getAccessToken());
        if (expectedAudiences == null) {
            assertNull(token.getAudience(), "Expected token to not contain audience");
        } else {
            assertAudiences(token, expectedAudiences);
        }
        assertScopes(token, expectedScopes);
        return token;
    }

    protected AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, UserRepresentation user, List<String> expectedAudiences, List<String> expectedScopes) {
        return assertAudiencesAndScopes(tokenExchangeResponse, user, expectedAudiences, expectedScopes, OAuth2Constants.ACCESS_TOKEN_TYPE, "subject-client");
    }

    protected AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, UserRepresentation user,
                                                 List<String> expectedAudiences, List<String> expectedScopes, String expectedTokenType, String expectedSubjectTokenClientId) {
        AccessToken token = assertAudiencesAndScopes(tokenExchangeResponse, expectedAudiences, expectedScopes);
        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.TOKEN_EXCHANGE)
                .clientId(token.getIssuedFor())
                .userId(user.getId())
                .sessionId(token.getSessionId())
                .details(Details.AUDIENCE, CollectionUtil.join(expectedAudiences, " "))
                .details(Details.USERNAME, user.getUsername())
                .details(Details.REQUESTED_TOKEN_TYPE, expectedTokenType)
                .details(Details.SUBJECT_TOKEN_CLIENT_ID, expectedSubjectTokenClientId);
        // Verify scopes
        Set<String> expectedScopeSet = new HashSet<>(expectedScopes);
        Set<String> actualScopeSet = new HashSet<>(Arrays.asList(event.getDetails().get(Details.SCOPE).split(" ")));
        assertEquals(expectedScopeSet, actualScopeSet);

        return token;
    }

    protected void assertIntrospectSuccess(String token, String clientId, String clientSecret, String userId) {
        oauth.client(clientId, clientSecret);
        TokenMetadataRepresentation rep = introspectToken(token);
        assertTrue(rep.isActive());
        assertEquals(userId, rep.getSubject());
    }

    protected void assertIntrospectError(String token) {
        TokenMetadataRepresentation rep = introspectToken(token);
        assertFalse(rep.isActive());
    }

    protected void assertUserInfoSuccess(String token, String clientId, String clientSecret, String userId) {
        UserInfoResponse userInfoResp = oauth.client(clientId, clientSecret).userInfoRequest(token).send();
        assertEquals(Response.Status.OK.getStatusCode(), userInfoResp.getStatusCode());
        assertEquals(userId, userInfoResp.getUserInfo().getSub());
    }

    protected void assertUserInfoError(String token, String clientId, String clientSecret, String error, String errorDesciption) {
        UserInfoResponse userInfoResp = oauth.client(clientId, clientSecret).userInfoRequest(token).send();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), userInfoResp.getStatusCode());
        assertEquals(String.format("Bearer realm=\"%s\", error=\"%s\", error_description=\"%s\"", realm.getName(), error, errorDesciption),
                userInfoResp.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    protected void assertAccessTokenContext(String jti, AccessTokenContext.SessionType sessionType, AccessTokenContext.TokenType tokenType, String grantType) {
        AccessTokenContext ctx = getAccessTokenContext(jti);
        assertEquals(sessionType, ctx.getSessionType());
        assertEquals(tokenType, ctx.getTokenType());
        assertEquals(grantType, ctx.getGrantType());
    }

    protected AccessTokenContext getAccessTokenContext(String jti) {
        return runOnServer.fetch(session -> session.getProvider(TokenContextEncoderProvider.class).getTokenContextFromTokenId(jti), AccessTokenContext.class);
    }

    protected Integer getClientSessionsCountInUserSession(String sessionId) {
        return runOnServer.fetch(session -> {
            UserSessionModel sessionModel = session.sessions().getUserSession(session.getContext().getRealm(), sessionId);
            if (sessionModel == null) {
                throw new NotFoundException("Session not found");
            }
            return sessionModel.getAuthenticatedClientSessions().size();
            }, Integer.class);
    }

    public IDToken verifyIdToken(String idToken) {
        return verifyToken(idToken, IDToken.class);
    }

    public AccessToken verifyAccessToken(String accessToken) {
        return verifyToken(accessToken, AccessToken.class);
    }

    public <T extends JsonWebToken> T verifyToken(String token, Class<T> clazz) {
        TokenVerifier<T> tokenVerifier = TokenVerifier.create(token, clazz);
        try {
            return tokenVerifier.parse().getToken();
        } catch (VerificationException e) {
            fail("Error verifying token", e);
        }
        return null;
    }

    public TokenMetadataRepresentation introspectToken(String token) {
        try {
            return oauth.doIntrospectionAccessTokenRequest(token).asTokenMetadata();
        } catch (IOException e) {
            fail("Error during token introspection", e);
        }
        return null;
    }

}
