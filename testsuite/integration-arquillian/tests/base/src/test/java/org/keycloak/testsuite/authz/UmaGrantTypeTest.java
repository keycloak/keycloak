/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UmaGrantTypeTest extends AbstractResourceServerTest {

    private ResourceRepresentation resourceA;

    @Before
    public void configureAuthorization() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();

        JSPolicyRepresentation policy = new JSPolicyRepresentation();

        policy.setName("Default Policy");
        policy.setType("script-scripts/default-policy.js");

        authorization.policies().js().create(policy).close();

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        resourceA = addResource("Resource A", null, Collections.singleton("/resource"), false, "ScopeA", "ScopeB", "ScopeC");

        permission.setName(resourceA.getName() + " Permission");
        permission.addResource(resourceA.getName());
        permission.addPolicy(policy.getName());

        authorization.permissions().resource().create(permission).close();

        policy = new JSPolicyRepresentation();

        policy.setName("Deny Policy");
        policy.setType("script-scripts/always-deny-policy.js");

        authorization.policies().js().create(policy).close();
    }

    @Test
    public void testObtainRptWithClientAdditionalScopes() throws Exception {
        AuthorizationResponse response = authorize("marta", "password", "Resource A", new String[] {"ScopeA", "ScopeB"}, new String[] {"ScopeC"});
        AccessToken accessToken = toAccessToken(response.getToken());
        AccessToken.Authorization authorization = accessToken.getAuthorization();
        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB", "ScopeC");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptWithUpgrade() throws Exception {
        AuthorizationResponse response = authorize("marta", "password", "Resource A", new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();
        AccessToken.Authorization authorization = toAccessToken(rpt).getAuthorization();
        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        response = authorize("marta", "password", "Resource A", new String[] {"ScopeC"}, rpt);
        assertTrue(response.isUpgraded());

        authorization = toAccessToken(response.getToken()).getAuthorization();
        permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB", "ScopeC");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptWithUpgradeOnlyScopes() throws Exception {
        AuthorizationResponse response = authorize("marta", "password", null, new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();
        AccessToken.Authorization authorization = toAccessToken(rpt).getAuthorization();
        Collection<Permission> permissions = authorization.getPermissions();

        assertFalse(response.isUpgraded());
        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        response = authorize("marta", "password", "Resource A", new String[] {"ScopeC"}, rpt);

        authorization = toAccessToken(response.getToken()).getAuthorization();
        permissions = authorization.getPermissions();

        assertTrue(response.isUpgraded());
        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB", "ScopeC");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptWithUpgradeWithUnauthorizedResource() throws Exception {
        AuthorizationResponse response = authorize("marta", "password", "Resource A", new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();
        AccessToken.Authorization authorization = toAccessToken(rpt).getAuthorization();
        Collection<Permission> permissions = authorization.getPermissions();

        assertFalse(response.isUpgraded());
        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        ResourceRepresentation resourceB = addResource("Resource B", "ScopeA", "ScopeB", "ScopeC");

        permission.setName(resourceB.getName() + " Permission");
        permission.addResource(resourceB.getName());
        permission.addPolicy("Deny Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        try {
            authorize("marta", "password", "Resource B", new String[]{"ScopeC"}, rpt);
            fail("Should be denied, resource b not granted");
        } catch (AuthorizationDeniedException ignore) {

        }
    }

    @Test
    public void testObtainRptWithUpgradeWithUnauthorizedResourceFromRpt() throws Exception {
        ResourcePermissionRepresentation permissionA = new ResourcePermissionRepresentation();
        ResourceRepresentation resourceA = addResource(KeycloakModelUtils.generateId(), "ScopeA", "ScopeB", "ScopeC");

        permissionA.setName(resourceA.getName() + " Permission");
        permissionA.addResource(resourceA.getName());
        permissionA.addPolicy("Default Policy");

        AuthorizationResource authzResource = getClient(getRealm()).authorization();

        authzResource.permissions().resource().create(permissionA).close();
        AuthorizationResponse response = authorize("marta", "password", resourceA.getId(), new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();
        AccessToken.Authorization authorization = toAccessToken(rpt).getAuthorization();
        Collection<Permission> permissions = authorization.getPermissions();

        assertFalse(response.isUpgraded());
        assertNotNull(permissions);
        assertPermissions(permissions, resourceA.getName(), "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        ResourceRepresentation resourceB = addResource(KeycloakModelUtils.generateId(), "ScopeA", "ScopeB", "ScopeC");
        ResourcePermissionRepresentation permissionB = new ResourcePermissionRepresentation();

        permissionB.setName(resourceB.getName() + " Permission");
        permissionB.addResource(resourceB.getName());
        permissionB.addPolicy("Default Policy");

        authzResource.permissions().resource().create(permissionB).close();
        response = authorize("marta", "password", resourceB.getId(), new String[] {"ScopeC"}, rpt);
        rpt = response.getToken();
        authorization = toAccessToken(rpt).getAuthorization();
        permissions = authorization.getPermissions();

        assertTrue(response.isUpgraded());
        assertNotNull(permissions);
        assertPermissions(permissions, resourceA.getName(), "ScopeA", "ScopeB");
        assertPermissions(permissions, resourceB.getName(), "ScopeC");
        assertTrue(permissions.isEmpty());

        permissionB = authzResource.permissions().resource().findByName(permissionB.getName());
        permissionB.removePolicy("Default Policy");
        permissionB.addPolicy("Deny Policy");

        authzResource.permissions().resource().findById(permissionB.getId()).update(permissionB);

        response = authorize("marta", "password", resourceA.getId(), new String[] {"ScopeC"}, rpt);
        rpt = response.getToken();
        authorization = toAccessToken(rpt).getAuthorization();
        permissions = authorization.getPermissions();

        assertFalse(response.isUpgraded());
        assertNotNull(permissions);
        assertPermissions(permissions, resourceA.getName(), "ScopeA", "ScopeB", "ScopeC");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptOnlyAuthorizedScopes() throws Exception {
        ResourceRepresentation resourceA = addResource(KeycloakModelUtils.generateId(), "READ", "WRITE");
        ScopePermissionRepresentation permissionA = new ScopePermissionRepresentation();

        permissionA.setName(KeycloakModelUtils.generateId());
        permissionA.addScope("READ");
        permissionA.addPolicy("Default Policy");

        AuthorizationResource authzResource = getClient(getRealm()).authorization();

        authzResource.permissions().scope().create(permissionA).close();

        ScopePermissionRepresentation permissionB = new ScopePermissionRepresentation();

        permissionB.setName(KeycloakModelUtils.generateId());
        permissionB.addScope("WRITE");
        permissionB.addPolicy("Deny Policy");

        authzResource.permissions().scope().create(permissionB).close();

        AuthorizationResponse response = authorize("marta", "password", resourceA.getName(), new String[] {"READ"});
        String rpt = response.getToken();
        AccessToken.Authorization authorization = toAccessToken(rpt).getAuthorization();
        Collection<Permission> permissions = authorization.getPermissions();

        assertFalse(response.isUpgraded());
        assertNotNull(permissions);
        assertPermissions(permissions, resourceA.getName(), "READ");
        assertTrue(permissions.isEmpty());

        response = authorize("marta", "password", resourceA.getName(), new String[] {"READ", "WRITE"});
        rpt = response.getToken();
        authorization = toAccessToken(rpt).getAuthorization();
        permissions = authorization.getPermissions();

        assertFalse(response.isUpgraded());
        assertNotNull(permissions);
        assertPermissions(permissions, resourceA.getName(), "READ");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptWithOwnerManagedResource() throws Exception {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        ResourceRepresentation resourceA = addResource("Resource Marta", "marta", true, "ScopeA", "ScopeB", "ScopeC");

        permission.setName(resourceA.getName() + " Permission");
        permission.addResource(resourceA.getId());
        permission.addPolicy("Default Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        ResourceRepresentation resourceB = addResource("Resource B", "marta", "ScopeA", "ScopeB", "ScopeC");

        permission.setName(resourceB.getName() + " Permission");
        permission.addResource(resourceB.getId());
        permission.addPolicy("Default Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        AuthorizationResponse response = authorize("marta", "password",
                new PermissionRequest(resourceA.getName(), "ScopeA", "ScopeB"),
                new PermissionRequest(resourceB.getName(), "ScopeC"));
        String rpt = response.getToken();
        AccessToken.Authorization authorization = toAccessToken(rpt).getAuthorization();
        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, resourceA.getName(), "ScopeA", "ScopeB");
        assertPermissions(permissions, resourceB.getName(), "ScopeC");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptWithClientCredentials() throws Exception {
        AuthorizationResponse response = authorize("Resource A", new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");

        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptUsingAccessToken() throws Exception {
        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");
        AuthorizationResponse response = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainDecisionUsingAccessToken() throws Exception {
        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");

        // use "rsid" as "uri"
        // uri and scopes exist
        AuthorizationResponse response = authorizeDecision(accessTokenResponse.getToken(), null,
            new PermissionRequest("/resource", "ScopeA", "ScopeB"));
        assertTrue((Boolean) response.getOtherClaims().getOrDefault("result", "false"));

        // uri and scopes are empty
        try {
            response = authorizeDecision(accessTokenResponse.getToken(), null, new PermissionRequest(null));
            fail();
        } catch (Exception ignore) {
        }

        // uri is empty but scopes exist
        response = authorizeDecision(accessTokenResponse.getToken(), null, new PermissionRequest(null, "ScopeA", "ScopeB"));
        assertTrue((Boolean) response.getOtherClaims().getOrDefault("result", "false"));

        // test wild card
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        ResourceRepresentation resourceB = addResource("Resource B", null, Collections.singleton("/rs/*"), false, "ScopeD",
            "ScopeE");

        permission.setName(resourceB.getName() + " Permission");
        permission.addResource(resourceB.getName());
        permission.addPolicy("Default Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        // matchingUri is null, then result error
        try {
            response = authorizeDecision(accessTokenResponse.getToken(), null,
                new PermissionRequest("/rs/data", "ScopeD", "ScopeE"));
            fail();
        } catch (Exception ignore) {
        }

        // matchingUri is true, then result true
        response = authorizeDecision(accessTokenResponse.getToken(), true,
            new PermissionRequest("/rs/data", "ScopeD", "ScopeE"));
        assertTrue((Boolean) response.getOtherClaims().getOrDefault("result", "false"));

        // matchingUri is false, then result error
        try {
            response = authorizeDecision(accessTokenResponse.getToken(), false,
                new PermissionRequest("/rs/data", "ScopeD", "ScopeE"));
            fail();
        } catch (Exception ignore) {
        }
    }

    @Test
    public void testCORSHeadersInFailedRptRequest() throws Exception {
        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");

        UserRepresentation userRepresentation = getRealm().users().search("marta").get(0);
        UserRepresentation updatedUser = UserBuilder.edit(userRepresentation).enabled(false).build();
        getRealm().users().get(userRepresentation.getId()).update(updatedUser);

        PermissionRequest permissions = new PermissionRequest("Resource A", "ScopeA", "ScopeB");
        String ticket = getAuthzClient().protection().permission().create(Arrays.asList(permissions)).getTicket();

        String tokenEndpoint = getAuthzClient().getServerConfiguration().getTokenEndpoint();
        HttpPost post = new HttpPost(tokenEndpoint);
        post.addHeader("Origin", "http://localhost");
        post.addHeader("Authorization", "Bearer " + accessTokenResponse.getToken());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.UMA_GRANT_TYPE));
        parameters.add(new BasicNameValuePair("ticket", ticket));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        try (CloseableHttpResponse response = oauth.httpClient().get().execute(post)) {
            assertEquals(401, response.getStatusLine().getStatusCode());
            assertEquals("http://localhost", response.getFirstHeader("Access-Control-Allow-Origin").getValue());
        }
    }

    @Test
    public void testRefreshRpt() {
        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");
        AuthorizationResponse response = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));
        String rpt = response.getToken();

        assertNotNull(rpt);

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        String refreshToken = response.getRefreshToken();

        assertNotNull(refreshToken);

        AccessToken refreshTokenToken = toAccessToken(refreshToken);

        assertNotNull(refreshTokenToken.getAuthorization());

        Client client = AdminClientUtil.createResteasyClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI uri = OIDCLoginProtocolService.tokenUrl(builder).build(REALM_NAME);
        WebTarget target = client.target(uri);

        Form parameters = new Form();

        parameters.param("grant_type", OAuth2Constants.REFRESH_TOKEN);
        parameters.param(OAuth2Constants.REFRESH_TOKEN, refreshToken);

        AccessTokenResponse refreshTokenResponse = target.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("resource-server-test", "secret"))
                .post(Entity.form(parameters)).readEntity(AccessTokenResponse.class);

        assertNotNull(refreshTokenResponse.getToken());
        refreshToken = refreshTokenResponse.getRefreshToken();
        refreshTokenToken = toAccessToken(refreshToken);

        assertNotNull(refreshTokenToken.getAuthorization());

        AccessToken refreshedToken = toAccessToken(rpt);
        authorization = refreshedToken.getAuthorization();

        assertNotNull(authorization);

        permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        refreshTokenResponse = target.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("resource-server-test", "secret"))
                .post(Entity.form(parameters)).readEntity(AccessTokenResponse.class);

        assertNotNull(refreshTokenResponse.getToken());
        refreshToken = refreshTokenResponse.getRefreshToken();
        refreshTokenToken = toAccessToken(refreshToken);

        assertNotNull(refreshTokenToken.getAuthorization());

        refreshedToken = toAccessToken(rpt);
        authorization = refreshedToken.getAuthorization();

        assertNotNull(authorization);

        permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptWithIDToken() throws Exception {
        String idToken = getIdToken("marta", "password");
        AuthorizationResponse response = authorize("Resource A", new String[] {"ScopeA", "ScopeB"}, idToken, "http://openid.net/specs/openid-connect-core-1_0.html#IDToken");
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");

        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testTokenIntrospect() throws Exception {
        AuthzClient authzClient = getAuthzClient();
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("marta", "password");
        AuthorizationResponse response = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        TokenIntrospectionResponse introspectionResponse = authzClient.protection().introspectRequestingPartyToken(rpt);

        assertNotNull(introspectionResponse);
        assertNotNull(introspectionResponse.getPermissions());

        oauth.realm("authz-test").client("resource-server-test", "secret");
        String introspectHttpResponse = oauth.doIntrospectionRequest(rpt, "requesting_party_token").getRaw();

        Map jsonNode = JsonSerialization.readValue(introspectHttpResponse, Map.class);

        assertEquals(true, jsonNode.get("active"));

        Collection permissionClaims = (Collection) jsonNode.get("permissions");

        assertNotNull(permissionClaims);
        assertEquals(1, permissionClaims.size());

        Map<String, Object> claim = (Map) permissionClaims.iterator().next();

        assertThat(claim.keySet(), containsInAnyOrder("resource_id", "rsname", "resource_scopes", "scopes", "rsid"));
        assertThat(claim.get("rsname"), equalTo("Resource A"));

        ResourceRepresentation resourceRep = authzClient.protection().resource().findByName("Resource A");
        assertThat(claim.get("rsid"), equalTo(resourceRep.getId()));
        assertThat(claim.get("resource_id"), equalTo(resourceRep.getId()));

        assertThat((Collection<String>) claim.get("resource_scopes"), containsInAnyOrder("ScopeA", "ScopeB"));
        assertThat((Collection<String>) claim.get("scopes"), containsInAnyOrder("ScopeA", "ScopeB"));
    }

    @Test
    public void testNoRefreshToken() {
        ClientResource client = getClient(getRealm());
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);

        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");
        AuthorizationResponse response = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null,
            new PermissionRequest("Resource A", "ScopeA", "ScopeB"));
        String rpt = response.getToken();
        String refreshToken = response.getRefreshToken();
        assertNotNull(rpt);
        assertNull(refreshToken);

        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "true");
        client.update(clientRepresentation);
    }

    private String getIdToken(String username, String password) {
        oauth.realm("authz-test");
        oauth.client("test-app");
        oauth.openLoginForm();
        AuthorizationEndpointResponse resp = oauth.doLogin(username, password);
        String code = resp.getCode();
        org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        return response.getIdToken();
    }
}
