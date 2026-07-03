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
package org.keycloak.authorization.policy.provider.permission;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.policy.provider.role.RolePolicyProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class UMAPolicyProviderFactoryRoleTypeTest {

    @Test
    public void representationExportsOnlyRealmAndClientRoles() throws Exception {
        ClientModel client = client("client-id", "client");
        RoleModel clientRole = role("client-role-id", "client-role", RoleModel.Type.CLIENT, client);
        RoleModel realmRole = role("realm-role-id", "realm-role", RoleModel.Type.REALM, container("realm"));
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"));
        Policy associatedRolePolicy = policy("role-policy", RolePolicyProviderFactory.ID, Map.of("roles", rolesConfig(clientRole, realmRole, organizationRole)), Set.of());
        Policy umaPolicy = policy("uma-policy", "uma", Map.of(), Set.of(associatedRolePolicy));

        UmaPermissionRepresentation representation = new UMAPolicyProviderFactory().toRepresentation(umaPolicy, authorization(clientRole, realmRole, organizationRole));

        assertEquals(Set.of("realm-role", "client/client-role"), representation.getRoles());
        assertFalse(representation.getRoles().contains("organization-role"));
        assertFalse(representation.getRoles().contains("organization/client-role"));
    }

    private static String rolesConfig(RoleModel... roles) throws Exception {
        Set<RolePolicyRepresentation.RoleDefinition> definitions = new LinkedHashSet<>();
        for (RoleModel role : roles) {
            definitions.add(new RolePolicyRepresentation.RoleDefinition(role.getId(), false));
        }
        return JsonSerialization.writeValueAsString(definitions);
    }

    private static AuthorizationProvider authorization(RoleModel... roles) {
        RealmModel realm = realm(roles);
        PolicyProviderFactory<RolePolicyRepresentation> rolePolicyProviderFactory = new RolePolicyProviderFactory();
        KeycloakSessionFactory sessionFactory = proxy(KeycloakSessionFactory.class, (factoryProxy, method, args) -> switch (method.getName()) {
            case "getProviderFactory" -> RolePolicyProviderFactory.ID.equals(args[1]) ? rolePolicyProviderFactory : null;
            default -> defaultValue(method.getReturnType());
        });
        KeycloakSession session = proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "getKeycloakSessionFactory" -> sessionFactory;
            default -> defaultValue(method.getReturnType());
        });
        return new AuthorizationProvider(session, realm, null);
    }

    private static RealmModel realm(RoleModel... roles) {
        Map<String, RoleModel> rolesById = new HashMap<>();
        for (RoleModel role : roles) {
            rolesById.put(role.getId(), role);
        }
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getRoleById" -> rolesById.get(args[0]);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static Policy policy(String name, String type, Map<String, String> config, Set<Policy> associatedPolicies) {
        return proxy(Policy.class, (policyProxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> name;
            case "getType" -> type;
            case "getConfig" -> config;
            case "getAssociatedPolicies" -> associatedPolicies;
            case "getScopes" -> Set.of();
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

    private static RoleModel role(String id, String name, RoleModel.Type type, RoleContainerModel container) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> name;
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
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
