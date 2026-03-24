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
package org.keycloak.testsuite.composites;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
public class CompositeClientRoleMappingsTest extends AbstractCompositeKeycloakTest {

    // Client A: has leaf roles and a composite role bundling some of them
    private static final String CLIENT_A = "CLIENT_A";
    // Client B: has its own roles and composite, ensuring cross-client isolation
    private static final String CLIENT_B = "CLIENT_B";

    // User 1: assigned composite role from client A + direct leaf from client B
    private static final String USER_1 = "USER_COMPOSITE_CLIENT_ROLES_1";
    // User 2: assigned direct leaf roles only (no composites)
    private static final String USER_2 = "USER_COMPOSITE_CLIENT_ROLES_2";
    // User 3: has no roles at all
    private static final String USER_3 = "USER_NO_ROLES";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realmBuilder = RealmBuilder.create()
                .name("test")
                .ssoSessionIdleTimeout(3000)
                .accessTokenLifespan(10000)
                .ssoSessionMaxLifespan(10000);

        realmBuilder.client(ClientBuilder.create()
                .clientId(CLIENT_A)
                .name(CLIENT_A)
                .secret("password")
                .redirectUris("http://localhost:8180/auth/realms/master/app/*"));

        realmBuilder.client(ClientBuilder.create()
                .clientId(CLIENT_B)
                .name(CLIENT_B)
                .secret("password")
                .redirectUris("http://localhost:8180/auth/realms/master/app/*"));

        realmBuilder.user(UserBuilder.create()
                .username(USER_1)
                .enabled(true)
                .password("password"));

        realmBuilder.user(UserBuilder.create()
                .username(USER_2)
                .enabled(true)
                .password("password"));

        realmBuilder.user(UserBuilder.create()
                .username(USER_3)
                .enabled(true)
                .password("password"));

        testRealms.add(realmBuilder.build());
    }

    @Before
    public void before() {
        if (testContext.isInitialized()) {
            return;
        }

        // --- Client A roles ---
        ClientResource clientA = ApiUtil.findClientByClientId(testRealm(), CLIENT_A);
        createRole(clientA, "A_LEAF_1");
        createRole(clientA, "A_LEAF_2");
        createRole(clientA, "A_LEAF_3");
        createRoleWithAttributes(clientA, "A_LEAF_WITH_ATTRS", Map.of("env", List.of("production"), "tier", List.of("premium")));

        // A_COMPOSITE bundles A_LEAF_1 + A_LEAF_2 + A_LEAF_WITH_ATTRS
        createRole(clientA, "A_COMPOSITE");
        RoleRepresentation aLeaf1 = clientA.roles().get("A_LEAF_1").toRepresentation();
        RoleRepresentation aLeaf2 = clientA.roles().get("A_LEAF_2").toRepresentation();
        RoleRepresentation aLeafWithAttrs = clientA.roles().get("A_LEAF_WITH_ATTRS").toRepresentation();
        clientA.roles().get("A_COMPOSITE").addComposites(List.of(aLeaf1, aLeaf2, aLeafWithAttrs));

        // A_NESTED_COMPOSITE contains A_COMPOSITE (depth 2) + A_LEAF_3
        createRole(clientA, "A_NESTED_COMPOSITE");
        RoleRepresentation aComposite = clientA.roles().get("A_COMPOSITE").toRepresentation();
        RoleRepresentation aLeaf3 = clientA.roles().get("A_LEAF_3").toRepresentation();
        clientA.roles().get("A_NESTED_COMPOSITE").addComposites(List.of(aComposite, aLeaf3));

        // --- Client B roles ---
        ClientResource clientB = ApiUtil.findClientByClientId(testRealm(), CLIENT_B);
        createRole(clientB, "B_LEAF_1");
        createRole(clientB, "B_LEAF_2");

        // B_COMPOSITE bundles B_LEAF_1 + B_LEAF_2
        createRole(clientB, "B_COMPOSITE");
        RoleRepresentation bLeaf1 = clientB.roles().get("B_LEAF_1").toRepresentation();
        RoleRepresentation bLeaf2 = clientB.roles().get("B_LEAF_2").toRepresentation();
        clientB.roles().get("B_COMPOSITE").addComposites(List.of(bLeaf1, bLeaf2));

        // --- User 1: A_NESTED_COMPOSITE (expands to all A roles) + B_LEAF_1 (direct) ---
        UserResource user1 = ApiUtil.findUserByUsernameId(testRealm(), USER_1);
        RoleRepresentation aNestedComposite = clientA.roles().get("A_NESTED_COMPOSITE").toRepresentation();
        user1.roles().clientLevel(clientA.toRepresentation().getId()).add(Collections.singletonList(aNestedComposite));
        user1.roles().clientLevel(clientB.toRepresentation().getId()).add(Collections.singletonList(bLeaf1));

        // --- User 2: A_LEAF_1 (direct) + B_COMPOSITE (expands to B_LEAF_1 + B_LEAF_2) ---
        UserResource user2 = ApiUtil.findUserByUsernameId(testRealm(), USER_2);
        user2.roles().clientLevel(clientA.toRepresentation().getId()).add(Collections.singletonList(aLeaf1));
        RoleRepresentation bComposite = clientB.roles().get("B_COMPOSITE").toRepresentation();
        user2.roles().clientLevel(clientB.toRepresentation().getId()).add(Collections.singletonList(bComposite));

        // User 3 gets no roles

        testContext.setInitialized(true);
    }

    private void createRole(ClientResource client, String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        client.roles().create(role);
    }

    private void createRoleWithAttributes(ClientResource client, String name, Map<String, List<String>> attributes) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        role.setAttributes(attributes);
        client.roles().create(role);
    }

    // --- User 1 + Client A: nested composite expands to all A roles ---

    @Test
    public void testUser1EffectiveClientARoles() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_1);
        ClientResource clientA = ApiUtil.findClientByClientId(testRealm(), CLIENT_A);
        String clientAId = clientA.toRepresentation().getId();

        List<RoleRepresentation> effective = user.roles().clientLevel(clientAId).listEffective();
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
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_1);
        ClientResource clientB = ApiUtil.findClientByClientId(testRealm(), CLIENT_B);
        String clientBId = clientB.toRepresentation().getId();

        List<RoleRepresentation> effective = user.roles().clientLevel(clientBId).listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        // Only B_LEAF_1 was directly assigned, no composite
        assertThat(roleNames, containsInAnyOrder("B_LEAF_1"));
        assertThat(effective, hasSize(1));
    }

    // --- User 2 + Client A: direct leaf only ---

    @Test
    public void testUser2EffectiveClientARoles() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_2);
        ClientResource clientA = ApiUtil.findClientByClientId(testRealm(), CLIENT_A);
        String clientAId = clientA.toRepresentation().getId();

        List<RoleRepresentation> effective = user.roles().clientLevel(clientAId).listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        // Only A_LEAF_1 directly assigned
        assertThat(roleNames, containsInAnyOrder("A_LEAF_1"));
        assertThat(effective, hasSize(1));
    }

    // --- User 2 + Client B: composite expands ---

    @Test
    public void testUser2EffectiveClientBRoles() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_2);
        ClientResource clientB = ApiUtil.findClientByClientId(testRealm(), CLIENT_B);
        String clientBId = clientB.toRepresentation().getId();

        List<RoleRepresentation> effective = user.roles().clientLevel(clientBId).listEffective();
        Set<String> roleNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        // B_COMPOSITE -> B_LEAF_1 + B_LEAF_2
        assertThat(roleNames, containsInAnyOrder("B_COMPOSITE", "B_LEAF_1", "B_LEAF_2"));
        assertThat(effective, hasSize(3));
    }

    // --- Cross-client isolation: Client A roles never appear in Client B results ---

    @Test
    public void testCrossClientIsolation() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_1);
        ClientResource clientB = ApiUtil.findClientByClientId(testRealm(), CLIENT_B);
        String clientBId = clientB.toRepresentation().getId();

        List<RoleRepresentation> effective = user.roles().clientLevel(clientBId).listEffective();

        // None of Client A's roles should appear
        for (RoleRepresentation role : effective) {
            assertThat("Client A role leaked into Client B results: " + role.getName(),
                    role.getName().startsWith("A_"), is(false));
        }
    }

    // --- User with no roles returns empty ---

    @Test
    public void testUserWithNoRolesReturnsEmpty() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_3);
        ClientResource clientA = ApiUtil.findClientByClientId(testRealm(), CLIENT_A);
        String clientAId = clientA.toRepresentation().getId();

        List<RoleRepresentation> effective = user.roles().clientLevel(clientAId).listEffective();
        assertThat(effective, is(empty()));
    }

    // --- briefRepresentation=true: attributes should be null ---

    @Test
    public void testBriefRepresentationOmitsAttributes() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_1);
        ClientResource clientA = ApiUtil.findClientByClientId(testRealm(), CLIENT_A);
        String clientAId = clientA.toRepresentation().getId();

        // briefRepresentation=true (default)
        List<RoleRepresentation> effective = user.roles().clientLevel(clientAId).listEffective(true);

        // All roles should have basic fields
        for (RoleRepresentation role : effective) {
            assertThat(role.getId(), is(notNullValue()));
            assertThat(role.getName(), is(notNullValue()));
        }

        // Attributes should be null in brief mode
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
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_1);
        ClientResource clientA = ApiUtil.findClientByClientId(testRealm(), CLIENT_A);
        String clientAId = clientA.toRepresentation().getId();

        // briefRepresentation=false
        List<RoleRepresentation> effective = user.roles().clientLevel(clientAId).listEffective(false);

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
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_1);
        ClientResource clientA = ApiUtil.findClientByClientId(testRealm(), CLIENT_A);
        String clientAId = clientA.toRepresentation().getId();

        List<RoleRepresentation> effective = user.roles().clientLevel(clientAId).listEffective();

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
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), USER_1);
        ClientResource clientA = ApiUtil.findClientByClientId(testRealm(), CLIENT_A);
        String clientAId = clientA.toRepresentation().getId();

        Set<String> briefNames = user.roles().clientLevel(clientAId).listEffective(true)
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        Set<String> fullNames = user.roles().clientLevel(clientAId).listEffective(false)
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        assertThat("Brief and full representations should return the same roles",
                briefNames, is(fullNames));
    }
}
