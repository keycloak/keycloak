package org.keycloak.tests.admin.realm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class RealmRolesUserTest extends AbstractRealmRolesTest {

    /**
     * KEYCLOAK-2035 Verifies that Users assigned to Role are being properly retrieved as members in API endpoint for role membership
     */
    @Test
    public void testUsersInRole() {
        RoleResource role = managedRealm.admin().roles().get("role-with-users");

        List<UserRepresentation> users = managedRealm.admin().users().search("test-role-member");
        assertEquals(1, users.size());
        UserResource user = managedRealm.admin().users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleResource.getUserMembers()));
    }

    /**
     * KEYCLOAK-2035  Verifies that Role with no users assigned is being properly retrieved without members in API endpoint for role membership
     */
    @Test
    public void testUsersNotInRole() {
        RoleResource role = managedRealm.admin().roles().get("role-without-users");

        role = managedRealm.admin().roles().get(role.toRepresentation().getName());
        assertThat(role.getUserMembers(), is(empty()));
    }

    /**
     * KEYCLOAK-2035 Verifies that Role Membership is ok after user removal
     */
    @Test
    public void roleMembershipAfterUserRemoval() {
        RoleResource role = managedRealm.admin().roles().get("role-with-users");

        List<UserRepresentation> users = managedRealm.admin().users().search("test-role-member", null, null, null, null, null);
        assertEquals(1, users.size());
        UserResource user = managedRealm.admin().users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleResource.getUserMembers()));

        managedRealm.admin().users().delete(userRep.getId());
        assertThat(roleResource.getUserMembers(), is(empty()));

        managedRealm.cleanup().add(r -> r.users().create(UserConfigBuilder.create().username("test-role-member").name("Test", "Role User").
                email("test-role-member@test-role-member.com").roles("default-roles-default").emailVerified(true).requiredActions().build()));
    }

    @Test
    public void testRoleMembershipWithPagination() {
        RoleResource role = managedRealm.admin().roles().get("role-with-users");

        // Add a second user
        UserRepresentation userRep2 = new UserRepresentation();
        userRep2.setUsername("test-role-member2");
        userRep2.setEmail("test-role-member2@test-role-member.com");
        userRep2.setRequiredActions(Collections.<String>emptyList());
        userRep2.setEnabled(true);
        managedRealm.admin().users().create(userRep2);

        List<UserRepresentation> users = managedRealm.admin().users().search("test-role-member", null, null, null, null, null);
        assertThat(users, hasSize(2));
        for (UserRepresentation userRepFromList : users) {
            UserResource user = managedRealm.admin().users().get(userRepFromList.getId());
            UserRepresentation userRep = user.toRepresentation();

            RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
            List<RoleRepresentation> rolesToAdd = new LinkedList<>();
            rolesToAdd.add(roleResource.toRepresentation());
            managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);
        }

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());

        List<UserRepresentation> roleUserMembers = roleResource.getUserMembers(0, 1);
        assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleUserMembers));
        Assertions.assertNotNull(roleUserMembers.get(0).getNotBefore(), "Not in full representation");

        roleUserMembers = roleResource.getUserMembers(true, 1, 1);
        assertThat(roleUserMembers, hasSize(1));
        assertEquals(Collections.singletonList("test-role-member2"), extractUsernames(roleUserMembers));
        Assertions.assertNull(roleUserMembers.get(0).getNotBefore(), "Not in brief representation");

        roleUserMembers = roleResource.getUserMembers(true, 2, 1);
        assertThat(roleUserMembers, is(empty()));
    }

    private static List<String> extractUsernames(Collection<UserRepresentation> users) {
        return users.stream().map(UserRepresentation::getUsername).collect(Collectors.toList());
    }
}
