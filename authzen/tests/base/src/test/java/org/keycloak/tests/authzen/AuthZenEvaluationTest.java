/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.authzen;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.authzen.client.AuthZenClient;
import org.keycloak.testframework.authzen.client.AuthZenClient.EvaluationResult;
import org.keycloak.testframework.authzen.client.annotations.InjectAuthZenClient;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.authzen.AuthZen.SubjectType.CLIENT;
import static org.keycloak.authorization.authzen.AuthZen.SubjectType.USER;
import static org.keycloak.authorization.authzen.AuthZenRequestIdFilter.X_REQUEST_ID;
import static org.keycloak.authorization.authzen.AuthZenWellKnownProvider.accessEvaluationEndpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = AuthZenServerConfig.class)
public class AuthZenEvaluationTest {

    private static final String ADMIN_USER = "admin-user";
    private static final String REGULAR_USER = "regular-user";

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm realm;

    @InjectClient(ref = "authzen-client", config = AuthzClientConfig.class)
    ManagedClient client;

    @InjectClient(ref = "no-authz-client", config = NoAuthzClientConfig.class)
    ManagedClient noAuthzClient;

    @InjectClient(ref = "subject-id-client", config = AuthzClientConfig.class)
    ManagedClient subjectClient;

    @InjectClient(ref = "disableable-client", config = AuthzClientConfig.class)
    ManagedClient disableableClient;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectAuthZenClient
    AuthZenClient authZenClient;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @TestSetup
    public void setup() {
        configureAuthorizationResources();
    }

    // Resource properties are not used by Keycloak but are accepted and ignored per the AuthZen spec
    @Test
    public void testAdminUserAccessAdminResource() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .resourceProperty("ignored", "property")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
        assertNull(result.header(X_REQUEST_ID));
    }

    @Test
    public void testRegularUserDeniedAdminResource() throws IOException {
        EvaluationResult result = authzenClient("regular-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, REGULAR_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
        assertNull(result.header(X_REQUEST_ID));
    }

    @Test
    public void testAdminUserAccessUsersResource() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/users")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testRegularUserAccessUsersResource() throws IOException {
        EvaluationResult result = authzenClient("regular-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, REGULAR_USER)
                    .action("read")
                    .resource("endpoint", "/users")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testContextPassedToClaims() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/context-protected")
                    .contextProperty("environment", "production")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testContextMismatchDeniesAccess() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/context-protected")
                    .contextProperty("environment", "staging")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testMissingContextDeniesAccess() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/context-protected")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testNestedContextPassedToClaims() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/nested-context")
                    .contextProperty("request", Map.of("ip", "10.0.0.1"))
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testNestedContextMismatchDeniesAccess() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/nested-context")
                    .contextProperty("request", Map.of("ip", "192.168.1.1"))
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testUnknownResourceReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/nonexistent")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    // Resource exists but the requested type does not match the resource's configured type
    @Test
    public void testResourceTypeMismatchReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("wrong-type", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    // Resource has an empty type in Keycloak; an empty type string in the request should match
    @Test
    public void testEmptyTypeResourceMatchesEmptyType() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("", "/empty-type")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    // The /scope-limited resource defines both "read" and "write" scopes, but only the "read"
    // scope has a permission granting access. Requesting the "write" action should be denied.
    @Test
    public void testActionWithoutPermissionDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("write")
                    .resource("endpoint", "/scope-limited")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    // The authenticated user (regular-user) evaluates whether a different user subject (admin-user)
    // is authorized to access a protected resource
    @Test
    public void testClientEvaluatesUserSubjectAuthorized() throws IOException {
        EvaluationResult result = authzenClient("regular-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    // The authenticated client (authzen-client) evaluates whether a user subject without
    // the required role is denied access
    @Test
    public void testClientEvaluatesUserSubjectDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, REGULAR_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    // A CLIENT subject whose id matches the token's client evaluates against its own resource server
    @Test
    public void testClientSubjectMatchingTokenAuthorized() throws IOException {
        AccessTokenResponse tokenResponse = oauth
              .client(subjectClient.getClientId(), subjectClient.getSecret())
              .doClientCredentialsGrantAccessTokenRequest();

        EvaluationResult result = authZenClient.withAccessToken(tokenResponse.getAccessToken())
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(CLIENT, "subject-id-client")
                    .action("read")
                    .resource("endpoint", "/subject-only")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    // A CLIENT subject whose id does not match the token's client returns decision:false
    @Test
    public void testClientSubjectMismatchingTokenReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(CLIENT, "subject-id-client")
                    .action("read")
                    .resource("endpoint", "/subject-only")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    // A client obtains a token, is then disabled, and attempts to use the stale token to request authorization.
    @Test
    public void testDisabledClientWithStaleTokenReturnsUnauthorized() throws IOException {
        AccessTokenResponse tokenResponse = oauth
              .client(disableableClient.getClientId(), disableableClient.getSecret())
              .doClientCredentialsGrantAccessTokenRequest();

        disableableClient.updateWithCleanup(c -> c.enabled(false));

        EvaluationResult result = authZenClient.withAccessToken(tokenResponse.getAccessToken())
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(CLIENT, "disableable-client")
                    .action("read")
                    .resource("endpoint", "/open")
                    .build());

        assertEquals(401, result.statusCode());
    }

    // Subject properties are merged into identity attributes. The /subject-protected resource
    // requires a "department" identity attribute matching "engineering".
    @Test
    public void testSubjectPropertiesPassedToIdentityAttributes() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .subjectProperty("department", "engineering")
                    .action("read")
                    .resource("endpoint", "/subject-protected")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testSubjectPropertiesMismatchDeniesAccess() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .subjectProperty("department", "marketing")
                    .action("read")
                    .resource("endpoint", "/subject-protected")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testMissingSubjectPropertiesDeniesAccess() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/subject-protected")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testUnauthenticatedUserReturnsUnauthorized() throws IOException {
        EvaluationResult result = authZenClient.evaluate(AuthZenClient.evaluationRequest()
              .subject(USER, ADMIN_USER)
              .action("read")
              .resource("endpoint", "/admin")
              .build());

        assertEquals(401, result.statusCode());
    }

    @Test
    public void testMissingSubjectReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testMissingSubjectTypeReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(null, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testMissingSubjectIdReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, null)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testUnknownUserSubjectReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "nonexistent-user")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testUnknownClientSubjectReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(CLIENT, "nonexistent-client")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testInvalidSubjectTypeReturnsBadRequest() throws IOException {
        String url = accessEvaluationEndpoint(realmUrl());
        AccessTokenResponse tokenResponse = oauth
              .client(client.getClientId(), client.getSecret())
              .doPasswordGrantRequest("admin-user", "password");

        String json = """
              {"subject":{"type":"invalid-type","id":"%s"},\
              "resource":{"type":"endpoint","id":"/admin"},\
              "action":{"name":"read"}}""".formatted(ADMIN_USER);

        try (SimpleHttpResponse response = simpleHttp.doPost(url)
              .auth(tokenResponse.getAccessToken())
              .header("Content-Type", "application/json")
              .entity(new StringEntity(json))
              .asResponse()) {
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    public void testMissingResourceReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testMissingResourceTypeReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource(null, "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testMissingResourceIdReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", null)
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testMissingActionReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testUndefinedActionReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("undefined-action")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testClientWithoutAuthorizationReturnsDenied() throws IOException {
        AccessTokenResponse tokenResponse = oauth
              .client(noAuthzClient.getClientId(), noAuthzClient.getSecret())
              .doPasswordGrantRequest("admin-user", "password");

        EvaluationResult result = authZenClient.withAccessToken(tokenResponse.getAccessToken())
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testInvalidJsonReturnsBadRequest() throws IOException {
        String url = accessEvaluationEndpoint(realmUrl());
        AccessTokenResponse tokenResponse = oauth
              .client(client.getClientId(), client.getSecret())
              .doPasswordGrantRequest("admin-user", "password");

        try (SimpleHttpResponse response = simpleHttp.doPost(url)
              .auth(tokenResponse.getAccessToken())
              .header("Content-Type", "application/json")
              .entity(new StringEntity("{invalid json"))
              .asResponse()) {
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    public void testXRequestIdEchoedInResponse() throws IOException {
        String requestId = "test-request-id-12345";
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(
                    AuthZenClient.evaluationRequest()
                          .subject(USER, ADMIN_USER)
                          .action("read")
                          .resource("endpoint", "/admin")
                          .build(),
                    Map.of(X_REQUEST_ID, requestId)
              );

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
        assertEquals(requestId, result.header(X_REQUEST_ID));
    }

    @Test
    public void testXRequestIdEchoedOnUnauthorizedResponse() throws IOException {
        String requestId = "unauth-request-id";

        EvaluationResult result = new AuthZenClient(simpleHttp, realmUrl())
              .evaluate(
                    AuthZenClient.evaluationRequest()
                          .subject(USER, ADMIN_USER)
                          .action("read")
                          .resource("endpoint", "/admin")
                          .build(),
                    Map.of(X_REQUEST_ID, requestId)
              );

        assertEquals(401, result.statusCode());
        assertEquals(requestId, result.header(X_REQUEST_ID));
    }

    @Test
    public void testXRequestIdEchoedOnBadRequestResponse() throws IOException {
        String requestId = "bad-request-id";
        String url = accessEvaluationEndpoint(realmUrl());
        AccessTokenResponse tokenResponse = oauth
              .client(client.getClientId(), client.getSecret())
              .doPasswordGrantRequest("admin-user", "password");

        try (SimpleHttpResponse response = simpleHttp.doPost(url)
              .auth(tokenResponse.getAccessToken())
              .header("Content-Type", "application/json")
              .header(X_REQUEST_ID, requestId)
              .entity(new StringEntity("{invalid json"))
              .asResponse()) {
            assertEquals(400, response.getStatus());
            assertEquals(requestId, response.getFirstHeader(X_REQUEST_ID));
        }
    }

    @Test
    public void testUsernameNamespaceResolvesUser() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "username:" + ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testIdNamespaceResolvesUser() throws IOException {
        String userId = lookupUserId(ADMIN_USER);

        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "id:" + userId)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testEmailNamespaceResolvesUser() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "email:admin@localhost")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testEmailNamespaceResolvesCorrectUser() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "email:regular@localhost")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testNoNamespaceUUIDFallsBackToIdLookup() throws IOException {
        String userId = lookupUserId(ADMIN_USER);

        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, userId)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testNoNamespaceNonUUIDFallsBackToUsernameLookup() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertTrue(result.decision());
    }

    @Test
    public void testEmailNamespaceWithDuplicateEmailsReturnsBadRequest() throws IOException {
        realm.updateWithCleanup(r -> r.duplicateEmailsAllowed(true));

        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "email:admin@localhost")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testUsernameNamespaceUnknownUserReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "username:nonexistent-user")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testIdNamespaceUnknownUserReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "id:00000000-0000-0000-0000-000000000000")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testEmailNamespaceUnknownUserReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "email:nonexistent@localhost")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testNoNamespaceUUIDUnknownUserReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    @Test
    public void testEmptyIdNamespaceReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "id:")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testEmptyUsernameNamespaceReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "username:")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testEmptyEmailNamespaceReturnsBadRequest() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "email:")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(400, result.statusCode());
    }

    @Test
    public void testMixedCaseNamespaceFallsBackToUsernameLookup() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "Email:admin@localhost")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        // "Email:admin@localhost" is treated as a username, which won't exist
        assertFalse(result.decision());
    }

    @Test
    public void testIdNamespaceWithNonUUIDReturnsDenied() throws IOException {
        EvaluationResult result = authzenClient("admin-user", "password")
              .evaluate(AuthZenClient.evaluationRequest()
                    .subject(USER, "id:not-a-uuid")
                    .action("read")
                    .resource("endpoint", "/admin")
                    .build());

        assertEquals(200, result.statusCode());
        assertFalse(result.decision());
    }

    private String lookupUserId(String username) {
        List<UserRepresentation> users = realm.admin().users().search(username, true);
        assertEquals(1, users.size(), "Expected exactly one user with username: " + username);
        return users.get(0).getId();
    }

    private String realmUrl() {
        return keycloakUrls.getBase() + "/realms/" + realm.getName();
    }

    private AuthZenClient.Authenticated authzenClient(String username, String password) {
        AccessTokenResponse tokenResponse = oauth
              .client(client.getClientId(), client.getSecret())
              .doPasswordGrantRequest(username, password);
        return authZenClient.withAccessToken(tokenResponse.getAccessToken());
    }

    private void configureAuthorizationResources() {
        configureAuthzenClientResources();
        configureSubjectClientResources();
        configureDisableableClientResources();
    }

    // authzen-client is the OAuth client used to authenticate all AuthZen API requests (bearer token).
    // For USER subject evaluations, it also acts as the resource server — resources and policies
    // are resolved from this client because the resource server is determined by token.getIssuedFor().
    private void configureAuthzenClientResources() {
        AuthorizationResource authz = client.admin().authorization();
        String adminRoleId = realm.admin().roles().get("admin").toRepresentation().getId();

        createScope(authz, "read");
        createScope(authz, "write");

        String adminPolicyId = createRolePolicy(authz, "Require Admin Role", adminRoleId);
        String alwaysGrantId = createAlwaysGrantPolicy(authz);

        createResource(authz, "/admin", "endpoint", "read");
        createResourcePermission(authz, "Admin Resource Permission", "/admin", adminPolicyId);

        createResource(authz, "/users", "endpoint", "read");
        createResourcePermission(authz, "Users Resource Permission", "/users", alwaysGrantId);

        createResource(authz, "/scope-limited", "endpoint", "read", "write");
        createScopePermission(authz, "Scope Limited Read Permission", "/scope-limited", "read", "Always Grant");

        createResource(authz, "/context-protected", "endpoint", "read");
        String envPolicyId = createRegexPolicy(authz, "Context Environment Policy", "environment", "production", true);
        createResourcePermission(authz, "Context Protected Permission", "/context-protected", envPolicyId);

        createResource(authz, "/nested-context", "endpoint", "read");
        String ipPolicyId = createRegexPolicy(authz, "Nested Context IP Policy", "request.ip", "10\\.0\\.0\\..*", true);
        createResourcePermission(authz, "Nested Context Permission", "/nested-context", ipPolicyId);

        createResource(authz, "/empty-type", null, "read");
        createResourcePermission(authz, "Empty Type Resource Permission", "/empty-type", alwaysGrantId);

        createResource(authz, "/subject-protected", "endpoint", "read");
        String deptPolicyId = createRegexPolicy(authz, "Subject Department Policy", "department", "engineering", false);
        createResourcePermission(authz, "Subject Protected Permission", "/subject-protected", deptPolicyId);
    }

    // subject-id-client is used as the subject in CLIENT-type evaluations (subject.id = "subject-id-client").
    // When subject.type is CLIENT, the resource server is resolved from subject.id rather than the
    // token's client — so this client's own resources and policies are evaluated, not authzen-client's.
    private void configureSubjectClientResources() {
        AuthorizationResource authz = subjectClient.admin().authorization();

        createScope(authz, "read");

        String alwaysGrantId = createAlwaysGrantPolicy(authz);

        // /subject-only - only exists on subject-id-client's resource server
        createResource(authz, "/subject-only", "endpoint", "read");
        createResourcePermission(authz, "Subject Only Permission", "/subject-only", alwaysGrantId);
    }

    // disableable-client is a dedicated client used to test that a disabled client with a
    // previously obtained token is denied. Isolated from subject-id-client so that disabling
    // it does not affect other tests.
    private void configureDisableableClientResources() {
        AuthorizationResource authz = disableableClient.admin().authorization();

        createScope(authz, "read");

        String alwaysGrantId = createAlwaysGrantPolicy(authz);

        createResource(authz, "/open", "endpoint", "read");
        createResourcePermission(authz, "Open Permission", "/open", alwaysGrantId);
    }

    private static void createScope(AuthorizationResource authz, String name) {
        try (Response response = authz.scopes().create(new ScopeRepresentation(name))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    private static void createResource(AuthorizationResource authz, String name, String type, String... scopes) {
        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setName(name);
        resource.setType(type);
        resource.addScope(scopes);
        try (Response response = authz.resources().create(resource)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    private static String createRolePolicy(AuthorizationResource authz, String name, String roleId) {
        RolePolicyRepresentation policy = new RolePolicyRepresentation();
        policy.setName(name);
        policy.addRole(roleId);
        try (Response response = authz.policies().role().create(policy)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        return authz.policies().role().findByName(name).getId();
    }

    private static String createAlwaysGrantPolicy(AuthorizationResource authz) {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName("Always Grant");
        policy.setType("always-grant");
        try (Response response = authz.policies().create(policy)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        return authz.policies().findByName("Always Grant").getId();
    }

    private static String createRegexPolicy(AuthorizationResource authz, String name,
                                            String claim, String pattern, boolean targetContextAttributes) {
        RegexPolicyRepresentation policy = new RegexPolicyRepresentation();
        policy.setName(name);
        policy.setTargetClaim(claim);
        policy.setPattern(pattern);
        policy.setTargetContextAttributes(targetContextAttributes);
        try (Response response = authz.policies().regex().create(policy)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        return authz.policies().findByName(name).getId();
    }

    private static void createResourcePermission(AuthorizationResource authz, String name,
                                                 String resourceName, String policyId) {
        ResourcePermissionRepresentation permission = ResourcePermissionRepresentation.create()
              .name(name)
              .resources(Set.of(authz.resources().findByName(resourceName).get(0).getId()))
              .policies(Set.of(policyId))
              .build();
        try (Response response = authz.permissions().resource().create(permission)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    private static void createScopePermission(AuthorizationResource authz, String name,
                                              String resourceName, String scopeName, String policyName) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(name);
        permission.setResources(Set.of(authz.resources().findByName(resourceName).get(0).getId()));
        permission.setScopes(Set.of(authz.scopes().findByName(scopeName).getId()));
        permission.addPolicy(policyName);
        try (Response response = authz.permissions().scope().create(permission)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    public static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.realmRoles("admin");

            realm.users(UserBuilder.create("admin-user")
                  .username(ADMIN_USER)
                  .name("Admin", "User")
                  .email("admin@localhost")
                  .password("password")
                  .realmRoles("admin"));

            realm.users(UserBuilder.create("regular-user")
                  .username(REGULAR_USER)
                  .name("Regular", "User")
                  .email("regular@localhost")
                  .password("password"));

            return realm;
        }
    }

    public static class AuthzClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                  .secret("secret")
                  .directAccessGrantsEnabled(true)
                  .authorizationServicesEnabled(true);
        }
    }

    public static class NoAuthzClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                  .secret("secret")
                  .directAccessGrantsEnabled(true);
        }
    }
}
