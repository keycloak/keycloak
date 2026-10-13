/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.admin;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.ui.rest.model.RoleDeleteRequest;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for the "ui-ext/role-mapping-delete" endpoints ({@code RoleMappingDeleteResource}), used by the Admin
 * Console to bulk-remove role mappings from users, groups, clients, client scopes and composite roles.
 *
 * Regression coverage for #51459 / #52230: deleting role mappings through these endpoints previously produced
 * admin events with a null representation, unlike the equivalent Admin API calls (e.g. RoleMapperResource,
 * ClientRoleMappingsResource, ScopeMappedResource and RoleResource).
 *
 * These endpoints accept a mix of realm and client roles in a single bulk request, unlike the Admin API where
 * realm and client role mappings are always handled by separate, single-type resources/routes. To keep the
 * resulting admin events accurate, deleted roles are grouped by their actual type (and, for client roles, by
 * client) and one correctly-typed event is fired per group - so a single bulk request can produce more than
 * one admin event.
 */
@KeycloakIntegrationTest
public class RoleMappingDeleteResourceTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void deleteUserRealmRoleMappingIncludesRepresentation() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation role = createRealmRole(realm, "user-realm-role");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("roledeleteuser");
        String userId;
        try (Response response = realm.users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }
        realm.users().get(userId).roles().realmLevel().add(List.of(role));

        assertDeleteIncludesRepresentation("users", userId, ResourceType.REALM_ROLE_MAPPING, List.of(role));
    }

    @Test
    public void deleteGroupRealmRoleMappingIncludesRepresentation() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation role = createRealmRole(realm, "group-realm-role");

        GroupRepresentation group = GroupBuilder.create().name("roledeletegroup").build();
        String groupId;
        try (Response response = realm.groups().add(group)) {
            groupId = ApiUtil.getCreatedId(response);
        }
        realm.groups().group(groupId).roles().realmLevel().add(List.of(role));

        assertDeleteIncludesRepresentation("groups", groupId, ResourceType.REALM_ROLE_MAPPING, List.of(role));
    }

    @Test
    public void deleteClientScopeRoleMappingIncludesRepresentation() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation role = createRealmRole(realm, "client-scope-role");

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("roledeletescope");
        clientScope.setProtocol("openid-connect");
        String scopeId;
        try (Response response = realm.clientScopes().create(clientScope)) {
            scopeId = ApiUtil.getCreatedId(response);
        }
        realm.clientScopes().get(scopeId).getScopeMappings().realmLevel().add(List.of(role));

        assertDeleteIncludesRepresentation("clientScopes", scopeId, ResourceType.REALM_SCOPE_MAPPING, List.of(role));
    }

    @Test
    public void deleteClientRoleMappingIncludesRepresentation() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation role = createRealmRole(realm, "client-scope-mapping-role");

        ClientRepresentation client = ClientBuilder.create().clientId("roledeleteclient").build();
        String clientUuid;
        try (Response response = realm.clients().create(client)) {
            clientUuid = ApiUtil.getCreatedId(response);
        }
        realm.clients().get(clientUuid).getScopeMappings().realmLevel().add(List.of(role));

        // a realm role removed from a client's scope mappings must be tagged REALM_SCOPE_MAPPING, matching
        // ClientResource.getScopeMappedResource() (ScopeMappedResource), regardless of which container
        // (client or client scope) the ui-ext request targets
        assertDeleteIncludesRepresentation("clients", clientUuid, ResourceType.REALM_SCOPE_MAPPING, List.of(role));
    }

    @Test
    public void deleteUserClientRoleMappingIncludesRepresentation() {
        RealmResource realm = managedRealm.admin();

        ClientRepresentation client = ClientBuilder.create().clientId("roledeleteuserclientrole").build();
        String clientUuid;
        try (Response response = realm.clients().create(client)) {
            clientUuid = ApiUtil.getCreatedId(response);
        }
        RoleRepresentation role = createClientRole(realm, clientUuid, "user-client-role");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("roledeleteuserclient");
        String userId;
        try (Response response = realm.users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }
        realm.users().get(userId).roles().clientLevel(clientUuid).add(List.of(role));

        // a client role removed from a user must be tagged CLIENT_ROLE_MAPPING, not REALM_ROLE_MAPPING
        assertDeleteIncludesRepresentation("users", userId, ResourceType.CLIENT_ROLE_MAPPING, List.of(role));
    }

    /**
     * Deleting a mix of realm and client roles from a user in a single bulk request must produce two
     * separately-typed admin events - one REALM_ROLE_MAPPING and one CLIENT_ROLE_MAPPING - each carrying only
     * the representations of the roles of that type. This matches the granularity of the equivalent Admin API
     * calls (RoleMapperResource vs ClientRoleMappingsResource), which are always scoped to a single role type.
     */
    @Test
    public void deleteUserMixedRealmAndClientRoleMappingsFiresSeparateEvents() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation realmRole = createRealmRole(realm, "mixed-realm-role");

        ClientRepresentation client = ClientBuilder.create().clientId("roledeletemixedclient").build();
        String clientUuid;
        try (Response response = realm.clients().create(client)) {
            clientUuid = ApiUtil.getCreatedId(response);
        }
        RoleRepresentation clientRole = createClientRole(realm, clientUuid, "mixed-client-role");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("roledeletemixeduser");
        String userId;
        try (Response response = realm.users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }
        realm.users().get(userId).roles().realmLevel().add(List.of(realmRole));
        realm.users().get(userId).roles().clientLevel(clientUuid).add(List.of(clientRole));

        adminEvents.clear();
        List<RoleDeleteRequest> body = List.of(
                new RoleDeleteRequest(realmRole.getId(), realmRole.getName(), null),
                new RoleDeleteRequest(clientRole.getId(), clientRole.getName(), clientUuid));

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin").path("realms").path(managedRealm.getName())
                    .path("ui-ext").path("role-mapping-delete").path("users").path(userId)
                    .register(new BearerAuthFilter(adminClient.tokenManager()));

            try (Response response = target.request(MediaType.APPLICATION_JSON).post(Entity.json(body))) {
                assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
            }
        }

        // realm roles are grouped and emitted before client roles, see RoleMappingDeleteResource#deleteRoleMappings
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.REALM_ROLE_MAPPING)
                .representation(List.of(realmRole));

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.CLIENT_ROLE_MAPPING)
                .representation(List.of(clientRole));
    }

    @Test
    public void deleteCompositeRoleIncludesRepresentation() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation parentRole = createRealmRole(realm, "composite-parent-role");
        RoleRepresentation childRole = createRealmRole(realm, "composite-child-role");

        realm.roles().get(parentRole.getName()).addComposites(List.of(childRole));

        assertDeleteIncludesRepresentation("roles", parentRole.getId(), ResourceType.REALM_ROLE, List.of(childRole));
    }

    /**
     * Unlike the other delete endpoints, deleteCompositeRoles() picks the admin event's resource type based on
     * whether the parent role is a client role or a realm role. This covers the CLIENT_ROLE branch, the realm
     * role parent case is covered by {@link #deleteCompositeRoleIncludesRepresentation()}.
     */
    @Test
    public void deleteCompositeClientRoleIncludesRepresentation() {
        RealmResource realm = managedRealm.admin();

        ClientRepresentation client = ClientBuilder.create().clientId("roledeletecompositeclient").build();
        String clientUuid;
        try (Response response = realm.clients().create(client)) {
            clientUuid = ApiUtil.getCreatedId(response);
        }

        RoleRepresentation parentRole = createClientRole(realm, clientUuid, "composite-client-parent-role");
        RoleRepresentation childRole = createClientRole(realm, clientUuid, "composite-client-child-role");

        realm.clients().get(clientUuid).roles().get(parentRole.getName()).addComposites(List.of(childRole));

        assertDeleteIncludesRepresentation("roles", parentRole.getId(), ResourceType.CLIENT_ROLE, List.of(childRole));
    }

    private RoleRepresentation createRealmRole(RealmResource realm, String name) {
        realm.roles().create(RoleBuilder.create().name(name).build());
        return realm.roles().get(name).toRepresentation();
    }

    private RoleRepresentation createClientRole(RealmResource realm, String clientUuid, String name) {
        realm.clients().get(clientUuid).roles().create(RoleBuilder.create().name(name).build());
        return realm.clients().get(clientUuid).roles().get(name).toRepresentation();
    }

    /**
     * Calls the ui-ext role-mapping-delete endpoint for the given resource/roles and asserts that the
     * resulting admin event includes the representation of the deleted roles.
     */
    private void assertDeleteIncludesRepresentation(String resourcePathSegment, String resourceId,
            ResourceType expectedResourceType, List<RoleRepresentation> deletedRoles) {
        List<RoleDeleteRequest> body = deletedRoles.stream()
                .map(role -> new RoleDeleteRequest(role.getId(), role.getName(), null))
                .collect(Collectors.toList());

        adminEvents.clear();

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin").path("realms").path(managedRealm.getName())
                    .path("ui-ext").path("role-mapping-delete").path(resourcePathSegment).path(resourceId)
                    .register(new BearerAuthFilter(adminClient.tokenManager()));

            try (Response response = target.request(MediaType.APPLICATION_JSON).post(Entity.json(body))) {
                assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
            }
        }

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(expectedResourceType)
                .representation(deletedRoles);
    }
}
