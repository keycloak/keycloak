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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.group.Member;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.assertManagedByAnotherClient;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.assertStatus;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.group;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.memberIds;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.members;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.patch;
import static org.keycloak.tests.scim.tck.GroupOwnershipSupport.put;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groups owned by the SCIM client that created them. One directory behaves like an Okta app built with the App
 * Integration Wizard (full member list on PUT), the other like Microsoft Entra ID (PATCH add and remove).
 */
@KeycloakIntegrationTest(config = GroupOwnershipServerConfig.class)
public class GroupOwnershipTest {

    @InjectRealm(config = GroupOwnershipRealmConfig.class)
    ManagedRealm realm;

    @InjectScimClient(clientId = "okta-directory")
    ScimClient okta;

    @InjectScimClient(clientId = "entra-directory")
    ScimClient entra;

    @InjectAdminEvents
    AdminEvents adminEvents;

    GroupOwnershipSupport support;

    @BeforeEach
    public void setUp() {
        support = new GroupOwnershipSupport(realm);
    }

    @Test
    public void testCreateWithMembersMakesTheCallerOwner() {
        String alice = support.createUser();
        adminEvents.clear();

        Group group = support.createGroup(okta, alice);
        support.assertMembers(group.getId(), alice);
        // The create request URI ends at the collection, so the event path needs the new group id as well.
        assertEquals(List.of("CREATE scim/v2/Groups/" + group.getId() + "/" + alice), membershipEventPaths());

        // Okta owns the group: Entra cannot write to it, Okta can.
        assertManagedByAnotherClient(() -> put(entra, group));
        put(okta, group);
        support.assertMembers(group.getId());
    }

    @Test
    public void testCreateWithAnUnknownMemberCreatesNothing() {
        String alice = support.createUser();
        String name = "owned-" + KeycloakModelUtils.generateId();

        assertStatus(400, () -> okta.groups().create(group(null, name, alice, KeycloakModelUtils.generateId())));
        assertTrue(realm.admin().groups().query(name).stream().noneMatch(g -> name.equals(g.getName())));
    }

    @Test
    public void testOwnerPutAddsAndRemovesMembers() {
        String alice = support.createUser();
        String bob = support.createUser();
        String carol = support.createUser();
        Group group = support.createGroup(okta);

        put(okta, group, alice, bob);
        support.assertMembers(group.getId(), alice, bob);

        put(okta, group, bob, carol);
        support.assertMembers(group.getId(), bob, carol);

        put(okta, group);
        support.assertMembers(group.getId());
    }

    @Test
    public void testOwnerPutRemovesMembersAnAdministratorAdded() {
        String alice = support.createUser();
        String manual = support.createUser();
        Group group = support.createGroup(okta, alice);

        // An administrator may change an owned group. The owner's next full list overwrites that change.
        support.adminJoin(manual, group.getId());
        support.assertMembers(group.getId(), alice, manual);

        put(okta, group, alice);
        support.assertMembers(group.getId(), alice);
    }

    @Test
    public void testOwnerPatchAddAndRemove() {
        String alice = support.createUser();
        String bob = support.createUser();
        String carol = support.createUser();
        String id = support.createGroup(entra).getId();

        patch(entra, id, new PatchOperation("Add", "members", members(alice, bob, carol)));
        support.assertMembers(id, alice, bob, carol);

        // remove by filter
        patch(entra, id, new PatchOperation("Remove", "members[value eq \"" + alice + "\"]", null));
        support.assertMembers(id, bob, carol);

        // remove by value list, as Microsoft Entra ID sends it
        patch(entra, id, new PatchOperation("Remove", "members", members(bob)));
        support.assertMembers(id, carol);

        // an empty value list removes nobody
        patch(entra, id, new PatchOperation("Remove", "members", "[]"));
        support.assertMembers(id, carol);

        // a filter with or
        patch(entra, id, new PatchOperation("Add", "members", members(alice)));
        patch(entra, id, new PatchOperation("remove", "members[value eq \"" + alice + "\" or value eq \"" + carol + "\"]", null));
        support.assertMembers(id);

        // remove without a filter or value removes everyone
        patch(entra, id, new PatchOperation("Add", "members", members(alice, bob)));
        patch(entra, id, new PatchOperation("Remove", "members", null));
        support.assertMembers(id);
    }

    @Test
    public void testOwnerPatchReplace() {
        String alice = support.createUser();
        String bob = support.createUser();
        String carol = support.createUser();
        String id = support.createGroup(okta, alice).getId();

        patch(okta, id, new PatchOperation("Replace", "members", members(bob)));
        support.assertMembers(id, bob);

        // no path, members in the value
        patch(okta, id, new PatchOperation("Replace", null, "{\"members\":" + members(alice, carol) + "}"));
        support.assertMembers(id, alice, carol);

        // RFC 7644 section 3.5.2: the schema URN as the path targets the group itself, like no path
        patch(okta, id, new PatchOperation("Replace", Group.SCHEMA, "{\"members\":" + members(bob) + "}"));
        support.assertMembers(id, bob);

        patch(okta, id, new PatchOperation("Add", Group.SCHEMA, "{\"members\":" + members(carol) + "}"));
        support.assertMembers(id, bob, carol);

        // replace together with another attribute
        String renamed = "owned-renamed-" + KeycloakModelUtils.generateId();
        patch(okta, id, new PatchOperation("Replace", null, "{\"displayName\":\"" + renamed + "\",\"members\":[]}"));
        support.assertMembers(id);
        assertEquals(renamed, support.groupName(id));
    }

    @Test
    public void testOwnerPatchWithoutMembersKeepsThem() {
        String alice = support.createUser();
        String id = support.createGroup(okta, alice).getId();
        String renamed = "owned-renamed-" + KeycloakModelUtils.generateId();

        patch(okta, id, new PatchOperation("Replace", null, "{\"displayName\":\"" + renamed + "\"}"));
        assertEquals(renamed, support.groupName(id));
        support.assertMembers(id, alice);
    }

    // Service accounts and administrators are never removed by an owner's write
    @Test
    public void testProtectedMembersLeftOutOfAFullListAreKept() {
        String serviceAccount = support.createServiceAccountUser();
        String admin = support.createAdminUser();
        String alice = support.createUser();
        String bob = support.createUser();
        Group group = support.createGroup(okta, bob);
        support.adminJoin(serviceAccount, group.getId());
        support.adminJoin(admin, group.getId());

        // The full list leaves both out: they stay, and the rest of the request is applied.
        put(okta, group, alice);
        support.assertMembers(group.getId(), serviceAccount, admin, alice);

        patch(okta, group.getId(), new PatchOperation("Replace", "members", members()));
        support.assertMembers(group.getId(), serviceAccount, admin);

        patch(okta, group.getId(), new PatchOperation("Add", "members", members(alice)));
        patch(okta, group.getId(), new PatchOperation("Remove", "members", null));
        support.assertMembers(group.getId(), serviceAccount, admin);

        // Listing a protected member who is already in the group changes nothing and is accepted.
        put(okta, group, admin, alice);
        support.assertMembers(group.getId(), serviceAccount, admin, alice);
    }

    @Test
    public void testNamingAProtectedMemberIsRefused() {
        String serviceAccount = support.createServiceAccountUser();
        String admin = support.createAdminUser();
        String otherAdmin = support.createAdminUser();
        String alice = support.createUser();
        Group group = support.createGroup(okta, alice);
        support.adminJoin(admin, group.getId());

        // Adding one who is not a member, through a full list or PATCH add, is refused as without owners.
        assertStatus(400, () -> put(okta, group, alice, serviceAccount));
        assertStatus(403, () -> put(okta, group, alice, admin, otherAdmin));
        assertStatus(400, () -> patch(okta, group.getId(), new PatchOperation("Add", "members", members(serviceAccount))));
        assertStatus(403, () -> patch(okta, group.getId(), new PatchOperation("Add", "members", members(otherAdmin))));

        // Naming one in a PATCH add or remove is refused even when they are a member.
        assertStatus(403, () -> patch(okta, group.getId(), new PatchOperation("Add", "members", members(admin))));
        assertStatus(403, () -> patch(okta, group.getId(), new PatchOperation("Remove", "members[value eq \"" + admin + "\"]", null)));
        assertStatus(403, () -> patch(okta, group.getId(), new PatchOperation("Remove", "members", members(admin))));

        support.assertMembers(group.getId(), alice, admin);
    }

    @Test
    public void testAnUnknownMemberRejectsTheWholeRequest() {
        String alice = support.createUser();
        String bob = support.createUser();
        String missing = KeycloakModelUtils.generateId();
        Group group = support.createGroup(okta, bob);
        String name = group.getDisplayName();

        // PUT with one unknown member: no rename, nobody added, nobody removed.
        Group renamed = group(group.getId(), "owned-renamed-" + KeycloakModelUtils.generateId(), alice, missing);
        assertStatus(400, () -> okta.groups().update(renamed));
        support.assertMembers(group.getId(), bob);
        assertEquals(name, support.groupName(group.getId()));

        // PUT with a member without a value.
        Group blank = group(group.getId(), name, alice);
        blank.addMember(new Member());
        assertStatus(400, () -> okta.groups().update(blank));
        support.assertMembers(group.getId(), bob);

        // PATCH where the bad member comes after a rename and a good one.
        assertStatus(400, () -> patch(okta, group.getId(),
                new PatchOperation("replace", "displayName", "owned-renamed-" + KeycloakModelUtils.generateId()),
                new PatchOperation("Add", "members", members(alice)),
                new PatchOperation("Add", "members", members(missing))));
        support.assertMembers(group.getId(), bob);
        assertEquals(name, support.groupName(group.getId()));

        // PATCH replace with an unknown member removes nobody.
        assertStatus(400, () -> patch(okta, group.getId(), new PatchOperation("Replace", "members", members(alice, missing))));
        assertStatus(400, () -> patch(okta, group.getId(), new PatchOperation("Remove", "members[value eq \"" + missing + "\"]", null)));
        support.assertMembers(group.getId(), bob);
    }

    // A read of an owned group shows every member, to every client, without being asked for them.
    @Test
    public void testReadShowsEveryMemberToEveryClient() {
        String alice = support.createUser();
        String manual = support.createUser();
        Group group = support.createGroup(okta, alice);
        support.adminJoin(manual, group.getId());

        assertEquals(Set.of(alice, manual), memberIds(okta.groups().get(group.getId())));
        assertEquals(Set.of(alice, manual), memberIds(entra.groups().get(group.getId())));
        assertEquals(Set.of(alice, manual), memberIds(entra.groups().get(group.getId(), List.of("displayName", "members"))));

        // A list filtered by name, which is how a directory looks a group up, shows the same.
        List<Group> listed = entra.groups().getAll("displayName eq \"" + group.getDisplayName() + "\"").getResources();
        assertEquals(1, listed.size());
        assertEquals(Set.of(alice, manual), memberIds(listed.get(0)));

        // Excluding members, or asking only for other attributes, leaves them out.
        assertEquals(Set.of(), memberIds(okta.groups().get(group.getId(), null, List.of("members"))));
        assertEquals(Set.of(), memberIds(okta.groups().get(group.getId(), List.of("displayName"))));
    }

    // The live Okta run of 14 November 2026: after each full-list PUT, Okta read the group back and sent a second PUT
    // built from what it read. A read without members would make the second PUT empty the group.
    @Test
    public void testOktaFollowUpPutBuiltFromTheReadKeepsItsMembers() {
        String alice = support.createUser();
        String bob = support.createUser();
        Group group = support.createGroup(okta, alice, bob);

        put(okta, group, alice);
        Set<String> seen = new HashSet<>(memberIds(okta.groups().get(group.getId())));
        seen.remove(bob);
        put(okta, group, seen.toArray(String[]::new));
        support.assertMembers(group.getId(), alice);

        put(okta, group, alice, bob);
        seen = new HashSet<>(memberIds(okta.groups().get(group.getId())));
        seen.add(bob);
        put(okta, group, seen.toArray(String[]::new));
        support.assertMembers(group.getId(), alice, bob);
    }

    @Test
    public void testMembershipEventsOnlyForJoinsAndLeaves() {
        String alice = support.createUser();
        Group group = support.createGroup(okta);

        adminEvents.clear();
        put(okta, group, alice);
        assertEquals(List.of("CREATE scim/v2/Groups/" + group.getId() + "/" + alice), membershipEventPaths());

        put(okta, group, alice);
        assertEquals(List.of(), membershipEventPaths());

        put(okta, group);
        assertEquals(List.of("DELETE scim/v2/Groups/" + group.getId() + "/" + alice), membershipEventPaths());
    }

    // Two full lists for the same group at the same time: both succeed, and the group ends with one of them.
    @Test
    public void testConcurrentOwnerPutsEndConsistent() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            for (int i = 0; i < 15; i++) {
                String alice = support.createUser();
                String bob = support.createUser();
                String carol = support.createUser();
                Group group = support.createGroup(okta, alice);

                CountDownLatch start = new CountDownLatch(1);
                Future<?> first = executor.submit(gated(start, () -> put(okta, group, alice, bob)));
                Future<?> second = executor.submit(gated(start, () -> put(okta, group, carol)));
                start.countDown();
                first.get(60, TimeUnit.SECONDS);
                second.get(60, TimeUnit.SECONDS);

                Set<String> actual = realm.admin().groups().group(group.getId()).members(0, 1000).stream()
                        .map(UserRepresentation::getId)
                        .collect(Collectors.toSet());
                assertTrue(actual.equals(Set.of(alice, bob)) || actual.equals(Set.of(carol)), "unexpected members " + actual);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // An administrator removes a member while the owner drops it. Both requests must succeed.
    @Test
    public void testAdministratorRemovalRacingOwnerDropBothSucceed() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            for (int i = 0; i < 15; i++) {
                String bob = support.createUser();
                Group group = support.createGroup(okta, bob);

                CountDownLatch start = new CountDownLatch(1);
                Future<?> adminRemoves = executor.submit(gated(start, () -> support.adminLeave(bob, group.getId())));
                Future<?> oktaDrops = executor.submit(gated(start, () -> put(okta, group)));
                start.countDown();
                adminRemoves.get(60, TimeUnit.SECONDS);
                oktaDrops.get(60, TimeUnit.SECONDS);

                support.assertMembers(group.getId());
                assertFalse(support.groupName(group.getId()).isBlank());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private List<String> membershipEventPaths() {
        List<String> events = new ArrayList<>();
        for (AdminEventRepresentation event = adminEvents.poll(); event != null; event = adminEvents.poll()) {
            if ("GROUP_MEMBERSHIP".equals(event.getResourceType())) {
                events.add(event.getOperationType() + " " + event.getResourcePath());
            }
        }
        return events;
    }

    private static Callable<Void> gated(CountDownLatch start, Runnable request) {
        return () -> {
            start.await();
            try {
                request.run();
            } catch (ScimClientException sce) {
                throw new AssertionError("concurrent request failed: "
                        + (sce.getError() == null ? "no error body" : sce.getError().getStatusInt() + " " + sce.getError().getDetail()), sce);
            }
            return null;
        };
    }
}
