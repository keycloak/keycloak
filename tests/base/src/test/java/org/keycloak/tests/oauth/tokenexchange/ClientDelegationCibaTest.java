package org.keycloak.tests.oauth.tokenexchange;

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.CibaProvider;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectCibaProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.testsuite.util.oauth.ciba.AuthenticationRequestAcknowledgement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuth2Constants.ACCESS_TOKEN_TYPE;
import static org.keycloak.tests.oauth.tokenexchange.ClientDelegationTest.AGENT_CLIENT_ID;
import static org.keycloak.tests.oauth.tokenexchange.ClientDelegationTest.AGENT_CLIENT_SECRET;
import static org.keycloak.tests.oauth.tokenexchange.ClientDelegationTest.AGENT_DELEGATION_SCOPE;
import static org.keycloak.tests.oauth.tokenexchange.ClientDelegationTest.PASSWORD;
import static org.keycloak.tests.oauth.tokenexchange.ClientDelegationTest.TEST_CLIENT_ID;
import static org.keycloak.tests.oauth.tokenexchange.ClientDelegationTest.TEST_CLIENT_SECRET;
import static org.keycloak.tests.oauth.tokenexchange.ClientDelegationTest.USERNAME;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertActPresent;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertMayActNotPresent;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertMayActPresent;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertScopeContains;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertScopeNotContains;

@KeycloakIntegrationTest(config = ClientDelegationCibaTest.CibaServerConfig.class)
public class ClientDelegationCibaTest {

    @InjectRealm(config = CibaRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = CibaOAuthClientConfig.class)
    OAuthClient oauth;

    @InjectClient(config = ClientDelegationTest.AgentClientConfig.class)
    ManagedClient agentApp;

    @InjectEvents
    protected Events events;

    @InjectCibaProvider
    protected CibaProvider ciba;

    @BeforeEach
    public void beforeEach() {
        addDelegationPermission();
    }

    @AfterEach
    public void afterEach() {
        AccountHelper.logout(realm.admin(), USERNAME);
    }

    @Test
    public void successfulDelegation() throws Exception {
        AccessTokenResponse res = cibaLogin(AGENT_DELEGATION_SCOPE, "delegation-test", true);

        String serviceAccountUserId = getServiceAccountUserId();
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), serviceAccountUserId, AGENT_CLIENT_ID);

        // refresh — may_act should persist
        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), serviceAccountUserId, AGENT_CLIENT_ID);

        // token exchange with delegation
        assertTokenExchangeSuccess(res.getAccessToken(), serviceAccountUserId);

        logout(res.getRefreshToken());
    }

    @Test
    public void revokedOnRefresh() throws Exception {
        AccessTokenResponse res = cibaLogin(AGENT_DELEGATION_SCOPE, "delegation-revoke", true);
        assertScopeContains(res.getScope(), AGENT_DELEGATION_SCOPE);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), getServiceAccountUserId(), AGENT_CLIENT_ID);

        // remove delegation permission and refresh — may_act should be stripped
        removeDelegationPermission();
        res = oauth.client(TEST_CLIENT_ID, TEST_CLIENT_SECRET).scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeNotContains(res.getScope(), AGENT_DELEGATION_SCOPE);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        logout(res.getRefreshToken());
    }

    @Test
    public void delegationNotEnabled() throws Exception {
        removeDelegationPermission();

        AccessTokenResponse res = cibaLogin(AGENT_DELEGATION_SCOPE, "delegation-disabled", false);
        assertScopeNotContains(res.getScope(), AGENT_DELEGATION_SCOPE);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        logout(res.getRefreshToken());
    }

    @Test
    public void consentDenied() throws Exception {
        AuthenticationRequestAcknowledgement response = sendBackchannelAuthRequest(AGENT_DELEGATION_SCOPE, "consent-denied");

        CibaProvider.CibaAuthenticationChannelRequest channelReq = ciba.getAuthChannel("consent-denied");
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                oauth.ciba().doAuthenticationChannelCallback(channelReq.getBearerToken(), AuthenticationChannelResponse.Status.CANCELLED));

        ClientNotificationEndpointRequest notification = ciba.getPushedCibaClientNotification("client-notification-token");
        Assertions.assertEquals(notification.getAuthReqId(), response.getAuthReqId());

        AccessTokenResponse res = oauth.ciba().doBackchannelAuthenticationTokenRequest(response.getAuthReqId());
        Assertions.assertFalse(res.isSuccess());
        Assertions.assertEquals(OAuthErrorException.ACCESS_DENIED, res.getError());
    }

    private AuthenticationRequestAcknowledgement sendBackchannelAuthRequest(String scope, String bindingMessage) {
        oauth.scope(scope);
        AuthenticationRequestAcknowledgement response = oauth.ciba().backchannelAuthenticationRequest(USERNAME)
                .bindingMessage(bindingMessage)
                .clientNotificationToken("client-notification-token")
                .additionalParams(Map.of("user_device", "mobile"))
                .send();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getAuthReqId());
        return response;
    }

    private AccessTokenResponse cibaLogin(String scope, String bindingMessage, boolean expectConsent) throws Exception {
        AuthenticationRequestAcknowledgement response = sendBackchannelAuthRequest(scope, bindingMessage);

        CibaProvider.CibaAuthenticationChannelRequest channelReq = ciba.getAuthChannel(bindingMessage);
        if (expectConsent) {
            Assertions.assertTrue(channelReq.getRequest().getConsentRequired());
            assertScopeContains(channelReq.getRequest().getScope(), scope);
        }

        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                oauth.ciba().doAuthenticationChannelCallback(channelReq.getBearerToken(), AuthenticationChannelResponse.Status.SUCCEED));

        ClientNotificationEndpointRequest notification = ciba.getPushedCibaClientNotification("client-notification-token");
        Assertions.assertEquals(notification.getAuthReqId(), response.getAuthReqId());

        AccessTokenResponse res = oauth.ciba().doBackchannelAuthenticationTokenRequest(response.getAuthReqId());
        Assertions.assertTrue(res.isSuccess());

        if (expectConsent) {
            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.AUTHREQID_TO_TOKEN)
                    .hasSessionId()
                    .hasUserId()
                    .clientId(TEST_CLIENT_ID)
                    .details(Details.USERNAME, USERNAME)
                    .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
        }

        return res;
    }

    private void assertTokenExchangeSuccess(String subjectToken, String expectedActorId) {
        String actorToken = getActorToken();
        AccessTokenResponse exchangeRes = oauth.client(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET)
                .tokenExchangeRequest(subjectToken)
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .send();
        Assertions.assertTrue(exchangeRes.isSuccess(), exchangeRes.getError() + " - " + exchangeRes.getErrorDescription());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.TOKEN_EXCHANGE)
                .clientId(AGENT_CLIENT_ID)
                .hasUserId()
                .details(Details.USERNAME, USERNAME)
                .details(Details.ACTOR_TYPE, Details.ACTOR_TYPE_CLIENT)
                .details(Details.ACTOR, AGENT_CLIENT_ID)
                .details(Details.ACTOR_ID, expectedActorId)
                .details(Details.REQUESTED_TOKEN_TYPE, ACCESS_TOKEN_TYPE)
                .details(Details.SUBJECT_TOKEN_CLIENT_ID, TEST_CLIENT_ID);

        AccessToken teToken = oauth.verifyToken(exchangeRes.getAccessToken());
        Assertions.assertEquals(USERNAME, teToken.getPreferredUsername());
        assertActPresent(teToken, expectedActorId, AGENT_CLIENT_ID);
        Assertions.assertNull(teToken.getSessionId(), "Session is not transient");
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

    private void logout(String refreshToken) {
        LogoutResponse logout = oauth.client(TEST_CLIENT_ID, TEST_CLIENT_SECRET).doLogout(refreshToken);
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    private ScopePermissionRepresentation addDelegationPermission() {
        ClientResource adminPerms = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        ClientPolicyRepresentation policy = PermissionTestUtils.createClientPolicy(realm, adminPerms, "Agent Client Policy", AGENT_CLIENT_ID);
        return PermissionTestUtils.createAllPermission(adminPerms, AdminPermissionsSchema.USERS_RESOURCE_TYPE, policy, Set.of(AdminPermissionsSchema.DELEGATE));
    }

    private void removeDelegationPermission() {
        ClientResource adminPerms = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        adminPerms.authorization().permissions().scope().findAll(null, null, null, "*", null, null).stream()
                .filter(p -> p.getScopesData() != null && p.getScopesData().stream()
                        .anyMatch(s -> AdminPermissionsSchema.DELEGATE.equals(s.getName())))
                .forEach(p -> adminPerms.authorization().permissions().scope().findById(p.getId()).remove());
    }

    static class CibaServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES, Profile.Feature.TOKEN_EXCHANGE_DELEGATION)
                    .option("spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri",
                            "http://localhost:8500/ciba/request-authentication-channel");
        }
    }

    static class CibaRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.adminPermissionsEnabled(true)
                    .users(
                    UserBuilder.create(USERNAME).password(PASSWORD)
                            .email("test@localhost").firstName("Test").lastName("User"));
        }
    }

    static class CibaOAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client)
                    .defaultClientScopes("acr", "basic", "email", "profile")
                    .optionalClientScopes(OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE)
                    .consentRequired(true)
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString())
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "ping")
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, "http://localhost:8500/ciba/push-ciba-client-notification")
                    .attribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        }
    }
}
