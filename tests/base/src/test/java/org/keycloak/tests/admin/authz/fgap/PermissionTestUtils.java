/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Static utility methods for creating FGAP policies and permissions in tests.
 */
public final class PermissionTestUtils {

    private PermissionTestUtils() {
    }

    public static UserPolicyRepresentation createUserPolicy(ManagedRealm realm, ManagedClient client, String name, String... userIds) {
        return createUserPolicy(Logic.POSITIVE, realm, client, name, userIds);
    }

    public static UserPolicyRepresentation createUserPolicy(Logic logic, ManagedRealm realm, ManagedClient client, String name, String... userIds) {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName(name);
        for (String userId : userIds) {
            policy.addUser(userId);
        }
        policy.setLogic(logic);
        try (Response response = client.admin().authorization().policies().user().create(policy)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            realm.cleanup().add(r -> {
                UserPolicyRepresentation userPolicy = r.clients().get(client.getId()).authorization().policies().user().findByName(name);
                if (userPolicy != null) {
                    r.clients().get(client.getId()).authorization().policies().user().findById(userPolicy.getId()).remove();
                }
            });
        }
        return policy;
    }

    public static void createPermission(ManagedClient client, ScopePermissionRepresentation permission) {
        createPermission(client, permission, Response.Status.CREATED);
    }

    public static void createPermission(ManagedClient client, ScopePermissionRepresentation permission, Response.Status expected) {
        ScopePermissionsResource scopePermissions = client.admin().authorization().permissions().scope();
        try (Response response = scopePermissions.create(permission)) {
            assertEquals(expected.getStatusCode(), response.getStatus());
            if (Response.Status.CREATED.equals(expected)) {
                ScopePermissionRepresentation created = scopePermissions.findByName(permission.getName());
                assertNotNull(created);
                permission.setId(created.getId());
            }
        }
    }

    public static ScopePermissionRepresentation createPermission(ManagedClient client, String resourceId, String resourceType, Set<String> scopes, AbstractPolicyRepresentation... policies) {
        return createPermission(client, Set.of(resourceId), resourceType, scopes, policies);
    }

    public static ScopePermissionRepresentation createPermission(ManagedClient client, Set<String> resourceIds, String resourceType, Set<String> scopes, AbstractPolicyRepresentation... policies) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(resourceType);
        permission.setScopes(scopes);
        permission.setResources(resourceIds);
        Arrays.stream(policies).map(AbstractPolicyRepresentation::getName).forEach(permission::addPolicy);

        createPermission(client, permission);

        return permission;
    }

    public static ScopePermissionRepresentation createAllPermission(ManagedClient client, String resourceType, AbstractPolicyRepresentation policy, Set<String> scopes) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(resourceType);
        permission.setScopes(scopes);
        permission.addPolicy(policy.getName());

        createPermission(client, permission);

        return permission;
    }
}
