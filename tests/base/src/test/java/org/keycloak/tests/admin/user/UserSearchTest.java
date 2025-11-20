package org.keycloak.tests.admin.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.userprofile.DefaultAttributes;
import org.keycloak.userprofile.validator.UsernameProhibitedCharactersValidator;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class UserSearchTest extends AbstractUserTest {

    @Test
    public void countUsersByEnabledFilter() {

        // create 2 enabled and 1 disabled user
        UserRepresentation enabledUser1 = new UserRepresentation();
        enabledUser1.setUsername("enabled1");
        enabledUser1.setEmail("enabled1@enabledfilter.com");
        enabledUser1.setEnabled(true);
        createUser(enabledUser1);

        UserRepresentation enabledUser2 = new UserRepresentation();
        enabledUser2.setUsername("enabled2");
        enabledUser2.setEmail("enabled2@enabledfilter.com");
        enabledUser2.setEnabled(true);
        createUser(enabledUser2);

        UserRepresentation disabledUser1 = new UserRepresentation();
        disabledUser1.setUsername("disabled1");
        disabledUser1.setEmail("disabled1@enabledfilter.com");
        disabledUser1.setEnabled(false);
        createUser(disabledUser1);

        Boolean enabled = true;
        Boolean disabled = false;

        // count all users with @enabledfilter.com
        assertThat(managedRealm.admin().users().count(null, null, null, "@enabledfilter.com", null, null, null, null), is(3));

        // count users that are enabled and have username enabled1
        assertThat(managedRealm.admin().users().count(null, null, null, "@enabledfilter.com", null, "enabled1", enabled, null),is(1));

        // count users that are disabled
        assertThat(managedRealm.admin().users().count(null, null, null, "@enabledfilter.com", null, null, disabled, null), is(1));

        // count users that are enabled
        assertThat(managedRealm.admin().users().count(null, null, null, "@enabledfilter.com", null, null, enabled, null), is(2));
    }

    @Test
    public void searchByEmail() {
        createUsers();

        List<UserRepresentation> users = managedRealm.admin().users().search(null, null, null, "user1@localhost", null, null);
        assertEquals(1, users.size());

        users = managedRealm.admin().users().search(null, null, null, "@localhost", null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void searchByEmailExactMatch() {
        createUsers();
        List<UserRepresentation> users = managedRealm.admin().users().searchByEmail("user1@localhost", true);
        assertEquals(1, users.size());

        users = managedRealm.admin().users().search("@localhost", true);
        assertEquals(0, users.size());
    }

    @Test
    public void searchByUsername() {
        createUsers();

        List<UserRepresentation> users = managedRealm.admin().users().search("username1", null, null, null, null, null);
        assertEquals(1, users.size());

        users = managedRealm.admin().users().search("user", null, null, null, null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void searchByAttribute() {
        createUsers();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("test", "test1");
        List<UserRepresentation> users = managedRealm.admin().users().searchByAttributes(mapToSearchQuery(attributes));
        assertEquals(1, users.size());

        attributes.clear();
        attributes.put("attr", "common");

        users = managedRealm.admin().users().searchByAttributes(mapToSearchQuery(attributes));
        assertEquals(9, users.size());

        attributes.clear();
        attributes.put("x", "common");
        users = managedRealm.admin().users().searchByAttributes(mapToSearchQuery(attributes));
        assertEquals(0, users.size());
    }

    @Test
    public void searchByMultipleAttributes() {
        createUsers();

        List<UserRepresentation> users = managedRealm.admin().users().searchByAttributes(mapToSearchQuery(Map.of("username", "user", "test", "test1", "attr", "common", "test1", "test1")));
        assertThat(users, hasSize(1));

        //custom user attribute should not use wildcard search by default
        users = managedRealm.admin().users().searchByAttributes(mapToSearchQuery(Map.of("username", "user", "test", "est", "attr", "mm", "test1", "test1")));
        assertThat(users, hasSize(0));

        //custom user attribute should use wildcard
        users = managedRealm.admin().users().searchByAttributes(mapToSearchQuery(Map.of("username", "user", "test", "est", "attr", "mm", "test1", "test1")), false);
        assertThat(users, hasSize(1));

        //with exact=true the user shouldn't be returned
        users = managedRealm.admin().users().searchByAttributes(mapToSearchQuery(Map.of("test", "est", "attr", "mm", "test1", "test1")), Boolean.TRUE);
        assertThat(users, hasSize(0));
    }

    @Test
    public void searchByAttributesWithPagination() {
        createUsers();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("attr", "Common");
        for (int i = 1; i < 10; i++) {
            List<UserRepresentation> users = managedRealm.admin().users().searchByAttributes(i - 1, 1, null, false, mapToSearchQuery(attributes));
            assertEquals(1, users.size());
            assertTrue(users.get(0).getAttributes().keySet().stream().anyMatch(attributes::containsKey));
        }
    }

    @Test
    public void storeAndReadUserWithLongAttributeValue() {
        String longValue = RandomStringUtils.random(Integer.parseInt(DefaultAttributes.DEFAULT_MAX_LENGTH_ATTRIBUTES), true, true);

        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").password("password").name("user1FirstName", "user1LastName")
                .email("user1@example.com").emailVerified(true).attribute("attr", longValue).build();
        String userId = createUser(userRep);

        UserRepresentation user1 = managedRealm.admin().users().get(userId).toRepresentation();

        Assertions.assertNotNull(user1);
        assertThat(user1.getAttributes().get("attr").get(0), equalTo(longValue));

        UserRepresentation userRep2 = UserConfigBuilder.create()
                .username("user2").password("password").name("user2FirstName", "user2LastName")
                .email("user2@example.com").emailVerified(true).attribute("attr", longValue + "a").build();

        Response response =  managedRealm.admin().users().create(userRep2);
        assertThat(response.getStatus(), equalTo(400));
        assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(), equalTo("error-invalid-length"));
    }

    @Test
    public void searchByLongAttributes() {
        // random string with suffix that makes it case-sensitive and distinct
        String longValue = RandomStringUtils.random(Integer.parseInt(DefaultAttributes.DEFAULT_MAX_LENGTH_ATTRIBUTES) - 1, true, true) + "u";
        String longValue2 = RandomStringUtils.random(Integer.parseInt(DefaultAttributes.DEFAULT_MAX_LENGTH_ATTRIBUTES) - 1, true, true) + "v";

        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").password("password").name("user1FirstName", "user1LastName")
                .email("user1@example.com").emailVerified(true)
                .attribute("test1", longValue, "v2").attribute("test2", "v2").build();
        UserRepresentation userRep2 = UserConfigBuilder.create()
                .username("user2").password("password").name("user2FirstName", "user2LastName")
                .email("user2@example.com").emailVerified(true)
                .attribute("test1", longValue, "v2").attribute("test2", longValue2).build();
        UserRepresentation userRep3 = UserConfigBuilder.create()
                .username("user3").password("password").name("user3FirstName", "user3LastName")
                .email("user3@example.com").emailVerified(true)
                .attribute("test2", longValue, "v3").attribute("test4", "v4").build();

        createUser(userRep);
        createUser(userRep2);
        createUser(userRep3);

        assertThat(managedRealm.admin().users().searchByAttributes(mapToSearchQuery(Map.of("test1", longValue))).stream().map(UserRepresentation::getUsername).collect(Collectors.toList()),
                containsInAnyOrder("user1", "user2"));
        assertThat(managedRealm.admin().users().searchByAttributes(mapToSearchQuery(Map.of("test1", longValue, "test2", longValue2))).stream().map(UserRepresentation::getUsername).collect(Collectors.toList()),
                contains("user2"));

        //case-insensitive search
        assertThat(managedRealm.admin().users().searchByAttributes(mapToSearchQuery(Map.of("test1", longValue, "test2", longValue2.toLowerCase(Locale.ENGLISH)))).stream().map(UserRepresentation::getUsername).collect(Collectors.toList()),
                contains("user2"));
    }

    @Test
    public void searchByUsernameExactMatch() {
        createUsers();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("username11");

        createUser(user);

        List<UserRepresentation> users = managedRealm.admin().users().search("username1", true);
        assertEquals(1, users.size());

        users = managedRealm.admin().users().searchByUsername("username1", true);
        assertEquals(1, users.size());

        users = managedRealm.admin().users().search("user", true);
        assertEquals(0, users.size());
    }

    @Test
    public void searchByFirstNameExact() {
        createUsers();
        List<UserRepresentation> users = managedRealm.admin().users().searchByFirstName("First1", true);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByLastNameExact() {
        createUsers();
        List<UserRepresentation> users = managedRealm.admin().users().searchByLastName("Last1", true);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByFirstNameNullForLastName() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        user.setFirstName("Erik");
        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(true);

        createUser(user);

        List<UserRepresentation> users = managedRealm.admin().users().search("Erik", 0, 50);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByLastNameNullForFirstName() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        user.setLastName("de Wit");
        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(true);

        createUser(user);

        List<UserRepresentation> users = managedRealm.admin().users().search("*wit*", null, null);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByEnabled() {
        String userCommonName = "enabled-disabled-user";

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername(userCommonName + "1");
        user1.setRequiredActions(Collections.emptyList());
        user1.setEnabled(true);
        createUser(user1);

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername(userCommonName + "2");
        user2.setRequiredActions(Collections.emptyList());
        user2.setEnabled(false);
        createUser(user2);

        List<UserRepresentation> enabledUsers = managedRealm.admin().users().search(null, null, null, null, null, null, true, false);
        assertEquals(1, enabledUsers.size());

        List<UserRepresentation> enabledUsersWithFilter = managedRealm.admin().users().search(userCommonName, null, null, null, null, null, true, true);
        assertEquals(1, enabledUsersWithFilter.size());
        assertEquals(user1.getUsername(), enabledUsersWithFilter.get(0).getUsername());

        List<UserRepresentation> disabledUsers = managedRealm.admin().users().search(userCommonName, null, null, null, null, null, false, false);
        assertEquals(1, disabledUsers.size());
        assertEquals(user2.getUsername(), disabledUsers.get(0).getUsername());

        List<UserRepresentation> allUsers = managedRealm.admin().users().search(userCommonName, null, null, null, 0, 100, null, true);
        assertEquals(2, allUsers.size());
    }

    @Test
    public void searchWithFilters() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail("user2@localhost");
        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(false);
        createUser(user);

        List<UserRepresentation> searchFirstNameAndDisabled = managedRealm.admin().users().search(null, "First", null, null, null, null, false, true);
        assertEquals(1, searchFirstNameAndDisabled.size());
        assertEquals(user.getUsername(), searchFirstNameAndDisabled.get(0).getUsername());

        List<UserRepresentation> searchLastNameAndEnabled = managedRealm.admin().users().search(null, null, "Last", null, null, null, true, false);
        assertEquals(0, searchLastNameAndEnabled.size());

        List<UserRepresentation> searchEmailAndDisabled = managedRealm.admin().users().search(null, null, null, "user2@localhost", 0, 50, false, true);
        assertEquals(1, searchEmailAndDisabled.size());
        assertEquals(user.getUsername(), searchEmailAndDisabled.get(0).getUsername());

        List<UserRepresentation> searchInvalidSizeAndDisabled = managedRealm.admin().users().search(null, null, null, null, 10, 20, null, false);
        assertEquals(0, searchInvalidSizeAndDisabled.size());
    }

    @Test
    public void searchWithFilterAndEnabledAttribute() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user3");
        user.setFirstName("user3First");
        user.setLastName("user3Last");
        user.setEmail("user3@localhost");
        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(false);
        createUser(user);

        List<UserRepresentation> searchFilterUserNameAndDisabled = managedRealm.admin().users().search("user3", false, 0, 5);
        assertEquals(1, searchFilterUserNameAndDisabled.size());
        assertEquals(user.getUsername(), searchFilterUserNameAndDisabled.get(0).getUsername());

        List<UserRepresentation> searchFilterMailAndDisabled = managedRealm.admin().users().search("user3@localhost", false, 0, 5);
        assertEquals(1, searchFilterMailAndDisabled.size());
        assertEquals(user.getUsername(), searchFilterMailAndDisabled.get(0).getUsername());

        List<UserRepresentation> searchFilterLastNameAndEnabled = managedRealm.admin().users().search("user3Last", true, 0, 5);
        assertEquals(0, searchFilterLastNameAndEnabled.size());
    }

    @Test
    public void searchByIdp() {
        // Add user without IDP
        createUser();

        // add sample Identity Providers
        final String identityProviderAlias1 = "identity-provider-alias1";
        addSampleIdentityProvider(identityProviderAlias1, 0);
        final String identityProviderAlias2 = "identity-provider-alias2";
        addSampleIdentityProvider(identityProviderAlias2, 1);

        final String commonIdpUserId = "commonIdpUserId";

        // create first IDP1 User with link
        final String idp1User1Username = "idp1user1";
        final String idp1User1KeycloakId = createUser(idp1User1Username, "idp1user1@localhost");
        final String idp1User1UserId = "idp1user1Id";
        FederatedIdentityRepresentation link1_1 = new FederatedIdentityRepresentation();
        link1_1.setUserId(idp1User1UserId);
        link1_1.setUserName(idp1User1Username);
        addFederatedIdentity(idp1User1KeycloakId, identityProviderAlias1, link1_1);

        // create second IDP1 User with link
        final String idp1User2Username = "idp1user2";
        final String idp1User2KeycloakId = createUser(idp1User2Username, "idp1user2@localhost");
        FederatedIdentityRepresentation link1_2 = new FederatedIdentityRepresentation();
        link1_2.setUserId(commonIdpUserId);
        link1_2.setUserName(idp1User2Username);
        addFederatedIdentity(idp1User2KeycloakId, identityProviderAlias1, link1_2);

        // create IDP2 user with link
        final String idp2UserUsername = "idp2user";
        final String idp2UserKeycloakId = createUser(idp2UserUsername, "idp2user@localhost");
        FederatedIdentityRepresentation link2 = new FederatedIdentityRepresentation();
        link2.setUserId(commonIdpUserId);
        link2.setUserName(idp2UserUsername);
        addFederatedIdentity(idp2UserKeycloakId, identityProviderAlias2, link2);

        // run search tests
        List<UserRepresentation> searchForAllUsers =
                managedRealm.admin().users().search(null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(4, searchForAllUsers.size());

        List<UserRepresentation> searchByIdpAlias =
                managedRealm.admin().users().search(null, null, null, null, null, identityProviderAlias1, null, null, null, null,
                        null);
        assertEquals(2, searchByIdpAlias.size());
        assertEquals(idp1User1Username, searchByIdpAlias.get(0).getUsername());
        assertEquals(idp1User2Username, searchByIdpAlias.get(1).getUsername());

        List<UserRepresentation> searchByIdpUserId =
                managedRealm.admin().users().search(null, null, null, null, null, null, commonIdpUserId, null, null, null, null);
        assertEquals(2, searchByIdpUserId.size());
        assertEquals(idp1User2Username, searchByIdpUserId.get(0).getUsername());
        assertEquals(idp2UserUsername, searchByIdpUserId.get(1).getUsername());

        List<UserRepresentation> searchByIdpAliasAndUserId =
                managedRealm.admin().users().search(null, null, null, null, null, identityProviderAlias1, idp1User1UserId, null, null,
                        null,
                        null);
        assertEquals(1, searchByIdpAliasAndUserId.size());
        assertEquals(idp1User1Username, searchByIdpAliasAndUserId.get(0).getUsername());
    }

    @Test
    public void searchByIdpAndEnabled() {
        // add sample Identity Provider
        final String identityProviderAlias = "identity-provider-alias";
        addSampleIdentityProvider(identityProviderAlias, 0);

        // add disabled user with IDP link
        UserRepresentation disabledUser = new UserRepresentation();
        final String disabledUsername = "disabled_username";
        disabledUser.setUsername(disabledUsername);
        disabledUser.setEmail("disabled@localhost");
        disabledUser.setEnabled(false);
        final String disabledUserKeycloakId = createUser(disabledUser);
        FederatedIdentityRepresentation disabledUserLink = new FederatedIdentityRepresentation();
        final String disabledUserId = "disabledUserId";
        disabledUserLink.setUserId(disabledUserId);
        disabledUserLink.setUserName(disabledUsername);
        addFederatedIdentity(disabledUserKeycloakId, identityProviderAlias, disabledUserLink);

        // add enabled user with IDP link
        UserRepresentation enabledUser = new UserRepresentation();
        final String enabledUsername = "enabled_username";
        enabledUser.setUsername(enabledUsername);
        enabledUser.setEmail("enabled@localhost");
        enabledUser.setEnabled(true);
        final String enabledUserKeycloakId = createUser(enabledUser);
        FederatedIdentityRepresentation enabledUserLink = new FederatedIdentityRepresentation();
        final String enabledUserId = "enabledUserId";
        enabledUserLink.setUserId(enabledUserId);
        enabledUserLink.setUserName(enabledUsername);
        addFederatedIdentity(enabledUserKeycloakId, identityProviderAlias, enabledUserLink);

        // run search tests
        List<UserRepresentation> searchByIdpAliasAndEnabled =
                managedRealm.admin().users().search(null, null, null, null, null, identityProviderAlias, null, null, null, true, null);
        assertEquals(1, searchByIdpAliasAndEnabled.size());
        assertEquals(enabledUsername, searchByIdpAliasAndEnabled.get(0).getUsername());

        List<UserRepresentation> searchByIdpAliasAndDisabled =
                managedRealm.admin().users().search(null, null, null, null, null, identityProviderAlias, null, null, null, false,
                        null);
        assertEquals(1, searchByIdpAliasAndDisabled.size());
        assertEquals(disabledUsername, searchByIdpAliasAndDisabled.get(0).getUsername());

        List<UserRepresentation> searchByIdpAliasWithoutEnabledFlag =
                managedRealm.admin().users().search(null, null, null, null, null, identityProviderAlias, null, null, null, null, null);
        assertEquals(2, searchByIdpAliasWithoutEnabledFlag.size());
        assertEquals(disabledUsername, searchByIdpAliasWithoutEnabledFlag.get(0).getUsername());
        assertEquals(enabledUsername, searchByIdpAliasWithoutEnabledFlag.get(1).getUsername());
    }

    @Test
    public void searchById() {
        List<String> userIds = createUsers();
        String expectedUserId = userIds.get(0);
        List<UserRepresentation> users = managedRealm.admin().users().search("id:" + expectedUserId, null, null);

        assertEquals(1, users.size());
        assertEquals(expectedUserId, users.get(0).getId());

        users = managedRealm.admin().users().search("id:   " + expectedUserId + "     ", null, null);

        assertEquals(1, users.size());
        assertEquals(expectedUserId, users.get(0).getId());

        // Should allow searching for multiple users
        String expectedUserId2 = userIds.get(1);
        List<UserRepresentation> multipleUsers = managedRealm.admin().users().search(String.format("id:%s %s", expectedUserId, expectedUserId2), 0 , 10);;
        assertThat(multipleUsers, hasSize(2));
        assertThat(multipleUsers.get(0).getId(), is(expectedUserId));
        assertThat(multipleUsers.get(1).getId(), is(expectedUserId2));

        // Should take arbitrary amount of spaces in between ids
        List<UserRepresentation> multipleUsers2 = managedRealm.admin().users().search(String.format("id:  %s   %s  ", expectedUserId, expectedUserId2), 0 , 10);;
        assertThat(multipleUsers2, hasSize(2));
        assertThat(multipleUsers2.get(0).getId(), is(expectedUserId));
        assertThat(multipleUsers2.get(1).getId(), is(expectedUserId2));
    }

    @Test
    public void infixSearch() {
        List<String> userIds = createUsers();

        // Username search
        List<UserRepresentation> users = managedRealm.admin().users().search("*1*", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        users = managedRealm.admin().users().search("*y*", null, null);
        assertThat(users.size(), is(0));

        users = managedRealm.admin().users().search("*name*", null, null);
        assertThat(users, hasSize(9));

        users = managedRealm.admin().users().search("**", null, null);
        assertThat(users, hasSize(9));

        // First/Last name search
        users = managedRealm.admin().users().search("*first1*", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        users = managedRealm.admin().users().search("*last*", null, null);
        assertThat(users, hasSize(9));

        // Email search
        users = managedRealm.admin().users().search("*@localhost*", null, null);
        assertThat(users, hasSize(9));

        users = managedRealm.admin().users().search("*1@local*", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));
    }

    @Test
    public void prefixSearch() {
        List<String> userIds = createUsers();

        // Username search
        List<UserRepresentation> users = managedRealm.admin().users().search("user", null, null);
        assertThat(users, hasSize(9));

        users = managedRealm.admin().users().search("user*", null, null);
        assertThat(users, hasSize(9));

        users = managedRealm.admin().users().search("name", null, null);
        assertThat(users, hasSize(0));

        users = managedRealm.admin().users().search("name*", null, null);
        assertThat(users, hasSize(0));

        users = managedRealm.admin().users().search("username1", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        users = managedRealm.admin().users().search("username1*", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        users = managedRealm.admin().users().search(null, null, null);
        assertThat(users, hasSize(9));

        users = managedRealm.admin().users().search("", null, null);
        assertThat(users, hasSize(9));

        users = managedRealm.admin().users().search("*", null, null);
        assertThat(users, hasSize(9));

        // First/Last name search
        users = managedRealm.admin().users().search("first1", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        users = managedRealm.admin().users().search("first1*", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        users = managedRealm.admin().users().search("last", null, null);
        assertThat(users, hasSize(9));

        users = managedRealm.admin().users().search("last*", null, null);
        assertThat(users, hasSize(9));

        // Email search
        users = managedRealm.admin().users().search("user1@local", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        users = managedRealm.admin().users().search("user1@local*", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));
    }

    @Test
    public void circumfixSearch() {
        createUsers();

        List<UserRepresentation> users = managedRealm.admin().users().search("u*name", null, null);
        assertThat(users, hasSize(9));
    }

    @Test
    public void wildcardSearch() {
        UserProfileResource upResource = managedRealm.admin().users().userProfile();
        UPConfig upConfig = upResource.getConfiguration();
        Map<String, Object> prohibitedCharsOrigCfg = upConfig.getAttribute(UserModel.USERNAME).getValidations().get(UsernameProhibitedCharactersValidator.ID);
        upConfig.getAttribute(UserModel.USERNAME).getValidations().remove(UsernameProhibitedCharactersValidator.ID);
        upResource.update(upConfig);
        adminEvents.clear();

        try {
            createUser("0user\\\\0", "email0@emal");
            createUser("1user\\\\", "email1@emal");
            createUser("2user\\\\%", "email2@emal");
            createUser("3user\\\\*", "email3@emal");
            createUser("4user\\\\_", "email4@emal");

            assertThat(managedRealm.admin().users().search("*", null, null), hasSize(5));
            assertThat(managedRealm.admin().users().search("*user\\", null, null), hasSize(5));
            assertThat(managedRealm.admin().users().search("\"2user\\\\%\"", null, null), hasSize(1));
        } finally {
            upConfig.getAttribute(UserModel.USERNAME).addValidation(UsernameProhibitedCharactersValidator.ID, prohibitedCharsOrigCfg);
            upResource.update(upConfig);
        }
    }

    @Test
    public void exactSearch() {
        List<String> userIds = createUsers();

        // Username search
        List<UserRepresentation> users = managedRealm.admin().users().search("\"username1\"", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        users = managedRealm.admin().users().search("\"user\"", null, null);
        assertThat(users, hasSize(0));

        users = managedRealm.admin().users().search("\"\"", null, null);
        assertThat(users, hasSize(0));

        // First/Last name search
        users = managedRealm.admin().users().search("\"first1\"", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));

        // Email search
        users = managedRealm.admin().users().search("\"user1@localhost\"", null, null);
        assertThat(users, hasSize(1));
        assertThat(userIds.get(0), equalTo(users.get(0).getId()));
    }

    @Test
    public void searchWithExactMatch() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("test_username");
        user.setFirstName("test_first_name");
        user.setLastName("test_last_name");
        user.setEmail("test_email@test.com");
        user.setEnabled(true);
        user.setEmailVerified(true);
        createUser(user);

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("test_username2");
        user2.setFirstName("test_first_name2");
        user2.setLastName("test_last_name");
        user2.setEmail("test_email@test.com2");
        user2.setEnabled(true);
        user2.setEmailVerified(true);
        createUser(user2);

        UserRepresentation user3 = new UserRepresentation();
        user3.setUsername("test_username3");
        user3.setFirstName("test_first_name");
        user3.setLastName("test_last_name3");
        user3.setEmail("test_email@test.com3");
        user3.setEnabled(true);
        user3.setEmailVerified(true);
        createUser(user3);

        List<UserRepresentation> users = managedRealm.admin().users().search(
                null, null, null, "test_email@test.co",
                0, 10, null, null, true
        );
        assertEquals(0, users.size());
        users = managedRealm.admin().users().search(
                null, null, null, "test_email@test.com",
                0, 10, null, null, true
        );
        assertEquals(1, users.size());
        users = managedRealm.admin().users().search(
                null, null, "test_last", "test_email@test.com",
                0, 10, null, null, true
        );
        assertEquals(0, users.size());
        users = managedRealm.admin().users().search(
                null, null, "test_last_name", "test_email@test.com",
                0, 10, null, null, true
        );
        assertEquals(1, users.size());
        users = managedRealm.admin().users().search(
                null, "test_first", "test_last_name", "test_email@test.com",
                0, 10, null, null, true
        );
        assertEquals(0, users.size());
        users = managedRealm.admin().users().search(
                null, "test_first_name", "test_last_name", "test_email@test.com",
                0, 10, null, null, true
        );
        assertEquals(1, users.size());
        users = managedRealm.admin().users().search(
                "test_usernam", "test_first_name", "test_last_name", "test_email@test.com",
                0, 10, null, null, true
        );
        assertEquals(0, users.size());
        users = managedRealm.admin().users().search(
                "test_username", "test_first_name", "test_last_name", "test_email@test.com",
                0, 10, null, null, true
        );
        assertEquals(1, users.size());

        users = managedRealm.admin().users().search(
                null, null, "test_last_name", null,
                0, 10, null, null, true
        );
        assertEquals(2, users.size());
        users = managedRealm.admin().users().search(
                null, "test_first_name", null, null,
                0, 10, null, null, true
        );
        assertEquals(2, users.size());
    }

    @Test
    public void countUsersNotServiceAccount() {
        createUsers();

        Integer count = managedRealm.admin().users().count();
        assertEquals(9, count.intValue());

        ClientRepresentation client = new ClientRepresentation();

        client.setClientId("test-client");
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setServiceAccountsEnabled(true);
        client.setEnabled(true);
        client.setRedirectUris(Arrays.asList("http://url"));

        String clientId = ApiUtil.getCreatedId(managedRealm.admin().clients().create(client));

        // KEYCLOAK-5660, should not consider service accounts
        assertEquals(9, managedRealm.admin().users().count().intValue());

        // client cleanup
        managedRealm.admin().clients().get(clientId).remove();
    }

    @Test
    public void searchPaginated() {
        createUsers();

        List<UserRepresentation> users = managedRealm.admin().users().search("username", 0, 1);
        assertEquals(1, users.size());
        assertEquals("username1", users.get(0).getUsername());

        users = managedRealm.admin().users().search("username", 5, 2);
        assertEquals(2, users.size());
        assertEquals("username6", users.get(0).getUsername());
        assertEquals("username7", users.get(1).getUsername());

        users = managedRealm.admin().users().search("username", 7, 20);
        assertEquals(2, users.size());
        assertEquals("username8", users.get(0).getUsername());
        assertEquals("username9", users.get(1).getUsername());

        users = managedRealm.admin().users().search("username", 0, 20);
        assertEquals(9, users.size());
    }
}
