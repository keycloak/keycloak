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
import java.util.TreeSet;
import java.util.stream.Collectors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

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

            // Delete all local users and add some new for testing
            session.users().searchForUserStream(appRealm, new HashMap<>()).collect(Collectors.toList()).forEach(u -> session.users().removeUser(appRealm, u));

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john", "Some", "Some", "john14@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john00", "john", "Doe", "john0@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john01", "john", "Doe", "john1@email.org", null, "1234");
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
        LinkedList<String> importedUsers = new LinkedList(adminClient.realm(TEST_REALM_NAME).users().search("*", 0, 5).stream().map(UserRepresentation::getUsername).collect(Collectors.toCollection(TreeSet::new)));

        //this call should ommit first 3 already imported users from local db
        //it should return 2 local(imported) users and 8 users from ldap
        List<String> search = adminClient.realm(TEST_REALM_NAME).users().search("*", 3, 10).stream().map(UserRepresentation::getUsername).collect(Collectors.toList());

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

    private void assertLDAPSearchMatchesLocalDB(String searchString) {
        //this call should import some users into local database
        List<String> importedUsers = adminClient.realm(TEST_REALM_NAME).users().search(searchString, null, null).stream().map(UserRepresentation::getUsername).collect(Collectors.toList());

        //this should query local db
        List<String> search = adminClient.realm(TEST_REALM_NAME).users().search(searchString, null, null).stream().map(UserRepresentation::getUsername).collect(Collectors.toList());

        assertThat(search, containsInAnyOrder(importedUsers.toArray()));
    }
}
