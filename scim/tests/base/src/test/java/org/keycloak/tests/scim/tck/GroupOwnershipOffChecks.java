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
import java.util.Set;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.testframework.realm.ManagedRealm;

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
 * What SCIM does when one of the two group ownership switches is off: exactly what it does without the feature.
 */
final class GroupOwnershipOffChecks {

    private final ManagedRealm realm;
    private final GroupOwnershipSupport support;
    private final ScimClient okta;
    private final ScimClient entra;

    GroupOwnershipOffChecks(ManagedRealm realm, ScimClient okta, ScimClient entra) {
        this.realm = realm;
        this.support = new GroupOwnershipSupport(realm);
        this.okta = okta;
        this.entra = entra;
    }

    void createWithMembersIsRefused() {
        String alice = support.createUser();
        String name = "unowned-" + KeycloakModelUtils.generateId();

        assertStatus(400, () -> okta.groups().create(group(null, name, alice)));
        assertTrue(realm.admin().groups().query(name).stream().noneMatch(g -> name.equals(g.getName())));
    }

    void putWithMembersIsRefused() {
        String alice = support.createUser();
        Group group = support.createGroup(okta);

        assertStatus(400, () -> put(okta, group, alice));
        assertStatus(400, () -> put(okta, group));
        support.assertMembers(group.getId());
    }

    void anyClientCanWriteAndDelete() {
        String alice = support.createUser();
        Group group = support.createGroup(okta);

        patch(entra, group.getId(), new PatchOperation("Add", "members", members(alice)));
        support.assertMembers(group.getId(), alice);
        patch(entra, group.getId(), new PatchOperation("replace", "displayName", "unowned-" + KeycloakModelUtils.generateId()));

        entra.groups().delete(group.getId());
        assertFalse(support.groupExists(group.getId()));
    }

    void usersGroupsWritesWork() {
        String bob = support.createUser();
        Group group = support.createGroup(okta);

        entra.users().patch(bob, PatchRequest.create().add("groups", group.getId()).build());
        support.assertMembers(group.getId(), bob);
    }

    void readShowsMembersOnlyOnRequest() {
        String alice = support.createUser();
        Group group = support.createGroup(okta);
        support.adminJoin(alice, group.getId());

        assertTrue(memberIds(okta.groups().get(group.getId())).isEmpty());
        assertEquals(Set.of(alice), memberIds(okta.groups().get(group.getId(), List.of("displayName", "members"))));
    }
}
