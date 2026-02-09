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
package org.keycloak.testsuite.federation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;

/**
 * Test graceful degradation when user storage providers fail.
 * Verifies that when LDAP or other external user stores fail, 
 * local users remain accessible.
 */
public class UserStorageGracefulDegradationTest extends AbstractTestRealmKeycloakTest implements Serializable {

    private String failingStorageProviderId;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void addUserStorageProvider() {
        AtomicReference<String> providerIdRef = new AtomicReference<>();
        
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealm("test");

            // Create a failing user storage provider (will be highest priority)
            ComponentModel failingProvider = new ComponentModel();
            failingProvider.setId(KeycloakModelUtils.generateId());
            failingProvider.setName("failing-user-storage");
            failingProvider.setProviderId("failing-user-storage");
            failingProvider.setProviderType(UserStorageProvider.class.getName());
            failingProvider.setParentId(realm.getId());
            failingProvider.getConfig().putSingle("priority", "0"); // Highest priority, will be queried first
            failingProvider.getConfig().putSingle("failOnSearch", "true");
            failingProvider.getConfig().putSingle("failOnCount", "true");
            realm.addComponentModel(failingProvider);
            providerIdRef.set(failingProvider.getId());

            // Create local users in the realm (these should always work)
            UserModel localUser1 = session.users().addUser(realm, "local-user1");
            localUser1.setEnabled(true);
            localUser1.setEmail("local1@example.com");
            
            UserModel localUser2 = session.users().addUser(realm, "local-user2");
            localUser2.setEnabled(true);
            localUser2.setEmail("local2@example.com");
            
            UserModel localUser3 = session.users().addUser(realm, "local-admin");
            localUser3.setEnabled(true);
            localUser3.setEmail("admin@example.com");
        });

        failingStorageProviderId = providerIdRef.get();
    }

    @After
    public void removeUserStorageProvider() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealm("test");
            
            if (failingStorageProviderId != null) {
                ComponentModel model = realm.getComponent(failingStorageProviderId);
                if (model != null) {
                    realm.removeComponent(model);
                }
            }
            
            // Remove local users
            UserModel localUser1 = session.users().getUserByUsername(realm, "local-user1");
            if (localUser1 != null) {
                session.users().removeUser(realm, localUser1);
            }
            
            UserModel localUser2 = session.users().getUserByUsername(realm, "local-user2");
            if (localUser2 != null) {
                session.users().removeUser(realm, localUser2);
            }
            
            UserModel localAdmin = session.users().getUserByUsername(realm, "local-admin");
            if (localAdmin != null) {
                session.users().removeUser(realm, localAdmin);
            }
        });
    }

    @Test
    @ModelTest
    public void testGracefulDegradationOnSearchFailure(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm("test");
        
        // Verify that user search returns results from working providers despite failing provider
        Map<String, String> searchParams = new HashMap<>();
        
        // This should not throw an exception despite the failing provider
        // Instead, it should return users from local storage and working federation provider
        Stream<UserModel> users = session.users().searchForUserStream(realm, searchParams, 0, 10);
        List<String> usernames = users.map(UserModel::getUsername).toList();
        
        // Should contain local users despite failing provider
        assertThat("Should contain our local users", usernames, 
                  hasItems("local-user1", "local-user2", "local-admin"));
        
        // Should have at least our 3 local users
        Assert.assertTrue("Should have at least 3 users, found: " + usernames.size(), usernames.size() >= 3);
    }


    @Test
    @ModelTest
    public void testGracefulDegradationWithSearchString(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm("test");
        
        // Search for users with "user" in the name
        Map<String, String> searchParams = new HashMap<>();
        searchParams.put(UserModel.SEARCH, "user");
        
        // This should not throw an exception despite the failing provider
        // Instead, it should return users from local storage that match the search
        Stream<UserModel> users = session.users().searchForUserStream(realm, searchParams, 0, 10);
        List<String> usernames = users.map(UserModel::getUsername).toList();
        
        // Should find users from working providers that match the search
        Assert.assertFalse("Should find some users matching 'user'", usernames.isEmpty());
        
        // Verify all returned users have "user" in their username
        for (String username : usernames) {
            assertThat("Username should contain 'user'", username.toLowerCase(), containsString("user"));
        }
    }

    @Test
    @ModelTest
    public void testGracefulDegradationOnCountFailure(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm("test");
        
        // Verify that user count works despite failing provider
        Map<String, String> searchParams = new HashMap<>();
        
        // This should throw an exception without graceful degradation, 
        // but with graceful degradation should return only local users
        int userCount = session.users().getUsersCount(realm, searchParams);
        
        // Should count users from local storage despite failing provider
        // At least 3 users: 3 local users
        Assert.assertTrue("User count should be at least 3, but was: " + userCount, userCount >= 3);
    }

}
