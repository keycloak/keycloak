/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ResourcePermissionResource;
import org.keycloak.admin.client.resource.ResourcePermissionsResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.representation.ScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePermissionManagementTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("authz-test")
                .user(UserBuilder.create().username("marta").password("password"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants())
                .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        createResourcesAndScopes();
        RealmResource realm = getRealm();
        createPolicies(realm, getClient(realm));
    }

    @Test
    public void testCreateResourcePermission() {
        AuthorizationResource authorization = getClient(getRealm()).authorization();
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName("Resource A Permission");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addResource(getResourceId("Resource A", authorization));
        representation.addPolicies(getPolicyIds(Arrays.asList("Only Marta Policy", "Only Kolo Policy"), authorization).stream().toArray((IntFunction<String[]>) value -> new String[value]));

        assertCreated(authorization, representation);
    }

    @Test
    public void testCreateResourceType() {
        AuthorizationResource authorization = getClient(getRealm()).authorization();
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName("Resource A Type Permission");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.setResourceType("test-resource");
        representation.addPolicies(getPolicyIds(Arrays.asList("Only Marta Policy"), authorization).stream().toArray((IntFunction<String[]>) value -> new String[value]));

        assertCreated(authorization, representation);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient(getRealm()).authorization();
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName("Test Delete Permission");
        representation.setResourceType("test-resource");
        representation.addPolicies(getPolicyIds(Arrays.asList("Only Marta Policy"), authorization).stream().toArray((IntFunction<String[]>) value -> new String[value]));

        ResourcePermissionsResource permissions = authorization.permissions().resource();
        Response response = permissions.create(representation);
        ResourcePermissionRepresentation created = response.readEntity(ResourcePermissionRepresentation.class);

        permissions.findById(created.getId()).remove();

        ResourcePermissionResource removed = permissions.findById(created.getId());

        try {
            removed.toRepresentation();
            fail("Permission not removed");
        } catch (NotFoundException ignore) {

        }
    }

    @Test
    public void failCreateWithSameName() {
        AuthorizationResource authorization = getClient(getRealm()).authorization();
        ResourcePermissionRepresentation permission1 = new ResourcePermissionRepresentation();

        permission1.setName("Conflicting Name Permission");
        permission1.setResourceType("test-resource");
        permission1.addPolicies(getPolicyIds(Arrays.asList("Only Marta Policy"), authorization).stream().toArray((IntFunction<String[]>) value -> new String[value]));

        ResourcePermissionsResource permissions = authorization.permissions().resource();

        permissions.create(permission1);

        ResourcePermissionRepresentation permission2 = new ResourcePermissionRepresentation();

        permission2.setName(permission1.getName());

        Response response = permissions.create(permission2);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    private void assertCreated(AuthorizationResource authorization, ResourcePermissionRepresentation representation) {
        ResourcePermissionsResource permissions = authorization.permissions().resource();
        Response response = permissions.create(representation);
        ResourcePermissionRepresentation created = response.readEntity(ResourcePermissionRepresentation.class);

        assertNotNull(created);
        assertNotNull(created.getId());

        ResourcePermissionResource permission = permissions.findById(created.getId());
        ResourcePermissionRepresentation found = permission.toRepresentation();

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals(created.getName(), found.getName());
        assertEquals(created.getDescription(), found.getDescription());
        assertEquals(created.getDecisionStrategy(), found.getDecisionStrategy());
        assertEquals(created.getLogic(), found.getLogic());
        assertEquals(created.getResourceType(), found.getResourceType());
        assertNull(found.getResources());
        assertNull(found.getPolicies());

        assertEquals(representation.getPolicies().size(), permission.associatedPolicies().stream().map(representation1 -> representation1.getId()).filter(policyId -> representation.getPolicies().contains(policyId)).count());

        if (representation.getResources() != null) {
            assertEquals(representation.getResources().size(), permission.resources().stream().map(representation1 -> representation1.getId()).filter(resourceId -> representation.getResources().contains(resourceId)).count());
        } else {
            assertTrue(permission.resources().isEmpty());
        }
    }

    private void createResourcesAndScopes() throws IOException {
        AuthzClient authzClient = getAuthzClient();
        Set<ScopeRepresentation> scopes = new HashSet<>();

        scopes.add(new ScopeRepresentation("read"));
        scopes.add(new ScopeRepresentation("write"));
        scopes.add(new ScopeRepresentation("execute"));

        List<ResourceRepresentation> resources = new ArrayList<>();

        resources.add(new ResourceRepresentation("Resource A", scopes));
        resources.add(new ResourceRepresentation("Resource B", scopes));
        resources.add(new ResourceRepresentation("Resource C", scopes));

        resources.forEach(resource -> authzClient.protection().resource().create(resource));
    }

    private void createPolicies(RealmResource realm, ClientResource client) throws IOException {
        createUserPolicy("Only Marta Policy", realm, client, "marta");
        createUserPolicy("Only Kolo Policy", realm, client, "kolo");
    }

    private void createUserPolicy(String name, RealmResource realm, ClientResource client, String username) throws IOException {
        String userId = realm.users().search(username).stream().map(representation -> representation.getId()).findFirst().orElseThrow(() -> new RuntimeException("Expected user [userId]"));

        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setName(name);
        representation.setType("user");

        Map<String, String> config = new HashMap<>();

        config.put("users", JsonSerialization.writeValueAsString(new String[] {userId}));

        representation.setConfig(config);

        client.authorization().policies().create(representation);
    }

    private ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private RealmResource getRealm() {
        try {
            return AdminClientUtil.createAdminClient().realm("authz-test");
        } catch (Exception cause) {
            throw new RuntimeException("Failed to create admin client", cause);
        }
    }

    private String getResourceId(String resourceName, AuthorizationResource authorization) {
        return authorization.resources().findByName(resourceName).stream().map(representation -> representation.getId()).findFirst().orElseThrow(() -> new RuntimeException("Expected user [userId]"));
    }

    private List<String> getPolicyIds(List<String> policies, AuthorizationResource authorization) {
        return policies.stream().map(policyName -> authorization.policies().findByName(policyName).getId()).collect(Collectors.toList());
    }

    private AuthzClient getAuthzClient() {
        try {
            return AuthzClient.create(JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"), Configuration.class));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to create authz client", cause);
        }
    }
}
