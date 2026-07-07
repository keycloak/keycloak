package org.keycloak.tests.oauth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ParameterizedScopeMapper;
import org.keycloak.protocol.oidc.mappers.ParameterizedScopeUserPropertyMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
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
    private static final String RAW_PARAM_CLAIM = "param_value";
    private static final String USER_ID_CLAIM = "resolved_user_id";
    private static final String USER_EMAIL_CLAIM = "resolved_user_email";
    private static final String SECOND_RAW_PARAM_CLAIM = "other_param_value";

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
        try {
            AccountHelper.logout(realm.admin(), DEFAULT_USERNAME);
        } catch (Exception ignored) {
        }
        try {
            List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
            if (userConsents.stream().anyMatch(m -> CLIENT_ID.equals(m.get("clientId")))) {
                AccountHelper.revokeConsents(realm.admin(), DEFAULT_USERNAME, CLIENT_ID);
            }
        } catch (Exception ignored) {
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
        oauth.client(CLIENT_ID, "password");
        oauth.scope(scope);
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
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
                    .enabled(true));
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
}
