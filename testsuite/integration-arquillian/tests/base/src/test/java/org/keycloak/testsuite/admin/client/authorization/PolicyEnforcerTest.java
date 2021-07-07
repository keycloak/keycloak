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
package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.common.Profile.Feature.UPLOAD_SCRIPTS;

import javax.security.cert.X509Certificate;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.AuthorizationContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.CorsHeaders;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade.Cookie;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.adapters.spi.HttpFacade.Response;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.PermissionsResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
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
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
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
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-bearer-only.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        OIDCHttpFacade httpFacade = createHttpFacade("/api/resourcea");
        AuthorizationContext context = policyEnforcer.enforce(httpFacade);

        assertFalse(context.isGranted());
        assertEquals(403, TestResponse.class.cast(httpFacade.getResponse()).getStatus());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        httpFacade = createHttpFacade("/api/resourcea", token);

        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        httpFacade = createHttpFacade("/api/resourceb");

        context = policyEnforcer.enforce(httpFacade);
        assertFalse(context.isGranted());
        assertEquals(403, TestResponse.class.cast(httpFacade.getResponse()).getStatus());
    }

    @Test
    public void testPathConfigurationPrecendenceWhenLazyLoadingPaths() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-paths.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        OIDCHttpFacade httpFacade = createHttpFacade("/api/resourcea");
        AuthorizationContext context = policyEnforcer.enforce(httpFacade);

        assertFalse(context.isGranted());
        assertEquals(403, TestResponse.class.cast(httpFacade.getResponse()).getStatus());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        httpFacade = createHttpFacade("/api/resourcea", token);

        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        httpFacade = createHttpFacade("/");

        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());
    }

    @Test
    public void testResolvingClaimsOnce() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-bearer-only-with-cip.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        OIDCHttpFacade httpFacade = createHttpFacade("/api/resourcea", token, new Function<String, String>() {
            AtomicBoolean resolved = new AtomicBoolean();

            @Override
            public String apply(String s) {
                Assert.assertTrue(resolved.compareAndSet(false, true));
                return "value-" + s;
            }
        });

        AuthorizationContext context = policyEnforcer.enforce(httpFacade);
        Permission permission = context.getPermissions().get(0);
        Map<String, Set<String>> claims = permission.getClaims();

        assertTrue(context.isGranted());
        assertEquals("value-claim-a", claims.get("claim-a").iterator().next());
        assertEquals("claim-b", claims.get("claim-b").iterator().next());
    }

    @Test
    public void testCustomClaimProvider() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-bearer-only-with-cip.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        OIDCHttpFacade httpFacade = createHttpFacade("/api/resourcea", token);

        AuthorizationContext context = policyEnforcer.enforce(httpFacade);
        Permission permission = context.getPermissions().get(0);
        Map<String, Set<String>> claims = permission.getClaims();

        assertTrue(context.isGranted());
        assertEquals("test", claims.get("resolved-claim").iterator().next());
    }

    @Test
    public void testOnDenyRedirectTo() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-on-deny-redirect.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        OIDCHttpFacade httpFacade = createHttpFacade("/api/resourcea");
        AuthorizationContext context = policyEnforcer.enforce(httpFacade);

        assertFalse(context.isGranted());
        TestResponse response = TestResponse.class.cast(httpFacade.getResponse());
        assertEquals(302, response.getStatus());
        List<String> location = response.getHeaders().getOrDefault("Location", Collections.emptyList());
        assertFalse(location.isEmpty());
        assertEquals("/accessDenied", location.get(0));
    }

    @Test
    public void testNotAuthenticatedDenyUnmapedPath() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-bearer-only.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        OIDCHttpFacade httpFacade = createHttpFacade("/api/unmmaped");
        AuthorizationContext context = policyEnforcer.enforce(httpFacade);

        assertFalse(context.isGranted());
        TestResponse response = TestResponse.class.cast(httpFacade.getResponse());
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testPublicEndpointNoBearerAbortRequest() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-bearer-only.json"));
        OIDCHttpFacade httpFacade = createHttpFacade("/api/public");
        AuthenticatedActionsHandler handler = new AuthenticatedActionsHandler(deployment, httpFacade);

        assertTrue(handler.handledRequest());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();
        httpFacade = createHttpFacade("/api/resourcea", token);
        handler = new AuthenticatedActionsHandler(deployment, httpFacade);

        assertFalse(handler.handledRequest());
    }

    @Test
    public void testMappedPathEnforcementModeDisabled() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-disabled-enforce-mode-path.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();

        OIDCHttpFacade httpFacade = createHttpFacade("/api/resource/public");
        AuthorizationContext context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        httpFacade = createHttpFacade("/api/resourceb");
        context = policyEnforcer.enforce(httpFacade);
        assertFalse(context.isGranted());
        TestResponse response = TestResponse.class.cast(httpFacade.getResponse());
        assertEquals(403, response.getStatus());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");
        String token = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), null).getAccessToken();

        httpFacade = createHttpFacade("/api/resourcea", token);
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        httpFacade = createHttpFacade("/api/resourceb", token);
        context = policyEnforcer.enforce(httpFacade);
        assertFalse(context.isGranted());
        response = TestResponse.class.cast(httpFacade.getResponse());
        assertEquals(403, response.getStatus());

        httpFacade = createHttpFacade("/api/resource/public", token);
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());
    }

    @Test
    public void testEnforcementModeDisabled() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-disabled-enforce-mode.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();

        OIDCHttpFacade httpFacade = createHttpFacade("/api/resource/public");
        policyEnforcer.enforce(httpFacade);
        TestResponse response = TestResponse.class.cast(httpFacade.getResponse());
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testDefaultWWWAuthenticateCorsHeader() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-disabled-enforce-mode-path.json"));

        deployment.setCors(true);
        Map<String, List<String>> headers = new HashMap<>();

        headers.put(CorsHeaders.ORIGIN,Arrays.asList("http://localhost:8180"));

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");
        String token = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), null).getAccessToken();
        OIDCHttpFacade httpFacade = createHttpFacade("http://server/api/resource/public", HttpMethod.OPTIONS, token, headers, Collections.emptyMap(), null, deployment);
        new AuthenticatedActionsHandler(deployment, httpFacade).handledRequest();
        assertEquals(HttpHeaders.WWW_AUTHENTICATE, headers.get(CorsHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).get(0));
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

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-match-http-verbs-scopes.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        OIDCHttpFacade httpFacade = createHttpFacade("/api/resource-with-scope", token);

        AuthorizationContext context = policyEnforcer.enforce(httpFacade);

        assertFalse("Should fail because resource does not have any scope named GET", context.isGranted());
        assertEquals(403, TestResponse.class.cast(httpFacade.getResponse()).getStatus());
        
        resource.addScope("GET", "POST");

        clientResource.authorization().resources().resource(resource.getId()).update(resource);

        deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-match-http-verbs-scopes.json"));
        policyEnforcer = deployment.getPolicyEnforcer();

        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        httpFacade = createHttpFacade("/api/resource-with-scope", token, "POST");
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        // create a PATCH scope without associated it with the resource so that a PATCH request is denied accordingly even though
        // the scope exists on the server
        clientResource.authorization().scopes().create(new ScopeRepresentation("PATCH"));
        httpFacade = createHttpFacade("/api/resource-with-scope", token, "PATCH");
        context = policyEnforcer.enforce(httpFacade);
        assertFalse(context.isGranted());

        ScopePermissionRepresentation postPermission = new ScopePermissionRepresentation();

        postPermission.setName("GET permission");
        postPermission.addScope("GET");
        postPermission.addPolicy("Always Deny Policy");

        permissions.scope().create(postPermission).close();

        httpFacade = createHttpFacade("/api/resource-with-scope", token);
        context = policyEnforcer.enforce(httpFacade);
        assertFalse(context.isGranted());

        postPermission = permissions.scope().findByName(postPermission.getName());

        postPermission.addScope("GET");
        postPermission.addPolicy("Always Grant Policy");

        permissions.scope().findById(postPermission.getId()).update(postPermission);

        AuthzClient authzClient = getAuthzClient("default-keycloak.json");
        AuthorizationResponse authorize = authzClient.authorization(token).authorize();
        token = authorize.getToken();

        httpFacade = createHttpFacade("/api/resource-with-scope", token);
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        httpFacade = createHttpFacade("/api/resource-with-scope", token, "POST");
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        postPermission = permissions.scope().findByName(postPermission.getName());
        postPermission.addScope("GET");
        postPermission.addPolicy("Always Deny Policy");
        permissions.scope().findById(postPermission.getId()).update(postPermission);
        authorize = authzClient.authorization(token).authorize();
        token = authorize.getToken();

        httpFacade = createHttpFacade("/api/resource-with-scope", token);
        context = policyEnforcer.enforce(httpFacade);
        assertFalse(context.isGranted());

        httpFacade = createHttpFacade("/api/resource-with-scope", token, "POST");
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        postPermission = permissions.scope().findByName(postPermission.getName());
        postPermission.addScope("GET");
        postPermission.addPolicy("Always Grant Policy");
        permissions.scope().findById(postPermission.getId()).update(postPermission);
        authorize = authzClient.authorization(token).authorize();
        token = authorize.getToken();

        httpFacade = createHttpFacade("/api/resource-with-scope", token);
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        httpFacade = createHttpFacade("/api/resource-with-scope", token, "POST");
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        postPermission = permissions.scope().findByName(postPermission.getName());
        postPermission.addScope("POST");
        postPermission.addPolicy("Always Deny Policy");
        permissions.scope().findById(postPermission.getId()).update(postPermission);
        AuthorizationRequest request = new AuthorizationRequest();

        request.addPermission(null, "GET");

        authorize = authzClient.authorization(token).authorize(request);
        token = authorize.getToken();

        httpFacade = createHttpFacade("/api/resource-with-scope", token);
        context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());

        httpFacade = createHttpFacade("/api/resource-with-scope", token, "POST");
        context = policyEnforcer.enforce(httpFacade);
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

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-bearer-only.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        OIDCHttpFacade httpFacade = createHttpFacade("/api/check-subject-token");
        AuthorizationContext context = policyEnforcer.enforce(httpFacade);

        assertFalse(context.isGranted());
        assertEquals(403, TestResponse.class.cast(httpFacade.getResponse()).getStatus());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        httpFacade = createHttpFacade("/api/check-subject-token", token);

        context = policyEnforcer.enforce(httpFacade);
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

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-bearer-only.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        OIDCHttpFacade httpFacade = createHttpFacade("/api/check-subject-token");

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        httpFacade = createHttpFacade("/api/check-subject-token", token);

        AuthorizationContext context = policyEnforcer.enforce(httpFacade);
        assertTrue(context.isGranted());
        
        oauth.doLogout(response.getRefreshToken(), null);

        context = policyEnforcer.enforce(httpFacade);
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

            javax.ws.rs.core.Response response = clientResource.authorization().resources().create(representation);

            representation.setId(response.readEntity(ResourceRepresentation.class).getId());

            response.close();
        }

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName("Test Permission");
        permission.setResourceType("test");
        permission.addPolicy("Only User Policy");

        PermissionsResource permissions = clientResource.authorization().permissions();
        permissions.resource().create(permission).close();

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-no-lazyload.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();

        assertEquals(205, policyEnforcer.getPaths().size());

        deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-lazyload.json"));
        policyEnforcer = deployment.getPolicyEnforcer();
        assertEquals(0, policyEnforcer.getPathMatcher().getPathCache().size());
        assertEquals(0, policyEnforcer.getPaths().size());

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        for (int i = 0; i < 101; i++) {
            policyEnforcer.enforce(createHttpFacade("/api/" + i, token));
        }

        assertEquals(101, policyEnforcer.getPathMatcher().getPathCache().size());

        for (int i = 101; i < 200; i++) {
            policyEnforcer.enforce(createHttpFacade("/api/" + i, token));
        }

        assertEquals(200, policyEnforcer.getPathMatcher().getPathCache().size());
        assertEquals(0, policyEnforcer.getPaths().size());

        ResourceRepresentation resource = clientResource.authorization().resources()
                .findByName("Root").get(0);

        clientResource.authorization().resources().resource(resource.getId()).remove();

        deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-lazyload-with-paths.json"));
        policyEnforcer = deployment.getPolicyEnforcer();

        AuthorizationContext context = policyEnforcer.enforce(createHttpFacade("/api/0", token));

        assertTrue(context.isGranted());
    }

    @Test
    public void testSetMethodConfigs() {
        ClientResource clientResource = getClientResource(RESOURCE_SERVER_CLIENT_ID);
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName(KeycloakModelUtils.generateId());
        representation.setUris(Collections.singleton("/api-method/*"));

        ResourcesResource resources = clientResource.authorization().resources();
        javax.ws.rs.core.Response response = resources.create(representation);

        representation.setId(response.readEntity(ResourceRepresentation.class).getId());

        response.close();

        try {
            KeycloakDeployment deployment = KeycloakDeploymentBuilder
                    .build(getAdapterConfiguration("enforcer-paths-use-method-config.json"));
            PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();

            oauth.realm(REALM_NAME);
            oauth.clientId("public-client-test");
            oauth.doLogin("marta", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse tokeResponse = oauth.doAccessTokenRequest(code, null);
            String token = tokeResponse.getAccessToken();

            AuthorizationContext context = policyEnforcer.enforce(createHttpFacade("/api-method/foo", token));

            // GET is disabled in the config
            assertTrue(context.isGranted());

            PolicyEnforcerConfig.PathConfig pathConfig = policyEnforcer.getPaths().get("/api-method/*");

            assertNotNull(pathConfig);
            List<PolicyEnforcerConfig.MethodConfig> methods = pathConfig.getMethods();
            assertEquals(1, methods.size());
            assertTrue(PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED.equals(methods.get(0).getScopesEnforcementMode()));

            // other verbs should be protected
            context = policyEnforcer.enforce(createHttpFacade("/api-method/foo", token, "POST"));

            assertFalse(context.isGranted());
        } finally {
            resources.resource(representation.getId()).remove();
        }
    }

    private void initAuthorizationSettings(ClientResource clientResource) {
        if (clientResource.authorization().resources().findByName("Resource A").isEmpty()) {
            JSPolicyRepresentation jsPolicy = new JSPolicyRepresentation();

            jsPolicy.setName("Always Grant Policy");

            StringBuilder code = new StringBuilder();

            code.append("$evaluation.grant();");

            jsPolicy.setCode(code.toString());

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

            StringBuilder code = new StringBuilder();

            code.append("$evaluation.deny();");

            policy.setCode(code.toString());

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

    private InputStream getAdapterConfiguration(String fileName) {
        try {
            return httpsAwareConfigurationStream(getClass().getResourceAsStream("/authorization-test/" + fileName));
        } catch (IOException e) {
            throw new AssertionError("Unexpected I/O error while dealing with configuration", e);
        }
    }

    private ResourceRepresentation createResource(ClientResource clientResource, String name, String uri, String... scopes) {
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName(name);
        representation.setUri(uri);
        representation.setScopes(Arrays.asList(scopes).stream().map(ScopeRepresentation::new).collect(Collectors.toSet()));

        javax.ws.rs.core.Response response = clientResource.authorization().resources().create(representation);

        representation.setId(response.readEntity(ResourceRepresentation.class).getId());

        response.close();

        return representation;
    }

    private ClientResource getClientResource(String name) {
        ClientsResource clients = realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation representation = clients.findByClientId(name).get(0);
        return clients.get(representation.getId());
    }

    private OIDCHttpFacade createHttpFacade(String path, String method, String token, Map<String, List<String>> headers, Map<String, List<String>> parameters, InputStream requestBody, KeycloakDeployment deployment) {
        return createHttpFacade(path, method, token, headers, parameters, requestBody, deployment, null);
    }

    private OIDCHttpFacade createHttpFacade(String path, String method, String token, Map<String, List<String>> headers, Map<String, List<String>> parameters, InputStream requestBody, KeycloakDeployment deployment, Function<String, String> parameterFunction) {
        return new OIDCHttpFacade() {
            Request request;
            Response response;

            @Override
            public KeycloakSecurityContext getSecurityContext() {
                if (token != null) {
                    AccessToken accessToken;
                    try {
                        accessToken = new JWSInput(token).readJsonContent(AccessToken.class);
                    } catch (JWSInputException cause) {
                        throw new RuntimeException(cause);
                    }
                    return new RefreshableKeycloakSecurityContext(deployment, null, token, accessToken, null, null, null);
                }
                return null;
            }

            @Override
            public Request getRequest() {
                if (request == null) {
                    request = createHttpRequest(path, method, headers, parameters, requestBody, parameterFunction);
                }
                return request;
            }

            @Override
            public Response getResponse() {
                if (response == null) {
                    response = createHttpResponse(headers);
                }
                return response;
            }

            @Override
            public X509Certificate[] getCertificateChain() {
                return new X509Certificate[0];
            }
        };
    }

    private OIDCHttpFacade createHttpFacade(String path, String token) {
        return createHttpFacade(path, null, token, new HashMap<>(), new HashMap<>(), null, null);
    }

    private OIDCHttpFacade createHttpFacade(String path, String token, String method) {
        return createHttpFacade(path, method, token, new HashMap<>(), new HashMap<>(), null, null);
    }

    private OIDCHttpFacade createHttpFacade(String path) {
        return createHttpFacade(path, null, null, new HashMap<>(), new HashMap<>(), null, null);
    }

    private OIDCHttpFacade createHttpFacade(String path, String token, Function<String, String> parameterFunction) {
        return createHttpFacade(path, null, token, new HashMap<>(), new HashMap<>(), null, null, parameterFunction);
    }

    private Response createHttpResponse(Map<String, List<String>> headers) {
        return new TestResponse(headers);
    }

    private Request createHttpRequest(String path, String method, Map<String, List<String>> headers, Map<String, List<String>> parameters, InputStream requestBody, Function<String, String> parameterFunction) {
        if (parameterFunction == null) {
            parameterFunction = param -> {
                List<String> values = parameters.getOrDefault(param, Collections.emptyList());

                if (!values.isEmpty()) {
                    return values.get(0);
                }

                return null;
            };
        }
        Function<String, String> finalParameterFunction = parameterFunction;
        return new Request() {

            private InputStream inputStream;

            @Override
            public String getMethod() {
                return method == null ? "GET" : method;
            }

            @Override
            public String getURI() {
                return path;
            }

            @Override
            public String getRelativePath() {
                return path;
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getFirstParam(String param) {
                return finalParameterFunction.apply(param);
            }

            @Override
            public String getQueryParamValue(String param) {
                return getFirstParam(param);
            }

            @Override
            public Cookie getCookie(String cookieName) {
                return null;
            }

            @Override
            public String getHeader(String name) {
                List<String> headers = getHeaders(name);

                if (!headers.isEmpty()) {
                    return headers.get(0);
                }

                return null;
            }

            @Override
            public List<String> getHeaders(String name) {
                return headers.getOrDefault(name, Collections.emptyList());
            }

            @Override
            public InputStream getInputStream() {
                return getInputStream(false);
            }

            @Override
            public InputStream getInputStream(boolean buffer) {
                if (requestBody == null) {
                    return new ByteArrayInputStream(new byte[] {});
                }

                if (inputStream != null) {
                    return inputStream;
                }

                if (buffer) {
                    return inputStream = new BufferedInputStream(requestBody);
                }

                return requestBody;
            }

            @Override
            public String getRemoteAddr() {
                return "user-remote-addr";
            }

            @Override
            public void setError(AuthenticationError error) {

            }

            @Override
            public void setError(LogoutError error) {

            }
        };
    }

    protected AuthzClient getAuthzClient(String fileName) {
        return AuthzClient.create(getAdapterConfiguration(fileName));
    }

    private class TestResponse implements Response {

        private final Map<String, List<String>> headers;
        private int status;

        public TestResponse(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        @Override
        public void setStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        @Override
        public void addHeader(String name, String value) {
            setHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            headers.put(name, Arrays.asList(value));
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        @Override
        public void resetCookie(String name, String path) {

        }

        @Override
        public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly) {

        }

        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        @Override
        public void sendError(int code) {
            status = code;
        }

        @Override
        public void sendError(int code, String message) {
            status = code;
        }

        @Override
        public void end() {

        }
    }
}
