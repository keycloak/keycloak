package org.keycloak.tests.oauth;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantTypeFactory;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.protocol.oidc.mappers.ParameterizedScopeUserPropertyMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.CibaProvider;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectCibaProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.testsuite.util.oauth.ciba.AuthenticationRequestAcknowledgement;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.representations.IDToken.MAY_ACT;
import static org.keycloak.representations.IDToken.PREFERRED_USERNAME;
import static org.keycloak.representations.JsonWebToken.SUBJECT;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = TokenExchangeDelegationTest.TokenExchangeDelegationServerConfig.class)
public class TokenExchangeDelegationTest {

    private static final String USERNAME = "test-user@localhost";
    private static final String PASSWORD = "password";

    @InjectRealm(config = TokenExchangeDelegationRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = TestOAuthClientConfig.class)
    OAuthClient oauth;

    @InjectUser(config = AdministratorUserConfig.class)
    ManagedUser administrator;

    @InjectEvents
    protected Events events;

    @InjectCibaProvider
    protected CibaProvider ciba;

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
    public void delegationNoImpersonation() {
        // request delegation with a user that cannot impersonate — scope is silently dropped
        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        oauth.scope(scope).openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        MatcherAssert.assertThat(grants, Matchers.not(Matchers.hasItem(Matchers.containsString("Delegate token"))));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId("test-app")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        // delegation scope should not be present as user cannot impersonate
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeNotContains(res.getScope(), scope);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void delegation() {
        final ClientResource realmManagement = AdminApiUtil.findClientByClientId(realm.admin(), Constants.REALM_MANAGEMENT_CLIENT_ID);
        final String clientUUID = realmManagement.toRepresentation().getId();
        final RoleRepresentation impersonation = realmManagement.roles().get(AdminRoles.IMPERSONATION).toRepresentation();
        administrator.admin().roles().clientLevel(clientUUID).add(List.of(impersonation));

        // request the delegation to administrator and accept the delegation
        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        oauth.scope(scope).openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        MatcherAssert.assertThat(grants, Matchers.hasItem("Delegate token to administrator administrator?"));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId("test-app")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        // get the code and obtain the scope
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), administrator.getId());

        // refresh the token
        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), administrator.getId());

        // remove the impersonation and refresh the token
        administrator.admin().roles().clientLevel(clientUUID).remove(List.of(impersonation));
        res = oauth.doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeNotContains(res.getScope(), scope);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void delegationWithPreferredUsernameMapper() {
        final ClientResource realmManagement = AdminApiUtil.findClientByClientId(realm.admin(), Constants.REALM_MANAGEMENT_CLIENT_ID);
        final String clientUUID = realmManagement.toRepresentation().getId();
        final RoleRepresentation impersonation = realmManagement.roles().get(AdminRoles.IMPERSONATION).toRepresentation();
        administrator.admin().roles().clientLevel(clientUUID).add(List.of(impersonation));

        realm.cleanup().add(r -> r.users().get(administrator.getId()).roles().clientLevel(clientUUID).remove(List.of(impersonation)));
        String delegationScopeId = findDelegationScopeId();
        ProtocolMapperModel preferredUsernameMapper = ParameterizedScopeUserPropertyMapper.create(
                "may_act preferred_username", "username",
                MAY_ACT + "." + PREFERRED_USERNAME, "String",
                true, true, true);

        try (var response = realm.admin().clientScopes().get(delegationScopeId).getProtocolMappers()
                .createMapper(ModelToRepresentation.toRepresentation(preferredUsernameMapper))) {
            assertEquals(201, response.getStatus(), "Mapper creation should succeed");
        }
        realm.cleanup().add(r -> {
            r.clientScopes().get(delegationScopeId).getProtocolMappers()
                    .getMappers().stream()
                    .filter(m -> "may_act preferred_username".equals(m.getName()))
                    .findFirst()
                    .ifPresent(m -> r.clientScopes().get(delegationScopeId).getProtocolMappers().delete(m.getId()));
        });

        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        oauth.scope(scope).openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId("test-app")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        assertMayActPresent(token, administrator.getId());
        assertMayActPreferredUsernamePresent(token, administrator.getUsername());

        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);

        AccessToken refreshedToken = oauth.verifyToken(res.getAccessToken());
        assertMayActPresent(refreshedToken, administrator.getId());
        assertMayActPreferredUsernamePresent(refreshedToken, administrator.getUsername());

        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void cibaDelegationNoImpersonation() throws Exception {
        // request delegation with a user that cannot impersonate — scope is silently dropped
        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        oauth.scope(scope);
        AuthenticationRequestAcknowledgement response = oauth.ciba().backchannelAuthenticationRequest(USERNAME)
                .bindingMessage("asdfghjkl")
                .clientNotificationToken("client-notification-token")
                .additionalParams(Map.of("user_device", "mobile"))
                .send();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getAuthReqId());

        CibaProvider.CibaAuthenticationChannelRequest clientAuthenticationChannelReq = ciba.getAuthChannel("asdfghjkl");
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                oauth.ciba().doAuthenticationChannelCallback(clientAuthenticationChannelReq.getBearerToken(), AuthenticationChannelResponse.Status.SUCCEED));

        ClientNotificationEndpointRequest pushedClientNotification = ciba.getPushedCibaClientNotification("client-notification-token");
        Assertions.assertEquals(pushedClientNotification.getAuthReqId(), response.getAuthReqId());

        // delegation scope should not be present as user cannot impersonate
        AccessTokenResponse res = oauth.ciba().doBackchannelAuthenticationTokenRequest(response.getAuthReqId());
        Assertions.assertTrue(res.isSuccess());
        assertScopeNotContains(res.getScope(), scope);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void cibaDelegation() throws Exception {
        final ClientResource realmManagement = AdminApiUtil.findClientByClientId(realm.admin(), Constants.REALM_MANAGEMENT_CLIENT_ID);
        final String clientUUID = realmManagement.toRepresentation().getId();
        final RoleRepresentation impersonation = realmManagement.roles().get(AdminRoles.IMPERSONATION).toRepresentation();
        administrator.admin().roles().clientLevel(clientUUID).add(List.of(impersonation));

        // client Backchannel Authentication Request
        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        oauth.scope(scope);
        AuthenticationRequestAcknowledgement response = oauth.ciba().backchannelAuthenticationRequest(USERNAME)
                .bindingMessage("asdfghjkl")
                .clientNotificationToken("client-notification-token")
                .additionalParams(Map.of("user_device", "mobile"))
                .send();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getAuthReqId());

        // client Authentication Channel Request
        CibaProvider.CibaAuthenticationChannelRequest clientAuthenticationChannelReq = ciba.getAuthChannel("asdfghjkl");
        Assertions.assertTrue(clientAuthenticationChannelReq.getRequest().getConsentRequired());
        assertScopeContains(clientAuthenticationChannelReq.getRequest().getScope(), scope);

        // client Authentication Channel completed
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                oauth.ciba().doAuthenticationChannelCallback(clientAuthenticationChannelReq.getBearerToken(), AuthenticationChannelResponse.Status.SUCCEED));

        // Check clientNotification exists now for our authReqId
        ClientNotificationEndpointRequest pushedClientNotification = ciba.getPushedCibaClientNotification("client-notification-token");
        Assertions.assertEquals(pushedClientNotification.getAuthReqId(), response.getAuthReqId());

        // client Token Request should be OK now
        AccessTokenResponse res = oauth.ciba().doBackchannelAuthenticationTokenRequest(response.getAuthReqId());
        Assertions.assertTrue(res.isSuccess());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.AUTHREQID_TO_TOKEN)
                .hasSessionId()
                .hasIpAddress()
                .hasCodeId()
                .hasUserId()
                .clientId(oauth.getClientId())
                .hasTokenId(Details.REFRESH_TOKEN_ID)
                .hasAccessTokenId(CibaGrantTypeFactory.GRANT_SHORTCUT)
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
       assertScopeContains(res.getScope(), scope);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), administrator.getId());

        // refresh the token
        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), administrator.getId());

        // remove the impersonation and refresh the token
        administrator.admin().roles().clientLevel(clientUUID).remove(List.of(impersonation));
        res = oauth.doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeNotContains(res.getScope(), scope);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @SuppressWarnings("unchecked")
    private static void assertMayActPresent(AccessToken token, String expectedActorId) {
        Map<String, Object> mayAct = (Map<String, Object>) token.getOtherClaims().get(MAY_ACT);
        Assertions.assertNotNull(mayAct, "may_act claim should be present");
        Assertions.assertEquals(expectedActorId, mayAct.get(SUBJECT), "may_act.sub should contain the actor user ID");
    }

    private static void assertMayActNotPresent(AccessToken token) {
        Assertions.assertNull(token.getOtherClaims().get(MAY_ACT), "may_act claim should not be present");
    }

    @SuppressWarnings("unchecked")
    private static void assertMayActPreferredUsernamePresent(AccessToken token, String expectedUsername) {
        Map<String, Object> mayAct = (Map<String, Object>) token.getOtherClaims().get(MAY_ACT);
        Assertions.assertNotNull(mayAct, "may_act claim should be present");
        Assertions.assertEquals(expectedUsername, mayAct.get(PREFERRED_USERNAME),
                "may_act.preferred_username should contain the actor username");
    }

    private String findDelegationScopeId() {
        return realm.admin().clientScopes().findAll().stream()
                .filter(cs -> OIDCLoginProtocolFactory.DELEGATION_SCOPE.equals(cs.getName()))
                .map(ClientScopeRepresentation::getId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("delegation client scope not found"));
    }

    private static void assertScopeContains(String scopeString, String expectedScope) {
        Assertions.assertNotNull(scopeString, "Scope string should not be null");
        MatcherAssert.assertThat(Arrays.asList(scopeString.split(" ")), Matchers.hasItem(expectedScope));
    }

    private static void assertScopeNotContains(String scopeString, String expectedScope) {
        Assertions.assertNotNull(scopeString, "Scope string should not be null");
        MatcherAssert.assertThat(Arrays.asList(scopeString.split(" ")), Matchers.not(Matchers.hasItem(expectedScope)));
    }

    static class TokenExchangeDelegationServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES, Profile.Feature.TOKEN_EXCHANGE_DELEGATION)
                    .option("spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri",
                            "http://localhost:8500/ciba/request-authentication-channel");
        }
    }

    static class TokenExchangeDelegationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create(USERNAME).password(PASSWORD)
                    .email("test@localhost").firstName("Test").lastName("User"));
        }
    }

    static class TestOAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client)
                    .defaultClientScopes("acr", "basic", "email", "profile")
                    .optionalClientScopes(OIDCLoginProtocolFactory.DELEGATION_SCOPE)
                    .consentRequired(true)
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "ping")
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, "http://localhost:8500/ciba/push-ciba-client-notification")
                    .attribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        }
    }

    static class AdministratorUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("administrator").password(PASSWORD)
                    .email("administrator@localhost").firstName("Administrator").lastName("User");
        }
    }

}
