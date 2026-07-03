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
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.protocol.saml.SamlProtocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SAMLAudienceResolveProtocolMapperTest {

    @Test
    public void transformLoginResponseAddsAudiencesOnlyForClientRoles() throws Exception {
        RealmModel realm = realm();
        ClientModel currentClient = client("current-client-id", "current-client", realm, SamlProtocol.LOGIN_PROTOCOL);
        ClientModel samlAudienceClient = client("saml-client-id", "saml-client", realm, SamlProtocol.LOGIN_PROTOCOL);
        ClientModel oidcClient = client("oidc-client-id", "oidc-client", realm, "openid-connect");
        RoleModel samlClientRole = role("saml-role", RoleModel.Type.CLIENT, samlAudienceClient);
        RoleModel oidcClientRole = role("oidc-role", RoleModel.Type.CLIENT, oidcClient);
        RoleModel realmRole = role("realm-role", RoleModel.Type.REALM, realm);
        RoleModel organizationRole = role("organization-role", RoleModel.Type.ORGANIZATION, container("organization-id"));
        AudienceRestrictionType audience = new AudienceRestrictionType();
        ResponseType response = response(audience);

        new SAMLAudienceResolveProtocolMapper().transformLoginResponse(response, new ProtocolMapperModel(), null, null,
                clientSessionContext(currentClient, samlClientRole, oidcClientRole, realmRole, organizationRole));

        assertEquals(List.of(URI.create("saml-client")), audience.getAudience());
    }

    private static ResponseType response(AudienceRestrictionType audience) throws DatatypeConfigurationException {
        AssertionType assertion = new AssertionType("assertion-id", DatatypeFactory.newInstance().newXMLGregorianCalendar());
        ConditionsType conditions = new ConditionsType();
        conditions.addCondition(audience);
        assertion.setConditions(conditions);
        ResponseType response = new ResponseType("response-id", DatatypeFactory.newInstance().newXMLGregorianCalendar());
        response.addAssertion(new ResponseType.RTChoiceType(assertion));
        return response;
    }

    private static ClientSessionContext clientSessionContext(ClientModel currentClient, RoleModel... roles) {
        AuthenticatedClientSessionModel clientSession = proxy(AuthenticatedClientSessionModel.class, (sessionProxy, method, args) -> switch (method.getName()) {
            case "getClient" -> currentClient;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(ClientSessionContext.class, (contextProxy, method, args) -> switch (method.getName()) {
            case "getClientSession" -> clientSession;
            case "getRolesStream" -> Stream.of(roles);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm() {
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm-id";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientModel client(String id, String clientId, RealmModel realm, String protocol) {
        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getClientId" -> clientId;
            case "getRealm" -> realm;
            case "getProtocol" -> protocol;
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
