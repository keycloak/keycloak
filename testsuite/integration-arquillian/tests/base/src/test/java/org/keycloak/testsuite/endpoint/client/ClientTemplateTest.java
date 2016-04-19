/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.endpoint.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientTemplatesResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientTemplateTest extends AbstractClientTest {

    @Test
    public void testAddDuplicatedTemplate() {
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("template1");
        String templateId = createTemplate(templateRep);

        templateRep = new ClientTemplateRepresentation();
        templateRep.setName("template1");
        Response response = clientTemplates().create(templateRep);
        assertEquals(409, response.getStatus());

        ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
        Assert.assertEquals("Client Template template1 already exists", error.getErrorMessage());

        // Cleanup
        clientTemplates().get(templateId).remove();
    }


    @Test (expected = NotFoundException.class)
    public void testGetUnknownTemplate() {
        clientTemplates().get("unknown-id").toRepresentation();
    }


    @Test
    public void testRemoveTemplate() {
        // Create template1
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("template1");
        String template1Id = createTemplate(templateRep);

        List<ClientTemplateRepresentation> clientTemplates = clientTemplates().findAll();
        Assert.assertEquals(1, clientTemplates.size());
        Assert.assertEquals("template1", clientTemplates.get(0).getName());

        // Create template2
        templateRep = new ClientTemplateRepresentation();
        templateRep.setName("template2");
        String template2Id = createTemplate(templateRep);

        clientTemplates = clientTemplates().findAll();
        Assert.assertEquals(2, clientTemplates.size());

        // Remove template1
        clientTemplates().get(template1Id).remove();

        clientTemplates = clientTemplates().findAll();
        Assert.assertEquals(1, clientTemplates.size());
        Assert.assertEquals("template2", clientTemplates.get(0).getName());


        // Remove template2
        clientTemplates().get(template2Id).remove();

        clientTemplates = clientTemplates().findAll();
        Assert.assertEquals(0, clientTemplates.size());
    }


    @Test
    public void testUpdateTemplate() {
        // Test creating
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("template1");
        templateRep.setDescription("template1-desc");
        templateRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        templateRep.setFullScopeAllowed(true);
        String template1Id = createTemplate(templateRep);

        // Assert created attributes
        templateRep = clientTemplates().get(template1Id).toRepresentation();
        Assert.assertEquals("template1", templateRep.getName());
        Assert.assertEquals("template1-desc", templateRep.getDescription());
        Assert.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, templateRep.getProtocol());
        Assert.assertEquals(true, templateRep.isFullScopeAllowed());


        // Test updating
        templateRep.setName("template1-updated");
        templateRep.setDescription("template1-desc-updated");
        templateRep.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        templateRep.setFullScopeAllowed(false);

        clientTemplates().get(template1Id).update(templateRep);

        // Assert updated attributes
        templateRep = clientTemplates().get(template1Id).toRepresentation();
        Assert.assertEquals("template1-updated", templateRep.getName());
        Assert.assertEquals("template1-desc-updated", templateRep.getDescription());
        Assert.assertEquals(SamlProtocol.LOGIN_PROTOCOL, templateRep.getProtocol());
        Assert.assertEquals(false, templateRep.isFullScopeAllowed());

        // Remove template1
        clientTemplates().get(template1Id).remove();
    }


    @Test
    public void testScopes() {
        // Add realm role1
        RoleRepresentation roleRep1 = new RoleRepresentation();
        roleRep1.setName("role1");
        testRealmResource().roles().create(roleRep1);
        roleRep1 = testRealmResource().roles().get("role1").toRepresentation();

        // Add realm role2
        RoleRepresentation roleRep2 = roleRep2 = new RoleRepresentation();
        roleRep2.setName("role2");
        testRealmResource().roles().create(roleRep2);
        roleRep2 = testRealmResource().roles().get("role2").toRepresentation();

        // Add role2 as composite to role1
        testRealmResource().roles().get("role1").addChildren(Collections.singletonList(roleRep2));


        // create client template
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("bar-template");
        templateRep.setFullScopeAllowed(false);
        String templateId = createTemplate(templateRep);

        // update with some scopes
        String accountMgmtId = testRealmResource().clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewAccountRoleRep = testRealmResource().clients().get(accountMgmtId).roles().get(AccountRoles.VIEW_PROFILE).toRepresentation();
        RoleMappingResource scopesResource = clientTemplates().get(templateId).getScopeMappings();

        scopesResource.realmLevel().add(Collections.singletonList(roleRep1));
        scopesResource.clientLevel(accountMgmtId).add(Collections.singletonList(viewAccountRoleRep));

        // test that scopes are available (also through composite role)
        List<RoleRepresentation> allRealm = scopesResource.realmLevel().listAll();
        List<RoleRepresentation> availableRealm = scopesResource.realmLevel().listAvailable();
        List<RoleRepresentation> effectiveRealm = scopesResource.realmLevel().listEffective();
        List<RoleRepresentation> accountRoles = scopesResource.clientLevel(accountMgmtId).listAll();

        assertRolesPresent(allRealm, "role1");
        assertRolesNotPresent(availableRealm, "role1", "role2");
        assertRolesPresent(effectiveRealm, "role1", "role2");
        assertRolesPresent(accountRoles, AccountRoles.VIEW_PROFILE);
        MappingsRepresentation mappingsRep = clientTemplates().get(templateId).getScopeMappings().getAll();
        assertRolesPresent(mappingsRep.getRealmMappings(), "role1");
        assertRolesPresent(mappingsRep.getClientMappings().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings(), AccountRoles.VIEW_PROFILE);


        // remove scopes
        scopesResource.realmLevel().remove(Collections.singletonList(roleRep1));
        scopesResource.clientLevel(accountMgmtId).remove(Collections.singletonList(viewAccountRoleRep));

        // assert scopes are removed
        allRealm = scopesResource.realmLevel().listAll();
        availableRealm = scopesResource.realmLevel().listAvailable();
        effectiveRealm = scopesResource.realmLevel().listEffective();
        accountRoles = scopesResource.clientLevel(accountMgmtId).listAll();
        assertRolesNotPresent(allRealm, "role1");
        assertRolesPresent(availableRealm, "role1", "role2");
        assertRolesNotPresent(effectiveRealm, "role1", "role2");
        assertRolesNotPresent(accountRoles, AccountRoles.VIEW_PROFILE);

        // remove template
        clientTemplates().get(templateId).remove();
    }

    private void assertRolesPresent(List<RoleRepresentation> roles, String... expectedRoleNames) {
        List<String> expectedList = Arrays.asList(expectedRoleNames);

        Set<String> presentRoles = new HashSet<>();
        for (RoleRepresentation roleRep : roles) {
            presentRoles.add(roleRep.getName());
        }

        for (String expected : expectedList) {
            if (!presentRoles.contains(expected)) {
                Assert.fail("Expected role " + expected + " not available");
            }
        }
    }

    private void assertRolesNotPresent(List<RoleRepresentation> roles, String... notExpectedRoleNames) {
        List<String> notExpectedList = Arrays.asList(notExpectedRoleNames);
        for (RoleRepresentation roleRep : roles) {
            if (notExpectedList.contains(roleRep.getName())) {
                Assert.fail("Role " + roleRep.getName() + " wasn't expected to be available");
            }
        }
    }


    // KEYCLOAK-2809
    @Test
    public void testRemoveScopedRole() {
        // Add realm role
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("foo-role");
        testRealmResource().roles().create(roleRep);
        roleRep = testRealmResource().roles().get("foo-role").toRepresentation();

        // Add client template
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("bar-template");
        templateRep.setFullScopeAllowed(false);
        String templateId = createTemplate(templateRep);

        // Add realm role to scopes of clientTemplate
        clientTemplates().get(templateId).getScopeMappings().realmLevel().add(Collections.singletonList(roleRep));

        List<RoleRepresentation> roleReps = clientTemplates().get(templateId).getScopeMappings().realmLevel().listAll();
        Assert.assertEquals(1, roleReps.size());
        Assert.assertEquals("foo-role", roleReps.get(0).getName());

        // Remove realm role
        testRealmResource().roles().deleteRole("foo-role");

        // Get scope mappings
        roleReps = clientTemplates().get(templateId).getScopeMappings().realmLevel().listAll();
        Assert.assertEquals(0, roleReps.size());

        // Cleanup
        clientTemplates().get(templateId).remove();
    }


    private ClientTemplatesResource clientTemplates() {
        return testRealmResource().clientTemplates();
    }

    private String createTemplate(ClientTemplateRepresentation templateRep) {
        Response resp = clientTemplates().create(templateRep);
        Assert.assertEquals(201, resp.getStatus());
        resp.close();
        return ApiUtil.getCreatedId(resp);
    }

}
