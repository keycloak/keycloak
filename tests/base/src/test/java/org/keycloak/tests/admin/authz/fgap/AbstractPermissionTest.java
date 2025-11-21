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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.PermissionsResource;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractPermissionTest {

    @InjectRealm(config = RealmAdminPermissionsConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(attachTo = Constants.ADMIN_PERMISSIONS_CLIENT_ID)
    ManagedClient client;

    protected static PermissionsResource getPermissionsResource(ManagedClient client) {
        return client.admin().authorization().permissions();
    }

    protected PoliciesResource getPolicies() {
        return client.admin().authorization().policies();
    }

    protected static ScopePermissionsResource getScopePermissionsResource(ManagedClient client) {
        return getPermissionsResource(client).scope();
    }

    protected static void createPermission(ManagedClient client, ScopePermissionRepresentation permission) {
        createPermission(client, permission, Response.Status.CREATED);
    }

    protected static void createPermission(ManagedClient client, ScopePermissionRepresentation permission, Response.Status expected) {
        try (Response response = getScopePermissionsResource(client).create(permission)) {
            assertEquals(expected.getStatusCode(), response.getStatus());
        }
    }

    protected static class PermissionBuilder {
        private final ScopePermissionRepresentation permission;

        static PermissionBuilder create() {
            ScopePermissionRepresentation rep = new ScopePermissionRepresentation();
            rep.setName(KeycloakModelUtils.generateId());
            return new PermissionBuilder(rep);
        }

        private PermissionBuilder(ScopePermissionRepresentation rep) {
            this.permission = rep;
        }
        ScopePermissionRepresentation build() {
            return permission;
        }
        PermissionBuilder logic(Logic logic) {
            permission.setLogic(logic);
            return this;
        }
        PermissionBuilder resourceType(String resourceType) {
            permission.setResourceType(resourceType);
            return this;
        }
        PermissionBuilder scopes(Set<String> scopes) {
            permission.setScopes(scopes);
            return this;
        }
        PermissionBuilder resources(Set<String> resources) {
            permission.setResources(resources);
            return this;
        }
        PermissionBuilder addPolicies(List<String> policies) {
            policies.forEach(policy -> permission.addPolicy(policy));
            return this;
        }
    }

    protected static UserPolicyRepresentation createUserPolicy(ManagedRealm realm, ManagedClient client, String name, String... userIds) {
        return createUserPolicy(Logic.POSITIVE, realm, client, name, userIds);
    }

    protected static UserPolicyRepresentation createUserPolicy(Logic logic, ManagedRealm realm, ManagedClient client, String name, String... userIds) {
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

    protected static GroupPolicyRepresentation createGroupPolicy(ManagedRealm realm, ManagedClient client, String name, String groupId, Logic logic) {
        GroupPolicyRepresentation policy = new GroupPolicyRepresentation();
        policy.setName(name);
        policy.addGroup(groupId);
        policy.setLogic(logic);
        try (Response response = client.admin().authorization().policies().group().create(policy)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            realm.cleanup().add(r -> {
                String policyId = r.clients().get(client.getId()).authorization().policies().group().findByName(name).getId();
                r.clients().get(client.getId()).authorization().policies().group().findById(policyId).remove();
            });
        }
        return policy;
    }

    protected static RolePolicyRepresentation createRolePolicy(ManagedRealm realm, ManagedClient client, String name, String roleId, Logic logic) {
        RolePolicyRepresentation policy = new RolePolicyRepresentation();
        policy.setName(name);
        policy.addRole(roleId);
        policy.setLogic(logic);
        try (Response response = client.admin().authorization().policies().role().create(policy)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            realm.cleanup().add(r -> {
                String policyId = r.clients().get(client.getId()).authorization().policies().group().findByName(name).getId();
                r.clients().get(client.getId()).authorization().policies().group().findById(policyId).remove();
            });
        }
        return policy;
    }

    protected static ClientPolicyRepresentation createClientPolicy(ManagedRealm realm, ManagedClient client, String name, String... clientIds) {
        ClientPolicyRepresentation policy = new ClientPolicyRepresentation();
        policy.setName(name);
        for (String clientId : clientIds) {
            policy.addClient(clientId);
        }
        policy.setLogic(Logic.POSITIVE);
        try (Response response = client.admin().authorization().policies().client().create(policy)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            realm.cleanup().add(r -> {
                ClientPolicyRepresentation clientPolicy = r.clients().get(client.getId()).authorization().policies().client().findByName(name);
                if (clientPolicy != null) {
                    r.clients().get(client.getId()).authorization().policies().client().findById(clientPolicy.getId()).remove();
                }
            });
        }
        return policy;
    }

    protected static ScopePermissionRepresentation createAllPermission(ManagedClient client, String resourceType, AbstractPolicyRepresentation policy, Set<String> scopes) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(resourceType)
                .scopes(scopes)
                .addPolicies(List.of(policy.getName()))
                .build();

        createPermission(client, permission);

        return permission;
    }

    protected ScopePermissionRepresentation createPermission(ManagedClient client, String resourceId, String resourceType, Set<String> scopes, AbstractPolicyRepresentation... policies) {
        return createPermission(client, Set.of(resourceId), resourceType, scopes, policies);
    }

    protected ScopePermissionRepresentation createPermission(ManagedClient client, Set<String> resourceIds, String resourceType, Set<String> scopes, AbstractPolicyRepresentation... policies) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(resourceType)
                .scopes(scopes)
                .resources(resourceIds)
                .addPolicies(Arrays.stream(policies).map(AbstractPolicyRepresentation::getName).collect(Collectors.toList()))
                .build();

        createPermission(client, permission);

        return permission;
    }

    protected ScopePermissionRepresentation createGroupPermission(GroupRepresentation group, Set<String> scopes, AbstractPolicyRepresentation... policies) {
        return createPermission(client, group.getId(), AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, scopes, policies);
    }
}
