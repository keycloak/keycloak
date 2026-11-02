/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.authz.fgap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.CLIENTS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES_COMPOSITE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@KeycloakIntegrationTest
public class ClientResourceTypeEvaluationTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectClient(ref = "realmClient")
    ManagedClient realmClient;

    private final String clientsType = AdminPermissionsSchema.CLIENTS.getType();

    @Test
    public void testUnsupportedPolicyTypes() {
        assertSupportForPolicyType("resource", () -> getPermissionsResource(adminPermissionsClient).resource().create(new ResourcePermissionRepresentation()), false);
    }

    @Test
    public void testSupportedPolicyTypes() {
        assertSupportForPolicyType("scope", () -> getPermissionsResource(adminPermissionsClient).scope().create(PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .build()), true);
        assertSupportForPolicyType("user", () -> getPolicies().user().create(new UserPolicyRepresentation()), true);
        assertSupportForPolicyType("client", () -> getPolicies().client().create(new ClientPolicyRepresentation()), true);
        assertSupportForPolicyType("group", () -> getPolicies().group().create(new GroupPolicyRepresentation()), true);
        assertSupportForPolicyType("role", () -> getPolicies().role().create(new RolePolicyRepresentation()), true);
        assertSupportForPolicyType("aggregate", () -> getPolicies().aggregate().create(new AggregatePolicyRepresentation()), true);
        assertSupportForPolicyType("client-scope", () -> getPolicies().clientScope().create(new ClientScopePolicyRepresentation()), true);
        assertSupportForPolicyType("js", () -> getPolicies().js().create(new JSPolicyRepresentation()), true);
        assertSupportForPolicyType("regex", () -> getPolicies().regex().create(new RegexPolicyRepresentation()), true);
        assertSupportForPolicyType("time", () -> getPolicies().time().create(new TimePolicyRepresentation()), true);
    }

    private void assertSupportForPolicyType(String type, Supplier<Response> operation, boolean supported) {
        try (Response response = operation.get()) {
            assertPolicyEndpointResponse(type, supported, response);
        }

        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setType(type);

        try (Response response = getPolicies().create(representation)) {
            assertPolicyEndpointResponse(type, supported, response);
        }
    }

    private void assertPolicyEndpointResponse(String type, boolean supported, Response response) {
        assertThat("Policy type [" + type + "] should be " + (supported ? "supported" : "unsupported"), Status.BAD_REQUEST.equals(Status.fromStatusCode(response.getStatus())), not(supported));
        assertThat("Policy type [" + type + "] should be " + (supported ? "supported" : "unsupported"), response.readEntity(String.class).contains("Policy type not supported by feature"), not(supported));
    }

    @Test
    public void testManageOnlyOneClient() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientResource clientResource = realmAdminClient.realm(realm.getName()).clients().get(myclient.getId());

        // the following operations should fail as the permission wasn't granted yet
        try {
            clientResource.toRepresentation();
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
        try {
            myclient.setName("somethingNew");
            clientResource.update(myclient);
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
        try {
            clientResource.getDefaultClientScopes();
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

        // the caller can view myclient
        clientResource.toRepresentation();

        // the caller can list myclient
        List<ClientRepresentation> allClients = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertThat(allClients, hasSize(1));

        // can update myclient
        myclient.setName("somethingNew");
        clientResource.update(myclient);

        // can't update a different client
        ClientRepresentation realmClientRep = realm.admin().clients().get(realmClient.getId()).toRepresentation();
        realmClientRep.setName("somethingNew");
        try {
            realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).update(realmClientRep);
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testClientScopeAssignmentPermissions() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientResource clientResource = realmAdminClient.realm(realm.getName()).clients().get(myclient.getId());

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());

        ClientResource fullAdminClient = realm.admin().clients().get(myclient.getId());
        for (boolean defaultScope : List.of(true, false)) {
            ClientScopeRepresentation scope = new ClientScopeRepresentation();
            scope.setName(defaultScope ? "assigned-default" : "assigned-optional");
            scope.setDescription("Description of " + scope.getName());
            scope.setProtocol("openid-connect");
            scope.setAttributes(Map.of(
                    ClientScopeModel.IS_PARAMETERIZED_SCOPE, Boolean.toString(!defaultScope),
                    ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "string",
                    "non-display-attribute", "hidden-value"));
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("assigned-scope-mapper");
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
            mapper.setConfig(Map.of("claim.name", "hidden-claim", "claim.value", "hidden-value"));
            scope.setProtocolMappers(List.of(mapper));
            try (Response response = realm.admin().clientScopes().create(scope)) {
                String scopeId = ApiUtil.getCreatedId(response);
                if (defaultScope) {
                    fullAdminClient.addDefaultClientScope(scopeId);
                } else {
                    fullAdminClient.addOptionalClientScope(scopeId);
                }
            }
        }
        List<ClientScopeRepresentation> defaultScopes = fullAdminClient.getDefaultClientScopes();
        List<ClientScopeRepresentation> optionalScopes = fullAdminClient.getOptionalClientScopes();
        ClientScopeRepresentation unassignedScope = new ClientScopeRepresentation();
        unassignedScope.setName("unassigned-scope");
        unassignedScope.setProtocol("openid-connect");
        try (Response response = realm.admin().clientScopes().create(unassignedScope)) {
            unassignedScope.setId(ApiUtil.getCreatedId(response));
        }
        Set<String> expectedAvailableIds = realm.admin().clientScopes().findAll().stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet());
        Set<String> expectedDefaultIds = defaultScopes.stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet());
        Set<String> expectedOptionalIds = optionalScopes.stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet());
        ClientRepresentation expectedClient = fullAdminClient.toRepresentation();
        ClientScopeRepresentation assignedScope = defaultScopes.stream().filter(scope -> "assigned-default".equals(scope.getName())).findFirst().orElseThrow();
        ClientScopeRepresentation optionalScope = optionalScopes.stream().filter(scope -> "assigned-optional".equals(scope.getName())).findFirst().orElseThrow();

        // per-client VIEW: can see scopes currently assigned to this client
        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(VIEW), onlyMyAdminUserPolicy);
        assertThat(clientResource.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet()), is(expectedDefaultIds));
        assertThat(clientResource.getOptionalClientScopes().stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet()), is(expectedOptionalIds));
        ClientRepresentation viewedClient = clientResource.toRepresentation();
        assertThat(Set.copyOf(viewedClient.getDefaultClientScopes()), is(Set.copyOf(expectedClient.getDefaultClientScopes())));
        assertThat(Set.copyOf(viewedClient.getOptionalClientScopes()), is(Set.copyOf(expectedClient.getOptionalClientScopes())));

        // Assigned scope metadata is available without access to the realm-wide catalog.
        assertThat(realmAdminClient.realm(realm.getName()).clientScopes().findAll(), empty());
        for (ClientScopeRepresentation scope : List.of(
                clientResource.getDefaultClientScopes().stream().filter(s -> assignedScope.getId().equals(s.getId())).findFirst().orElseThrow(),
                clientResource.getOptionalClientScopes().stream().filter(s -> optionalScope.getId().equals(s.getId())).findFirst().orElseThrow())) {
            assertThat(scope.getDescription(), is("Description of " + scope.getName()));
            assertThat(scope.getAttributes(), is(Map.of(ClientScopeModel.IS_PARAMETERIZED_SCOPE, Boolean.toString("assigned-optional".equals(scope.getName())))));
            assertThat(scope.getProtocolMappers(), nullValue());
            assertThat(realm.admin().clientScopes().get(scope.getId()).toRepresentation().getProtocolMappers(), not(empty()));
        }

        // VIEW alone must not permit changes to either kind of assignment.
        Assertions.assertThrows(ForbiddenException.class, clientResource::getAvailableClientScopes);
        Assertions.assertThrows(ForbiddenException.class, () -> clientResource.removeDefaultClientScope(assignedScope.getId()));
        Assertions.assertThrows(ForbiddenException.class, () -> clientResource.removeOptionalClientScope(optionalScope.getId()));
        Assertions.assertThrows(ForbiddenException.class, () -> clientResource.addDefaultClientScope(assignedScope.getId()));
        Assertions.assertThrows(ForbiddenException.class, () -> clientResource.addOptionalClientScope(optionalScope.getId()));

        // per-client MANAGE: can remove an already-assigned scope (scope is currently on this client)
        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MANAGE), onlyMyAdminUserPolicy);
        List<ClientScopeRepresentation> availableScopes = clientResource.getAvailableClientScopes();
        assertThat(availableScopes.stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet()), is(expectedAvailableIds));
        for (ClientScopeRepresentation scope : availableScopes) {
            assertThat(scope.getProtocolMappers(), nullValue());
            assertThat(scope.getAttributes().keySet(), is(Set.of(ClientScopeModel.IS_PARAMETERIZED_SCOPE)));
            assertThat(scope.getProtocol(), notNullValue());
        }
        clientResource.removeDefaultClientScope(assignedScope.getId());
        clientResource.removeOptionalClientScope(optionalScope.getId());
        assertThat(clientResource.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getId).toList(), not(hasItem(assignedScope.getId())));
        assertThat(clientResource.getOptionalClientScopes().stream().map(ClientScopeRepresentation::getId).toList(), not(hasItem(optionalScope.getId())));

        // Per-client MANAGE permits assigning any scope, including one never assigned to this client.
        clientResource.addDefaultClientScope(unassignedScope.getId());
        assertThat(clientResource.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getId).toList(), hasItem(unassignedScope.getId()));
        clientResource.removeDefaultClientScope(unassignedScope.getId());
        clientResource.addOptionalClientScope(optionalScope.getId());
        assertThat(clientResource.getOptionalClientScopes().stream().map(ClientScopeRepresentation::getId).toList(), hasItem(optionalScope.getId()));

        // Changing an ordinary scope from default to optional and back requires only per-client MANAGE.
        clientResource.addOptionalClientScope(assignedScope.getId());
        assertThat(clientResource.getOptionalClientScopes().stream().map(ClientScopeRepresentation::getId).toList(), hasItem(assignedScope.getId()));
        clientResource.removeOptionalClientScope(assignedScope.getId());
        clientResource.addDefaultClientScope(assignedScope.getId());
        assertThat(clientResource.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getId).toList(), hasItem(assignedScope.getId()));

        // Scope definitions remain protected even though their assignment to this client is allowed.
        assertThat(realmAdminClient.realm(realm.getName()).clientScopes().findAll(), empty());
        Assertions.assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).clientScopes().get(unassignedScope.getId()).toRepresentation());
        Assertions.assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).clientScopes().get(unassignedScope.getId()).update(unassignedScope));

        ClientResource otherClient = realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId());
        Assertions.assertThrows(ForbiddenException.class, otherClient::getDefaultClientScopes);
        Assertions.assertThrows(ForbiddenException.class, otherClient::getAvailableClientScopes);
        Assertions.assertThrows(ForbiddenException.class, () -> otherClient.addDefaultClientScope(assignedScope.getId()));
        Assertions.assertThrows(ForbiddenException.class, () -> otherClient.removeDefaultClientScope(assignedScope.getId()));

        // Removing an absent assignment remains an idempotent operation for a delegated manager.
        clientResource.removeDefaultClientScope(unassignedScope.getId());

        assertThat(clientResource.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet()), is(expectedDefaultIds));
        assertThat(clientResource.getOptionalClientScopes().stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet()), is(expectedOptionalIds));

        // the realm-wide client scopes catalog requires type-level view — that is not affected by this change
        createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));
        assertThat(realmAdminClient.realm(realm.getName()).clientScopes().findAll(), not(empty()));
    }

    @Test
    public void testManageAllClients() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setClientId("newClient");
        newClient.setProtocol("openid-connect");

        // the following operations should fail as the permission wasn't granted yet
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertThat(found, empty());

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        // can create a new client
        realmAdminClient.realm(realm.getName()).clients().create(newClient).close();
        found = realmAdminClient.realm(realm.getName()).clients().findByClientId("newClient");
        assertThat(found, hasSize(1));

        // can delete
        realmAdminClient.realm(realm.getName()).clients().get(found.get(0).getId()).remove();
        found = realmAdminClient.realm(realm.getName()).clients().findByClientId("newClient");
        assertThat(found, empty());

        // can list & view all clients
        found = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertThat(found, not(empty()));
    }

    @Test
    public void testManageAllClientsDoesNotGrantManageOnAdminPermissionsClient() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        String adminPermissionsClientId = adminPermissionsClient.toRepresentation().getId();

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        ClientResource clientResource = realmAdminClient.realm(realm.getName()).clients().get(adminPermissionsClientId);

        // the admin-permissions client holds the whole FGAP model, so a Clients:manage permission must not grant manage on it
        ClientRepresentation rep = clientResource.toRepresentation();
        assertThat(rep.getAccess().get("manage"), is(false));
        assertThat(rep.getAccess().get("configure"), is(false));

        // enforcement has to agree with the access report above
        rep.setName("somethingNew");
        Assertions.assertThrows(ForbiddenException.class, () -> clientResource.update(rep));

        // renaming it must not be possible either, otherwise it could be deleted as a regular client afterwards
        ClientRepresentation renamed = adminPermissionsClient.toRepresentation();
        renamed.setClientId("renamed-admin-permissions");
        Assertions.assertThrows(ForbiddenException.class, () -> clientResource.update(renamed));

        // nothing was persisted
        ClientRepresentation current = adminPermissionsClient.toRepresentation();
        assertThat(current.getClientId(), is(Constants.ADMIN_PERMISSIONS_CLIENT_ID));
        assertThat(current.getName(), nullValue());
    }

    @Test
    public void testClientScopeEvaluation() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setClientId("newClient");
        newClient.setProtocol("openid-connect");

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        realmAdminClient.realm(realm.getName()).clients().create(newClient).close();
        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findByClientId("newClient");
        assertThat(found, hasSize(1));

        UserRepresentation user = UserBuilder.create()
                .username(KeycloakModelUtils.generateId())
                .build();
        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        ClientResource clientApi = realmAdminClient.realm(realm.getName()).clients().get(found.get(0).getId());

        try {
            clientApi.clientScopesEvaluate().generateAccessToken("openid", user.getId(), null);
            fail("no permissions to view the user.");
        } catch (ForbiddenException e) {
            assertEquals("You have no access to this user", e.getResponse().readEntity(OAuth2ErrorRepresentation.class).getError());
        }

        createPermission(adminPermissionsClient, user.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), onlyMyAdminUserPolicy);
        clientApi.clientScopesEvaluate().generateAccessToken("openid", user.getId(), null);
    }

    @Test
    public void testPolicyEvaluation() {
        ClientRepresentation myResourceServer = realm.admin().clients().findByClientId("myresourceserver").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myResourceServer.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

        UserRepresentation user = UserBuilder.create()
                .username(KeycloakModelUtils.generateId())
                .build();
        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        ClientResource clientApi = realmAdminClient.realm(realm.getName()).clients().get(myResourceServer.getId());

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());

        try {
            clientApi.authorization().policies().evaluate(request);
            fail("no permissions to view the user.");
        } catch (ForbiddenException e) {
            assertEquals("You have no access to this user", e.getResponse().readEntity(OAuth2ErrorRepresentation.class).getError());
        }

        createPermission(adminPermissionsClient, user.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), onlyMyAdminUserPolicy);
        PolicyEvaluationResponse response = clientApi.authorization().policies().evaluate(request);
        assertThat(response, notNullValue());
    }

    @Test
    public void testPolicyEvaluationRequiresViewOnTargetClient() {
        ClientRepresentation myResourceServer = realm.admin().clients().findByClientId("myresourceserver").get(0);
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myResourceServer.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

        UserRepresentation user = UserBuilder.create()
                .username(KeycloakModelUtils.generateId())
                .build();
        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        createPermission(adminPermissionsClient, user.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), onlyMyAdminUserPolicy);

        ClientResource clientApi = realmAdminClient.realm(realm.getName()).clients().get(myResourceServer.getId());

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(myclient.getId());

        try {
            clientApi.authorization().policies().evaluate(request);
            fail("no permissions to view the client.");
        } catch (ForbiddenException e) {
            assertEquals("You have no access to this client", e.getResponse().readEntity(OAuth2ErrorRepresentation.class).getError());
        }

        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(VIEW), onlyMyAdminUserPolicy);
        PolicyEvaluationResponse response = clientApi.authorization().policies().evaluate(request);
        assertThat(response, notNullValue());
    }

    @Test
    public void testViewAllClients() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        // the following operations should fail as the permission wasn't granted yet
        try {
            realmAdminClient.realm(realm.getName()).clients().get(myclient.getId()).getProtocolMappers().getMappers();
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findAll(true);
        assertThat(found, empty());


        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW));

        // can list and view all clients
        found = realmAdminClient.realm(realm.getName()).clients().findAll(true);
        assertThat(found, not(empty()));

        // can't create a protocol mapper
        ProtocolMapperRepresentation protocolMapperRep = new ProtocolMapperRepresentation();
        protocolMapperRep.setName("my-protocol-mapper");
        protocolMapperRep.setProtocol("openid-connect");
        protocolMapperRep.setProtocolMapper("oidc-hardcoded-claim-mapper");
        try (Response response = realmAdminClient.realm(realm.getName()).clients().get(myclient.getId()).getProtocolMappers().createMapper(protocolMapperRep)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // can get protocol mappers
        assertThat(realmAdminClient.realm(realm.getName()).clients().get(myclient.getId()).getProtocolMappers().getMappers(), empty());

        // can't create a new client
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(null)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testCreateClientsRequireManageScope() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation newClient = new ClientRepresentation();

        newClient.setClientId(KeycloakModelUtils.generateId());

        // can't create a new client
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findAll(true);
        assertThat(found, empty());


        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        ScopePermissionRepresentation allPermission = createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, CLIENTS.getScopes().stream().filter(Predicate.not(MANAGE::equals)).collect(Collectors.toSet()));

        // can't create a new client
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // grants manage access
        allPermission = getScopePermissionsResource(adminPermissionsClient).findByName(allPermission.getName());
        allPermission.setScopes(Set.of(VIEW, MANAGE));
        getScopePermissionsResource(adminPermissionsClient).findById(allPermission.getId()).update(allPermission);

        // create clients is permission is granted to all clients
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        found = realmAdminClient.realm(realm.getName()).clients().findByClientId(newClient.getClientId());
        assertThat(found, not(empty()));
    }

    @Test
    public void testMapRolesOnlyOneClient() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);

        // create a role
        RoleRepresentation role = new RoleRepresentation();
        role.setName("myclient-role");
        role.setClientRole(true);
        realm.admin().clients().get(myclient.getId()).roles().create(role);
        role = realm.admin().clients().get(myclient.getId()).roles().get("myclient-role").toRepresentation();

        // the following operations should fail as the permission wasn't granted yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().clientLevel(myclient.getId()).add(List.of(role));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myadmin.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(MAP_ROLES), onlyMyAdminUserPolicy);
        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MAP_ROLES), onlyMyAdminUserPolicy);

        // now those should pass
        realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().clientLevel(myclient.getId()).add(List.of(role));
    }

    @Test
    public void testMapCompositesToAnotherClientRole() {
        // to add a client role ('myclient') 'roleA' as a composite role of 'roleB' (client role of 'realmClient' client)
        // it is required to have permission to manage the `realmClient` and to map-roles-composite of the `myclient`
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);

        // create two roles, each for seprate client
        RoleRepresentation roleA = new RoleRepresentation();
        roleA.setName("roleA");
        roleA.setClientRole(true);
        realm.admin().clients().get(myclient.getId()).roles().create(roleA);
        roleA = realm.admin().clients().get(myclient.getId()).roles().get("roleA").toRepresentation();

        RoleRepresentation roleB = new RoleRepresentation();
        roleB.setName("roleB");
        roleB.setClientRole(true);
        realm.admin().clients().get(realmClient.getId()).roles().create(roleB);

        // the following operations should fail as the permission wasn't granted yet
        try {
            realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).roles().get("roleB").addComposites(List.of(roleA));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());

        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MAP_ROLES_COMPOSITE), onlyMyAdminUserPolicy);

        // the following operations should fail as the permission to manage the realmClient is missing
        try {
            realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).roles().get("roleB").addComposites(List.of(roleA));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        createPermission(adminPermissionsClient, realmClient.getId(), clientsType, Set.of(MANAGE), onlyMyAdminUserPolicy);

        // now those should pass
        realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).roles().get("roleB").addComposites(List.of(roleA));
    }

    @Test
    public void testEvaluateAllResourcePermissionsForSpecificResourcePermission() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation adminUser = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin", adminUser.getId());
        ScopePermissionRepresentation allResourcesPermission = createAllPermission(adminPermissionsClient, clientsType, allowPolicy, Set.of(MANAGE, MAP_ROLES));
        // all resource permissions grants manage scope
        ClientsResource clients = realmAdminClient.realm(realm.getName()).clients();
        clients.get(myclient.getId()).update(myclient);

        ScopePermissionRepresentation resourcePermission = createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MANAGE), allowPolicy);
        // both all and specific resource permission grants manage scope
        clients.get(myclient.getId()).update(myclient);

        allResourcesPermission = getScopePermissionsResource(adminPermissionsClient).findByName(allResourcesPermission.getName());
        allResourcesPermission.setScopes(Set.of(MAP_ROLES));
        getScopePermissionsResource(adminPermissionsClient).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission does not have the manage scope but the scope is granted by the resource permission
        clients.get(myclient.getId()).update(myclient);

        resourcePermission = getScopePermissionsResource(adminPermissionsClient).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(MAP_ROLES));
        getScopePermissionsResource(adminPermissionsClient).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // neither the all and specific resource permission grants access to the manage scope
            clients.get(myclient.getId()).update(myclient);
            Assertions.fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        allResourcesPermission.setScopes(Set.of(MANAGE));
        getScopePermissionsResource(adminPermissionsClient).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission grants access again to manage
        clients.get(myclient.getId()).update(myclient);

        UserPolicyRepresentation notAllowPolicy = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient, "Not My Admin", adminUser.getId());
        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MANAGE), notAllowPolicy);
        try {
            // a specific resource permission that explicitly negates access to the manage scope denies access to the scope
            clients.get(myclient.getId()).update(myclient);
            Assertions.fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        resourcePermission = getScopePermissionsResource(adminPermissionsClient).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(MAP_ROLES, MANAGE));
        getScopePermissionsResource(adminPermissionsClient).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // the specific resource permission that explicitly negates access to the manage scope denies access to the scope
            // even though there is another resource permission that grants access to the scope - conflict resolution denies by default
            clients.get(myclient.getId()).update(myclient);
            Assertions.fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}
    }

    @Test
    public void testManageClientWithAuthorizationSettings() {
        ClientRepresentation myResourceServer = realm.admin().clients().findByClientId("myresourceserver").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientResource clientResource = realmAdminClient.realm(realm.getName()).clients().get(myResourceServer.getId());
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myResourceServer.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

        // can update myResourceServer because manage also implies managing authorization service settings
        myResourceServer.setName("somethingNew");
        clientResource.update(myResourceServer);
    }

    @Test
    public void testRoleGroupMembersFilteredByGroupPermissions() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        String myclientId = myclient.getId();

        // create a client role
        RoleRepresentation role = new RoleRepresentation();
        role.setName("visible-role");
        role.setClientRole(true);
        realm.admin().clients().get(myclientId).roles().create(role);
        role = realm.admin().clients().get(myclientId).roles().get("visible-role").toRepresentation();

        // create two groups and assign the role to both
        GroupRepresentation visibleGroup = createGroup("visible-group");
        GroupRepresentation hiddenGroup = createGroup("hidden-group");

        realm.admin().groups().group(visibleGroup.getId()).roles().clientLevel(myclientId).add(List.of(role));
        realm.admin().groups().group(hiddenGroup.getId()).roles().clientLevel(myclientId).add(List.of(role));

        // create a realm role and assign it to both groups
        RoleRepresentation realmRole = new RoleRepresentation();
        realmRole.setName("visible-realm-role");
        realm.admin().roles().create(realmRole);
        realmRole = realm.admin().roles().get("visible-realm-role").toRepresentation();

        realm.admin().groups().group(visibleGroup.getId()).roles().realmLevel().add(List.of(realmRole));
        realm.admin().groups().group(hiddenGroup.getId()).roles().realmLevel().add(List.of(realmRole));

        // grant limited-admin view permission on the client only
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myclientId, clientsType, Set.of(VIEW), policy);

        // grant view permission on visible-group only
        createGroupPermission(visibleGroup, Set.of(AdminPermissionsSchema.VIEW), policy);

        // grant view-realm role so the limited admin can access realm-level roles endpoint
        String realmMgmtClientId = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewRealmRole = realm.admin().clients().get(realmMgmtClientId).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmMgmtClientId).add(List.of(viewRealmRole));
        realm.cleanup().add(r -> r.users().get(myadmin.getId()).roles().clientLevel(realmMgmtClientId).remove(List.of(viewRealmRole)));

        // limited admin can view the client and its role
        realmAdminClient.realm(realm.getName()).clients().get(myclientId).toRepresentation();
        realmAdminClient.realm(realm.getName()).clients().get(myclientId).roles().get("visible-role").toRepresentation();

        // limited admin cannot directly access hidden-group
        try {
            realmAdminClient.realm(realm.getName()).groups().group(hiddenGroup.getId()).toRepresentation();
            fail("Should not be able to access hidden group directly");
        } catch (ForbiddenException expected) {
        }

        // client role group members should only return the visible group, not the hidden one
        Set<GroupRepresentation> roleGroups = realmAdminClient.realm(realm.getName()).clients().get(myclientId)
                .roles().get("visible-role").getRoleGroupMembers();
        assertThat(roleGroups, hasSize(1));
        assertEquals("visible-group", roleGroups.iterator().next().getName());

        // realm role group members should also be filtered by group permissions
        Set<GroupRepresentation> realmRoleGroups = realmAdminClient.realm(realm.getName()).roles()
                .get("visible-realm-role").getRoleGroupMembers();
        assertThat(realmRoleGroups, hasSize(1));
        assertEquals("visible-group", realmRoleGroups.iterator().next().getName());
    }

    @Test
    public void testRoleMappingEndpointsFilterByRoleContainerPermissions() {
        ClientRepresentation visibleClient = new ClientRepresentation();
        visibleClient.setClientId("visible-client");
        visibleClient.setEnabled(true);
        try (Response response = realm.admin().clients().create(visibleClient)) {
            visibleClient.setId(ApiUtil.getCreatedId(response));
        }

        ClientRepresentation hiddenClient = new ClientRepresentation();
        hiddenClient.setClientId("hidden-client");
        hiddenClient.setEnabled(true);
        try (Response response = realm.admin().clients().create(hiddenClient)) {
            hiddenClient.setId(ApiUtil.getCreatedId(response));
        }

        RoleRepresentation visibleRole = new RoleRepresentation();
        visibleRole.setName("VISIBLE_ROLE");
        visibleRole.setClientRole(true);
        realm.admin().clients().get(visibleClient.getId()).roles().create(visibleRole);
        visibleRole = realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_ROLE").toRepresentation();

        RoleRepresentation hiddenRole = new RoleRepresentation();
        hiddenRole.setName("HIDDEN_ROLE");
        hiddenRole.setClientRole(true);
        realm.admin().clients().get(hiddenClient.getId()).roles().create(hiddenRole);
        hiddenRole = realm.admin().clients().get(hiddenClient.getId()).roles().get("HIDDEN_ROLE").toRepresentation();

        RoleRepresentation realmRole = new RoleRepresentation();
        realmRole.setName("REALM_ROLE");
        realm.admin().roles().create(realmRole);
        realmRole = realm.admin().roles().get("REALM_ROLE").toRepresentation();

        UserRepresentation targetUser = createUser("target-user");
        GroupRepresentation targetGroup = createGroup("target-group");

        realm.admin().users().get(targetUser.getId()).roles().clientLevel(visibleClient.getId()).add(List.of(visibleRole));
        realm.admin().users().get(targetUser.getId()).roles().clientLevel(hiddenClient.getId()).add(List.of(hiddenRole));
        realm.admin().users().get(targetUser.getId()).roles().realmLevel().add(List.of(realmRole));
        realm.admin().groups().group(targetGroup.getId()).roles().clientLevel(visibleClient.getId()).add(List.of(visibleRole));
        realm.admin().groups().group(targetGroup.getId()).roles().clientLevel(hiddenClient.getId()).add(List.of(hiddenRole));
        realm.admin().groups().group(targetGroup.getId()).roles().realmLevel().add(List.of(realmRole));

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "myadmin-policy", myadmin.getId());
        createPermission(adminPermissionsClient, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createGroupPermission(targetGroup, Set.of(VIEW), policy);
        createPermission(adminPermissionsClient, visibleClient.getId(), clientsType, Set.of(VIEW), policy);

        // user role-mappings: getAll should contain visible-client but not hidden-client
        // realm roles should be filtered since myadmin lacks view-realm
        MappingsRepresentation userMappings = realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles().getAll();
        assertThat(userMappings.getRealmMappings(), nullValue());
        Map<String, ?> userClientMappings = userMappings.getClientMappings();
        assertThat(userClientMappings, notNullValue());
        assertThat(userClientMappings, hasKey("visible-client"));
        assertThat(userClientMappings, not(hasKey("hidden-client")));

        // user: visible client roles should be accessible
        List<RoleRepresentation> visibleRoles = realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles().clientLevel(visibleClient.getId()).listAll();
        assertThat(visibleRoles, hasSize(1));

        // user: hidden client role-mappings and composites should be filtered out
        String hiddenId = hiddenClient.getId();
        assertThat(realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles().clientLevel(hiddenId).listAll(), is(empty()));
        assertThat(realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles().clientLevel(hiddenId).listEffective(), is(empty()));

        // group role-mappings: getAll should contain visible-client but not hidden-client
        // realm roles should be filtered since myadmin lacks view-realm
        MappingsRepresentation groupMappings = realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).roles().getAll();
        assertThat(groupMappings.getRealmMappings(), nullValue());
        Map<String, ?> groupClientMappings = groupMappings.getClientMappings();
        assertThat(groupClientMappings, notNullValue());
        assertThat(groupClientMappings, hasKey("visible-client"));
        assertThat(groupClientMappings, not(hasKey("hidden-client")));

        // group: hidden client role-mappings and composites should be filtered out
        assertThat(realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).roles().clientLevel(hiddenId).listAll(), is(empty()));
        assertThat(realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).roles().clientLevel(hiddenId).listEffective(), is(empty()));

        // group full representation should not contain hidden-client in clientRoles
        GroupRepresentation groupRep = realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).toRepresentation();
        Map<String, List<String>> clientRoles = groupRep.getClientRoles();
        assertThat(clientRoles, notNullValue());
        assertThat(clientRoles, hasKey("visible-client"));
        assertThat(clientRoles, not(hasKey("hidden-client")));
    }

    @Test
    public void testRealmRoleMappingVisibleWithMapRolePermission() {
        RoleRepresentation mappableRole = new RoleRepresentation();
        mappableRole.setName("MAPPABLE_REALM_ROLE");
        realm.admin().roles().create(mappableRole);
        mappableRole = realm.admin().roles().get("MAPPABLE_REALM_ROLE").toRepresentation();
        final String mappableRoleId = mappableRole.getId();
        realm.cleanup().add(r -> r.roles().deleteRole("MAPPABLE_REALM_ROLE"));

        UserRepresentation targetUser = createUser("map-role-target-user");
        GroupRepresentation targetGroup = createGroup("map-role-target-group");

        realm.admin().users().get(targetUser.getId()).roles().realmLevel().add(List.of(mappableRole));
        realm.admin().groups().group(targetGroup.getId()).roles().realmLevel().add(List.of(mappableRole));

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "myadmin-map-role-policy", myadmin.getId());
        createPermission(adminPermissionsClient, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createGroupPermission(targetGroup, Set.of(VIEW), policy);
        // grant map-role on the specific realm role only, without view-realm
        createPermission(adminPermissionsClient, mappableRoleId, AdminPermissionsSchema.ROLES.getType(), Set.of(MAP_ROLE), policy);

        // user role-mappings: the mappable realm role must be visible through the map-role permission
        MappingsRepresentation userMappings = realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles().getAll();
        assertThat(userMappings.getRealmMappings(), notNullValue());
        assertThat(userMappings.getRealmMappings().stream().map(RoleRepresentation::getName).toList(), hasItem("MAPPABLE_REALM_ROLE"));

        // group role-mappings: same expectation
        MappingsRepresentation groupMappings = realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).roles().getAll();
        assertThat(groupMappings.getRealmMappings(), notNullValue());
        assertThat(groupMappings.getRealmMappings().stream().map(RoleRepresentation::getName).toList(), hasItem("MAPPABLE_REALM_ROLE"));
    }

    @Test
    public void testMapRolePermissionGrantsRoleByIdRead() {
        RoleRepresentation mappableRole = new RoleRepresentation();
        mappableRole.setName("MAPPABLE_REALM_ROLE");
        realm.admin().roles().create(mappableRole);
        mappableRole = realm.admin().roles().get("MAPPABLE_REALM_ROLE").toRepresentation();
        final String mappableRoleId = mappableRole.getId();
        realm.cleanup().add(r -> r.roles().deleteRole("MAPPABLE_REALM_ROLE"));

        RoleRepresentation unmappableRole = new RoleRepresentation();
        unmappableRole.setName("UNMAPPABLE_REALM_ROLE");
        realm.admin().roles().create(unmappableRole);
        final String unmappableRoleId = realm.admin().roles().get("UNMAPPABLE_REALM_ROLE").toRepresentation().getId();
        realm.cleanup().add(r -> r.roles().deleteRole("UNMAPPABLE_REALM_ROLE"));

        UserRepresentation targetUser = createUser("map-role-view-user");
        realm.admin().users().get(targetUser.getId()).roles().realmLevel().add(List.of(mappableRole));

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "myadmin-map-role-view-policy", myadmin.getId());
        createPermission(adminPermissionsClient, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createPermission(adminPermissionsClient, mappableRoleId, AdminPermissionsSchema.ROLES.getType(), Set.of(MAP_ROLE), policy);

        // the role is visible in the role-mappings list
        MappingsRepresentation userMappings = realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles().getAll();
        assertThat(userMappings.getRealmMappings(), notNullValue());
        assertThat(userMappings.getRealmMappings().stream().map(RoleRepresentation::getName).toList(), hasItem("MAPPABLE_REALM_ROLE"));

        // and reading the role by id is allowed because map-role implies visibility of the role
        RoleRepresentation byId = realmAdminClient.realm(realm.getName()).rolesById().getRole(mappableRoleId);
        assertThat(byId, notNullValue());
        assertThat(byId.getName(), is("MAPPABLE_REALM_ROLE"));

        // roles without map-role remain hidden
        Assertions.assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).rolesById().getRole(unmappableRoleId));

        // but map-role does not grant management of the role itself
        RolesResource roles = realmAdminClient.realm(realm.getName()).roles();
        RoleByIdResource rolesById = realmAdminClient.realm(realm.getName()).rolesById();
        Assertions.assertThrows(ForbiddenException.class, () -> rolesById.updateRole(mappableRoleId, byId));
        Assertions.assertThrows(ForbiddenException.class, () -> rolesById.addComposites(mappableRoleId, List.of()));
        Assertions.assertThrows(ForbiddenException.class, () -> rolesById.deleteComposites(mappableRoleId, List.of()));
        Assertions.assertThrows(ForbiddenException.class, () -> rolesById.deleteRole(mappableRoleId));
        Assertions.assertThrows(ForbiddenException.class, () -> roles.get("MAPPABLE_REALM_ROLE").update(byId));
        Assertions.assertThrows(ForbiddenException.class, () -> roles.get("MAPPABLE_REALM_ROLE").addComposites(List.of()));
        Assertions.assertThrows(ForbiddenException.class, () -> roles.get("MAPPABLE_REALM_ROLE").deleteComposites(List.of()));
        Assertions.assertThrows(ForbiddenException.class, () -> roles.deleteRole("MAPPABLE_REALM_ROLE"));
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    public void testGroupRepresentationFiltersRolesByVisibility(boolean briefRepresentation, boolean canViewParent) {
        RoleRepresentation mappableRealmRole = new RoleRepresentation();
        mappableRealmRole.setName("GROUP_REP_MAPPABLE_REALM_ROLE");
        realm.admin().roles().create(mappableRealmRole);
        mappableRealmRole = realm.admin().roles().get("GROUP_REP_MAPPABLE_REALM_ROLE").toRepresentation();
        realm.cleanup().add(r -> r.roles().deleteRole("GROUP_REP_MAPPABLE_REALM_ROLE"));

        RoleRepresentation hiddenRealmRole = new RoleRepresentation();
        hiddenRealmRole.setName("GROUP_REP_HIDDEN_REALM_ROLE");
        realm.admin().roles().create(hiddenRealmRole);
        hiddenRealmRole = realm.admin().roles().get("GROUP_REP_HIDDEN_REALM_ROLE").toRepresentation();
        realm.cleanup().add(r -> r.roles().deleteRole("GROUP_REP_HIDDEN_REALM_ROLE"));

        ClientRepresentation hiddenClient = new ClientRepresentation();
        hiddenClient.setClientId("group-rep-hidden-client");
        hiddenClient.setEnabled(true);
        try (Response response = realm.admin().clients().create(hiddenClient)) {
            hiddenClient.setId(ApiUtil.getCreatedId(response));
        }
        realm.cleanup().add(r -> r.clients().get(hiddenClient.getId()).remove());

        RoleRepresentation mappableClientRole = new RoleRepresentation();
        mappableClientRole.setName("GROUP_REP_MAPPABLE_CLIENT_ROLE");
        realm.admin().clients().get(hiddenClient.getId()).roles().create(mappableClientRole);
        mappableClientRole = realm.admin().clients().get(hiddenClient.getId()).roles().get("GROUP_REP_MAPPABLE_CLIENT_ROLE").toRepresentation();

        RoleRepresentation hiddenClientRole = new RoleRepresentation();
        hiddenClientRole.setName("GROUP_REP_HIDDEN_CLIENT_ROLE");
        realm.admin().clients().get(hiddenClient.getId()).roles().create(hiddenClientRole);
        hiddenClientRole = realm.admin().clients().get(hiddenClient.getId()).roles().get("GROUP_REP_HIDDEN_CLIENT_ROLE").toRepresentation();

        GroupRepresentation parent = createGroup("group-rep-visibility-parent");
        GroupRepresentation targetGroup = GroupBuilder.create().name("group-rep-visibility-group").build();
        try (Response response = realm.admin().groups().group(parent.getId()).subGroup(targetGroup)) {
            targetGroup.setId(ApiUtil.getCreatedId(response));
        }
        for (GroupRepresentation group : List.of(parent, targetGroup)) {
            group.setAttributes(Map.of("visibility-attribute", List.of("value")));
            realm.admin().groups().group(group.getId()).update(group);
            realm.admin().groups().group(group.getId()).roles().realmLevel().add(List.of(mappableRealmRole, hiddenRealmRole));
            realm.admin().groups().group(group.getId()).roles().clientLevel(hiddenClient.getId()).add(List.of(mappableClientRole, hiddenClientRole));
        }

        UserRepresentation member = createUser("group-rep-visibility-member");
        realm.admin().users().get(member.getId()).joinGroup(targetGroup.getId());

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "myadmin-group-rep-policy", myadmin.getId());
        createGroupPermission(targetGroup, Set.of(VIEW), policy);
        if (canViewParent) {
            createGroupPermission(parent, Set.of(VIEW), policy);
        }
        createPermission(adminPermissionsClient, member.getId(), AdminPermissionsSchema.USERS.getType(), Set.of(VIEW), policy);
        // map-role on specific roles only, without view-realm or view on the client
        createPermission(adminPermissionsClient, mappableRealmRole.getId(), AdminPermissionsSchema.ROLES.getType(), Set.of(MAP_ROLE), policy);
        createPermission(adminPermissionsClient, mappableClientRole.getId(), AdminPermissionsSchema.ROLES.getType(), Set.of(MAP_ROLE), policy);

        RealmResource adminRealm = realmAdminClient.realm(realm.getName());
        GroupRepresentation groupRep = adminRealm.groups().group(targetGroup.getId()).toRepresentation();
        assertGroupRepresentationFiltered(groupRep);

        if (!canViewParent) {
            Assertions.assertThrows(ForbiddenException.class, () -> adminRealm.groups().group(parent.getId()).toRepresentation());
        }

        List<GroupRepresentation> listed = adminRealm.groups().groups(targetGroup.getName(), true, null, null, briefRepresentation);
        GroupRepresentation listedParent = assertSingleGroup(listed, parent.getId(), briefRepresentation || !canViewParent);
        assertThat(listedParent.getSubGroupCount(), is(1L));
        if (!canViewParent) {
            assertThat(listedParent.getAccess(), nullValue());
        }
        assertSingleGroup(listedParent.getSubGroups(), targetGroup.getId(), briefRepresentation);

        List<GroupRepresentation> flat = adminRealm.groups().groups(targetGroup.getName(), true, null, null, briefRepresentation, true, false);
        GroupRepresentation flatGroup = assertSingleGroup(flat, targetGroup.getId(), briefRepresentation);
        assertThat(flatGroup.getSubGroups(), empty());

        String targetPath = "/" + parent.getName() + "/" + targetGroup.getName();
        if (canViewParent) {
            assertGroup(adminRealm.getGroupByPath(targetPath), targetGroup.getId(), false);
        } else {
            // the path cannot be resolved through a parent the caller cannot view
            Assertions.assertThrows(NotFoundException.class, () -> adminRealm.getGroupByPath(targetPath));
        }

        List<GroupRepresentation> memberGroups = adminRealm.users().get(member.getId()).groups(null, null, briefRepresentation);
        assertSingleGroup(memberGroups, targetGroup.getId(), briefRepresentation);
    }

    private static GroupRepresentation assertSingleGroup(List<GroupRepresentation> groups, String groupId, boolean briefRepresentation) {
        assertThat(groups, hasSize(1));
        return assertGroup(groups.get(0), groupId, briefRepresentation);
    }

    private static GroupRepresentation assertGroup(GroupRepresentation group, String groupId, boolean briefRepresentation) {
        assertThat(group.getId(), is(groupId));
        if (briefRepresentation) {
            assertThat(group.getRealmRoles(), nullValue());
            assertThat(group.getClientRoles(), nullValue());
            assertThat(group.getAttributes(), nullValue());
        } else {
            assertGroupRepresentationFiltered(group);
            assertThat(group.getAttributes(), is(Map.of("visibility-attribute", List.of("value"))));
        }
        return group;
    }

    private static void assertGroupRepresentationFiltered(GroupRepresentation groupRep) {
        assertThat(groupRep.getRealmRoles(), contains("GROUP_REP_MAPPABLE_REALM_ROLE"));
        assertThat(groupRep.getClientRoles(), is(Map.of("group-rep-hidden-client", List.of("GROUP_REP_MAPPABLE_CLIENT_ROLE"))));
    }
}
