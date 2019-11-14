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
package org.keycloak.testsuite.authz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class AuthorizationTest extends AbstractAuthzTest {

    private AuthzClient authzClient;

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
        ClientResource client = getClient();
        AuthorizationResource authorization = client.authorization();

        JSPolicyRepresentation policy = new JSPolicyRepresentation();

        policy.setName("Grant Policy");
        policy.setCode("$evaluation.grant();");

        authorization.policies().js().create(policy).close();

        policy = new JSPolicyRepresentation();

        policy.setName("Deny Policy");
        policy.setCode("$evaluation.deny();");
    }

    @After
    public void onAfter() {
        ResourcesResource resources = getClient().authorization().resources();
        List<ResourceRepresentation> existingResources = resources.resources();

        for (ResourceRepresentation resource : existingResources) {
            resources.resource(resource.getId()).remove();
        }
    }

    @Test
    public void testResourceWithSameNameDifferentOwner() throws JWSInputException {
        ResourceRepresentation koloResource = createResource("Resource A", "kolo", "Scope A", "Scope B");

        createResourcePermission(koloResource, "Grant Policy");

        ResourceRepresentation martaResource = createResource("Resource A", "marta", "Scope A", "Scope B");

        createResourcePermission(martaResource, "Grant Policy");

        assertNotEquals(koloResource.getId(), martaResource.getId());

        AuthorizationRequest request = new AuthorizationRequest();

        request.addPermission("Resource A");

        List<Permission> permissions = authorize("kolo", "password", request);

        assertEquals(1, permissions.size());

        Permission permission = permissions.get(0);
        assertTrue(permission.getScopes().containsAll(Arrays.asList("Scope A", "Scope B")));

        assertEquals(koloResource.getId(), permission.getResourceId());

        permissions = authorize("marta", "password", request);

        assertEquals(1, permissions.size());

        permission = permissions.get(0);

        assertEquals(martaResource.getId(), permission.getResourceId());
        assertTrue(permission.getScopes().containsAll(Arrays.asList("Scope A", "Scope B")));
    }

    @Test
    public void testResourceServerWithSameNameDifferentOwner() {
        ResourceRepresentation koloResource = createResource("Resource A", "kolo", "Scope A", "Scope B");

        createResourcePermission(koloResource, "Grant Policy");

        ResourceRepresentation serverResource = createResource("Resource A", null, "Scope A", "Scope B");

        createResourcePermission(serverResource, "Grant Policy");

        AuthorizationRequest request = new AuthorizationRequest();

        request.addPermission("Resource A");

        List<Permission> permissions = authorize("kolo", "password", request);

        assertEquals(2, permissions.size());

        for (Permission permission : permissions) {
            assertTrue(permission.getResourceId().equals(koloResource.getId()) || permission.getResourceId().equals(serverResource.getId()));
            assertEquals("Resource A", permission.getResourceName());
        }
    }

    private List<Permission> authorize(String userName, String password, AuthorizationRequest request) {
        AuthorizationResponse response = getAuthzClient().authorization(userName, password).authorize(request);
        AccessToken token = toAccessToken(response.getToken());
        Authorization authorization = token.getAuthorization();
        return new ArrayList<>(authorization.getPermissions());
    }

    private void createResourcePermission(ResourceRepresentation resource, String... policies) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(resource.getName() + UUID.randomUUID().toString());
        permission.addResource(resource.getId());
        permission.addPolicy(policies);

        try (Response response = getClient().authorization().permissions().resource().create(permission)) {
            assertEquals(201, response.getStatus());
        }
    }

    @NotNull
    private ResourceRepresentation createResource(String name, String owner, String... scopes) {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName(name);
        resource.setOwner(owner != null ? new ResourceOwnerRepresentation(owner) : null);
        resource.addScope(scopes);

        Response response = getClient().authorization().resources().create(resource);
        ResourceRepresentation stored = response.readEntity(ResourceRepresentation.class);
        response.close();

        resource.setId(stored.getId());

        return resource;
    }

    private RealmResource getRealm() {
        return adminClient.realm("authz-test");
    }

    private ClientResource getClient() {
        ClientsResource clients = getRealm().clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private AuthzClient getAuthzClient() {
        if (authzClient == null) {
            authzClient = AuthzClient.create(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"));
        }

        return authzClient;
    }
}
