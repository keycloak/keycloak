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
package org.keycloak.services.clientregistration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.provider.AbstractPersistentClientIdMetadataDocumentProvider;

import org.jboss.logging.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClientRegistrationDefaultRolesTest {

    @Test
    public void dynamicClientRegistrationDefaultRolesUseOnlyMatchingClientRoles() throws Exception {
        ClientModel client = clientWithDefaultRoleComposites();
        AbstractClientRegistrationProvider provider = new AbstractClientRegistrationProvider(null) {
        };

        assertEquals(List.of("client-role"), defaultRoles(provider, client));
    }

    @Test
    public void clientIdMetadataDefaultRolesUseOnlyMatchingClientRoles() throws Exception {
        ClientModel client = clientWithDefaultRoleComposites();
        AbstractPersistentClientIdMetadataDocumentProvider<?> provider = new TestPersistentClientIdMetadataDocumentProvider();

        assertEquals(List.of("client-role"), defaultRoles(provider, client));
    }

    private static List<String> defaultRoles(Object provider, ClientModel client) throws Exception {
        Method method = provider.getClass().getSuperclass().getDeclaredMethod("getDefaultRolesStream", ClientModel.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Stream<String> stream = (Stream<String>) method.invoke(provider, client);
        return stream.toList();
    }

    private static ClientModel clientWithDefaultRoleComposites() {
        RealmModel realm = realm();
        ClientModel client = client("client-id", realm);
        ClientModel otherClient = client("other-client-id", realm);
        RoleModel defaultRole = role("default-role", RoleModel.Type.REALM, realm);
        RoleModel clientRole = role("client-role", RoleModel.Type.CLIENT, client);
        RoleModel otherClientRole = role("other-client-role", RoleModel.Type.CLIENT, otherClient);
        RoleModel realmRole = role("realm-role", RoleModel.Type.REALM, realm);
        RoleModel organizationRole = role("organization-role", RoleModel.Type.ORGANIZATION, container("organization-id"));

        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> client.getId();
            case "getRealm" -> proxy(RealmModel.class, (realmProxy, realmMethod, realmArgs) -> switch (realmMethod.getName()) {
                case "getDefaultRole" -> proxy(RoleModel.class, (roleProxy, roleMethod, roleArgs) -> switch (roleMethod.getName()) {
                    case "getCompositesStream" -> Stream.of(clientRole, otherClientRole, realmRole, organizationRole);
                    default -> invokeOrDefault(defaultRole, roleMethod.getName(), roleArgs, roleMethod.getReturnType());
                });
                default -> invokeOrDefault(realm, realmMethod.getName(), realmArgs, realmMethod.getReturnType());
            });
            default -> invokeOrDefault(client, method.getName(), args, method.getReturnType());
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

    private static class TestPersistentClientIdMetadataDocumentProvider extends AbstractPersistentClientIdMetadataDocumentProvider<AbstractClientIdMetadataDocumentExecutor.Configuration> {

        private TestPersistentClientIdMetadataDocumentProvider() {
            super(null);
        }

        @Override
        protected Logger getLogger() {
            return Logger.getLogger(TestPersistentClientIdMetadataDocumentProvider.class);
        }

        @Override
        public AbstractClientIdMetadataDocumentExecutor.Configuration getConfiguration() {
            return null;
        }

        @Override
        public void setConfiguration(AbstractClientIdMetadataDocumentExecutor.Configuration configuration) {
        }

        @Override
        public void augmentClientMetadata(org.keycloak.representations.idm.ClientRepresentation clientRep) {
        }
    }
}
