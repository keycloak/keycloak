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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.AuthorizationRequest;
import org.keycloak.authorization.client.representation.AuthorizationResponse;
import org.keycloak.authorization.client.representation.PermissionRequest;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionClaimTest extends AbstractAuthzTest {

    private JSPolicyRepresentation claimAPolicy;
    private JSPolicyRepresentation claimBPolicy;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("authz-test")
                .roles(RolesBuilder.create().realmRole(RoleBuilder.create().name("uma_authorization").build()))
                .user(UserBuilder.create().username("marta").password("password").addRoles("uma_authorization"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .redirectUris("http://localhost/resource-server-test")
                    .defaultRoles("uma_protection")
                    .directAccessGrants())
                .client(ClientBuilder.create().clientId("test-client")
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .redirectUris("http://localhost/test-client")
                    .directAccessGrants())
                .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();

        claimAPolicy = new JSPolicyRepresentation();

        claimAPolicy.setName("Claim A Policy");
        claimAPolicy.setCode("$evaluation.getPermission().addClaim('claim-a', 'claim-a');$evaluation.getPermission().addClaim('claim-a', 'claim-a1');$evaluation.grant();");

        authorization.policies().js().create(claimAPolicy).close();

        claimBPolicy = new JSPolicyRepresentation();

        claimBPolicy.setName("Policy Claim B");
        claimBPolicy.setCode("$evaluation.getPermission().addClaim('claim-b', 'claim-b');$evaluation.grant();");

        authorization.policies().js().create(claimBPolicy).close();
    }

    @Test
    public void testPermissionWithClaims() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();
        ResourceRepresentation resource = new ResourceRepresentation("Resource A");

        authorization.resources().create(resource).close();

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getName());
        permission.addPolicy(claimAPolicy.getName());

        authorization.permissions().resource().create(permission).close();

        PermissionRequest request = new PermissionRequest();

        request.setResourceSetName(resource.getName());

        String accessToken = new OAuthClient().realm("authz-test").clientId("test-client").doGrantAccessTokenRequest("secret", "marta", "password").getAccessToken();
        AuthzClient authzClient = getAuthzClient();
        String ticket = authzClient.protection().permission().forResource(request).getTicket();
        AuthorizationResponse response = authzClient.authorization(accessToken).authorize(new AuthorizationRequest(ticket));

        assertNotNull(response.getRpt());
        AccessToken rpt = toAccessToken(response.getRpt());
        Authorization authorizationClaim = rpt.getAuthorization();
        List<Permission> permissions = authorizationClaim.getPermissions();

        assertEquals(1, permissions.size());

        assertTrue(permissions.get(0).getClaims().get("claim-a").containsAll(Arrays.asList("claim-a", "claim-a1")));
    }

    @Test
    public void testPermissionWithClaimsDifferentPolicies() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();

        ResourceRepresentation resource = new ResourceRepresentation("Resource B");

        authorization.resources().create(resource).close();

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getName());
        permission.addPolicy(claimAPolicy.getName(), claimBPolicy.getName());

        authorization.permissions().resource().create(permission).close();

        PermissionRequest request = new PermissionRequest();

        request.setResourceSetName(resource.getName());

        String accessToken = new OAuthClient().realm("authz-test").clientId("test-client").doGrantAccessTokenRequest("secret", "marta", "password").getAccessToken();
        AuthzClient authzClient = getAuthzClient();
        String ticket = authzClient.protection().permission().forResource(request).getTicket();
        AuthorizationResponse response = authzClient.authorization(accessToken).authorize(new AuthorizationRequest(ticket));

        assertNotNull(response.getRpt());
        AccessToken rpt = toAccessToken(response.getRpt());
        Authorization authorizationClaim = rpt.getAuthorization();
        List<Permission> permissions = authorizationClaim.getPermissions();

        assertEquals(1, permissions.size());

        Map<String, Set<String>> claims = permissions.get(0).getClaims();

        assertTrue(claims.containsKey("claim-a"));
        assertTrue(claims.containsKey("claim-b"));
    }

    private RealmResource getRealm() throws Exception {
        return adminClient.realm("authz-test");
    }

    private ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private AuthzClient getAuthzClient() {
        try {
            return AuthzClient.create(JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"), Configuration.class));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to create authz client", cause);
        }
    }
}
