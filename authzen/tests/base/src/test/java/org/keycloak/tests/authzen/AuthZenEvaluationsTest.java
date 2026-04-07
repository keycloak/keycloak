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
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.authorization.authzen.AuthZen;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
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
import org.keycloak.testframework.authzen.client.AuthZenClient.EvaluationsResult;
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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.authzen.AuthZen.SubjectType.USER;
import static org.keycloak.authorization.authzen.AuthZenRequestIdFilter.X_REQUEST_ID;
import static org.keycloak.authorization.authzen.AuthZenWellKnownProvider.accessEvaluationsEndpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the AuthZen Evaluations (batch) endpoint.
 */
@KeycloakIntegrationTest(config = AuthZenServerConfig.class)
public class AuthZenEvaluationsTest {

    private static final String ADMIN_USER = "admin-user";
    private static final String REGULAR_USER = "regular-user";

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm realm;

    @InjectClient(ref = "authzen-client", config = AuthzClientConfig.class)
    ManagedClient client;

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
    }

    @Test
    public void testEvaluationsBatchAllGranted() throws IOException {
        EvaluationsResult result = authzenClient("admin-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/users")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(2, result.evaluations().size());
        assertTrue(result.evaluations().get(0).decision());
        assertTrue(result.evaluations().get(1).decision());
    }

    @Test
    public void testEvaluationsBatchMixedDecisions() throws IOException {
        EvaluationsResult result = authzenClient("regular-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, REGULAR_USER)
                    .action("read")
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/users")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(2, result.evaluations().size());
        assertFalse(result.evaluations().get(0).decision());
        assertTrue(result.evaluations().get(1).decision());
    }

    @Test
    public void testEvaluationsItemOverridesDefaults() throws IOException {
        EvaluationsResult result = authzenClient("admin-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .resource("endpoint", "/admin")
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .action("write")
                          .resource("endpoint", "/scope-limited")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(2, result.evaluations().size());
        assertTrue(result.evaluations().get(0).decision());
        assertFalse(result.evaluations().get(1).decision());
    }

    @Test
    public void testEvaluationsUnauthenticatedReturnsUnauthorized() throws IOException {
        EvaluationsResult result = authZenClient.evaluations(AuthZenClient.evaluationsRequest()
              .subject(USER, ADMIN_USER)
              .action("read")
              .addEvaluation(AuthZenClient.evaluationItem()
                    .resource("endpoint", "/admin")
                    .build())
              .build());

        assertEquals(401, result.statusCode());
    }

    @Test
    public void testEvaluationsEmptyArrayFallsBackToSingleEvaluation() throws IOException {
        String json = """
              {"subject":{"type":"user","id":"%s"},\
              "resource":{"type":"endpoint","id":"/admin"},\
              "action":{"name":"read"},\
              "evaluations":[]}""".formatted(ADMIN_USER);

        JsonNode body = postEvaluations("admin-user", "password", json);
        assertTrue(body.get("decision").asBoolean());
        assertNull(body.get("evaluations"));
    }

    @Test
    public void testEvaluationsAbsentFallsBackToSingleEvaluation() throws IOException {
        String json = """
              {"subject":{"type":"user","id":"%s"},\
              "resource":{"type":"endpoint","id":"/admin"},\
              "action":{"name":"read"}}""".formatted(ADMIN_USER);

        JsonNode body = postEvaluations(ADMIN_USER, "password", json);
        assertTrue(body.get("decision").asBoolean());
        assertNull(body.get("evaluations"));
    }

    private JsonNode postEvaluations(String username, String password, String json) throws IOException {
        String url = accessEvaluationsEndpoint(keycloakUrls.getBase() + "/realms/" + realm.getName());
        AccessTokenResponse tokenResponse = oauth
              .client(client.getClientId(), client.getSecret())
              .doPasswordGrantRequest(username, password);

        try (SimpleHttpResponse response = simpleHttp.doPost(url)
              .auth(tokenResponse.getAccessToken())
              .header("Content-Type", "application/json")
              .entity(new StringEntity(json))
              .asResponse()) {
            assertEquals(200, response.getStatus());
            return response.asJson();
        }
    }

    @Test
    public void testExecuteAllSemantic() throws IOException {
        EvaluationsResult result = authzenClient("regular-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, REGULAR_USER)
                    .action("read")
                    .evaluationsSemantic(AuthZen.EvaluationsSemantic.EXECUTE_ALL)
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/users")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(2, result.evaluations().size());
        assertFalse(result.evaluations().get(0).decision());
        assertTrue(result.evaluations().get(1).decision());
    }

    @Test
    public void testDenyOnFirstDenyStopsOnDenial() throws IOException {
        EvaluationsResult result = authzenClient("regular-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, REGULAR_USER)
                    .action("read")
                    .evaluationsSemantic(AuthZen.EvaluationsSemantic.DENY_ON_FIRST_DENY)
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/users")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(1, result.evaluations().size());
        assertFalse(result.evaluations().get(0).decision());
    }

    @Test
    public void testDenyOnFirstDenyReturnsAllWhenAllPermitted() throws IOException {
        EvaluationsResult result = authzenClient("admin-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .evaluationsSemantic(AuthZen.EvaluationsSemantic.DENY_ON_FIRST_DENY)
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/users")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(2, result.evaluations().size());
        assertTrue(result.evaluations().get(0).decision());
        assertTrue(result.evaluations().get(1).decision());
    }

    @Test
    public void testPermitOnFirstPermitStopsOnPermit() throws IOException {
        EvaluationsResult result = authzenClient("regular-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, REGULAR_USER)
                    .action("read")
                    .evaluationsSemantic(AuthZen.EvaluationsSemantic.PERMIT_ON_FIRST_PERMIT)
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/users")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(2, result.evaluations().size());
        assertFalse(result.evaluations().get(0).decision());
        assertTrue(result.evaluations().get(1).decision());
    }

    @Test
    public void testPermitOnFirstPermitStopsImmediatelyOnFirstPermit() throws IOException {
        EvaluationsResult result = authzenClient("admin-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .evaluationsSemantic(AuthZen.EvaluationsSemantic.PERMIT_ON_FIRST_PERMIT)
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/users")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(1, result.evaluations().size());
        assertTrue(result.evaluations().get(0).decision());
    }

    @Test
    public void testXRequestIdEchoedInResponse() throws IOException {
        String requestId = "test-request-id-12345";

        EvaluationsResult result = authzenClient("admin-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .build(),
                    Map.of(X_REQUEST_ID, requestId));

        assertEquals(200, result.statusCode());
        assertTrue(result.evaluations().get(0).decision());
        assertEquals(requestId, result.header(X_REQUEST_ID));
    }

    @Test
    public void testXRequestIdEchoedOnUnauthorizedResponse() throws IOException {
        String requestId = "unauth-request-id";

        EvaluationsResult result = new AuthZenClient(simpleHttp,
              keycloakUrls.getBase() + "/realms/" + realm.getName())
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, ADMIN_USER)
                    .action("read")
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .build(),
                    Map.of(X_REQUEST_ID, requestId));

        assertEquals(401, result.statusCode());
        assertEquals(requestId, result.header(X_REQUEST_ID));
    }

    @Test
    public void testXRequestIdEchoedOnBadRequestResponse() throws IOException {
        String requestId = "bad-request-id";

        String url = accessEvaluationsEndpoint(keycloakUrls.getBase() + "/realms/" + realm.getName());
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
    public void testDefaultSemanticIsExecuteAll() throws IOException {
        EvaluationsResult result = authzenClient("regular-user", "password")
              .evaluations(AuthZenClient.evaluationsRequest()
                    .subject(USER, REGULAR_USER)
                    .action("read")
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/admin")
                          .build())
                    .addEvaluation(AuthZenClient.evaluationItem()
                          .resource("endpoint", "/users")
                          .build())
                    .build());

        assertEquals(200, result.statusCode());
        assertEquals(2, result.evaluations().size());
    }

    private AuthZenClient.Authenticated authzenClient(String username, String password) {
        AccessTokenResponse tokenResponse = oauth
              .client(client.getClientId(), client.getSecret())
              .doPasswordGrantRequest(username, password);
        return authZenClient.withAccessToken(tokenResponse.getAccessToken());
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
            return realm.realmRoles("admin")
                  .users(
                        UserBuilder.create("admin-user")
                              .name("Admin", "User")
                              .email("admin@localhost")
                              .password("password")
                              .realmRoles("admin"),

                        UserBuilder.create("regular-user")
                              .name("Regular", "User")
                              .email("regular@localhost")
                              .password("password")
                  );
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
}
