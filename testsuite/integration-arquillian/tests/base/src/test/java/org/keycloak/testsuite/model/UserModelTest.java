/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.util.RealmBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class UserModelTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("original").build());
        testRealms.add(RealmBuilder.create().name("other").build());
        testRealms.add(RealmBuilder.create().name("realm1").build());
        testRealms.add(RealmBuilder.create().name("realm2").build());
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Test
    @ModelTest
    public void persistUser(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesPersistUser) -> {
            KeycloakSession currentSession = sesPersistUser;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user = currentSession.users().addUser(realm, "user");
            user.setFirstName("first-name");
            user.setLastName("last-name");
            user.setEmail("email");
            assertNotNull(user.getCreatedTimestamp());
            // test that timestamp is current with 10s tollerance
            Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);

            user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
            user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);

            RealmModel searchRealm = currentSession.realms().getRealm(realm.getId());
            UserModel persisted = currentSession.users().getUserByUsername(searchRealm, "user");

            assertUserModel(user, persisted);

            searchRealm = currentSession.realms().getRealm(realm.getId());
            UserModel persisted2 = currentSession.users().getUserById(searchRealm, user.getId());
            assertUserModel(user, persisted2);

            Map<String, String> attributes = new HashMap<>();
            attributes.put(UserModel.LAST_NAME, "last-name");
            List<UserModel> search = currentSession.users().searchForUserStream(realm, attributes)
                    .collect(Collectors.toList());
            Assert.assertThat(search, hasSize(1));
            Assert.assertThat(search.get(0).getUsername(), equalTo("user"));

            attributes.clear();
            attributes.put(UserModel.EMAIL, "email");
            search = currentSession.users().searchForUserStream(realm, attributes)
                    .collect(Collectors.toList());
            Assert.assertThat(search, hasSize(1));
            Assert.assertThat(search.get(0).getUsername(), equalTo("user"));

            attributes.clear();
            attributes.put(UserModel.LAST_NAME, "last-name");
            attributes.put(UserModel.EMAIL, "email");
            search = currentSession.users().searchForUserStream(realm, attributes).collect(Collectors.toList());
            Assert.assertThat(search, hasSize(1));
            Assert.assertThat(search.get(0).getUsername(), equalTo("user"));
        });
    }

    @Test
    @ModelTest
    public void webOriginSetTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesWebOrigin) -> {
            KeycloakSession currentSession = sesWebOrigin;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            ClientModel client = realm.addClient("user");

            Assert.assertThat(client.getWebOrigins(), empty());

            client.addWebOrigin("origin-1");
            Assert.assertThat(client.getWebOrigins(), hasSize(1));

            client.addWebOrigin("origin-2");
            Assert.assertThat(client.getWebOrigins(), hasSize(2));

            client.removeWebOrigin("origin-2");
            Assert.assertThat(client.getWebOrigins(), hasSize(1));

            client.removeWebOrigin("origin-1");
            Assert.assertThat(client.getWebOrigins(), empty());

            client = realm.addClient("oauthclient2");

            Assert.assertThat(client.getWebOrigins(), empty());

            client.addWebOrigin("origin-1");
            Assert.assertThat(client.getWebOrigins(), hasSize(1));

            client.addWebOrigin("origin-2");
            Assert.assertThat(client.getWebOrigins(), hasSize(2));

            client.removeWebOrigin("origin-2");
            Assert.assertThat(client.getWebOrigins(), hasSize(1));

            client.removeWebOrigin("origin-1");
            Assert.assertThat(client.getWebOrigins(), empty());
        });
    }

    @Test
    @ModelTest
    public void testUserRequiredActions(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUserReqActions) -> {
            KeycloakSession currentSession = sesUserReqActions;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user = currentSession.users().addUser(realm, "user");
            List<String> requiredActions = user.getRequiredActionsStream().collect(Collectors.toList());
            Assert.assertThat(requiredActions, empty());

            user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
            String id = realm.getId();

            realm = currentSession.realms().getRealm(id);
            user = currentSession.users().getUserByUsername(realm, "user");

            requiredActions = user.getRequiredActionsStream().collect(Collectors.toList());
            Assert.assertThat(requiredActions, hasSize(1));
            Assert.assertThat(requiredActions, contains(RequiredAction.CONFIGURE_TOTP.name()));
            
            user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
            user = currentSession.users().getUserByUsername(realm, "user");

            requiredActions = user.getRequiredActionsStream().collect(Collectors.toList());
            Assert.assertThat(requiredActions, hasSize(1));
            Assert.assertThat(requiredActions, contains(RequiredAction.CONFIGURE_TOTP.name()));

            user.addRequiredAction(RequiredAction.VERIFY_EMAIL.name());
            user = currentSession.users().getUserByUsername(realm, "user");

            requiredActions = user.getRequiredActionsStream().collect(Collectors.toList());
            Assert.assertThat(requiredActions, hasSize(2));
            Assert.assertThat(requiredActions, containsInAnyOrder(
                    RequiredAction.CONFIGURE_TOTP.name(), 
                    RequiredAction.VERIFY_EMAIL.name())
            );

            user.removeRequiredAction(RequiredAction.CONFIGURE_TOTP.name());
            user = currentSession.users().getUserByUsername(realm, "user");

            requiredActions = user.getRequiredActionsStream().collect(Collectors.toList());
            Assert.assertThat(requiredActions, hasSize(1));
            Assert.assertThat(requiredActions, contains(RequiredAction.VERIFY_EMAIL.name()));

            user.removeRequiredAction(RequiredAction.VERIFY_EMAIL.name());
            user = currentSession.users().getUserByUsername(realm, "user");

            requiredActions = user.getRequiredActionsStream().collect(Collectors.toList());
            Assert.assertThat(requiredActions, empty());
        });
    }

    @Test
    @ModelTest
    public void testUserMultipleAttributes(KeycloakSession session) throws Exception {
        AtomicReference<List<String>> attrValsAtomic = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesMultipleAtr1) -> {
            KeycloakSession currentSession = sesMultipleAtr1;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user = currentSession.users().addUser(realm, "user");
            currentSession.users().addUser(realm, "user-noattrs");

            user.setSingleAttribute("key1", "value1");

            List<String> attrVals = new ArrayList<>(Arrays.asList("val21", "val22"));
            attrValsAtomic.set(attrVals);

            user.setAttribute("key2", attrVals);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesMultipleAtr2) -> {
            KeycloakSession currentSession = sesMultipleAtr2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            // Test read attributes
            UserModel user = currentSession.users().getUserByUsername(realm, "user");

            List<String> attrVals = user.getAttributeStream("key1").collect(Collectors.toList());
            Assert.assertThat(attrVals, hasSize(1));
            Assert.assertThat(attrVals, contains("value1"));
            Assert.assertThat(user.getFirstAttribute("key1"), equalTo("value1"));

            attrVals = user.getAttributeStream("key2").collect(Collectors.toList());
            Assert.assertThat(attrVals, hasSize(2));
            Assert.assertThat(attrVals, containsInAnyOrder("val21", "val22"));

            attrVals = user.getAttributeStream("key3").collect(Collectors.toList());
            Assert.assertThat(attrVals, empty());
            Assert.assertThat(user.getFirstAttribute("key3"), nullValue());

            Map<String, List<String>> allAttrVals = user.getAttributes();
            Assert.assertThat(allAttrVals.keySet(), hasSize(6));
            Assert.assertThat(allAttrVals.keySet(), containsInAnyOrder(UserModel.USERNAME, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.EMAIL, "key1", "key2"));
            Assert.assertThat(allAttrVals.get("key1"), equalTo(user.getAttributeStream("key1").collect(Collectors.toList())));
            Assert.assertThat(allAttrVals.get("key2"), equalTo(user.getAttributeStream("key2").collect(Collectors.toList())));

            // Test remove and rewrite attribute
            user.removeAttribute("key1");
            user.setSingleAttribute("key2", "val23");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesMultipleAtr3) -> {
            KeycloakSession currentSession = sesMultipleAtr3;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user = currentSession.users().getUserByUsername(realm, "user");
            Assert.assertThat(user.getFirstAttribute("key1"), nullValue());

            List<String> attrVals = user.getAttributeStream("key2").collect(Collectors.toList());

            Assert.assertThat(attrVals, hasSize(1));
            Assert.assertThat(attrVals.get(0), equalTo("val23"));
        });
    }

    // KEYCLOAK-3494
    @Test
    @ModelTest
    public void testUpdateUserAttribute(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUpdateAtr1) -> {
            KeycloakSession currentSession = sesUpdateAtr1;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user = currentSession.users().addUser(realm, "user");

            user.setSingleAttribute("key1", "value1");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUpdateAtr2) -> {
            KeycloakSession currentSession = sesUpdateAtr2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user = currentSession.users().getUserByUsername(realm, "user");

            // Update attribute
            List<String> attrVals = new ArrayList<>(Arrays.asList("val2"));
            user.setAttribute("key1", attrVals);
            Map<String, List<String>> allAttrVals = user.getAttributes();

            // Ensure same transaction is able to see updated value
            Assert.assertThat(allAttrVals.keySet(), hasSize(5));
            Assert.assertThat(allAttrVals.keySet(), containsInAnyOrder("key1", UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.EMAIL, UserModel.USERNAME));
            Assert.assertThat(allAttrVals.get("key1"), contains("val2"));
        });
    }

    // KEYCLOAK-3608
    @Test
    @ModelTest
    public void testUpdateUserSingleAttribute(KeycloakSession session) {

        AtomicReference<Map<String, List<String>>> expectedAtomic = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUpdateUserSingleAtr) -> {
            KeycloakSession currentSession = sesUpdateUserSingleAtr;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            Map<String, List<String>> expected = new HashMap<>();
            expected.put("key1", Collections.singletonList("value3"));
            expected.put("key2", Collections.singletonList("value2"));
            expected.put(UserModel.FIRST_NAME, Collections.singletonList(null));
            expected.put(UserModel.LAST_NAME, Collections.singletonList(null));
            expected.put(UserModel.EMAIL, Collections.singletonList(null));
            expected.put(UserModel.USERNAME, Collections.singletonList("user"));

            UserModel user = currentSession.users().addUser(realm, "user");

            user.setSingleAttribute("key1", "value1");
            user.setSingleAttribute("key2", "value2");
            user.setSingleAttribute("key3", null); //KEYCLOAK-7014

            // Overwrite the first attribute
            user.setSingleAttribute("key1", "value3");

            Assert.assertThat(user.getAttributes(), equalTo(expected));

            expectedAtomic.set(expected);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUpdateUserSingleAtr2) -> {
            KeycloakSession currentSession = sesUpdateUserSingleAtr2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            Map<String, List<String>> expected = expectedAtomic.get();
            Assert.assertThat(currentSession.users().getUserByUsername(realm, "user").getAttributes(), equalTo(expected));
        });
    }

    @Test
    @ModelTest
    public void testSearchByString(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesSearchString1) -> {
            KeycloakSession currentSession = sesSearchString1;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            currentSession.users().addUser(realm, "user1");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesSearchString1) -> {
            KeycloakSession currentSession = sesSearchString1;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");

            List<UserModel> users = currentSession.users().searchForUserStream(realm, "user", 0, 7)
                    .collect(Collectors.toList());
            Assert.assertThat(users, hasSize(1));
            Assert.assertThat(users, contains(user1));
        });
    }

    @Test
    @ModelTest
    public void testSearchByUserAttribute(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesSearchAtr1) -> {
            KeycloakSession currentSession = sesSearchAtr1;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().addUser(realm, "user1");
            UserModel user2 = currentSession.users().addUser(realm, "user2");
            UserModel user3 = currentSession.users().addUser(realm, "user3");
            RealmModel otherRealm = currentSession.realms().getRealmByName("other");
            UserModel otherRealmUser = currentSession.users().addUser(otherRealm, "user1");

            user1.setSingleAttribute("key1", "value1");
            user1.setSingleAttribute("key2", "value21");

            user2.setSingleAttribute("key1", "value1");
            user2.setSingleAttribute("key2", "value22");

            user3.setSingleAttribute("key2", "value21");

            otherRealmUser.setSingleAttribute("key2", "value21");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesSearchAtr2) -> {
            KeycloakSession currentSession = sesSearchAtr2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");
            UserModel user2 = currentSession.users().getUserByUsername(realm, "user2");
            UserModel user3 = currentSession.users().getUserByUsername(realm, "user3");

            List<UserModel> users = currentSession.users().searchForUserByUserAttributeStream(realm, "key1", "value1")
                    .collect(Collectors.toList());
            Assert.assertThat(users, hasSize(2));
            Assert.assertThat(users, containsInAnyOrder(user1, user2));

            users = currentSession.users().searchForUserByUserAttributeStream(realm, "key2", "value21")
                    .collect(Collectors.toList());
            Assert.assertThat(users, hasSize(2));
            Assert.assertThat(users, containsInAnyOrder(user1, user3));

            users = currentSession.users().searchForUserByUserAttributeStream(realm, "key2", "value22")
                    .collect(Collectors.toList());
            Assert.assertThat(users, hasSize(1));
            Assert.assertThat(users, contains(user2));

            users = currentSession.users().searchForUserByUserAttributeStream(realm, "key3", "value3")
                    .collect(Collectors.toList());
            Assert.assertThat(users, empty());
        });
    }

    @Test
    @ModelTest
    public void testServiceAccountLink(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesServiceLink1) -> {
            KeycloakSession currentSession = sesServiceLink1;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            ClientModel client = realm.addClient("foo");

            UserModel user1 = currentSession.users().addUser(realm, "user1");
            user1.setFirstName("John");
            user1.setLastName("Doe");

            UserModel user2 = currentSession.users().addUser(realm, "user2");
            user2.setFirstName("John");
            user2.setLastName("Doe");

            // Search
            Assert.assertThat(currentSession.users().getServiceAccount(client), nullValue());
            List<UserModel> users = currentSession.users().searchForUserStream(realm, "John Doe")
                    .collect(Collectors.toList());
            Assert.assertThat(users, hasSize(2));
            Assert.assertThat(users, containsInAnyOrder(user1, user2));

            // Link service account
            user1.setServiceAccountClientLink(client.getId());
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesServiceLink2) -> {
            KeycloakSession currentSession = sesServiceLink2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");
            UserModel user2 = currentSession.users().getUserByUsername(realm, "user2");

            // Search and assert service account user not found
            ClientModel client = realm.getClientByClientId("foo");
            UserModel searched = currentSession.users().getServiceAccount(client);
            Assert.assertThat(searched, equalTo(user1));
            List<UserModel> users = currentSession.users().searchForUserStream(realm, "John Doe")
                    .collect(Collectors.toList());
            Assert.assertThat(users, hasSize(1));
            Assert.assertThat(users, contains(user2));

            users = currentSession.users().getUsersStream(realm, false).collect(Collectors.toList());
            Assert.assertThat(users, hasSize(1));
            Assert.assertThat(users, contains(user2));

            users = currentSession.users().getUsersStream(realm, true).collect(Collectors.toList());
            Assert.assertThat(users, hasSize(2));
            Assert.assertThat(users, containsInAnyOrder(user1, user2));

            Assert.assertThat(currentSession.users().getUsersCount(realm, true), equalTo(2));
            Assert.assertThat(currentSession.users().getUsersCount(realm, false), equalTo(1));

            // Remove client
            RealmManager realmMgr = new RealmManager(currentSession);
            ClientManager clientMgr = new ClientManager(realmMgr);

            clientMgr.removeClient(realm, client);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesServiceLink3) -> {
            KeycloakSession currentSession = sesServiceLink3;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            // Assert service account removed as well
            Assert.assertThat(currentSession.users().getUserByUsername(realm, "user1"), nullValue());
        });
    }

    @Test
    @ModelTest
    public void testGrantToAll(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesGrantToAll1) -> {
            KeycloakSession currentSession = sesGrantToAll1;

            RealmModel realm1 = currentSession.realms().getRealmByName("realm1");

            realm1.addRole("role1");
            currentSession.users().addUser(realm1, "user1");
            currentSession.users().addUser(realm1, "user2");

            RealmModel realm2 = currentSession.realms().getRealmByName("realm2");
            currentSession.users().addUser(realm2, "user1");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesGrantToAll2) -> {
            KeycloakSession currentSession = sesGrantToAll2;
            RealmModel realm1 = currentSession.realms().getRealmByName("realm1");

            RoleModel role1 = realm1.getRole("role1");
            currentSession.users().grantToAllUsers(realm1, role1);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesGrantToAll2) -> {
            KeycloakSession currentSession = sesGrantToAll2;
            RealmModel realm1 = currentSession.realms().getRealmByName("realm1");

            RoleModel role1 = realm1.getRole("role1");
            UserModel user1 = currentSession.users().getUserByUsername(realm1, "user1");
            UserModel user2 = currentSession.users().getUserByUsername(realm1, "user2");
            Assert.assertTrue(user1.hasRole(role1));
            Assert.assertTrue(user2.hasRole(role1));

            RealmModel realm2 = currentSession.realms().getRealmByName("realm2");
            UserModel realm2User1 = currentSession.users().getUserByUsername(realm2, "user1");
            Assert.assertFalse(realm2User1.hasRole(role1));

            currentSession.realms().removeRealm(realm1.getId());
            currentSession.realms().removeRealm(realm2.getId());
        });
    }

    @Test
    @ModelTest
    public void testUserNotBefore(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUserNotBefore1) -> {
            KeycloakSession currentSession = sesUserNotBefore1;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().addUser(realm, "user1");
            currentSession.users().setNotBeforeForUser(realm, user1, 10);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUserNotBefore2) -> {
            KeycloakSession currentSession = sesUserNotBefore2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");
            int notBefore = currentSession.users().getNotBeforeOfUser(realm, user1);
            Assert.assertThat(notBefore, equalTo(10));

            // Try to update
            currentSession.users().setNotBeforeForUser(realm, user1, 20);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUserNotBefore3) -> {
            KeycloakSession currentSession = sesUserNotBefore3;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");
            int notBefore = currentSession.users().getNotBeforeOfUser(realm, user1);
            Assert.assertThat(notBefore, equalTo(20));
        });
    }

    private static void assertUserModel(UserModel expected, UserModel actual) {
        Assert.assertThat(actual.getUsername(), equalTo(expected.getUsername()));
        Assert.assertThat(actual.getCreatedTimestamp(), equalTo(expected.getCreatedTimestamp()));
        Assert.assertThat(actual.getFirstName(), equalTo(expected.getFirstName()));
        Assert.assertThat(actual.getLastName(), equalTo(expected.getLastName()));
        Assert.assertThat(actual.getRequiredActionsStream().collect(Collectors.toSet()),
                containsInAnyOrder(expected.getRequiredActionsStream().toArray()));
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }
}
