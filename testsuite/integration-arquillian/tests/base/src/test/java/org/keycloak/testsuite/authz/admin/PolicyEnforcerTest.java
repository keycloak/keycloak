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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.AuthorizationContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.PermissionsResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
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
import org.keycloak.testsuite.util.WaitUtils;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcerTest extends AbstractKeycloakTest {

    private static final String RESOURCE_SERVER_CLIENT_ID = "resource-server-test";
    private static final String REALM_NAME = "authz-test";

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
                        .realmRole(RoleBuilder.create().name("user").build())
                )
                .user(UserBuilder.create().username("marta").password("password")
                        .addRoles("uma_authorization", "uma_protection", "user")
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

    @Before
    public void onBefore() {
        initAuthorizationSettings(getClientResource(RESOURCE_SERVER_CLIENT_ID));
    }

    @Test
    public void testBearerOnlyClientResponse() {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-bearer-only.json", true);

        AuthzTestUtils.TestResponse testResponse = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourcea"), testResponse);

        assertFalse(context.isGranted());
        assertEquals(403, testResponse.getStatus());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourcea", token), testResponse.clear());
        assertTrue(context.isGranted());

        testResponse = new AuthzTestUtils.TestResponse();
        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourceb"), testResponse.clear());
        assertFalse(context.isGranted());
        assertEquals(403, testResponse.getStatus());
    }

    @Test
    public void testPathConfigurationPrecendenceWhenLazyLoadingPaths() throws IOException {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-paths.json", false);

        AuthzTestUtils.TestResponse testResponse = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourcea"), testResponse);

        assertFalse(context.isGranted());
        assertEquals(403, testResponse.getStatus());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourcea", token), testResponse.clear());
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/"), testResponse.clear());
        assertTrue(context.isGranted());
    }

    @Test
    public void testResolvingClaimsOnce() {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-bearer-only-with-cip.json", true);

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        AuthorizationContext context = policyEnforcer.enforce(
                AuthzTestUtils.createHttpRequest("/api/resourcea", token, Collections.singletonMap("claim-a", Collections.singletonList("value-claim-a"))),
                new AuthzTestUtils.TestResponse());
        Permission permission = context.getPermissions().get(0);
        Map<String, Set<String>> claims = permission.getClaims();

        assertTrue(context.isGranted());
        assertEquals("value-claim-a", claims.get("claim-a").iterator().next());
        assertEquals("claim-b", claims.get("claim-b").iterator().next());
    }

    @Test
    public void testCustomClaimProvider() {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-bearer-only-with-cip.json", true);

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourcea", token), new AuthzTestUtils.TestResponse());
        Permission permission = context.getPermissions().get(0);
        Map<String, Set<String>> claims = permission.getClaims();

        assertTrue(context.isGranted());
        assertEquals("test", claims.get("resolved-claim").iterator().next());
    }

    @Test
    public void testOnDenyRedirectTo() {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-on-deny-redirect.json", false);

        AuthzTestUtils.TestResponse response = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourcea"), response);

        assertFalse(context.isGranted());
        assertEquals(302, response.getStatus());
        List<String> location = response.getHeaders().getOrDefault("Location", Collections.emptyList());
        assertFalse(location.isEmpty());
        assertEquals("/accessDenied", location.get(0));
    }

    @Test
    public void testNotAuthenticatedDenyUnmapedPath() {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-bearer-only.json", true);

        AuthzTestUtils.TestResponse response = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/unmmaped"), response);

        assertFalse(context.isGranted());
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testMappedPathEnforcementModeDisabled() {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-disabled-enforce-mode-path.json", true);

        AuthzTestUtils.TestResponse response = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource/public"), response);
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourceb"), response.clear());
        assertFalse(context.isGranted());
        assertEquals(403, response.getStatus());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");
        String token = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), null).getAccessToken();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourcea", token), response.clear());
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resourceb", token), response.clear());
        assertFalse(context.isGranted());
        assertEquals(403, response.getStatus());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource/public", token), response.clear());
        assertTrue(context.isGranted());
    }

    @Test
    public void testDisabledPathNoCache() {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-disabled-path-nocache.json", true);

        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource/public"), new AuthzTestUtils.TestResponse());
        assertTrue(context.isGranted());

        ClientResource clientResource = getClientResource(RESOURCE_SERVER_CLIENT_ID);
        ResourceRepresentation resource = clientResource.authorization().resources()
                .findByName("Root").get(0);

        clientResource.authorization().resources().resource(resource.getId()).remove();

        // first request caches the path and the entry is invalidated due to the lifespan
        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource/all-public"), new AuthzTestUtils.TestResponse());
        assertTrue(context.isGranted());

        WaitUtils.pause(1000);

        // second request can not fail because entry should not be invalidated
        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource/all-public"), new AuthzTestUtils.TestResponse());
        assertTrue(context.isGranted());
    }

    @Test
    public void testLazyLoadedPathIsCached() {
        ClientResource clientResource = getClientResource(RESOURCE_SERVER_CLIENT_ID);
        createResource(clientResource, "Static Test Resource", "/api/any-resource/*");

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName("Any Resource Permission");
        permission.addResource("Static Test Resource");
        permission.addPolicy("Always Grant Policy");

        clientResource.authorization().permissions().resource().create(permission);

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-disabled-path-nocache.json", true);

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/any-resource/test", token), new AuthzTestUtils.TestResponse());
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/any-resource/test", token), new AuthzTestUtils.TestResponse());
        assertTrue(context.isGranted());

        ResourceRepresentation resource = clientResource.authorization().resources()
                .findByName("Static Test Resource").get(0);

        clientResource.authorization().resources().resource(resource.getId()).remove();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/any-resource/test", token), new AuthzTestUtils.TestResponse());
        assertFalse(context.isGranted());
    }

    @Test
    public void testEnforcementModeDisabled() {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-disabled-enforce-mode.json", true);

        AuthzTestUtils.TestResponse response = new AuthzTestUtils.TestResponse();
        policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource/public"), response);
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testMatchHttpVerbsToScopes() {
        ClientResource clientResource = getClientResource(RESOURCE_SERVER_CLIENT_ID);
        ResourceRepresentation resource = createResource(clientResource, "Resource With HTTP Scopes", "/api/resource-with-scope");

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getName());
        permission.addPolicy("Always Grant Policy");

        PermissionsResource permissions = clientResource.authorization().permissions();
        permissions.resource().create(permission).close();

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-match-http-verbs-scopes.json", true);
        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        AuthzTestUtils.TestResponse testResponse = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token), testResponse);

        assertFalse("Should fail because resource does not have any scope named GET", context.isGranted());
        assertEquals(403, testResponse.getStatus());
        
        resource.addScope("GET", "POST");

        clientResource.authorization().resources().resource(resource.getId()).update(resource);

        policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-match-http-verbs-scopes.json", true);

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token), testResponse.clear());
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token, "POST"), testResponse.clear());
        assertTrue(context.isGranted());

        // create a PATCH scope without associated it with the resource so that a PATCH request is denied accordingly even though
        // the scope exists on the server
        clientResource.authorization().scopes().create(new ScopeRepresentation("PATCH"));
        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token, "PATCH"), testResponse.clear());
        assertFalse(context.isGranted());

        ScopePermissionRepresentation postPermission = new ScopePermissionRepresentation();

        postPermission.setName("GET permission");
        postPermission.addScope("GET");
        postPermission.addPolicy("Always Deny Policy");

        permissions.scope().create(postPermission).close();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token), testResponse.clear());
        assertFalse(context.isGranted());

        postPermission = permissions.scope().findByName(postPermission.getName());

        postPermission.addScope("GET");
        postPermission.addPolicy("Always Grant Policy");

        permissions.scope().findById(postPermission.getId()).update(postPermission);

        AuthzClient authzClient = policyEnforcer.getAuthzClient();
        AuthorizationResponse authorize = authzClient.authorization(token).authorize();
        token = authorize.getToken();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token), testResponse.clear());
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token, "POST"), testResponse.clear());
        assertTrue(context.isGranted());

        postPermission = permissions.scope().findByName(postPermission.getName());
        postPermission.addScope("GET");
        postPermission.addPolicy("Always Deny Policy");
        permissions.scope().findById(postPermission.getId()).update(postPermission);
        authorize = authzClient.authorization(token).authorize();
        token = authorize.getToken();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token), testResponse.clear());
        assertFalse(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token, "POST"), testResponse.clear());
        assertTrue(context.isGranted());

        postPermission = permissions.scope().findByName(postPermission.getName());
        postPermission.addScope("GET");
        postPermission.addPolicy("Always Grant Policy");
        permissions.scope().findById(postPermission.getId()).update(postPermission);
        authorize = authzClient.authorization(token).authorize();
        token = authorize.getToken();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token), testResponse.clear());
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token, "POST"), testResponse.clear());
        assertTrue(context.isGranted());

        postPermission = permissions.scope().findByName(postPermission.getName());
        postPermission.addScope("POST");
        postPermission.addPolicy("Always Deny Policy");
        permissions.scope().findById(postPermission.getId()).update(postPermission);
        AuthorizationRequest request = new AuthorizationRequest();

        request.addPermission(null, "GET");

        authorize = authzClient.authorization(token).authorize(request);
        token = authorize.getToken();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token), testResponse.clear());
        assertTrue(context.isGranted());

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/resource-with-scope", token, "POST"), testResponse.clear());
        assertFalse(context.isGranted());
    }

    @Test
    public void testUsingSubjectToken() {
        ClientResource clientResource = getClientResource(RESOURCE_SERVER_CLIENT_ID);
        ResourceRepresentation resource = createResource(clientResource, "Resource Subject Token", "/api/check-subject-token");

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getName());
        permission.addPolicy("Only User Policy");

        PermissionsResource permissions = clientResource.authorization().permissions();
        permissions.resource().create(permission).close();

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-bearer-only.json", true);
        AuthzTestUtils.TestResponse testResponse = new AuthzTestUtils.TestResponse();
        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/check-subject-token"), testResponse);

        assertFalse(context.isGranted());
        assertEquals(403, testResponse.getStatus());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/check-subject-token", token), testResponse.clear());
        assertTrue(context.isGranted());
    }

    @Test
    public void testUsingInvalidToken() {
        ClientResource clientResource = getClientResource(RESOURCE_SERVER_CLIENT_ID);
        ResourceRepresentation resource = createResource(clientResource, "Resource Subject Invalid Token", "/api/check-subject-token");

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getName());
        permission.addPolicy("Only User Policy");

        PermissionsResource permissions = clientResource.authorization().permissions();
        permissions.resource().create(permission).close();

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-bearer-only.json", true);

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/check-subject-token", token), new AuthzTestUtils.TestResponse());
        assertTrue(context.isGranted());
        
        oauth.doLogout(response.getRefreshToken(), null);

        context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/check-subject-token", token), new AuthzTestUtils.TestResponse());
        assertFalse(context.isGranted());
    }

    @Test
    public void testLazyLoadPaths() {
        ClientResource clientResource = getClientResource(RESOURCE_SERVER_CLIENT_ID);

        for (int i = 0; i < 200; i++) {
            ResourceRepresentation representation = new ResourceRepresentation();

            representation.setType("test");
            representation.setName("Resource " + i);
            representation.setUri("/api/" + i);

            jakarta.ws.rs.core.Response response = clientResource.authorization().resources().create(representation);

            representation.setId(response.readEntity(ResourceRepresentation.class).getId());

            response.close();
        }

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName("Test Permission");
        permission.setResourceType("test");
        permission.addPolicy("Only User Policy");

        PermissionsResource permissions = clientResource.authorization().permissions();
        permissions.resource().create(permission).close();

        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-no-lazyload.json", true);

        assertEquals(205, policyEnforcer.getPaths().size());

        policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-lazyload.json", true);
        assertEquals(0, policyEnforcer.getPathMatcher().getPathCache().size());
        assertEquals(0, policyEnforcer.getPaths().size());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        for (int i = 0; i < 101; i++) {
            policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/" + i, token), new AuthzTestUtils.TestResponse());
        }

        assertEquals(101, policyEnforcer.getPathMatcher().getPathCache().size());

        for (int i = 101; i < 200; i++) {
            policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/" + i, token), new AuthzTestUtils.TestResponse());
        }

        assertEquals(200, policyEnforcer.getPathMatcher().getPathCache().size());
        assertEquals(0, policyEnforcer.getPaths().size());

        ResourceRepresentation resource = clientResource.authorization().resources()
                .findByName("Root").get(0);

        clientResource.authorization().resources().resource(resource.getId()).remove();

        policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-lazyload-with-paths.json", true);

        AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api/0", token), new AuthzTestUtils.TestResponse());

        assertTrue(context.isGranted());
    }

    @Test
    public void testSetMethodConfigs() {
        ClientResource clientResource = getClientResource(RESOURCE_SERVER_CLIENT_ID);
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName(KeycloakModelUtils.generateId());
        representation.setUris(Collections.singleton("/api-method/*"));

        ResourcesResource resources = clientResource.authorization().resources();
        jakarta.ws.rs.core.Response response = resources.create(representation);

        representation.setId(response.readEntity(ResourceRepresentation.class).getId());

        response.close();

        try {
            PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-paths-use-method-config.json", true);

            oauth.realm(REALM_NAME);
            oauth.clientId("public-client-test");
            oauth.doLogin("marta", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse tokeResponse = oauth.doAccessTokenRequest(code, null);
            String token = tokeResponse.getAccessToken();

            AuthorizationContext context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api-method/foo", token), new AuthzTestUtils.TestResponse());

            // GET is disabled in the config
            assertTrue(context.isGranted());

            PolicyEnforcerConfig.PathConfig pathConfig = policyEnforcer.getPaths().get("/api-method/*");

            assertNotNull(pathConfig);
            List<PolicyEnforcerConfig.MethodConfig> methods = pathConfig.getMethods();
            assertEquals(1, methods.size());
            assertTrue(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED.equals(methods.get(0).getScopesEnforcementMode()));

            // other verbs should be protected
            context = policyEnforcer.enforce(AuthzTestUtils.createHttpRequest("/api-method/foo", token, "POST"), new AuthzTestUtils.TestResponse());

            assertFalse(context.isGranted());
        } finally {
            resources.resource(representation.getId()).remove();
        }
    }

    private void initAuthorizationSettings(ClientResource clientResource) {
        if (clientResource.authorization().resources().findByName("Resource A").isEmpty()) {
            JSPolicyRepresentation jsPolicy = new JSPolicyRepresentation();

            jsPolicy.setName("Always Grant Policy");
            jsPolicy.setType("script-scripts/default-policy.js");

            clientResource.authorization().policies().js().create(jsPolicy).close();

            RolePolicyRepresentation rolePolicy = new RolePolicyRepresentation();

            rolePolicy.setName("Only User Policy");
            rolePolicy.addRole("user");

            clientResource.authorization().policies().role().create(rolePolicy).close();

            createResource(clientResource, "Resource A", "/api/resourcea");

            ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

            permission.setName("Resource A Permission");
            permission.addResource("Resource A");
            permission.addPolicy(jsPolicy.getName());

            clientResource.authorization().permissions().resource().create(permission).close();
        }

        if (clientResource.authorization().resources().findByName("Resource B").isEmpty()) {
            JSPolicyRepresentation policy = new JSPolicyRepresentation();

            policy.setName("Always Deny Policy");
            policy.setType("script-scripts/always-deny-policy.js");

            clientResource.authorization().policies().js().create(policy).close();

            createResource(clientResource, "Resource B", "/api/resourceb");

            ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

            permission.setName("Resource B Permission");
            permission.addResource("Resource B");
            permission.addPolicy(policy.getName());

            clientResource.authorization().permissions().resource().create(permission).close();
        }

        if (clientResource.authorization().resources().findByName("Root").isEmpty()) {
            createResource(clientResource, "Root", "/*");
        }
    }

    private ResourceRepresentation createResource(ClientResource clientResource, String name, String uri, String... scopes) {
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName(name);
        representation.setUri(uri);
        representation.setScopes(Arrays.asList(scopes).stream().map(ScopeRepresentation::new).collect(Collectors.toSet()));

        jakarta.ws.rs.core.Response response = clientResource.authorization().resources().create(representation);

        representation.setId(response.readEntity(ResourceRepresentation.class).getId());

        response.close();

        return representation;
    }

    private ClientResource getClientResource(String name) {
        ClientsResource clients = realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation representation = clients.findByClientId(name).get(0);
        return clients.get(representation.getId());
    }
}
