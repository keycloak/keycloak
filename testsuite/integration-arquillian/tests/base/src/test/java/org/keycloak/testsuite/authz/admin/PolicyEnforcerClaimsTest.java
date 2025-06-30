/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.AuthorizationContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.util.AuthzTestUtils;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcerClaimsTest extends AbstractKeycloakTest {

    protected static final String REALM_NAME = "authz-test";

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name(REALM_NAME)
                .roles(RolesBuilder.create()
                        .realmRole(RoleBuilder.create().name("uma_authorization").build())
                        .realmRole(RoleBuilder.create().name("uma_protection").build())
                )
                .user(UserBuilder.create().username("marta").password("password")
                        .addRoles("uma_authorization", "uma_protection")
                        .role("resource-server-test", "uma_protection"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId("resource-server-uma-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-uma-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants())
                .client(ClientBuilder.create().clientId("resource-server-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants())
                .client(ClientBuilder.create().clientId("public-client-test")
                        .publicClient()
                        .redirectUris("http://localhost:8180/auth/realms/master/app/auth/*", "https://localhost:8543/auth/realms/master/app/auth/*")
                        .directAccessGrants())
                .build());
    }

    @Test
    public void testEnforceUMAAccessWithClaimsUsingBearerToken() {
        initAuthorizationSettings(getClientResource("resource-server-uma-test"));

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-uma-claims-test.json", true);
        HashMap<String, List<String>> headers = new HashMap<>();
        HashMap<String, List<String>> parameters = new HashMap<>();

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        AuthzClient authzClient = policyEnforcer.getAuthzClient();
        String token = authzClient.obtainAccessToken("marta", "password").getToken();

        headers.put("Authorization", Arrays.asList("Bearer " + token));

        AuthzTestUtils.TestResponse testResponse = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", "POST", token, headers, parameters, null),
                testResponse);
        assertFalse(context.isGranted());

        AuthorizationRequest request = new AuthorizationRequest();

        request.setTicket(extractTicket(testResponse.getHeaders()));

        AuthorizationResponse response = authzClient.authorization("marta", "password").authorize(request);
        token = response.getToken();

        assertNotNull(token);

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", "POST", token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("200"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", "POST", token, headers, parameters, null),
                testResponse.clear());
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", "POST", token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("10"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", "POST", token, headers, parameters, null),
                testResponse.clear());

        request = new AuthorizationRequest();

        request.setTicket(extractTicket(testResponse.getHeaders()));

        response = authzClient.authorization("marta", "password").authorize(request);
        token = response.getToken();

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", "POST", token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", "GET", token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        assertEquals(1, context.getPermissions().size());
        Permission permission = context.getPermissions().get(0);

        assertEquals(parameters.get("withdrawal.amount").get(0), permission.getClaims().get("withdrawal.amount").iterator().next());
    }

    @Test
    public void testEnforceEntitlementAccessWithClaimsWithoutBearerToken() {
        initAuthorizationSettings(getClientResource("resource-server-test"));

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-entitlement-claims-test.json", false);
        HashMap<String, List<String>> headers = new HashMap<>();
        HashMap<String, List<String>> parameters = new HashMap<>();

        AuthzClient authzClient = policyEnforcer.getAuthzClient();
        String token = authzClient.obtainAccessToken("marta", "password").getToken();

        AuthzTestUtils.TestResponse testResponse = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse);
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());
        assertEquals(1, context.getPermissions().size());
        Permission permission = context.getPermissions().get(0);
        assertEquals(parameters.get("withdrawal.amount").get(0), permission.getClaims().get("withdrawal.amount").iterator().next());

        parameters.put("withdrawal.amount", Arrays.asList("200"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("10"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());

        assertTrue(context.isGranted());

        assertEquals(1, context.getPermissions().size());
        permission = context.getPermissions().get(0);
        assertEquals(parameters.get("withdrawal.amount").get(0), permission.getClaims().get("withdrawal.amount").iterator().next());
    }

    @Test
    public void testEnforceEntitlementAccessWithClaimsWithBearerToken() {
        initAuthorizationSettings(getClientResource("resource-server-test"));

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-entitlement-claims-test.json", false);
        HashMap<String, List<String>> headers = new HashMap<>();
        HashMap<String, List<String>> parameters = new HashMap<>();

        AuthzClient authzClient = policyEnforcer.getAuthzClient();
        String token = authzClient.obtainAccessToken("marta", "password").getToken();

        headers.put("Authorization", Arrays.asList("Bearer " + token));

        AuthzTestUtils.TestResponse testResponse = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse);
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("200"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("10"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());

        assertTrue(context.isGranted());
    }

    @Test
    public void testEnforceEntitlementAccessWithClaimsWithBearerTokenFromPublicClient() {
        initAuthorizationSettings(getClientResource("resource-server-test"));

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-entitlement-claims-test.json", false);
        HashMap<String, List<String>> headers = new HashMap<>();
        HashMap<String, List<String>> parameters = new HashMap<>();

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        headers.put("Authorization", Arrays.asList("Bearer " + token));

        AuthzTestUtils.TestResponse testResponse = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse);
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("200"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("10"));

        context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/bank/account/1/withdrawal", null, token, headers, parameters, null),
                testResponse.clear());

        assertTrue(context.isGranted());
    }

    private String extractTicket(Map<String, List<String>> headers) {
        List<String> wwwAuthenticateHeader = headers.get("WWW-Authenticate");

        assertNotNull(wwwAuthenticateHeader);
        assertFalse(wwwAuthenticateHeader.isEmpty());

        String wwwAuthenticate = wwwAuthenticateHeader.get(0);
        return wwwAuthenticate.substring(wwwAuthenticate.indexOf("ticket=") + "ticket=\"".length(), wwwAuthenticate.lastIndexOf('"'));
    }

    private void initAuthorizationSettings(ClientResource clientResource) {
        if (clientResource.authorization().resources().findByName("Bank Account").isEmpty()) {
            JSPolicyRepresentation policy = new JSPolicyRepresentation();

            policy.setName("Withdrawal Limit Policy");
            policy.setType("script-scripts/enforce-withdraw-limit-policy.js");

            clientResource.authorization().policies().js().create(policy).close();

            createResource(clientResource, "Bank Account", "/api/bank/account/{id}/withdrawal", "withdrawal");

            ScopePermissionRepresentation permission = new ScopePermissionRepresentation();

            permission.setName("Withdrawal Permission");
            permission.addScope("withdrawal");
            permission.addPolicy(policy.getName());

            clientResource.authorization().permissions().scope().create(permission).close();
        }
    }

    private ResourceRepresentation createResource(ClientResource clientResource, String name, String uri, String... scopes) {
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName(name);
        representation.setUri(uri);
        representation.setScopes(Arrays.asList(scopes).stream().map(ScopeRepresentation::new).collect(Collectors.toSet()));

        try (jakarta.ws.rs.core.Response response = clientResource.authorization().resources().create(representation)) {

            representation.setId(response.readEntity(ResourceRepresentation.class).getId());

            return representation;
        }
    }

    private ClientResource getClientResource(String name) {
        ClientsResource clients = realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation representation = clients.findByClientId(name).get(0);
        return clients.get(representation.getId());
    }
}
