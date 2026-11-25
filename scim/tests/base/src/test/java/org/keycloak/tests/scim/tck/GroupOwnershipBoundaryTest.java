/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.tests.scim.tck;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.assertManagedByAnotherClient;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.assertMutability;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.assertStatus;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.group;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.members;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.patch;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.put;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The edges of group ownership: other SCIM clients, a User's {@code groups}, groups that no SCIM client owns, and the
 * owning client being deleted.
 */
@KeycloakIntegrationTest(config = GroupOwnershipServerConfig.class)
public class GroupOwnershipBoundaryTest {

    private static final String TEMPORARY_DIRECTORY = "ownership-temporary-directory";

    @InjectRealm(config = GroupOwnershipRealmConfig.class)
    ManagedRealm realm;

    @InjectScimClient(clientId = "okta-directory")
    ScimClient okta;

    @InjectScimClient(clientId = "entra-directory")
    ScimClient entra;

    @InjectScimClient(clientId = TEMPORARY_DIRECTORY)
    ScimClient temporary;

    GroupOwnershipSupport support;

    @BeforeEach
    public void setUp() {
        support = new GroupOwnershipSupport(realm);
    }

    @Test
    public void testAnotherClientCannotWriteToAnOwnedGroup() {
        String alice = support.createUser();
        String bob = support.createUser();
        Group group = support.createGroup(okta, alice);
        String name = group.getDisplayName();

        assertManagedByAnotherClient(() -> put(entra, group, bob));
        // a PUT without members, and a PATCH that only renames, are refused too
        Group rename = new Group();
        rename.setId(group.getId());
        rename.setDisplayName("owned-renamed-" + KeycloakModelUtils.generateId());
        assertManagedByAnotherClient(() -> entra.groups().update(rename));
        assertManagedByAnotherClient(() -> patch(entra, group.getId(), new PatchOperation("replace", "displayName", "owned-renamed")));
        assertManagedByAnotherClient(() -> patch(entra, group.getId(), new PatchOperation("Add", "members", members(bob))));
        assertManagedByAnotherClient(() -> patch(entra, group.getId(), new PatchOperation("Remove", "members", members(alice))));
        assertManagedByAnotherClient(() -> entra.groups().delete(group.getId()));

        support.assertMembers(group.getId(), alice);
        assertEquals(name, support.groupName(group.getId()));

        okta.groups().delete(group.getId());
        assertFalse(support.groupExists(group.getId()));
    }

    // An administrator can move one directory's group under another's. Deleting the parent would delete both.
    @Test
    public void testClientCannotDeleteAGroupThatHoldsAnotherClientsGroup() {
        Group child = support.createGroup(okta);
        Group parent = support.createGroup(entra);
        GroupRepresentation moved = realm.admin().groups().group(child.getId()).toRepresentation();
        try (Response response = realm.admin().groups().group(parent.getId()).subGroup(moved)) {
            assertEquals(204, response.getStatus());
        }

        assertManagedByAnotherClient(() -> entra.groups().delete(parent.getId()));
        assertTrue(support.groupExists(child.getId()));

        // A group below it that no SCIM client owns does not stop the delete.
        realm.admin().groups().group(child.getId()).remove();
        GroupRepresentation manual = new GroupRepresentation();
        manual.setName("unowned-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parent.getId()).subGroup(manual)) {
            assertEquals(201, response.getStatus());
        }

        entra.groups().delete(parent.getId());
        assertFalse(support.groupExists(parent.getId()));
    }

    // RFC 7643 section 4.1.2: a User's groups are read-only, membership changes go through the Group resource
    @Test
    public void testUsersPatchCannotChangeAnOwnedGroup() {
        String alice = support.createUser();
        String bob = support.createUser();
        Group group = support.createGroup(okta, alice);

        assertMutability(() -> okta.users().patch(bob, PatchRequest.create().add("groups", group.getId()).build()));
        assertMutability(() -> okta.users().patch(alice, PatchRequest.create()
                .remove("groups[value eq \"" + group.getId() + "\"]").build()));
        support.assertMembers(group.getId(), alice);
    }

    @Test
    public void testUsersPutThatChangesAnOwnedGroupIsRefused() {
        String bob = support.createUser();
        Group group = support.createGroup(okta);
        User user = okta.users().get(bob);
        user.addGroup(group.getId());

        assertMutability(() -> okta.users().update(user));
        support.assertMembers(group.getId());
    }

    @Test
    public void testUsersCreateWithAnOwnedGroupIsRefused() {
        Group group = support.createGroup(okta);
        User user = newUser();
        user.addGroup(group.getId());

        assertMutability(() -> okta.users().create(user));
        support.assertMembers(group.getId());
    }

    // Okta's user PUT carries no groups. It must neither be refused nor drop the user's memberships.
    @Test
    public void testUsersPutWithoutGroupsKeepsMemberships() {
        String alice = support.createUser();
        Group group = support.createGroup(okta, alice);

        User user = okta.users().get(alice);
        user.setGroups(null);
        user.setFirstName("NoGroupsSent");
        okta.users().update(user);

        support.assertMembers(group.getId(), alice);
        assertEquals("NoGroupsSent", okta.users().get(alice).getFirstName());
    }

    @Test
    public void testUsersPutWithUnchangedGroupsIsAccepted() {
        String alice = support.createUser();
        Group group = support.createGroup(okta, alice);

        User user = okta.users().get(alice, List.of("groups"));
        assertNotNull(user.getGroups());
        user.setFirstName("Unchanged");
        okta.users().update(user);

        support.assertMembers(group.getId(), alice);
        assertEquals("Unchanged", okta.users().get(alice).getFirstName());
    }

    // A group that no SCIM client owns keeps the behaviour of a realm without owners, also through /Users
    @Test
    public void testUsersWritesToAnUnownedGroupWorkAsBefore() {
        String bob = support.createUser();
        String unowned = support.createAdminGroup();

        okta.users().patch(bob, PatchRequest.create().add("groups", unowned).build());
        support.assertMembers(unowned, bob);
        entra.users().patch(bob, PatchRequest.create().remove("groups[value eq \"" + unowned + "\"]").build());
        support.assertMembers(unowned);
    }

    @Test
    public void testUnownedGroupWorksAsBeforeAndIsNeverAdopted() {
        String alice = support.createUser();
        String bob = support.createUser();
        String id = support.createAdminGroup();
        Group unowned = group(id, support.groupName(id));

        // PATCH add and remove work for any client
        patch(okta, id, new PatchOperation("Add", "members", members(alice, bob)));
        patch(entra, id, new PatchOperation("Remove", "members[value eq \"" + alice + "\"]", null));
        support.assertMembers(id, bob);

        // A PUT with members is refused, for every client, and does not make the caller its owner
        assertStatus(400, () -> put(okta, unowned, alice));
        assertStatus(400, () -> put(entra, unowned));
        support.assertMembers(id, bob);

        // Nobody owns it after all of this: another client can still change and delete it
        patch(entra, id, new PatchOperation("Add", "members", members(alice)));
        support.assertMembers(id, alice, bob);
        entra.groups().delete(id);
        assertFalse(support.groupExists(id));
    }

    // The owner is stored by clientId. Deleting the client keeps it, and a new client with the same clientId owns
    // the group again.
    @Test
    public void testOwnerSurvivesClientDeletionAndComesBackWithTheSameClientId() {
        String alice = support.createUser();
        String bob = support.createUser();
        Group group = support.createGroup(temporary, alice);
        ClientRepresentation original = realm.admin().clients().findByClientId(TEMPORARY_DIRECTORY).get(0);
        List<ProtocolMapperRepresentation> mappers = realm.admin().clients().get(original.getId()).getProtocolMappers().getMappers();

        realm.admin().clients().get(original.getId()).remove();

        try {
            // No owner left that can be reached, but the group is still owned.
            assertManagedByAnotherClient(() -> put(okta, group, bob));
            assertManagedByAnotherClient(() -> okta.groups().delete(group.getId()));
        } finally {
            recreateClient(mappers);
        }

        put(temporary, group, bob);
        support.assertMembers(group.getId(), bob);
        assertManagedByAnotherClient(() -> put(okta, group, alice));
    }

    /**
     * Creates the temporary directory's client again, as the SCIM client supplier first created it.
     */
    private void recreateClient(List<ProtocolMapperRepresentation> mappers) {
        mappers.forEach(mapper -> mapper.setId(null));

        try (Response response = realm.admin().clients().create(ClientBuilder.create()
                .clientId(TEMPORARY_DIRECTORY)
                .secret("secret")
                .serviceAccountsEnabled(true)
                .fullScopeEnabled(false)
                .protocolMappers(mappers.toArray(ProtocolMapperRepresentation[]::new))
                .enabled(true)
                .build())) {
            assertEquals(201, response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            String serviceAccountUserId = realm.admin().clients().get(id).toRepresentation().getServiceAccountUserId();
            support.grantManageUsers(serviceAccountUserId);
            realm.admin().clients().get(id).getScopeMappings().clientLevel(support.realmManagementId()).add(List.of(support.manageUsersRole()));
        }
    }

    private static User newUser() {
        String name = "owned-" + KeycloakModelUtils.generateId();
        User user = new User();
        user.setUserName(name);
        user.setEmail(name + "@testco.example");
        user.setFirstName("Lab");
        user.setLastName("User");
        return user;
    }
}
