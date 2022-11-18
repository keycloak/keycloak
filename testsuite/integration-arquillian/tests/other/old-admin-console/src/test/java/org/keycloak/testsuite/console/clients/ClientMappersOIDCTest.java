/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.console.clients;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.console.page.clients.mappers.ClientMapper;
import org.keycloak.testsuite.console.page.clients.mappers.ClientMappers;
import org.keycloak.testsuite.console.page.clients.mappers.CreateClientMappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.keycloak.testsuite.auth.page.login.Login.OIDC;
import static org.keycloak.testsuite.console.clients.AbstractClientTest.createClientRep;
import static org.keycloak.testsuite.console.page.clients.mappers.CreateClientMappersForm.*;

/**
 * 
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientMappersOIDCTest extends AbstractClientTest {

    private String id;
    
    @Page
    private ClientMappers clientMappersPage;
    @Page
    private ClientMapper clientMapperPage;

    @Page 
    private CreateClientMappers createClientMappersPage;
    
    @Before
    public void beforeClientMappersTest() {
        ClientRepresentation newClient = createClientRep(TEST_CLIENT_ID, OIDC);
        testRealmResource().clients().create(newClient).close();
        
        id = findClientByClientId(TEST_CLIENT_ID).getId();
        clientMappersPage.setId(id);
        clientMappersPage.navigateTo();
    }
    
    private void setInitialValues(String name) {
        createClientMappersPage.form().setName(name);
    }
    
    @Test
    public void testHardcodedRole() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("hardcoded role");
        createClientMappersPage.form().setMapperType(HARDCODED_ROLE);
        createClientMappersPage.form().selectRole(REALM_ROLE, "offline_access", null);
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "hardcoded role");
        assertNotNull(found);

        assertEquals("oidc-hardcoded-role-mapper", found.getProtocolMapper());
        Map<String, String> config = found.getConfig();
        
        assertEquals("offline_access", config.get("role"));
        
        //edit
        createClientMappersPage.form().selectRole(CLIENT_ROLE, "view-profile", "account");
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        config = findClientMapperByName(id, "hardcoded role").getConfig();
        assertEquals("account.view-profile", config.get("role"));
        
        //delete
        clientMapperPage.setMapperId(found.getId());
        clientMapperPage.delete();
        assertAlertSuccess();
        
        //check
        assertNull(findClientMapperByName(id, "hardcoded role"));
    }
    
    @Test
    public void testHardcodedClaim() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("hardcoded claim");
        createClientMappersPage.form().setMapperType(HARDCODED_CLAIM);
        createClientMappersPage.form().setTokenClaimName("claim name");
        createClientMappersPage.form().setTokenClaimValue("claim value");
        createClientMappersPage.form().setClaimJSONType("long");
        createClientMappersPage.form().setAddToIDToken(true);
        createClientMappersPage.form().setAddToAccessToken(true);
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "hardcoded claim");
        assertNotNull(found);

        assertEquals("oidc-hardcoded-claim-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("true", config.get("id.token.claim"));
        assertEquals("true", config.get("access.token.claim"));
        assertEquals("claim name", config.get("claim.name"));
        assertEquals("claim value", config.get("claim.value"));
        assertEquals("long", config.get("jsonType.label"));
    }
    
    @Test
    public void testUserSessionNote() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user session note");
        createClientMappersPage.form().setMapperType(USER_SESSION_NOTE);
        createClientMappersPage.form().setUserSessionNote("session note");
        createClientMappersPage.form().setTokenClaimName("claim name");
        createClientMappersPage.form().setClaimJSONType("int");
        createClientMappersPage.form().setAddToIDToken(false);
        createClientMappersPage.form().setAddToAccessToken(false);
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "user session note");
        assertNotNull(found);

        assertEquals("oidc-usersessionmodel-note-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("claim name", config.get("claim.name"));
        assertEquals("session note", config.get("user.session.note"));
        assertEquals("int", config.get("jsonType.label"));
    }

    @Test
    public void testRoleName() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("role name");
        createClientMappersPage.form().setMapperType(ROLE_NAME_MAPPER);
        createClientMappersPage.form().setRole("offline_access");
        createClientMappersPage.form().setNewRole("new role");
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "role name");
        assertEquals("oidc-role-name-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("offline_access", config.get("role"));
        assertEquals("new role", config.get("new.role.name"));
    }

    @Test
    public void testUserAddress() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user address");
        createClientMappersPage.form().setMapperType(USERS_FULL_NAME);
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "user address");
        assertEquals("oidc-full-name-mapper", found.getProtocolMapper());
    }
    
    @Test
    public void testUserFullName() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user full name");
        createClientMappersPage.form().setMapperType(USERS_FULL_NAME);
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "user full name");
        assertEquals("oidc-full-name-mapper", found.getProtocolMapper());
    }
    
    @Test
    public void testUserAttribute() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user attribute");
        createClientMappersPage.form().setMapperType(USER_ATTRIBUTE);
        createClientMappersPage.form().setUserAttribute("user attribute");
        createClientMappersPage.form().setMultivalued(true);
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "user attribute");
        assertEquals("oidc-usermodel-attribute-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("true", config.get("multivalued"));
        assertEquals("user attribute", config.get("user.attribute"));
    }

    @Test
    public void testUserProperty() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user property");
        createClientMappersPage.form().setMapperType(USER_PROPERTY);
        createClientMappersPage.form().setProperty("property");
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "user property");
        assertEquals("oidc-usermodel-property-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("property", config.get("user.attribute"));
    }
    
    @Test
    public void testGroupMembership() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("group membership");
        createClientMappersPage.form().setMapperType(GROUP_MEMBERSHIP);
        createClientMappersPage.form().setFullGroupPath(true);
        createClientMappersPage.form().save();
        assertAlertSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName(id, "group membership");
        assertEquals("oidc-group-membership-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("true", config.get("full.path"));
    }
    
    @Test
    public void testEditMapper() {
        //prepare data
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("mapper name");
        //mapper.setConsentRequired(true);
        //mapper.setConsentText("consent text");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usersessionmodel-note-mapper");
        
        Map<String, String> config = new HashMap<>();
        config.put("access.token.claim", "true");
        config.put("id.token.claim", "true");
        config.put("claim.name", "claim name");
        config.put("jsonType.label", "String");
        config.put("user.session.note", "session note");
        
        mapper.setConfig(config);
        
        //insert data
        testRealmResource().clients().get(id).getProtocolMappers().createMapper(mapper).close();
        
        //check form
        clientMapperPage.setId(id);
        String mapperId = findClientMapperByName(id, "mapper name").getId();
        clientMapperPage.setMapperId(mapperId);
        clientMapperPage.navigateTo();
        
        assertEquals("openid-connect", clientMapperPage.form().getProtocol());
        assertEquals(mapperId, clientMapperPage.form().getMapperId());
        assertEquals("mapper name", clientMapperPage.form().getName());
        assertEquals("User Session Note", clientMapperPage.form().getMapperType());
        assertEquals("session note", clientMapperPage.form().getUserSessionNote());
        assertEquals("claim name", clientMapperPage.form().getTokenClaimName());
        assertEquals("String", clientMapperPage.form().getClaimJSONType());
        assertTrue(clientMapperPage.form().isAddToIDToken());
        assertTrue(clientMapperPage.form().isAddToAccessToken());
        
        //edit
        clientMapperPage.form().setAddToAccessToken(false);
        clientMapperPage.form().save();
        assertAlertSuccess();
        
        //check
        assertTrue(clientMapperPage.form().isAddToIDToken());
        assertFalse(clientMapperPage.form().isAddToAccessToken());

        ProtocolMapperRepresentation rep = findClientMapperByName(id, "mapper name");
        assertEquals("false", rep.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN));
        assertEquals("true", rep.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN));

    }
    
    @Test
    public void testAddBuiltin() {
        clientMappersPage.mapperTable().addBuiltin();
        clientMappersPage.mapperTable().checkBuiltinMapper("locale");
        clientMappersPage.mapperTable().clickAddSelectedBuiltinMapper();
        assertAlertSuccess();
        
        assertTrue("Builtin mapper \"locale\" should be present.", isMapperPresent("locale"));
        
        clientMappersPage.mapperTable().deleteMapper("locale");
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        
        assertFalse("Builtin mapper \"locale\" should not be present.", isMapperPresent("locale"));
    }
    
    private boolean isMapperPresent(String name) {
        List<ProtocolMapperRepresentation> mappers = testRealmResource().clients().get(id).getProtocolMappers().getMappers();
        boolean found = false;
        for (ProtocolMapperRepresentation mapper : mappers) {
            if (name.equals(mapper.getName())) {
                found = true;
            }
        }
        return found;
    }
    
    @Test
    public void testCreateMapperInvalidValues() {
        //create some mapper, so we have some existing
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("hardcoded role - existing");
        createClientMappersPage.form().setMapperType(HARDCODED_ROLE);
        createClientMappersPage.form().selectRole(REALM_ROLE, "offline_access", null);
        createClientMappersPage.form().save();
        assertAlertSuccess();

        //empty mapper type
        clientMappersPage.navigateTo();
        clientMappersPage.mapperTable().createMapper();
        createClientMappersPage.form().save();
        assertAlertDanger();
        
        //empty name
        createClientMappersPage.form().setMapperType(HARDCODED_ROLE);
        createClientMappersPage.form().save();
        assertAlertDanger();
        
        createClientMappersPage.form().setName("");
        createClientMappersPage.form().save();
        assertAlertDanger();
        
        createClientMappersPage.form().setName("name");
        createClientMappersPage.form().setName("");
        createClientMappersPage.form().save();
        assertAlertDanger();
        
        //existing name
        createClientMappersPage.form().setName("hardcoded role - existing");
        createClientMappersPage.form().save();
        assertAlertDanger();
    }

    @Test
    public void testUpdateTokenClaimName() {
        clientMappersPage.mapperTable().createMapper();

        createClientMappersPage.form().setName("test");
        createClientMappersPage.form().setMapperType(USER_ATTRIBUTE);
        createClientMappersPage.form().setTokenClaimName("test");
        createClientMappersPage.form().save();
        assertAlertSuccess();

        createClientMappersPage.form().setTokenClaimName("test2");
        createClientMappersPage.form().save();
        assertAlertSuccess();

        ProtocolMapperRepresentation mapper = testRealmResource().clients().get(id).getProtocolMappers().getMappers()
                .stream().filter(m -> m.getName().equals("test")).findFirst().get();

        assertThat(mapper.getConfig().get("claim.name"), is(equalTo("test2")));
    }
}