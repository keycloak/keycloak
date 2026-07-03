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
package org.keycloak.broker.saml.mappers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AbstractAttributeToRoleMapperTest {

    @Test
    public void updateBrokeredUserChecksExistingMappingsByRoleType() {
        RealmModel realm = realm();
        ClientModel client = client("client-id", realm);
        RoleModel realmRole = role("realm-role", RoleModel.Type.REALM, realm);
        RoleModel clientRole = role("client-role", RoleModel.Type.CLIENT, client);
        RoleModel organizationRole = role("organization-role", RoleModel.Type.ORGANIZATION, container("organization-id"));

        assertGrantAndDelete(realm, realmRole);
        assertGrantAndDelete(realm, clientRole);
        assertGrantAndDelete(realm, organizationRole);
    }

    private static void assertGrantAndDelete(RealmModel realm, RoleModel role) {
        IdentityProviderMapperModel mapperModel = mapper(role.getName());
        KeycloakSession session = session(realm, role);
        TestUser missing = new TestUser();
        TestMapper applies = new TestMapper(true);

        applies.updateBrokeredUser(session, realm, missing.model, mapperModel, brokeredIdentityContext());

        assertEquals(1, missing.grants);

        TestUser existing = new TestUser(role);
        TestMapper doesNotApply = new TestMapper(false);

        doesNotApply.updateBrokeredUser(session, realm, existing.model, mapperModel, brokeredIdentityContext());

        assertEquals(1, existing.deletes);
    }

    private static IdentityProviderMapperModel mapper(String roleName) {
        IdentityProviderMapperModel mapper = new IdentityProviderMapperModel();
        mapper.setName("mapper");
        mapper.setConfig(new HashMap<>(Map.of(ConfigConstants.ROLE, roleName)));
        return mapper;
    }

    private static BrokeredIdentityContext brokeredIdentityContext() {
        IdentityProviderModel idp = new IdentityProviderModel();
        idp.setAlias("idp");
        idp.setEnabled(true);
        return new BrokeredIdentityContext(idp);
    }

    private static KeycloakSession session(RealmModel realm, RoleModel role) {
        AlternativeLookupProvider lookup = proxy(AlternativeLookupProvider.class, (lookupProxy, method, args) -> switch (method.getName()) {
            case "lookupRoleFromString" -> Objects.equals(args[1], role.getName()) ? role : null;
            default -> defaultValue(method.getReturnType());
        });
        ClientProvider clients = proxy(ClientProvider.class, (clientsProxy, method, args) -> switch (method.getName()) {
            case "getClientById" -> role.getContainer() instanceof ClientModel client && Objects.equals(args[1], client.getId()) ? client : null;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> {
            if ("getProvider".equals(method.getName()) && args[0].equals(AlternativeLookupProvider.class)) {
                return lookup;
            }
            if ("clients".equals(method.getName())) {
                return clients;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm() {
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm-id";
            case "getName" -> "realm";
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

    private static class TestMapper extends AbstractAttributeToRoleMapper {
        private final boolean applies;

        private TestMapper(boolean applies) {
            this.applies = applies;
        }

        @Override
        protected boolean applies(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
            return applies;
        }

        @Override
        public String[] getCompatibleProviders() {
            return new String[] { "saml" };
        }

        @Override
        public String getDisplayCategory() {
            return "test";
        }

        @Override
        public String getDisplayType() {
            return "test";
        }

        @Override
        public String getHelpText() {
            return "test";
        }

        @Override
        public String getId() {
            return "test";
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return List.of();
        }
    }

    private static class TestUser {
        private final Set<RoleModel> roles = new LinkedHashSet<>();
        private int grants;
        private int deletes;
        private final UserModel model = proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
            case "getRealmRoleMappingsStream" -> roles.stream().filter(role -> role.getType() == RoleModel.Type.REALM);
            case "getClientRoleMappingsStream" -> roles.stream()
                    .filter(role -> role.getType() == RoleModel.Type.CLIENT)
                    .filter(role -> Objects.equals(role.getContainer(), args[0]));
            case "getRoleMappingsStream" -> roles.stream();
            case "grantRole" -> {
                grants++;
                roles.add((RoleModel) args[0]);
                yield null;
            }
            case "deleteRoleMapping" -> {
                deletes++;
                roles.remove(args[0]);
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });

        private TestUser(RoleModel... roles) {
            this.roles.addAll(Set.of(roles));
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
