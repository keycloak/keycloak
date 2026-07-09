package org.keycloak.tests.oauth;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantTypeFactory;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.ParameterizedScopeUserPropertyMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
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
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
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
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.testsuite.util.oauth.ciba.AuthenticationRequestAcknowledgement;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.representations.IDToken.ACT;
import static org.keycloak.representations.IDToken.MAY_ACT;
import static org.keycloak.representations.IDToken.PREFERRED_USERNAME;
import static org.keycloak.representations.JsonWebToken.SUBJECT;

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

    @InjectClient(config = AdminClientConfig.class)
    ManagedClient adminApp;

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

        // request delegation with a user that cannot impersonate
        AccessTokenResponse res = loginWithDelegation(scope, grants -> MatcherAssert.assertThat(grants,
                Matchers.not(Matchers.hasItem(Matchers.containsString("Delegate token")))));
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
        AccessTokenResponse res = loginWithDelegation(scope);

        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), administrator.getId());

        // refresh the token
        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), administrator.getId());

        // perform the token exchange with delegation
        tokenExchangeDelegationSuccess(res.getAccessToken(), getActorToken());

        // remove the impersonation and refresh the token
        administrator.admin().roles().clientLevel(clientUUID).remove(List.of(impersonation));
        res = oauth.doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeNotContains(res.getScope(), scope);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        // token exchange does not work without "may_act" claim
        String actorToken = getActorToken();
        AccessTokenResponse tokenExchangeRes = oauth.client("test-app", "test-secret").tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken).actorTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE).send();
        assertExchangeError(tokenExchangeRes, Errors.INVALID_TOKEN, "Invalid may_act claim in the subject_token");

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
            Assertions.assertEquals(201, response.getStatus(), "Mapper creation should succeed");
        }
        realm.cleanup().add(r -> {
            r.clientScopes().get(delegationScopeId).getProtocolMappers()
                    .getMappers().stream()
                    .filter(m -> "may_act preferred_username".equals(m.getName()))
                    .findFirst()
                    .ifPresent(m -> r.clientScopes().get(delegationScopeId).getProtocolMappers().delete(m.getId()));
        });

        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        AccessTokenResponse res = loginWithDelegation(scope);

        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        assertMayActPresent(token, administrator.getId(), null, administrator.getUsername());

        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);

        AccessToken refreshedToken = oauth.verifyToken(res.getAccessToken());
        assertMayActPresent(refreshedToken, administrator.getId(), null, administrator.getUsername());

        // perform the token exchange with delegation
        tokenExchangeDelegationSuccess(res.getAccessToken(), getActorToken(),
                teToken -> assertActPresent(teToken, administrator.getId(), null, administrator.getUsername()));

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

        // perform the token exchange with delegation
        tokenExchangeDelegationSuccess(res.getAccessToken(), getActorToken());

        // remove the impersonation and refresh the token
        administrator.admin().roles().clientLevel(clientUUID).remove(List.of(impersonation));
        res = oauth.doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeNotContains(res.getScope(), scope);
        assertMayActNotPresent(oauth.verifyToken(res.getAccessToken()));

        // token exchange does not work without "may_act" claim
        String actorToken = getActorToken();
        AccessTokenResponse tokenExchangeRes = oauth.client("test-app", "test-secret").tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken).actorTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE).send();
        assertExchangeError(tokenExchangeRes, Errors.INVALID_TOKEN, "Invalid may_act claim in the subject_token");

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void failIfDisabledActor() {
        addImpersonationToAdministrator();

        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        AccessTokenResponse res = loginWithDelegation(scope);

        ProtocolMapperRepresentation audienceMapper = adminApp.admin().getProtocolMappers().getMappers().iterator().next();
        adminApp.admin().getProtocolMappers().delete(audienceMapper.getId());
        adminApp.cleanup().add(c -> c.getProtocolMappers().createMapper(List.of(audienceMapper)));

        String actorToken = getActorToken();
        administrator.updateWithCleanup(user -> user.enabled(false));
        AccessTokenResponse tokenExchangeRes = oauth.client("test-app", "test-secret").tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken).actorTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE).send();
        assertExchangeError(tokenExchangeRes, null, Errors.INVALID_TOKEN, "actor_token validation failure");

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void failIfInvalidAudienceInActorToken() {
        addImpersonationToAdministrator();

        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        AccessTokenResponse res = loginWithDelegation(scope);

        ProtocolMapperRepresentation audienceMapper = adminApp.admin().getProtocolMappers().getMappers().iterator().next();
        adminApp.admin().getProtocolMappers().delete(audienceMapper.getId());
        adminApp.cleanup().add(c -> c.getProtocolMappers().createMapper(List.of(audienceMapper)));

        String actorToken = getActorToken();
        AccessTokenResponse tokenExchangeRes = oauth.client("test-app", "test-secret").tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken).actorTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE).send();
        assertExchangeError(tokenExchangeRes, Errors.NOT_ALLOWED, "client is not within the token audience");

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void failIfNoAccessTokenRequested() {
        addImpersonationToAdministrator();

        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        AccessTokenResponse res = loginWithDelegation(scope);

        String actorToken = getActorToken();
        AccessTokenResponse tokenExchangeRes = oauth.client("test-app", "test-secret").tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken).actorTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE)
                .requestedTokenType(OAuth2Constants.REFRESH_TOKEN_TYPE) // request refresh which is not valid
                .send();
        assertExchangeError(tokenExchangeRes, Errors.INVALID_REQUEST, "requested_token_type unsupported");

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void failIfOtherAdminInMayAct() {
        addImpersonationToAdministrator();

        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        AccessTokenResponse res = loginWithDelegation(scope);

        String actorToken = getActorToken("otheruser", PASSWORD);
        AccessTokenResponse tokenExchangeRes = oauth.client("test-app", "test-secret").tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken).actorTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE)
                .send();
        assertExchangeError(tokenExchangeRes, "otheruser", Errors.INVALID_TOKEN,
                "Actor user is not allowed by the may_act claim inside the subject_token");

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    @Test
    public void issuerInMayAct() {
        addImpersonationToAdministrator();

        // create a hardcoded claim to add the iss of the realm
        ProtocolMapperRepresentation issMapper = new ProtocolMapperRepresentation();
        issMapper.setName("iss-may-act-mapper");
        issMapper.setProtocol("openid-connect");
        issMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "may_act.iss");
        config.put(HardcodedClaim.CLAIM_VALUE, realm.getBaseUrl());
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        issMapper.setConfig(config);
        ClientScopeResource delegationScope = AdminApiUtil.findClientScopeByName(realm.admin(), OIDCLoginProtocolFactory.DELEGATION_SCOPE);
        String issMapperId = ApiUtil.getCreatedId(delegationScope.getProtocolMappers().createMapper(issMapper));
        issMapper.setId(issMapperId);
        realm.cleanup().add(r -> AdminApiUtil.findClientScopeByName(r, OIDCLoginProtocolFactory.DELEGATION_SCOPE)
                .getProtocolMappers().delete(issMapperId));

        final String scope = OIDCLoginProtocolFactory.DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + administrator.getUsername();
        AccessTokenResponse res = loginWithDelegation(scope);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), administrator.getId(), realm.getBaseUrl(), null);

        tokenExchangeDelegationSuccess(res.getAccessToken(), getActorToken(),
                token -> assertActPresent(token, administrator.getId(), realm.getBaseUrl(), null));

        // change the iss to other url
        issMapper.getConfig().put(HardcodedClaim.CLAIM_VALUE, "http://otheriss");
        delegationScope.getProtocolMappers().update(issMapperId, issMapper);

        // refresh the token
        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        assertScopeContains(res.getScope(), scope);
        assertMayActPresent(oauth.verifyToken(res.getAccessToken()), administrator.getId(), "http://otheriss", null);

        // check iss is wrong now
        String actorToken = getActorToken();
        AccessTokenResponse tokenExchangeRes = oauth.client("test-app", "test-secret").tokenExchangeRequest(res.getAccessToken())
                .actorToken(actorToken).actorTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE)
                .send();
        assertExchangeError(tokenExchangeRes, Errors.INVALID_TOKEN, "Invalid issuer in the may_act claim of the subject_token");

        // logout
        LogoutResponse logout = oauth.doLogout(res.getRefreshToken());
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    private void addImpersonationToAdministrator() {
        final ClientResource realmManagement = AdminApiUtil.findClientByClientId(realm.admin(), Constants.REALM_MANAGEMENT_CLIENT_ID);
        final String clientUUID = realmManagement.toRepresentation().getId();
        final RoleRepresentation impersonation = realmManagement.roles().get(AdminRoles.IMPERSONATION).toRepresentation();
        administrator.admin().roles().clientLevel(clientUUID).add(List.of(impersonation));
        administrator.cleanup().add(user -> user.roles().clientLevel(clientUUID).remove(List.of(impersonation)));
    }

    private void assertExchangeError(AccessTokenResponse tokenExchangeRes, String error, String reason) {
        assertExchangeError(tokenExchangeRes, administrator.getUsername(), error, reason);
    }

    private void assertExchangeError(AccessTokenResponse tokenExchangeRes, String actor, String error, String reason) {
        Assertions.assertFalse(tokenExchangeRes.isSuccess());
        EventAssertion.assertError(events.poll())
                .type(EventType.TOKEN_EXCHANGE_ERROR)
                .clientId("test-app")
                .hasUserId()
                .details(Details.USERNAME, USERNAME)
                .details(Details.ACTOR, actor)
                .error(error)
                .details(Details.REASON, reason)
                .details(Details.SUBJECT_TOKEN_CLIENT_ID, "test-app");
    }

    private void tokenExchangeDelegationSuccess(String subjectToken, String actorToken) {
        tokenExchangeDelegationSuccess(subjectToken, actorToken,
                token -> assertActPresent(token, administrator.getId(), null, null));
    }

    private void tokenExchangeDelegationSuccess(String subjectToken, String actorToken, Consumer<AccessToken> actValidator) {
        AccessTokenResponse tokenExchangeRes = oauth.client("test-app", "test-secret").tokenExchangeRequest(subjectToken)
                .actorToken(actorToken)
                .actorTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE)
                .send();
        Assertions.assertTrue(tokenExchangeRes.isSuccess());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.TOKEN_EXCHANGE)
                .clientId("test-app")
                .details(Details.USERNAME, USERNAME)
                .details(Details.ACTOR, administrator.getUsername())
                .details(Details.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                .details(Details.SUBJECT_TOKEN_CLIENT_ID, "test-app");

        AccessToken teToken = oauth.verifyToken(tokenExchangeRes.getAccessToken());
        Assertions.assertEquals(USERNAME, teToken.getPreferredUsername());
        actValidator.accept(teToken);
        Assertions.assertNull(teToken.getSessionId(), "Session is not transient");

        IntrospectionResponse introspectRes = oauth.doIntrospectionAccessTokenRequest(tokenExchangeRes.getAccessToken());
        Assertions.assertTrue(introspectRes.isSuccess());
        try {
            TokenMetadataRepresentation rep = introspectRes.asTokenMetadata();
            Assertions.assertEquals(USERNAME, rep.getUserName());
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    private AccessTokenResponse loginWithDelegation(String scope) {
        return loginWithDelegation(scope, grants -> MatcherAssert.assertThat(grants,
                Matchers.hasItem("Delegate token to administrator administrator?")));
    }

    private AccessTokenResponse loginWithDelegation(String scope, Consumer<List<String>> grantsValidator) {
        oauth.scope(scope).openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        grantsValidator.accept(grants);
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId("test-app")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);
        return res;
    }

    private String getActorToken() {
        return getActorToken(administrator.getUsername(), administrator.getPassword());
    }

    private String getActorToken(String username, String password) {
        String actorToken = oauth.client(adminApp.getClientId(), adminApp.getSecret()).scope(null)
                .doPasswordGrantRequest(username, password).getAccessToken();
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN).details(Details.USERNAME, username);
        return actorToken;
    }

    private static void assertMayActPresent(AccessToken token, String expectedActorId) {
        assertMayActPresent(token, expectedActorId, null, null);
    }

    @SuppressWarnings("unchecked")
    private static void assertMayActPresent(AccessToken token, String expectedActorId, String expectedIss, String expectedUsername) {
        Map<String, Object> mayAct = (Map<String, Object>) token.getOtherClaims().get(MAY_ACT);
        Assertions.assertNotNull(mayAct, "may_act claim should be present");
        Assertions.assertEquals(expectedActorId, mayAct.get(SUBJECT), "may_act.sub should contain the actor user ID");
        if (expectedIss != null) {
            Assertions.assertEquals(expectedIss, mayAct.get(OIDCLoginProtocol.ISSUER), "may_act.iss is not correct");
        } else {
            Assertions.assertNull(mayAct.get(OIDCLoginProtocol.ISSUER), "may_act.iss is not null");
        }
        if (expectedUsername != null) {
            Assertions.assertEquals(expectedUsername, mayAct.get(PREFERRED_USERNAME), "may_act.preferred_username is not correct");
        } else {
            Assertions.assertNull(mayAct.get(PREFERRED_USERNAME), "may_act.preferred_username is not null");
        }
    }

    private static void assertActPresent(AccessToken token, String expectedActorId, String expectedIss, String expectedUsername) {
        Map<String, Object> act = (Map<String, Object>) token.getOtherClaims().get(ACT);
        Assertions.assertNotNull(act, "act claim should be present");
        Assertions.assertEquals(expectedActorId, act.get(SUBJECT), "act.sub should contain the actor user ID");
        if (expectedIss != null) {
            Assertions.assertEquals(expectedIss, act.get(OIDCLoginProtocol.ISSUER), "act.iss is not correct");
        } else {
            Assertions.assertNull(act.get(OIDCLoginProtocol.ISSUER), "act.iss is not null");
        }
        if (expectedUsername != null) {
            Assertions.assertEquals(expectedUsername, act.get(PREFERRED_USERNAME), "act.preferred_username is not correct");
        } else {
            Assertions.assertNull(act.get(PREFERRED_USERNAME), "act.preferred_username is not null");
        }
    }

    private static void assertMayActNotPresent(AccessToken token) {
        Assertions.assertNull(token.getOtherClaims().get(MAY_ACT), "may_act claim should not be present");
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
                    .optionalClientScopes(OIDCLoginProtocolFactory.DELEGATION_SCOPE)
                    .consentRequired(true)
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString())
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

    static class AdminClientConfig implements ClientConfig {

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

            return client.clientId("admin-app").name("admin-app").secret("secret")
                    .directAccessGrantsEnabled()
                    .protocolMappers(audienceMapper);
        }
    }

}
