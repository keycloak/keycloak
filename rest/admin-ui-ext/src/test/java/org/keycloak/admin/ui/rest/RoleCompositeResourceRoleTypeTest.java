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

import org.keycloak.admin.ui.rest.model.RoleMappingRepresentation;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RolePermissionEvaluator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class RoleCompositeResourceRoleTypeTest {

    @Test
    public void compositeMappingsGroupOnlyClientAndRealmRoles() {
        ClientModel client = client("client-id", "client");
        RoleModel clientRole = role("client-role-id", "client-role", RoleModel.Type.CLIENT, client, Stream.empty());
        RoleModel realmRole = role("realm-role-id", "realm-role", RoleModel.Type.REALM, container("realm"), Stream.empty());
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"), Stream.empty());
        RoleModel composite = role("composite-role-id", "composite-role", RoleModel.Type.REALM, container("realm"), Stream.of(clientRole, realmRole, organizationRole));

        RoleMappingRepresentation representation = new RoleCompositeResource(session(), realm(client, composite), auth()).getCompositeRoleMappings(composite.getId());

        assertNotNull(representation.getRealmMappings());
        assertEquals(List.of("realm-role"), representation.getRealmMappings().stream().map(RoleMappingRepresentation.RoleRepresentation::getName).toList());
        assertNotNull(representation.getClientMappings());
        assertEquals(1, representation.getClientMappings().size());
        assertEquals("client-role", representation.getClientMappings().get("client").getMappings().get(0).getName());
        assertFalse(representation.getClientMappings().containsKey("organization"));
    }

    @Test
    public void compositeMappingsReturnNullBucketsWhenOnlyOrganizationRolesArePresent() {
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"), Stream.empty());
        RoleModel composite = role("composite-role-id", "composite-role", RoleModel.Type.REALM, container("realm"), Stream.of(organizationRole));

        RoleMappingRepresentation representation = new RoleCompositeResource(session(), realm(null, composite), auth()).getCompositeRoleMappings(composite.getId());

        assertNull(representation.getRealmMappings());
        assertNull(representation.getClientMappings());
    }

    @Test
    public void compositeMappingsRejectOrganizationRoleParents() {
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"), Stream.empty());

        assertThrows(RuntimeException.class, () -> new RoleCompositeResource(session(), realm(null, organizationRole), auth())
                .getCompositeRoleMappings(organizationRole.getId()));
    }

    @Test
    public void compositeMappingsUseProviderFallback() {
        RoleModel composite = role("composite-role-id", "composite-role", RoleModel.Type.REALM, container("realm"), Stream.empty());

        RoleMappingRepresentation representation = new RoleCompositeResource(session(composite), realm(null), auth())
                .getCompositeRoleMappings(composite.getId());

        assertNull(representation.getRealmMappings());
        assertNull(representation.getClientMappings());
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

    private static RealmModel realm(ClientModel client, RoleModel... composites) {
        Map<String, RoleModel> roles = java.util.Arrays.stream(composites)
                .collect(java.util.stream.Collectors.toMap(RoleModel::getId, role -> role));
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getRoleById" -> roles.get(args[0]);
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

    private static RoleModel role(String id, String name, RoleModel.Type type, RoleContainerModel container, Stream<RoleModel> composites) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> name;
            case "getDescription" -> name + " description";
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
            case "isOrganizationRole" -> type == RoleModel.Type.ORGANIZATION;
            case "isComposite" -> true;
            case "getCompositesStream" -> composites;
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
