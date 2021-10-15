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

package org.keycloak.testsuite.admin;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RoleBuilder;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RoleByIdResourceTest extends AbstractAdminTest {

    private RoleByIdResource resource;

    private Map<String, String> ids = new HashMap<>();

    private String clientId;

    private String clientUuid;

    @Before
    public void before() {
        adminClient.realm(REALM_NAME).roles().create(RoleBuilder.create().name("role-a").description("Role A").build());
        adminClient.realm(REALM_NAME).roles().create(RoleBuilder.create().name("role-b").description("Role B").build());

        clientId = "client-a";
        Response response = adminClient.realm(REALM_NAME).clients().create(ClientBuilder.create().clientId(clientId).build());
        clientUuid = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(clientUuid);
        response.close();
        adminClient.realm(REALM_NAME).clients().get(clientUuid).roles().create(RoleBuilder.create().name("role-c").description("Role C").build());

        for (RoleRepresentation r : adminClient.realm(REALM_NAME).roles().list()) {
            ids.put(r.getName(), r.getId());
        }

        for (RoleRepresentation r : adminClient.realm(REALM_NAME).clients().get(clientUuid).roles().list()) {
            ids.put(r.getName(), r.getId());
        }

        getCleanup().addRoleId(ids.get("role-a"));
        getCleanup().addRoleId(ids.get("role-b"));
        getCleanup().addRoleId(ids.get("role-c"));

        resource = adminClient.realm(REALM_NAME).rolesById();

        assertAdminEvents.clear(); // Tested in RealmRolesTest already
    }

    @Test
    public void getRole() {
        RoleRepresentation role = resource.getRole(ids.get("role-a"));
        assertNotNull(role);
        assertEquals("role-a", role.getName());
        assertEquals("Role A", role.getDescription());
        assertFalse(role.isComposite());
    }

    @Test
    public void updateRole() {
        RoleRepresentation role = resource.getRole(ids.get("role-a"));

        role.setName("role-a-new");
        role.setDescription("Role A New");

        resource.updateRole(ids.get("role-a"), role);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.roleByIdResourcePath(ids.get("role-a")), role, ResourceType.REALM_ROLE);

        role = resource.getRole(ids.get("role-a"));

        assertNotNull(role);
        assertEquals("role-a-new", role.getName());
        assertEquals("Role A New", role.getDescription());
        assertFalse(role.isComposite());
    }

    @Test
    public void deleteRole() {
        assertNotNull(resource.getRole(ids.get("role-a")));
        resource.deleteRole(ids.get("role-a"));
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.roleByIdResourcePath(ids.get("role-a")), ResourceType.REALM_ROLE);

        try {
            resource.getRole(ids.get("role-a"));
            fail("Expected 404");
        } catch (NotFoundException e) {
        }
    }

    @Test
    public void composites() {
        assertFalse(resource.getRole(ids.get("role-a")).isComposite());
        assertEquals(0, resource.getRoleComposites(ids.get("role-a")).size());

        List<RoleRepresentation> l = new LinkedList<>();
        l.add(RoleBuilder.create().id(ids.get("role-b")).build());
        l.add(RoleBuilder.create().id(ids.get("role-c")).build());
        resource.addComposites(ids.get("role-a"), l);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleByIdResourceCompositesPath(ids.get("role-a")), l, ResourceType.REALM_ROLE);

        Set<RoleRepresentation> composites = resource.getRoleComposites(ids.get("role-a"));

        assertTrue(resource.getRole(ids.get("role-a")).isComposite());
        Assert.assertNames(composites, "role-b", "role-c");

        Set<RoleRepresentation> realmComposites = resource.getRealmRoleComposites(ids.get("role-a"));
        Assert.assertNames(realmComposites, "role-b");

        Set<RoleRepresentation> clientComposites = resource.getClientRoleComposites(ids.get("role-a"), clientUuid);
        Assert.assertNames(clientComposites, "role-c");

        composites = resource.searchRoleComposites(ids.get("role-a"), null, null, null);
        Assert.assertNames(composites, "role-b", "role-c");

        composites = resource.searchRoleComposites(ids.get("role-a"), "b", null, null);
        Assert.assertNames(composites, "role-b");

        composites = resource.searchRoleComposites(ids.get("role-a"), null, 0, 0);
        assertThat(composites, is(empty()));

        composites = resource.searchRoleComposites(ids.get("role-a"), null, 0, 1);
        Assert.assertNames(composites, "role-b");

        composites = resource.searchRoleComposites(ids.get("role-a"), null, 1, 1);
        Assert.assertNames(composites, "role-c");

        resource.deleteComposites(ids.get("role-a"), l);
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.roleByIdResourceCompositesPath(ids.get("role-a")), l, ResourceType.REALM_ROLE);

        assertFalse(resource.getRole(ids.get("role-a")).isComposite());
        assertEquals(0, resource.getRoleComposites(ids.get("role-a")).size());

    }

    /**
     * see KEYCLOAK-12754
     */
    @Test
    public void createNewMixedRealmCompositeRole() {

        RoleRepresentation newRoleComp = RoleBuilder.create().name("role-mixed-comp").composite().realmComposite("role-a").clientComposite(clientId, "role-c").build();
        adminClient.realm(REALM_NAME).roles().create(newRoleComp);

        RoleRepresentation roleMixedComp = adminClient.realm(REALM_NAME).roles().get(newRoleComp.getName()).toRepresentation();
        assertTrue(roleMixedComp.isComposite());

        Predicate<RoleRepresentation> isClientRole = RoleRepresentation::getClientRole;

        Set<RoleRepresentation> roleComposites = resource.getRoleComposites(roleMixedComp.getId());
        Set<RoleRepresentation> containedRealmRoles = roleComposites.stream().filter(isClientRole.negate()).collect(Collectors.toSet());
        assertFalse(containedRealmRoles.isEmpty());
        assertTrue(containedRealmRoles.stream().anyMatch(r -> r.getName().equals("role-a")));

        Set<RoleRepresentation> containedClientRoles = roleComposites.stream().filter(isClientRole).collect(Collectors.toSet());
        assertFalse(containedClientRoles.isEmpty());
        assertTrue(containedClientRoles.stream().anyMatch(r -> r.getContainerId().equals(clientUuid) && r.getName().equals("role-c")));
    }

    /**
     * see KEYCLOAK-12754
     */
    @Test(expected = NotFoundException.class)
    public void createNewMixedRealmCompositeRoleWithUnknownRealmRoleShouldThrow() {

        String unknownRealmRole = "realm-role-unknown";
        RoleRepresentation newRoleComp = RoleBuilder.create().name("role-broken-comp1").composite().realmComposite(unknownRealmRole).clientComposite(clientId, "role-c").build();

        adminClient.realm(REALM_NAME).roles().create(newRoleComp);
    }

    /**
     * see KEYCLOAK-12754
     */
    @Test(expected = NotFoundException.class)
    public void createNewMixedRealmCompositeRoleWithUnknownClientRoleShouldThrow() {

        String unknownClientRole = "client-role-unknown";
        RoleRepresentation newRoleComp = RoleBuilder.create().name("role-broken-comp2").composite().realmComposite("role-a").clientComposite(clientId, unknownClientRole).build();
        adminClient.realm(REALM_NAME).roles().create(newRoleComp);
    }

    @Test
    public void attributes() {
        for (String id : ids.values()) {
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

    @Test (expected = BadRequestException.class)
    public void deleteDefaultRole() {
        resource.deleteRole(adminClient.realm(REALM_NAME).roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME).toRepresentation().getId());
    }
}
