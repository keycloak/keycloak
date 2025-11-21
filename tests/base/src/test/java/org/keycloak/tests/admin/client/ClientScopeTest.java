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

package org.keycloak.tests.admin.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
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
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.tests.utils.matchers.Matchers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.keycloak.tests.utils.Assert.assertNames;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class ClientScopeTest extends AbstractClientScopeTest {

    @Test
    public void testAddFailureWithInvalidScopeName() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("マルチバイト");

        ErrorRepresentation error;
        try (Response response = clientScopes().create(scopeRep)) {
            Assertions.assertEquals(400, response.getStatus());
            error = response.readEntity(ErrorRepresentation.class);
        }

        Assertions.assertEquals("Unexpected name \"マルチバイト\" for ClientScope", error.getErrorMessage());
    }

    @Test
    public void testUpdateFailureWithInvalidScopeName() {
        // Creating first
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        scopeRep.setProtocol("openid-connect");
        String scope1Id = createClientScopeWithCleanup(scopeRep);
        // Assert created
        scopeRep = clientScopes().get(scope1Id).toRepresentation();
        Assertions.assertEquals("scope1", scopeRep.getName());

        // Test updating
        scopeRep.setName("マルチバイト");
        try {
            clientScopes().get(scope1Id).update(scopeRep);
        } catch (ClientErrorException e) {
            ErrorRepresentation error;
            try (Response response = e.getResponse()) {
                Assertions.assertEquals(400, response.getStatus());
                error = response.readEntity(ErrorRepresentation.class);
            }
            Assertions.assertEquals("Unexpected name \"マルチバイト\" for ClientScope", error.getErrorMessage());
        }
    }

    @Test
    public void testAddDuplicatedClientScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        scopeRep.setProtocol("openid-connect");
        createClientScopeWithCleanup(scopeRep);

        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        scopeRep.setProtocol("openid-connect");
        Response response = clientScopes().create(scopeRep);
        Assertions.assertEquals(409, response.getStatus());

        ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
        Assertions.assertEquals("Client Scope scope1 already exists", error.getErrorMessage());
        response.close();
    }


    @Test
    public void testGetUnknownScope() {
        String unknownId = UUID.randomUUID().toString();
        Assertions.assertThrows(NotFoundException.class, () -> clientScopes().get(unknownId).toRepresentation());
    }


    private List<String> getClientScopeNames(List<ClientScopeRepresentation> scopes) {
        return scopes.stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList());
    }

    @Test
    public void testRemoveClientScope() {
        // Create scope1
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope1");
        scopeRep.setProtocol("openid-connect");

        String scope1Id = createClientScope(scopeRep);

        List<ClientScopeRepresentation> clientScopes = clientScopes().findAll();
        Assertions.assertTrue(getClientScopeNames(clientScopes).contains("scope1"));

        // Create scope2
        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope2");
        scopeRep.setProtocol("openid-connect");

        String scope2Id = createClientScope(scopeRep);

        clientScopes = clientScopes().findAll();
        Assertions.assertTrue(getClientScopeNames(clientScopes).contains("scope2"));

        // Remove scope1
        removeClientScope(scope1Id);

        clientScopes = clientScopes().findAll();
        Assertions.assertFalse(getClientScopeNames(clientScopes).contains("scope1"));
        Assertions.assertTrue(getClientScopeNames(clientScopes).contains("scope2"));


        // Remove scope2
        removeClientScope(scope2Id);

        clientScopes = clientScopes().findAll();
        Assertions.assertFalse(getClientScopeNames(clientScopes).contains("scope1"));
        Assertions.assertFalse(getClientScopeNames(clientScopes).contains("scope2"));
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
        String scope1Id = createClientScopeWithCleanup(scopeRep);

        // Assert created attributes
        scopeRep = clientScopes().get(scope1Id).toRepresentation();
        Assertions.assertEquals("scope1", scopeRep.getName());
        Assertions.assertEquals("scope1-desc", scopeRep.getDescription());
        Assertions.assertEquals("someAttrValue", scopeRep.getAttributes().get("someAttr"));
        Assertions.assertTrue(ObjectUtil.isBlank(scopeRep.getAttributes().get("emptyAttr")));
        Assertions.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, scopeRep.getProtocol());


        // Test updating
        scopeRep.setName("scope1-updated");
        scopeRep.setDescription("scope1-desc-updated");
        scopeRep.setProtocol(SamlProtocol.LOGIN_PROTOCOL);

        // Test update attribute to some non-blank value
        scopeRep.getAttributes().put("emptyAttr", "someValue");

        clientScopes().get(scope1Id).update(scopeRep);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientScopeResourcePath(scope1Id), scopeRep, ResourceType.CLIENT_SCOPE);

        // Assert updated attributes
        scopeRep = clientScopes().get(scope1Id).toRepresentation();
        Assertions.assertEquals("scope1-updated", scopeRep.getName());
        Assertions.assertEquals("scope1-desc-updated", scopeRep.getDescription());
        Assertions.assertEquals(SamlProtocol.LOGIN_PROTOCOL, scopeRep.getProtocol());
        Assertions.assertEquals("someAttrValue", scopeRep.getAttributes().get("someAttr"));
        Assertions.assertEquals("someValue", scopeRep.getAttributes().get("emptyAttr"));
    }

    @Test
    public void testRenameScope() {
        // Create two scopes
        ClientScopeRepresentation scope1Rep = new ClientScopeRepresentation();
        scope1Rep.setName("scope1");
        scope1Rep.setDescription("scope1-desc");
        scope1Rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String scope1Id = createClientScopeWithCleanup(scope1Rep);

        ClientScopeRepresentation scope2Rep = new ClientScopeRepresentation();
        scope2Rep.setName("scope2");
        scope2Rep.setDescription("scope2-desc");
        scope2Rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String scope2Id = createClientScopeWithCleanup(scope2Rep);

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
        RoleRepresentation realmCompositeRole = createRealmRoleWithCleanup("realm-composite");
        RoleRepresentation realmChildRole = createRealmRoleWithCleanup("realm-child");
        managedRealm.admin().roles().get("realm-composite").addComposites(Collections.singletonList(realmChildRole));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE,
                AdminEventPaths.roleResourceCompositesPath("realm-composite"),
                Collections.singletonList(realmChildRole), ResourceType.REALM_ROLE);

        // create client scope
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("bar-scope");
        scopeRep.setProtocol("openid-connect");
        String scopeId = createClientScopeWithCleanup(scopeRep);

        // update with some scopes
        String accountMgmtId =
                managedRealm.admin().clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewAccountRoleRep = managedRealm.admin().clients().get(accountMgmtId).roles()
                .get(AccountRoles.VIEW_PROFILE).toRepresentation();
        RoleMappingResource scopesResource = clientScopes().get(scopeId).getScopeMappings();

        scopesResource.realmLevel().add(Collections.singletonList(realmCompositeRole));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE,
                AdminEventPaths.clientScopeRoleMappingsRealmLevelPath(scopeId),
                Collections.singletonList(realmCompositeRole), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).add(Collections.singletonList(viewAccountRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE,
                AdminEventPaths.clientScopeRoleMappingsClientLevelPath(scopeId, accountMgmtId),
                Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        // test that scopes are available (also through composite role)
        List<RoleRepresentation> allRealm = scopesResource.realmLevel().listAll();
        List<RoleRepresentation> availableRealm = scopesResource.realmLevel().listAvailable();
        List<RoleRepresentation> effectiveRealm = scopesResource.realmLevel().listEffective();
        List<RoleRepresentation> accountRoles = scopesResource.clientLevel(accountMgmtId).listAll();

        assertNames(allRealm, "realm-composite");
        assertNames(availableRealm, "realm-child", Constants.OFFLINE_ACCESS_ROLE, Constants.AUTHZ_UMA_AUTHORIZATION,
                Constants.DEFAULT_ROLES_ROLE_PREFIX + "-default");
        assertNames(effectiveRealm, "realm-composite", "realm-child");
        assertNames(accountRoles, AccountRoles.VIEW_PROFILE);
        MappingsRepresentation mappingsRep = clientScopes().get(scopeId).getScopeMappings().getAll();
        assertNames(mappingsRep.getRealmMappings(), "realm-composite");
        assertNames(mappingsRep.getClientMappings().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings(),
                AccountRoles.VIEW_PROFILE);


        // remove scopes
        scopesResource.realmLevel().remove(Collections.singletonList(realmCompositeRole));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE,
                AdminEventPaths.clientScopeRoleMappingsRealmLevelPath(scopeId),
                Collections.singletonList(realmCompositeRole), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).remove(Collections.singletonList(viewAccountRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE,
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
                Constants.DEFAULT_ROLES_ROLE_PREFIX + "-default");
        assertNames(effectiveRealm);
        assertNames(accountRoles);
    }

    /**
     * Test for KEYCLOAK-10603.
     */
    @Test
    public void rolesCanBeAddedToScopeEvenWhenTheyAreAlreadyIndirectlyAssigned() {
        RealmResource realm = managedRealm.admin();
        ClientScopeRepresentation clientScopeRep = new ClientScopeRepresentation();
        clientScopeRep.setName("my-scope");
        clientScopeRep.setProtocol("openid-connect");

        String clientScopeId = createClientScopeWithCleanup(clientScopeRep);

        createRealmRoleWithCleanup("realm-composite");
        createRealmRoleWithCleanup("realm-child");
        realm.roles().get("realm-composite")
                .addComposites(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.REALM_ROLE);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("role-container-client");
        createClientWithCleanup(clientRep);
        String roleContainerClientUuid = realm.clients().findByClientId("role-container-client").stream().findFirst().orElseThrow().getId();
        ClientResource roleContainerClient = realm.clients().get(roleContainerClientUuid);

        RoleRepresentation clientCompositeRole = RoleConfigBuilder.create().name("client-composite").build();
        roleContainerClient.roles().create(clientCompositeRole);
        roleContainerClient.roles().create(RoleConfigBuilder.create().name("client-child").build());
        roleContainerClient.roles().get("client-composite").addComposites(Collections
                .singletonList(
                        roleContainerClient.roles().get("client-child").toRepresentation()));

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
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-default");
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
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-default");
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
        scopeRep.setProtocol("openid-connect");

        String scopeId = createClientScopeWithCleanup(scopeRep);

        // Add realm role to scopes of clientScope
        clientScopes().get(scopeId).getScopeMappings().realmLevel().add(Collections.singletonList(roleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientScopeRoleMappingsRealmLevelPath(scopeId), Collections.singletonList(roleRep), ResourceType.REALM_SCOPE_MAPPING);

        List<RoleRepresentation> roleReps = clientScopes().get(scopeId).getScopeMappings().realmLevel().listAll();
        Assertions.assertEquals(1, roleReps.size());
        Assertions.assertEquals("foo-role", roleReps.get(0).getName());

        // Remove realm role
        managedRealm.admin().roles().deleteRole("foo-role");
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleResourcePath("foo-role"), ResourceType.REALM_ROLE);

        // Get scope mappings
        roleReps = clientScopes().get(scopeId).getScopeMappings().realmLevel().listAll();
        Assertions.assertEquals(0, roleReps.size());
    }

    private RoleRepresentation createRealmRole(String roleName) {
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(roleName);
        managedRealm.admin().roles().create(roleRep);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), roleRep, ResourceType.REALM_ROLE);

        return managedRealm.admin().roles().get(roleName).toRepresentation();
    }

    private RoleRepresentation createRealmRoleWithCleanup(String roleName) {
        RoleRepresentation roleRep = createRealmRole(roleName);
        managedRealm.cleanup().add(r -> r.roles().get(roleName).remove());
        return roleRep;
    }

    @Test
    public void testRemoveClientScopeInUse() {
        // Add client scope
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("foo-scope");
        scopeRep.setProtocol("openid-connect");
        String scopeId = createClientScope(scopeRep);

        // Add client with the clientScope
        managedRealm.updateWithCleanup(r -> {
            r.addClient("bar-client")
                    .name("bar-client")
                    .protocol("openid-connect")
                    .defaultClientScopes("foo-scope");
            return r;
        });
        adminEvents.skip();

        removeClientScope(scopeId);
    }


    @Test
    public void testRealmDefaultClientScopes() {
        // Create 2 client scopes
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-def");
        scopeRep.setProtocol("openid-connect");
        String scopeDefId = createClientScopeWithCleanup(scopeRep);

        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-opt");
        scopeRep.setProtocol("openid-connect");
        String scopeOptId = createClientScopeWithCleanup(scopeRep);

        // Add scope-def as default and scope-opt as optional client scope
        managedRealm.admin().addDefaultDefaultClientScope(scopeDefId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.defaultDefaultClientScopePath(scopeDefId), ResourceType.CLIENT_SCOPE);
        managedRealm.admin().addDefaultOptionalClientScope(scopeOptId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.defaultOptionalClientScopePath(scopeOptId), ResourceType.CLIENT_SCOPE);

        // Ensure defaults and optional scopes are here
        List<String> realmDefaultScopes = getClientScopeNames(managedRealm.admin().getDefaultDefaultClientScopes());
        List<String> realmOptionalScopes = getClientScopeNames(managedRealm.admin().getDefaultOptionalClientScopes());
        Assertions.assertTrue(realmDefaultScopes.contains("scope-def"));
        Assertions.assertFalse(realmOptionalScopes.contains("scope-def"));
        Assertions.assertFalse(realmDefaultScopes.contains("scope-opt"));
        Assertions.assertTrue(realmOptionalScopes.contains("scope-opt"));

        // create client. Ensure that it has scope-def and scope-opt scopes assigned
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client");
        clientRep.setProtocol("openid-connect");
        final String clientUuid = createClientWithCleanup(clientRep);

        ClientResource client = managedRealm.admin().clients().get(clientUuid);
        List<String> clientDefaultScopes = getClientScopeNames(client.getDefaultClientScopes());
        List<String> clientOptionalScopes = getClientScopeNames(client.getOptionalClientScopes());
        Assertions.assertTrue(clientDefaultScopes.contains("scope-def"));
        Assertions.assertFalse(clientOptionalScopes.contains("scope-def"));
        Assertions.assertFalse(clientDefaultScopes.contains("scope-opt"));
        Assertions.assertTrue(clientOptionalScopes.contains("scope-opt"));

        // Unassign scope-def and scope-opt from realm
        managedRealm.admin().removeDefaultDefaultClientScope(scopeDefId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.defaultDefaultClientScopePath(scopeDefId), ResourceType.CLIENT_SCOPE);
        managedRealm.admin().removeDefaultOptionalClientScope(scopeOptId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.defaultOptionalClientScopePath(scopeOptId), ResourceType.CLIENT_SCOPE);

        realmDefaultScopes = getClientScopeNames(managedRealm.admin().getDefaultDefaultClientScopes());
        realmOptionalScopes = getClientScopeNames(managedRealm.admin().getDefaultOptionalClientScopes());
        Assertions.assertFalse(realmDefaultScopes.contains("scope-def"));
        Assertions.assertFalse(realmOptionalScopes.contains("scope-def"));
        Assertions.assertFalse(realmDefaultScopes.contains("scope-opt"));
        Assertions.assertFalse(realmOptionalScopes.contains("scope-opt"));

        // Create another client. Check it doesn't have scope-def and scope-opt scopes assigned
        ClientRepresentation clientRep2 = new ClientRepresentation();
        clientRep2.setClientId("bar-client-2");
        clientRep2.setProtocol("openid-connect");
        final String clientUuid2 = createClientWithCleanup(clientRep2);

        ClientResource client2 = managedRealm.admin().clients().get(clientUuid2);
        clientDefaultScopes = getClientScopeNames(client2.getDefaultClientScopes());
        clientOptionalScopes = getClientScopeNames(client2.getOptionalClientScopes());
        Assertions.assertFalse(clientDefaultScopes.contains("scope-def"));
        Assertions.assertFalse(clientOptionalScopes.contains("scope-def"));
        Assertions.assertFalse(clientDefaultScopes.contains("scope-opt"));
        Assertions.assertFalse(clientOptionalScopes.contains("scope-opt"));
    }

    // KEYCLOAK-9999
    @Test
    public void defaultOptionalClientScopeCanBeAssignedToClientAsDefaultScope() {

        // Create optional client scope
        ClientScopeRepresentation optionalClientScope = new ClientScopeRepresentation();
        optionalClientScope.setName("optional-client-scope");
        optionalClientScope.setProtocol("openid-connect");
        String optionalClientScopeId = createClientScopeWithCleanup(optionalClientScope);

        managedRealm.admin().addDefaultOptionalClientScope(optionalClientScopeId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.defaultOptionalClientScopePath(optionalClientScopeId), ResourceType.CLIENT_SCOPE);

        // Ensure that scope is optional
        List<String> realmOptionalScopes = getClientScopeNames(managedRealm.admin().getDefaultOptionalClientScopes());
        Assertions.assertTrue(realmOptionalScopes.contains("optional-client-scope"));

        // Create client
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-client");
        clientRep.setDefaultClientScopes(Collections.singletonList("optional-client-scope"));
        final String clientUuid = createClientWithCleanup(clientRep);

        // Ensure that default optional client scope is a default scope of the client
        ClientResource client = managedRealm.admin().clients().get(clientUuid);
        List<String> clientDefaultScopes = getClientScopeNames(client.getDefaultClientScopes());
        Assertions.assertTrue(clientDefaultScopes.contains("optional-client-scope"));

        // Ensure that no optional scopes are assigned to the client, even if there are default optional scopes!
        List<String> clientOptionalScopes = getClientScopeNames(client.getOptionalClientScopes());
        Assertions.assertTrue(clientOptionalScopes.isEmpty());

        // Unassign optional client scope from realm for cleanup
        managedRealm.admin().removeDefaultOptionalClientScope(optionalClientScopeId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.defaultOptionalClientScopePath(optionalClientScopeId), ResourceType.CLIENT_SCOPE);
    }

    // KEYCLOAK-18332
    @Test
    public void scopesRemainAfterClientUpdate() {
        // Create a bunch of scopes
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-def");
        scopeRep.setProtocol("openid-connect");
        String scopeDefId = createClientScopeWithCleanup(scopeRep);

        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-opt");
        scopeRep.setProtocol("openid-connect");
        String scopeOptId = createClientScopeWithCleanup(scopeRep);

        // Add scope-def as default and scope-opt as optional client scope
        managedRealm.admin().addDefaultDefaultClientScope(scopeDefId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.defaultDefaultClientScopePath(scopeDefId), ResourceType.CLIENT_SCOPE);
        managedRealm.admin().addDefaultOptionalClientScope(scopeOptId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.defaultOptionalClientScopePath(scopeOptId), ResourceType.CLIENT_SCOPE);

        // Create a client
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client");
        clientRep.setProtocol("openid-connect");
        final String clientUuid = createClientWithCleanup(clientRep);

        ClientResource client = managedRealm.admin().clients().get(clientUuid);
        Assertions.assertTrue(getClientScopeNames(client.getDefaultClientScopes()).contains("scope-def"));
        Assertions.assertTrue(getClientScopeNames(client.getOptionalClientScopes()).contains("scope-opt"));

        // Remove the scopes from client
        client.removeDefaultClientScope(scopeDefId);
        client.removeOptionalClientScope(scopeOptId);
        List<String> expectedDefScopes = getClientScopeNames(client.getDefaultClientScopes());
        List<String> expectedOptScopes = getClientScopeNames(client.getOptionalClientScopes());
        Assertions.assertFalse(expectedDefScopes.contains("scope-def"));
        Assertions.assertFalse(expectedOptScopes.contains("scope-opt"));

        // Update the client
        clientRep = client.toRepresentation();
        clientRep.setDescription("desc"); // Make a small change
        client.update(clientRep);

        // Assert scopes are intact
        Assertions.assertEquals(expectedDefScopes, getClientScopeNames(client.getDefaultClientScopes()));
        Assertions.assertEquals(expectedOptScopes, getClientScopeNames(client.getOptionalClientScopes()));
    }

    // KEYCLOAK-5863
    @Test
    public void testUpdateProtocolMappers() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("testUpdateProtocolMappers");
        scopeRep.setProtocol("openid-connect");
        String scopeId = createClientScopeWithCleanup(scopeRep);

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
        Assertions.assertEquals(1, mappers.size());
        Assertions.assertEquals(2, mappers.get(0).getConfig().size());
        Assertions.assertEquals("test", mappers.get(0).getConfig().get("user.attribute"));
        Assertions.assertEquals("claim", mappers.get(0).getConfig().get("claim.name"));
    }

    @Test
    public void updateClientWithDefaultScopeAssignedAsOptionalAndOpposite() {
        // create client
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("bar-client");
        clientRep.setProtocol("openid-connect");
        final String clientUuid = createClientWithCleanup(clientRep);

        // Create 2 client scopes
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-def");
        scopeRep.setProtocol("openid-connect");
        String scopeDefId = createClientScopeWithCleanup(scopeRep);

        scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-opt");
        scopeRep.setProtocol("openid-connect");
        String scopeOptId = createClientScopeWithCleanup(scopeRep);

        ClientResource client = managedRealm.admin().clients().get(clientUuid);
        // assign "scope-def" as optional client scope to client
        client.addOptionalClientScope(scopeDefId);

        // assign "scope-opt" as default client scope to client
        client.addDefaultClientScope(scopeOptId);

        // Add scope-def as default and scope-opt as optional client scope within the realm
        managedRealm.admin().addDefaultDefaultClientScope(scopeDefId);
        managedRealm.admin().addDefaultOptionalClientScope(scopeOptId);

        //update client - check it passes (it used to throw ModelDuplicateException before)
        clientRep.setDescription("new_description");
        Assertions.assertDoesNotThrow(() -> managedRealm.admin().clients().get(clientUuid).update(clientRep));
    }

    @Test
    public void testCreateDynamicScopeWithFeatureDisabledAndIsDynamicScopeTrue() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("non-dynamic-scope-def2");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "");
        }});
        handleExpectedCreateFailure(scopeRep, 400, "Unexpected value \"true\" for attribute is.dynamic.scope in ClientScope");
    }

    @Test
    public void testCreateDynamicScopeWithFeatureDisabledAndNonEmptyDynamicScopeRegexp() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("non-dynamic-scope-def3");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "false");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "not-empty");
        }});
        handleExpectedCreateFailure(scopeRep, 400, "Unexpected value \"not-empty\" for attribute dynamic.scope.regexp in ClientScope");
    }

    @Test
    public void deleteAllClientScopesMustFail() {
        List<ClientScopeRepresentation> clientScopes = clientScopes().findAll();
        for (int i = 0; i < clientScopes.size(); i++) {
            ClientScopeRepresentation clientScope = clientScopes.get(i);
            if (i != clientScopes.size() - 1) {
                removeClientScope(clientScope.getId());
            } else {
                removeClientScopeMustFail(clientScope.getId());
            }
        }
    }

    @Test
    public void createClientScopeWithoutProtocol() {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol(null); // this should cause a BadRequestException
        clientScope.setAttributes(Map.of("test-attribute", "test-value"));

        try (Response response = clientScopes().create(clientScope)) {
            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            String errorMessage = response.readEntity(String.class);
            Assertions.assertTrue(errorMessage.contains("Unexpected protocol"));
        }
    }

    @DisplayName("Create ClientScope with protocol:")
    @ParameterizedTest
    @ValueSource(strings = {"openid-connect", "saml", "oid4vc"})
    public void createClientScopeWithOpenIdProtocol(String protocol) {
        createClientScope(protocol);
    }

    private void createClientScope(String protocol) {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol(protocol);
        clientScope.setAttributes(Map.of("test-attribute", "test-value"));

        String clientScopeId = null;
        try (Response response = clientScopes().create(clientScope)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String location = (String) Optional.ofNullable(response.getHeaders().get(HttpHeaders.LOCATION))
                                               .map(list -> list.get(0))
                                               .orElse(null);
            Assertions.assertNotNull(location);
            clientScopeId = location.substring(location.lastIndexOf("/") + 1);
        } finally {
            Assertions.assertNotNull(clientScopeId);
            // cleanup
            clientScopes().get(clientScopeId).remove();
        }
    }

    private void removeClientScopeMustFail(String clientScopeId) {
        try {
            clientScopes().get(clientScopeId).remove();
        } catch (Exception expected) {

        }
    }

    private void removeClientScope(String clientScopeId) {
        clientScopes().get(clientScopeId).remove();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientScopeResourcePath(clientScopeId), ResourceType.CLIENT_SCOPE);
    }

}
