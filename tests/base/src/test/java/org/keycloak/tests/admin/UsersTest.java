/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@KeycloakIntegrationTest(config = UsersTest.ServerConfig.class)
public class UsersTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @BeforeEach
    public void cleanUsers() {
        List<UserRepresentation> userRepresentations = realm.admin().users().list();
        for (UserRepresentation user : userRepresentations) {
            realm.admin().users().delete(user.getId());
        }
        List<GroupRepresentation> groups = realm.admin().groups().groups();
        for (GroupRepresentation group : groups) {
            realm.admin().groups().group(group.getId()).remove();
        }
    }

    private void createUser(String username, String password, String firstName, String lastName, String email) {
        UserRepresentation user = UserConfigBuilder.create()
                .username(username)
                .password(password)
                .name(firstName, lastName)
                .email(email)
                .enabled(true)
                .build();
        realm.admin().users().create(user);
    }

    @Test
    public void searchUserWithWildcards() {
        createUser("User", "password", "firstName", "lastName", "user@example.com");

        assertThat(realm.admin().users().search("Use%", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Use_", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Us_r", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Use", null, null), hasSize(1));
        assertThat(realm.admin().users().search("Use*", null, null), hasSize(1));
        assertThat(realm.admin().users().search("Us*e", null, null), hasSize(1));
    }

    @Test
    public void searchUserDefaultSettings() throws Exception {
        createUser("User", "password", "firstName", "lastName", "user@example.com");

        assertCaseInsensitiveSearch();
    }

    @Test
    public void searchUserMatchUsersCount() {
        createUser("john.doe", "password", "John", "Doe Smith", "john.doe@keycloak.org");
        String search = "jo do";

        assertThat(realm.admin().users().count(search), is(1));
        List<UserRepresentation> users = realm.admin().users().search(search, null, null);
        assertThat(users, hasSize(1));
        assertThat(users.get(0).getUsername(), is("john.doe"));
    }

    /**
     * https://issues.redhat.com/browse/KEYCLOAK-15146
     */
    @Test
    public void findUsersByEmailVerifiedStatus() {
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user1);

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        realm.admin().users().create(user2);

        boolean emailVerified;
        emailVerified = true;
        List<UserRepresentation> usersEmailVerified = realm.admin().users().search(null, null, null, null, emailVerified, null, null, null, true);
        assertThat(usersEmailVerified, is(not(empty())));
        assertThat(usersEmailVerified.get(0).getUsername(), is("user1"));

        emailVerified = false;
        List<UserRepresentation> usersEmailNotVerified = realm.admin().users().search(null, null, null, null, emailVerified, null, null, null, true);
        assertThat(usersEmailNotVerified, is(not(empty())));
        assertThat(usersEmailNotVerified.get(0).getUsername(), is("user2"));
    }

    /**
     * https://issues.redhat.com/browse/KEYCLOAK-15146
     */
    @Test
    public void countUsersByEmailVerifiedStatus() {
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user1);

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        realm.admin().users().create(user2);

        UserRepresentation user3 = UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user3);

        boolean emailVerified;
        emailVerified = true;
        assertThat(realm.admin().users().countEmailVerified(emailVerified), is(2));
        assertThat(realm.admin().users().count(null,null,null,emailVerified,null), is(2));

        emailVerified = false;
        assertThat(realm.admin().users().countEmailVerified(emailVerified), is(1));
        assertThat(realm.admin().users().count(null,null,null,emailVerified,null), is(1));
    }

    @Test
    public void countUsersWithViewPermission() {
        createUser("user1", "password", "user1FirstName", "user1LastName", "user1@example.com");
        createUser("user2", "password", "user2FirstName", "user2LastName", "user2@example.com");
        assertThat(realm.admin().users().count(), is(2));
    }

    @Test
    public void countUsersBySearchWithViewPermission() {
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user1);

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        realm.admin().users().create(user2);

        UserRepresentation user3 = UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user3);

        // Prefix search count
        assertSearchMatchesCount(realm.admin(), "user", 3);
        assertSearchMatchesCount(realm.admin(), "user*", 3);
        assertSearchMatchesCount(realm.admin(), "er", 0);
        assertSearchMatchesCount(realm.admin(), "", 3);
        assertSearchMatchesCount(realm.admin(), "*", 3);
        assertSearchMatchesCount(realm.admin(), "user2FirstName", 1);
        assertSearchMatchesCount(realm.admin(), "user2First", 1);
        assertSearchMatchesCount(realm.admin(), "user2First*", 1);
        assertSearchMatchesCount(realm.admin(), "user1@example", 1);
        assertSearchMatchesCount(realm.admin(), "user1@example*", 1);
        assertSearchMatchesCount(realm.admin(), null, 3);

        // Infix search count
        assertSearchMatchesCount(realm.admin(), "*user*", 3);
        assertSearchMatchesCount(realm.admin(), "**", 3);
        assertSearchMatchesCount(realm.admin(), "*foobar*", 0);
        assertSearchMatchesCount(realm.admin(), "*LastName*", 3);
        assertSearchMatchesCount(realm.admin(), "*FirstName*", 3);
        assertSearchMatchesCount(realm.admin(), "*@example.com*", 3);

        // Exact search count
        assertSearchMatchesCount(realm.admin(), "\"user1\"", 1);
        assertSearchMatchesCount(realm.admin(), "\"1\"", 0);
        assertSearchMatchesCount(realm.admin(), "\"\"", 0);
        assertSearchMatchesCount(realm.admin(), "\"user1FirstName\"", 1);
        assertSearchMatchesCount(realm.admin(), "\"user1LastName\"", 1);
        assertSearchMatchesCount(realm.admin(), "\"user1@example.com\"", 1);
    }

    private void assertSearchMatchesCount(RealmResource realmResource, String search, Integer expectedCount) {
        Integer count = realmResource.users().count(search);
        assertThat(count, is(expectedCount));
        assertThat(realmResource.users().search(search, null, null), hasSize(count));
    }

    @Test
    public void countUsersByFiltersWithViewPermission() {
        createUser("user1", "password", "user1FirstName", "user1LastName", "user1@example.com");
        createUser("user2", "password", "user2FirstName", "user2LastName", "user2@example.com");
        //search username
        assertThat(realm.admin().users().count(null, null, null, "user"), is(2));
        assertThat(realm.admin().users().count(null, null, null, "user1"), is(1));
        assertThat(realm.admin().users().count(null, null, null, "notExisting"), is(0));
        assertThat(realm.admin().users().count(null, null, null, ""), is(2));
        //search first name
        assertThat(realm.admin().users().count(null, "FirstName", null, null), is(2));
        assertThat(realm.admin().users().count(null, "user2FirstName", null, null), is(1));
        assertThat(realm.admin().users().count(null, "notExisting", null, null), is(0));
        assertThat(realm.admin().users().count(null, "", null, null), is(2));
        //search last name
        assertThat(realm.admin().users().count("LastName", null, null, null), is(2));
        assertThat(realm.admin().users().count("user2LastName", null, null, null), is(1));
        assertThat(realm.admin().users().count("notExisting", null, null, null), is(0));
        assertThat(realm.admin().users().count("", null, null, null), is(2));
        //search in email
        assertThat(realm.admin().users().count(null, null, "@example.com", null), is(2));
        assertThat(realm.admin().users().count(null, null, "user1@example.com", null), is(1));
        assertThat(realm.admin().users().count(null, null, "user1@test.com", null), is(0));
        assertThat(realm.admin().users().count(null, null, "", null), is(2));
        //search for combinations
        assertThat(realm.admin().users().count("LastName", "FirstName", null, null), is(2));
        assertThat(realm.admin().users().count("user1LastName", "FirstName", null, null), is(1));
        assertThat(realm.admin().users().count("user1LastName", "", null, null), is(1));
        assertThat(realm.admin().users().count("LastName", "", null, null), is(2));
        assertThat(realm.admin().users().count("LastName", "", null, null), is(2));
        assertThat(realm.admin().users().count(null, null, "@example.com", "user"), is(2));
        //search not specified (defaults to simply /count)
        assertThat(realm.admin().users().count(null, null, null, null), is(2));
        assertThat(realm.admin().users().count("", "", "", ""), is(2));
    }


    @Test
    public void countUsersWithGroupViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(true);
        assertThat(testRealmResource.users().count(), is(3));
    }

    @Test
    public void countUsersBySearchWithGroupViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
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
    public void countUsersByFiltersWithGroupViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
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
    public void countUsersWithNoViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(false);
        assertThat(testRealmResource.users().count(), is(0));
    }

    @Test
    public void countUsersBySearchWithNoViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
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
    public void countUsersByFiltersWithNoViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
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

    private RealmResource setupTestEnvironmentWithPermissions(boolean grp1ViewPermissions) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
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

        //create test users and groups
        List<GroupRepresentation> groups = setupUsersInGroupsWithPermissions();

        if (grp1ViewPermissions) {
            AuthorizationResource authorizationResource = realm.admin().clients().get(realmManagementId).authorization();
            //create a user policy for the test user
            UserPolicyRepresentation policy = new UserPolicyRepresentation();
            String policyName = "test-policy";
            policy.setName(policyName);
            policy.setUsers(Collections.singleton(testUserId));
            authorizationResource.policies().user().create(policy).close();
            PolicyRepresentation policyRepresentation = authorizationResource.policies().findByName(policyName);
            //add the policy to grp1
            Optional<GroupRepresentation> optional = groups.stream().filter(g -> g.getName().equals("grp1")).findFirst();
            assertThat(optional.isPresent(), is(true));
            GroupRepresentation grp1 = optional.get();
            ScopePermissionRepresentation scopePermissionRepresentation = authorizationResource.permissions().scope().findByName("view.members.permission.group." + grp1.getId());
            scopePermissionRepresentation.setPolicies(Collections.singleton(policyRepresentation.getId()));
            scopePermissionRepresentation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
            authorizationResource.permissions().scope().findById(scopePermissionRepresentation.getId()).update(scopePermissionRepresentation);
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

    private List<GroupRepresentation> setupUsersInGroupsWithPermissions() {
        //create two groups
        GroupRepresentation grp1 = createGroupWithPermissions("grp1");
        GroupRepresentation grp2 = createGroupWithPermissions("grp2");
        //create test users
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .enabled(true)
                .build();
        String user1Id = ApiUtil.getCreatedId(realm.admin().users().create(user1));

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        String user2Id = ApiUtil.getCreatedId(realm.admin().users().create(user2));

        UserRepresentation user3 = UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .enabled(true)
                .build();
        String user3Id = ApiUtil.getCreatedId(realm.admin().users().create(user3));

        UserRepresentation user4 = UserConfigBuilder.create()
                .username("user4")
                .password("password")
                .name("user4FirstName", "user4LastName")
                .email("user4@example.com")
                .enabled(true)
                .build();
        String user4Id = ApiUtil.getCreatedId(realm.admin().users().create(user4));

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

    private GroupRepresentation createGroupWithPermissions(String name) {
        GroupRepresentation grp = new GroupRepresentation();
        grp.setName(name);
        realm.admin().groups().add(grp);
        Optional<GroupRepresentation> optional = realm.admin().groups().groups().stream().filter(g -> g.getName().equals(name)).findFirst();
        assertThat(optional.isPresent(), is(true));
        grp = optional.get();
        String id = grp.getId();
        //enable the permissions
        realm.admin().groups().group(id).setPermissions(new ManagementPermissionRepresentation(true));
        assertThat(realm.admin().groups().group(id).getPermissions().isEnabled(), is(true));

        return grp;
    }

    private void assertCaseInsensitiveSearch() {
        // not-exact case-insensitive search
        assertThat(realm.admin().users().search("user"), hasSize(1));
        assertThat(realm.admin().users().search("User"), hasSize(1));
        assertThat(realm.admin().users().search("USER"), hasSize(1));
        assertThat(realm.admin().users().search("Use"), hasSize(1));

        // exact case-insensitive search
        assertThat(realm.admin().users().search("user", true), hasSize(1));
        assertThat(realm.admin().users().search("User", true), hasSize(1));
        assertThat(realm.admin().users().search("USER", true), hasSize(1));
        assertThat(realm.admin().users().search("Use", true), hasSize(0));
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        }

    }
}
