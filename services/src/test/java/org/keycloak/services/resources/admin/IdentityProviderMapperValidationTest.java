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
package org.keycloak.services.resources.admin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import jakarta.ws.rs.WebApplicationException;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.HardcodedRoleMapper;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.provider.IdentityProviderMapperConfigException;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RealmPermissionEvaluator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class IdentityProviderMapperValidationTest {

    @Test
    public void validatesCreateAndUpdateBeforePersistence() {
        TestContext context = new TestContext();
        IdentityProviderMapperRepresentation representation = context.mapperRepresentation(ValidatingMapper.PROVIDER_ID);

        assertBadRequest(() -> context.resource.addMapper(representation));
        assertBadRequest(() -> context.resource.update("mapper-id", representation));

        assertEquals(2, context.mapper.validations.get());
        assertEquals(0, context.creates.get());
        assertEquals(0, context.updates.get());
    }

    @Test
    public void rejectsUnknownMapperProviderBeforePersistence() {
        TestContext context = new TestContext();

        assertBadRequest(() -> context.resource.addMapper(context.mapperRepresentation("missing-provider")));

        assertEquals(0, context.creates.get());
    }

    @Test
    public void acceptsValidConfigurationBeforeStorageOperations() {
        TestContext context = new TestContext();
        context.persistenceFails = true;
        IdentityProviderMapperRepresentation representation = context.mapperRepresentation(ValidatingMapper.PROVIDER_ID, "true");

        assertBadRequest(() -> context.resource.addMapper(representation));
        assertThrows(ModelException.class, () -> context.resource.update("mapper-id", representation));

        assertEquals(2, context.mapper.validations.get());
        assertEquals(1, context.creates.get());
        assertEquals(1, context.updates.get());
    }

    @Test
    public void preservesMappersWithoutCustomValidation() {
        TestContext context = new TestContext();
        context.persistenceFails = true;

        assertBadRequest(() -> context.resource.addMapper(context.mapperRepresentation(HardcodedRoleMapper.PROVIDER_ID)));

        assertEquals(0, context.mapper.validations.get());
        assertEquals(1, context.creates.get());
    }

    private static void assertBadRequest(Runnable invocation) {
        WebApplicationException exception = assertThrows(WebApplicationException.class, invocation::run);
        assertEquals(400, exception.getResponse().getStatus());
    }

    private static class TestContext {
        private final AtomicInteger creates = new AtomicInteger();
        private final AtomicInteger updates = new AtomicInteger();
        private boolean persistenceFails;
        private final ValidatingMapper mapper = new ValidatingMapper();
        private final RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm-id";
            case "getName" -> "realm";
            case "getEventsListenersStream" -> Stream.empty();
            case "isAdminEventsEnabled" -> false;
            default -> defaultValue(method.getReturnType());
        });
        private final KeycloakSessionFactory sessionFactory = proxy(KeycloakSessionFactory.class, (proxy, method, args) -> switch (method.getName()) {
            case "getProviderFactory" -> {
                if (ValidatingMapper.PROVIDER_ID.equals(args[1])) {
                    yield mapper;
                }
                yield HardcodedRoleMapper.PROVIDER_ID.equals(args[1]) ? new HardcodedRoleMapper() : null;
            }
            case "getProviderFactoriesStream" -> Stream.empty();
            default -> defaultValue(method.getReturnType());
        });
        private final IdentityProviderStorageProvider identityProviders = proxy(IdentityProviderStorageProvider.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapperById" -> new IdentityProviderMapperModel();
            case "createMapper" -> {
                creates.incrementAndGet();
                if (persistenceFails) {
                    throw new ModelException("Create failed");
                }
                yield args[0];
            }
            case "updateMapper" -> {
                updates.incrementAndGet();
                if (persistenceFails) {
                    throw new ModelException("Update failed");
                }
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });
        private final KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> switch (method.getName()) {
            case "getKeycloakSessionFactory" -> sessionFactory;
            case "identityProviders" -> identityProviders;
            default -> defaultValue(method.getReturnType());
        });
        private final IdentityProviderModel identityProvider = identityProvider();
        private final IdentityProviderResource resource = new IdentityProviderResource(adminPermissions(), realm, session, identityProvider,
                adminEventBuilder(realm, session));

        private IdentityProviderMapperRepresentation mapperRepresentation(String providerId) {
            return mapperRepresentation(providerId, "false");
        }

        private IdentityProviderMapperRepresentation mapperRepresentation(String providerId, String valid) {
            IdentityProviderMapperRepresentation representation = new IdentityProviderMapperRepresentation();
            representation.setName("mapper");
            representation.setIdentityProviderAlias(identityProvider.getAlias());
            representation.setIdentityProviderMapper(providerId);
            representation.setConfig(Map.of("valid", valid));
            return representation;
        }
    }

    private static class ValidatingMapper extends AbstractIdentityProviderMapper {
        private static final String PROVIDER_ID = "validating-mapper";
        private final AtomicInteger validations = new AtomicInteger();

        @Override
        public void validateConfig(KeycloakSession session, RealmModel realm, IdentityProviderModel identityProviderModel,
                IdentityProviderMapperModel mapperModel) throws IdentityProviderMapperConfigException {
            validations.incrementAndGet();
            if (!"true".equals(mapperModel.getConfig().get("valid"))) {
                throw new IdentityProviderMapperConfigException("Invalid mapper configuration");
            }
        }

        @Override
        public String[] getCompatibleProviders() {
            return new String[] { IdentityProviderMapper.ANY_PROVIDER };
        }

        @Override
        public String getDisplayCategory() {
            return "Test";
        }

        @Override
        public String getDisplayType() {
            return "Test";
        }

        @Override
        public String getId() {
            return PROVIDER_ID;
        }

        @Override
        public String getHelpText() {
            return "Test mapper";
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return List.of();
        }
    }

    private static IdentityProviderModel identityProvider() {
        IdentityProviderModel identityProvider = new IdentityProviderModel();
        identityProvider.setAlias("idp");
        identityProvider.setProviderId("oidc");
        return identityProvider;
    }

    private static AdminPermissionEvaluator adminPermissions() {
        RealmPermissionEvaluator realmPermissions = proxy(RealmPermissionEvaluator.class,
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        return proxy(AdminPermissionEvaluator.class, (proxy, method, args) -> switch (method.getName()) {
            case "realm" -> realmPermissions;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static AdminEventBuilder adminEventBuilder(RealmModel realm, KeycloakSession session) {
        ClientModel client = proxy(ClientModel.class, (proxy, method, args) -> "getId".equals(method.getName()) ? "client-id" : defaultValue(method.getReturnType()));
        UserModel user = proxy(UserModel.class, (proxy, method, args) -> "getId".equals(method.getName()) ? "user-id" : defaultValue(method.getReturnType()));
        ClientConnection connection = proxy(ClientConnection.class,
                (proxy, method, args) -> method.getName().startsWith("get") ? "localhost" : defaultValue(method.getReturnType()));
        return new AdminEventBuilder(realm, new AdminAuth(realm, null, user, client), session, connection);
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
