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
package org.keycloak.testsuite.authz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

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
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyTest extends AbstractAuthzTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("authz-test")
                .roles(RolesBuilder.create()
                        .realmRole(RoleBuilder.create().name("uma_authorization").build())
                        .realmRole(RoleBuilder.create().name("Role A").build())
                        .realmRole(RoleBuilder.create().name("Role B").build())
                        .realmRole(RoleBuilder.create().name("Role C").build())
                )
                .group(GroupBuilder.create().name("Group A").realmRoles(Arrays.asList("Role A")).build())
                .group(GroupBuilder.create().name("Group B").realmRoles(Arrays.asList("Role C")).build())
                .user(UserBuilder.create().username("marta").password("password").addRoles("uma_authorization", "Role A"))
                .user(UserBuilder.create().username("kolo").password("password").addRoles("uma_authorization", "Role B"))
                .user(UserBuilder.create().username("alice").password("password").addRoles("uma_authorization").addGroups("Group B"))
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
        createResource("Resource A");
        createResource("Resource B");
        createResource("Resource C");

        createRealmRolePolicy("Role A Policy", "Role A");
        createRealmRolePolicy("Role B Policy", "Role B");
        createRealmRolePolicy("Role C Policy", "Role C");

        createResourcePermission("Resource A Permission", "Resource A", "Role A Policy");
        createResourcePermission("Resource B Permission", "Resource B", "Role B Policy");
        createResourcePermission("Resource C Permission", "Resource C", "Role C Policy");
    }

    @Test
    public void testUserWithExpectedRole() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest();

        request.setResourceSetName("Resource A");

        String ticket = authzClient.protection().permission().forResource(request).getTicket();
        AuthorizationResponse response = authzClient.authorization("marta", "password").authorize(new AuthorizationRequest(ticket));

        assertNotNull(response.getRpt());
    }

    @Test
    public void testUserWithoutExpectedRole() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest();

        request.setResourceSetName("Resource A");

        String ticket = authzClient.protection().permission().forResource(request).getTicket();

        try {
            authzClient.authorization("kolo", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail because user is not granted with expected role");
        } catch (AuthorizationDeniedException ignore) {

        }

        request.setResourceSetName("Resource B");
        ticket = authzClient.protection().permission().forResource(request).getTicket();
        assertNotNull(authzClient.authorization("kolo", "password").authorize(new AuthorizationRequest(ticket)));

        UserRepresentation user = getRealm().users().search("kolo").get(0);
        RoleRepresentation roleA = getRealm().roles().get("Role A").toRepresentation();
        getRealm().users().get(user.getId()).roles().realmLevel().add(Arrays.asList(roleA));

        request.setResourceSetName("Resource A");
        ticket = authzClient.protection().permission().forResource(request).getTicket();
        assertNotNull(authzClient.authorization("kolo", "password").authorize(new AuthorizationRequest(ticket)));
    }

    @Test
    public void testUserWithGroupRole() throws InterruptedException {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest();

        request.setResourceSetName("Resource C");

        String ticket = authzClient.protection().permission().forResource(request).getTicket();
        assertNotNull(authzClient.authorization("alice", "password").authorize(new AuthorizationRequest(ticket)));

        UserRepresentation user = getRealm().users().search("alice").get(0);
        GroupRepresentation groupB = getRealm().groups().groups().stream().filter(representation -> "Group B".equals(representation.getName())).findFirst().get();
        getRealm().users().get(user.getId()).leaveGroup(groupB.getId());

        try {
            authzClient.authorization("alice", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail because user is not granted with expected role");
        } catch (AuthorizationDeniedException ignore) {

        }

        request.setResourceSetName("Resource A");
        ticket = authzClient.protection().permission().forResource(request).getTicket();

        try {
            authzClient.authorization("alice", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail because user is not granted with expected role");
        } catch (AuthorizationDeniedException ignore) {

        }

        GroupRepresentation groupA = getRealm().groups().groups().stream().filter(representation -> "Group A".equals(representation.getName())).findFirst().get();
        getRealm().users().get(user.getId()).joinGroup(groupA.getId());

        assertNotNull(authzClient.authorization("alice", "password").authorize(new AuthorizationRequest(ticket)));
    }

    private void createRealmRolePolicy(String name, String... roles) {
        RolePolicyRepresentation policy = new RolePolicyRepresentation();

        policy.setName(name);

        for (String role : roles) {
            policy.addRole(role);
        }

        getClient().authorization().policies().role().create(policy);
    }

    private void createResourcePermission(String name, String resource, String... policies) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(name);
        permission.addResource(resource);
        permission.addPolicy(policies);

        getClient().authorization().permissions().resource().create(permission);
    }

    private void createResource(String name) {
        AuthorizationResource authorization = getClient().authorization();
        ResourceRepresentation resource = new ResourceRepresentation(name);

        authorization.resources().create(resource);
    }

    private RealmResource getRealm() {
        try {
            return AdminClientUtil.createAdminClient().realm("authz-test");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create admin client");
        }
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

    private ClientResource getClient() {
        return getClient(getRealm());
    }
}
