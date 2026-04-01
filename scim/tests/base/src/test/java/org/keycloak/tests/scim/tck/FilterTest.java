package org.keycloak.tests.scim.tck;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.user.EnterpriseUser;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.scim.model.user.AbstractUserModelSchema.ANNOTATION_SCIM_SCHEMA_ATTRIBUTE;
import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprehensive integration tests for SCIM filter functionality covering all operators and complex combinations.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class FilterTest extends AbstractScimTest {

    private final List<String> userIdsToRemove = new ArrayList<>();
    private final List<String> groupIdsToRemove = new ArrayList<>();

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
        userIdsToRemove.clear();
    }

    @AfterEach
    public void onAfter() {
        userIdsToRemove.forEach(id -> realm.admin().users().delete(id).close());
        groupIdsToRemove.forEach(id -> realm.admin().groups().group(id).remove());
    }

    @Test
    public void testEqualFilter() {
        User bob = createUser("bob");
        createUser("alice");

        String filter = ResourceFilter.filter().eq("userName", bob.getUserName()).build();
        ListResponse<User> response = client.users().getAll(filter);
        assertSingleResult(response, bob.getUserName());

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
        User bob = createUser("bob");
        User alice = createUser("alice");

        String filter = ResourceFilter.filter().ne("userName", bob.getUserName()).build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));
        assertThat(response.getResources().stream().noneMatch(u -> u.getUserName().equals(bob.getUserName())), is(true));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(alice.getUserName())), is(true));
    }

    @Test
    public void testGreaterThanFilters() {
        User user = createUser("bob");

        // fetch the user representation to get the start date
        UserRepresentation userRep = realm.admin().users().get(user.getId()).toRepresentation();
        long createdDate = userRep.getCreatedTimestamp();
        String createdDateStr = Instant.ofEpochMilli(createdDate).toString();

        // filtering with greater-than on the user's createdTimestamp should return no user
        String filter = ResourceFilter.filter().gt("meta.created", createdDateStr).build();
        ListResponse<User> response = client.users().getAll(filter);
        assertNoResults(response);

        // filtering with greater-than-or-equal on the user's createdTimestamp should get the new user
        filter = ResourceFilter.filter().ge("meta.created", createdDateStr).build();
        response = client.users().getAll(filter);
        assertSingleResult(response, user.getUserName());

        // filtering with greater-than on the user's (createdTimestamp - 1) should now get the new user
        createdDateStr = Instant.ofEpochMilli(createdDate - 1).toString();
        filter = ResourceFilter.filter().gt("meta.created", createdDateStr).build();
        response = client.users().getAll(filter);
        assertSingleResult(response, user.getUserName());

        // fetching with greater-than-or-equal on the user's (createdTimestamp + 1) should yield no results
        createdDateStr = Instant.ofEpochMilli(createdDate + 1).toString();
        filter = ResourceFilter.filter().ge("meta.created", createdDateStr).build();
        response = client.users().getAll(filter);
        assertNoResults(response);
    }

    @Test
    public void testLessThanFilters() {
        User user = createUser("bob");
        final String expectedUsername = user.getUserName();

        // fetch the user representation to get the start date
        UserRepresentation userRep = realm.admin().users().get(user.getId()).toRepresentation();
        long createdDate = userRep.getCreatedTimestamp();
        String createdDateStr = Instant.ofEpochMilli(createdDate).toString();

        // filtering with less-than on the user's createdTimestamp should not return the new user
        String filter = ResourceFilter.filter().lt("meta.created", createdDateStr).build();
        ListResponse<User> response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getResources().stream().noneMatch(u -> u.getUserName().equals(expectedUsername)), is(true));

        // filtering with less-than-or-equal on the user's createdTimestamp should get the new user
        filter = ResourceFilter.filter().le("meta.created", createdDateStr).build();
        response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(expectedUsername)), is(true));

        // filtering with less-than on the user's (createdTimestamp + 1) should now get the new user
        createdDateStr = Instant.ofEpochMilli(createdDate + 1).toString();
        filter = ResourceFilter.filter().lt("meta.created", createdDateStr).build();
        response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(expectedUsername)), is(true));

        // fetching with less-than-or-equal on the user's (createdTimestamp - 1) should not get the new user
        createdDateStr = Instant.ofEpochMilli(createdDate - 1).toString();
        filter = ResourceFilter.filter().le("meta.created", createdDateStr).build();
        response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getResources().stream().noneMatch(u -> u.getUserName().equals(expectedUsername)), is(true));
    }

    @Test
    public void testStartsWithFilter() {
        User user = createUser("bob");
        createUser("alice_user");
        final String expectedUsername = user.getUserName();

        String filter = ResourceFilter.filter().sw("userName", "bob").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, user.getUserName());
    }

    @Test
    public void testContainsFilter() {
        User user = createUser("bob-the-builder");
        createUser("alice-not-builder");
        final String expectedUsername = user.getUserName();

        String filter = ResourceFilter.filter().co("userName", "the").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, expectedUsername);
    }

    @Test
    public void testEndsWithFilter() {
        User user = createUser("bob-the-builder");
        createUser("alice-the-tester");
        final String expectedUsername = user.getUserName();

        String filter = ResourceFilter.filter().ew("userName", "builder").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, expectedUsername);
    }

    @Test
    public void testPresentFilter() {
        User user = createUser("bob", "bob@keycloak.org");
        createUser("alice");
        final String expectedUsername = user.getUserName();

        String filter = ResourceFilter.filter().pr("emails.value").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, expectedUsername);
    }

    @Test
    public void testBooleanFilter() {
        User bob = createUser("bob", true);
        User alice = createUser("alice", false);

        String filter = ResourceFilter.filter().eq("active", "true").build();
        ListResponse<User> response = client.users().getAll(filter);
        assertSingleResult(response, bob.getUserName());
        assertThat(response.getResources().get(0).getActive(), is(true));

        filter = ResourceFilter.filter().eq("active", "false").build();
        response = client.users().getAll(filter);
        assertSingleResult(response, alice.getUserName());
        assertThat(response.getResources().get(0).getActive(), is(false));
    }

    @Test
    public void testLogicalAndFilter() {
        User user = createUser("bob", true);

        String filter = ResourceFilter.filter()
            .eq("userName", user.getUserName())
            .and()
            .ne("active", "false")
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, user.getUserName());
    }

    @Test
    public void testLogicalOrFilter() {
        User user1 = createUser("bob");
        final String user1Name = user1.getUserName();
        User user2 = createUser("alice");
        final String user2Name = user2.getUserName();
        User user3 = createUser("jane");
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
    public void testLogicalNotFilter() {
        User bob = createUser("bob");
        User alice = createUser("alice");

        String filter = ResourceFilter.filter()
            .not()
            .lparen()
            .eq("userName", bob.getUserName())
            .rparen()
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, alice.getUserName());
    }

    @Test
    public void testComplexAndOrCombination() {
        String prefix = "prefix-";

        User user1 = createUser(prefix + "bob", true);
        final String user1Name = user1.getUserName();
        User user2 = createUser(prefix + "alice", false);
        final String user2Name = user2.getUserName();
        createUser("other-jane", true);

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
        String prefix = "prefix-";

        createUser(prefix + "bob");
        User alice = createUser(prefix + "alice", false);

        String filter = ResourceFilter.filter()
            .sw("userName", prefix)
            .and()
            .not()
            .lparen()
            .eq("active", "true")
            .rparen()
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, alice.getUserName());
    }

    @Test
    public void testMultipleAndConditions() {
        String uniqueId = KeycloakModelUtils.generateId();
        User bob = createUser("bob-" + uniqueId, true);

        String filter = ResourceFilter.filter()
            .sw("userName", "bob-")
            .and()
            .co("userName", uniqueId.substring(0, 8))
            .and()
            .eq("active", "true")
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, bob.getUserName());
    }

    @Test
    public void testNestedParentheses() {
        String prefix1 = "prefix1-";
        String prefix2 = "prefix2-";

        User user1 = createUser(prefix1 + "bob", true);
        final String user1Name = user1.getUserName();
        User user2 = createUser(prefix2 + "alice", true);
        final String user2Name = user2.getUserName();
        createUser(prefix1 + "other-jane", false);

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

    @Test
    public void testInvalidFilters() {
        createUser("bob");

        // using a non-boolean value in a boolean expression should not be allowed
        ScimClientException ce = assertThrows(ScimClientException.class,
                () -> client.users().getAll(ResourceFilter.filter().eq("active", "abcde").build()));
        assertThat(ce.getError(), is(not(nullValue())));
        assertThat(ce.getError().getScimType(), is("invalidFilter"));

        // using an invalid operator should not be allowed
        ce = assertThrows(ScimClientException.class,
                () -> client.users().getAll("userName invalid \"invalid\""));
        assertThat(ce.getError(), is(not(nullValue())));
        assertThat(ce.getError().getScimType(), is("invalidFilter"));

        // using a filter with mismatched parentheses should not be allowed
        ce = assertThrows(ScimClientException.class,
                () -> client.users().getAll(ResourceFilter.filter().lparen().eq("userName", "invalid").build()));
        assertThat(ce.getError(), is(not(nullValue())));
        assertThat(ce.getError().getScimType(), is("invalidFilter"));

        // using a filter with an operator that is not valid for the type should not be allowed (e.g. greater-than on a boolean attribute)
        // or starts-with on a date attribute
        ce = assertThrows(ScimClientException.class,
                () -> client.users().getAll(ResourceFilter.filter().gt("active", "invalid").build()));
        assertThat(ce.getError(), is(not(nullValue())));
        assertThat(ce.getError().getScimType(), is("invalidFilter"));

        ce = assertThrows(ScimClientException.class,
                () -> client.users().getAll(ResourceFilter.filter().sw("meta.created", "2026-10").build()));
        assertThat(ce.getError(), is(not(nullValue())));
        assertThat(ce.getError().getScimType(), is("invalidFilter"));
    }

    // Tests with rich user objects

    @Test
    public void testFilterByGivenName() {
        User bob = createUser("bob", "Robert", "Johnson", "bob@keycloak.org", true);
        createUser("alice", "Alice", "Smith", "alice@keycloak.org", true);

        String filter = ResourceFilter.filter().eq("name.givenName", "Robert").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, bob.getUserName());
    }

    @Test
    public void testFilterByFamilyName() {
        User bob = createUser("bob", "Robert", "Johnson", "bob@keycloak.org", true);
        createUser("alice", "Alice", "Smith", "alice@keycloak.org", true);

        String filter = ResourceFilter.filter().eq("name.familyName", "Johnson").build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, bob.getUserName());
    }

    @Test
    public void testFilterByEmail() {
        User bob = createUser("bob", "Robert", "Johnson", "bob@keycloak.org", true);
        User alice = createUser("alice", "Alice", "Smith", "alice@keycloak.org", true);

        String filter = ResourceFilter.filter().eq("emails", "bob@keycloak.org").build();
        ListResponse<User> response = client.users().getAll(filter);
        assertSingleResult(response, bob.getUserName());

        filter = ResourceFilter.filter().eq("emails.value", "alice@keycloak.org").build();
        response = client.users().getAll(filter);
        assertSingleResult(response, alice.getUserName());

        filter = ResourceFilter.filter().co("emails", "keycloak").build();
        response = client.users().getAll(filter);
        assertThat(response.getTotalResults(), is(2));
    }

    @Test
    public void testFilterByUnknownAttribute() {
        final String userName = createUser("bob").getUserName();

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

        User bob = createUser("bob", "Robert", "Anderson", "bob@keycloak.org", false);
        User alice = createUser("alice", "Alice", "Anderson", "alice@keycloak.org", true);

        // filter: name.familyName eq "Anderson" AND active eq true  - should match alice but not bob
        String filter = ResourceFilter.filter()
                .eq("name.familyName", "Anderson")
                .and()
                .eq("active", "true")
                .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertSingleResult(response, alice.getUserName());
    }

    // Tests for complex attribute (value path) filters

    @Test
    public void testComplexAttributeFilterByName() {
        User bob = createUser("bob", "Robert", "Anderson", "bob@keycloak.org", true);
        createUser("alice", "Alice", "Smith", "alice@keycloak.org", true);

        // value path equivalent of: name.familyName eq "Anderson"
        String filter = "name[familyName eq \"Anderson\"]";
        ListResponse<User> response = client.users().getAll(filter);
        assertSingleResult(response, bob.getUserName());

        // value path equivalent of: name.givenName sw "Rob"
        filter = "name[givenName sw \"Rob\"]";
        response = client.users().getAll(filter);
        assertSingleResult(response, bob.getUserName());
    }

    @Test
    public void testComplexAttributeFilterWithLogicalOperators() {
        User bob = createUser("bob", "Robert", "Anderson", "bob@keycloak.org", true);
        User alice = createUser("alice", "Alice", "Anderson", "alice@keycloak.org", false);

        // value path with AND: name[familyName eq "Anderson" and givenName eq "Alice"]
        String filter = "name[familyName eq \"Anderson\" and givenName eq \"Alice\"]";
        ListResponse<User> response = client.users().getAll(filter);
        assertSingleResult(response, alice.getUserName());

        // value path with OR: name[givenName eq "Robert" or givenName eq "Alice"]
        filter = "name[givenName eq \"Robert\" or givenName eq \"Alice\"]";
        response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
    }

    @Test
    public void testComplexAttributeCombinedWithRegularFilter() {
        User bob = createUser("bob", "Robert", "Anderson", "bob@keycloak.org", true);
        createUser("alice", "Alice", "Anderson", "alice@keycloak.org", false);

        // value path combined with regular filter: name[familyName eq "Anderson"] and active eq true
        String filter = "name[familyName eq \"Anderson\"] and active eq true";
        ListResponse<User> response = client.users().getAll(filter);
        assertSingleResult(response, bob.getUserName());
    }

    // Tests for POST /.search endpoint

    @Test
    public void testPostSearchEndpointEq() {
        User user = createUser("bob");
        final String userName = user.getUserName();

        // Use search() which calls POST /.search
        String filter = ResourceFilter.filter().eq("userName", userName).build();
        ListResponse<User> response = client.users().search(filter);

        assertSingleResult(response, userName);
    }

    @Test
    public void testPostSearchEndpointComplex() {

        User bob = createUser("user-bob", "Robert", "Johnson", "bob@keycloak.org", true);
        User alice = createUser("user-alice", "Alice", "Smith", "alice@keycloak.org", false);

        // Complex filter: (userName sw prefix AND active eq true) OR (name.givenName eq "Alice")
        // Should match both users
        String filter = ResourceFilter.filter()
            .lparen()
            .sw("userName", "user-")
            .and()
            .eq("active", "true")
            .rparen()
            .or()
            .lparen()
            .eq("name.givenName", "Alice")
            .rparen()
            .build();
        ListResponse<User> response = client.users().search(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(bob.getUserName())), is(true));
        assertThat(response.getResources().stream().anyMatch(u -> u.getUserName().equals(alice.getUserName())), is(true));
    }

    @Test
    public void testPostSearchWithEmailFilter() {

        createUser("user-bob", "Robert", "Johnson", "bob@keycloak.org", true);
        User alice = createUser("user-alice", "Alice", "Smith", "alice@keycloak-corp.org", true);

        // Test POST search with email contains filter
        String filter = ResourceFilter.filter().co("emails", "corp").build();
        ListResponse<User> response = client.users().search(filter);

        assertSingleResult(response, alice.getUserName());
    }

    // Tests for enterprise user searches

    @Test
    public void testSearchEnterpriseUsers() {
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();

        // update user profile configuration
        configuration.addOrReplaceAttribute(new UPAttribute("department", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".department")));
        configuration.addOrReplaceAttribute(new UPAttribute("division", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".division")));
        configuration.addOrReplaceAttribute(new UPAttribute("costCenter", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".costCenter")));
        configuration.addOrReplaceAttribute(new UPAttribute("employeeNumber", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".employeeNumber")));
        configuration.addOrReplaceAttribute(new UPAttribute("organization", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".organization")));
        configuration.addOrReplaceAttribute(new UPAttribute("manager", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".manager.value")));
        configuration.addOrReplaceAttribute(new UPAttribute("managerName", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".manager.displayName")));
        realm.admin().users().userProfile().update(configuration);

        User user1 = createEnterpriseUser("user1", "Engineering", "E1234", "Bruce Wayne");
        User user2 = createEnterpriseUser("user2", "QE", "E7763", "Lucius Fox");

        // now search by the various enterprise user attributes to verify that the filtering works for them
        String filter = ResourceFilter.filter().eq(ENTERPRISE_USER_SCHEMA + ":department", "QE").build();
        ListResponse<User> response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(user2.getUserName()));

        // search by cost center using starts-with filter
        filter = ResourceFilter.filter().sw(ENTERPRISE_USER_SCHEMA + ":costCenter", "AMER").build();
        response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
        assertThat(response.getResources().stream().map(User::getUserName).toList(), containsInAnyOrder(user1.getUserName(), user2.getUserName()));

        // search by organization using contains filter
        filter = ResourceFilter.filter().co(ENTERPRISE_USER_SCHEMA + ":organization", "ganiza").build();
        response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
        assertThat(response.getResources().stream().map(User::getUserName).toList(), containsInAnyOrder(user1.getUserName(), user2.getUserName()));

        // search by manager's display name using ends-with filter
        filter = ResourceFilter.filter().ew(ENTERPRISE_USER_SCHEMA + ":manager.displayName", "Technical Manager").build();
        response = client.users().getAll(filter);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(2));
        assertThat(response.getResources().stream().map(User::getUserName).toList(), containsInAnyOrder(user1.getUserName(), user2.getUserName()));

        // search by manager's name
        filter = ResourceFilter.filter().eq(ENTERPRISE_USER_SCHEMA + ":manager", "Bruce Wayne").build();
        response = client.users().getAll(filter);
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(user1.getUserName()));
    }

    private void assertNoResults(ListResponse<User> response) {
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(0));
    }

    private void assertSingleResult(ListResponse<User> response, String expectedUsername) {
        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(expectedUsername));

    }

    public User createUser(String username) {
        return this.createUser(username, null);
    }

    public User createUser(String username, String email) {
        return this.createUser(username, null, null, email, true);
    }

    public User createUser(String username, boolean active) {
        return this.createUser(username, null, null, null, active);
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

    private User createEnterpriseUser(String username, String department, String employeeNumber, String managerName) {
        User user = new User();
        user.setUserName(username);
        user.setEmail(username + "@keycloak.org");
        user.setActive(true);

        EnterpriseUser enterpriseUser = new EnterpriseUser();
        enterpriseUser.setDepartment(department);
        enterpriseUser.setEmployeeNumber(employeeNumber);
        user.setEnterpriseUser(enterpriseUser);
        enterpriseUser.setCostCenter("AMER-4015");
        enterpriseUser.setDivision("Open Source");
        enterpriseUser.setOrganization("Organization");
        user.setEnterpriseUser(enterpriseUser);

        EnterpriseUser.Manager manager = new EnterpriseUser.Manager();
        manager.setValue(managerName);
        manager.setDisplayName(managerName + ", Technical Manager");
        enterpriseUser.setManager(manager);

        user = client.users().create(user);
        userIdsToRemove.add(user.getId());
        return user;
    }

    @Test
    public void testFilterByMetaTimestamps() {
        Instant before = Instant.now();

        User user = createUser("bob");
        final String userName = user.getUserName();

        Instant after = Instant.now();

        // filter by meta.created gt <before> — should include the user
        String filter = ResourceFilter.filter()
            .gt("meta.created", before.toString())
            .and()
            .eq("userName", userName)
            .build();
        ListResponse<User> response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(userName));

        // filter by meta.lastModified lt <after> — should include the user
        filter = ResourceFilter.filter()
            .lt("meta.lastModified", after.toString())
            .and()
            .eq("userName", userName)
            .build();
        response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getUserName(), is(userName));

        // filter by meta.created gt <after> — should NOT include the user
        filter = ResourceFilter.filter()
            .gt("meta.created", after.toString())
            .and()
            .eq("userName", userName)
            .build();
        response = client.users().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(0));
    }

    @Test
    public void testFilterGroupsByMetaTimestamps() {
        Instant before = Instant.now();

        Group group = new Group();
        group.setDisplayName(KeycloakModelUtils.generateId());
        group = client.groups().create(group);
        groupIdsToRemove.add(group.getId());
        String displayName = group.getDisplayName();

        Instant after = Instant.now();

        // filter by meta.created gt <before> — should include the group
        String filter = ResourceFilter.filter()
            .gt("meta.created", before.toString())
            .and()
            .eq("displayName", displayName)
            .build();
        ListResponse<Group> response = client.groups().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getDisplayName(), is(displayName));

        // filter by meta.lastModified lt <after> — should include the group
        filter = ResourceFilter.filter()
            .lt("meta.lastModified", after.toString())
            .and()
            .eq("displayName", displayName)
            .build();
        response = client.groups().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getDisplayName(), is(displayName));

        // filter by meta.created gt <after> — should NOT include the group
        filter = ResourceFilter.filter()
            .gt("meta.created", after.toString())
            .and()
            .eq("displayName", displayName)
            .build();
        response = client.groups().getAll(filter);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTotalResults(), is(0));
    }
}
