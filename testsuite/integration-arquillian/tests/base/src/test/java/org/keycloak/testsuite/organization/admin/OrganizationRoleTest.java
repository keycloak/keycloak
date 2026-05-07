package org.keycloak.testsuite.organization.admin;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class OrganizationRoleTest extends AbstractOrganizationTest {

    private OrganizationRepresentation org;
    private OrganizationResource orgResource;

    @Before
    public void onBefore() {
        for (OrganizationRepresentation o : managedRealm.admin().organizations().list(null, null)) {
            managedRealm.admin().organizations().get(o.getId()).delete().close();
        }
        org = createOrganization();
        orgResource = managedRealm.admin().organizations().get(org.getId());
    }

    @Test
    public void testCreateRole() {
        RoleRepresentation rep = roleRep("viewer", "Can view resources");
        String roleId;
        try (Response response = orgResource.roles().createRole(rep)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            roleId = ApiUtil.getCreatedId(response);
        }
        assertNotNull(roleId);
        RoleRepresentation created = orgResource.roles().getRoleById(roleId).getRole();
        assertThat(created.getName(), is(equalTo("viewer")));
    }

    @Test
    public void testListRoles() {
        createRole("role-a");
        createRole("role-b");
        List<RoleRepresentation> roles = orgResource.roles().getRoles(null, null, null);
        assertThat(roles, hasSize(2));
        assertThat(roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList()),
                containsInAnyOrder("role-a", "role-b"));
    }

    @Test
    public void testUpdateRole() {
        String id = createRole("old-name");
        RoleRepresentation update = roleRep("new-name", "Updated desc");
        try (Response response = orgResource.roles().getRoleById(id).updateRole(update)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        assertThat(orgResource.roles().getRoleById(id).getRole().getName(), is("new-name"));
    }

    @Test
    public void testDeleteRole() {
        String id = createRole("temp-role");
        orgResource.roles().getRoleById(id).deleteRole();
        assertThrows(NotFoundException.class, () -> orgResource.roles().getRoleById(id).getRole());
    }

    @Test
    public void testAssignRoleToMember() {
        String roleId = createRole("contributor");
        String userId = createMember();

        orgResource.roles().getRoleById(roleId).assignRoleToUser(userId);

        List<UserRepresentation> users = orgResource.roles().getRoleById(roleId).getUsersInRole();
        assertThat(users, hasSize(1));
        assertThat(users.get(0).getId(), is(userId));
    }

    @Test
    public void testAssignRoleToNonMemberFails() {
        String roleId = createRole("contributor");
        String nonMemberId = createRealmUser("not-a-member@test.com");

        try {
            orgResource.roles().getRoleById(roleId).assignRoleToUser(nonMemberId);
            fail("Should have thrown error");
        } catch (WebApplicationException e) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
        }
    }

    private String createRole(String name) {
        try (Response response = orgResource.roles().createRole(roleRep(name, null))) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private static RoleRepresentation roleRep(String name, String description) {
        RoleRepresentation rep = new RoleRepresentation();
        rep.setName(name);
        rep.setDescription(description);
        return rep;
    }

    private String createMember() {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setEmail("member-" + System.nanoTime() + "@test.org");
        user.setUsername(user.getEmail());
        String userId;
        try (Response response = managedRealm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }
        orgResource.members().addMember(userId).close();
        return userId;
    }

    private String createRealmUser(String email) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setEmail(email);
        user.setUsername(email);
        try (Response response = managedRealm.admin().users().create(user)) {
            return ApiUtil.getCreatedId(response);
        }
    }
}
