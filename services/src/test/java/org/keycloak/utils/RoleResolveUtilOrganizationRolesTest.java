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
package org.keycloak.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RoleResolveUtilOrganizationRolesTest {

    @Test
    public void resolvedGlobalRolesIgnoreOrganizationRolesAndUnknownContainers() {
        TestContext context = new TestContext();

        AccessToken.Access realmAccess = RoleResolveUtil.getResolvedRealmRoles(context.session, context.clientSessionContext, false);
        Map<String, AccessToken.Access> clientAccess = RoleResolveUtil.getAllResolvedClientRoles(context.session, context.clientSessionContext);

        assertEquals(Set.of("realm-role"), realmAccess.getRoles());
        assertEquals(Set.of("client-role"), clientAccess.get("client").getRoles());
        assertFalse(clientAccess.containsKey("organization"));
    }

    private static class TestContext {
        private final Map<String, Object> sessionAttributes = new HashMap<>();
        private final RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm";
            default -> defaultValue(method.getReturnType());
        });
        private final ClientModel client = proxy(ClientModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "client-id";
            case "getClientId" -> "client";
            case "isSurrogateAuthRequired" -> false;
            default -> defaultValue(method.getReturnType());
        });
        private final RoleContainerModel unknownContainer = proxy(RoleContainerModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "unknown";
            default -> defaultValue(method.getReturnType());
        });
        private final RoleModel realmRole = role("realm-role", false, false, realm);
        private final RoleModel clientRole = role("client-role", true, false, client);
        private final RoleModel organizationRole = role("organization-role", false, true, unknownContainer);
        private final RoleModel unknownRole = role("unknown-role", false, false, unknownContainer);
        private final UserSessionModel userSession = proxy(UserSessionModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "user-session";
            default -> defaultValue(method.getReturnType());
        });
        private final AuthenticatedClientSessionModel clientSession = proxy(AuthenticatedClientSessionModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getUserSession" -> userSession;
            case "getClient" -> client;
            default -> defaultValue(method.getReturnType());
        });
        private final ClientSessionContext clientSessionContext = proxy(ClientSessionContext.class, (proxy, method, args) -> switch (method.getName()) {
            case "getClientSession" -> clientSession;
            case "getRolesStream" -> Stream.of(realmRole, clientRole, organizationRole, unknownRole);
            default -> defaultValue(method.getReturnType());
        });
        private final KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> switch (method.getName()) {
            case "getAttribute" -> sessionAttributes.get(args[0]);
            case "setAttribute" -> {
                sessionAttributes.put((String) args[0], args[1]);
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });

        private RoleModel role(String name, boolean clientRole, boolean organizationRole, RoleContainerModel container) {
            return proxy(RoleModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getName" -> name;
                case "isClientRole" -> clientRole;
                case "isOrganizationRole" -> organizationRole;
                case "getContainer" -> container;
                default -> defaultValue(method.getReturnType());
            });
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
