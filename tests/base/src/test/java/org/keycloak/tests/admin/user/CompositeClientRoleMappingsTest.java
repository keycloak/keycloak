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
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
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
 * Tests for the composite client role mappings admin API endpoint:
 * GET /admin/realms/{realm}/users/{user-id}/role-mappings/clients/{client-id}/composite
 *
 * Call chain exercised by these tests:
 *   test: user.roles().clientLevel(clientId).listEffective(briefRepresentation)
 *     -> RoleScopeResource: GET .../role-mappings/clients/{client-id}/composite
 *       -> ClientRoleMappingsResource.getCompositeClientRoleMappings(briefRepresentation)
 *
 * Verifies that effective (composite-expanded) client roles are correctly returned
 * for users with different role assignments across multiple clients, and that the
 * briefRepresentation parameter controls attribute inclusion.
 */
@KeycloakIntegrationTest
public class CompositeClientRoleMappingsTest {

    @InjectRealm
    ManagedRealm managedRealm;

    // Client A: has leaf roles and a composite role bundling some of them
    // Client B: has its own roles and composite, ensuring cross-client isolation
    private static String clientAId;
    private static String clientBId;

    // User 1: assigned composite role from client A + direct leaf from client B
    // User 2: assigned direct leaf roles only (no composites)
    // User 3: has no roles at all
    private static String user1Id;
    private static String user2Id;
    private static String user3Id;

    @TestSetup
    public void setup() {
        RealmResource realm = managedRealm.admin();

        // --- Create Client A ---
        try (Response r = realm.clients().create(ClientConfigBuilder.create().clientId("CLIENT_A").build())) {
            clientAId = ApiUtil.getCreatedId(r);
        }

        // --- Client A roles ---
        realm.clients().get(clientAId).roles().create(RoleConfigBuilder.create().name("A_LEAF_1").build());
        realm.clients().get(clientAId).roles().create(RoleConfigBuilder.create().name("A_LEAF_2").build());
        realm.clients().get(clientAId).roles().create(RoleConfigBuilder.create().name("A_LEAF_3").build());
        realm.clients().get(clientAId).roles().create(
                RoleConfigBuilder.create().name("A_LEAF_WITH_ATTRS")
                        .attributes(Map.of("env", List.of("production"), "tier", List.of("premium")))
                        .build());

        // A_COMPOSITE bundles A_LEAF_1 + A_LEAF_2 + A_LEAF_WITH_ATTRS
        realm.clients().get(clientAId).roles().create(RoleConfigBuilder.create().name("A_COMPOSITE").build());
        realm.clients().get(clientAId).roles().get("A_COMPOSITE").addComposites(List.of(
                realm.clients().get(clientAId).roles().get("A_LEAF_1").toRepresentation(),
                realm.clients().get(clientAId).roles().get("A_LEAF_2").toRepresentation(),
                realm.clients().get(clientAId).roles().get("A_LEAF_WITH_ATTRS").toRepresentation()
        ));

        // A_NESTED_COMPOSITE contains A_COMPOSITE (depth 2) + A_LEAF_3
        realm.clients().get(clientAId).roles().create(RoleConfigBuilder.create().name("A_NESTED_COMPOSITE").build());
        realm.clients().get(clientAId).roles().get("A_NESTED_COMPOSITE").addComposites(List.of(
                realm.clients().get(clientAId).roles().get("A_COMPOSITE").toRepresentation(),
                realm.clients().get(clientAId).roles().get("A_LEAF_3").toRepresentation()
        ));

        // --- Create Client B ---
        try (Response r = realm.clients().create(ClientConfigBuilder.create().clientId("CLIENT_B").build())) {
            clientBId = ApiUtil.getCreatedId(r);
        }

        // --- Client B roles ---
        realm.clients().get(clientBId).roles().create(RoleConfigBuilder.create().name("B_LEAF_1").build());
        realm.clients().get(clientBId).roles().create(RoleConfigBuilder.create().name("B_LEAF_2").build());

        // B_COMPOSITE bundles B_LEAF_1 + B_LEAF_2
        realm.clients().get(clientBId).roles().create(RoleConfigBuilder.create().name("B_COMPOSITE").build());
        realm.clients().get(clientBId).roles().get("B_COMPOSITE").addComposites(List.of(
                realm.clients().get(clientBId).roles().get("B_LEAF_1").toRepresentation(),
                realm.clients().get(clientBId).roles().get("B_LEAF_2").toRepresentation()
        ));

        // --- Create Users ---
        try (Response r = realm.users().create(UserConfigBuilder.create().username("USER_1").build())) {
            user1Id = ApiUtil.getCreatedId(r);
        }
        try (Response r = realm.users().create(UserConfigBuilder.create().username("USER_2").build())) {
            user2Id = ApiUtil.getCreatedId(r);
        }
        try (Response r = realm.users().create(UserConfigBuilder.create().username("USER_3").build())) {
            user3Id = ApiUtil.getCreatedId(r);
        }

        // --- User 1: A_NESTED_COMPOSITE (expands to all A roles) + B_LEAF_1 (direct) ---
        realm.users().get(user1Id).roles().clientLevel(clientAId).add(Collections.singletonList(
                realm.clients().get(clientAId).roles().get("A_NESTED_COMPOSITE").toRepresentation()));
        realm.users().get(user1Id).roles().clientLevel(clientBId).add(Collections.singletonList(
                realm.clients().get(clientBId).roles().get("B_LEAF_1").toRepresentation()));

        // --- User 2: A_LEAF_1 (direct) + B_COMPOSITE (expands to B_LEAF_1 + B_LEAF_2) ---
        realm.users().get(user2Id).roles().clientLevel(clientAId).add(Collections.singletonList(
                realm.clients().get(clientAId).roles().get("A_LEAF_1").toRepresentation()));
        realm.users().get(user2Id).roles().clientLevel(clientBId).add(Collections.singletonList(
                realm.clients().get(clientBId).roles().get("B_COMPOSITE").toRepresentation()));

        // User 3 gets no roles
    }

    // --- User 1 + Client A: nested composite expands to all A roles ---

    @Test
    public void testUser1EffectiveClientARoles() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().clientLevel(clientAId).listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        // A_NESTED_COMPOSITE -> A_COMPOSITE + A_LEAF_3
        // A_COMPOSITE -> A_LEAF_1 + A_LEAF_2 + A_LEAF_WITH_ATTRS
        assertThat(roleNames, containsInAnyOrder(
                "A_NESTED_COMPOSITE", "A_COMPOSITE", "A_LEAF_1", "A_LEAF_2", "A_LEAF_3", "A_LEAF_WITH_ATTRS"));
        assertThat(effective, hasSize(6));
    }

    // --- User 1 + Client B: only direct B_LEAF_1, no composite expansion ---

    @Test
    public void testUser1EffectiveClientBRoles() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().clientLevel(clientBId).listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat(roleNames, containsInAnyOrder("B_LEAF_1"));
        assertThat(effective, hasSize(1));
    }

    // --- User 2 + Client A: direct leaf only ---

    @Test
    public void testUser2EffectiveClientARoles() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user2Id)
                .roles().clientLevel(clientAId).listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat(roleNames, containsInAnyOrder("A_LEAF_1"));
        assertThat(effective, hasSize(1));
    }

    // --- User 2 + Client B: composite expands ---

    @Test
    public void testUser2EffectiveClientBRoles() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user2Id)
                .roles().clientLevel(clientBId).listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        // B_COMPOSITE -> B_LEAF_1 + B_LEAF_2
        assertThat(roleNames, containsInAnyOrder("B_COMPOSITE", "B_LEAF_1", "B_LEAF_2"));
        assertThat(effective, hasSize(3));
    }

    // --- Cross-client isolation: Client A roles never appear in Client B results ---

    @Test
    public void testCrossClientIsolation() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().clientLevel(clientBId).listEffective();

        for (RoleRepresentation role : effective) {
            assertFalse(role.getName().startsWith("A_"),
                    "Client A role leaked into Client B results: " + role.getName());
        }
    }

    // --- User with no roles returns empty ---

    @Test
    public void testUserWithNoRolesReturnsEmpty() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user3Id)
                .roles().clientLevel(clientAId).listEffective();
        assertThat(effective, is(empty()));
    }

    // --- briefRepresentation=true: attributes should be null ---

    @Test
    public void testBriefRepresentationOmitsAttributes() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().clientLevel(clientAId).listEffective(true);

        for (RoleRepresentation role : effective) {
            assertThat(role.getId(), is(notNullValue()));
            assertThat(role.getName(), is(notNullValue()));
        }

        RoleRepresentation leafWithAttrs = effective.stream()
                .filter(r -> "A_LEAF_WITH_ATTRS".equals(r.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("A_LEAF_WITH_ATTRS not found in effective roles"));
        assertThat("briefRepresentation should not include attributes",
                leafWithAttrs.getAttributes(), is(nullValue()));
    }

    // --- briefRepresentation=false: attributes should be present ---

    @Test
    public void testFullRepresentationIncludesAttributes() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().clientLevel(clientAId).listEffective(false);

        RoleRepresentation leafWithAttrs = effective.stream()
                .filter(r -> "A_LEAF_WITH_ATTRS".equals(r.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("A_LEAF_WITH_ATTRS not found in effective roles"));

        assertThat("Full representation should include attributes",
                leafWithAttrs.getAttributes(), is(notNullValue()));
        assertThat(leafWithAttrs.getAttributes().get("env"), containsInAnyOrder("production"));
        assertThat(leafWithAttrs.getAttributes().get("tier"), containsInAnyOrder("premium"));
    }

    // --- Composite role flag is correctly set ---

    @Test
    public void testCompositeRoleFlagIsCorrect() {
        List<RoleRepresentation> effective = managedRealm.admin().users().get(user1Id)
                .roles().clientLevel(clientAId).listEffective();

        Map<String, Boolean> compositeFlags = effective.stream()
                .collect(Collectors.toMap(RoleRepresentation::getName, RoleRepresentation::isComposite));

        assertThat("A_NESTED_COMPOSITE should be composite", compositeFlags.get("A_NESTED_COMPOSITE"), is(true));
        assertThat("A_COMPOSITE should be composite", compositeFlags.get("A_COMPOSITE"), is(true));
        assertThat("A_LEAF_1 should not be composite", compositeFlags.get("A_LEAF_1"), is(false));
        assertThat("A_LEAF_2 should not be composite", compositeFlags.get("A_LEAF_2"), is(false));
        assertThat("A_LEAF_3 should not be composite", compositeFlags.get("A_LEAF_3"), is(false));
        assertThat("A_LEAF_WITH_ATTRS should not be composite", compositeFlags.get("A_LEAF_WITH_ATTRS"), is(false));
    }

    // --- Both representations return the same role names ---

    @Test
    public void testBriefAndFullReturnSameRoles() {
        Set<String> briefNames = managedRealm.admin().users().get(user1Id)
                .roles().clientLevel(clientAId).listEffective(true)
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        Set<String> fullNames = managedRealm.admin().users().get(user1Id)
                .roles().clientLevel(clientAId).listEffective(false)
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat("Brief and full representations should return the same roles", briefNames, is(fullNames));
    }
}
