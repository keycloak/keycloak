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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.ui.rest.model.EffectiveRole;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RolePermissionEvaluator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AllEffectiveRoleMappingResourceRoleTypeTest {

    @Test
    public void allEffectiveMappingsExcludeOrganizationRoles() {
        ClientModel client = client("client-id", "client");
        RoleModel clientRole = role("client-role-id", "client-role", RoleModel.Type.CLIENT, client, List.of());
        RoleModel realmRole = role("realm-role-id", "realm-role", RoleModel.Type.REALM, container("realm"), List.of());
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"), List.of());
        RoleModel parent = role("parent-role-id", "parent-role", RoleModel.Type.REALM, container("realm"), List.of(clientRole, realmRole, organizationRole));
        AllEffectiveRoleMappingResource resource = new AllEffectiveRoleMappingResource(session(), realm(client, parent), auth());

        List<String> names = resource.listAllEffectiveRealmRoleMappings(parent.getId()).stream()
                .map(EffectiveRole::getName)
                .toList();

        assertTrue(names.contains("parent-role"));
        assertTrue(names.contains("realm-role"));
        assertTrue(names.contains("client-role"));
        assertFalse(names.contains("organization-role"));
    }

    @Test
    public void allEffectiveMappingsRejectOrganizationRoleParents() {
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"), List.of());
        AllEffectiveRoleMappingResource resource = new AllEffectiveRoleMappingResource(session(), realm(null, organizationRole), auth());

        assertThrows(RuntimeException.class, () -> resource.listAllEffectiveRealmRoleMappings(organizationRole.getId()));
    }

    @Test
    public void allEffectiveMappingsUseProviderFallback() {
        RoleModel parent = role("parent-role-id", "parent-role", RoleModel.Type.REALM, container("realm"), List.of());
        AllEffectiveRoleMappingResource resource = new AllEffectiveRoleMappingResource(session(parent), realm(null), auth());

        List<String> names = resource.listAllEffectiveRealmRoleMappings(parent.getId()).stream()
                .map(EffectiveRole::getName)
                .toList();

        assertTrue(names.contains("parent-role"));
    }

    private static KeycloakSession session() {
        return session(null);
    }

    private static KeycloakSession session(RoleModel fallbackRole) {
        RoleProvider roles = proxy(RoleProvider.class, (rolesProxy, method, args) -> switch (method.getName()) {
            case "getRoleById" -> fallbackRole != null && fallbackRole.getId().equals(args[1]) ? fallbackRole : null;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "roles" -> roles;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static AdminPermissionEvaluator auth() {
        RolePermissionEvaluator roles = proxy(RolePermissionEvaluator.class, (rolesProxy, method, args) -> defaultValue(method.getReturnType()));
        return proxy(AdminPermissionEvaluator.class, (authProxy, method, args) -> switch (method.getName()) {
            case "roles" -> roles;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm(ClientModel client, RoleModel... roles) {
        Map<String, RoleModel> rolesById = new HashMap<>();
        for (RoleModel role : roles) {
            rolesById.put(role.getId(), role);
        }
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getRoleById" -> rolesById.get(args[0]);
            case "getClientById" -> client != null && client.getId().equals(args[0]) ? client : null;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientModel client(String id, String clientId) {
        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getClientId" -> clientId;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleContainerModel container(String id) {
        return proxy(RoleContainerModel.class, (containerProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel role(String id, String name, RoleModel.Type type, RoleContainerModel container, List<RoleModel> composites) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> name;
            case "getDescription" -> name + " description";
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
            case "isOrganizationRole" -> type == RoleModel.Type.ORGANIZATION;
            case "getCompositesStream" -> composites.stream();
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
