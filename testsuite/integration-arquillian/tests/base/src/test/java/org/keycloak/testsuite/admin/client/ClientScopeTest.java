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

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.Matchers;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientScopeTest extends AbstractClientTest {

    @Test
    public void testAddDuplicatedClientScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        String scopeId = createClientScope(scopeRep);

        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        Response response = clientScopes().create(scopeRep);
        assertEquals(409, response.getStatus());

        ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
        Assert.assertEquals("Client Scope scope1 already exists", error.getErrorMessage());

        // Cleanup
        removeClientScope(scopeId);
    }


    @Test (expected = NotFoundException.class)
    public void testGetUnknownScope() {
        String unknownId = UUID.randomUUID().toString();
        clientScopes().get(unknownId).toRepresentation();
    }


    private List<String> getClientScopeNames(List<ClientScopeRepresentation> scopes) {
        return scopes.stream().map((ClientScopeRepresentation clientScope) -> {

            return clientScope.getName();

        }).collect(Collectors.toList());
    }

    @Test
    public void testRemoveClientScope() {
        // Create scope1
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        String scope1Id = createClientScope(scopeRep);

        List<ClientScopeRepresentation> clientScopes = clientScopes().findAll();
        assertTrue(getClientScopeNames(clientScopes).contains("scope1"));

        // Create scope2
        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope2");
        String scope2Id = createClientScope(scopeRep);

        clientScopes = clientScopes().findAll();
        assertTrue(getClientScopeNames(clientScopes).contains("scope2"));

        // Remove scope1
        removeClientScope(scope1Id);

        clientScopes = clientScopes().findAll();
        Assert.assertFalse(getClientScopeNames(clientScopes).contains("scope1"));
        assertTrue(getClientScopeNames(clientScopes).contains("scope2"));


        // Remove scope2
        removeClientScope(scope2Id);

        clientScopes = clientScopes().findAll();
        Assert.assertFalse(getClientScopeNames(clientScopes).contains("scope1"));
        Assert.assertFalse(getClientScopeNames(clientScopes).contains("scope2"));
    }


    @Test
    public void testUpdateScopeScope() {
        // Test creating
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        scopeRep.setDescription("scope1-desc");
        scopeRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Map<String, String> attrs = new HashMap<>();
        attrs.put("someAttr", "someAttrValue");
        attrs.put("emptyAttr", "");
        scopeRep.setAttributes(attrs);
        String scope1Id = createClientScope(scopeRep);

        // Assert created attributes
        scopeRep = clientScopes().get(scope1Id).toRepresentation();
        Assert.assertEquals("scope1", scopeRep.getName());
        Assert.assertEquals("scope1-desc", scopeRep.getDescription());
        Assert.assertEquals("someAttrValue", scopeRep.getAttributes().get("someAttr"));
        assertTrue(ObjectUtil.isBlank(scopeRep.getAttributes().get("emptyAttr")));
        Assert.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, scopeRep.getProtocol());


        // Test updating
        scopeRep.setName("scope1-updated");
        scopeRep.setDescription("scope1-desc-updated");
        scopeRep.setProtocol(SamlProtocol.LOGIN_PROTOCOL);

        // Test update attribute to some non-blank value
        scopeRep.getAttributes().put("emptyAttr", "someValue");

        clientScopes().get(scope1Id).update(scopeRep);

        assertAdminEvents.assertEvent(getRealmId(), OperationType.UPDATE, AdminEventPaths.clientScopeResourcePath(scope1Id), scopeRep, ResourceType.CLIENT_SCOPE);

        // Assert updated attributes
        scopeRep = clientScopes().get(scope1Id).toRepresentation();
        Assert.assertEquals("scope1-updated", scopeRep.getName());
        Assert.assertEquals("scope1-desc-updated", scopeRep.getDescription());
        Assert.assertEquals(SamlProtocol.LOGIN_PROTOCOL, scopeRep.getProtocol());
        Assert.assertEquals("someAttrValue", scopeRep.getAttributes().get("someAttr"));
        Assert.assertEquals("someValue", scopeRep.getAttributes().get("emptyAttr"));

        // Remove scope1
        clientScopes().get(scope1Id).remove();
    }


    @Test
    public void testRenameScope() {
        // Create two scopes
        ClientScopeRepresentation scope1Rep = new ClientScopeRepresentation();
        scope1Rep.setName("scope1");
        scope1Rep.setDescription("scope1-desc");
        scope1Rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        createClientScope(scope1Rep);

        ClientScopeRepresentation scope2Rep = new ClientScopeRepresentation();
        scope2Rep.setName("scope2");
        scope2Rep.setDescription("scope2-desc");
        scope2Rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String scope2Id = createClientScope(scope2Rep);

        // Test updating
        scope2Rep.setName("scope1");

        try {
            clientScopes().get(scope2Id).update(scope2Rep);
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Status.CONFLICT));
        }
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

        // create client scope
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("bar-scope");
        String scopeId = createClientScope(scopeRep);

        // update with some scopes
        String accountMgmtId = testRealmResource().clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewAccountRoleRep = testRealmResource().clients().get(accountMgmtId).roles().get(AccountRoles.VIEW_PROFILE).toRepresentation();
        RoleMappingResource scopesResource = clientScopes().get(scopeId).getScopeMappings();

        scopesResource.realmLevel().add(Collections.singletonList(roleRep1));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientScopeRoleMappingsRealmLevelPath(scopeId), Collections.singletonList(roleRep1), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).add(Collections.singletonList(viewAccountRoleRep));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientScopeRoleMappingsClientLevelPath(scopeId, accountMgmtId), Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        // test that scopes are available (also through composite role)
        List<RoleRepresentation> allRealm = scopesResource.realmLevel().listAll();
        List<RoleRepresentation> availableRealm = scopesResource.realmLevel().listAvailable();
        List<RoleRepresentation> effectiveRealm = scopesResource.realmLevel().listEffective();
        List<RoleRepresentation> accountRoles = scopesResource.clientLevel(accountMgmtId).listAll();

        assertRolesPresent(allRealm, "role1");
        assertRolesNotPresent(availableRealm, "role1", "role2");
        assertRolesPresent(effectiveRealm, "role1", "role2");
        assertRolesPresent(accountRoles, AccountRoles.VIEW_PROFILE);
        MappingsRepresentation mappingsRep = clientScopes().get(scopeId).getScopeMappings().getAll();
        assertRolesPresent(mappingsRep.getRealmMappings(), "role1");
        assertRolesPresent(mappingsRep.getClientMappings().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings(), AccountRoles.VIEW_PROFILE);


        // remove scopes
        scopesResource.realmLevel().remove(Collections.singletonList(roleRep1));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientScopeRoleMappingsRealmLevelPath(scopeId), Collections.singletonList(roleRep1), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).remove(Collections.singletonList(viewAccountRoleRep));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientScopeRoleMappingsClientLevelPath(scopeId, accountMgmtId), Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        // assert scopes are removed
        allRealm = scopesResource.realmLevel().listAll();
        availableRealm = scopesResource.realmLevel().listAvailable();
        effectiveRealm = scopesResource.realmLevel().listEffective();
        accountRoles = scopesResource.clientLevel(accountMgmtId).listAll();
        assertRolesNotPresent(allRealm, "role1");
        assertRolesPresent(availableRealm, "role1", "role2");
        assertRolesNotPresent(effectiveRealm, "role1", "role2");
        assertRolesNotPresent(accountRoles, AccountRoles.VIEW_PROFILE);

        // remove scope
        removeClientScope(scopeId);
    }

    private void assertRolesPresent(List<RoleRepresentation> roles, String... expectedRoleNames) {
        String[] expectedList = expectedRoleNames;

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

        // Add client scope
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("bar-scope");
        String scopeId = createClientScope(scopeRep);

        // Add realm role to scopes of clientScope
        clientScopes().get(scopeId).getScopeMappings().realmLevel().add(Collections.singletonList(roleRep));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientScopeRoleMappingsRealmLevelPath(scopeId), Collections.singletonList(roleRep), ResourceType.REALM_SCOPE_MAPPING);

        List<RoleRepresentation> roleReps = clientScopes().get(scopeId).getScopeMappings().realmLevel().listAll();
        Assert.assertEquals(1, roleReps.size());
        Assert.assertEquals("foo-role", roleReps.get(0).getName());

        // Remove realm role
        testRealmResource().roles().deleteRole("foo-role");
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.roleResourcePath("foo-role"), ResourceType.REALM_ROLE);

        // Get scope mappings
        roleReps = clientScopes().get(scopeId).getScopeMappings().realmLevel().listAll();
        Assert.assertEquals(0, roleReps.size());

        // Cleanup
        removeClientScope(scopeId);
    }

    private RoleRepresentation createRealmRole(String roleName) {
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(roleName);
        testRealmResource().roles().create(roleRep);

        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), roleRep, ResourceType.REALM_ROLE);

        return testRealmResource().roles().get(roleName).toRepresentation();
    }

    @Test
    public void testRemoveClientScopeInUse() {
        // Add client scope
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("foo-scope");
        scopeRep.setProtocol("openid-connect");
        String scopeId = createClientScope(scopeRep);

        // Add client with the clientScope
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client");
        clientRep.setName("bar-client");
        clientRep.setProtocol("openid-connect");
        clientRep.setDefaultClientScopes(Collections.singletonList("foo-scope"));
        String clientDbId = createClient(clientRep);

        removeClientScope(scopeId);
        removeClient(clientDbId);
    }


    @Test
    public void testRealmDefaultClientScopes() {
        // Create 2 client scopes
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-def");
        scopeRep.setProtocol("openid-connect");
        String scopeDefId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeDefId);

        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-opt");
        scopeRep.setProtocol("openid-connect");
        String scopeOptId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeOptId);

        // Add scope-def as default and scope-opt as optional client scope
        testRealmResource().addDefaultDefaultClientScope(scopeDefId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.defaultDefaultClientScopePath(scopeDefId), ResourceType.CLIENT_SCOPE);
        testRealmResource().addDefaultOptionalClientScope(scopeOptId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.defaultOptionalClientScopePath(scopeOptId), ResourceType.CLIENT_SCOPE);

        // Ensure defaults and optional scopes are here
        List<String> realmDefaultScopes = getClientScopeNames(testRealmResource().getDefaultDefaultClientScopes());
        List<String> realmOptionalScopes = getClientScopeNames(testRealmResource().getDefaultOptionalClientScopes());
        assertTrue(realmDefaultScopes.contains("scope-def"));
        Assert.assertFalse(realmOptionalScopes .contains("scope-def"));
        Assert.assertFalse(realmDefaultScopes.contains("scope-opt"));
        assertTrue(realmOptionalScopes .contains("scope-opt"));

        // create client. Ensure that it has scope-def and scope-opt scopes assigned
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client");
        clientRep.setProtocol("openid-connect");
        String clientUuid = createClient(clientRep);
        getCleanup().addClientUuid(clientUuid);

        List<String> clientDefaultScopes = getClientScopeNames(testRealmResource().clients().get(clientUuid).getDefaultClientScopes());
        List<String> clientOptionalScopes = getClientScopeNames(testRealmResource().clients().get(clientUuid).getOptionalClientScopes());
        assertTrue(clientDefaultScopes.contains("scope-def"));
        Assert.assertFalse(clientOptionalScopes .contains("scope-def"));
        Assert.assertFalse(clientDefaultScopes.contains("scope-opt"));
        assertTrue(clientOptionalScopes .contains("scope-opt"));

        // Unassign scope-def and scope-opt from realm
        testRealmResource().removeDefaultDefaultClientScope(scopeDefId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.defaultDefaultClientScopePath(scopeDefId), ResourceType.CLIENT_SCOPE);
        testRealmResource().removeDefaultOptionalClientScope(scopeOptId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.defaultOptionalClientScopePath(scopeOptId), ResourceType.CLIENT_SCOPE);

        realmDefaultScopes = getClientScopeNames(testRealmResource().getDefaultDefaultClientScopes());
        realmOptionalScopes = getClientScopeNames(testRealmResource().getDefaultOptionalClientScopes());
        Assert.assertFalse(realmDefaultScopes.contains("scope-def"));
        Assert.assertFalse(realmOptionalScopes .contains("scope-def"));
        Assert.assertFalse(realmDefaultScopes.contains("scope-opt"));
        Assert.assertFalse(realmOptionalScopes .contains("scope-opt"));

        // Create another client. Check it doesn't have scope-def and scope-opt scopes assigned
        clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client-2");
        clientRep.setProtocol("openid-connect");
        clientUuid = createClient(clientRep);
        getCleanup().addClientUuid(clientUuid);

        clientDefaultScopes = getClientScopeNames(testRealmResource().clients().get(clientUuid).getDefaultClientScopes());
        clientOptionalScopes = getClientScopeNames(testRealmResource().clients().get(clientUuid).getOptionalClientScopes());
        Assert.assertFalse(clientDefaultScopes.contains("scope-def"));
        Assert.assertFalse(clientOptionalScopes .contains("scope-def"));
        Assert.assertFalse(clientDefaultScopes.contains("scope-opt"));
        Assert.assertFalse(clientOptionalScopes .contains("scope-opt"));
    }

    // KEYCLOAK-9999
    @Test
    public void defaultOptionalClientScopeCanBeAssignedToClientAsDefaultScope() {

        // Create optional client scope
        ClientScopeRepresentation optionalClientScope = new ClientScopeRepresentation();
        optionalClientScope.setName("optional-client-scope");
        optionalClientScope.setProtocol("openid-connect");
        String optionalClientScopeId = createClientScope(optionalClientScope);
        getCleanup().addClientScopeId(optionalClientScopeId);

        testRealmResource().addDefaultOptionalClientScope(optionalClientScopeId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.defaultOptionalClientScopePath(optionalClientScopeId), ResourceType.CLIENT_SCOPE);

        // Ensure that scope is optional
        List<String> realmOptionalScopes = getClientScopeNames(testRealmResource().getDefaultOptionalClientScopes());
        assertTrue(realmOptionalScopes.contains("optional-client-scope"));

        // Create client
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-client");
        client.setDefaultClientScopes(Collections.singletonList("optional-client-scope"));
        String clientUuid = createClient(client);
        getCleanup().addClientUuid(clientUuid);

        // Ensure that default optional client scope is a default scope of the client
        List<String> clientDefaultScopes = getClientScopeNames(testRealmResource().clients().get(clientUuid).getDefaultClientScopes());
        assertTrue(clientDefaultScopes.contains("optional-client-scope"));

        // Ensure that no optional scopes are assigned to the client, even if there are default optional scopes!
        List<String> clientOptionalScopes = getClientScopeNames(testRealmResource().clients().get(clientUuid).getOptionalClientScopes());
        assertTrue(clientOptionalScopes.isEmpty());

        // Unassign optional client scope from realm for cleanup
        testRealmResource().removeDefaultOptionalClientScope(optionalClientScopeId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.defaultOptionalClientScopePath(optionalClientScopeId), ResourceType.CLIENT_SCOPE);
    }

    // KEYCLOAK-18332
    @Test
    public void scopesRemainAfterClientUpdate() {
        // Create a bunch of scopes
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-def");
        scopeRep.setProtocol("openid-connect");
        String scopeDefId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeDefId);

        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-opt");
        scopeRep.setProtocol("openid-connect");
        String scopeOptId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeOptId);

        // Add scope-def as default and scope-opt as optional client scope
        testRealmResource().addDefaultDefaultClientScope(scopeDefId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.defaultDefaultClientScopePath(scopeDefId), ResourceType.CLIENT_SCOPE);
        testRealmResource().addDefaultOptionalClientScope(scopeOptId);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.defaultOptionalClientScopePath(scopeOptId), ResourceType.CLIENT_SCOPE);

        // Create a client
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client");
        clientRep.setProtocol("openid-connect");
        String clientUuid = createClient(clientRep);
        ClientResource client = testRealmResource().clients().get(clientUuid);
        getCleanup().addClientUuid(clientUuid);
        assertTrue(getClientScopeNames(client.getDefaultClientScopes()).contains("scope-def"));
        assertTrue(getClientScopeNames(client.getOptionalClientScopes()).contains("scope-opt"));

        // Remove the scopes from client
        client.removeDefaultClientScope(scopeDefId);
        client.removeOptionalClientScope(scopeOptId);
        List<String> expectedDefScopes = getClientScopeNames(client.getDefaultClientScopes());
        List<String> expectedOptScopes = getClientScopeNames(client.getOptionalClientScopes());
        assertFalse(expectedDefScopes.contains("scope-def"));
        assertFalse(expectedOptScopes.contains("scope-opt"));

        // Update the client
        clientRep = client.toRepresentation();
        clientRep.setDescription("desc"); // Make a small change
        client.update(clientRep);

        // Assert scopes are intact
        assertEquals(expectedDefScopes, getClientScopeNames(client.getDefaultClientScopes()));
        assertEquals(expectedOptScopes, getClientScopeNames(client.getOptionalClientScopes()));
    }

    // KEYCLOAK-5863
    @Test
    public void testUpdateProtocolMappers() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("testUpdateProtocolMappers");
        scopeRep.setProtocol("openid-connect");


        String scopeId = createClientScope(scopeRep);

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("test");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usermodel-attribute-mapper");

        Map<String, String> m = new HashMap<>();
        m.put("user.attribute", "test");
        m.put("claim.name", "");
        m.put("jsonType.label", "");

        mapper.setConfig(m);

        ProtocolMappersResource protocolMappers = clientScopes().get(scopeId).getProtocolMappers();

        Response response = protocolMappers.createMapper(mapper);
        String mapperId = ApiUtil.getCreatedId(response);

        mapper = protocolMappers.getMapperById(mapperId);

        mapper.getConfig().put("claim.name", "claim");

        protocolMappers.update(mapperId, mapper);

        List<ProtocolMapperRepresentation> mappers = protocolMappers.getMappers();
        assertEquals(1, mappers.size());
        assertEquals(2, mappers.get(0).getConfig().size());
        assertEquals("test", mappers.get(0).getConfig().get("user.attribute"));
        assertEquals("claim", mappers.get(0).getConfig().get("claim.name"));

        clientScopes().get(scopeId).remove();
    }

    @Test
    public void updateClientWithDefaultScopeAssignedAsOptionalAndOpposite() {
        // create client
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client");
        clientRep.setProtocol("openid-connect");
        String clientUuid = createClient(clientRep);
        getCleanup().addClientUuid(clientUuid);

        // Create 2 client scopes
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-def");
        scopeRep.setProtocol("openid-connect");
        String scopeDefId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeDefId);

        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-opt");
        scopeRep.setProtocol("openid-connect");
        String scopeOptId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeOptId);

        // assign "scope-def" as optional client scope to client
        testRealmResource().clients().get(clientUuid).addOptionalClientScope(scopeDefId);

        // assign "scope-opt" as default client scope to client
        testRealmResource().clients().get(clientUuid).addDefaultClientScope(scopeOptId);

        // Add scope-def as default and scope-opt as optional client scope within the realm
        testRealmResource().addDefaultDefaultClientScope(scopeDefId);
        testRealmResource().addDefaultOptionalClientScope(scopeOptId);

        //update client - check it passes (it used to throw ModelDuplicateException before)
        clientRep.setDescription("new_description");
        testRealmResource().clients().get(clientUuid).update(clientRep);
    }

    private ClientScopesResource clientScopes() {
        return testRealmResource().clientScopes();
    }

    private String createClientScope(ClientScopeRepresentation clientScopeRep) {
        Response resp = clientScopes().create(clientScopeRep);
        Assert.assertEquals(201, resp.getStatus());
        resp.close();
        String clientScopeId = ApiUtil.getCreatedId(resp);
        getCleanup().addClientScopeId(clientScopeId);

        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.clientScopeResourcePath(clientScopeId), clientScopeRep, ResourceType.CLIENT_SCOPE);

        return clientScopeId;
    }

    private void removeClientScope(String clientScopeId) {
        clientScopes().get(clientScopeId).remove();
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientScopeResourcePath(clientScopeId), ResourceType.CLIENT_SCOPE);
    }

}
