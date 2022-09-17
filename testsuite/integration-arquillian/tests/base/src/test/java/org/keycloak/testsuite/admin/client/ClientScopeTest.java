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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientScopeModel;
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
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.Assert.assertNames;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientScopeTest extends AbstractClientTest {

    @Test
    public void testAddFailureWithInvalidScopeName() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("マルチバイト");

        ErrorRepresentation error;
        try (Response response = clientScopes().create(scopeRep)) {
            Assert.assertEquals(400, response.getStatus());
            error = response.readEntity(ErrorRepresentation.class);
        }

        Assert.assertEquals("Unexpected name \"マルチバイト\" for ClientScope", error.getErrorMessage());
    }

    @Test
    public void testUpdateFailureWithInvalidScopeName() {
        // Creating first
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        String scope1Id = createClientScope(scopeRep);
        // Assert created
        scopeRep = clientScopes().get(scope1Id).toRepresentation();
        Assert.assertEquals("scope1", scopeRep.getName());

        // Test updating
        scopeRep.setName("マルチバイト");
        try {
            clientScopes().get(scope1Id).update(scopeRep);
        } catch (ClientErrorException e) {
            ErrorRepresentation error;
            try (Response response = e.getResponse()) {
                Assert.assertEquals(400, response.getStatus());
                error = response.readEntity(ErrorRepresentation.class);
            }
            Assert.assertEquals("Unexpected name \"マルチバイト\" for ClientScope", error.getErrorMessage());
        }

        removeClientScope(scope1Id);
    }

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
        RoleRepresentation realmCompositeRole = createRealmRole("realm-composite");
        RoleRepresentation realmChildRole = createRealmRole("realm-child");
        testRealmResource().roles().get("realm-composite").addComposites(Collections.singletonList(realmChildRole));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE,
                AdminEventPaths.roleResourceCompositesPath("realm-composite"),
                Collections.singletonList(realmChildRole), ResourceType.REALM_ROLE);

        // create client scope
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("bar-scope");
        String scopeId = createClientScope(scopeRep);

        // update with some scopes
        String accountMgmtId =
                testRealmResource().clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewAccountRoleRep = testRealmResource().clients().get(accountMgmtId).roles()
                .get(AccountRoles.VIEW_PROFILE).toRepresentation();
        RoleMappingResource scopesResource = clientScopes().get(scopeId).getScopeMappings();

        scopesResource.realmLevel().add(Collections.singletonList(realmCompositeRole));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE,
                AdminEventPaths.clientScopeRoleMappingsRealmLevelPath(scopeId),
                Collections.singletonList(realmCompositeRole), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).add(Collections.singletonList(viewAccountRoleRep));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE,
                AdminEventPaths.clientScopeRoleMappingsClientLevelPath(scopeId, accountMgmtId),
                Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        // test that scopes are available (also through composite role)
        List<RoleRepresentation> allRealm = scopesResource.realmLevel().listAll();
        List<RoleRepresentation> availableRealm = scopesResource.realmLevel().listAvailable();
        List<RoleRepresentation> effectiveRealm = scopesResource.realmLevel().listEffective();
        List<RoleRepresentation> accountRoles = scopesResource.clientLevel(accountMgmtId).listAll();

        assertNames(allRealm, "realm-composite");
        assertNames(availableRealm, "realm-child", Constants.OFFLINE_ACCESS_ROLE, Constants.AUTHZ_UMA_AUTHORIZATION,
                Constants.DEFAULT_ROLES_ROLE_PREFIX + "-test");
        assertNames(effectiveRealm, "realm-composite", "realm-child");
        assertNames(accountRoles, AccountRoles.VIEW_PROFILE);
        MappingsRepresentation mappingsRep = clientScopes().get(scopeId).getScopeMappings().getAll();
        assertNames(mappingsRep.getRealmMappings(), "realm-composite");
        assertNames(mappingsRep.getClientMappings().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings(),
                AccountRoles.VIEW_PROFILE);


        // remove scopes
        scopesResource.realmLevel().remove(Collections.singletonList(realmCompositeRole));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE,
                AdminEventPaths.clientScopeRoleMappingsRealmLevelPath(scopeId),
                Collections.singletonList(realmCompositeRole), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).remove(Collections.singletonList(viewAccountRoleRep));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE,
                AdminEventPaths.clientScopeRoleMappingsClientLevelPath(scopeId, accountMgmtId),
                Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        // assert scopes are removed
        allRealm = scopesResource.realmLevel().listAll();
        availableRealm = scopesResource.realmLevel().listAvailable();
        effectiveRealm = scopesResource.realmLevel().listEffective();
        accountRoles = scopesResource.clientLevel(accountMgmtId).listAll();
        assertNames(allRealm);
        assertNames(availableRealm, "realm-composite", "realm-child", Constants.OFFLINE_ACCESS_ROLE,
                Constants.AUTHZ_UMA_AUTHORIZATION,
                Constants.DEFAULT_ROLES_ROLE_PREFIX + "-test");
        assertNames(effectiveRealm);
        assertNames(accountRoles);

        // remove scope
        removeClientScope(scopeId);
    }

    /**
     * Test for KEYCLOAK-10603.
     */
    @Test
    public void rolesCanBeAddedToScopeEvenWhenTheyAreAlreadyIndirectlyAssigned() {
        RealmResource realm = testRealmResource();
        ClientScopeRepresentation clientScopeRep = new ClientScopeRepresentation();
        clientScopeRep.setName("my-scope");
        String clientScopeId = createClientScope(clientScopeRep);

        createRealmRole("realm-composite");
        createRealmRole("realm-child");
        realm.roles().get("realm-composite")
                .addComposites(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));

        Response response = realm.clients().create(ClientBuilder.create().clientId("role-container-client").build());
        String roleContainerClientUuid = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(roleContainerClientUuid);
        response.close();

        RoleRepresentation clientCompositeRole = RoleBuilder.create().name("client-composite").build();
        realm.clients().get(roleContainerClientUuid).roles().create(clientCompositeRole);
        realm.clients().get(roleContainerClientUuid).roles().create(RoleBuilder.create().name("client-child").build());
        realm.clients().get(roleContainerClientUuid).roles().get("client-composite").addComposites(Collections
                .singletonList(
                        realm.clients().get(roleContainerClientUuid).roles().get("client-child").toRepresentation()));

        // Make indirect assignments: assign composite roles
        RoleMappingResource scopesResource = realm.clientScopes().get(clientScopeId).getScopeMappings();
        scopesResource.realmLevel()
                .add(Collections.singletonList(realm.roles().get("realm-composite").toRepresentation()));
        scopesResource.clientLevel(roleContainerClientUuid).add(Collections
                .singletonList(realm.clients().get(roleContainerClientUuid).roles().get("client-composite")
                        .toRepresentation()));

        // check state before making the direct assignments
        assertNames(scopesResource.realmLevel().listAll(), "realm-composite");
        assertNames(scopesResource.realmLevel().listAvailable(), "realm-child", "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-test");
        assertNames(scopesResource.realmLevel().listEffective(), "realm-composite", "realm-child");

        assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAll(), "client-composite");
        assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAvailable(), "client-child");
        assertNames(scopesResource.clientLevel(roleContainerClientUuid).listEffective(), "client-composite",
                "client-child");

        // Make direct assignments for roles which are already indirectly assigned
        scopesResource.realmLevel().add(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));
        scopesResource.clientLevel(roleContainerClientUuid).add(Collections
                .singletonList(
                        realm.clients().get(roleContainerClientUuid).roles().get("client-child").toRepresentation()));

        // List realm roles
        assertNames(scopesResource.realmLevel().listAll(), "realm-composite", "realm-child");
        assertNames(scopesResource.realmLevel().listAvailable(), "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-test");
        assertNames(scopesResource.realmLevel().listEffective(), "realm-composite", "realm-child");

        // List client roles
        assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAll(), "client-composite",
                "client-child");
        assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAvailable());
        assertNames(scopesResource.clientLevel(roleContainerClientUuid).listEffective(), "client-composite",
                "client-child");
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

        RoleRepresentation createdRole = testRealmResource().roles().get(roleName).toRepresentation();

        getCleanup().addRoleId(createdRole.getId());

        return createdRole;
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

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void testCreateValidDynamicScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dynamic-scope-def");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<String, String>(){{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-def:*");
        }});
        String scopeDefId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeDefId);

        // Assert updated attributes
        scopeRep = clientScopes().get(scopeDefId).toRepresentation();
        assertEquals("dynamic-scope-def", scopeRep.getName());
        assertEquals("true", scopeRep.getAttributes().get(ClientScopeModel.IS_DYNAMIC_SCOPE));
        assertEquals("dynamic-scope-def:*", scopeRep.getAttributes().get(ClientScopeModel.DYNAMIC_SCOPE_REGEXP));
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void testCreateNonDynamicScopeWithFeatureEnabled() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("non-dynamic-scope-def");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<String, String>(){{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "false");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "");
        }});
        String scopeDefId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeDefId);

        // Assert updated attributes
        scopeRep = clientScopes().get(scopeDefId).toRepresentation();
        assertEquals("non-dynamic-scope-def", scopeRep.getName());
        assertEquals("false", scopeRep.getAttributes().get(ClientScopeModel.IS_DYNAMIC_SCOPE));
        assertThat(scopeRep.getAttributes().get(ClientScopeModel.DYNAMIC_SCOPE_REGEXP), anyOf(nullValue(), equalTo("")));
    }

    @Test
    @DisableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void testCreateDynamicScopeWithFeatureDisabledAndIsDynamicScopeTrue() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("non-dynamic-scope-def2");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<String, String>(){{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "");
        }});
        handleExpectedCreateFailure(scopeRep, 400, "Unexpected value \"true\" for attribute is.dynamic.scope in ClientScope");
    }

    @Test
    @DisableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void testCreateDynamicScopeWithFeatureDisabledAndNonEmptyDynamicScopeRegexp() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("non-dynamic-scope-def3");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<String, String>(){{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "false");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "not-empty");
        }});
        handleExpectedCreateFailure(scopeRep, 400, "Unexpected value \"not-empty\" for attribute dynamic.scope.regexp in ClientScope");
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void testCreateInvalidRegexpDynamicScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dynamic-scope-def4");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<String, String>(){{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-def:*:*");
        }});
        handleExpectedCreateFailure(scopeRep, 400, "Invalid format for the Dynamic Scope regexp dynamic-scope-def:*:*");
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void updateAssignedDefaultClientScopeToDynamicScope() {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("dyn-scope-client");
        clientRep.setProtocol("openid-connect");
        String clientUuid = createClient(clientRep);
        getCleanup().addClientUuid(clientUuid);

        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dynamic-scope-def");
        scopeRep.setProtocol("openid-connect");
        String scopeDefId = createClientScope(scopeRep);
        getCleanup().addClientScopeId(scopeDefId);

        testRealmResource().clients().get(clientUuid).addDefaultClientScope(scopeDefId);

        scopeRep.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-def:*:*");
        }});

        try {
            clientScopes().get(scopeDefId).update(scopeRep);
            Assert.fail("This update should fail");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Status.BAD_REQUEST));
        }
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void dynamicClientScopeCannotBeAssignedAsDefaultClientScope() {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("dyn-scope-client");
        clientRep.setProtocol("openid-connect");
        String clientUuid = createClient(clientRep);
        getCleanup().addClientUuid(clientUuid);

        ClientScopeRepresentation optionalClientScope = new ClientScopeRepresentation();
        optionalClientScope.setName("optional-dynamic-client-scope");
        optionalClientScope.setProtocol("openid-connect");
        optionalClientScope.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-def:*");
        }});
        String optionalClientScopeId = createClientScope(optionalClientScope);
        getCleanup().addClientScopeId(optionalClientScopeId);

        try {
            ClientResource clientResource = testRealmResource().clients().get(clientUuid);
            clientResource.addDefaultClientScope(optionalClientScopeId);
            Assert.fail("A Dynamic Scope shouldn't not be assigned as a default scope to a client");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Status.BAD_REQUEST));
        }

    }

    private void handleExpectedCreateFailure(ClientScopeRepresentation scopeRep, int expectedErrorCode, String expectedErrorMessage) {
        try(Response resp = clientScopes().create(scopeRep)) {
            Assert.assertEquals(expectedErrorCode, resp.getStatus());
            String respBody = resp.readEntity(String.class);
            Map<String, String> responseJson = null;
            try {
                responseJson = JsonSerialization.readValue(respBody, Map.class);
                Assert.assertEquals(expectedErrorMessage, responseJson.get("errorMessage"));
            } catch (IOException e) {
                fail("Failed to extract the errorMessage from a CreateScope Response");
            }
        }
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
