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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationRequest.Metadata;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
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
public class EntitlementAPITest extends AbstractAuthzTest {

    private static final String RESOURCE_SERVER_TEST = "resource-server-test";
    private static final String TEST_CLIENT = "test-client";
    private static final String AUTHZ_CLIENT_CONFIG = "default-keycloak.json";
    private static final String PAIRWISE_RESOURCE_SERVER_TEST = "pairwise-resource-server-test";
    private static final String PAIRWISE_TEST_CLIENT = "test-client-pairwise";
    private static final String PAIRWISE_AUTHZ_CLIENT_CONFIG = "default-keycloak-pairwise.json";

    private AuthzClient authzClient;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("authz-test")
                .roles(RolesBuilder.create().realmRole(RoleBuilder.create().name("uma_authorization").build()))
                .user(UserBuilder.create().username("marta").password("password").addRoles("uma_authorization"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId(RESOURCE_SERVER_TEST)
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants())
                .client(ClientBuilder.create().clientId(PAIRWISE_RESOURCE_SERVER_TEST)
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .pairwise("http://pairwise.com")
                        .directAccessGrants())
                .client(ClientBuilder.create().clientId(TEST_CLIENT)
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/test-client")
                        .directAccessGrants())
                .client(ClientBuilder.create().clientId(PAIRWISE_TEST_CLIENT)
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/test-client")
                        .pairwise("http://pairwise.com")
                        .directAccessGrants())
                .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        configureAuthorization(RESOURCE_SERVER_TEST);
        configureAuthorization(PAIRWISE_RESOURCE_SERVER_TEST);
    }

    public void configureAuthorization(String clientId) throws Exception {
        ClientResource client = getClient(getRealm(), clientId);
        AuthorizationResource authorization = client.authorization();

        JSPolicyRepresentation policy = new JSPolicyRepresentation();

        policy.setName("Default Policy");
        policy.setCode("$evaluation.grant();");

        authorization.policies().js().create(policy).close();

        for (int i = 1; i <= 20; i++) {
            ResourceRepresentation resource = new ResourceRepresentation("Resource " + i);

            authorization.resources().create(resource).close();

            ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

            permission.setName(resource.getName() + " Permission");
            permission.addResource(resource.getName());
            permission.addPolicy(policy.getName());

            authorization.permissions().resource().create(permission).close();
        }
    }

    @Test
    public void testRptRequestWithoutResourceName() {
        testRptRequestWithoutResourceName(AUTHZ_CLIENT_CONFIG);
    }

    @Test
    public void testRptRequestWithoutResourceNamePairwise() {
        testRptRequestWithoutResourceName(PAIRWISE_AUTHZ_CLIENT_CONFIG);
    }

    public void testRptRequestWithoutResourceName(String configFile) {
        Metadata metadata = new Metadata();

        metadata.setIncludeResourceName(false);

        assertResponse(metadata, () -> {
            AuthorizationRequest request = new AuthorizationRequest();

            request.setMetadata(metadata);
            request.addPermission("Resource 1");

            return getAuthzClient(configFile).authorization("marta", "password").authorize(request);
        });
    }

    @Test
    public void testRptRequestWithResourceName() {
        testRptRequestWithResourceName(AUTHZ_CLIENT_CONFIG);
    }

    @Test
    public void testRptRequestWithResourceNamePairwise() {
        testRptRequestWithResourceName(PAIRWISE_AUTHZ_CLIENT_CONFIG);
    }


    public void testRptRequestWithResourceName(String configFile) {
        Metadata metadata = new Metadata();

        metadata.setIncludeResourceName(true);

        assertResponse(metadata, () -> getAuthzClient(configFile).authorization("marta", "password").authorize());

        AuthorizationRequest request = new AuthorizationRequest();

        request.setMetadata(metadata);
        request.addPermission("Resource 13");

        assertResponse(metadata, () -> getAuthzClient(configFile).authorization("marta", "password").authorize(request));

        request.setMetadata(null);

        assertResponse(metadata, () -> getAuthzClient(configFile).authorization("marta", "password").authorize(request));
    }

    @Test
    public void testPermissionLimit() {
        testPermissionLimit(AUTHZ_CLIENT_CONFIG);
    }

    @Test
    public void testPermissionLimitPairwise() {
        testPermissionLimit(PAIRWISE_AUTHZ_CLIENT_CONFIG);
    }

    public void testPermissionLimit(String configFile) {
        AuthorizationRequest request = new AuthorizationRequest();

        for (int i = 1; i <= 10; i++) {
            request.addPermission("Resource " + i);
        }

        Metadata metadata = new Metadata();

        metadata.setLimit(10);

        request.setMetadata(metadata);

        AuthorizationResponse response = getAuthzClient(configFile).authorization("marta", "password").authorize(request);
        AccessToken rpt = toAccessToken(response.getToken());

        List<Permission> permissions = rpt.getAuthorization().getPermissions();

        assertEquals(10, permissions.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Resource " + (i + 1), permissions.get(i).getResourceName());
        }

        request = new AuthorizationRequest();

        for (int i = 11; i <= 15; i++) {
            request.addPermission("Resource " + i);
        }

        request.setMetadata(metadata);
        request.setRpt(response.getToken());

        response = getAuthzClient(configFile).authorization("marta", "password").authorize(request);
        rpt = toAccessToken(response.getToken());

        permissions = rpt.getAuthorization().getPermissions();

        assertEquals(10, permissions.size());

        for (int i = 0; i < 10; i++) {
            if (i < 5) {
                assertEquals("Resource " + (i + 11), permissions.get(i).getResourceName());
            } else {
                assertEquals("Resource " + (i - 4), permissions.get(i).getResourceName());
            }
        }

        request = new AuthorizationRequest();

        for (int i = 16; i <= 18; i++) {
            request.addPermission("Resource " + i);
        }

        request.setMetadata(metadata);
        request.setRpt(response.getToken());

        response = getAuthzClient(configFile).authorization("marta", "password").authorize(request);
        rpt = toAccessToken(response.getToken());

        permissions = rpt.getAuthorization().getPermissions();

        assertEquals(10, permissions.size());
        assertEquals("Resource 16", permissions.get(0).getResourceName());
        assertEquals("Resource 17", permissions.get(1).getResourceName());
        assertEquals("Resource 18", permissions.get(2).getResourceName());
        assertEquals("Resource 11", permissions.get(3).getResourceName());
        assertEquals("Resource 12", permissions.get(4).getResourceName());
        assertEquals("Resource 13", permissions.get(5).getResourceName());
        assertEquals("Resource 14", permissions.get(6).getResourceName());
        assertEquals("Resource 15", permissions.get(7).getResourceName());
        assertEquals("Resource 1", permissions.get(8).getResourceName());
        assertEquals("Resource 2", permissions.get(9).getResourceName());

        request = new AuthorizationRequest();

        metadata.setLimit(5);
        request.setMetadata(metadata);
        request.setRpt(response.getToken());

        response = getAuthzClient(configFile).authorization("marta", "password").authorize(request);
        rpt = toAccessToken(response.getToken());

        permissions = rpt.getAuthorization().getPermissions();

        assertEquals(5, permissions.size());
        assertEquals("Resource 16", permissions.get(0).getResourceName());
        assertEquals("Resource 17", permissions.get(1).getResourceName());
        assertEquals("Resource 18", permissions.get(2).getResourceName());
        assertEquals("Resource 11", permissions.get(3).getResourceName());
        assertEquals("Resource 12", permissions.get(4).getResourceName());
    }

    @Test
    public void testResourceServerAsAudience() throws Exception {
        testResourceServerAsAudience(
                TEST_CLIENT,
                RESOURCE_SERVER_TEST,
                AUTHZ_CLIENT_CONFIG);
    }

    @Test
    public void testResourceServerAsAudienceWithPairwiseClient() throws Exception {
        testResourceServerAsAudience(
                PAIRWISE_TEST_CLIENT,
                RESOURCE_SERVER_TEST,
                AUTHZ_CLIENT_CONFIG);
    }

    @Test
    public void testPairwiseResourceServerAsAudience() throws Exception {
        testResourceServerAsAudience(
                TEST_CLIENT,
                PAIRWISE_RESOURCE_SERVER_TEST,
                PAIRWISE_AUTHZ_CLIENT_CONFIG);
    }

    @Test
    public void testPairwiseResourceServerAsAudienceWithPairwiseClient() throws Exception {
        testResourceServerAsAudience(
                PAIRWISE_TEST_CLIENT,
                PAIRWISE_RESOURCE_SERVER_TEST,
                PAIRWISE_AUTHZ_CLIENT_CONFIG);
    }

    public void testResourceServerAsAudience(String testClientId, String resourceServerClientId, String configFile) throws Exception {
        AuthorizationRequest request = new AuthorizationRequest();

        request.addPermission("Resource 1");

        String accessToken = new OAuthClient().realm("authz-test").clientId(testClientId).doGrantAccessTokenRequest("secret", "marta", "password").getAccessToken();
        AuthorizationResponse response = getAuthzClient(configFile).authorization(accessToken).authorize(request);
        AccessToken rpt = toAccessToken(response.getToken());

        assertEquals(resourceServerClientId, rpt.getAudience()[0]);
    }

    private void assertResponse(Metadata metadata, Supplier<AuthorizationResponse> responseSupplier) {
        AccessToken.Authorization authorization = toAccessToken(responseSupplier.get().getToken()).getAuthorization();

        List<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertFalse(permissions.isEmpty());

        for (Permission permission : permissions) {
            if (metadata.getIncludeResourceName()) {
                assertNotNull(permission.getResourceName());
            } else {
                assertNull(permission.getResourceName());
            }
        }
    }

    private RealmResource getRealm() throws Exception {
        return adminClient.realm("authz-test");
    }

    private ClientResource getClient(RealmResource realm, String clientId) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId(clientId).stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private AuthzClient getAuthzClient(String configFile) {
        if (authzClient == null) {
            try {
                authzClient = AuthzClient.create(JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/" + configFile), Configuration.class));
            } catch (IOException cause) {
                throw new RuntimeException("Failed to create authz client", cause);
            }
        }

        return authzClient;
    }
}
