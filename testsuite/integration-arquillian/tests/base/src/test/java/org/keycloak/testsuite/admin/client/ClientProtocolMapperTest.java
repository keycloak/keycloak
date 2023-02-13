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

package org.keycloak.testsuite.admin.client;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientProtocolMapperTest extends AbstractProtocolMapperTest {

    private ClientResource oidcClientRsc;
    private String oidcClientId;
    private ProtocolMappersResource oidcMappersRsc;
    private ClientResource samlClientRsc;
    private String samlClientId;
    private ProtocolMappersResource samlMappersRsc;

    @Before
    public void init() {
        oidcClientId = createOidcClient("oidcMapperClient");
        oidcClientRsc = findClientResource("oidcMapperClient");
        oidcMappersRsc = oidcClientRsc.getProtocolMappers();

        samlClientId = createSamlClient("samlMapperClient");
        samlClientRsc = findClientResource("samlMapperClient");
        samlMappersRsc = samlClientRsc.getProtocolMappers();

        super.initBuiltinMappers();
    }

    @After
    public void tearDown() {
        removeClient(oidcClientId);
        removeClient(samlClientId);
    }

    @Test
    public void test01GetMappersList() {
        // Built-in protocol mappers are empty by default
        assertTrue(oidcMappersRsc.getMappers().isEmpty());
        assertTrue(samlMappersRsc.getMappers().isEmpty());
    }

    @Test
    public void test02CreateOidcMappersFromList() {
        testAddAllBuiltinMappers(oidcMappersRsc, "openid-connect", AdminEventPaths.clientProtocolMappersPath(oidcClientId));
    }

    @Test
    public void test03CreateSamlMappersFromList() {
        testAddAllBuiltinMappers(samlMappersRsc, "saml", AdminEventPaths.clientProtocolMappersPath(samlClientId));
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
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(samlClientId, createdId), rep, ResourceType.PROTOCOL_MAPPER);

        assertEquals(totalMappers + 1, samlMappersRsc.getMappers().size());
        assertEquals(totalSamlMappers + 1, samlMappersRsc.getMappersPerProtocol("saml").size());

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
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(oidcClientId, createdId), rep, ResourceType.PROTOCOL_MAPPER);

        assertEquals(totalMappers + 1, oidcMappersRsc.getMappers().size());
        assertEquals(totalOidcMappers + 1, oidcMappersRsc.getMappersPerProtocol("openid-connect").size());

        ProtocolMapperRepresentation created = oidcMappersRsc.getMapperById(createdId);//findByName(samlMappersRsc, "saml-role-name-mapper");
        assertEqualMappers(rep, created);

    }

    @Test
    public void test06UpdateSamlMapper() {
        ProtocolMapperRepresentation rep = makeSamlMapper("saml-role-name-mapper2");

        Response resp = samlMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(samlClientId, createdId), rep, ResourceType.PROTOCOL_MAPPER);

        rep.getConfig().put("role", "account.manage-account");
        rep.setId(createdId);
        samlMappersRsc.update(createdId, rep);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.UPDATE, AdminEventPaths.clientProtocolMapperPath(samlClientId, createdId), rep, ResourceType.PROTOCOL_MAPPER);

        ProtocolMapperRepresentation updated = samlMappersRsc.getMapperById(createdId);
        assertEqualMappers(rep, updated);
    }

    @Test
    public void test07UpdateOidcMapper() {
        ProtocolMapperRepresentation rep = makeOidcMapper("oidc-hardcoded-role-mapper2");

        Response resp = oidcMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(oidcClientId, createdId), rep, ResourceType.PROTOCOL_MAPPER);

        rep.getConfig().put("role", "myotherrole");
        rep.setId(createdId);
        oidcMappersRsc.update(createdId, rep);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.UPDATE, AdminEventPaths.clientProtocolMapperPath(oidcClientId, createdId), rep, ResourceType.PROTOCOL_MAPPER);

        ProtocolMapperRepresentation updated = oidcMappersRsc.getMapperById(createdId);
        assertEqualMappers(rep, updated);
    }

    @Test
    public void test08DeleteSamlMapper() {
        ProtocolMapperRepresentation rep = makeSamlMapper("saml-role-name-mapper3");

        Response resp = samlMappersRsc.createMapper(rep);
        resp.close();
        String createdId = ApiUtil.getCreatedId(resp);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(samlClientId, createdId), rep, ResourceType.PROTOCOL_MAPPER);

        samlMappersRsc.delete(createdId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientProtocolMapperPath(samlClientId, createdId), ResourceType.PROTOCOL_MAPPER);

        try {
            samlMappersRsc.getMapperById(createdId);
            Assert.fail("Not expected to find mapper");
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
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(oidcClientId, createdId), rep, ResourceType.PROTOCOL_MAPPER);

        oidcMappersRsc.delete(createdId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientProtocolMapperPath(oidcClientId, createdId), ResourceType.PROTOCOL_MAPPER);

        try {
            oidcMappersRsc.getMapperById(createdId);
            Assert.fail("Not expected to find mapper");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

}
