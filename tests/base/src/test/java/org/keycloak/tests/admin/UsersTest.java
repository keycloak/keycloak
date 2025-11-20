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

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest
public class UsersTest {

    private static final String realmName = "default";

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.keycloak.admin"})
    RunOnServerClient runOnServer;

    @Test
    public void searchUserWithWildcards() {
        createUser("User", "firstName", "lastName", "user@example.com");

        assertThat(realm.admin().users().search("Use%", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Use_", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Us_r", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Use", null, null), hasSize(1));
        assertThat(realm.admin().users().search("Use*", null, null), hasSize(1));
        assertThat(realm.admin().users().search("Us*e", null, null), hasSize(1));
    }

    @Test
    public void testFullRepresentationOnSearches() {
        createUser("user", "firstName", "lastName", "user@example.com");

        List<UserRepresentation> users = realm.admin().users().search("user", null, null, false);
        UserRepresentation user = users.get(0);
        assertThat(user.getRequiredActions(), empty());

        users = realm.admin().users().search("user", null, null, true);
        user = users.get(0);
        assertThat(user.getRequiredActions(), nullValue());
        assertThat(user.isTotp(), nullValue());
    }

    @Test
    public void searchUserDefaultSettings() throws Exception {
        createUser("User", "firstName", "lastName", "user@example.com");

        assertCaseInsensitiveSearch();
    }

    @Test
    public void searchUserMatchUsersCount() {
        createUser("john.doe", "John", "Doe Smith", "john.doe@keycloak.org");
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
        createUser(UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified(true)
                .enabled(true)
                .build());

        createUser(UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build());

        boolean emailVerified;
        emailVerified = true;
        List<UserRepresentation> usersEmailVerified = realm.admin().users().search(null, null, null, null, emailVerified, null, null, null, true);
        assertThat(usersEmailVerified, is(not(empty())));
        assertThat(usersEmailVerified.get(0).getUsername(), is("user1"));

        createUser(UserConfigBuilder.create()
                .username("testuser2")
                .password("password")
                .name("testuser2", "testuser2")
                .email("testuser2@example.com")
                .emailVerified(true)
                .enabled(true)
                .build());

        usersEmailVerified = realm.admin().users().search("user", null, null, null, emailVerified, null, null, null);
        assertThat(usersEmailVerified, is(not(empty())));
        assertThat(usersEmailVerified.size(), is(1));
        assertThat(usersEmailVerified.get(0).getUsername(), is("user1"));
        assertThat(realm.admin().users().count("user", null, null, null, emailVerified, null, null, null), is(1));

        emailVerified = false;
        List<UserRepresentation> usersEmailNotVerified = realm.admin().users().search(null, null, null, null, emailVerified, null, null, null, true);
        assertThat(usersEmailNotVerified, is(not(empty())));
        assertThat(usersEmailNotVerified.get(0).getUsername(), is("user2"));

        createUser(UserConfigBuilder.create()
                .username("testuser3")
                .password("password")
                .name("testuser3", "testuser3")
                .email("testuser3@example.com")
                .enabled(true)
                .build());

        usersEmailVerified = realm.admin().users().search("user", null, null, null, emailVerified, null, null, null);
        assertThat(usersEmailVerified, is(not(empty())));
        assertThat(usersEmailVerified.size(), is(1));
        assertThat(usersEmailVerified.get(0).getUsername(), is("user2"));
        assertThat(realm.admin().users().count("user", null, null, null, emailVerified, null, null, null), is(1));
    }

    @Test
    public void testCountUsersByEnabledStatus() {
        createUser(UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified(true)
                .enabled(true)
                .build());

        createUser(UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(false)
                .build());

        assertThat(realm.admin().users().count("user", null, null, null, null, null, true, null), is(1));
        assertThat(realm.admin().users().count("user", null, null, null, null, null, false, null), is(1));
    }

    @Test
    public void testCountUsersByFederatedIdentity() {
        createUser(UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified(true)
                .enabled(true)
                .build());
        createUser(UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(false)
                .build());

        runOnServer.run((session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            UserProvider users = session.users();
            users.addFederatedIdentity(realm, users.getUserById(realm, users.getUserByUsername(realm, "user1").getId()), new FederatedIdentityModel("user1Broker", "user1BrokerId", "user1BrokerUsername"));
            users.addFederatedIdentity(realm, users.getUserById(realm, users.getUserByUsername(realm, "user2").getId()), new FederatedIdentityModel("user2Broker", "user2BrokerId", "user2BrokerUsername"));
        }));

        assertThat(realm.admin().users().count(null, "user", null, null, null, null, null, "user1Broker", null, null), is(1));
        assertThat(realm.admin().users().count(null, "user", null, null, null, null, null, "user1Broker", "user1BrokerId", null), is(1));
        assertThat(realm.admin().users().count(null, "user", null, null, null, null, null, "user1Broker", "invalidId", null), is(0));
        assertThat(realm.admin().users().count(null, "user", null, null, null, null, false, "user2Broker", null, null), is(1));
        assertThat(realm.admin().users().count(null, "user", null, null, null, null, false, "user2Broker", "user1BrokerId", null), is(0));
    }

    /**
     * https://issues.redhat.com/browse/KEYCLOAK-15146
     */
    @Test
    public void countUsersByEmailVerifiedStatus() {
        createUser(UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified(true)
                .enabled(true)
                .build());

        createUser(UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build());

        createUser(UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .emailVerified(true)
                .enabled(true)
                .build());

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
        createUser("user1", "user1FirstName", "user1LastName", "user1@example.com");
        createUser("user2", "user2FirstName", "user2LastName", "user2@example.com");
        assertThat(realm.admin().users().count(), is(2));
    }

    @Test
    public void countUsersBySearchWithViewPermission() {
        createUser(UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified(true)
                .enabled(true)
                .build());

        createUser(UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build());

        createUser(UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .emailVerified(true)
                .enabled(true)
                .build());

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

    @Test
    public void countUsersByFiltersWithViewPermission() {
        createUser("user1", "user1FirstName", "user1LastName", "user1@example.com");
        createUser("user2", "user2FirstName", "user2LastName", "user2@example.com");
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

    private void assertSearchMatchesCount(RealmResource realmResource, String search, Integer expectedCount) {
        Integer count = realmResource.users().count(search);
        assertThat(count, is(expectedCount));
        assertThat(realmResource.users().search(search, null, null), hasSize(count));
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

    private void createUser(UserRepresentation user) {
        realm.admin().users().create(user).close();
    }

    private void createUser(String username, String firstName, String lastName, String email) {
        createUser(UserConfigBuilder.create()
                .username(username)
                .password("password")
                .name(firstName, lastName)
                .email(email)
                .enabled(true)
                .build());
    }
}
