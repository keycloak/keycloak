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

package org.keycloak.testsuite.admin.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientTemplatesResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;

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
        removeTemplate(templateId);
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
        removeTemplate(template1Id);

        clientTemplates = clientTemplates().findAll();
        Assert.assertEquals(1, clientTemplates.size());
        Assert.assertEquals("template2", clientTemplates.get(0).getName());


        // Remove template2
        removeTemplate(template2Id);

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

        assertAdminEvents.assertEvent(getRealmId(), OperationType.UPDATE, AdminEventPaths.clientTemplateResourcePath(template1Id), templateRep, ResourceType.CLIENT_TEMPLATE);

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
        RoleRepresentation roleRep1 = createRealmRole("role1");

        // Add realm role2
        RoleRepresentation roleRep2 = createRealmRole("role2");

        // Add role2 as composite to role1
        testRealmResource().roles().get("role1").addComposites(Collections.singletonList(roleRep2));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath("role1"), Collections.singletonList(roleRep2), ResourceType.REALM_ROLE);

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
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientTemplateScopeMappingsRealmLevelPath(templateId), Collections.singletonList(roleRep1), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).add(Collections.singletonList(viewAccountRoleRep));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientTemplateScopeMappingsClientLevelPath(templateId, accountMgmtId), Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

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
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientTemplateScopeMappingsRealmLevelPath(templateId), Collections.singletonList(roleRep1), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).remove(Collections.singletonList(viewAccountRoleRep));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientTemplateScopeMappingsClientLevelPath(templateId, accountMgmtId), Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

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
        removeTemplate(templateId);
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
        RoleRepresentation roleRep = createRealmRole("foo-role");

        // Add client template
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("bar-template");
        templateRep.setFullScopeAllowed(false);
        String templateId = createTemplate(templateRep);

        // Add realm role to scopes of clientTemplate
        clientTemplates().get(templateId).getScopeMappings().realmLevel().add(Collections.singletonList(roleRep));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientTemplateScopeMappingsRealmLevelPath(templateId), Collections.singletonList(roleRep), ResourceType.REALM_SCOPE_MAPPING);

        List<RoleRepresentation> roleReps = clientTemplates().get(templateId).getScopeMappings().realmLevel().listAll();
        Assert.assertEquals(1, roleReps.size());
        Assert.assertEquals("foo-role", roleReps.get(0).getName());

        // Remove realm role
        testRealmResource().roles().deleteRole("foo-role");
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.roleResourcePath("foo-role"), ResourceType.REALM_ROLE);

        // Get scope mappings
        roleReps = clientTemplates().get(templateId).getScopeMappings().realmLevel().listAll();
        Assert.assertEquals(0, roleReps.size());

        // Cleanup
        removeTemplate(templateId);
    }

    private RoleRepresentation createRealmRole(String roleName) {
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(roleName);
        testRealmResource().roles().create(roleRep);

        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), roleRep, ResourceType.REALM_ROLE);

        return testRealmResource().roles().get(roleName).toRepresentation();
    }


    // KEYCLOAK-2844
    @Test
    public void testRemoveTemplateInUse() {
        // Add client template
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("foo-template");
        templateRep.setFullScopeAllowed(false);
        String templateId = createTemplate(templateRep);

        // Add client with the clientTemplate
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client");
        clientRep.setName("bar-client");
        clientRep.setRootUrl("foo");
        clientRep.setProtocol("openid-connect");
        clientRep.setClientTemplate("foo-template");
        String clientDbId = createClient(clientRep);

        // Can't remove clientTemplate
        try {
            clientTemplates().get(templateId).remove();
        } catch (BadRequestException bre) {
            ErrorRepresentation error = bre.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("Cannot remove client template, it is currently in use", error.getErrorMessage());
            assertAdminEvents.assertEmpty();
        }

        // Remove client
        removeClient(clientDbId);

        // Can remove clientTemplate now
        removeTemplate(templateId);
    }


    private ClientTemplatesResource clientTemplates() {
        return testRealmResource().clientTemplates();
    }

    private String createTemplate(ClientTemplateRepresentation templateRep) {
        Response resp = clientTemplates().create(templateRep);
        Assert.assertEquals(201, resp.getStatus());
        resp.close();
        String templateId = ApiUtil.getCreatedId(resp);

        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientTemplateResourcePath(templateId), templateRep, ResourceType.CLIENT_TEMPLATE);

        return templateId;
    }

    private void removeTemplate(String templateId) {
        clientTemplates().get(templateId).remove();
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientTemplateResourcePath(templateId), ResourceType.CLIENT_TEMPLATE);
    }

}
