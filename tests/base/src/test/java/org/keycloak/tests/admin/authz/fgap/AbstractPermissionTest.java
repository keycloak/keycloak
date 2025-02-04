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

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.keycloak.admin.client.resource.PermissionsResource;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;

public abstract class AbstractPermissionTest {

    final static String REF_USER_MY_ADMIN = "myadmin";
    final static String REF_MY_CLIENT = "myclient";

    @InjectRealm(config = RealmAdminPermissionsConfig.class)
    ManagedRealm realm;

    @InjectUser(config = UserAdminPermissionsConfig.class, ref = REF_USER_MY_ADMIN)
    ManagedUser myAdmin;

    @InjectClient(config = ClientAdminPermissionsConfig.class, ref = REF_MY_CLIENT)
    ManagedClient myClient;

    @InjectClient(attachTo = Constants.ADMIN_PERMISSIONS_CLIENT_ID)
    ManagedClient client;

    protected PermissionsResource getPermissionsResource() {
        return client.admin().authorization().permissions();
    }

    protected PoliciesResource getPolicies() {
        return client.admin().authorization().policies();
    }

    protected ScopePermissionsResource getScopePermissionsResource() {
        return getPermissionsResource().scope();
    }

    protected void createPermission(ScopePermissionRepresentation permission) {
        this.createPermission(permission, Response.Status.CREATED);
    }

    protected void createPermission(ScopePermissionRepresentation permission, Response.Status expected) {
        try (Response response = getScopePermissionsResource().create(permission)) {
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
        PermissionBuilder name(String name) {
            permission.setName(name);
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

    public static class RealmAdminPermissionsConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.adminPermissionsEnabled(true);
        }
    }

    public static class UserAdminPermissionsConfig implements UserConfig {
        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("myadmin")
                    .name("My", "Admin")
                    .email("myadmin@localhost")
                    .emailVerified()
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_USERS);
        }
    }

    public static class ClientAdminPermissionsConfig implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("myclient")
                    .secret("mysecret")
                    .directAccessGrants();
        }
    }
}
