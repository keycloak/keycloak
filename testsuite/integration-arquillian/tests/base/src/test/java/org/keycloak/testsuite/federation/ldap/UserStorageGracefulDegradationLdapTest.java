/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.federation.ldap;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Test graceful degradation with real embedded LDAP server that gets unreachable.
 * Tests realistic scenario where LDAP infrastructure fails during operations.
 */
public class UserStorageGracefulDegradationLdapTest extends AbstractLDAPTest {

    @Rule
    public LDAPRule ldapRule = new LDAPRule()
            .assumeTrue(LDAPTestConfiguration::isStartEmbeddedLdapServer);

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
    }

    @Test
    @ModelTest
    public void testGracefulDegradationWhenLdapGoesDown(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm("test");

        // Create some local users
        UserModel localAdmin = session.users().addUser(realm, "local-admin");
        localAdmin.setEnabled(true);
        
        UserModel localUser = session.users().addUser(realm, "local-user");
        localUser.setEnabled(true);

        try {
            // First create an LDAP user to test with
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject johnLdap = LDAPTestUtils.addLDAPUser(ldapProvider, realm, "john", "John", "Doe", "john@example.com", null, "1234");
            
            // Verify LDAP is working by searching for LDAP users
            Map<String, String> searchParams = new HashMap<>();
            searchParams.put(UserModel.SEARCH, "john");
            Stream<UserModel> users = session.users().searchForUserStream(realm, searchParams, 0, 10);
            List<String> usernames = users.map(UserModel::getUsername).toList();
            
            // Should find LDAP user "john" when LDAP is up
            Assert.assertTrue("Should find john from LDAP when server is up", usernames.contains("john"));

            // Simulate LDAP going down by changing connection URL to invalid server
            ldapModel.getConfig().putSingle("connectionUrl", "ldap://invalid-server:999");
            realm.updateComponent(ldapModel);

            // This search should gracefully degrade and return local users only
            searchParams.clear();
            searchParams.put(UserModel.SEARCH, "local");
            
            users = session.users().searchForUserStream(realm, searchParams, 0, 10);
            usernames = users.map(UserModel::getUsername).toList();
            
            // Should gracefully return local users despite LDAP being down
            Assert.assertTrue("Should find local user despite LDAP down",
                            usernames.contains("local-user"));
            Assert.assertTrue("Should find local admin despite LDAP down",
                            usernames.contains("local-admin"));

        } finally {
            // Clean up - remove users (LDAP provider will be cleaned up by AbstractLDAPTest)
            session.users().removeUser(realm, localAdmin);
            session.users().removeUser(realm, localUser);
        }
    }
}
