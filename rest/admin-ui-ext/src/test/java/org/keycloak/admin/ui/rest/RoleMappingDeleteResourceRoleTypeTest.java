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
package org.keycloak.admin.ui.rest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.admin.ui.rest.model.RoleDeleteRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RolePermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class RoleMappingDeleteResourceRoleTypeTest {

    @Test
    public void deleteCompositeRolesRejectsOrganizationRoleParents() {
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"));
        RoleMappingDeleteResource resource = new RoleMappingDeleteResource(session(user()), realm(organizationRole), auth(), null);

        assertThrows(RuntimeException.class, () -> resource.deleteCompositeRoles(organizationRole.getId(), List.of()));
    }

    @Test
    public void deleteUserRoleMappingsRejectsOrganizationRoles() {
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"));
        RoleMappingDeleteResource resource = new RoleMappingDeleteResource(session(user()), realm(organizationRole), auth(), null);

        assertThrows(RuntimeException.class, () -> resource.deleteUserRoleMappings("user-id",
                List.of(new RoleDeleteRequest(organizationRole.getId(), organizationRole.getName(), null))));
    }

    @Test
    public void deleteUserRoleMappingsAcceptsRealmRoles() {
        RoleModel realmRole = role("realm-role-id", "realm-role", RoleModel.Type.REALM, container("realm"));
        RoleMappingDeleteResource resource = new RoleMappingDeleteResource(session(user()), realm(realmRole), auth(), new NoopAdminEventBuilder());

        resource.deleteUserRoleMappings("user-id", List.of(new RoleDeleteRequest(realmRole.getId(), realmRole.getName(), null)));
    }

    private static KeycloakSession session(UserModel user) {
        UserProvider users = proxy(UserProvider.class, (usersProxy, method, args) -> switch (method.getName()) {
            case "getUserById" -> user;
            default -> defaultValue(method.getReturnType());
        });
        KeycloakContext context = proxy(KeycloakContext.class, (contextProxy, method, args) -> switch (method.getName()) {
            case "getUri" -> null;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "users" -> users;
            case "getContext" -> context;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm(RoleModel... roles) {
        Map<String, RoleModel> rolesById = java.util.Arrays.stream(roles)
                .collect(java.util.stream.Collectors.toMap(RoleModel::getId, role -> role));
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getRoleById" -> rolesById.get(args[0]);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static AdminPermissionEvaluator auth() {
        UserPermissionEvaluator users = proxy(UserPermissionEvaluator.class, (usersProxy, method, args) -> defaultValue(method.getReturnType()));
        RolePermissionEvaluator roles = proxy(RolePermissionEvaluator.class, (rolesProxy, method, args) -> defaultValue(method.getReturnType()));
        return proxy(AdminPermissionEvaluator.class, (authProxy, method, args) -> switch (method.getName()) {
            case "users" -> users;
            case "roles" -> roles;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static UserModel user() {
        return proxy(UserModel.class, (userProxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static RoleContainerModel container(String id) {
        return proxy(RoleContainerModel.class, (containerProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel role(String id, String name, RoleModel.Type type, RoleContainerModel container) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> name;
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
            case "isOrganizationRole" -> type == RoleModel.Type.ORGANIZATION;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static final class NoopAdminEventBuilder extends AdminEventBuilder {

        private NoopAdminEventBuilder() {
            super(eventRealm(), eventAuth(), eventSession(), eventConnection());
        }

        @Override
        public AdminEventBuilder operation(OperationType operationType) {
            return this;
        }

        @Override
        public AdminEventBuilder resource(ResourceType resourceType) {
            return this;
        }

        @Override
        public AdminEventBuilder resourcePath(UriInfo uriInfo) {
            return this;
        }

        @Override
        public void success() {
        }
    }

    private static RealmModel eventRealm() {
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> "realm";
            case "getEventsListenersStream" -> Stream.empty();
            default -> defaultValue(method.getReturnType());
        });
    }

    private static KeycloakSession eventSession() {
        KeycloakSessionFactory factory = proxy(KeycloakSessionFactory.class, (factoryProxy, method, args) -> switch (method.getName()) {
            case "getProviderFactoriesStream" -> Stream.empty();
            default -> defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "getKeycloakSessionFactory" -> factory;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static AdminAuth eventAuth() {
        RealmModel realm = eventRealm();
        ClientModel client = proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "client";
            default -> defaultValue(method.getReturnType());
        });
        UserModel user = proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "user";
            default -> defaultValue(method.getReturnType());
        });
        return new AdminAuth(realm, null, user, client);
    }

    private static ClientConnection eventConnection() {
        return proxy(ClientConnection.class, (connectionProxy, method, args) -> switch (method.getName()) {
            case "getRemoteAddr", "getRemoteHost", "getLocalAddr" -> "127.0.0.1";
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getDeclaringClass().equals(Object.class)) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> null;
                };
            }
            return handler.invoke(proxy, method, args);
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return 0;
    }
}
