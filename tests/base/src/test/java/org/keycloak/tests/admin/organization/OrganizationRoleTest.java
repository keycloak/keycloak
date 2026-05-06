package org.keycloak.tests.admin.organization;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OrganizationRoleTest {

    @InjectRealm(lifecycle = LifeCycle.CLASS)
    ManagedRealm managedRealm;

    private OrganizationResource orgResource;
    private String orgId;

    @BeforeEach
    public void onBefore() {
        List<OrganizationRepresentation> orgs = managedRealm.admin().organizations().list(null, null);
        if (orgs != null) {
            for (OrganizationRepresentation o : orgs) {
                managedRealm.admin().organizations().get(o.getId()).delete().close();
            }
        }

        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName("org-" + System.currentTimeMillis());

        try (Response response = managedRealm.admin().organizations().create(orgRep)) {
            assertEquals(201, response.getStatus());
            orgId = getCreatedId(response);
            orgResource = managedRealm.admin().organizations().get(orgId);
        }
    }

    @Test
    public void testCreateRole() {
        RoleRepresentation rep = roleRep("viewer", "Can view resources");
        String roleId;
        try (Response response = orgResource.roles().createRole(rep)) {
            assertEquals(201, response.getStatus());
            roleId = getCreatedId(response);
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
            assertEquals(204, response.getStatus());
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
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    private String createRole(String name) {
        try (Response response = orgResource.roles().createRole(roleRep(name, null))) {
            return getCreatedId(response);
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
            userId = getCreatedId(response);
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
            return getCreatedId(response);
        }
    }

    private String getCreatedId(Response response) {
        String path = response.getLocation().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
