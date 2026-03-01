package org.keycloak.tests.scim.tck;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.common.Email;
import org.keycloak.scim.resource.common.Name;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Comprehensive integration tests for SCIM filter functionality covering all operators and complex combinations.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class FilterTest extends AbstractScimTest {

    @BeforeEach
    public void onBefore() {
        UPConfig upConfig = realm.admin().users().userProfile().getConfiguration();
        upConfig.getAttribute(UserModel.FIRST_NAME).setRequired(null);
        upConfig.getAttribute(UserModel.LAST_NAME).setRequired(null);
        upConfig.getAttribute(UserModel.EMAIL).setRequired(null);
        Iterator<UPAttribute> iterator = upConfig.getAttributes().iterator();
        while (iterator.hasNext()) {
            UPAttribute attribute = iterator.next();
            if (Set.of(UserModel.USERNAME, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.EMAIL).contains(attribute.getName())) {
                continue;
            }
            iterator.remove();
        }
        realm.admin().users().userProfile().update(upConfig);
    }

    @Test
    public void testEqualityFilter() {
        User john = new User();
        john.setUserName("john_" + KeycloakModelUtils.generateId());
        john = client.users().create(john);

        User jane = new User();
        jane.setUserName("jane_" + KeycloakModelUtils.generateId());
        jane = client.users().create(jane);

        String filter = ResourceFilter.filter().eq("userName", john.getUserName()).build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(john.getUserName()));

        // create a couple of groups to test group filtering as well
        Group groupA = new Group();
        groupA.setDisplayName("GroupA");
        groupA = client.groups().create(groupA);
        assertNotNull(groupA);

        Group groupB = new Group();
        groupB.setDisplayName("GroupB");
        groupB = client.groups().create(groupB);
        assertNotNull(groupB);

        String groupFilter = ResourceFilter.filter().eq("displayName", groupA.getDisplayName()).build();
        ListResponse<Group> groupResponse = client.groups().getAll(groupFilter);
        assertThat(groupResponse, is(not(nullValue())));
        assertThat(groupResponse.getTotalResults(), is(1));
        assertThat(groupResponse.getResources().get(0).getDisplayName(), is(groupA.getDisplayName()));
    }

    @Test
    public void testNotEqualFilter() {
        User user1 = new User();
        user1.setUserName("testne1_" + KeycloakModelUtils.generateId());
        user1 = client.users().create(user1);
        final String user1Name = user1.getUserName();

        User user2 = new User();
        user2.setUserName("testne2_" + KeycloakModelUtils.generateId());
        user2 = client.users().create(user2);
        final String user2Name = user2.getUserName();

        String filter = ResourceFilter.filter().ne("userName", user1Name).build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().noneMatch(u -> u.getUserName().equals(user1Name)), is(true));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user2Name)), is(true));
    }

    @Test
    public void testStartsWithFilter() {
        User user = new User();
        user.setUserName("testswuser_" + KeycloakModelUtils.generateId());
        user = client.users().create(user);
        final String expectedUsername = user.getUserName();

        String filter = ResourceFilter.filter().sw("userName", "testswuser_").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(expectedUsername)), is(true));
    }

    @Test
    public void testContainsFilter() {
        User user = new User();
        user.setUserName("test_contains_xyz_" + KeycloakModelUtils.generateId());
        user = client.users().create(user);
        final String expectedUsername = user.getUserName();

        String filter = ResourceFilter.filter().co("userName", "contains_xyz").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(expectedUsername)), is(true));
    }

    @Test
    public void testEndsWithFilter() {
        String suffix = "_endstest_" + KeycloakModelUtils.generateId();
        User user = new User();
        user.setUserName("user" + suffix);
        user = client.users().create(user);
        final String expectedUsername = user.getUserName();

        String filter = ResourceFilter.filter().ew("userName", suffix).build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(expectedUsername)), is(true));
    }

    @Test
    public void testPresentFilter() {
        User user = new User();
        user.setUserName("test_pr_" + KeycloakModelUtils.generateId());
        user = client.users().create(user);
        final String expectedUsername = user.getUserName();

        String filter = ResourceFilter.filter().pr("userName").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(expectedUsername)), is(true));
    }

    @Test
    public void testBooleanFilter() {
        User activeUser = new User();
        activeUser.setUserName("activetrue_" + KeycloakModelUtils.generateId());
        activeUser.setActive(true);
        activeUser = client.users().create(activeUser);

        User inactiveUser = new User();
        inactiveUser.setUserName("activefalse_" + KeycloakModelUtils.generateId());
        inactiveUser.setActive(false);
        inactiveUser = client.users().create(inactiveUser);

        String filter = ResourceFilter.filter().eq("active", "true").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getResources().stream().allMatch(User::getActive), is(true));

        filter = ResourceFilter.filter().eq("active", "false").build();
        response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getResources().stream().noneMatch(User::getActive), is(true));
    }

    @Test
    public void testLogicalAndFilter() {
        User user = new User();
        user.setUserName("testuser_and_" + KeycloakModelUtils.generateId());
        user.setActive(true);
        user = client.users().create(user);

        String filter = ResourceFilter.filter()
            .eq("userName", user.getUserName())
            .and()
            .eq("active", "true")
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(user.getUserName()));
    }

    @Test
    public void testLogicalOrFilter() {
        User user1 = new User();
        user1.setUserName("testor1_" + KeycloakModelUtils.generateId());
        user1 = client.users().create(user1);
        final String user1Name = user1.getUserName();

        User user2 = new User();
        user2.setUserName("testor2_" + KeycloakModelUtils.generateId());
        user2 = client.users().create(user2);
        final String user2Name = user2.getUserName();

        User user3 = new User();
        user3.setUserName("testor3_" + KeycloakModelUtils.generateId());
        user3 = client.users().create(user3);
        final String user3Name = user3.getUserName();

        String filter = ResourceFilter.filter()
            .eq("userName", user1Name)
            .or()
            .eq("userName", user2Name)
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user1Name)), is(true));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user2Name)), is(true));
        assertThat(response.getResources().stream().noneMatch(u -> u.getUserName().equals(user3Name)), is(true));
    }

    @Test
    public void testNotOperator() {
        User user = new User();
        user.setUserName("testnot_" + KeycloakModelUtils.generateId());
        user = client.users().create(user);
        final String userName = user.getUserName();

        String filter = ResourceFilter.filter()
            .not()
            .lparen()
            .eq("userName", userName)
            .rparen()
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getResources().stream().noneMatch(u -> u.getUserName().equals(userName)), is(true));
    }

    @Test
    public void testComplexAndOrCombination() {
        String prefix = "complex_" + KeycloakModelUtils.generateId() + "_";

        User user1 = new User();
        user1.setUserName(prefix + "active");
        user1.setActive(true);
        user1 = client.users().create(user1);
        final String user1Name = user1.getUserName();

        User user2 = new User();
        user2.setUserName(prefix + "inactive");
        user2.setActive(false);
        user2 = client.users().create(user2);
        final String user2Name = user2.getUserName();

        User user3 = new User();
        user3.setUserName("other_" + KeycloakModelUtils.generateId());
        user3.setActive(true);
        user3 = client.users().create(user3);

        String filter = ResourceFilter.filter()
            .lparen()
            .sw("userName", prefix)
            .and()
            .eq("active", "true")
            .rparen()
            .or()
            .lparen()
            .sw("userName", prefix)
            .and()
            .eq("active", "false")
            .rparen()
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user1Name)), is(true));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user2Name)), is(true));
    }

    @Test
    public void testNotWithAndCombination() {
        String prefix = "notand_" + KeycloakModelUtils.generateId() + "_";

        User user1 = new User();
        user1.setUserName(prefix + "user1");
        user1.setActive(true);
        user1 = client.users().create(user1);

        User user2 = new User();
        user2.setUserName(prefix + "user2");
        user2.setActive(false);
        user2 = client.users().create(user2);

        String filter = ResourceFilter.filter()
            .sw("userName", prefix)
            .and()
            .not()
            .lparen()
            .eq("active", "true")
            .rparen()
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(user2.getUserName()));
    }

    @Test
    public void testMultipleAndConditions() {
        String uniqueId = KeycloakModelUtils.generateId();
        User user = new User();
        user.setUserName("multiand_" + uniqueId);
        user.setActive(true);
        user = client.users().create(user);
        final String userName = user.getUserName();

        String filter = ResourceFilter.filter()
            .sw("userName", "multiand_")
            .and()
            .co("userName", uniqueId.substring(0, 8))
            .and()
            .eq("active", "true")
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(userName)), is(true));
    }

    @Test
    public void testNestedParentheses() {
        String prefix1 = "nested1_" + KeycloakModelUtils.generateId() + "_";
        String prefix2 = "nested2_" + KeycloakModelUtils.generateId() + "_";

        User user1 = new User();
        user1.setUserName(prefix1 + "active");
        user1.setActive(true);
        user1 = client.users().create(user1);
        final String user1Name = user1.getUserName();

        User user2 = new User();
        user2.setUserName(prefix2 + "active");
        user2.setActive(true);
        user2 = client.users().create(user2);
        final String user2Name = user2.getUserName();

        User user3 = new User();
        user3.setUserName(prefix1 + "inactive");
        user3.setActive(false);
        user3 = client.users().create(user3);

        String filter = ResourceFilter.filter()
            .lparen()
            .sw("userName", prefix1)
            .or()
            .sw("userName", prefix2)
            .rparen()
            .and()
            .eq("active", "true")
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user1Name)), is(true));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user2Name)), is(true));
    }

    // Tests with rich user objects

    @Test
    public void testFilterByGivenName() {
        User user = new User();
        user.setUserName("nametest_" + KeycloakModelUtils.generateId());
        Name name = new Name();
        name.setGivenName("Alice");
        name.setFamilyName("Smith");
        user.setName(name);
        user = client.users().create(user);
        final String userName = user.getUserName();

        String filter = ResourceFilter.filter().eq("name.givenName", "Alice").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(userName)), is(true));
    }

    @Test
    public void testFilterByFamilyName() {
        User user = new User();
        user.setUserName("familytest_" + KeycloakModelUtils.generateId());
        Name name = new Name();
        name.setGivenName("Bob");
        name.setFamilyName("Johnson");
        user.setName(name);
        user = client.users().create(user);
        final String userName = user.getUserName();

        String filter = ResourceFilter.filter().eq("name.familyName", "Johnson").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(userName)), is(true));
    }

    @Test
    public void testFilterByEmail() {
        String emailValue = "test." + KeycloakModelUtils.generateId() + "@example.com";
        User user = new User();
        user.setUserName("emailtest_" + KeycloakModelUtils.generateId());
        Email email = new Email();
        email.setValue(emailValue);
        email.setPrimary(true);
        user.setEmails(List.of(email));
        user = client.users().create(user);
        final String userName = user.getUserName();

        String filter = ResourceFilter.filter().eq("emails[0].value", emailValue).build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(userName)), is(true));
    }

    @Test
    public void testFilterByUnknownAttribute() {
        String emailValue = "test@example.com";
        User user = new User();
        user.setUserName("testuser");
        Email email = new Email();
        email.setValue(emailValue);
        email.setPrimary(true);
        user.setEmails(List.of(email));
        user = client.users().create(user);
        final String userName = user.getUserName();

        // using a filter with an unknown attribute should not match any users if combined with 'and' since the unknown attribute condition cannot be satisfied
        String filter = ResourceFilter.filter().eq("userName", userName).and().pr("unkonwn").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(0));

        // using a filter with 'or' where one side has an unknown attribute should still return the user since the other side matches
        filter = ResourceFilter.filter().eq("userName", userName).or().pr("unkonwn").build();
        response = client.users().getAll(filter);
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(userName));

        // using 'not' with an unknown attribute should not match any users since the condition inside 'not' cannot be satisfied
        filter = ResourceFilter.filter().not().pr("unkonwn").build();
        response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(0));
    }


    @Test
    public void testComplexFilterWithNames() {
        String uniqueId = KeycloakModelUtils.generateId();

        User alice = new User();
        alice.setUserName("alice_" + uniqueId);
        Name aliceName = new Name();
        aliceName.setGivenName("Alice");
        aliceName.setFamilyName("Anderson");
        alice.setName(aliceName);
        alice.setActive(true);
        alice = client.users().create(alice);
        final String aliceName1 = alice.getUserName();

        User bob = new User();
        bob.setUserName("bob_" + uniqueId);
        Name bobName = new Name();
        bobName.setGivenName("Bob");
        bobName.setFamilyName("Anderson");
        bob.setName(bobName);
        bob.setActive(false);
        bob = client.users().create(bob);

        // Filter: name.familyName eq "Anderson" AND active eq true
        // Should match Alice but not Bob
        String filter = ResourceFilter.filter()
            .eq("name.familyName", "Anderson")
            .and()
            .eq("active", "true")
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(aliceName1)), is(true));
    }

    // Tests for POST /.search endpoint

    @Test
    public void testPostSearchEndpointEq() {
        User user = new User();
        user.setUserName("postsearch_" + KeycloakModelUtils.generateId());
        user = client.users().create(user);
        final String userName = user.getUserName();

        // Use search() which calls POST /.search
        String filter = ResourceFilter.filter().eq("userName", userName).build();
        ListResponse<User> response = client.users().search(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(userName));
    }

    @Test
    public void testPostSearchEndpointComplex() {
        String prefix = "postcomplex_" + KeycloakModelUtils.generateId() + "_";

        User user1 = new User();
        user1.setUserName(prefix + "user1");
        Name name1 = new Name();
        name1.setGivenName("Charlie");
        name1.setFamilyName("Davis");
        user1.setName(name1);
        user1.setActive(true);
        user1 = client.users().create(user1);
        final String user1Name = user1.getUserName();

        User user2 = new User();
        user2.setUserName(prefix + "user2");
        Name name2 = new Name();
        name2.setGivenName("David");
        name2.setFamilyName("Davis");
        user2.setName(name2);
        user2.setActive(false);
        user2 = client.users().create(user2);
        final String user2Name = user2.getUserName();

        // Complex filter: (userName sw prefix AND active eq true) OR (name.givenName eq "David")
        // Should match both users
        String filter = ResourceFilter.filter()
            .lparen()
            .sw("userName", prefix)
            .and()
            .eq("active", "true")
            .rparen()
            .or()
            .lparen()
            .eq("name.givenName", "David")
            .rparen()
            .build();
        ListResponse<User> response = client.users().search(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user1Name)), is(true));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(user2Name)), is(true));
    }

    @Test
    public void testPostSearchWithEmailFilter() {
        String emailValue = "postemailtest." + KeycloakModelUtils.generateId() + "@example.com";
        User user = new User();
        user.setUserName("postemailuser_" + KeycloakModelUtils.generateId());
        Email email = new Email();
        email.setValue(emailValue);
        email.setPrimary(true);
        user.setEmails(List.of(email));
        user = client.users().create(user);
        final String userName = user.getUserName();

        // Test POST search with email contains filter
        String filter = ResourceFilter.filter().co("emails[0].value", "postemailtest").build();
        ListResponse<User> response = client.users().search(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(userName)), is(true));
    }
}
