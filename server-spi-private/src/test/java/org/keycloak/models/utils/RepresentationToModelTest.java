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
package org.keycloak.models.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RepresentationToModelTest {

    private static final String ORGANIZATION_COMPOSITES_MESSAGE =
            "Organization role composites are only valid inside organization role import";

    @Test
    public void importRolesRejectsOrganizationCompositesFromGenericRoleDefinitions() {
        ModelException realmException = assertThrows(ModelException.class,
                () -> RepresentationToModel.importRoles(rolesWithRealmRole(organizationCompositeRole()), null));
        ModelException clientException = assertThrows(ModelException.class,
                () -> RepresentationToModel.importRoles(rolesWithClientRole(organizationCompositeRole()), null));

        assertEquals(ORGANIZATION_COMPOSITES_MESSAGE, realmException.getMessage());
        assertEquals(ORGANIZATION_COMPOSITES_MESSAGE, clientException.getMessage());
    }

    @Test
    public void importRolesAllowsEmptyGenericRoleDefinitions() {
        RepresentationToModel.importRoles(new RolesRepresentation(), null);
    }

    @Test
    public void importRolesAllowsRealmRoleDefinitionsWithoutOrganizationComposites() {
        RepresentationToModel.importRoles(rolesWithRealmRole(roleWithoutComposites("role")), realmWithRoleStorage());
    }

    @Test
    public void importRolesAllowsRoleDefinitionsWithoutOrganizationCompositesPastValidation() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> RepresentationToModel.importRoles(rolesWithClientRoles(roleWithoutComposites("role"), roleWithEmptyComposites()),
                        realmWithoutClients()));

        assertEquals("App doesn't exist in role definitions: client-a", exception.getMessage());
    }

    private static RoleRepresentation organizationCompositeRole() {
        RoleRepresentation role = new RoleRepresentation();
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setOrganization(Set.of("organization-child"));
        role.setComposites(composites);
        return role;
    }

    private static RoleRepresentation roleWithoutComposites(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }

    private static RoleRepresentation roleWithEmptyComposites() {
        RoleRepresentation role = new RoleRepresentation();
        role.setComposites(new RoleRepresentation.Composites());
        return role;
    }

    private static RolesRepresentation rolesWithRealmRole(RoleRepresentation role) {
        RolesRepresentation roles = new RolesRepresentation();
        roles.setRealm(List.of(role));
        return roles;
    }

    private static RolesRepresentation rolesWithClientRole(RoleRepresentation role) {
        return rolesWithClientRoles(role);
    }

    private static RolesRepresentation rolesWithClientRoles(RoleRepresentation... role) {
        RolesRepresentation roles = new RolesRepresentation();
        roles.setClient(Map.of("client-a", Arrays.asList(role)));
        return roles;
    }

    private static RealmModel realmWithoutClients() {
        return proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getClientByClientId" -> null;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realmWithRoleStorage() {
        Map<String, RoleModel> roles = new HashMap<>();
        RoleModel defaultRole = roleModel("default-role");
        roles.put(defaultRole.getName(), defaultRole);

        return proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getDefaultRole" -> defaultRole;
            case "addRole" -> {
                String roleName = args.length == 2 ? (String) args[1] : (String) args[0];
                RoleModel role = roleModel(roleName);
                roles.put(roleName, role);
                yield role;
            }
            case "getRole" -> roles.get((String) args[0]);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel roleModel(String name) {
        return proxy(RoleModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> name;
            case "getId" -> name;
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
