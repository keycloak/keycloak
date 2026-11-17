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
package org.keycloak.tests.admin.user;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for the composite realm role mappings admin API endpoint:
 * GET /admin/realms/{realm}/users/{user-id}/role-mappings/realm/composite
 *
 * Call chain exercised by these tests:
 *   test: user.roles().realmLevel().listEffective(briefRepresentation)
 *     -> RoleScopeResource: GET .../role-mappings/realm/composite
 *       -> RoleMapperResource.getCompositeRealmRoleMappings(briefRepresentation)
 *
 * Regression coverage for #51531: the realm variant previously iterated
 * realm.getRolesStream().filter(roleMapper::hasRole), which is O(C x M x D)
 * and was especially expensive for group-inherited roles. The fix mirrors
 * the client-role fix from #47157 by using RoleUtils.getDeepRoleMappings().
 *
 * These tests cover direct role assignment, composite expansion, and
 * group-inherited roles (including nested/parent groups), since the group
 * path was the primary driver of the reported performance regression.
 */
@KeycloakIntegrationTest
public class CompositeRealmRoleMappingsTest {

    @InjectRealm
    ManagedRealm managedRealm;

    // ROLE_LEAF_1/2/3: plain realm roles
    // ROLE_COMPOSITE: bundles LEAF_1 + LEAF_2 + LEAF_WITH_ATTRS
    // ROLE_NESTED_COMPOSITE: bundles ROLE_COMPOSITE (depth 2) + LEAF_3
    // ROLE_GROUP_ONLY: assigned only via group membership, not directly to any user

    private static String user1Id;
    private static String user2Id;
    private static String user3Id;
    private static String user4Id;

    private static String parentGroupId;
    private static String childGroupId;
    private static String compositeGroupId;

    @TestSetup
    public void setup() {
        RealmResource realm = managedRealm.admin();

        // --- Realm roles ---
        realm.roles().create(RoleBuilder.create().name("LEAF_1").build());
        realm.roles().create(RoleBuilder.create().name("LEAF_2").build());
        realm.roles().create(RoleBuilder.create().name("LEAF_3").build());
        realm.roles().create(
                RoleBuilder.create().name("LEAF_WITH_ATTRS")
                        .attributes(Map.of("env", List.of("production"), "tier", List.of("premium")))
                        .build());

        realm.roles().create(RoleBuilder.create().name("ROLE_COMPOSITE").build());
        realm.roles().get("ROLE_COMPOSITE").addComposites(List.of(
                realm.roles().get("LEAF_1").toRepresentation(),
                realm.roles().get("LEAF_2").toRepresentation(),
                realm.roles().get("LEAF_WITH_ATTRS").toRepresentation()
        ));

        realm.roles().create(RoleBuilder.create().name("ROLE_NESTED_COMPOSITE").build());
        realm.roles().get("ROLE_NESTED_COMPOSITE").addComposites(List.of(
                realm.roles().get("ROLE_COMPOSITE").toRepresentation(),
                realm.roles().get("LEAF_3").toRepresentation()
        ));

        realm.roles().create(RoleBuilder.create().name("ROLE_GROUP_ONLY").build());

        // --- Groups: parent -> child hierarchy, role attached to parent ---
        try (Response r = realm.groups().add(GroupBuilder.create().name("PARENT_GROUP").build())) {
            parentGroupId = ApiUtil.getCreatedId(r);
        }
        GroupRepresentation childGroup = GroupBuilder.create().name("CHILD_GROUP").build();
        try (Response r = realm.groups().group(parentGroupId).subGroup(childGroup)) {
            childGroupId = ApiUtil.getCreatedId(r);
        }

        try (Response r = realm.groups().add(GroupBuilder.create().name("COMPOSITE_GROUP").build())) {
            compositeGroupId = ApiUtil.getCreatedId(r);
        }
        realm.groups().group(compositeGroupId).roles().realmLevel().add(
                Collections.singletonList(realm.roles().get("ROLE_NESTED_COMPOSITE").toRepresentation()));

        realm.groups().group(parentGroupId).roles().realmLevel().add(
                Collections.singletonList(realm.roles().get("ROLE_GROUP_ONLY").toRepresentation()));

        // --- Users ---
        try (Response r = realm.users().create(UserBuilder.create().username("USER_1").build())) {
            user1Id = ApiUtil.getCreatedId(r);
        }
        try (Response r = realm.users().create(UserBuilder.create().username("USER_2").build())) {
            user2Id = ApiUtil.getCreatedId(r);
        }
        try (Response r = realm.users().create(UserBuilder.create().username("USER_3").build())) {
            user3Id = ApiUtil.getCreatedId(r);
        }
        try (Response r = realm.users().create(UserBuilder.create().username("USER_4").build())) {
            user4Id = ApiUtil.getCreatedId(r);
        }

        RoleRepresentation defaultRole = realm.roles()
        .get("default-roles-" + managedRealm.getName()).toRepresentation();
        for (String userId : List.of(user1Id, user2Id, user3Id, user4Id)) {
        realm.users().get(userId).roles().realmLevel().remove(Collections.singletonList(defaultRole));
        }

        try (Response r = realm.clients().create(ClientBuilder.create().clientId("TEST_CLIENT").build())) {
        String testClientId = ApiUtil.getCreatedId(r);
        realm.clients().get(testClientId).roles().create(RoleBuilder.create().name("CLIENT_LEAF").build());
        realm.users().get(user1Id).roles().clientLevel(testClientId).add(Collections.singletonList(
                realm.clients().get(testClientId).roles().get("CLIENT_LEAF").toRepresentation()));
        }

        // USER_1: direct ROLE_NESTED_COMPOSITE (expands to all LEAF_* roles)
        realm.users().get(user1Id).roles().realmLevel().add(Collections.singletonList(
                realm.roles().get("ROLE_NESTED_COMPOSITE").toRepresentation()));

        // USER_2: direct LEAF_1 only
        realm.users().get(user2Id).roles().realmLevel().add(Collections.singletonList(
                realm.roles().get("LEAF_1").toRepresentation()));

        // USER_3: no roles, no group membership

        // USER_4: no direct roles, but member of CHILD_GROUP, which inherits
        // ROLE_GROUP_ONLY from its parent PARENT_GROUP
        realm.users().get(user4Id).joinGroup(childGroupId);
    }

    // --- Direct composite role expands correctly ---

    @Test
    public void testUser1EffectiveRealmRoles() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().realmLevel().listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat(roleNames, containsInAnyOrder(
                "ROLE_NESTED_COMPOSITE", "ROLE_COMPOSITE", "LEAF_1", "LEAF_2", "LEAF_3", "LEAF_WITH_ATTRS"));
        assertThat(effective, hasSize(6));
    }

    // --- Direct leaf role only, no expansion ---

    @Test
    public void testUser2EffectiveRealmRoles() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user2Id)
                .roles().realmLevel().listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat(roleNames, containsInAnyOrder("LEAF_1"));
        assertThat(effective, hasSize(1));
    }

    // --- User with no roles and no group membership returns empty ---

    @Test
    public void testUserWithNoRolesReturnsEmpty() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user3Id)
                .roles().realmLevel().listEffective();
        assertThat(effective, is(empty()));
    }

    // --- Group-inherited role via parent group is included in effective roles ---
    // This is the primary regression scenario for #51531: role reached only
    // through group membership (and here, a parent-group chain) must still
    // appear in the composite realm role result.

    @Test
    public void testGroupInheritedRoleIsEffective() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user4Id)
                .roles().realmLevel().listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat("Role attached to a parent group should be inherited by a member of the child group",
                roleNames, containsInAnyOrder("ROLE_GROUP_ONLY"));
        assertThat(effective, hasSize(1));
    }

    // --- Client roles must never appear in realm composite results ---

    @Test
    public void testNoClientRoleLeaksIntoRealmResults() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().realmLevel().listEffective();

        for (RoleRepresentation role : effective) {
            assertFalse(role.getClientRole() != null && role.getClientRole(),
                    "Client role leaked into realm composite results: " + role.getName());
        }
    }

    // --- briefRepresentation=true: attributes should be null ---

    @Test
    public void testBriefRepresentationOmitsAttributes() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().realmLevel().listEffective(true);

        RoleRepresentation leafWithAttrs = effective.stream()
                .filter(r -> "LEAF_WITH_ATTRS".equals(r.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("LEAF_WITH_ATTRS not found in effective roles"));
        assertThat("briefRepresentation should not include attributes",
                leafWithAttrs.getAttributes(), is(nullValue()));
    }

    // --- briefRepresentation=false: attributes should be present ---

    @Test
    public void testFullRepresentationIncludesAttributes() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().realmLevel().listEffective(false);

        RoleRepresentation leafWithAttrs = effective.stream()
                .filter(r -> "LEAF_WITH_ATTRS".equals(r.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("LEAF_WITH_ATTRS not found in effective roles"));

        assertThat("Full representation should include attributes",
                leafWithAttrs.getAttributes(), is(notNullValue()));
        assertThat(leafWithAttrs.getAttributes().get("env"), containsInAnyOrder("production"));
        assertThat(leafWithAttrs.getAttributes().get("tier"), containsInAnyOrder("premium"));
    }

    // --- Composite role flag is correctly set ---

    @Test
    public void testCompositeRoleFlagIsCorrect() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().realmLevel().listEffective();

        Map<String, Boolean> compositeFlags = effective.stream()
                .collect(Collectors.toMap(RoleRepresentation::getName, RoleRepresentation::isComposite));

        assertThat("ROLE_NESTED_COMPOSITE should be composite", compositeFlags.get("ROLE_NESTED_COMPOSITE"), is(true));
        assertThat("ROLE_COMPOSITE should be composite", compositeFlags.get("ROLE_COMPOSITE"), is(true));
        assertThat("LEAF_1 should not be composite", compositeFlags.get("LEAF_1"), is(false));
    }

    // --- Both representations return the same role names ---

    @Test
    public void testBriefAndFullReturnSameRoles() {
        Set<String> briefNames = managedRealm.admin().users().get(user1Id)
                .roles().realmLevel().listEffective(true)
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        Set<String> fullNames = managedRealm.admin().users().get(user1Id)
                .roles().realmLevel().listEffective(false)
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat("Brief and full representations should return the same roles", briefNames, is(fullNames));
    }
    // --- Group endpoint: child group inherits role from parent group directly (not via a user) ---
    // Exercises the GroupModel branch of RoleMapperResource.getCompositeRealmRoleMappings(),
    // which must walk the parent-group chain explicitly since
    // RoleUtils.getDeepRoleMappings() does not do so for GroupModel.

    @Test
    public void testChildGroupEffectiveRealmRolesIncludesParentGroupRole() {
        List<RoleRepresentation> effective = managedRealm.admin().groups().group(childGroupId)
                .roles().realmLevel().listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat("Child group should inherit realm roles assigned directly to its parent group",
                roleNames, containsInAnyOrder("ROLE_GROUP_ONLY"));
        assertThat(effective, hasSize(1));
    }

    // --- Group endpoint: parent group's own direct role is returned ---

    @Test
    public void testParentGroupEffectiveRealmRoles() {
        List<RoleRepresentation> effective = managedRealm.admin().groups().group(parentGroupId)
                .roles().realmLevel().listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat(roleNames, containsInAnyOrder("ROLE_GROUP_ONLY"));
        assertThat(effective, hasSize(1));
    }

    // --- Group endpoint: composite role assigned directly to a group expands correctly ---
    // Verifies RoleUtils.expandCompositeRoles() is still applied for the GroupModel
    // branch after the parent-chain fix, not just for UserModel.

    @Test
    public void testGroupCompositeRoleExpandsCorrectly() {
        List<RoleRepresentation> effective = managedRealm.admin().groups().group(compositeGroupId)
                .roles().realmLevel().listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat(roleNames, containsInAnyOrder(
                "ROLE_NESTED_COMPOSITE", "ROLE_COMPOSITE", "LEAF_1", "LEAF_2", "LEAF_3", "LEAF_WITH_ATTRS"));
        assertThat(effective, hasSize(6));
    }
}