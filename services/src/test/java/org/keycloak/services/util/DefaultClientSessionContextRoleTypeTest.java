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
package org.keycloak.services.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class DefaultClientSessionContextRoleTypeTest {

    @Test
    public void requestedAudienceFilteringRemovesOnlyClientRolesOutsideAudience() throws Exception {
        RealmModel realm = realm();
        ClientModel requestedClient = client("requested-client-id", realm);
        ClientModel otherClient = client("other-client-id", realm);
        RoleModel otherClientRole = role("other-client-role", RoleModel.Type.CLIENT, otherClient);
        UserModel user = user(otherClientRole);
        ClientScopeModel scope = clientScope(otherClientRole);
        DefaultClientSessionContext context = DefaultClientSessionContext.fromClientSessionAndClientScopes(
                clientSession(requestedClient, realm, user), Set.of(scope), null, session());
        context.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, new ClientModel[] { requestedClient });

        Method method = DefaultClientSessionContext.class.getDeclaredMethod("isClientScopePermittedForUser", ClientScopeModel.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(context, scope));
    }

    private static KeycloakSession session() {
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static AuthenticatedClientSessionModel clientSession(ClientModel client, RealmModel realm, UserModel user) {
        UserSessionModel userSession = proxy(UserSessionModel.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "getUser" -> user;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(AuthenticatedClientSessionModel.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "getClient" -> client;
            case "getRealm" -> realm;
            case "getUserSession" -> userSession;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientScopeModel clientScope(RoleModel... roles) {
        return proxy(ClientScopeModel.class, (scopeProxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> "scope";
            case "getScopeMappingsStream" -> Stream.of(roles);
            case "getProtocolMappersStream" -> Stream.empty();
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm() {
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm-id";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientModel client(String id, RealmModel realm) {
        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getRealm" -> realm;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel role(String name, RoleModel.Type type, RoleContainerModel container) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> name;
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
            case "getCompositesStream" -> Stream.empty();
            default -> defaultValue(method.getReturnType());
        });
    }

    private static UserModel user(RoleModel... roles) {
        return proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
            case "getRoleMappingsStream" -> Stream.of(roles);
            case "getGroupsStream" -> Stream.empty();
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
