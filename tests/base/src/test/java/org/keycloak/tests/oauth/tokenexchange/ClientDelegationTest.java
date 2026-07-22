package org.keycloak.tests.oauth.tokenexchange;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuth2Constants.ACCESS_TOKEN_TYPE;
import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.representations.IDToken.ACT;
import static org.keycloak.representations.IDToken.MAY_ACT;
import static org.keycloak.representations.JsonWebToken.SUBJECT;

@KeycloakIntegrationTest(config = ClientDelegationTest.ClientDelegationServerConfig.class)
public class ClientDelegationTest {

    static final String USERNAME = "test-user@localhost";
    static final String PASSWORD = "password";
    static final String TEST_CLIENT_ID = "test-app";
    static final String TEST_CLIENT_SECRET = "test-secret";
    static final String AGENT_CLIENT_ID = "agent-app";
    static final String AGENT_CLIENT_SECRET = "agent-secret";
    static final String AGENT_DELEGATION_SCOPE = OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + AGENT_CLIENT_ID;

    @InjectRealm(config = ClientDelegationRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = TestOAuthClientConfig.class)
    OAuthClient oauth;

    @InjectClient(config = AgentClientConfig.class)
    ManagedClient agentApp;

    @InjectEvents
    protected Events events;

    @InjectPage
    protected OAuthGrantPage grantPage;

    @AfterEach
    public void afterEach() {
        AccountHelper.logout(realm.admin(), USERNAME);
        List<Map<String, Object>> consents = AccountHelper.getUserConsents(realm.admin(), USERNAME);
        if (consents.stream().anyMatch(m -> oauth.getClientId().equals(m.get("clientId")))) {
            AccountHelper.revokeConsents(realm.admin(), USERNAME, oauth.getClientId());
        }
    }

    @Test
    public void successfulExchangeAndRefresh() {
        AccessTokenResponse res = loginWithDelegation(AGENT_DELEGATION_SCOPE);
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String serviceAccountUserId = getServiceAccountUserId();
        assertMayActPresent(token, serviceAccountUserId, AGENT_CLIENT_ID);

        // refresh the token
        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), serviceAccountUserId, AGENT_CLIENT_ID);

        // perform the token exchange with delegation
        String actorToken = getActorToken();
        assertTokenExchangeSuccess(res.getAccessToken(), actorToken, serviceAccountUserId);

        logout(res.getRefreshToken());
    }

    @Test
    public void scopeReduction() {
        AccessTokenResponse res = loginWithDelegation(AGENT_DELEGATION_SCOPE);
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);

        String serviceAccountUserId = getServiceAccountUserId();
        String actorToken = getActorToken();

        // exchange requesting the optional "email" scope
        AccessTokenResponse withEmail = oauth.client(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET)
                .scope("openid email")
                .tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .send();
        Assertions.assertTrue(withEmail.isSuccess(), withEmail.getError() + " - " + withEmail.getErrorDescription());
        events.poll(); // consume TOKEN_EXCHANGE event
        assertScopeContains(withEmail.getScope(), "email");
        assertActPresent(oauth.verifyToken(withEmail.getAccessToken()), serviceAccountUserId, AGENT_CLIENT_ID);

        // exchange without requesting "email" — optional scope should be absent
        actorToken = getActorToken();
        AccessTokenResponse withoutEmail = oauth.client(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET)
                .scope("openid")
                .tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .send();
        Assertions.assertTrue(withoutEmail.isSuccess(), withoutEmail.getError() + " - " + withoutEmail.getErrorDescription());
        events.poll(); // consume TOKEN_EXCHANGE event
        assertScopeNotContains(withoutEmail.getScope(), "email");
        assertActPresent(oauth.verifyToken(withoutEmail.getAccessToken()), serviceAccountUserId, AGENT_CLIENT_ID);

        logout(res.getRefreshToken());
    }

    @Test
    public void notEnabled() {
        agentApp.updateWithCleanup(c -> c.attribute(OIDCConfigAttributes.CLIENT_DELEGATION_ENABLED, Boolean.FALSE.toString()));
        assertDelegationScopeRejected(AGENT_DELEGATION_SCOPE);
    }

    @Test
    public void nonExistentClient() {
        String scope = OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + "nonexistent-client";
        assertDelegationScopeRejected(scope);
    }

    @Test
    public void disabledClient() {
        agentApp.updateWithCleanup(c -> c.enabled(false));
        assertDelegationScopeRejected(AGENT_DELEGATION_SCOPE);
    }

    @Test
    public void serviceAccountsNotEnabled() {
        agentApp.updateWithCleanup(c -> c.serviceAccountsEnabled(false));
        assertDelegationScopeRejected(AGENT_DELEGATION_SCOPE);
    }

    @Test
    public void revokedOnRefresh() {
        AccessTokenResponse res = loginWithDelegation(AGENT_DELEGATION_SCOPE);
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), getServiceAccountUserId(), AGENT_CLIENT_ID);

        // disable client delegation and refresh — may_act should be stripped
        agentApp.updateWithCleanup(c -> c.attribute(OIDCConfigAttributes.CLIENT_DELEGATION_ENABLED, Boolean.FALSE.toString()));
        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeNotContains(res.getScope(), AGENT_DELEGATION_SCOPE);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        // token exchange should not work without may_act claim
        String actorToken = getActorToken();
        assertTokenExchangeError(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, res.getAccessToken(), actorToken,
                AGENT_CLIENT_ID, "Invalid may_act claim in the subject_token");

        logout(res.getRefreshToken());
    }

    @Test
    public void clientIdMismatchOnExchange() {
        String clientDelegationScopeId = findClientDelegationScopeId();

        // Remove the original client_id mapper so the hardcoded one takes effect
        ProtocolMapperRepresentation originalMapper = realm.admin().clientScopes().get(clientDelegationScopeId)
                .getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.CLIENT_DELEGATION_MAY_ACT_CLIENT_ID.equals(m.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Original client_id mapper not found"));
        realm.admin().clientScopes().get(clientDelegationScopeId)
                .getProtocolMappers().delete(originalMapper.getId());
        realm.cleanup().add(r -> r.clientScopes().get(clientDelegationScopeId)
                .getProtocolMappers().createMapper(originalMapper));

        // Add a hardcoded claim mapper that sets may_act.client_id to a wrong value
        ProtocolMapperRepresentation wrongClientIdMapper = new ProtocolMapperRepresentation();
        wrongClientIdMapper.setName("wrong-client-id-mapper");
        wrongClientIdMapper.setProtocol("openid-connect");
        wrongClientIdMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "may_act.client_id");
        config.put(HardcodedClaim.CLAIM_VALUE, "wrong-client");
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        wrongClientIdMapper.setConfig(config);

        String mapperId;
        try (var response = realm.admin().clientScopes().get(clientDelegationScopeId)
                .getProtocolMappers().createMapper(wrongClientIdMapper)) {
            Assertions.assertEquals(201, response.getStatus(), "Mapper creation should succeed");
            mapperId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clientScopes().get(clientDelegationScopeId)
                .getProtocolMappers().delete(mapperId));

        AccessTokenResponse res = loginWithDelegation(AGENT_DELEGATION_SCOPE);
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);

        // actor token is from agent-app (azp = "agent-app") but may_act.client_id is "wrong-client"
        String actorToken = getActorToken();
        assertTokenExchangeError(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, res.getAccessToken(), actorToken,
                AGENT_CLIENT_ID, "Actor token client does not match the client_id in the may_act claim");

        logout(res.getRefreshToken());
    }

    @Test
    public void wrongRequestingClient() {
        AccessTokenResponse res = loginWithDelegation(AGENT_DELEGATION_SCOPE);
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);

        // test-app tries to perform the exchange instead of agent-app — should fail
        String actorToken = getActorToken();
        assertTokenExchangeError(TEST_CLIENT_ID, TEST_CLIENT_SECRET, res.getAccessToken(), actorToken,
                TEST_CLIENT_ID, "Requesting client does not match the client_id in the may_act claim");

        logout(res.getRefreshToken());
    }

    @Test
    public void wrongActor() {
        AccessTokenResponse res = loginWithDelegation(AGENT_DELEGATION_SCOPE);
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);

        // use a different user as actor — should fail
        String wrongActorToken = oauth.client(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET).scope(null)
                .doPasswordGrantRequest("otheruser", PASSWORD).getAccessToken();
        events.poll(); // consume the login event

        assertTokenExchangeError(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, res.getAccessToken(), wrongActorToken,
                AGENT_CLIENT_ID, "Actor user is not allowed by the may_act claim inside the subject_token");

        logout(res.getRefreshToken());
    }

    private AccessTokenResponse loginWithDelegation(String scope) {
        return loginWithDelegation(scope, grants -> MatcherAssert.assertThat(grants,
                Matchers.hasItem("Allow " + AGENT_CLIENT_ID + " to act on your behalf?")));
    }

    private AccessTokenResponse loginWithDelegation(String scope, Consumer<List<String>> grantsValidator) {
        oauth.client(TEST_CLIENT_ID, TEST_CLIENT_SECRET);
        oauth.scope(scope).openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        grantsValidator.accept(grants);
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(TEST_CLIENT_ID)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);
        return res;
    }

    private void assertDelegationScopeRejected(String scope) {
        AccessTokenResponse res = loginWithDelegation(scope, grants -> MatcherAssert.assertThat(grants,
                Matchers.not(Matchers.hasItem(Matchers.containsString("act on your behalf")))));

        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeNotContains(res.getScope(), scope);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        logout(res.getRefreshToken());
    }

    private String getActorToken() {
        AccessTokenResponse res = oauth.client(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET).scope(null)
                .doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertTrue(res.isSuccess(), res.getError());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CLIENT_LOGIN)
                .clientId(AGENT_CLIENT_ID);
        return res.getAccessToken();
    }

    private String getServiceAccountUserId() {
        return realm.admin().clients().findByClientId(AGENT_CLIENT_ID).stream()
                .findFirst()
                .map(c -> realm.admin().clients().get(c.getId()).getServiceAccountUser().getId())
                .orElseThrow(() -> new AssertionError("agent-app client not found"));
    }

    private void assertTokenExchangeSuccess(String subjectToken, String actorToken, String expectedActorId) {
        AccessTokenResponse tokenExchangeRes = oauth.client(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET).tokenExchangeRequest(subjectToken)
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .send();
        Assertions.assertTrue(tokenExchangeRes.isSuccess(), tokenExchangeRes.getError() + " - " + tokenExchangeRes.getErrorDescription());

        String serviceAccountUsername = "service-account-" + AGENT_CLIENT_ID;
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.TOKEN_EXCHANGE)
                .clientId(AGENT_CLIENT_ID)
                .hasUserId()
                .details(Details.USERNAME, USERNAME)
                .details(Details.ACTOR, serviceAccountUsername)
                .details(Details.ACTOR_ID, expectedActorId)
                .details(Details.REQUESTED_TOKEN_TYPE, ACCESS_TOKEN_TYPE)
                .details(Details.SUBJECT_TOKEN_CLIENT_ID, TEST_CLIENT_ID);

        AccessToken teToken = oauth.verifyToken(tokenExchangeRes.getAccessToken());
        Assertions.assertEquals(USERNAME, teToken.getPreferredUsername());
        assertActPresent(teToken, expectedActorId, AGENT_CLIENT_ID);
        Assertions.assertNull(teToken.getSessionId(), "Session is not transient");

        IntrospectionResponse introspectRes = oauth.client(TEST_CLIENT_ID, TEST_CLIENT_SECRET)
                .doIntrospectionAccessTokenRequest(tokenExchangeRes.getAccessToken());
        Assertions.assertTrue(introspectRes.isSuccess());
        try {
            TokenMetadataRepresentation rep = introspectRes.asTokenMetadata();
            Assertions.assertEquals(USERNAME, rep.getUserName());
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    private void assertTokenExchangeError(String requestingClientId, String requestingClientSecret,
                                           String subjectToken, String actorToken,
                                           String expectedEventClientId, String expectedReason) {
        AccessTokenResponse tokenExchangeRes = oauth.client(requestingClientId, requestingClientSecret)
                .tokenExchangeRequest(subjectToken)
                .actorToken(actorToken).actorTokenType(ACCESS_TOKEN_TYPE).send();
        Assertions.assertFalse(tokenExchangeRes.isSuccess());
        EventAssertion.assertError(events.poll())
                .type(EventType.TOKEN_EXCHANGE_ERROR)
                .clientId(expectedEventClientId)
                .error(Errors.INVALID_TOKEN)
                .details(Details.REASON, expectedReason);
    }

    private void logout(String refreshToken) {
        LogoutResponse logout = oauth.client(TEST_CLIENT_ID, TEST_CLIENT_SECRET).doLogout(refreshToken);
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @SuppressWarnings("unchecked")
    static void assertMayActPresent(AccessToken token, String expectedActorId, String expectedClientId) {
        Map<String, Object> mayAct = (Map<String, Object>) token.getOtherClaims().get(MAY_ACT);
        Assertions.assertNotNull(mayAct, "may_act claim should be present");
        Assertions.assertEquals(expectedActorId, mayAct.get(SUBJECT), "may_act.sub should contain the service account user ID");
        Assertions.assertEquals(expectedClientId, mayAct.get(CLIENT_ID), "may_act.client_id should contain the client ID");
    }

    @SuppressWarnings("unchecked")
    static void assertActPresent(AccessToken token, String expectedActorId, String expectedClientId) {
        Map<String, Object> act = (Map<String, Object>) token.getOtherClaims().get(ACT);
        Assertions.assertNotNull(act, "act claim should be present");
        Assertions.assertEquals(expectedActorId, act.get(SUBJECT), "act.sub should contain the service account user ID");
        Assertions.assertEquals(expectedClientId, act.get(CLIENT_ID), "act.client_id should contain the client ID");
    }

    static void assertMayActNotPresent(AccessToken token) {
        Assertions.assertNull(token.getOtherClaims().get(MAY_ACT), "may_act claim should not be present");
    }

    static void assertScopeContains(String scopeString, String expectedScope) {
        Assertions.assertNotNull(scopeString, "Scope string should not be null");
        MatcherAssert.assertThat(Arrays.asList(scopeString.split(" ")), Matchers.hasItem(expectedScope));
    }

    private String findClientDelegationScopeId() {
        return realm.admin().clientScopes().findAll().stream()
                .filter(cs -> OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE.equals(cs.getName()))
                .map(ClientScopeRepresentation::getId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("delegation:client client scope not found"));
    }

    static void assertScopeNotContains(String scopeString, String expectedScope) {
        Assertions.assertNotNull(scopeString, "Scope string should not be null");
        MatcherAssert.assertThat(Arrays.asList(scopeString.split(" ")), Matchers.not(Matchers.hasItem(expectedScope)));
    }

    static class ClientDelegationServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES, Profile.Feature.TOKEN_EXCHANGE_DELEGATION);
        }
    }

    static class ClientDelegationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(
                    UserBuilder.create(USERNAME).password(PASSWORD)
                            .email("test@localhost").firstName("Test").lastName("User"),
                    UserBuilder.create("otheruser").password(PASSWORD)
                            .email("otheruser@localhost").firstName("Other").lastName("User"));
        }
    }

    static class TestOAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client)
                    .defaultClientScopes("acr", "basic", "email", "profile")
                    .optionalClientScopes(OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE)
                    .consentRequired(true)
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString());
        }
    }

    static class AgentClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
            audienceMapper.setName("audience-mapper");
            audienceMapper.setProtocol("openid-connect");
            audienceMapper.setProtocolMapper("oidc-audience-mapper");
            Map<String, String> config = new HashMap<>();
            config.put("included.client.audience", "test-app");
            config.put("id.token.claim", "false");
            config.put("lightweight.claim", "false");
            config.put("access.token.claim", "true");
            config.put("introspection.token.claim", "true");
            audienceMapper.setConfig(config);

            return client.clientId(AGENT_CLIENT_ID).name("AI Agent App").secret(AGENT_CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled()
                    .defaultClientScopes("acr", "basic", "profile")
                    .optionalClientScopes("email")
                    .attribute(OIDCConfigAttributes.CLIENT_DELEGATION_ENABLED, Boolean.TRUE.toString())
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString())
                    .protocolMappers(audienceMapper);
        }
    }
}
