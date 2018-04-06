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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.BasicAuthHelper;

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
        policy.setCode("$evaluation.grant();");

        Response response = authorization.policies().js().create(policy);
        response.close();

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        resourceA = addResource("Resource A", "ScopeA", "ScopeB", "ScopeC");

        permission.setName(resourceA.getName() + " Permission");
        permission.addResource(resourceA.getName());
        permission.addPolicy(policy.getName());

        response = authorization.permissions().resource().create(permission);
        response.close();
    }

    @Test
    public void testObtainRptWithClientAdditionalScopes() throws Exception {
        AuthorizationResponse response = authorize("marta", "password", "Resource A", new String[] {"ScopeA", "ScopeB"}, new String[] {"ScopeC"});
        AccessToken accessToken = toAccessToken(response.getToken());
        AccessToken.Authorization authorization = accessToken.getAuthorization();
        List<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB", "ScopeC");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testObtainRptWithUpgrade() throws Exception {
        AuthorizationResponse response = authorize("marta", "password", "Resource A", new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();
        AccessToken.Authorization authorization = toAccessToken(rpt).getAuthorization();
        List<Permission> permissions = authorization.getPermissions();

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
        List<Permission> permissions = authorization.getPermissions();

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

        List<Permission> permissions = authorization.getPermissions();

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

        List<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testRefreshRpt() throws Exception {
        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");
        AuthorizationResponse response = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));
        String rpt = response.getToken();

        assertNotNull(rpt);

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        List<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        String refreshToken = response.getRefreshToken();

        assertNotNull(refreshToken);

        Client client = ClientBuilder.newClient();
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

        AccessToken refreshedToken = toAccessToken(rpt);
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

        List<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");

        assertTrue(permissions.isEmpty());
    }

    private String getIdToken(String username, String password) {
        oauth.realm("authz-test");
        oauth.clientId("test-app");
        oauth.openLoginForm();
        OAuthClient.AuthorizationEndpointResponse resp = oauth.doLogin(username, password);
        String code = resp.getCode();
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, password);
        return response.getIdToken();
    }
}
