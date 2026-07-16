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

package org.keycloak.broker.provider.mappersync;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class RoleConfigPropertyByRoleNameSynchronizerTest {

    @Test
    public void realmRoleRenameUpdatesUnqualifiedMapperReference() {
        IdentityProviderMapperModel mapper = mapper("owner");
        TestIdentityProviders identityProviders = new TestIdentityProviders(mapper);
        RealmModel realm = realm();
        RoleModel role = role(RoleModel.Type.REALM, realm);

        RoleConfigPropertyByRoleNameSynchronizer.INSTANCE.handleEvent(event(realm, role, identityProviders, "owner", "admin"));

        assertEquals(1, identityProviders.queries);
        assertEquals(Map.of(ConfigConstants.ROLE, "owner"), identityProviders.lastOptions);
        assertEquals(1, identityProviders.updates);
        assertSame(mapper, identityProviders.updatedMapper);
        assertEquals("admin", mapper.getConfig().get(ConfigConstants.ROLE));
    }

    @Test
    public void clientRoleRenameUsesPublicClientIdInMapperReference() {
        IdentityProviderMapperModel mapper = mapper("public-client.owner");
        TestIdentityProviders identityProviders = new TestIdentityProviders(mapper);
        RealmModel realm = realm();
        ClientModel client = client("client-internal-id", "public-client", realm);
        RoleModel role = role(RoleModel.Type.CLIENT, client);

        RoleConfigPropertyByRoleNameSynchronizer.INSTANCE.handleEvent(event(realm, role, identityProviders, "owner", "admin"));

        assertEquals(1, identityProviders.queries);
        assertEquals(Map.of(ConfigConstants.ROLE, "public-client.owner"), identityProviders.lastOptions);
        assertEquals(1, identityProviders.updates);
        assertEquals("public-client.admin", mapper.getConfig().get(ConfigConstants.ROLE));
    }

    @Test
    public void organizationRoleRenameDoesNotQueryOrMutateNameBasedMappers() {
        IdentityProviderMapperModel mapper = mapper("owner");
        TestIdentityProviders identityProviders = new TestIdentityProviders(mapper);
        RealmModel realm = realm();
        RoleModel role = role(RoleModel.Type.ORGANIZATION, container("organization-id"));

        RoleConfigPropertyByRoleNameSynchronizer.INSTANCE.handleEvent(event(realm, role, identityProviders, "owner", "admin"));

        assertEquals(0, identityProviders.queries);
        assertEquals(0, identityProviders.updates);
        assertEquals("owner", mapper.getConfig().get(ConfigConstants.ROLE));
    }

    private static RoleModel.RoleNameChangeEvent event(RealmModel realm, RoleModel role,
                                                       TestIdentityProviders identityProviders,
                                                       String previousName, String newName) {
        KeycloakSession session = proxy(KeycloakSession.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "identityProviders" -> identityProviders.provider;
            default -> defaultValue(method.getReturnType());
        });
        return new RoleModel.RoleNameChangeEvent() {
            @Override
            public String getNewName() {
                return newName;
            }

            @Override
            public String getPreviousName() {
                return previousName;
            }

            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public RoleModel getRole() {
                return role;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
    }

    private static IdentityProviderMapperModel mapper(String roleReference) {
        IdentityProviderMapperModel mapper = new IdentityProviderMapperModel();
        mapper.setId("mapper-id");
        mapper.setName("mapper");
        mapper.setIdentityProviderAlias("idp");
        mapper.setConfig(new HashMap<>(Map.of(ConfigConstants.ROLE, roleReference)));
        return mapper;
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

    private static RoleModel role(RoleModel.Type type, RoleContainerModel container) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "role-id";
            case "getName" -> "admin";
            case "getType" -> type;
            case "getContainer" -> container;
            case "getContainerId" -> container.getId();
            case "isRealmRole" -> type == RoleModel.Type.REALM;
            case "isClientRole" -> type == RoleModel.Type.CLIENT;
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

    private static class TestIdentityProviders {
        private final IdentityProviderMapperModel mapper;
        private final IdentityProviderStorageProvider provider;
        private int queries;
        private int updates;
        private Map<String, String> lastOptions;
        private IdentityProviderMapperModel updatedMapper;

        @SuppressWarnings("unchecked")
        private TestIdentityProviders(IdentityProviderMapperModel mapper) {
            this.mapper = mapper;
            this.provider = proxy(IdentityProviderStorageProvider.class, (providerProxy, method, args) -> switch (method.getName()) {
                case "getMappersStream" -> {
                    queries++;
                    lastOptions = Map.copyOf((Map<String, String>) args[0]);
                    yield Objects.equals(mapper.getConfig().get(ConfigConstants.ROLE), lastOptions.get(ConfigConstants.ROLE))
                            ? Stream.of(mapper) : Stream.empty();
                }
                case "updateMapper" -> {
                    updates++;
                    updatedMapper = (IdentityProviderMapperModel) args[0];
                    yield null;
                }
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            });
        }
    }
}
