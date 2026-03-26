package org.keycloak.tests.oauth.tokenexchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
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
import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.models.Constants.CREATE_DEFAULT_CLIENT_SCOPES;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseAbstractTokenExchangeTest {

    @InjectEvents
    Events events;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    ConsentPage consentPage;

    @InjectRealm(config = TokenExchangeRealm.class)
    ManagedRealm realm;

    @InjectClient(attachTo = "subject-client", lifecycle = LifeCycle.METHOD)
    ManagedClient subjectClient;

    @InjectClient(attachTo = "requester-client", lifecycle = LifeCycle.METHOD)
    ManagedClient requesterClient;

    @InjectClient(attachTo = "requester-client-2", lifecycle = LifeCycle.METHOD)
    ManagedClient requesterClient2;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectSimpleHttp
    org.keycloak.http.simple.SimpleHttp simpleHttp;

    UserRepresentation john;

    UserRepresentation mike;

    UserRepresentation alice;


    @BeforeEach
    public void setup() {
        john = AdminApiUtil.findUserByUsernameId(realm.admin(), "john").toRepresentation();
        mike = AdminApiUtil.findUserByUsernameId(realm.admin(), "mike").toRepresentation();
        alice = AdminApiUtil.findUserByUsernameId(realm.admin(), "alice").toRepresentation();

        // Assign roles to service account user
        UserRepresentation serviceAccount = subjectClient.admin().getServiceAccountUser();

        // Assign realm role default-roles-test
        RoleRepresentation defaultRolesTest = realm.admin().roles().get("default-roles-test").toRepresentation();
        realm.admin().users().get(serviceAccount.getId()).roles().realmLevel().add(List.of(defaultRolesTest));

        // Assign client role target-client1-role
        String targetClient1Id = realm.admin().clients().findByClientId("target-client1").get(0).getId();
        RoleRepresentation targetClient1Role = realm.admin().clients().get(targetClient1Id).roles().get("target-client1-role").toRepresentation();
        realm.admin().users().get(serviceAccount.getId()).roles().clientLevel(targetClient1Id).add(List.of(targetClient1Role));
        enableEvents(realm);
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
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {

            realm.eventsEnabled(true);

            realm.attribute(CREATE_DEFAULT_CLIENT_SCOPES, String.valueOf(true));

            // Client Scopes
            realm.addClientScope(createClientScope("default-scope1"));
            realm.addClientScope(createClientScope("optional-scope2"));
            realm.addClientScope(createClientScope("optional-scope3"));
            realm.addClientScope(createClientScope("optional-requester-scope"));

            // Clients
            realm.addClient("subject-client")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("service_account", "acr", "roles", "profile", "basic", "email")
                    .optionalClientScopes("optional-scope2", "optional-requester-scope", OAuth2Constants.OFFLINE_ACCESS)
                    .protocolMappers(List.of(
                            createAudienceMapper("subject-client", "subject-client"),
                            createAudienceMapper("requester-client", "requester-client")
                    ));

            realm.addClient("requester-client")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("service_account", "acr", "default-scope1", "roles", "basic")
                    .optionalClientScopes("optional-scope2", "optional-requester-scope", "offline_access", "profile", "email");

            realm.addClient("requester-client-2")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("service_account", "acr", "default-scope1", "roles", "basic")
                    .optionalClientScopes("optional-scope2", "optional-requester-scope", "offline_access", "profile", "email");

            realm.addClient("requester-client-public")
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .fullScopeEnabled(true)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("acr", "roles", "basic")
                    .optionalClientScopes("optional-scope2", "optional-requester-scope");

            realm.addClient("target-client1")
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .fullScopeEnabled(true)
                    .defaultClientScopes("acr", "roles", "basic");

            realm.addClient("target-client2")
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .fullScopeEnabled(true)
                    .defaultClientScopes("acr", "roles", "basic");

            realm.addClient("target-client3")
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .fullScopeEnabled(true)
                    .defaultClientScopes("acr", "roles", "basic");

            realm.addClient("invalid-requester-client")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(true)
                    .attribute("standard.token.exchange.enabled", "true")
                    .defaultClientScopes("service_account", "acr", "roles", "basic");

            realm.addClient("disabled-requester-client")
                    .secret("secret")
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(true)
                    .attribute("standard.token.exchange.enabled", "false")
                    .defaultClientScopes("service_account", "acr", "roles", "basic");

            // Realm roles
            realm.roles("all-target", "offline_access", "uma_authorization");

            realm.addRole("default-roles-test")
                    .description("${role_default-roles}")
                    .composite(true)
                    .realmComposite("offline_access")
                    .realmComposite("uma_authorization")
                    .clientComposite("account", "view-profile")
                    .clientComposite("account", "manage-account");

            // Client roles (must be after clients are created)
            realm.addClientRole("requester-client", "requester-client-role");
            realm.addClientRole("requester-client-2", "requester-client-2-role");
            realm.addClientRole("target-client1", "target-client1-role");
            realm.addClientRole("target-client2", "target-client2-role");
            realm.addClientRole("target-client3", "target-client3-role");

            // Users
            realm.addUser("alice")
                    .name("Alice", "Doe")
                    .email("alice@email.cz")
                    .password("password")
                    .roles("default-roles-test")
                    .clientRoles("requester-client", "requester-client-role")
                    .clientRoles("requester-client-2", "requester-client-2-role");

            realm.addUser("john")
                    .name("John", "Bar")
                    .email("john@email.cz")
                    .password("password")
                    .roles("default-roles-test")
                    .clientRoles("target-client1", "target-client1-role")
                    .clientRoles("target-client2", "target-client2-role");

            realm.addUser("mike")
                    .name("Mike", "Bar")
                    .email("mike@email.cz")
                    .password("password")
                    .roles("default-roles-test", "all-target")
                    .clientRoles("target-client1", "target-client1-role");

            // Scope mappings: map realm roles to client scopes
            realm.addClientScopeRealmRoleMapping("optional-scope2", "all-target");
            realm.addClientScopeRealmRoleMapping("offline_access", "offline_access");

            // Client scope mappings: map client roles to client scopes
            realm.addClientScopeClientRoleMapping("target-client1", "default-scope1", "target-client1-role");
            realm.addClientScopeClientRoleMapping("target-client2", "optional-scope2", "target-client2-role");
            realm.addClientScopeClientRoleMapping("requester-client", "optional-requester-scope", "requester-client-role");
            realm.addClientScopeClientRoleMapping("requester-client-2", "optional-requester-scope", "requester-client-2-role");

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

    protected String getSessionIdFromToken(String accessToken) throws Exception {
        return TokenVerifier.create(accessToken, AccessToken.class)
                .parse()
                .getToken()
                .getSessionId();
    }

    protected AccessTokenResponse resourceOwnerLogin(String username, String password, String clientId, String secret) throws Exception {
        return resourceOwnerLogin(username, password, clientId, secret, null);
    }

    protected AccessTokenResponse resourceOwnerLogin(String username, String password, String clientId, String secret, String scope) throws Exception {
        events.clear();
        oauth.client(clientId, secret);
        oauth.scope(scope);
        oauth.openid(false);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .clientId(clientId)
                .userId(token.getSubject())
                .sessionId(token.getSessionId())
                .details(Details.USERNAME, username);
        return response;
    }

    protected String loginWithConsents(UserRepresentation user, String password, String clientId, String secret) throws Exception {
        oauth.client(clientId, secret).openLoginForm();
        oauth.fillLoginForm(user.getUsername(), password);
        consentPage.assertCurrent();
        consentPage.confirm();
        assertNotNull(oauth.parseLoginResponse().getCode());
        AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        final EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId(clientId)
                .userId(user.getId())
                .sessionId(token.getSessionId())
                .details(Details.USERNAME, user.getUsername())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .isCodeId();
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

    protected void isAccessTokenEnabled(String accessToken, String clientId, String secret) throws IOException {
        oauth.client(clientId, secret);
        TokenMetadataRepresentation rep = oauth.doIntrospectionAccessTokenRequest(accessToken).asTokenMetadata();
        assertTrue(rep.isActive());
        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.INTROSPECT_TOKEN)
                .clientId(clientId);
        assertNotNull(event.getUserId());
        assertNotNull(event.getSessionId());
    }

    protected void isAccessTokenDisabled(String accessTokenString, String clientId, String secret) throws IOException {
        // Test introspection endpoint not possible
        oauth.client(clientId, secret);
        TokenMetadataRepresentation rep = oauth.doIntrospectionAccessTokenRequest(accessTokenString).asTokenMetadata();
        assertFalse(rep.isActive());
    }

    protected void isTokenEnabled(AccessTokenResponse tokenResponse, String clientId, String secret) throws IOException {
        isAccessTokenEnabled(tokenResponse.getAccessToken(), clientId, secret);
        AccessTokenResponse tokenRefreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(Response.Status.OK.getStatusCode(), tokenRefreshResponse.getStatusCode());
    }

    protected void isTokenDisabled(AccessTokenResponse tokenResponse, String clientId, String secret) throws IOException {
        isAccessTokenDisabled(tokenResponse.getAccessToken(), clientId, secret);

        oauth.client(clientId, secret);
        AccessTokenResponse tokenRefreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), tokenRefreshResponse.getStatusCode());
    }

    protected void assertAudiences(AccessToken token, List<String> expectedAudiences) {
        MatcherAssert.assertThat("Incompatible audiences", token.getAudience() == null ? List.of() : List.of(token.getAudience()), containsInAnyOrder(expectedAudiences.toArray()));
        MatcherAssert.assertThat("Incompatible resource access", token.getResourceAccess().keySet(), containsInAnyOrder(expectedAudiences.toArray()));
    }

    protected void assertScopes(AccessToken token, List<String> expectedScopes) {
        MatcherAssert.assertThat("Incompatible scopes", token.getScope().isEmpty() ? List.of() : List.of(token.getScope().split(" ")), containsInAnyOrder(expectedScopes.toArray()));
    }

    protected AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, List<String> expectedAudiences, List<String> expectedScopes) throws Exception {
        assertEquals(Response.Status.OK.getStatusCode(), tokenExchangeResponse.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(tokenExchangeResponse.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        if (expectedAudiences == null) {
            assertNull(token.getAudience(), "Expected token to not contain audience");
        } else {
            assertAudiences(token, expectedAudiences);
        }
        assertScopes(token, expectedScopes);
        return token;
    }

    protected AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, UserRepresentation user, List<String> expectedAudiences, List<String> expectedScopes) throws Exception {
        return assertAudiencesAndScopes(tokenExchangeResponse, user, expectedAudiences, expectedScopes, OAuth2Constants.ACCESS_TOKEN_TYPE, "subject-client");
    }

    protected AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, UserRepresentation user,
                                                 List<String> expectedAudiences, List<String> expectedScopes, String expectedTokenType, String expectedSubjectTokenClientId) throws Exception {
        AccessToken token = assertAudiencesAndScopes(tokenExchangeResponse, expectedAudiences, expectedScopes);
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.TOKEN_EXCHANGE)
                .clientId(token.getIssuedFor())
                .userId(user.getId())
                .sessionId(token.getSessionId())
                .details(Details.AUDIENCE, CollectionUtil.join(expectedAudiences, " "))
                .scopeDetails(CollectionUtil.join(expectedScopes, " "))
                .details(Details.USERNAME, user.getUsername())
                .details(Details.REQUESTED_TOKEN_TYPE, expectedTokenType)
                .details(Details.SUBJECT_TOKEN_CLIENT_ID, expectedSubjectTokenClientId);
        return token;
    }

  /*  protected void createClientScopeForRole(RealmResource realm, String clientId, String clientRoleName, String clientScopeName) {
        final ClientResource client = ApiUtil.findClientByClientId(realm, clientId);
        createClientScopeForRole(realm, client, clientRoleName, clientScopeName);
    }

    protected void createClientScopeForRole(RealmResource realm, ClientResource client, String clientRoleName, String clientScopeName) {
        final String clientUUID = client.toRepresentation().getId();
        final RoleRepresentation clientRole = ApiUtil.findClientRoleByName(client, clientRoleName).toRepresentation();

        final ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(clientScopeName);
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        final String clientScopeId = ApiUtil.getCreatedId(realm.clientScopes().create(clientScope));
        getCleanup().addClientScopeId(clientScopeId);
        realm.clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientUUID).add(List.of(clientRole));
    }*/

    protected void assertIntrospectSuccess(String token, String clientId, String clientSecret, String userId) throws IOException {
        TokenMetadataRepresentation rep = oauth.client(clientId, clientSecret).introspectionRequest(token).tokenTypeHint("access_token").send().asTokenMetadata();
        assertTrue(rep.isActive());
        assertEquals(userId, rep.getSubject());
    }

    protected void assertIntrospectError(String token, String clientId, String clientSecret) throws IOException {
        TokenMetadataRepresentation rep = oauth.client(clientId, clientSecret).introspectionRequest(token).tokenTypeHint("access_token").send().asTokenMetadata();
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

}
