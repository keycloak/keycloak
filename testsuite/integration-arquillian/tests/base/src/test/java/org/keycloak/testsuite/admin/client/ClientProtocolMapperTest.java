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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ClientProtocolMapperTest extends AbstractClientTest {

    private ClientResource oidcClientRsc;
    private ProtocolMappersResource oidcMappersRsc;
    private ClientResource samlClientRsc;
    private ProtocolMappersResource samlMappersRsc;

    private Map<String, List<ProtocolMapperRepresentation>> builtinMappers = null;

    @Before
    public void init() {
        createOidcClient("oidcMapperClient");
        oidcClientRsc = findClientResource("oidcMapperClient");
        oidcMappersRsc = oidcClientRsc.getProtocolMappers();

        createSamlClient("samlMapperClient");
        samlClientRsc = findClientResource("samlMapperClient");
        samlMappersRsc = samlClientRsc.getProtocolMappers();

        builtinMappers = adminClient.serverInfo().getInfo().getBuiltinProtocolMappers();
    }

    @After
    public void tearDown() {
        oidcClientRsc.remove();
        samlClientRsc.remove();
    }

    private ProtocolMapperRepresentation makeMapper(String protocol, String name, String mapperType, Map<String, String> config) {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        rep.setProtocol(protocol);
        rep.setName(name);
        rep.setProtocolMapper(mapperType);
        rep.setConfig(config);
        rep.setConsentRequired(true);
        rep.setConsentText("Test Consent Text");
        return rep;
    }

    private ProtocolMapperRepresentation makeSamlMapper(String name) {
        Map<String, String> config = new HashMap<>();
        config.put("role", "account.view-profile");
        config.put("new.role.name", "new-role-name");
        return makeMapper("saml", name, "saml-role-name-mapper", config);
    }

    private ProtocolMapperRepresentation makeOidcMapper(String name) {
        Map<String, String> config = new HashMap<>();
        config.put("role", "myrole");
        return makeMapper("openid-connect", name, "oidc-hardcoded-role-mapper", config);
    }

    private void assertEqualMappers(ProtocolMapperRepresentation original, ProtocolMapperRepresentation created) {
        assertNotNull(created);
        assertEquals(original.getName(), created.getName());
        assertEquals(original.getConfig(), created.getConfig());
        assertEquals(original.getConsentText(), created.getConsentText());
        assertEquals(original.isConsentRequired(), created.isConsentRequired());
        assertEquals(original.getProtocol(), created.getProtocol());
        assertEquals(original.getProtocolMapper(), created.getProtocolMapper());
    }

    @Test
    public void testGetMappersList() {
        assertFalse(oidcMappersRsc.getMappers().isEmpty());
        assertFalse(samlMappersRsc.getMappers().isEmpty());
    }

    private boolean containsMapper(List<ProtocolMapperRepresentation> mappers, ProtocolMapperRepresentation mapper) {
        for (ProtocolMapperRepresentation listedMapper : mappers) {
            if (listedMapper.getName().equals(mapper.getName())) return true;
        }

        return false;
    }

    private List<ProtocolMapperRepresentation> mappersToAdd(List<ProtocolMapperRepresentation> oldMappers,
                                                            List<ProtocolMapperRepresentation> builtins) {
        List<ProtocolMapperRepresentation> mappersToAdd = new ArrayList<>();
        for (ProtocolMapperRepresentation builtin : builtins) {
            if (!containsMapper(oldMappers, builtin)) mappersToAdd.add(builtin);
        }

        return mappersToAdd;
    }

    private void testAddAllBuiltinMappers(ProtocolMappersResource resource, String resourceName) {
        List<ProtocolMapperRepresentation> oldMappers = resource.getMappersPerProtocol(resourceName);
        List<ProtocolMapperRepresentation> builtins = builtinMappers.get(resourceName);

        List<ProtocolMapperRepresentation> mappersToAdd = mappersToAdd(oldMappers, builtins);

        // This is used by admin console to add builtin mappers
        resource.createMapper(mappersToAdd);

        List<ProtocolMapperRepresentation> newMappers = resource.getMappersPerProtocol(resourceName);
        assertEquals(oldMappers.size() + mappersToAdd.size(), newMappers.size());

        for (ProtocolMapperRepresentation rep : mappersToAdd) {
            assertTrue(containsMapper(newMappers, rep));
        }
    }

    @Test
    public void testCreateOidcMappersFromList() {
        testAddAllBuiltinMappers(oidcMappersRsc, "openid-connect");
    }

    @Test
    public void testCreateSamlMappersFromList() {
        testAddAllBuiltinMappers(samlMappersRsc, "saml");
    }

    @Test
    public void testCreateSamlProtocolMapper() {

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
        assertEquals(totalMappers + 1, samlMappersRsc.getMappers().size());
        assertEquals(totalSamlMappers + 1, samlMappersRsc.getMappersPerProtocol("saml").size());

        String createdId = ApiUtil.getCreatedId(resp);
        ProtocolMapperRepresentation created = samlMappersRsc.getMapperById(createdId);
        assertEqualMappers(rep, created);
    }

    @Test
    public void testCreateOidcProtocolMapper() {
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
        assertEquals(totalMappers + 1, oidcMappersRsc.getMappers().size());
        assertEquals(totalOidcMappers + 1, oidcMappersRsc.getMappersPerProtocol("openid-connect").size());

        String createdId = ApiUtil.getCreatedId(resp);
        ProtocolMapperRepresentation created = oidcMappersRsc.getMapperById(createdId);//findByName(samlMappersRsc, "saml-role-name-mapper");
        assertEqualMappers(rep, created);
    }

    @Test
    public void testUpdateSamlMapper() {
        ProtocolMapperRepresentation rep = makeSamlMapper("saml-role-name-mapper2");

        Response resp = samlMappersRsc.createMapper(rep);
        resp.close();

        String createdId = ApiUtil.getCreatedId(resp);

        rep.getConfig().put("role", "account.manage-account");
        rep.setId(createdId);
        rep.setConsentRequired(false);
        samlMappersRsc.update(createdId, rep);

        ProtocolMapperRepresentation updated = samlMappersRsc.getMapperById(createdId);
        assertEqualMappers(rep, updated);
    }

    @Test
    public void testUpdateOidcMapper() {
        ProtocolMapperRepresentation rep = makeOidcMapper("oidc-hardcoded-role-mapper2");

        Response resp = oidcMappersRsc.createMapper(rep);
        resp.close();

        String createdId = ApiUtil.getCreatedId(resp);

        rep.getConfig().put("role", "myotherrole");
        rep.setId(createdId);
        rep.setConsentRequired(false);
        oidcMappersRsc.update(createdId, rep);

        ProtocolMapperRepresentation updated = oidcMappersRsc.getMapperById(createdId);
        assertEqualMappers(rep, updated);
    }

    @Test (expected = NotFoundException.class)
    public void testDeleteSamlMapper() {
        ProtocolMapperRepresentation rep = makeSamlMapper("saml-role-name-mapper3");

        Response resp = samlMappersRsc.createMapper(rep);
        resp.close();

        String createdId = ApiUtil.getCreatedId(resp);

        samlMappersRsc.delete(createdId);

        samlMappersRsc.getMapperById(createdId);
    }

    @Test (expected = NotFoundException.class)
    public void testDeleteOidcMapper() {
        ProtocolMapperRepresentation rep = makeOidcMapper("oidc-hardcoded-role-mapper3");

        Response resp = oidcMappersRsc.createMapper(rep);
        resp.close();

        String createdId = ApiUtil.getCreatedId(resp);

        oidcMappersRsc.delete(createdId);

        oidcMappersRsc.getMapperById(createdId);
    }

}
