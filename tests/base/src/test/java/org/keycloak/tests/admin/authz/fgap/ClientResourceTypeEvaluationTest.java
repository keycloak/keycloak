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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
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
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.CLIENTS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES_COMPOSITE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
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

@KeycloakIntegrationTest(config = ClientResourceTypeEvaluationTest.DynamicClientScopeServerConfig.class)
public class ClientResourceTypeEvaluationTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectClient(ref = "realmClient")
    ManagedClient realmClient;

    private final String clientsType = AdminPermissionsSchema.CLIENTS.getType();

    @Test
    public void testUnsupportedPolicyTypes() {
        assertSupportForPolicyType("resource", () -> getPermissionsResource(client).resource().create(new ResourcePermissionRepresentation()), false);
    }

    @Test
    public void testSupportedPolicyTypes() {
        assertSupportForPolicyType("scope", () -> getPermissionsResource(client).scope().create(PermissionBuilder.create()
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

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createPermission(client, myclient.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

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

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());

        ClientResource fullAdminClient = realm.admin().clients().get(myclient.getId());
        for (boolean defaultScope : List.of(true, false)) {
            ClientScopeRepresentation scope = new ClientScopeRepresentation();
            scope.setName(defaultScope ? "assigned-default" : "assigned-optional");
            scope.setDescription("Description of " + scope.getName());
            scope.setProtocol("openid-connect");
            scope.setAttributes(Map.of(
                    ClientScopeModel.IS_DYNAMIC_SCOPE, Boolean.toString(!defaultScope),
                    ClientScopeModel.DYNAMIC_SCOPE_REGEXP, defaultScope ? "" : "assigned-optional:*",
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
        createPermission(client, myclient.getId(), clientsType, Set.of(VIEW), onlyMyAdminUserPolicy);
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
            assertThat(scope.getAttributes(), is(Map.of(ClientScopeModel.IS_DYNAMIC_SCOPE, Boolean.toString("assigned-optional".equals(scope.getName())))));
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
        createPermission(client, myclient.getId(), clientsType, Set.of(MANAGE), onlyMyAdminUserPolicy);
        List<ClientScopeRepresentation> availableScopes = clientResource.getAvailableClientScopes();
        assertThat(availableScopes.stream().map(ClientScopeRepresentation::getId).collect(Collectors.toSet()), is(expectedAvailableIds));
        for (ClientScopeRepresentation scope : availableScopes) {
            assertThat(scope.getProtocolMappers(), nullValue());
            assertThat(scope.getAttributes().keySet(), is(Set.of(ClientScopeModel.IS_DYNAMIC_SCOPE)));
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
        createAllPermission(client, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));
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

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(client, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

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
    public void testClientScopeEvaluation() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setClientId("newClient");
        newClient.setProtocol("openid-connect");

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(client, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        realmAdminClient.realm(realm.getName()).clients().create(newClient).close();
        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findByClientId("newClient");
        assertThat(found, hasSize(1));

        UserRepresentation user = UserConfigBuilder.create()
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

        createPermission(client, user.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), onlyMyAdminUserPolicy);
        clientApi.clientScopesEvaluate().generateAccessToken("openid", user.getId(), null);
    }

    @Test
    public void testPolicyEvaluation() {
        ClientRepresentation myResourceServer = realm.admin().clients().findByClientId("myresourceserver").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createPermission(client, myResourceServer.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

        UserRepresentation user = UserConfigBuilder.create()
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

        createPermission(client, user.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), onlyMyAdminUserPolicy);
        PolicyEvaluationResponse response = clientApi.authorization().policies().evaluate(request);
        assertThat(response, notNullValue());
    }

    @Test
    public void testPolicyEvaluationRequiresViewOnTargetClient() {
        ClientRepresentation myResourceServer = realm.admin().clients().findByClientId("myresourceserver").get(0);
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createPermission(client, myResourceServer.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

        UserRepresentation user = UserConfigBuilder.create()
                .username(KeycloakModelUtils.generateId())
                .build();
        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        createPermission(client, user.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), onlyMyAdminUserPolicy);

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

        createPermission(client, myclient.getId(), clientsType, Set.of(VIEW), onlyMyAdminUserPolicy);
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


        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(client, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW));

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


        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        ScopePermissionRepresentation allPermission = createAllPermission(client, clientsType, onlyMyAdminUserPolicy, CLIENTS.getScopes().stream().filter(Predicate.not(MANAGE::equals)).collect(Collectors.toSet()));

        // can't create a new client
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // grants manage access
        allPermission = getScopePermissionsResource(client).findByName(allPermission.getName());
        allPermission.setScopes(Set.of(VIEW, MANAGE));
        getScopePermissionsResource(client).findById(allPermission.getId()).update(allPermission);

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

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createPermission(client, myadmin.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(MAP_ROLES), onlyMyAdminUserPolicy);
        createPermission(client, myclient.getId(), clientsType, Set.of(MAP_ROLES), onlyMyAdminUserPolicy);

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

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());

        createPermission(client, myclient.getId(), clientsType, Set.of(MAP_ROLES_COMPOSITE), onlyMyAdminUserPolicy);

        // the following operations should fail as the permission to manage the realmClient is missing
        try {
            realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).roles().get("roleB").addComposites(List.of(roleA));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        createPermission(client, realmClient.getId(), clientsType, Set.of(MANAGE), onlyMyAdminUserPolicy);

        // now those should pass
        realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).roles().get("roleB").addComposites(List.of(roleA));
    }

    @Test
    public void testEvaluateAllResourcePermissionsForSpecificResourcePermission() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation adminUser = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, client, "Only My Admin", adminUser.getId());
        ScopePermissionRepresentation allResourcesPermission = createAllPermission(client, clientsType, allowPolicy, Set.of(MANAGE, MAP_ROLES));
        // all resource permissions grants manage scope
        ClientsResource clients = realmAdminClient.realm(realm.getName()).clients();
        clients.get(myclient.getId()).update(myclient);

        ScopePermissionRepresentation resourcePermission = createPermission(client, myclient.getId(), clientsType, Set.of(MANAGE), allowPolicy);
        // both all and specific resource permission grants manage scope
        clients.get(myclient.getId()).update(myclient);

        allResourcesPermission = getScopePermissionsResource(client).findByName(allResourcesPermission.getName());
        allResourcesPermission.setScopes(Set.of(MAP_ROLES));
        getScopePermissionsResource(client).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission does not have the manage scope but the scope is granted by the resource permission
        clients.get(myclient.getId()).update(myclient);

        resourcePermission = getScopePermissionsResource(client).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(MAP_ROLES));
        getScopePermissionsResource(client).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // neither the all and specific resource permission grants access to the manage scope
            clients.get(myclient.getId()).update(myclient);
            Assertions.fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        allResourcesPermission.setScopes(Set.of(MANAGE));
        getScopePermissionsResource(client).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission grants access again to manage
        clients.get(myclient.getId()).update(myclient);

        UserPolicyRepresentation notAllowPolicy = createUserPolicy(Logic.NEGATIVE, realm, client, "Not My Admin", adminUser.getId());
        createPermission(client, myclient.getId(), clientsType, Set.of(MANAGE), notAllowPolicy);
        try {
            // a specific resource permission that explicitly negates access to the manage scope denies access to the scope
            clients.get(myclient.getId()).update(myclient);
            Assertions.fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        resourcePermission = getScopePermissionsResource(client).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(MAP_ROLES, MANAGE));
        getScopePermissionsResource(client).findById(resourcePermission.getId()).update(resourcePermission);
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
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createPermission(client, myResourceServer.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

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
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createPermission(client, myclientId, clientsType, Set.of(VIEW), policy);

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
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "myadmin-policy", myadmin.getId());
        createPermission(client, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createGroupPermission(targetGroup, Set.of(VIEW), policy);
        createPermission(client, visibleClient.getId(), clientsType, Set.of(VIEW), policy);

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

        // user: hidden client role-mappings and composites should be denied
        String hiddenId = hiddenClient.getId();
        Assertions.assertThrows(ForbiddenException.class, () ->
                realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles().clientLevel(hiddenId).listAll());
        Assertions.assertThrows(ForbiddenException.class, () ->
                realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles().clientLevel(hiddenId).listEffective());

        // group role-mappings: getAll should contain visible-client but not hidden-client
        // realm roles should be filtered since myadmin lacks view-realm
        MappingsRepresentation groupMappings = realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).roles().getAll();
        assertThat(groupMappings.getRealmMappings(), nullValue());
        Map<String, ?> groupClientMappings = groupMappings.getClientMappings();
        assertThat(groupClientMappings, notNullValue());
        assertThat(groupClientMappings, hasKey("visible-client"));
        assertThat(groupClientMappings, not(hasKey("hidden-client")));

        // group: hidden client role-mappings and composites should be denied
        Assertions.assertThrows(ForbiddenException.class, () ->
                realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).roles().clientLevel(hiddenId).listAll());
        Assertions.assertThrows(ForbiddenException.class, () ->
                realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).roles().clientLevel(hiddenId).listEffective());

        // group full representation should not contain hidden-client in clientRoles
        GroupRepresentation groupRep = realmAdminClient.realm(realm.getName()).groups().group(targetGroup.getId()).toRepresentation();
        Map<String, List<String>> clientRoles = groupRep.getClientRoles();
        assertThat(clientRoles, notNullValue());
        assertThat(clientRoles, hasKey("visible-client"));
        assertThat(clientRoles, not(hasKey("hidden-client")));
    }

    public static class DynamicClientScopeServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.DYNAMIC_SCOPES);
        }
    }
}
