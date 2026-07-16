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
package org.keycloak.authorization.policy.provider.role;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class RolePolicyProviderRoleTypeTest {

    @Test
    public void evaluateChecksRoleTypeWithoutTreatingOrganizationRolesAsRealmRoles() {
        ClientModel client = client("client-id", "client");
        RoleModel clientRole = role("client-role-id", "client-role", RoleModel.Type.CLIENT, client);
        RoleModel realmRole = role("realm-role-id", "realm-role", RoleModel.Type.REALM, container("realm"));
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"));

        assertGranted(clientRole, identity(true, false));
        assertGranted(realmRole, identity(false, true));
        assertDenied(organizationRole, identity(true, true));
    }

    @Test
    public void exportConvertsOnlyClientAndRealmRolesToImportableNames() throws Exception {
        ClientModel client = client("client-id", "client");
        RoleModel clientRole = role("client-role-id", "client-role", RoleModel.Type.CLIENT, client);
        RoleModel realmRole = role("realm-role-id", "realm-role", RoleModel.Type.REALM, container("realm"));
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization"));
        RealmModel realm = realm(clientRole, realmRole, organizationRole);
        RolePolicyProviderFactory factory = new RolePolicyProviderFactory();
        Policy policy = policy("role-policy", RolePolicyProviderFactory.ID, Map.of("roles", rolesConfig(clientRole, realmRole, organizationRole)));
        PolicyRepresentation representation = new PolicyRepresentation();

        factory.onExport(policy, representation, authorization(realm));

        RolePolicyRepresentation.RoleDefinition[] exported = JsonSerialization.readValue(representation.getConfig().get("roles"), RolePolicyRepresentation.RoleDefinition[].class);

        assertEquals(3, exported.length);
        assertTrue(hasRole(exported, "client/client-role"));
        assertTrue(hasRole(exported, "realm-role"));
        assertTrue(hasRole(exported, "organization-role-id"));
        assertFalse(hasRole(exported, "organization-role"));
    }

    private static void assertGranted(RoleModel role, Identity identity) {
        Evaluation evaluation = evaluation(role, identity);

        new RolePolicyProvider((policy, authorizationProvider) -> policyRepresentation(role)).evaluate(evaluation);

        assertEquals(Effect.PERMIT, evaluation.getEffect());
    }

    private static void assertDenied(RoleModel role, Identity identity) {
        Evaluation evaluation = evaluation(role, identity);

        new RolePolicyProvider((policy, authorizationProvider) -> policyRepresentation(role)).evaluate(evaluation);

        assertNotEquals(Effect.PERMIT, evaluation.getEffect());
    }

    private static boolean hasRole(RolePolicyRepresentation.RoleDefinition[] roles, String id) {
        for (RolePolicyRepresentation.RoleDefinition role : roles) {
            if (id.equals(role.getId())) {
                return true;
            }
        }
        return false;
    }

    private static Evaluation evaluation(RoleModel role, Identity identity) {
        RealmModel realm = realm(role);
        AuthorizationProvider authorizationProvider = authorization(realm);
        Policy policy = policy("policy", RolePolicyProviderFactory.ID, Map.of());
        EvaluationContext context = proxy(EvaluationContext.class, (contextProxy, method, args) -> switch (method.getName()) {
            case "getIdentity" -> identity;
            default -> defaultValue(method.getReturnType());
        });
        final Effect[] effect = new Effect[] { null };
        return proxy(Evaluation.class, (evaluationProxy, method, args) -> switch (method.getName()) {
            case "getPolicy" -> policy;
            case "getAuthorizationProvider" -> authorizationProvider;
            case "getContext" -> context;
            case "grant" -> {
                effect[0] = Effect.PERMIT;
                yield null;
            }
            case "getEffect" -> effect[0];
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RolePolicyRepresentation policyRepresentation(RoleModel role) {
        RolePolicyRepresentation representation = new RolePolicyRepresentation();
        representation.setRoles(Set.of(new RolePolicyRepresentation.RoleDefinition(role.getId(), false)));
        return representation;
    }

    private static String rolesConfig(RoleModel... roles) throws Exception {
        Set<RolePolicyRepresentation.RoleDefinition> definitions = new java.util.LinkedHashSet<>();
        for (RoleModel role : roles) {
            definitions.add(new RolePolicyRepresentation.RoleDefinition(role.getId(), false));
        }
        return JsonSerialization.writeValueAsString(definitions);
    }

    private static AuthorizationProvider authorization(RealmModel realm) {
        KeycloakContext context = proxy(KeycloakContext.class, (contextProxy, method, args) -> switch (method.getName()) {
            case "getRealm" -> realm;
            default -> defaultValue(method.getReturnType());
        });
        KeycloakSession session = proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "getContext" -> context;
            default -> defaultValue(method.getReturnType());
        });
        return new AuthorizationProvider(session, realm, null);
    }

    private static Identity identity(boolean hasClientRole, boolean hasRealmRole) {
        return proxy(Identity.class, (identityProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "identity";
            case "hasClientRole" -> hasClientRole;
            case "hasRealmRole" -> hasRealmRole;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm(RoleModel... roles) {
        Map<String, RoleModel> rolesById = new HashMap<>();
        for (RoleModel role : roles) {
            rolesById.put(role.getId(), role);
        }
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getRoleById" -> rolesById.get(args[0]);
            case "getClientById" -> rolesById.values().stream()
                    .filter(role -> role.getType() == RoleModel.Type.CLIENT)
                    .map(role -> (ClientModel) role.getContainer())
                    .filter(client -> client.getId().equals(args[0]))
                    .findFirst()
                    .orElse(null);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static Policy policy(String name, String type, Map<String, String> config) {
        return proxy(Policy.class, (policyProxy, method, args) -> switch (method.getName()) {
            case "getName" -> name;
            case "getType" -> type;
            case "getConfig" -> config;
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
