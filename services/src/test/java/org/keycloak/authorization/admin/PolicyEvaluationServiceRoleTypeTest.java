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
package org.keycloak.authorization.admin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.stream.Stream;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PolicyEvaluationServiceRoleTypeTest {

    @Test
    public void fallbackIdentityCopiesOnlyRealmAndClientRoleMappings() throws Exception {
        RealmModel realm = realm();
        ClientModel resourceClient = client("resource-client-id", "resource-client", realm);
        ClientModel application = client("application-id", "application", realm);
        RoleModel realmRole = role("realm-role", RoleModel.Type.REALM, realm);
        RoleModel clientRole = role("client-role", RoleModel.Type.CLIENT, application);
        RoleModel organizationRole = role("organization-role", RoleModel.Type.ORGANIZATION, container("organization-id"));
        UserModel user = user("user-id", realmRole, clientRole, organizationRole);
        KeycloakSession session = session(realm, resourceClient, user);
        ResourceServer resourceServer = proxy(ResourceServer.class, (serverProxy, method, args) -> switch (method.getName()) {
            case "getClientId" -> null;
            default -> defaultValue(method.getReturnType());
        });
        PolicyEvaluationService service = new PolicyEvaluationService(resourceServer, new AuthorizationProvider(session, realm, null), null);
        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());

        Method createIdentity = PolicyEvaluationService.class.getDeclaredMethod("createIdentity", PolicyEvaluationRequest.class);
        createIdentity.setAccessible(true);
        Identity identity = (Identity) createIdentity.invoke(service, request);

        assertTrue(identity.hasRealmRole("realm-role"));
        assertTrue(identity.hasClientRole("application", "client-role"));
        assertFalse(identity.hasRealmRole("organization-role"));
        assertFalse(identity.hasClientRole("organization-id", "organization-role"));
    }

    private static KeycloakSession session(RealmModel realm, ClientModel resourceClient, UserModel user) {
        KeycloakContext context = proxy(KeycloakContext.class, (contextProxy, method, args) -> switch (method.getName()) {
            case "getRealm" -> realmWithClient(realm, resourceClient);
            case "getUri" -> uriInfo();
            case "getConnection" -> connection();
            default -> defaultValue(method.getReturnType());
        });
        UserProvider users = proxy(UserProvider.class, (usersProxy, method, args) -> switch (method.getName()) {
            case "getUserById" -> user.getId().equals(args[1]) ? user : null;
            case "getUserByUsername" -> null;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "getContext" -> context;
            case "users" -> users;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realmWithClient(RealmModel realm, ClientModel client) {
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId" -> realm.getId();
            case "getName" -> "realm";
            case "getClientById" -> args[0] == null || client.getId().equals(args[0]) ? client : null;
            case "getClientByClientId" -> client.getClientId().equals(args[0]) ? client : null;
            default -> defaultValue(method.getReturnType());
        });
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
            case "isServiceAccountsEnabled" -> false;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleContainerModel container(String id) {
        return proxy(RoleContainerModel.class, (containerProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel role(String name, RoleModel.Type type, RoleContainerModel container) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> name;
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
            default -> defaultValue(method.getReturnType());
        });
    }

    private static UserModel user(String id, RoleModel... roles) {
        return proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getUsername" -> "user";
            case "getRoleMappingsStream" -> Stream.of(roles);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static KeycloakUriInfo uriInfo() {
        UriInfo delegate = proxy(UriInfo.class, (uriProxy, method, args) -> switch (method.getName()) {
            case "getBaseUri" -> URI.create("http://localhost");
            default -> defaultValue(method.getReturnType());
        });
        HostnameProvider hostnameProvider = proxy(HostnameProvider.class, (providerProxy, method, args) ->
                "getBaseUri".equals(method.getName()) ? URI.create("http://localhost") : defaultValue(method.getReturnType()));
        KeycloakSession session = proxy(KeycloakSession.class, (sessionProxy, method, args) ->
                "getProvider".equals(method.getName()) && args[0].equals(HostnameProvider.class)
                        ? hostnameProvider : defaultValue(method.getReturnType()));
        return new KeycloakUriInfo(session, UrlType.FRONTEND, delegate);
    }

    private static ClientConnection connection() {
        return proxy(ClientConnection.class, (connectionProxy, method, args) -> "127.0.0.1");
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
