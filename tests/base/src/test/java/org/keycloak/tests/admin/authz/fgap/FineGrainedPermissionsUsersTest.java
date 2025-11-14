package org.keycloak.tests.admin.authz.fgap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;


@KeycloakIntegrationTest
public class FineGrainedPermissionsUsersTest extends AbstractPermissionTest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private void assertSearchMatchesCount(RealmResource realmResource, String search, Integer expectedCount) {
        Integer count = realmResource.users().count(search);
        assertThat(count, is(expectedCount));
        assertThat(realmResource.users().search(search, null, null), hasSize(count));
    }

    @Test
    public void countUsersWithGroupViewPermission() {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(true);
        assertThat(testRealmResource.users().count(), is(3));
    }

    @Test
    public void countUsersBySearchWithGroupViewPermission() {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(true);
        //search all
        assertSearchMatchesCount(testRealmResource, "user", 3);
        //search first name
        assertSearchMatchesCount(testRealmResource, "*FirstName*", 3);
        assertSearchMatchesCount(testRealmResource, "user2FirstName", 1);
        //search last name
        assertSearchMatchesCount(testRealmResource, "*LastName*", 3);
        assertSearchMatchesCount(testRealmResource, "user2LastName", 1);
        //search in email
        assertSearchMatchesCount(testRealmResource, "*@example.com*", 3);
        assertSearchMatchesCount(testRealmResource, "user1@example.com", 1);
        //search for something not existing
        assertSearchMatchesCount(testRealmResource, "notExisting", 0);
        //search for empty string
        assertSearchMatchesCount(testRealmResource, "", 3);
        //search not specified (defaults to simply /count)
        assertSearchMatchesCount(testRealmResource, null, 3);
    }

    @Test
    public void countUsersByFiltersWithGroupViewPermission() {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(true);
        //search username
        assertThat(testRealmResource.users().count(null, null, null, "user"), equalTo(testRealmResource.users().search("user", null, null, null, null, null).size()));
        assertThat(testRealmResource.users().count(null, null, null, "user"), is(3));
        assertThat(testRealmResource.users().count(null, null, null, "user1"), is(1));
        assertThat(testRealmResource.users().count(null, null, null, "notExisting"), is(0));
        assertThat(testRealmResource.users().count(null, null, null, ""), is(3));
        //search first name
        assertThat(testRealmResource.users().count(null, "FirstName", null, null), is(3));
        assertThat(testRealmResource.users().count(null, "user2FirstName", null, null), is(1));
        assertThat(testRealmResource.users().count(null, "notExisting", null, null), is(0));
        assertThat(testRealmResource.users().count(null, "", null, null), is(3));
        //search last name
        assertThat(testRealmResource.users().count("LastName", null, null, null), is(3));
        assertThat(testRealmResource.users().count("user2LastName", null, null, null), is(1));
        assertThat(testRealmResource.users().count("notExisting", null, null, null), is(0));
        assertThat(testRealmResource.users().count("", null, null, null), is(3));
        //search in email
        assertThat(testRealmResource.users().count(null, null, "@example.com", null), is(3));
        assertThat(testRealmResource.users().count(null, null, "user1@example.com", null), is(1));
        assertThat(testRealmResource.users().count(null, null, "user1@test.com", null), is(0));
        assertThat(testRealmResource.users().count(null, null, "", null), is(3));
        //search for combinations
        assertThat(testRealmResource.users().count("LastName", "FirstName", null, null), is(3));
        assertThat(testRealmResource.users().count("user1LastName", "FirstName", null, null), is(1));
        assertThat(testRealmResource.users().count("user1LastName", "", null, null), is(1));
        assertThat(testRealmResource.users().count("LastName", "", null, null), is(3));
        assertThat(testRealmResource.users().count("LastName", "", null, null), is(3));
        assertThat(testRealmResource.users().count(null, null, "@example.com", "user"), is(3));
        //search not specified (defaults to simply /count)
        assertThat(testRealmResource.users().count(null, null, null, null), is(3));
        assertThat(testRealmResource.users().count("", "", "", ""), is(3));
    }

    @Test
    public void countUsersWithNoViewPermission() {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(false);
        assertThat(testRealmResource.users().count(), is(0));
    }

    @Test
    public void countUsersBySearchWithNoViewPermission() {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(false);
        //search all
        assertSearchMatchesCount(testRealmResource, "user", 0);
        //search first name
        assertSearchMatchesCount(testRealmResource, "FirstName", 0);
        assertSearchMatchesCount(testRealmResource, "user2FirstName", 0);
        //search last name
        assertSearchMatchesCount(testRealmResource, "LastName", 0);
        assertSearchMatchesCount(testRealmResource, "user2LastName", 0);
        //search in email
        assertSearchMatchesCount(testRealmResource, "@example.com", 0);
        assertSearchMatchesCount(testRealmResource, "user1@example.com", 0);
        //search for something not existing
        assertSearchMatchesCount(testRealmResource, "notExisting", 0);
        //search for empty string
        assertSearchMatchesCount(testRealmResource, "", 0);
        //search not specified (defaults to simply /count)
        assertSearchMatchesCount(testRealmResource, null, 0);
    }

    @Test
    public void countUsersByFiltersWithNoViewPermission() {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(false);
        //search username
        assertThat(testRealmResource.users().count(null, null, null, "user"), is(0));
        assertThat(testRealmResource.users().count(null, null, null, "user1"), is(0));
        assertThat(testRealmResource.users().count(null, null, null, "notExisting"), is(0));
        assertThat(testRealmResource.users().count(null, null, null, ""), is(0));
        //search first name
        assertThat(testRealmResource.users().count(null, "FirstName", null, null), is(0));
        assertThat(testRealmResource.users().count(null, "user2FirstName", null, null), is(0));
        assertThat(testRealmResource.users().count(null, "notExisting", null, null), is(0));
        assertThat(testRealmResource.users().count(null, "", null, null), is(0));
        //search last name
        assertThat(testRealmResource.users().count("LastName", null, null, null), is(0));
        assertThat(testRealmResource.users().count("user2LastName", null, null, null), is(0));
        assertThat(testRealmResource.users().count("notExisting", null, null, null), is(0));
        assertThat(testRealmResource.users().count("", null, null, null), is(0));
        //search in email
        assertThat(testRealmResource.users().count(null, null, "@example.com", null), is(0));
        assertThat(testRealmResource.users().count(null, null, "user1@example.com", null), is(0));
        assertThat(testRealmResource.users().count(null, null, "user1@test.com", null), is(0));
        assertThat(testRealmResource.users().count(null, null, "", null), is(0));
        //search for combinations
        assertThat(testRealmResource.users().count("LastName", "FirstName", null, null), is(0));
        assertThat(testRealmResource.users().count("user1LastName", "FirstName", null, null), is(0));
        assertThat(testRealmResource.users().count("user1LastName", "", null, null), is(0));
        assertThat(testRealmResource.users().count("LastName", "", null, null), is(0));
        assertThat(testRealmResource.users().count("LastName", "", null, null), is(0));
        assertThat(testRealmResource.users().count(null, null, "@example.com", "user"), is(0));
        //search not specified (defaults to simply /count)
        assertThat(testRealmResource.users().count(null, null, null, null), is(0));
        assertThat(testRealmResource.users().count("", "", "", ""), is(0));
    }

    private RealmResource setupTestEnvironmentWithPermissions(boolean grp1ViewPermissions) {
        UserRepresentation user = UserConfigBuilder.create()
                .username("test-user")
                .password("password")
                .name("a", "b")
                .email("c@d.com")
                .enabled(true)
                .build();
        String testUserId = ApiUtil.getCreatedId(realm.admin().users().create(user));
        //assign 'query-users' role to test user
        ClientRepresentation clientRepresentation = realm.admin().clients().findByClientId("realm-management").get(0);
        String realmManagementId = clientRepresentation.getId();
        RoleRepresentation roleRepresentation = realm.admin().clients().get(realmManagementId).roles().get("query-users").toRepresentation();
        realm.admin().users().get(testUserId).roles().clientLevel(realmManagementId).add(Collections.singletonList(roleRepresentation));
        realm.cleanup().add(r -> r.users().get(testUserId).remove());

        //create test users and groups
        List<GroupRepresentation> groups = setupUsersInGroups();

        if (grp1ViewPermissions) {
            UserPolicyRepresentation userPolicy = createUserPolicy(realm, client, "allow-test-user", testUserId);
            createGroupPermission(groups.get(0), Set.of(VIEW), userPolicy);
        }

        Keycloak testUserClient = KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBaseUrl().toString())
                .realm(realm.getName())
                .username("test-user")
                .password("password")
                .clientId("admin-cli")
                .clientSecret("")
                .build();

        return testUserClient.realm(realm.getCreatedRepresentation().getRealm());
    }

    private List<GroupRepresentation> setupUsersInGroups() {
        //create two groups
        GroupRepresentation grp1 = createGroup("grp1");
        GroupRepresentation grp2 = createGroup("grp2");
        //create test users
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .enabled(true)
                .build();
        String user1Id = ApiUtil.getCreatedId(realm.admin().users().create(user1));
        realm.cleanup().add(r -> r.users().get(user1Id).remove());

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        String user2Id = ApiUtil.getCreatedId(realm.admin().users().create(user2));
        realm.cleanup().add(r -> r.users().get(user2Id).remove());

        UserRepresentation user3 = UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .enabled(true)
                .build();
        String user3Id = ApiUtil.getCreatedId(realm.admin().users().create(user3));
        realm.cleanup().add(r -> r.users().get(user3Id).remove());

        UserRepresentation user4 = UserConfigBuilder.create()
                .username("user4")
                .password("password")
                .name("user4FirstName", "user4LastName")
                .email("user4@example.com")
                .enabled(true)
                .build();
        String user4Id = ApiUtil.getCreatedId(realm.admin().users().create(user4));
        realm.cleanup().add(r -> r.users().get(user4Id).remove());

        //add users to groups
        realm.admin().users().get(user1Id).joinGroup(grp1.getId());
        realm.admin().users().get(user2Id).joinGroup(grp1.getId());
        realm.admin().users().get(user3Id).joinGroup(grp1.getId());
        realm.admin().users().get(user4Id).joinGroup(grp2.getId());

        List<GroupRepresentation> groups = new ArrayList<>();
        groups.add(grp1);
        groups.add(grp2);

        return groups;
    }

    private GroupRepresentation createGroup(String name) {
        GroupRepresentation grp = new GroupRepresentation();
        grp.setName(name);
        String groupId = ApiUtil.getCreatedId(realm.admin().groups().add(grp));
        grp.setId(groupId);
        realm.cleanup().add(r -> r.groups().group(groupId).remove());
        return grp;
    }
}
