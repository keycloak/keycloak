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
package org.keycloak.storage.adapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.organization.OrganizationProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AbstractInMemoryUserAdapterRoleTypeTest {

    @Test
    public void clientRoleMappingsFilterByTypeAndContainer() {
        RealmModel realm = realm();
        ClientModel client = client("client-id");
        ClientModel otherClient = client("other-client-id");
        RoleModel realmRole = role("realm-role", RoleModel.Type.REALM, realm);
        RoleModel clientRole = role("client-role", RoleModel.Type.CLIENT, client);
        RoleModel otherClientRole = role("other-client-role", RoleModel.Type.CLIENT, otherClient);
        TestUser user = new TestUser(session(), realm, "user-id", Map.of(
                realmRole.getId(), realmRole,
                clientRole.getId(), clientRole,
                otherClientRole.getId(), otherClientRole));

        user.grantRole(realmRole);
        user.grantRole(clientRole);
        user.grantRole(otherClientRole);

        assertEquals(
                java.util.List.of(clientRole.getName()),
                user.getClientRoleMappingsStream(client).map(RoleModel::getName).toList());
    }

    private static class TestUser extends AbstractInMemoryUserAdapter {
        private final Map<String, RoleModel> roles;

        private TestUser(KeycloakSession session, RealmModel realm, String id, Map<String, RoleModel> roles) {
            super(session, realm, id);
            this.roles = roles;
            this.realm = proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
                case "getRoleById" -> this.roles.get(args[0]);
                default -> invokeOrDefault(realm, method.getName(), args, method.getReturnType());
            });
        }

        @Override
        public SubjectCredentialManager credentialManager() {
            return null;
        }
    }

    private static KeycloakSession session() {
        OrganizationProvider organizations = proxy(OrganizationProvider.class, (providerProxy, method, args) -> defaultValue(method.getReturnType()));
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> {
            if ("getProvider".equals(method.getName()) && args[0].equals(OrganizationProvider.class)) {
                return organizations;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm() {
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm-id";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientModel client(String id) {
        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel role(String id, RoleModel.Type type, RoleContainerModel container) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> id;
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
            default -> defaultValue(method.getReturnType());
        });
    }

    private static Object invokeOrDefault(Object target, String methodName, Object[] args, Class<?> returnType) throws Throwable {
        try {
            Class<?>[] types = args == null ? new Class<?>[0] : java.util.Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
            return target.getClass().getMethod(methodName, types).invoke(target, args);
        } catch (ReflectiveOperationException ignored) {
            return defaultValue(returnType);
        }
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
