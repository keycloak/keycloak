/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class RoleByIdResourceTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectClient(lifecycle = LifeCycle.METHOD)
    ManagedClient managedClient;

    @InjectAdminEvents
    AdminEvents adminEvents;

    private RoleByIdResource resource;

    private final String roleNameA = "role-a";
    private final String roleNameB = "role-b";
    private final String roleNameC = "role-c";
    private final String roleNameD = "role-d";
    private final String roleNameCompositeWihD = "composite-role-with-d";
    private Map<String, String> roleIds = new HashMap<>();

    @BeforeEach
    public void before() {
        managedRealm.admin().roles().create(RoleConfigBuilder.create().name(roleNameA).description("Role A").build());
        managedRealm.admin().roles().create(RoleConfigBuilder.create().name(roleNameB).description("Role B").build());
        // add a role that is a composite role
        RoleRepresentation roleD = RoleConfigBuilder.create().name(roleNameD).description("Role D").build();
        managedRealm.admin().roles().create(roleD);
        managedRealm.admin().roles().create(RoleConfigBuilder.create()
                .name(roleNameCompositeWihD)
                .description("Composite Role with Role D")
                .composite(true)
                .realmComposite(roleD.getName())
                .build()
        );

        managedRealm.admin().clients().get(managedClient.getId()).roles().create(RoleConfigBuilder.create().name(roleNameC).description("Role C").build());

        managedRealm.admin().roles().list()
                .forEach(r -> roleIds.put(r.getName(), r.getId()));

        managedRealm.admin().clients().get(managedClient.getId()).roles().list()
                .forEach(r -> roleIds.put(r.getName(), r.getId()));

        resource = managedRealm.admin().rolesById();
        adminEvents.skipAll(); // Tested in RealmRolesTest already
    }

    @Test
    public void getRole() {
        RoleRepresentation role = resource.getRole(roleIds.get(roleNameA));
        assertNotNull(role);
        assertEquals(roleNameA, role.getName());
        assertEquals("Role A", role.getDescription());
        assertFalse(role.isComposite());
    }

    @Test
    public void updateRole() {
        RoleRepresentation role = resource.getRole(roleIds.get(roleNameA));

        role.setName("role-a-new");
        role.setDescription("Role A New");

        resource.updateRole(roleIds.get(roleNameA), role);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.roleByIdResourcePath(roleIds.get(roleNameA)), role, ResourceType.REALM_ROLE);

        role = resource.getRole(roleIds.get(roleNameA));

        assertNotNull(role);
        assertEquals("role-a-new", role.getName());
        assertEquals("Role A New", role.getDescription());
        assertFalse(role.isComposite());
    }

    @Test
    public void deleteRole() {
        assertNotNull(resource.getRole(roleIds.get(roleNameA)));
        resource.deleteRole(roleIds.get(roleNameA));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleByIdResourcePath(roleIds.get(roleNameA)), ResourceType.REALM_ROLE);

        try {
            resource.getRole(roleIds.get(roleNameA));
            fail("Expected 404");
        } catch (NotFoundException e) {
            // Ignore
        }
    }

    @Test
    public void composites() {
        assertFalse(resource.getRole(roleIds.get(roleNameA)).isComposite());
        assertEquals(0, resource.getRoleComposites(roleIds.get(roleNameA)).size());

        List<RoleRepresentation> l = new LinkedList<>();
        l.add(RoleConfigBuilder.create().id(roleIds.get(roleNameB)).build());
        l.add(RoleConfigBuilder.create().id(roleIds.get(roleNameC)).build());
        resource.addComposites(roleIds.get(roleNameA), l);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleByIdResourceCompositesPath(roleIds.get(roleNameA)), l, ResourceType.REALM_ROLE);

        Set<RoleRepresentation> composites = resource.getRoleComposites(roleIds.get(roleNameA));

        assertTrue(resource.getRole(roleIds.get(roleNameA)).isComposite());
        Assert.assertNames(composites, roleNameB, roleNameC);

        Set<RoleRepresentation> realmComposites = resource.getRealmRoleComposites(roleIds.get(roleNameA));
        Assert.assertNames(realmComposites, roleNameB);

        Set<RoleRepresentation> clientComposites = resource.getClientRoleComposites(roleIds.get(roleNameA), managedClient.getId());
        Assert.assertNames(clientComposites, roleNameC);

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), null, null, null);
        Assert.assertNames(composites, roleNameB, roleNameC);

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), "b", null, null);
        Assert.assertNames(composites, roleNameB);

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), null, 0, 0);
        assertThat(composites, is(empty()));

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), null, 0, 1);
        Assert.assertNames(composites, roleNameB);

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), null, 1, 1);
        Assert.assertNames(composites, roleNameC);

        resource.deleteComposites(roleIds.get(roleNameA), l);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleByIdResourceCompositesPath(roleIds.get(roleNameA)), l, ResourceType.REALM_ROLE);

        assertFalse(resource.getRole(roleIds.get(roleNameA)).isComposite());
        assertEquals(0, resource.getRoleComposites(roleIds.get(roleNameA)).size());

    }

    /**
     * see KEYCLOAK-12754
     */
    @Test
    public void createNewMixedRealmCompositeRole() {
        RoleRepresentation newRoleComp = RoleConfigBuilder.create()
                .name("role-mixed-comp")
                .composite(true)
                .realmComposite(roleNameA)
                .realmComposite(roleNameCompositeWihD)
                .clientComposite(managedClient.getClientId(), roleNameC).build();
        managedRealm.admin().roles().create(newRoleComp);

        RoleRepresentation roleMixedComp = managedRealm.admin().roles().get(newRoleComp.getName()).toRepresentation();
        assertTrue(roleMixedComp.isComposite());

        Predicate<RoleRepresentation> isClientRole = RoleRepresentation::getClientRole;

        Set<RoleRepresentation> roleComposites = resource.getRoleComposites(roleMixedComp.getId());
        Set<RoleRepresentation> containedRealmRoles = roleComposites.stream().filter(isClientRole.negate()).collect(Collectors.toSet());
        assertFalse(containedRealmRoles.isEmpty());
        assertTrue(containedRealmRoles.stream().anyMatch(r -> r.getName().equals(roleNameA)));
        assertTrue(containedRealmRoles.stream().anyMatch(r -> r.getName().equals(roleNameCompositeWihD)));

        Set<RoleRepresentation> containedClientRoles = roleComposites.stream().filter(isClientRole).collect(Collectors.toSet());
        assertFalse(containedClientRoles.isEmpty());
        assertTrue(containedClientRoles.stream().anyMatch(r -> r.getContainerId().equals(managedClient.getId()) && r.getName().equals(roleNameC)));

        // check that there are no unexpected roles contained
        int expectedCompositeCount = newRoleComp.getComposites().getRealm().size() + newRoleComp.getComposites().getClient().size();
        assertEquals(expectedCompositeCount, roleComposites.size());
    }

    /**
     * see KEYCLOAK-12754
     */
    @Test
    public void createNewMixedRealmCompositeRoleWithUnknownRealmRoleShouldThrow() {
        String unknownRealmRole = "realm-role-unknown";
        RoleRepresentation newRoleComp = RoleConfigBuilder.create()
                .name("role-broken-comp1")
                .composite(true)
                .realmComposite(unknownRealmRole)
                .clientComposite(managedRealm.getId(), roleNameC)
                .build();

        Assertions.assertThrowsExactly(NotFoundException.class, () -> managedRealm.admin().roles().create(newRoleComp));
    }

    /**
     * see KEYCLOAK-12754
     */
    @Test
    public void createNewMixedRealmCompositeRoleWithUnknownClientRoleShouldThrow() {
        String unknownClientRole = "client-role-unknown";
        RoleRepresentation newRoleComp = RoleConfigBuilder.create()
                .name("role-broken-comp2")
                .composite(true)
                .realmComposite(roleNameA)
                .clientComposite(managedClient.getClientId(), unknownClientRole)
                .build();

        Assertions.assertThrowsExactly(NotFoundException.class, () -> managedRealm.admin().roles().create(newRoleComp));
    }

    @Test
    public void attributes() {
        for (String id : roleIds.values()) {
            RoleRepresentation role = resource.getRole(id);
            assertNotNull(role.getAttributes());
            assertTrue(role.getAttributes().isEmpty());

            // update the role with attributes
            Map<String, List<String>> attributes = new HashMap<>();
            List<String> attributeValues = new ArrayList<>();
            attributeValues.add("value1");
            attributes.put("key1", attributeValues);
            attributeValues = new ArrayList<>();
            attributeValues.add("value2.1");
            attributeValues.add("value2.2");
            attributes.put("key2", attributeValues);
            role.setAttributes(attributes);

            resource.updateRole(id, role);
            role = resource.getRole(id);
            assertNotNull(role);
            Map<String, List<String>> roleAttributes = role.getAttributes();
            assertNotNull(roleAttributes);

            Assert.assertRoleAttributes(attributes, roleAttributes);


            // delete an attribute
            attributes.remove("key2");
            role.setAttributes(attributes);
            resource.updateRole(id, role);
            role = resource.getRole(id);
            assertNotNull(role);
            roleAttributes = role.getAttributes();
            assertNotNull(roleAttributes);

            Assert.assertRoleAttributes(attributes, roleAttributes);
        }
    }

    @Test
    public void deleteDefaultRole() {
        RoleRepresentation role = managedRealm.admin().roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()).toRepresentation();
        Assertions.assertThrowsExactly(BadRequestException.class, () -> resource.deleteRole(role.getId()));
    }

    /**
     * see #37320
     */
    @Test
    public void renameRoleToNamePreviouslyCached() {
        String roleName = "realm-role-new-" + new Random().nextInt();
        RoleRepresentation newRoleRepresentation = RoleConfigBuilder.create()
                .name(roleName)
                .build();
        managedRealm.admin().roles().create(newRoleRepresentation);
        RoleRepresentation roleRepresentation = managedRealm.admin().roles().get(roleName).toRepresentation();

        String newRoleName = "realm-role-renamed-" + new Random().nextInt();
        cacheMissingRoleName(newRoleName);

        RoleRepresentation updatedRoleRepresentation = RoleConfigBuilder.create()
                .id(roleRepresentation.getId())
                .name(newRoleName)
                .build();
        resource.updateRole(roleRepresentation.getId(), updatedRoleRepresentation);

        try {
            managedRealm.admin().roles().get(newRoleName).toRepresentation();
        } catch (NotFoundException e) {
            fail("Role is incorrectly cached");
        }
    }

    /**
     * see #37320
     */
    @Test
    public void createRolePreviouslyCached() {
        String roleName = "realm-role-new-" + new Random().nextInt();
        RoleRepresentation roleRepresentation = RoleConfigBuilder.create()
                .name(roleName)
                .build();

        cacheMissingRoleName(roleName);

        managedRealm.admin().roles().create(roleRepresentation);

        try {
            managedRealm.admin().roles().get(roleName).toRepresentation();
        } catch (NotFoundException e) {
            fail("Role is incorrectly cached");
        }
    }

    private void cacheMissingRoleName(String missingRoleName) {
        assertThrows(NotFoundException.class, () -> managedRealm.admin().roles().get(missingRoleName).toRepresentation());
    }
}
