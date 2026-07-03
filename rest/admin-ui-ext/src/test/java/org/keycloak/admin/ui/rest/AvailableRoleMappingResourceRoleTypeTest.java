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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AvailableRoleMappingResourceRoleTypeTest {

    @Test
    public void availableMappingsExcludeOnlyClientRoles() {
        ClientModel client = client("client-id", "client");
        RoleModel mappedClientRole = role("mapped-client-role-id", "mapped-client-role", RoleModel.Type.CLIENT, client);
        RoleModel mappedRealmRole = role("mapped-realm-role-id", "mapped-realm-role", RoleModel.Type.REALM, container("realm"));
        RoleModel mappedOrganizationRole = role("mapped-organization-role-id", "mapped-organization-role", RoleModel.Type.ORGANIZATION, container("organization"));
        RoleModel availableClientRole = role("available-client-role-id", "available-client-role", RoleModel.Type.CLIENT, client);
        Stream<RoleModel> mixedRoleMappings = Stream.of(mappedClientRole, mappedRealmRole, mappedOrganizationRole);
        ClientScopeModel clientScope = clientScope(mixedRoleMappings);
        ClientModel targetClient = client("target-client-id", "target-client", Stream.of(mappedRealmRole), Stream.of(mappedClientRole, mappedOrganizationRole));
        GroupModel group = group(Stream.of(mappedClientRole, mappedRealmRole, mappedOrganizationRole));
        UserModel user = user(Stream.of(mappedClientRole, mappedRealmRole, mappedOrganizationRole));
        List<List<String>> excludedIds = new ArrayList<>();
        KeycloakSession session = session(user, availableClientRole, excludedIds);
        AvailableRoleMappingResource resource = new AvailableRoleMappingResource(session,
                realm(clientScope, targetClient, group, client, user), adminAuth());

        assertEquals("available-client-role", resource.listAvailableClientScopeRoleMappings("client-scope-id", 0, 10, "").get(0).getRole());
        assertEquals("available-client-role", resource.listAvailableClientRoleMappings("target-client-id", 0, 10, "").get(0).getRole());
        assertEquals("available-client-role", resource.listAvailableGroupRoleMappings("group-id", 0, 10, "").get(0).getRole());
        assertEquals("available-client-role", resource.listAvailableUserRoleMappings("user-id", 0, 10, "").get(0).getRole());

        assertEquals(List.of(
                List.of("mapped-client-role-id"),
                List.of("mapped-client-role-id"),
                List.of("mapped-client-role-id"),
                List.of("mapped-client-role-id")
        ), excludedIds);
    }

    @Test
    public void availableCompositeMappingsRejectOrganizationRoleParents() {
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"));
        List<List<String>> excludedIds = new ArrayList<>();
        KeycloakSession session = session(user(Stream.empty()), organizationRole, excludedIds);
        AvailableRoleMappingResource resource = new AvailableRoleMappingResource(session,
                realm(clientScope(Stream.empty()), client("target-client-id", "target-client"), group(Stream.empty()), client("client-id", "client"), user(Stream.empty()), organizationRole), adminAuth());

        assertThrows(RuntimeException.class, () -> resource.listAvailableRoleMappings(organizationRole.getId(), 0, 10, ""));
    }

    @Test
    public void availableCompositeMappingsKeepMissingParentBehavior() {
        ClientModel client = client("client-id", "client");
        RoleModel availableClientRole = role("available-client-role-id", "available-client-role", RoleModel.Type.CLIENT, client);
        List<List<String>> excludedIds = new ArrayList<>();
        KeycloakSession session = session(user(Stream.empty()), availableClientRole, excludedIds);
        AvailableRoleMappingResource resource = new AvailableRoleMappingResource(session,
                realm(clientScope(Stream.empty()), client("target-client-id", "target-client"), group(Stream.empty()), client, user(Stream.empty())), adminAuth());

        assertEquals("available-client-role", resource.listAvailableRoleMappings("missing-parent-id", 0, 10, "").get(0).getRole());
        assertEquals(List.of(List.of("missing-parent-id")), excludedIds);
    }

    private static KeycloakSession session(UserModel user, RoleModel availableClientRole, List<List<String>> excludedIds) {
        RoleProvider roleProvider = proxy(RoleProvider.class, (roleProviderProxy, method, args) -> switch (method.getName()) {
            case "searchForClientRolesStream" -> {
                if (args[1] instanceof String) {
                    excludedIds.add(((Stream<String>) args[2]).toList());
                }
                yield Stream.of(availableClientRole);
            }
            default -> defaultValue(method.getReturnType());
        });
        UserProvider userProvider = proxy(UserProvider.class, (userProviderProxy, method, args) -> switch (method.getName()) {
            case "getUserById" -> user;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "roles" -> roleProvider;
            case "users" -> userProvider;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static AdminPermissionEvaluator adminAuth() {
        return proxy(AdminPermissionEvaluator.class, (authProxy, method, args) -> switch (method.getName()) {
            case "hasOneAdminRole" -> List.of((String[]) args[0]).contains(AdminRoles.MANAGE_CLIENTS)
                    || List.of((String[]) args[0]).contains(AdminRoles.MANAGE_USERS);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm(ClientScopeModel clientScope, ClientModel targetClient, GroupModel group, ClientModel roleClient, UserModel user, RoleModel... roles) {
        Map<String, ClientModel> clients = Map.of(targetClient.getId(), targetClient, roleClient.getId(), roleClient);
        Map<String, RoleModel> rolesById = new HashMap<>();
        for (RoleModel role : roles) {
            rolesById.put(role.getId(), role);
        }
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getClientScopeById" -> Objects.equals(args[0], "client-scope-id") ? clientScope : null;
            case "getClientById" -> clients.get(args[0]);
            case "getGroupById" -> Objects.equals(args[0], "group-id") ? group : null;
            case "getRoleById" -> rolesById.get(args[0]);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientScopeModel clientScope(Stream<RoleModel> roleMappings) {
        return proxy(ClientScopeModel.class, (clientScopeProxy, method, args) -> switch (method.getName()) {
            case "getScopeMappingsStream" -> roleMappings;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientModel client(String id, String clientId) {
        return client(id, clientId, Stream.empty(), Stream.empty());
    }

    private static ClientModel client(String id, String clientId, Stream<RoleModel> scopeMappings, Stream<RoleModel> roles) {
        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getClientId" -> clientId;
            case "getScopeMappingsStream" -> scopeMappings;
            case "getRolesStream" -> roles;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static GroupModel group(Stream<RoleModel> roleMappings) {
        return proxy(GroupModel.class, (groupProxy, method, args) -> switch (method.getName()) {
            case "getRoleMappingsStream" -> roleMappings;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static UserModel user(Stream<RoleModel> roleMappings) {
        return proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
            case "getRoleMappingsStream" -> roleMappings;
            default -> defaultValue(method.getReturnType());
        });
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
            case "getDescription" -> name + " description";
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
            case "isOrganizationRole" -> type == RoleModel.Type.ORGANIZATION;
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
