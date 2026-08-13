/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.federation.ldap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.naming.directory.SearchControls;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.datastore.DefaultDatastoreProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPSearchForUsersPaginationTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {

            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "streetMapper", LDAPConstants.STREET, LDAPConstants.STREET);

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            // Delete all local users and add some new for testing
            session.users().searchForUserStream(appRealm, new HashMap<>()).collect(Collectors.toList()).forEach(u -> session.users().removeUser(appRealm, u));

            LDAPObject ldapUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john", "Some", "Some", "john14@email.org", "Acacia Avenue", "1234");
            // somehow, for MSAD it is only possible to create an enabled user by updating the password and setting USER_ACCOUNT_CONTROL attribute to 512
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), ldapUser, "password");
            ldapUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john00", "john", "Doe", "john0@email.org", "Acacia Avenue", "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), ldapUser, "password");
            ldapUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john01", "john", "Doe", "john1@email.org", "Acacia Avenue", "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), ldapUser, "password");

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john02", "john", "Doe", "john2@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john03", "john", "Doe", "john3@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john04", "john", "Doe", "john4@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john05", "Some", "john", "john5@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john06", "Some", "john", "john6@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john07", "Some", "john", "john7@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john08", "Some", "john", "john8@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john09", "Some", "john", "john9@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john10", "Some", "Some", "john10@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john11", "Some", "Some", "john11@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john12", "Some", "Some", "john12@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john13", "Some", "Some", "john13@email.org", null, "1234");
        });
    }

    @Test
    public void testPagination() {
        //this call should import some users into local database
        //collecting to TreeSet for ordering as users are orderd by username when querying from local database
        @SuppressWarnings("unchecked")
        LinkedList<String> importedUsers = new LinkedList(managedRealm.admin().users().search("*", 0, 5).stream().map(UserRepresentation::getUsername).collect(Collectors.toCollection(TreeSet::new)));

        //this call should ommit first 3 already imported users from local db
        //it should return 2 local(imported) users and 8 users from ldap
        List<String> search = managedRealm.admin().users().search("*", 3, 10).stream().map(UserRepresentation::getUsername).collect(Collectors.toList());

        assertThat(search, hasSize(10));
        assertThat(search, not(contains(importedUsers.get(0))));
        assertThat(search, not(contains(importedUsers.get(1))));
        assertThat(search, not(contains(importedUsers.get(2))));
        assertThat(search, hasItems(importedUsers.get(3), importedUsers.get(4)));
    }

    @Test
    public void testSearchLDAPMatchesLocalDBTwoKeywords() {
        assertLDAPSearchMatchesLocalDB("Some Some");
    }

    @Test
    public void testSearchLDAPMatchesLocalDBExactSearch() {
        assertLDAPSearchMatchesLocalDB("\"Some\"");
    }

    @Test
    public void testSearchLDAPMatchesLocalDBInfixSearch() {
        assertLDAPSearchMatchesLocalDB("*ohn*");
    }

    @Test
    public void testSearchLDAPMatchesLocalDBPrefixSearch() {
        assertLDAPSearchMatchesLocalDB("john*");
    }

    @Test
    public void testSearchLDAPMatchesLocalDBDefaultPrefixSearch() {
        // default search is prefix search
        assertLDAPSearchMatchesLocalDB("john");
    }

    @Test
    public void testSearchLDAPStreet() {
        Set<String> usernames = managedRealm.admin().users().searchByAttributes("street:\"Acacia Avenue\"")
                .stream().map(UserRepresentation::getUsername)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john", "john00", "john01"), usernames);

        usernames = managedRealm.admin().users().searchByAttributes(0, 5, true, true, "street:\"Acacia Avenue\"")
                .stream().map(UserRepresentation::getUsername)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john", "john00", "john01"), usernames);
    }

    @Test
    public void testSearchNonExact() {
        Set<String> usernames = managedRealm.admin().users().searchByEmail("1@email.org", false)
                .stream()
                .map(UserRepresentation::getUsername)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john01", "john11"), usernames);

        usernames = managedRealm.admin().users().searchByEmail("1@email.org", false)
                .stream()
                .map(UserRepresentation::getUsername)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john01", "john11"), usernames);
    }

    @Test
    public void testSearchLDAPLdapId() {
        UserRepresentation john = managedRealm.admin().users().search("john", true).stream().findAny().orElse(null);
        Assertions.assertNotNull(john);
        Assertions.assertNotNull(john.firstAttribute(LDAPConstants.LDAP_ID));
        Set<String> usernames = managedRealm.admin().users()
                .searchByAttributes(LDAPConstants.LDAP_ID + ":" + john.firstAttribute(LDAPConstants.LDAP_ID))
                .stream().map(UserRepresentation::getUsername)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john"), usernames);
    }

    @Test
    public void testSearchLDAPLdapEntryDn() {
        UserRepresentation john = managedRealm.admin().users().search("john", true).stream().findAny().orElse(null);
        Assertions.assertNotNull(john);
        Assertions.assertNotNull(john.firstAttribute(LDAPConstants.LDAP_ENTRY_DN));
        Set<String> usernames = managedRealm.admin().users()
                .searchByAttributes(LDAPConstants.LDAP_ENTRY_DN + ":" + john.firstAttribute(LDAPConstants.LDAP_ENTRY_DN))
                .stream().map(UserRepresentation::getUsername)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john"), usernames);
    }

    @Test
    public void testSearchLDAPLdapEntryDnOutsideUsersDn() {
        // create an OU outside the configured users DN and add a user-like LDAP entry in it.
        String outOfScopeDn = testingClient.server().fetchString(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPStorageProvider ldapProvider = ctx.getLdapProvider();
            LDAPTestUtils.addLdapOUinBaseDn(ldapProvider, "ServiceAccounts");

            LDAPObject ldapObject = new LDAPObject();
            ldapObject.setRdnAttributeName(ldapProvider.getLdapIdentityStore().getConfig().getRdnLdapAttribute());
            ldapObject.setObjectClasses(ldapProvider.getLdapIdentityStore().getConfig().getUserObjectClasses());
            ldapObject.setSingleAttribute(ldapProvider.getLdapIdentityStore().getConfig().getRdnLdapAttribute(), "outsideuser");
            ldapObject.setSingleAttribute(LDAPConstants.SN, "Outside");
            ldapObject.setSingleAttribute(LDAPConstants.CN, "outsideuser");
            ldapObject.setSingleAttribute(LDAPConstants.GIVENNAME, "Outside");
            ldapObject.setSingleAttribute("mail", "outsideuser@example.org");

            LDAPDn dn = LDAPDn.fromString(ldapProvider.getLdapIdentityStore().getConfig().getBaseDn());
            dn.addFirst("ou", "ServiceAccounts");
            dn.addFirst(ldapProvider.getLdapIdentityStore().getConfig().getRdnLdapAttribute(), "outsideuser");
            ldapObject.setDn(dn);
            ldapProvider.getLdapIdentityStore().add(ldapObject);

            // verify the entry exists in LDAP.
            try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(ldapProvider, ctx.getRealm())) {
                ldapQuery.setSearchDn(dn.toString());
                ldapQuery.setSearchScope(SearchControls.OBJECT_SCOPE);
                Assertions.assertNotNull(ldapQuery.getFirstResult(), "Out-of-scope LDAP entry should exist");
            }
            return dn.toString();
        }).replace("\"", "");

        // search via the Admin REST API using the out-of-scope DN — should return no results.
        final String dnForServerRun = outOfScopeDn;
        Set<String> usernames = managedRealm.admin().users()
                .searchByAttributes(LDAPConstants.LDAP_ENTRY_DN + ":" + outOfScopeDn)
                .stream().map(UserRepresentation::getUsername).collect(Collectors.toSet());
        Assertions.assertTrue(usernames.isEmpty(), "Admin API searchByAttributes with out-of-scope DN should return no results");

        // verify the provider also rejects the out-of-scope DN via searchForUserStream and searchForUserByUserAttribute.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            LDAPStorageProvider ldapProvider = ctx.getLdapProvider();

            Map<String, String> params = new HashMap<>();
            params.put(LDAPConstants.LDAP_ENTRY_DN, dnForServerRun);
            Assertions.assertTrue(
                    ldapProvider.searchForUserStream(appRealm, params, null, null).toList().isEmpty(),
                    "searchForUserStream with out-of-scope DN should return no results");
            Assertions.assertTrue(
                    session.users().searchForUserByUserAttributeStream(appRealm, LDAPConstants.LDAP_ENTRY_DN, dnForServerRun).toList().isEmpty(),
                    "searchForUserByUserAttribute with out-of-scope DN should return no results");
        });

        // cleanup.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            LDAPStorageProvider ldapProvider = ctx.getLdapProvider();
            UserModel localUser = session.users().getUserByUsername(appRealm, "outsideuser");
            if (localUser != null) {
                session.users().removeUser(appRealm, localUser);
            }
            LDAPObject ldapObject = new LDAPObject();
            ldapObject.setDn(LDAPDn.fromString(dnForServerRun));
            ldapProvider.getLdapIdentityStore().remove(ldapObject);
            LDAPObject ouObject = new LDAPObject();
            LDAPDn ouDn = LDAPDn.fromString(ldapProvider.getLdapIdentityStore().getConfig().getBaseDn());
            ouDn.addFirst("ou", "ServiceAccounts");
            ouObject.setDn(ouDn);
            ldapProvider.getLdapIdentityStore().remove(ouObject);
        });
    }

    public void testDuplicateEmailInDatabase() {
        setLDAPEnabled(false);
        try {
            // create a local db user with the same email than an a ldap user
            String userId = ApiUtil.getCreatedId(managedRealm.admin().users().create(UserBuilder.create()
                    .username("jdoe").firstName("John").lastName("Doe")
                    .email("john14@email.org")
                    .build()));
            Assertions.assertNotNull(userId, "User not created");
            getCleanup().addUserId(userId);
        } finally {
            setLDAPEnabled(true);
        }

        List<UserRepresentation> search = managedRealm.admin().users()
                .search("john14@email.org", null, null)
                .stream().collect(Collectors.toList());
        Assertions.assertEquals(1, search.size(), "Incorrect users found");
        Assertions.assertEquals("jdoe", search.get(0).getUsername(), "Incorrect User");
        Assertions.assertTrue(managedRealm.admin().users().search("john", true).isEmpty(), "Duplicated user created");
    }

    @Test
    public void testSearchByUserAttributeDoesNotTriggerUserReimport() {

        testingClient.server().run(session -> {
            // add a new user for testing that searching by attributes should not cause the user to be re-imported.
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "bwayne", "Bruce", "Wayne", "bwayne@waynecorp.com", "Gotham Avenue", "666");
        });

        testingClient.server(TEST_REALM_NAME).run(session -> {
            // check the user doesn't yet exist in Keycloak
            UserProvider localProvider = ((DefaultDatastoreProvider) session.getProvider(DatastoreProvider.class)).userLocalStorage();
            UserModel user = localProvider.getUserByUsername(session.getContext().getRealm(), "bwayne");
            Assertions.assertNull(user);

            // import the user by searching for its username, and check it has the timestamp set by one of the LDAP mappers.
            user = session.users().getUserByUsername(session.getContext().getRealm(), "bwayne");
            Assertions.assertNotNull(user);
            Assertions.assertNotNull(user.getAttributes().get("createTimestamp"));

            // remove the create timestamp from the user.
            user.removeAttribute("createTimestamp");
            user = localProvider.getUserByUsername(session.getContext().getRealm(), "bwayne");
            Assertions.assertNull(user.getAttributes().get("createTimestamp"));
        });

        testingClient.server(TEST_REALM_NAME).run(session -> {
            // search users by user attribute - the existing user SHOULD NOT be re-imported (GHI #32870)
            List<UserModel> users = session.users().searchForUserByUserAttributeStream(session.getContext().getRealm(), "street", "Gotham Avenue").toList();
            Assertions.assertEquals(1, users.size());
            UserModel user = users.get(0);
            // create timestamp won't be null because it is provided directly from the LDAP mapper, so it should still be visible.
            Assertions.assertNotNull(user.getAttributes().get("createTimestamp"));

            // however, the local stored attribute should not have been updated (i.e. user should not have been fully re-imported).
            UserProvider localProvider = ((DefaultDatastoreProvider) session.getProvider(DatastoreProvider.class)).userLocalStorage();
            user = localProvider.getUserByUsername(session.getContext().getRealm(), "bwayne");
            Assertions.assertNull(user.getAttributes().get("createTimestamp"));
        });
    }

    private void setLDAPEnabled(final boolean enabled) {
        testingClient.server().run((KeycloakSession session) -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle("enabled", Boolean.toString(enabled));
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }

    private void assertLDAPSearchMatchesLocalDB(String searchString) {
        //this call should import some users into local database
        List<String> importedUsers = managedRealm.admin().users().search(searchString, null, null).stream().map(UserRepresentation::getUsername).collect(Collectors.toList());

        //this should query local db
        List<String> search = managedRealm.admin().users().search(searchString, null, null).stream().map(UserRepresentation::getUsername).collect(Collectors.toList());

        assertThat(search, containsInAnyOrder(importedUsers.toArray()));
    }
}
