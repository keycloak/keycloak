/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.authz.fgap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class UserResourceTypePermissionTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @InjectUser(ref = "bob")
    ManagedUser userBob;

    @BeforeEach
    public void onBefore() {
        for (int i = 0; i < 3; i++) {
            UserPolicyRepresentation policy = new UserPolicyRepresentation();

            policy.setName("User Policy " + i);

            client.admin().authorization().policies().user().create(policy).close();
        }
    }

    @AfterEach
    public void onAfter() {
        ScopePermissionsResource permissions = getScopePermissionsResource();

        for (ScopePermissionRepresentation permission : permissions.findAll(null, null, null, -1, -1)) {
            permissions.findById(permission.getId()).remove();
        }
    }

    @Test
    public void testCreateResourceTypePermission() {
        ScopePermissionRepresentation expected = createAllUserPermission();
        List<ScopePermissionRepresentation> result = getScopePermissionsResource().findAll(null, null, null, -1, -1);
        assertEquals(1, result.size());
        ScopePermissionRepresentation permissionRep = result.get(0);
        ScopePermissionResource permission = getScopePermissionsResource().findById(permissionRep.getId());
        assertEquals(expected.getName(), permissionRep.getName());
        assertEquals(AdminPermissionsSchema.USERS.getScopes().size(), permission.scopes().size());
        assertEquals(3, permission.associatedPolicies().size());
    }

    @Test
    public void testCreateResourceObjectPermission() {
        ScopePermissionRepresentation expected = createUserPermission(userAlice);
        List<ScopePermissionRepresentation> result = getScopePermissionsResource().findAll(null, null, null, -1, -1);
        assertEquals(1, result.size());
        ScopePermissionRepresentation permissionRep = result.get(0);
        ScopePermissionResource permission = getScopePermissionsResource().findById(permissionRep.getId());
        assertEquals(expected.getName(), permissionRep.getName());
        assertEquals(AdminPermissionsSchema.USERS.getScopes().size(), permission.scopes().size());
        assertEquals(1, permission.resources().size());
        assertEquals(3, permission.associatedPolicies().size());
    }

    @Test
    public void testFindByResourceObject() {
        createUserPermission(userAlice, userBob);

        List<ScopePermissionRepresentation> existing = getScopePermissionsResource().findAll(null, null, userAlice.getId(), -1, -1);
        assertEquals(1, existing.size());
        existing = getScopePermissionsResource().findAll(null, null, userBob.getId(), -1, -1);
        assertEquals(1, existing.size());
    }

    @Test
    public void testDelete() {
        createUserPermission(userAlice);
        createUserPermission(userBob);

        List<ScopePermissionRepresentation> existing = getScopePermissionsResource().findAll(null, null, userAlice.getId(), -1, -1);
        assertEquals(1, existing.size());
        getScopePermissionsResource().findById(existing.get(0).getId()).remove();
        existing = getScopePermissionsResource().findAll(null, null, userAlice.getId(), -1, -1);
        assertThat(existing, nullValue());

        existing = getScopePermissionsResource().findAll(null, null, userBob.getId(), -1, -1);
        assertEquals(1, existing.size());
    }

    private ScopePermissionRepresentation createUserPermission(ManagedUser... users) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .resources(Arrays.stream(users).map(ManagedUser::getUsername).collect(Collectors.toSet()))
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .addPolicies(List.of("User Policy 0", "User Policy 1", "User Policy 2"))
                .build();

        createPermission(permission);

        return permission;
    }

    private ScopePermissionRepresentation createAllUserPermission() {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .addPolicies(List.of("User Policy 0", "User Policy 1", "User Policy 2"))
                .build();

        createPermission(permission);

        return permission;
    }
}
