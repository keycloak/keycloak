package org.keycloak.tests.scim.tck;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class PaginationTest extends AbstractScimTest {

    private final List<String> userIdsToRemove = new ArrayList<>();
    private final List<String> groupIdsToRemove = new ArrayList<>();

    @AfterEach
    public void onAfter() {
        userIdsToRemove.forEach(id -> realm.admin().users().delete(id).close());
        userIdsToRemove.clear();
        groupIdsToRemove.forEach(id -> realm.admin().groups().group(id).remove());
        groupIdsToRemove.clear();
    }

    @Test
    public void testUserSearchWithPaginationAndNoFilter() {

        // create a few users to test pagination
        for (int i = 0; i < 10; i++) {
            createUser("user-" + i, "User " + i, "Test", "user" + i + "@keycloak.org", true);
        }

        // using a page size of 3, we should have 4 pages of results (3 + 3 + 3 + 1)
        int pageSize = 3;
        ListResponse<User> response = client.users().search(null, 1, pageSize);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(3));
        assertThat(response.getStartIndex(), is(1));

        // first page should have users 0, 1, 2
        for (int i = 0; i <= 2; i++) {
            assertThat(response.getResources().get(i).getUserName(), is("user-" + i));
        }

        // fetch the second page
        response = client.users().search(null, 4, pageSize);
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(3));
        assertThat(response.getStartIndex(), is(4));

        // second page should have users 3, 4, 5
        for (int i = 3; i <= 5; i++) {
            assertThat(response.getResources().get(i - pageSize).getUserName(), is("user-" + i));
        }

        // fetch the third page
        response = client.users().search(null, 7, pageSize);
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(3));
        assertThat(response.getStartIndex(), is(7));

        // third page should have users 6, 7, 8
        for (int i = 6; i <= 8; i++) {
            assertThat(response.getResources().get(i - pageSize * 2).getUserName(), is("user-" + i));
        }

        // fetch the fourth and final page
        response = client.users().search(null, 10, pageSize);
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(1));
        assertThat(response.getStartIndex(), is(10));

        assertThat(response.getResources().get(0).getUserName(), is("user-9"));
    }

    @Test
    public void testUserSearchWithPaginationAndFilter() {
        // create a few users to test pagination
        for (int i = 0; i < 10; i++) {
            createUser("user-" + i, "User " + i, "Smith", "user" + i + "@keycloak.org", true);
        }
        for (int i = 10; i < 20; i++) {
            createUser("user-" + i, "User " + i, "Johnson", "user" + i + "@keycloak.org", true);
        }

        // filter by family name and page size of 4, we should have 3 pages of results (4 + 4 + 2)
        int pageSize = 4;
        String filter = ResourceFilter.filter().eq("name.familyName", "Smith").build();
        ListResponse<User> response = client.users().search(filter, 1, pageSize);
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(4));
        assertThat(response.getStartIndex(), is(1));

        // first page should have users 0, 1, 2, 3
        for (int i = 0; i <= 3; i++) {
            assertThat(response.getResources().get(i).getUserName(), is("user-" + i));
        }

        // fetch the second page
        response = client.users().search(filter, 5, pageSize);
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(4));
        assertThat(response.getStartIndex(), is(5));

        // second page should have users 4, 5, 6, 7
        for (int i = 4; i <= 7; i++) {
            assertThat(response.getResources().get(i - pageSize).getUserName(), is("user-" + i));
        }

        // fetch the last page
        response = client.users().search(filter, 9, pageSize);
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(2));
        assertThat(response.getStartIndex(), is(9));

        // last page should have users 8, 9
        assertThat(response.getResources().get(0).getUserName(), is("user-8"));
        assertThat(response.getResources().get(1).getUserName(), is("user-9"));
    }

    @Test
    public void testUserSearchRespectsMaxResults() {
        // create more users so we have more than the default max results of 100
        for (int i = 0; i < 120; i++) {
            createUser("user-" + i, "User " + i, "Test", "user" + i + "@keycloak.org", true);
        }

        // if we send an unbounded request (count = null), we should get back only the default max results of 100
        ListResponse<User> response = client.users().getAll();
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(120));
        assertThat(response.getItemsPerPage(), is(100));
        assertThat(response.getStartIndex(), is(1));

        // if we send a request with a count higher than the default max results, we should still get back only the default max results of 100
        response = client.users().search(null, 1, 150);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(120));
        assertThat(response.getItemsPerPage(), is(100));
        assertThat(response.getStartIndex(), is(1));
    }

    @Test
    public void testGroupSearchWithPaginationAndNoFilter() {
        // create a few test groups
        for (int i = 0; i < 10; i++) {
            createGroup("group-" + i);
        }

        // using a page size of 6, we should have 2 pages of results (6 + 4)
        int pageSize = 6;
        ListResponse<Group> response = client.groups().search(null, 1, pageSize);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(6));
        assertThat(response.getStartIndex(), is(1));

        // first page should have groups 0, 1, 2, 3, 4, 5
        for (int i = 0; i < 6; i++) {
            assertThat(response.getResources().get(i).getDisplayName(), is("group-" + i));
        }

        // fetch the second page
        response = client.groups().search(null, 7, pageSize);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(4));
        assertThat(response.getStartIndex(), is(7));

        // second page should have groups 6, 7, 8, 9
        for (int i = 6; i < 10; i++) {
            assertThat(response.getResources().get(i - pageSize).getDisplayName(), is("group-" + i));
        }
    }

    @Test
    public void testGroupSearchWithPaginationAndFilter() {
        // create a few test groups
        for (int i = 0; i < 10; i++) {
            createGroup("group-" + i);
        }
        for (int i = 10; i < 20; i++) {
            createGroup("team-" + i);
        }

        // filter by display name and page size of 5, we should have 2 pages of results (5 + 5)
        int pageSize = 5;
        String filter = ResourceFilter.filter().sw("displayName", "team-").build();
        ListResponse<Group> response = client.groups().search(filter, 1, pageSize);
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(5));
        assertThat(response.getStartIndex(), is(1));

        // first page should have groups 10, 11, 12, 13, 14
        for (int i = 10; i <= 14; i++) {
            assertThat(response.getResources().get(i - 10).getDisplayName(), is("team-" + i));
        }

        // fetch the second page
        response = client.groups().search(filter, 6, pageSize);
        assertThat(response.getTotalResults(), is(10));
        assertThat(response.getItemsPerPage(), is(5));
        assertThat(response.getStartIndex(), is(6));

        // second page should have groups 15, 16, 17, 18, 19
        for (int i = 15; i <= 19; i++) {
            assertThat(response.getResources().get(i - 15).getDisplayName(), is("team-" + i));
        }
    }

    @Test
    public void testGroupSearchRespectsMaxResults() {
        // create more groups than the default max results of 100
        for (int i = 0; i < 120; i++) {
            createGroup("group-" + i);
        }

        // if we send an unbounded request (count = null), we should get back only the default max results of 100
        ListResponse<Group> response = client.groups().search(null, null, null);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(120));
        assertThat(response.getItemsPerPage(), is(100));
        assertThat(response.getStartIndex(), is(1));

        // if we send a request with a count higher than the default max results, we should still get back only the default max results of 100
        response = client.groups().search(null, 1, 150);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(120));
        assertThat(response.getItemsPerPage(), is(100));
        assertThat(response.getStartIndex(), is(1));
    }

    private User createUser(String username, String givenName, String familyName, String email, boolean active) {
        User user = new User();
        user.setUserName(username);
        user.setEmail(email);
        user.setActive(active);
        user.setFirstName(givenName);
        user.setLastName(familyName);
        user = client.users().create(user);
        userIdsToRemove.add(user.getId());
        return user;
    }

    private Group createGroup(String name) {
        Group group = new Group();
        group.setDisplayName(name);
        group = client.groups().create(group);
        groupIdsToRemove.add(group.getId());
        return group;
    }
}
