/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ResourceScopesResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionManagementTest extends AbstractResourceServerTest {

    @Test
    public void testCreatePermissionTicketWithResourceName() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", "kolo", true);
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(new PermissionRequest(resource.getId()));
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());
        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }
        assertPersistence(response, resource);
    }

    @Test
    public void removeUserWithPermissionTicketTest() throws Exception {
        String userToRemoveID = createUser(REALM_NAME, "user-to-remove", "password");

        ResourceRepresentation resource = addResource("Resource A", "kolo", true);
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("user-to-remove", "password").permission().create(new PermissionRequest(resource.getId()));
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("user-to-remove", "password").getToken());
        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }
        assertPersistence(response, resource);

        // Remove the user and expect the user and also hers permission tickets are successfully removed
        adminClient.realm(REALM_NAME).users().delete(userToRemoveID);
        assertThat(adminClient.realm(REALM_NAME).users().list().stream().map(UserRepresentation::getId).collect(Collectors.toList()),
                not(hasItem(userToRemoveID)));
        assertThat(getAuthzClient().protection().permission().findByResource(resource.getId()), is(empty()));
    }

    @Test
    public void testCreatePermissionTicketWithResourceId() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", "kolo", true);
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(new PermissionRequest(resource.getId()));
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }
        assertNotNull(response.getTicket());
        assertFalse(authzClient.protection().permission().findByResource(resource.getId()).isEmpty());
    }

    @Test
    public void testCreatePermissionTicketWithScopes() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", "kolo", true, "ScopeA", "ScopeB", "ScopeC");
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(new PermissionRequest(resource.getId(), "ScopeA", "ScopeB", "ScopeC"));
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }
        assertPersistence(response, resource, "ScopeA", "ScopeB", "ScopeC");
    }

    @Test
    public void testDeleteResourceAndPermissionTicket() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", "kolo", true, "ScopeA", "ScopeB", "ScopeC");
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(new PermissionRequest(resource.getId(), "ScopeA", "ScopeB", "ScopeC"));
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }

        assertPersistence(response, resource, "ScopeA", "ScopeB", "ScopeC");

        getAuthzClient().protection().resource().delete(resource.getId());
        assertTrue(getAuthzClient().protection().permission().findByResource(resource.getId()).isEmpty());
    }

    @Test
    public void testMultiplePermissionRequest() throws Exception {
        List<PermissionRequest> permissions = new ArrayList<>();

        permissions.add(new PermissionRequest(addResource("Resource A", true).getName()));
        permissions.add(new PermissionRequest(addResource("Resource B", true).getName()));
        permissions.add(new PermissionRequest(addResource("Resource C", true).getName()));
        permissions.add(new PermissionRequest(addResource("Resource D", true).getName()));

        PermissionResponse response = getAuthzClient().protection().permission().create(permissions);
        assertNotNull(response.getTicket());
    }

    @Test
    public void testDeleteScopeAndPermissionTicket() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", "kolo", true, "ScopeA", "ScopeB", "ScopeC");
        PermissionRequest permissionRequest = new PermissionRequest(resource.getId());

        permissionRequest.setScopes(new HashSet<>(Arrays.asList("ScopeA", "ScopeB", "ScopeC")));

        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(permissionRequest);
        assertNotNull(response.getTicket());

        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }

        assertEquals(3, authzClient.protection().permission().findByResource(resource.getId()).size());

        AuthorizationResource authorization = getClient(getRealm()).authorization();
        ResourceScopesResource scopes = authorization.scopes();
        ScopeRepresentation scope = scopes.findByName("ScopeA");

        List permissions = authzClient.protection().permission().findByScope(scope.getId());
        assertFalse(permissions.isEmpty());
        assertEquals(1, permissions.size());

        resource.setScopes(Collections.emptySet());
        authorization.resources().resource(resource.getId()).update(resource);
        scopes.scope(scope.getId()).remove();

        assertTrue(authzClient.protection().permission().findByScope(scope.getId()).isEmpty());
        assertEquals(0, authzClient.protection().permission().findByResource(resource.getId()).size());
    }

    @Test
    public void testRemoveScopeFromResource() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", "kolo", true, "ScopeA", "ScopeB");
        PermissionRequest permissionRequest = new PermissionRequest(resource.getId(), "ScopeA", "ScopeB");
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(permissionRequest);

        assertNotNull(response.getTicket());

        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }

        AuthorizationResource authorization = getClient(getRealm()).authorization();
        ResourceScopesResource scopes = authorization.scopes();
        ScopeRepresentation removedScope = scopes.findByName("ScopeA");
        List permissions = authzClient.protection().permission().findByScope(removedScope.getId());
        assertFalse(permissions.isEmpty());

        resource.setScopes(new HashSet<>());
        resource.addScope("ScopeB");

        authorization.resources().resource(resource.getId()).update(resource);
        permissions = authzClient.protection().permission().findByScope(removedScope.getId());
        assertTrue(permissions.isEmpty());

        ScopeRepresentation scopeB = scopes.findByName("ScopeB");
        permissions = authzClient.protection().permission().findByScope(scopeB.getId());
        assertFalse(permissions.isEmpty());
    }

    @Test
    public void testCreatePermissionTicketWithResourceWithoutManagedAccess() throws Exception {
        ResourceRepresentation resource = addResource("Resource A");
        PermissionResponse response = getAuthzClient().protection().permission().create(new PermissionRequest(resource.getName()));
        assertNotNull(response.getTicket());
        assertTrue(getAuthzClient().protection().permission().findByResource(resource.getId()).isEmpty());
    }

    @Test
    public void testTicketNotCreatedWhenResourceOwner() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", "marta", true);
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(new PermissionRequest(resource.getId()));
        assertNotNull(response.getTicket());
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List permissions = authzClient.protection().permission().findByResource(resource.getId());
        assertTrue(permissions.isEmpty());

        response = authzClient.protection("kolo", "password").permission().create(new PermissionRequest(resource.getId()));
        assertNotNull(response.getTicket());
        request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("kolo", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }
        permissions = authzClient.protection().permission().findByResource(resource.getId());
        assertFalse(permissions.isEmpty());
        assertEquals(1, permissions.size());
    }

    @Test
    public void testPermissionForTypedScope() throws Exception {
        ResourceRepresentation typedResource = addResource("Typed Resource", "ScopeC");

        typedResource.setType("typed-resource");

        getClient(getRealm()).authorization().resources().resource(typedResource.getId()).update(typedResource);

        ResourceRepresentation resourceA = addResource("Resource A", "marta", true, "ScopeA", "ScopeB");

        resourceA.setType(typedResource.getType());

        getClient(getRealm()).authorization().resources().resource(resourceA.getId()).update(resourceA);

        PermissionRequest permissionRequest = new PermissionRequest(resourceA.getId());

        permissionRequest.setScopes(new HashSet<>(Arrays.asList("ScopeA", "ScopeC")));

        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("kolo", "password").permission().create(permissionRequest);

        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("kolo", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }

        assertPersistence(response, resourceA, "ScopeA", "ScopeC");
    }

    @Test
    public void testSameTicketForSamePermissionRequest() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", true);
        PermissionResponse response = getAuthzClient().protection("marta", "password").permission().create(new PermissionRequest(resource.getName()));
        assertNotNull(response.getTicket());
    }

    @Test
    public void testSearchTicket() throws Exception {
        ResourceRepresentation resource = addResource("Resource A", "kolo", true);
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(new PermissionRequest(resource.getId()));
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());
        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {

        }
        List<PermissionTicketRepresentation> tickets = authzClient.protection("kolo", "password").permission().find(resource.getId(), null, null, null, null, true, -1, -1);
        assertEquals(1, tickets.size());
        tickets = authzClient.protection("marta", "password").permission().find(resource.getId(), null, null, null, null, true, -1, -1);
        assertEquals(1, tickets.size());

        response = authzClient.protection("alice", "password").permission().create(new PermissionRequest(resource.getId()));
        request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("alice", "password").getToken());
        try {
            authzClient.authorization().authorize(request);
        } catch (Exception e) {
        }
        // two tickets open for kolo, one for alice and one for marta
        tickets = authzClient.protection("kolo", "password").permission().find(resource.getId(), null, null, null, null, true, -1, -1);
        assertEquals(2, tickets.size());
        assertEquals(2L, (long) authzClient.protection("kolo", "password").permission().count(resource.getId(), null, null, null, null, true));
        // one ticket for alice
        tickets = authzClient.protection("alice", "password").permission().find(resource.getId(), null, null, null, null, true, -1, -1);
        assertEquals(1, tickets.size());
        assertEquals(1L, (long) authzClient.protection("alice", "password").permission().count(resource.getId(), null, null, null, null, true));
        PermissionTicketRepresentation aliceTicket = tickets.get(0);
        // one ticket for marta
        tickets = authzClient.protection("marta", "password").permission().find(resource.getId(), null, null, null, null, true, -1, -1);
        assertEquals(1, tickets.size());
        assertEquals(1L, (long) authzClient.protection("marta", "password").permission().count(resource.getId(), null, null, null, null, true));
        // the ticket for alice is different from the ticket for marta
        assertFalse(aliceTicket.getId().equals(tickets.get(0).getId()));


    }

    private void assertPersistence(PermissionResponse response, ResourceRepresentation resource, String... scopeNames) throws Exception {
        String ticket = response.getTicket();
        assertNotNull(ticket);

        int expectedPermissions = scopeNames.length > 0 ? scopeNames.length : 1;
        List<PermissionTicketRepresentation> tickets = getAuthzClient().protection().permission().findByResource(resource.getId());
        assertEquals(expectedPermissions, tickets.size());

        PermissionTicketToken token = new JWSInput(ticket).readJsonContent(PermissionTicketToken.class);

        List<Permission> tokenPermissions = token.getPermissions();
        assertNotNull(tokenPermissions);
        assertEquals(expectedPermissions, scopeNames.length > 0 ? scopeNames.length : tokenPermissions.size());

        Iterator<Permission> permissionIterator = tokenPermissions.iterator();

        while (permissionIterator.hasNext()) {
            Permission resourcePermission = permissionIterator.next();
            long count = tickets.stream().filter(representation -> representation.getResource().equals(resourcePermission.getResourceId())).count();
            if (count == (scopeNames.length > 0 ? scopeNames.length : 1)) {
                permissionIterator.remove();
            }
        }

        assertTrue(tokenPermissions.isEmpty());

        ArrayList<PermissionTicketRepresentation> expectedTickets = new ArrayList<>(tickets);
        Iterator<PermissionTicketRepresentation> ticketIterator = expectedTickets.iterator();

        while (ticketIterator.hasNext()) {
            PermissionTicketRepresentation ticketRep = ticketIterator.next();

            assertFalse(ticketRep.isGranted());

            if (ticketRep.getScope() != null) {
                ScopeRepresentation scope = getClient(getRealm()).authorization().scopes().scope(ticketRep.getScope()).toRepresentation();

                if (Arrays.asList(scopeNames).contains(scope.getName())) {
                    ticketIterator.remove();
                }
            } else if (ticketRep.getResource().equals(resource.getId())) {
                ticketIterator.remove();
            }
        }

        assertTrue(expectedTickets.isEmpty());
    }

    @Test
    public void failInvalidResource() {
        try {
            getAuthzClient().protection().permission().create(new PermissionRequest("Invalid Resource"));
            fail("Should fail, resource does not exist");
        } catch (RuntimeException cause) {
            assertTrue(HttpResponseException.class.isInstance(cause.getCause()));
            assertEquals(400, HttpResponseException.class.cast(cause.getCause()).getStatusCode());
            assertTrue(new String(HttpResponseException.class.cast(cause.getCause()).getBytes()).contains("invalid_resource_id"));
        }
        try {
            getAuthzClient().protection().permission().create(new PermissionRequest());
            fail("Should fail, resource is empty");
        } catch (RuntimeException cause) {
            cause.printStackTrace();
            assertTrue(HttpResponseException.class.isInstance(cause.getCause()));
            assertEquals(400, HttpResponseException.class.cast(cause.getCause()).getStatusCode());
            assertTrue(new String((HttpResponseException.class.cast(cause.getCause()).getBytes())).contains("invalid_resource_id"));
        }
    }

    @Test
    public void failInvalidScope() throws Exception {
        addResource("Resource A", "ScopeA", "ScopeB");
        try {
            PermissionRequest permissionRequest = new PermissionRequest("Resource A");

            permissionRequest.setScopes(new HashSet<>(Arrays.asList("ScopeA", "ScopeC")));

            getAuthzClient().protection().permission().create(permissionRequest);
            fail("Should fail, resource does not exist");
        } catch (RuntimeException cause) {
            assertTrue(HttpResponseException.class.isInstance(cause.getCause()));
            assertEquals(400, HttpResponseException.class.cast(cause.getCause()).getStatusCode());
            assertTrue(new String((HttpResponseException.class.cast(cause.getCause()).getBytes())).contains("invalid_scope"));
        }
    }

    @Test
    public void testGetPermissionTicketWithPagination() throws Exception {
      String[] scopes = {"ScopeA", "ScopeB", "ScopeC", "ScopeD"};
      ResourceRepresentation resource = addResource("Resource A", "kolo", true, scopes);
      AuthzClient authzClient = getAuthzClient();
      PermissionResponse response = authzClient.protection("marta", "password").permission().create(new PermissionRequest(resource.getId(), scopes));
      AuthorizationRequest request = new AuthorizationRequest();
      request.setTicket(response.getTicket());
      request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

      try {
        authzClient.authorization().authorize(request);
      } catch (Exception e) {

      }

      // start with fetching the second half of all permission tickets
      Collection<String> expectedScopes = new ArrayList(Arrays.asList(scopes));
      List<PermissionTicketRepresentation> tickets = getAuthzClient().protection().permission().find(resource.getId(), null, null, null, null, true, 2, 2);
      assertEquals(2, tickets.size(), "Returned number of permissions tickets must match the specified page size (i.e., 'maxResult').");
      boolean foundScope = expectedScopes.remove(tickets.get(0).getScopeName());
      assertTrue(foundScope, "Returned set of permission tickets must be only a sub-set as per pagination offset and specified page size.");
      foundScope = expectedScopes.remove(tickets.get(1).getScopeName());
      assertTrue(foundScope, "Returned set of permission tickets must be only a sub-set as per pagination offset and specified page size.");

      // fetch the first half of all permission tickets
      tickets = getAuthzClient().protection().permission().find(resource.getId(), null, null, null, null, true, 0, 2);
      assertEquals(2, tickets.size(), "Returned number of permissions tickets must match the specified page size (i.e., 'maxResult').");
      foundScope = expectedScopes.remove(tickets.get(0).getScopeName());
      assertTrue(foundScope, "Returned set of permission tickets must be only a sub-set as per pagination offset and specified page size.");
      foundScope = expectedScopes.remove(tickets.get(1).getScopeName());
      assertTrue(foundScope, "Returned set of permission tickets must be only a sub-set as per pagination offset and specified page size.");
    }

    @Test
    public void testPermissionCount() throws Exception {
        String[] scopes = {"ScopeA", "ScopeB", "ScopeC", "ScopeD"};
        ResourceRepresentation resource = addResource("Resource A", "kolo", true, scopes);
        AuthzClient authzClient = getAuthzClient();
        PermissionResponse response = authzClient.protection("marta", "password").permission().create(new PermissionRequest(resource.getId(), scopes));
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        try {
            authzClient.authorization().authorize(request);
        } catch (Exception ignored) {

        }

        Long ticketCount = getAuthzClient().protection().permission().count(resource.getId(), null, null, null, null, true);
        assertEquals(Long.valueOf(4), ticketCount, "Returned number of permissions tickets must match the amount of permission tickets.");
    }

    @Test
    public void testISAdminFilterRespectsResourceServer() {
        // create a second resource server
        getRealm().clients().create(
                ClientBuilder.create()
                        .clientId("resource-server-test-2")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test-2")
                        .defaultRoles("uma_protection")
                        .directAccessGrantsEnabled()
                        .serviceAccountsEnabled(true)
                        .build()
        ).close();

        testingClient.server().run(session -> {
            org.keycloak.models.RealmModel realm = session.realms().getRealmByName("authz-test");
            session.getContext().setRealm(realm);
            org.keycloak.authorization.AuthorizationProvider authorization =
                    session.getProvider(org.keycloak.authorization.AuthorizationProvider.class);
            org.keycloak.authorization.store.StoreFactory storeFactory = authorization.getStoreFactory();
            org.keycloak.authorization.store.PermissionTicketStore ticketStore =
                    storeFactory.getPermissionTicketStore();

            org.keycloak.models.ClientModel client1 = session.clients()
                    .getClientByClientId(realm, "resource-server-test");
            org.keycloak.authorization.model.ResourceServer rs1 =
                    storeFactory.getResourceServerStore().findByClient(client1);

            org.keycloak.models.ClientModel client2 = session.clients()
                    .getClientByClientId(realm, "resource-server-test-2");
            org.keycloak.authorization.model.ResourceServer rs2 =
                    storeFactory.getResourceServerStore().findByClient(client2);

            org.keycloak.authorization.model.Resource resource1 =
                    storeFactory.getResourceStore().create(rs1, "resource-rs1", client1.getId());
            org.keycloak.authorization.model.Resource resource2 =
                    storeFactory.getResourceStore().create(rs2, "resource-rs2", client2.getId());

            String requester = session.users().getUserByUsername(realm, "marta").getId();

            org.keycloak.authorization.model.PermissionTicket ticket1 =
                    ticketStore.create(rs1, resource1, null, requester);
            ticket1.setGrantedTimestamp(System.currentTimeMillis());

            org.keycloak.authorization.model.PermissionTicket ticket2 =
                    ticketStore.create(rs2, resource2, null, requester);
            ticket2.setGrantedTimestamp(System.currentTimeMillis());

            java.util.Map<org.keycloak.authorization.model.PermissionTicket.FilterOption, String> filters =
                    new java.util.EnumMap<>(org.keycloak.authorization.model.PermissionTicket.FilterOption.class);
            filters.put(org.keycloak.authorization.model.PermissionTicket.FilterOption.IS_ADMIN, "true");
            filters.put(org.keycloak.authorization.model.PermissionTicket.FilterOption.GRANTED, "true");

            java.util.List<org.keycloak.authorization.model.PermissionTicket> rs1Tickets =
                    ticketStore.find(rs1, filters, null, null);
            java.util.List<org.keycloak.authorization.model.PermissionTicket> rs2Tickets =
                    ticketStore.find(rs2, filters, null, null);

            assertEquals(1, rs1Tickets.size());
            assertEquals(rs1.getId(), rs1Tickets.get(0).getResourceServer().getId());

            assertEquals(1, rs2Tickets.size());
            assertEquals(rs2.getId(), rs2Tickets.get(0).getResourceServer().getId());
        });

        testingClient.server().run(session -> {
            org.keycloak.models.RealmModel realm = session.realms().getRealmByName("authz-test");
            session.getContext().setRealm(realm);
            org.keycloak.authorization.AuthorizationProvider authorization =
                    session.getProvider(org.keycloak.authorization.AuthorizationProvider.class);
            org.keycloak.authorization.store.StoreFactory storeFactory = authorization.getStoreFactory();
            org.keycloak.authorization.store.PermissionTicketStore ticketStore =
                    storeFactory.getPermissionTicketStore();
            org.keycloak.models.ClientModel client1 = session.clients()
                    .getClientByClientId(realm, "resource-server-test");
            org.keycloak.authorization.model.ResourceServer rs1 =
                    storeFactory.getResourceServerStore().findByClient(client1);

            org.keycloak.models.ClientModel client2 = session.clients()
                    .getClientByClientId(realm, "resource-server-test-2");
            org.keycloak.authorization.model.ResourceServer rs2 =
                    storeFactory.getResourceServerStore().findByClient(client2);
            java.util.Map<org.keycloak.authorization.model.PermissionTicket.FilterOption, String> filters =
                    new java.util.EnumMap<>(org.keycloak.authorization.model.PermissionTicket.FilterOption.class);
            filters.put(org.keycloak.authorization.model.PermissionTicket.FilterOption.IS_ADMIN, "true");
            filters.put(org.keycloak.authorization.model.PermissionTicket.FilterOption.GRANTED, "true");

            java.util.List<org.keycloak.authorization.model.PermissionTicket> rs1Tickets =
                    ticketStore.find(rs1, filters, null, null);
            java.util.List<org.keycloak.authorization.model.PermissionTicket> rs2Tickets =
                    ticketStore.find(rs2, filters, null, null);

            assertEquals(1, rs1Tickets.size());
            assertEquals(rs1.getId(), rs1Tickets.get(0).getResourceServer().getId());

            assertEquals(1, rs2Tickets.size());
            assertEquals(rs2.getId(), rs2Tickets.get(0).getResourceServer().getId());

            rs1Tickets =
                    ticketStore.find(null, filters, null, null);
            assertEquals(2, rs1Tickets.size());
            rs2Tickets =
                    ticketStore.find(null, filters, null, null);
            assertEquals(2, rs2Tickets.size());
        });
    }
}
