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
package org.keycloak.protocol.saml.mappers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.saml.SamlProtocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RoleListMapperOrganizationRolesTest {

    @Test
    public void roleListMapperDoesNotEmitOrganizationRolesAsGlobalRoles() {
        TestContext context = new TestContext();
        AttributeStatementType statement = new AttributeStatementType();
        ProtocolMapperModel mapperModel = RoleListMapper.create("role list", "Role", AttributeStatementHelper.BASIC, null, true);

        new RoleListMapper().mapRoles(statement, mapperModel, context.session, null, context.clientSessionContext);

        List<Object> values = statement.getAttributes().get(0).getAttribute().getAttributeValue();
        assertEquals(List.of("realm-role", "client-role"), values);
    }

    private static class TestContext {
        private final RoleModel realmRole = role("realm-role", false);
        private final RoleModel clientRole = role("client-role", false);
        private final RoleModel organizationRole = role("organization-role", true);
        private final ClientModel client = proxy(ClientModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getProtocol" -> SamlProtocol.LOGIN_PROTOCOL;
            case "getClientId" -> "saml-client";
            default -> defaultValue(method.getReturnType());
        });
        private final AuthenticatedClientSessionModel clientSession = proxy(AuthenticatedClientSessionModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getClient" -> client;
            default -> defaultValue(method.getReturnType());
        });
        private final ClientSessionContext clientSessionContext = proxy(ClientSessionContext.class, (proxy, method, args) -> switch (method.getName()) {
            case "getProtocolMappersStream" -> Stream.<ProtocolMapperModel>empty();
            case "getClientSession" -> clientSession;
            case "getRolesStream" -> Stream.of(realmRole, organizationRole, clientRole);
            default -> defaultValue(method.getReturnType());
        });
        private final KeycloakSessionFactory sessionFactory = proxy(KeycloakSessionFactory.class, (proxy, method, args) -> {
            if ("getProviderFactory".equals(method.getName()) && args[0].equals(ProtocolMapper.class)) {
                return null;
            }
            return defaultValue(method.getReturnType());
        });
        private final KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> switch (method.getName()) {
            case "getKeycloakSessionFactory" -> sessionFactory;
            default -> defaultValue(method.getReturnType());
        });

        private RoleModel role(String name, boolean organizationRole) {
            return proxy(RoleModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getName" -> name;
                case "isOrganizationRole" -> organizationRole;
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
