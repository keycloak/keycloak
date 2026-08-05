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
package org.keycloak.authorization.policy.evaluation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultEvaluationRoleTypeTest {

    @Test
    public void realmQueriesFilterRoleMappingsByRoleType() {
        RealmModel realm = realm();
        ClientModel client = client("client-id", "public-client", realm);
        RoleModel realmRole = role("realm-role-id", "realm-role", RoleModel.Type.REALM, realm);
        RoleModel clientRole = role("client-role-id", "client-role", RoleModel.Type.CLIENT, client);
        RoleModel organizationRole = role("organization-role-id", "organization-role", RoleModel.Type.ORGANIZATION, container("organization-id"));
        UserModel user = user("user-id", realmRole, clientRole, organizationRole);
        TestSessionState state = new TestSessionState(realm, user, client, realmRole, clientRole);
        DefaultEvaluation evaluation = new DefaultEvaluation(null, null, decision -> {}, new AuthorizationProvider(state.session, state.realm, null));

        assertTrue(evaluation.getRealm().isUserInRealmRole("user-id", "realm-role"));
        assertFalse(evaluation.getRealm().isUserInRealmRole("user-id", "organization-role"));
        assertTrue(evaluation.getRealm().isUserInClientRole("user-id", "public-client", "client-role"));
        assertFalse(evaluation.getRealm().isUserInClientRole("user-id", "other-client", "client-role"));
        assertEquals(List.of("realm-role"), evaluation.getRealm().getUserRealmRoles("user-id"));
        assertEquals(List.of("client-role"), evaluation.getRealm().getUserClientRoles("user-id", "public-client"));
    }

    private static RealmModel realm() {
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm-id";
            case "getName" -> "realm";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientModel client(String id, String clientId, RealmModel realm) {
        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getClientId" -> clientId;
            case "getRealm" -> realm;
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

    private static UserModel user(String id, RoleModel... roles) {
        return proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getRoleMappingsStream" -> Stream.of(roles);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static KeycloakSession session(TestSessionState state) {
        KeycloakContext context = proxy(KeycloakContext.class, (contextProxy, method, args) -> switch (method.getName()) {
            case "getRealm" -> state.realm;
            default -> defaultValue(method.getReturnType());
        });
        UserProvider users = proxy(UserProvider.class, (usersProxy, method, args) -> switch (method.getName()) {
            case "getUserById" -> Objects.equals(args[1], state.user.getId()) ? state.user : null;
            case "getUserByUsername", "getUserByEmail" -> null;
            case "getServiceAccount" -> null;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "getContext" -> context;
            case "users" -> users;
            case "getAttribute" -> state.attributes.get(args[0]);
            case "setAttribute" -> {
                state.attributes.put((String) args[0], args[1]);
                yield null;
            }
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

    private static class TestSessionState {
        private final RealmModel realm;
        private final UserModel user;
        private final ClientModel client;
        private final RoleModel realmRole;
        private final RoleModel clientRole;
        private final Map<String, Object> attributes = new HashMap<>();
        private final KeycloakSession session;

        private TestSessionState(RealmModel realm, UserModel user, ClientModel client, RoleModel realmRole, RoleModel clientRole) {
            this.realm = proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
                case "getId" -> realm.getId();
                case "getName" -> realm.getName();
                case "getRole" -> Objects.equals(args[0], realmRole.getName()) ? realmRole : null;
                case "getClientById" -> Objects.equals(args[0], client.getId()) ? clientWithRole(client, clientRole) : null;
                default -> defaultValue(method.getReturnType());
            });
            this.user = user;
            this.client = client;
            this.realmRole = realmRole;
            this.clientRole = clientRole;
            this.session = session(this);
        }
    }

    private static ClientModel clientWithRole(ClientModel client, RoleModel role) {
        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> client.getId();
            case "getClientId" -> client.getClientId();
            case "getRealm" -> client.getRealm();
            case "getRole" -> Objects.equals(args[0], role.getName()) ? role : null;
            default -> defaultValue(method.getReturnType());
        });
    }
}
