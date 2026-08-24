package org.keycloak.tests.oauth.tokenexchange;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
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
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.ExpectedActor;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuth2Constants.ACCESS_TOKEN_TYPE;
import static org.keycloak.representations.IDToken.ACT;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertActPresent;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertMayActNotPresent;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertMayActPresent;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertScopeContains;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertScopeNotContains;

/**
 * Tests the full 3-way client agent delegation flow as demonstrated in the token-exchange-delegation-demo:
 *
 * subject-app (public) -> actor-app (confidential, service account) -> resource-api (bearer-only)
 *
 * Covers audience resolution via role scope mappings, resource_access claims in the delegated token,
 * and end-to-end token exchange with a requested audience.
 */
@KeycloakIntegrationTest(config = ClientAgentDelegationTest.ServerConfig.class)
public class ClientAgentDelegationTest {

    static final String USERNAME = "test-user@localhost";
    static final String PASSWORD = "password";
    static final String SUBJECT_CLIENT_ID = "subject-app";
    static final String ACTOR_CLIENT_ID = "actor-app";
    static final String ACTOR_CLIENT_SECRET = "actor-secret";
    static final String RESOURCE_CLIENT_ID = "resource-api";
    static final String RESOURCE_CLIENT_SECRET = "resource-secret";
    static final String READ_DATA_ROLE = "read-data";
    static final String WRITE_DATA_ROLE = "write-data";
    static final String RESOURCE_ACCESS_SCOPE = "resource-api-access";
    static final String DELEGATION_SCOPE = OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE
            + ClientScopeModel.VALUE_SEPARATOR + ACTOR_CLIENT_ID;

    @InjectRealm(config = DelegationRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = SubjectAppConfig.class)
    OAuthClient oauth;

    @InjectClient(config = ActorAppConfig.class, ref = "actor")
    ManagedClient actorApp;

    @InjectClient(config = ResourceApiConfig.class, ref = "resource")
    ManagedClient resourceApi;

    @InjectEvents
    Events events;

    @InjectPage
    OAuthGrantPage grantPage;

    private ScopePermissionRepresentation delegationPermission;

    @BeforeEach
    public void beforeEach() {
        createResourceApiRoles();
        setupResourceApiAccessScope();
        assignReadDataRoleToUser();
        delegationPermission = addDelegationPermission();
    }

    @AfterEach
    public void afterEach() {
        AccountHelper.logout(realm.admin(), USERNAME);
        List<Map<String, Object>> consents = AccountHelper.getUserConsents(realm.admin(), USERNAME);
        if (consents.stream().anyMatch(m -> oauth.getClientId().equals(m.get("clientId")))) {
            AccountHelper.revokeConsents(realm.admin(), USERNAME, oauth.getClientId());
        }
    }

    @Test
    public void fullDelegationFlow() {
        AccessTokenResponse loginRes = loginWithDelegation();

        AccessToken subjectToken = oauth.verifyToken(loginRes.getAccessToken());
        String actorServiceAccountId = getServiceAccountUserId();

        // subject token: azp is subject-app
        Assertions.assertEquals(SUBJECT_CLIENT_ID, subjectToken.getIssuedFor(),
                "Subject token azp should be subject-app");

        // subject token: scope contains delegation:client:actor-app
        assertScopeContains(loginRes.getScope(), DELEGATION_SCOPE);

        // subject token: audience includes actor-app (added by ParameterizedScopeAudienceMapper)
        assertAudienceContains(subjectToken, ACTOR_CLIENT_ID);

        // subject token: audience does NOT include resource-api (Full Scope Allowed is off)
        assertAudienceNotContains(subjectToken, RESOURCE_CLIENT_ID);

        // subject token: contains may_act with actor's service account UUID and client_id
        assertMayActPresent(subjectToken, actorServiceAccountId, ACTOR_CLIENT_ID);

        // subject token: no act claim (not yet exchanged)
        Assertions.assertNull(subjectToken.getOtherClaims().get(ACT),
                "Subject token should not contain act claim");

        // perform token exchange with audience=resource-api
        String actorToken = getActorToken();
        AccessTokenResponse exchangeRes = oauth.client(ACTOR_CLIENT_ID, ACTOR_CLIENT_SECRET)
                .tokenExchangeRequest(loginRes.getAccessToken())
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .audience(RESOURCE_CLIENT_ID)
                .send();
        Assertions.assertTrue(exchangeRes.isSuccess(),
                exchangeRes.getError() + " - " + exchangeRes.getErrorDescription());
        events.poll();

        AccessToken delegatedToken = oauth.verifyToken(exchangeRes.getAccessToken());

        // delegated token: sub is the original user
        Assertions.assertEquals(USERNAME, delegatedToken.getPreferredUsername());

        // delegated token: azp is actor-app (the actor performed the exchange)
        Assertions.assertEquals(ACTOR_CLIENT_ID, delegatedToken.getIssuedFor(),
                "Delegated token azp should be actor-app");

        // delegated token: audience is resource-api (resolved via role scope mapping)
        assertAudienceContains(delegatedToken, RESOURCE_CLIENT_ID);

        // delegated token: audience does NOT include actor-app (shifted to resource-api)
        assertAudienceNotContains(delegatedToken, ACTOR_CLIENT_ID);

        // delegated token: scope contains profile (from actor-app default scopes)
        assertScopeContains(exchangeRes.getScope(), "profile");

        // delegated token: contains act claim (not may_act)
        assertActPresent(delegatedToken, actorServiceAccountId, ACTOR_CLIENT_ID);
        assertMayActNotPresent(delegatedToken);

        // delegated token: contains resource_access with read-data role
        AccessToken.Access resourceAccess = delegatedToken.getResourceAccess(RESOURCE_CLIENT_ID);
        Assertions.assertNotNull(resourceAccess, "resource_access for resource-api should be present");
        Assertions.assertTrue(resourceAccess.getRoles().contains(READ_DATA_ROLE),
                "resource_access should contain read-data role");

        // delegated token: session is transient (no refresh token)
        Assertions.assertNull(delegatedToken.getSessionId(), "Delegated token session should be transient");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void audienceNotAvailableWithoutRole() {
        // remove read-data role from user
        removeReadDataRoleFromUser();

        AccessTokenResponse loginRes = loginWithDelegation();
        assertScopeContains(loginRes.getScope(), DELEGATION_SCOPE);

        String actorToken = getActorToken();
        ExpectedActor clientActor = new ExpectedActor(Details.ACTOR_TYPE_CLIENT, ACTOR_CLIENT_ID, getServiceAccountUserId());
        AccessTokenResponse exchangeRes = oauth.client(ACTOR_CLIENT_ID, ACTOR_CLIENT_SECRET)
                .tokenExchangeRequest(loginRes.getAccessToken())
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .audience(RESOURCE_CLIENT_ID)
                .send();

        Assertions.assertFalse(exchangeRes.isSuccess(),
                "Token exchange should fail when user has no roles for the requested audience");
        EventAssertion.assertError(events.poll())
                .type(EventType.TOKEN_EXCHANGE_ERROR)
                .clientId(ACTOR_CLIENT_ID)
                .error(Errors.INVALID_REQUEST)
                .details(Details.REASON, "Requested audience not available: " + RESOURCE_CLIENT_ID)
                .details(Details.ACTOR_TYPE, clientActor.type())
                .details(Details.ACTOR, clientActor.actor())
                .details(Details.ACTOR_ID, clientActor.id());

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void delegationPermissionRemoved() {
        // remove delegation permission
        removeDelegationPermission();

        // login still succeeds but delegation scope is silently dropped
        AccessTokenResponse loginRes = loginWithDelegation(grants -> MatcherAssert.assertThat(grants,
                Matchers.not(Matchers.hasItem(Matchers.containsString("act on your behalf")))));

        Assertions.assertTrue(loginRes.isSuccess());
        assertScopeNotContains(loginRes.getScope(), DELEGATION_SCOPE);

        AccessToken subjectToken = oauth.verifyToken(loginRes.getAccessToken());
        assertMayActNotPresent(subjectToken);

        // token exchange fails without may_act
        String actorToken = getActorToken();
        ExpectedActor clientActor = new ExpectedActor(Details.ACTOR_TYPE_CLIENT, ACTOR_CLIENT_ID, getServiceAccountUserId());
        AccessTokenResponse exchangeRes = oauth.client(ACTOR_CLIENT_ID, ACTOR_CLIENT_SECRET)
                .tokenExchangeRequest(loginRes.getAccessToken())
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .audience(RESOURCE_CLIENT_ID)
                .send();
        Assertions.assertFalse(exchangeRes.isSuccess());
        EventAssertion.assertError(events.poll())
                .type(EventType.TOKEN_EXCHANGE_ERROR)
                .clientId(ACTOR_CLIENT_ID)
                .error(Errors.INVALID_TOKEN)
                .details(Details.REASON, "Invalid may_act claim in the subject_token")
                .details(Details.ACTOR_TYPE, clientActor.type())
                .details(Details.ACTOR, clientActor.actor())
                .details(Details.ACTOR_ID, clientActor.id());

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void delegationRevokedOnRefresh() {
        AccessTokenResponse loginRes = loginWithDelegation();
        assertScopeContains(loginRes.getScope(), DELEGATION_SCOPE);

        String actorServiceAccountId = getServiceAccountUserId();
        assertMayActPresent(oauth.verifyToken(loginRes.getAccessToken()), actorServiceAccountId, ACTOR_CLIENT_ID);

        // remove delegation permission and refresh
        removeDelegationPermission();
        AccessTokenResponse refreshRes = oauth.scope(null).doRefreshTokenRequest(loginRes.getRefreshToken());
        Assertions.assertTrue(refreshRes.isSuccess());
        assertScopeNotContains(refreshRes.getScope(), DELEGATION_SCOPE);
        assertMayActNotPresent(oauth.verifyToken(refreshRes.getAccessToken()));

        logout(refreshRes.getRefreshToken());
    }

    @Test
    public void exchangeWithScopeReduction() {
        AccessTokenResponse loginRes = loginWithDelegation();
        String actorToken = getActorToken();

        // exchange requesting only openid scope (no email)
        AccessTokenResponse exchangeRes = oauth.client(ACTOR_CLIENT_ID, ACTOR_CLIENT_SECRET)
                .scope("openid")
                .tokenExchangeRequest(loginRes.getAccessToken())
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .audience(RESOURCE_CLIENT_ID)
                .send();
        Assertions.assertTrue(exchangeRes.isSuccess(),
                exchangeRes.getError() + " - " + exchangeRes.getErrorDescription());
        events.poll();

        assertScopeNotContains(exchangeRes.getScope(), "email");

        AccessToken delegatedToken = oauth.verifyToken(exchangeRes.getAccessToken());
        assertAudienceContains(delegatedToken, RESOURCE_CLIENT_ID);
        assertActPresent(delegatedToken, getServiceAccountUserId(), ACTOR_CLIENT_ID);

        logout(loginRes.getRefreshToken());
    }

    // ===== Setup helpers =====

    private void createResourceApiRoles() {
        for (String roleName : List.of(READ_DATA_ROLE, WRITE_DATA_ROLE)) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            resourceApi.admin().roles().create(role);
            resourceApi.cleanup().add(client -> client.roles().deleteRole(roleName));
        }
    }

    private void setupResourceApiAccessScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName(RESOURCE_ACCESS_SCOPE);
        scopeRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String scopeId;
        try (Response response = realm.admin().clientScopes().create(scopeRep)) {
            scopeId = ApiUtil.getCreatedId(response);
        }
        final String finalScopeId = scopeId;

        RoleRepresentation readDataRole = resourceApi.admin().roles().get(READ_DATA_ROLE).toRepresentation();
        realm.admin().clientScopes().get(scopeId).getScopeMappings()
                .clientLevel(resourceApi.getId()).add(List.of(readDataRole));

        actorApp.admin().addDefaultClientScope(scopeId);
        realm.cleanup().add(r -> {
            r.clients().get(actorApp.getId()).removeDefaultClientScope(finalScopeId);
            r.clientScopes().get(finalScopeId).remove();
        });
    }

    private void assignReadDataRoleToUser() {
        RoleRepresentation readDataRole = resourceApi.admin().roles().get(READ_DATA_ROLE).toRepresentation();
        String userId = AdminApiUtil.findUserByUsername(realm.admin(), USERNAME).getId();
        realm.admin().users().get(userId).roles().clientLevel(resourceApi.getId()).add(List.of(readDataRole));
        realm.cleanup().add(r -> r.users().get(userId).roles().clientLevel(resourceApi.getId()).remove(List.of(readDataRole)));
    }

    private void removeReadDataRoleFromUser() {
        RoleRepresentation readDataRole = resourceApi.admin().roles().get(READ_DATA_ROLE).toRepresentation();
        String userId = AdminApiUtil.findUserByUsername(realm.admin(), USERNAME).getId();
        realm.admin().users().get(userId).roles().clientLevel(resourceApi.getId()).remove(List.of(readDataRole));
    }

    private ScopePermissionRepresentation addDelegationPermission() {
        ClientResource adminPerms = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        ClientPolicyRepresentation policy = PermissionTestUtils.createClientPolicy(
                realm, adminPerms, "Actor Client Delegation Policy", ACTOR_CLIENT_ID);
        ScopePermissionRepresentation permission = PermissionTestUtils.createAllPermission(adminPerms,
                AdminPermissionsSchema.USERS_RESOURCE_TYPE, policy, Set.of(AdminPermissionsSchema.DELEGATE));
        realm.cleanup().add(r -> {
            try {
                ClientResource perms = AdminApiUtil.findClientByClientId(r, Constants.ADMIN_PERMISSIONS_CLIENT_ID);
                perms.authorization().permissions().scope().findById(permission.getId()).remove();
            } catch (NotFoundException ignored) {
            }
        });
        return permission;
    }

    private void removeDelegationPermission() {
        ClientResource adminPerms = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        if (delegationPermission != null) {
            adminPerms.authorization().permissions().scope()
                    .findById(delegationPermission.getId()).remove();
            delegationPermission = null;
        }
    }

    // ===== Login / token helpers =====

    private AccessTokenResponse loginWithDelegation() {
        return loginWithDelegation(grants -> MatcherAssert.assertThat(grants,
                Matchers.hasItem("Allow " + ACTOR_CLIENT_ID + " to act on your behalf?")));
    }

    private AccessTokenResponse loginWithDelegation(Consumer<List<String>> grantsValidator) {
        oauth.client(SUBJECT_CLIENT_ID);
        oauth.scope(DELEGATION_SCOPE).openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        grantsValidator.accept(grants);
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(SUBJECT_CLIENT_ID)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);
        return res;
    }

    private String getActorToken() {
        AccessTokenResponse res = oauth.client(ACTOR_CLIENT_ID, ACTOR_CLIENT_SECRET).scope(null)
                .doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertTrue(res.isSuccess(), res.getError());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CLIENT_LOGIN)
                .clientId(ACTOR_CLIENT_ID);
        return res.getAccessToken();
    }

    private String getServiceAccountUserId() {
        return actorApp.admin().getServiceAccountUser().getId();
    }

    private void logout(String refreshToken) {
        LogoutResponse logout = oauth.client(SUBJECT_CLIENT_ID).doLogout(refreshToken);
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    // ===== Assertion helpers =====

    static void assertAudienceContains(AccessToken token, String expectedAudience) {
        String[] audiences = token.getAudience();
        Assertions.assertNotNull(audiences, "Token audience should not be null");
        MatcherAssert.assertThat("Token audience should contain " + expectedAudience,
                Arrays.asList(audiences), Matchers.hasItem(expectedAudience));
    }

    static void assertAudienceNotContains(AccessToken token, String unexpectedAudience) {
        String[] audiences = token.getAudience();
        if (audiences == null) return;
        MatcherAssert.assertThat("Token audience should not contain " + unexpectedAudience,
                Arrays.asList(audiences), Matchers.not(Matchers.hasItem(unexpectedAudience)));
    }

    // ===== Config classes =====

    static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES, Profile.Feature.TOKEN_EXCHANGE_DELEGATION);
        }
    }

    static class DelegationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.adminPermissionsEnabled(true)
                    .users(UserBuilder.create(USERNAME).password(PASSWORD)
                            .email("test@localhost").firstName("Test").lastName("User"));
        }
    }

    static class SubjectAppConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client)
                    .clientId(SUBJECT_CLIENT_ID).publicClient(true)
                    .defaultClientScopes("acr", "basic", "email", "profile")
                    .optionalClientScopes(OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE)
                    .consentRequired(true)
                    .fullScopeEnabled(false);
        }
    }

    static class ActorAppConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(ACTOR_CLIENT_ID).name("AI Agent").secret(ACTOR_CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .defaultClientScopes("acr", "basic", "profile", "roles")
                    .optionalClientScopes("email")
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString());
        }
    }

    static class ResourceApiConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(RESOURCE_CLIENT_ID).name("Backend API").secret(RESOURCE_CLIENT_SECRET)
                    .bearerOnly(true);
        }
    }
}
