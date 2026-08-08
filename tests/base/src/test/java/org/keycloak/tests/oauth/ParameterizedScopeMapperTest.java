package org.keycloak.tests.oauth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ParameterizedScopeMapper;
import org.keycloak.protocol.oidc.mappers.ParameterizedScopeUserPropertyMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientScopeBuilder;
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
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.oauth.ParameterizedScopeBuilder.create;

@DatabaseTest
@KeycloakIntegrationTest(config = ParameterizedScopeMapperTest.ServerConfig.class)
public class ParameterizedScopeMapperTest {

    private static final String SCOPE_NAME = "test-param-scope";
    private static final String SECOND_SCOPE_NAME = "other-param-scope";
    private static final String CLIENT_ID = "test-mapper-client";
    private static final String DEFAULT_USERNAME = "test-user@localhost";
    private static final String DEFAULT_PASSWORD = "password";
    private static final String TARGET_USERNAME = "target-user";
    private static final String UNPRIVILEGED_USERNAME = "unprivileged-user";
    private static final String RAW_PARAM_CLAIM = "param_value";
    private static final String USER_ID_CLAIM = "resolved_user_id";
    private static final String USER_EMAIL_CLAIM = "resolved_user_email";
    private static final String SECOND_RAW_PARAM_CLAIM = "other_param_value";
    private static final String SECOND_CLIENT_ID = "second-test-client";

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm realm;

    @InjectClient(config = TestClientConfig.class)
    ManagedClient client;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectUser(config = TargetUserConfig.class, ref = "target")
    ManagedUser targetUser;

    @InjectUser(config = SecondTargetUserConfig.class, ref = "secondTarget")
    ManagedUser secondTargetUser;

    @InjectUser(config = UnprivilegedUserConfig.class, ref = "unprivileged")
    ManagedUser unprivilegedUser;

    @InjectEvents
    Events events;

    @InjectPage
    OAuthGrantPage grantPage;

    @TestSetup
    public void setup() {
        String mainScopeId = createParameterizedScope(SCOPE_NAME);
        addMapper(mainScopeId, ParameterizedScopeMapper.create(
                "raw-param-mapper", RAW_PARAM_CLAIM, "String", true, false, true));
        addMapper(mainScopeId, ParameterizedScopeUserPropertyMapper.create(
                "user-id-mapper", "id", USER_ID_CLAIM, "String", true, false, true));
        addMapper(mainScopeId, ParameterizedScopeUserPropertyMapper.create(
                "user-email-mapper", "email", USER_EMAIL_CLAIM, "String", true, false, true));
        client.admin().addOptionalClientScope(mainScopeId);

        String secondScopeId = createParameterizedScope(SECOND_SCOPE_NAME);
        addMapper(secondScopeId, ParameterizedScopeMapper.create(
                "other-raw-param-mapper", SECOND_RAW_PARAM_CLAIM, "String", true, false, true));
        client.admin().addOptionalClientScope(secondScopeId);
    }

    @AfterEach
    public void afterEach() {
        for (String username : List.of(DEFAULT_USERNAME, UNPRIVILEGED_USERNAME)) {
            try {
                AccountHelper.logout(realm.admin(), username);
            } catch (Exception ignored) {
            }
            for (String cid : List.of(CLIENT_ID, SECOND_CLIENT_ID)) {
                try {
                    List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(realm.admin(), username);
                    if (userConsents.stream().anyMatch(m -> cid.equals(m.get("clientId")))) {
                        AccountHelper.revokeConsents(realm.admin(), username, cid);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    public void parameterizedScopeMapperRawValue() {
        AccessToken token = loginWithScopeParam(TARGET_USERNAME);
        assertRawParamClaim(token, TARGET_USERNAME);
    }

    @Test
    public void parameterizedScopeUserPropertyMapperId() {
        AccessToken token = loginWithScopeParam(TARGET_USERNAME);
        assertUserIdClaim(token, targetUser.getId());
    }

    @Test
    public void parameterizedScopeUserPropertyMapperEmail() {
        AccessToken token = loginWithScopeParam(TARGET_USERNAME);
        assertUserEmailClaim(token, "target@localhost");
    }

    @Test
    public void parameterizedScopeMapperNotPresent() {
        AccessTokenResponse res = loginAndGetResponse("openid");
        AccessToken token = oauth.verifyToken(res.getAccessToken());
        assertNoMappedClaims(token);
    }

    @Test
    public void parameterizedScopeMapperNonExistentUser() {
        AccessToken token = loginWithScopeParam("nonexistent");
        assertRawParamClaim(token, "nonexistent");
        assertNoUserClaims(token);
    }

    @Test
    public void userPropertyMapperWithViewUsersPermission() {
        addUsernameScopeWithUserPropertyMapper("authz-scope", false);

        AccessTokenResponse res = loginAndGetResponse("authz-scope:" + TARGET_USERNAME);
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        assertUserIdClaim(token, targetUser.getId());
    }

    @Test
    public void userPropertyMapperSuppressedWithoutViewUsersPermission() {
        addUsernameScopeWithUserPropertyMapper("authz-scope-denied", false);

        AccessTokenResponse res = loginAndGetResponse("authz-scope-denied:" + TARGET_USERNAME, UNPRIVILEGED_USERNAME);
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        Assertions.assertNull(token.getOtherClaims().get(USER_ID_CLAIM),
                "User id claim should not be present without view-users permission");
    }

    @Test
    public void userPropertyMapperAllowedWithAllowUserDataAccess() {
        addUsernameScopeWithUserPropertyMapper("allow-data-scope", true);

        AccessTokenResponse res = loginAndGetResponse("allow-data-scope:" + TARGET_USERNAME, UNPRIVILEGED_USERNAME);
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        assertUserIdClaim(token, targetUser.getId());
    }

    @Test
    public void claimsPersistOnRefresh() {
        AccessTokenResponse res = loginAndGetResponse(SCOPE_NAME + ":" + TARGET_USERNAME);

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        assertAllClaims(token, TARGET_USERNAME, targetUser.getId(), "target@localhost");

        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());

        AccessToken refreshedToken = oauth.verifyToken(res.getAccessToken());
        assertAllClaims(refreshedToken, TARGET_USERNAME, targetUser.getId(), "target@localhost");
    }

    @Test
    public void introspectionContainsClaims() throws IOException {
        AccessTokenResponse res = loginAndGetResponse(SCOPE_NAME + ":" + TARGET_USERNAME);

        IntrospectionResponse introspection = oauth.doIntrospectionAccessTokenRequest(res.getAccessToken());
        Assertions.assertTrue(introspection.isSuccess(),
                "Introspection failed with status " + introspection.getStatusCode()
                        + ": " + introspection.getError() + " - " + introspection.getErrorDescription());

        JsonNode json = introspection.asJsonNode();
        Assertions.assertTrue(json.get("active").asBoolean());
        Assertions.assertEquals(TARGET_USERNAME, json.get(RAW_PARAM_CLAIM).asText());
        Assertions.assertEquals(targetUser.getId(), json.get(USER_ID_CLAIM).asText());
        Assertions.assertEquals("target@localhost", json.get(USER_EMAIL_CLAIM).asText());
    }

    @Test
    public void nonParameterizedScopeMappersIgnored() {
        String nonParamScopeId = createNonParameterizedScopeWithMappers();
        client.admin().addOptionalClientScope(nonParamScopeId);
        realm.cleanup().add(r -> {
            r.clients().get(client.getId()).removeOptionalClientScope(nonParamScopeId);
            r.clientScopes().get(nonParamScopeId).remove();
        });

        AccessTokenResponse res = loginAndGetResponse("non-param-scope");
        AccessToken token = oauth.verifyToken(res.getAccessToken());
        assertNoMappedClaims(token);
    }

    @Test
    public void multipleParameterizedScopesMapIndependently() {
        AccessTokenResponse res = loginAndGetResponse(SCOPE_NAME + ":value-one " + SECOND_SCOPE_NAME + ":value-two");
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        Assertions.assertEquals("value-one", token.getOtherClaims().get(RAW_PARAM_CLAIM),
                "First scope's mapper should map its own parameter value");
        Assertions.assertEquals("value-two", token.getOtherClaims().get(SECOND_RAW_PARAM_CLAIM),
                "Second scope's mapper should map its own parameter value");
    }

    @Test
    public void repeatableScopeMapsMultipleValues() {
        String repScopeId = addOptionalScopeWithCleanup("rep-scope", true,
                ParameterizedScopeMapper.create("rep-raw-mapper", "rep_values", "String", true, false, true));

        AccessToken token = loginWithScopeParam("rep-scope", TARGET_USERNAME, "second-target");
        assertListClaim(token, "rep_values", TARGET_USERNAME, "second-target");
    }

    @Test
    public void repeatableScopeUserPropertyMapsMultipleUsers() {
        String repScopeId = addOptionalScopeWithCleanup("rep-user-scope", true,
                ParameterizedScopeUserPropertyMapper.create("rep-user-id-mapper", "id", "rep_user_ids", "String", true, false, true, true));

        AccessToken token = loginWithScopeParam("rep-user-scope", TARGET_USERNAME, secondTargetUser.getUsername());
        assertListClaim(token, "rep_user_ids", targetUser.getId(), secondTargetUser.getId());
    }

    @Test
    public void clientLevelMapperWithScopeCondition() {
        String scopeId = addOptionalScopeWithCleanup("client-cond-scope", false);

        ProtocolMapperModel mapper = ParameterizedScopeMapper.create(
                "client-raw-mapper", "client_param", "String", true, false, true, "client-cond-scope");
        addClientMapper(mapper);

        AccessTokenResponse res = loginAndGetResponse("client-cond-scope:myvalue");
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        Assertions.assertEquals("myvalue", token.getOtherClaims().get("client_param"),
                "Client-level mapper with scope.condition should produce the claim");
    }

    @Test
    public void clientLevelMapperNotExecutedWithoutScope() {
        String scopeId = addOptionalScopeWithCleanup("cond-not-granted", false);

        ProtocolMapperModel mapper = ParameterizedScopeMapper.create(
                "cond-not-granted-mapper", "cond_claim", "String", true, false, true, "cond-not-granted");
        addClientMapper(mapper);

        AccessTokenResponse res = loginAndGetResponse("openid");
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        Assertions.assertNull(token.getOtherClaims().get("cond_claim"),
                "Client-level mapper should not produce claim when scope is not granted");
    }

    @Test
    public void scopeConditionRejectedOnClientScopeMapper() {
        String scopeId = createParameterizedScope("reject-cond-scope");
        realm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());

        ProtocolMapperModel mapper = ParameterizedScopeMapper.create(
                "rejected-mapper", "rejected_claim", "String", true, false, true, "reject-cond-scope");
        ProtocolMapperRepresentation rep = ModelToRepresentation.toRepresentation(mapper);

        Assertions.assertThrows(BadRequestException.class,
                () -> realm.admin().clientScopes().get(scopeId).getProtocolMappers().createMapper(rep),
                "scope.condition should be rejected on client-scope-level mappers");
    }

    @Test
    public void scopeLevelAndClientLevelMappersComposeTogether() {
        String scopeId = createParameterizedScope("compose-scope");
        addMapper(scopeId, ParameterizedScopeMapper.create(
                "scope-level-mapper", "scope_claim", "String", true, false, true));
        client.admin().addOptionalClientScope(scopeId);
        realm.cleanup().add(r -> {
            r.clients().get(client.getId()).removeOptionalClientScope(scopeId);
            r.clientScopes().get(scopeId).remove();
        });

        ProtocolMapperModel clientMapper = ParameterizedScopeMapper.create(
                "client-level-mapper", "client_claim", "String", true, false, true, "compose-scope");
        addClientMapper(clientMapper);

        AccessTokenResponse res = loginAndGetResponse("compose-scope:myvalue");
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        Assertions.assertEquals("myvalue", token.getOtherClaims().get("scope_claim"),
                "Scope-level mapper should produce its claim");
        Assertions.assertEquals("myvalue", token.getOtherClaims().get("client_claim"),
                "Client-level mapper with scope.condition should also produce its claim");
    }

    @Test
    public void clientLevelUserPropertyMapperWithScopeCondition() {
        String scopeId = createUsernameTypeScope("client-user-cond", false);
        client.admin().addOptionalClientScope(scopeId);
        realm.cleanup().add(r -> {
            r.clients().get(client.getId()).removeOptionalClientScope(scopeId);
            r.clientScopes().get(scopeId).remove();
        });

        ProtocolMapperModel mapper = ParameterizedScopeUserPropertyMapper.create(
                "client-user-mapper", "email", "client_user_email", "String", true, false, true, false, "client-user-cond");
        addClientMapper(mapper);

        AccessTokenResponse res = loginAndGetResponse("client-user-cond:" + TARGET_USERNAME);
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        Assertions.assertEquals("target@localhost", token.getOtherClaims().get("client_user_email"),
                "Client-level user property mapper should resolve user and map claim");
    }

    @Test
    public void twoClientsDifferentClaimsForSameScope() {
        // Core issue scenario: two clients share scope "project", each with different claim mappings
        String scopeId = addOptionalScopeWithCleanup("project", false);

        // Client A (the default test client) maps parameter to "project_id"
        ProtocolMapperModel clientAMapper = ParameterizedScopeMapper.create(
                "client-a-project-mapper", "project_id", "String", true, false, true, "project");
        addClientMapper(clientAMapper);

        // Client B is a second client with its own mapper -> "active_project"
        ClientRepresentation secondClient = ClientBuilder.create()
                .clientId(SECOND_CLIENT_ID)
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .publicClient(false)
                .consentRequired(Boolean.TRUE)
                .redirectUris("*")
                .secret("password")
                .build();
        String secondClientUuid = ApiUtil.getCreatedId(realm.admin().clients().create(secondClient));
        realm.admin().clients().get(secondClientUuid).addOptionalClientScope(scopeId);
        realm.admin().clients().get(secondClientUuid).getProtocolMappers()
                .createMapper(ModelToRepresentation.toRepresentation(
                        ParameterizedScopeMapper.create("client-b-project-mapper", "active_project", "String", true, false, true, "project")));
        realm.cleanup().add(r -> r.clients().get(secondClientUuid).remove());

        // Client A should get "project_id" but not "active_project"
        AccessTokenResponse resA = loginAndGetResponse("project:alpha");
        AccessToken tokenA = oauth.verifyToken(resA.getAccessToken());
        Assertions.assertEquals("alpha", tokenA.getOtherClaims().get("project_id"),
                "Client A should produce project_id claim");
        Assertions.assertNull(tokenA.getOtherClaims().get("active_project"),
                "Client A should not produce Client B's claim");

        // Client B should get "active_project" but not "project_id"
        AccessTokenResponse resB = loginAndGetResponse("project:alpha", DEFAULT_USERNAME, SECOND_CLIENT_ID);
        AccessToken tokenB = oauth.verifyToken(resB.getAccessToken());
        Assertions.assertEquals("alpha", tokenB.getOtherClaims().get("active_project"),
                "Client B should produce active_project claim");
        Assertions.assertNull(tokenB.getOtherClaims().get("project_id"),
                "Client B should not produce Client A's claim");
    }

    @Test
    public void clientLevelMapperClaimsPersistOnRefresh() {
        String scopeId = addOptionalScopeWithCleanup("refresh-cond", false);

        ProtocolMapperModel mapper = ParameterizedScopeMapper.create(
                "refresh-mapper", "refresh_param", "String", true, false, true, "refresh-cond");
        addClientMapper(mapper);

        AccessTokenResponse res = loginAndGetResponse("refresh-cond:myvalue");
        AccessToken token = oauth.verifyToken(res.getAccessToken());
        Assertions.assertEquals("myvalue", token.getOtherClaims().get("refresh_param"));

        res = oauth.scope(null).doRefreshTokenRequest(res.getRefreshToken());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());

        AccessToken refreshedToken = oauth.verifyToken(res.getAccessToken());
        Assertions.assertEquals("myvalue", refreshedToken.getOtherClaims().get("refresh_param"),
                "Client-level conditional mapper claims should persist on refresh");
    }

    @Test
    public void clientLevelMapperIntrospection() throws IOException {
        String scopeId = addOptionalScopeWithCleanup("intro-cond", false);

        ProtocolMapperModel mapper = ParameterizedScopeMapper.create(
                "intro-mapper", "intro_param", "String", true, false, true, "intro-cond");
        addClientMapper(mapper);

        AccessTokenResponse res = loginAndGetResponse("intro-cond:myvalue");

        IntrospectionResponse introspection = oauth.doIntrospectionAccessTokenRequest(res.getAccessToken());
        Assertions.assertTrue(introspection.isSuccess());

        JsonNode json = introspection.asJsonNode();
        Assertions.assertTrue(json.get("active").asBoolean());
        Assertions.assertEquals("myvalue", json.get("intro_param").asText(),
                "Introspection should contain claim from client-level conditional mapper");
    }

    @Test
    public void scopeConditionWithNonParameterizedScopeProducesNoClaim() {
        ClientScopeRepresentation scope = ClientScopeBuilder.create()
                .name("non-param-cond")
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .attribute(ClientScopeModel.IS_PARAMETERIZED_SCOPE, Boolean.FALSE.toString())
                .attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, Boolean.TRUE.toString())
                .build();
        String scopeId = ApiUtil.getCreatedId(realm.admin().clientScopes().create(scope));
        client.admin().addOptionalClientScope(scopeId);
        realm.cleanup().add(r -> {
            r.clients().get(client.getId()).removeOptionalClientScope(scopeId);
            r.clientScopes().get(scopeId).remove();
        });

        ProtocolMapperModel mapper = ParameterizedScopeMapper.create(
                "non-param-cond-mapper", "non_param_claim", "String", true, false, true, "non-param-cond");
        addClientMapper(mapper);

        AccessTokenResponse res = loginAndGetResponse("non-param-cond");
        AccessToken token = oauth.verifyToken(res.getAccessToken());

        Assertions.assertNull(token.getOtherClaims().get("non_param_claim"),
                "scope.condition referencing a non-parameterized scope should not produce a claim");
    }

    // --- Helpers ---

    private AccessToken loginWithScopeParam(String paramValue) {
        AccessTokenResponse res = loginAndGetResponse(SCOPE_NAME + ":" + paramValue);
        return oauth.verifyToken(res.getAccessToken());
    }

    private AccessToken loginWithScopeParam(String scopeName, String... paramValues) {
        String scope = Arrays.stream(paramValues)
                .map(v -> scopeName + ":" + v)
                .collect(Collectors.joining(" "));
        AccessTokenResponse res = loginAndGetResponse(scope);
        return oauth.verifyToken(res.getAccessToken());
    }

    private String addOptionalScopeWithCleanup(String name, boolean repeatable, ProtocolMapperModel... mappers) {
        String id = createParameterizedScope(name, repeatable);
        for (ProtocolMapperModel mapper : mappers) {
            addMapper(id, mapper);
        }
        client.admin().addOptionalClientScope(id);
        realm.cleanup().add(r -> {
            r.clients().get(client.getId()).removeOptionalClientScope(id);
            r.clientScopes().get(id).remove();
        });
        return id;
    }

    @SuppressWarnings("unchecked")
    private static void assertListClaim(AccessToken token, String claimName, String... expectedValues) {
        Object claim = token.getOtherClaims().get(claimName);
        Assertions.assertInstanceOf(List.class, claim, claimName + " should be mapped as a list");
        List<String> values = (List<String>) claim;
        Assertions.assertEquals(expectedValues.length, values.size());
        for (String expected : expectedValues) {
            Assertions.assertTrue(values.contains(expected), claimName + " should contain " + expected);
        }
    }

    private AccessTokenResponse loginAndGetResponse(String scope) {
        return loginAndGetResponse(scope, DEFAULT_USERNAME, CLIENT_ID);
    }

    private AccessTokenResponse loginAndGetResponse(String scope, String username) {
        return loginAndGetResponse(scope, username, CLIENT_ID);
    }

    private AccessTokenResponse loginAndGetResponse(String scope, String username, String clientId) {
        oauth.client(clientId, "password");
        oauth.scope(scope);
        oauth.openLoginForm();
        oauth.fillLoginForm(username, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        grantPage.accept();
        events.poll();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        return res;
    }

    private String createParameterizedScope(String name) {
        return createParameterizedScope(name, false);
    }

    private String createParameterizedScope(String name, boolean repeatable) {
        ClientScopeRepresentation scope = create(name)
                .parameterizedScopeType("string")
                .isRepeatableScope(repeatable)
                .displayOnConsentScreen(true)
                .alwaysConsent(false)
                .build();
        return ApiUtil.getCreatedId(realm.admin().clientScopes().create(scope));
    }

    private String createUsernameTypeScope(String name, boolean allowUserDataAccess) {
        ClientScopeRepresentation scope = create(name)
                .parameterizedScopeType("username")
                .isRepeatableScope(false)
                .allowUserDataAccess(allowUserDataAccess)
                .displayOnConsentScreen(true)
                .alwaysConsent(false)
                .build();
        return ApiUtil.getCreatedId(realm.admin().clientScopes().create(scope));
    }

    private String addUsernameScopeWithUserPropertyMapper(String name, boolean allowUserDataAccess) {
        String scopeId = createUsernameTypeScope(name, allowUserDataAccess);
        addMapper(scopeId, ParameterizedScopeUserPropertyMapper.create(
                "username-user-id-mapper", "id", USER_ID_CLAIM, "String", true, false, true));
        client.admin().addOptionalClientScope(scopeId);
        realm.cleanup().add(r -> {
            r.clients().get(client.getId()).removeOptionalClientScope(scopeId);
            r.clientScopes().get(scopeId).remove();
        });
        return scopeId;
    }

    private String createNonParameterizedScopeWithMappers() {
        ClientScopeRepresentation scope = ClientScopeBuilder.create()
                .name("non-param-scope")
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .attribute(ClientScopeModel.IS_PARAMETERIZED_SCOPE, Boolean.FALSE.toString())
                .attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, Boolean.TRUE.toString())
                .build();
        String id = ApiUtil.getCreatedId(realm.admin().clientScopes().create(scope));
        addMapper(id, ParameterizedScopeMapper.create(
                "non-param-raw-mapper", RAW_PARAM_CLAIM, "String", true, false, true));
        addMapper(id, ParameterizedScopeUserPropertyMapper.create(
                "non-param-user-id-mapper", "id", USER_ID_CLAIM, "String", true, false, true));
        return id;
    }

    private void addMapper(String clientScopeId, ProtocolMapperModel mapper) {
        realm.admin().clientScopes().get(clientScopeId).getProtocolMappers()
                .createMapper(ModelToRepresentation.toRepresentation(mapper));
    }

    private void addClientMapper(ProtocolMapperModel mapper) {
        ProtocolMapperRepresentation rep = ModelToRepresentation.toRepresentation(mapper);
        realm.admin().clients().get(client.getId()).getProtocolMappers().createMapper(rep);
        realm.cleanup().add(r -> {
            List<ProtocolMapperRepresentation> mappers = r.clients().get(client.getId()).getProtocolMappers().getMappers();
            mappers.stream()
                    .filter(m -> rep.getName().equals(m.getName()))
                    .findFirst()
                    .ifPresent(m -> r.clients().get(client.getId()).getProtocolMappers().delete(m.getId()));
        });
    }

    private static void assertRawParamClaim(AccessToken token, String expectedValue) {
        Assertions.assertEquals(expectedValue, token.getOtherClaims().get(RAW_PARAM_CLAIM),
                "Raw param claim should contain the scope parameter value");
    }

    private static void assertUserIdClaim(AccessToken token, String expectedId) {
        Assertions.assertEquals(expectedId, token.getOtherClaims().get(USER_ID_CLAIM),
                "User id claim should contain the resolved user's ID");
    }

    private static void assertUserEmailClaim(AccessToken token, String expectedEmail) {
        Assertions.assertEquals(expectedEmail, token.getOtherClaims().get(USER_EMAIL_CLAIM),
                "User email claim should contain the resolved user's email");
    }

    private static void assertAllClaims(AccessToken token, String rawParam, String userId, String email) {
        assertRawParamClaim(token, rawParam);
        assertUserIdClaim(token, userId);
        assertUserEmailClaim(token, email);
    }

    private static void assertNoUserClaims(AccessToken token) {
        Assertions.assertNull(token.getOtherClaims().get(USER_ID_CLAIM),
                "User id claim should not be present");
        Assertions.assertNull(token.getOtherClaims().get(USER_EMAIL_CLAIM),
                "User email claim should not be present");
    }

    private static void assertNoMappedClaims(AccessToken token) {
        Assertions.assertNull(token.getOtherClaims().get(RAW_PARAM_CLAIM),
                "Raw param claim should not be present");
        assertNoUserClaims(token);
    }

    // --- Config classes ---

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES);
        }
    }

    static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create(DEFAULT_USERNAME)
                    .email(DEFAULT_USERNAME)
                    .name("Test", "User")
                    .emailVerified(true)
                    .password(DEFAULT_PASSWORD)
                    .enabled(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_USERS));
        }
    }

    static class TestClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(CLIENT_ID)
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .publicClient(false)
                    .consentRequired(Boolean.TRUE)
                    .redirectUris("*")
                    .secret("password")
                    .attribute(OIDCConfigAttributes.ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK, "true");
        }
    }

    static class TargetUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username(TARGET_USERNAME)
                    .password(DEFAULT_PASSWORD)
                    .email("target@localhost")
                    .name("Target", "User");
        }
    }

    static class SecondTargetUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("second-target-user")
                    .password(DEFAULT_PASSWORD)
                    .email("second-target@localhost")
                    .name("Second", "Target");
        }
    }

    static class UnprivilegedUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username(UNPRIVILEGED_USERNAME)
                    .password(DEFAULT_PASSWORD)
                    .email("unprivileged@localhost")
                    .name("Unprivileged", "User");
        }
    }
}
