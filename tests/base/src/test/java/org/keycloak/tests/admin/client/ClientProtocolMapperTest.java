/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.client;

import java.util.Collections;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AllowedWebOriginsProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@KeycloakIntegrationTest
public class ClientProtocolMapperTest extends AbstractProtocolMapperTest {

    @InjectClient(config = OidcClient.class, ref = "oidcClient")
    ManagedClient oidcClient;

    @InjectClient(config = SamlClient.class, ref = "samlClient")
    ManagedClient samlClient;

    private ProtocolMappersResource oidcMappersRsc;
    private ProtocolMappersResource samlMappersRsc;

    @BeforeEach
    public void setProtocolMapperResources() {
        oidcMappersRsc = oidcClient.admin().getProtocolMappers();
        samlMappersRsc = samlClient.admin().getProtocolMappers();
    }

    @Test
    public void test01GetMappersList() {
        // Built-in protocol mappers are empty by default
        Assertions.assertTrue(oidcMappersRsc.getMappers().isEmpty());
        Assertions.assertTrue(samlMappersRsc.getMappers().isEmpty());
    }

    @Test
    public void test02CreateOidcMappersFromList() {
        testAddAllBuiltinMappers(oidcMappersRsc, "openid-connect", AdminEventPaths.clientProtocolMappersPath(oidcClient.getId()));
    }

    @Test
    public void test03CreateSamlMappersFromList() {
        testAddAllBuiltinMappers(samlMappersRsc, "saml", AdminEventPaths.clientProtocolMappersPath(samlClient.getId()));
    }

    @Test
    public void test04CreateSamlProtocolMapper() {

        //{"protocol":"saml",
        // "config":{"role":"account.view-profile","new.role.name":"new-role-name"},
        // "consentRequired":true,
        // "consentText":"My consent text",
        // "name":"saml-role-name-maper",
        // "protocolMapper":"saml-role-name-mapper"}
        ProtocolMapperRepresentation rep = makeSamlMapper("saml-role-name-mapper");

        int totalMappers = samlMappersRsc.getMappers().size();
        int totalSamlMappers = samlMappersRsc.getMappersPerProtocol("saml").size();
        Response resp = samlMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(samlClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        Assertions.assertEquals(totalMappers + 1, samlMappersRsc.getMappers().size());
        Assertions.assertEquals(totalSamlMappers + 1, samlMappersRsc.getMappersPerProtocol("saml").size());

        ProtocolMapperRepresentation created = samlMappersRsc.getMapperById(createdId);
        assertEqualMappers(rep, created);
    }

    @Test
    public void test05CreateOidcProtocolMapper() {
        //{"protocol":"openid-connect",
        // "config":{"role":"myrole"},
        // "consentRequired":true,
        // "consentText":"My consent text",
        // "name":"oidc-hardcoded-role-mapper",
        // "protocolMapper":"oidc-hardcoded-role-mapper"}
        ProtocolMapperRepresentation rep = makeOidcMapper("oidc-hardcoded-role-mapper");

        int totalMappers = oidcMappersRsc.getMappers().size();
        int totalOidcMappers = oidcMappersRsc.getMappersPerProtocol("openid-connect").size();
        Response resp = oidcMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(oidcClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        Assertions.assertEquals(totalMappers + 1, oidcMappersRsc.getMappers().size());
        Assertions.assertEquals(totalOidcMappers + 1, oidcMappersRsc.getMappersPerProtocol("openid-connect").size());

        ProtocolMapperRepresentation created = oidcMappersRsc.getMapperById(createdId);//findByName(samlMappersRsc, "saml-role-name-mapper");
        assertEqualMappers(rep, created);

    }

    @Test
    public void test06UpdateSamlMapper() {
        ProtocolMapperRepresentation rep = makeSamlMapper("saml-role-name-mapper2");

        Response resp = samlMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(samlClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        rep.getConfig().put("role", "account.manage-account");
        rep.setId(createdId);
        samlMappersRsc.update(createdId, rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientProtocolMapperPath(samlClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        ProtocolMapperRepresentation updated = samlMappersRsc.getMapperById(createdId);
        assertEqualMappers(rep, updated);
    }

    @Test
    public void test07UpdateOidcMapper() {
        ProtocolMapperRepresentation rep = makeOidcMapper("oidc-hardcoded-role-mapper2");

        Response resp = oidcMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(oidcClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        rep.getConfig().put("role", "myotherrole");
        rep.setId(createdId);
        oidcMappersRsc.update(createdId, rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientProtocolMapperPath(oidcClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        ProtocolMapperRepresentation updated = oidcMappersRsc.getMapperById(createdId);
        assertEqualMappers(rep, updated);
    }

    @Test
    public void test08DeleteSamlMapper() {
        ProtocolMapperRepresentation rep = makeSamlMapper("saml-role-name-mapper3");

        Response resp = samlMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(samlClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        samlMappersRsc.delete(createdId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientProtocolMapperPath(samlClient.getId(), createdId), ResourceType.PROTOCOL_MAPPER);

        try {
            samlMappersRsc.getMapperById(createdId);
            Assertions.fail("Not expected to find mapper");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    @Test
    public void test09DeleteOidcMapper() {
        ProtocolMapperRepresentation rep = makeOidcMapper("oidc-hardcoded-role-mapper3");

        Response resp = oidcMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(oidcClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        oidcMappersRsc.delete(createdId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientProtocolMapperPath(oidcClient.getId(), createdId), ResourceType.PROTOCOL_MAPPER);

        try {
            oidcMappersRsc.getMapperById(createdId);
            Assertions.fail("Not expected to find mapper");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    @Test
    public void test10EffectiveMappers() {
        // Web origins mapper
        ProtocolMapperRepresentation rep = makeMapper(OIDCLoginProtocol.LOGIN_PROTOCOL, "web-origins", AllowedWebOriginsProtocolMapper.PROVIDER_ID, Collections.emptyMap());

        Response resp = oidcMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(oidcClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);
        rep = oidcMappersRsc.getMapperById(createdId);

        // Test default values available on the protocol mapper
        Assertions.assertEquals("true", rep.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN));
        Assertions.assertEquals("true", rep.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION));

        // Update mapper to not contain default values
        rep.getConfig().remove(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        rep.getConfig().remove(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION);
        oidcMappersRsc.update(createdId, rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientProtocolMapperPath(oidcClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        // Test configuration will contain "effective values", which are the default values of particular options
        rep = oidcMappersRsc.getMapperById(createdId);
        Assertions.assertEquals("true", rep.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN));
        Assertions.assertEquals("true", rep.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION));

        // Override "includeInIntrospection"
        rep.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "false");
        oidcMappersRsc.update(createdId, rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientProtocolMapperPath(oidcClient.getId(), createdId), rep, ResourceType.PROTOCOL_MAPPER);

        // Get mapper and check that "includeInIntrospection" is using overriden value instead of the default
        rep = oidcMappersRsc.getMapperById(createdId);
        Assertions.assertEquals("true", rep.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN));
        Assertions.assertEquals("false", rep.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION));
    }

    public static class OidcClient implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("oidcMapperClient")
                    .name("oidcMapperClient")
                    .protocol("openid-connect");
        }
    }

    public static class SamlClient implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("samlMapperClient")
                    .name("samlMapperClient")
                    .protocol("saml");
        }
    }
}
