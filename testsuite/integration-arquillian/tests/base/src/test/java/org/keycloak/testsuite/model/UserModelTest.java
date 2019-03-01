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

import com.google.common.collect.ImmutableMap;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.*;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserModelTest extends AbstractTestRealmKeycloakTest {

    @Deployment
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class, UserModelTest.class)
                .addPackages(true,
                        "org.keycloak.testsuite",
                        "org.keycloak.testsuite.model",
                        "com.google.common");
    }

    @After
    public void after() {
        testingClient.server().run(session -> {

            RealmModel realm = session.realms().getRealmByName("original");

            if (realm != null) {
                session.sessions().removeUserSessions(realm);
                UserModel user = session.users().getUserByUsername("user", realm);
                UserModel user1 = session.users().getUserByUsername("user1", realm);
                UserModel user2 = session.users().getUserByUsername("user2", realm);
                UserModel user3 = session.users().getUserByUsername("user3", realm);

                UserManager um = new UserManager(session);
                if (user != null) {
                    um.removeUser(realm, user);
                }
                if (user1 != null) {
                    um.removeUser(realm, user1);
                }
                if (user2 != null) {
                    um.removeUser(realm, user2);
                }
                if (user3 != null) {
                    um.removeUser(realm, user3);
                }
                session.realms().removeRealm(realm.getId());
            }
        });
    }

    @Test
    @ModelTest
    public void persistUser(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesPersistUser) -> {
            KeycloakSession currentSession = sesPersistUser;
            RealmModel realm = currentSession.realms().createRealm("original");

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
            UserModel persisted = currentSession.users().getUserByUsername("user", searchRealm);

            assertEquals(user, persisted);

            searchRealm = currentSession.realms().getRealm(realm.getId());
            UserModel persisted2 = currentSession.users().getUserById(user.getId(), searchRealm);
            assertEquals(user, persisted2);

            Map<String, String> attributes = new HashMap<>();
            attributes.put(UserModel.LAST_NAME, "last-name");
            List<UserModel> search = currentSession.users().searchForUser(attributes, realm);
            Assert.assertEquals(search.size(), 1);
            Assert.assertEquals(search.get(0).getUsername(), "user");

            attributes.clear();
            attributes.put(UserModel.EMAIL, "email");
            search = currentSession.users().searchForUser(attributes, realm);
            Assert.assertEquals(search.size(), 1);
            Assert.assertEquals(search.get(0).getUsername(), "user");

            attributes.clear();
            attributes.put(UserModel.LAST_NAME, "last-name");
            attributes.put(UserModel.EMAIL, "email");
            search = currentSession.users().searchForUser(attributes, realm);
            Assert.assertEquals(search.size(), 1);
            Assert.assertEquals(search.get(0).getUsername(), "user");
        });
    }

    @Test
    @ModelTest
    public void webOriginSetTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesWebOrigin) -> {
            KeycloakSession currentSession = sesWebOrigin;
            RealmModel realm = currentSession.realms().createRealm("original");

            ClientModel client = realm.addClient("user");

            Assert.assertTrue(client.getWebOrigins().isEmpty());

            client.addWebOrigin("origin-1");
            Assert.assertEquals(1, client.getWebOrigins().size());

            client.addWebOrigin("origin-2");
            Assert.assertEquals(2, client.getWebOrigins().size());

            client.removeWebOrigin("origin-2");
            Assert.assertEquals(1, client.getWebOrigins().size());

            client.removeWebOrigin("origin-1");
            Assert.assertTrue(client.getWebOrigins().isEmpty());

            client = realm.addClient("oauthclient2");

            Assert.assertTrue(client.getWebOrigins().isEmpty());

            client.addWebOrigin("origin-1");
            Assert.assertEquals(1, client.getWebOrigins().size());

            client.addWebOrigin("origin-2");
            Assert.assertEquals(2, client.getWebOrigins().size());

            client.removeWebOrigin("origin-2");
            Assert.assertEquals(1, client.getWebOrigins().size());

            client.removeWebOrigin("origin-1");
            Assert.assertTrue(client.getWebOrigins().isEmpty());
        });
    }

    @Test
    @ModelTest
    public void testUserRequiredActions(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUserReqActions) -> {
            KeycloakSession currentSession = sesUserReqActions;
            RealmModel realm = currentSession.realms().createRealm("original");

            UserModel user = currentSession.users().addUser(realm, "user");

            Assert.assertTrue(user.getRequiredActions().isEmpty());

            user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
            String id = realm.getId();

            realm = currentSession.realms().getRealm(id);
            user = currentSession.users().getUserByUsername("user", realm);

            Assert.assertEquals(1, user.getRequiredActions().size());
            Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));

            user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
            user = currentSession.users().getUserByUsername("user", realm);

            Assert.assertEquals(1, user.getRequiredActions().size());
            Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));

            user.addRequiredAction(RequiredAction.VERIFY_EMAIL.name());
            user = currentSession.users().getUserByUsername("user", realm);

            Assert.assertEquals(2, user.getRequiredActions().size());
            Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));
            Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

            user.removeRequiredAction(RequiredAction.CONFIGURE_TOTP.name());
            user = currentSession.users().getUserByUsername("user", realm);

            Assert.assertEquals(1, user.getRequiredActions().size());
            Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

            user.removeRequiredAction(RequiredAction.VERIFY_EMAIL.name());
            user = currentSession.users().getUserByUsername("user", realm);

            Assert.assertTrue(user.getRequiredActions().isEmpty());
        });
    }

    @Test
    @ModelTest
    public void testUserMultipleAttributes(KeycloakSession session) throws Exception {
        AtomicReference<List<String>> attrValsAtomic = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesMultipleAtr1) -> {
            KeycloakSession currentSession = sesMultipleAtr1;
            RealmModel realm = currentSession.realms().createRealm("original");

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
            UserModel user = currentSession.users().getUserByUsername("user", realm);

            List<String> attrVals = attrValsAtomic.get();

            attrVals = user.getAttribute("key1");
            Assert.assertEquals(1, attrVals.size());
            Assert.assertEquals("value1", attrVals.get(0));
            Assert.assertEquals("value1", user.getFirstAttribute("key1"));

            attrVals = user.getAttribute("key2");
            Assert.assertEquals(2, attrVals.size());
            Assert.assertTrue(attrVals.contains("val21"));
            Assert.assertTrue(attrVals.contains("val22"));

            attrVals = user.getAttribute("key3");
            Assert.assertTrue(attrVals.isEmpty());
            Assert.assertNull(user.getFirstAttribute("key3"));

            Map<String, List<String>> allAttrVals = user.getAttributes();
            Assert.assertEquals(2, allAttrVals.size());
            Assert.assertEquals(allAttrVals.get("key1"), user.getAttribute("key1"));
            Assert.assertEquals(allAttrVals.get("key2"), user.getAttribute("key2"));

            // Test remove and rewrite attribute
            user.removeAttribute("key1");
            user.setSingleAttribute("key2", "val23");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesMultipleAtr3) -> {
            KeycloakSession currentSession = sesMultipleAtr3;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user = currentSession.users().getUserByUsername("user", realm);
            Assert.assertNull(user.getFirstAttribute("key1"));

            List<String> attrVals = attrValsAtomic.get();
            attrVals = user.getAttribute("key2");

            Assert.assertEquals(1, attrVals.size());
            Assert.assertEquals("val23", attrVals.get(0));

        });
    }

    // KEYCLOAK-3494
    @Test
    @ModelTest
    public void testUpdateUserAttribute(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUpdateAtr1) -> {
            KeycloakSession currentSession = sesUpdateAtr1;
            RealmModel realm = currentSession.realms().createRealm("original");

            UserModel user = currentSession.users().addUser(realm, "user");

            user.setSingleAttribute("key1", "value1");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUpdateAtr2) -> {
            KeycloakSession currentSession = sesUpdateAtr2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user = currentSession.users().getUserByUsername("user", realm);

            // Update attribute
            List<String> attrVals = new ArrayList<>(Arrays.asList("val2"));
            user.setAttribute("key1", attrVals);
            Map<String, List<String>> allAttrVals = user.getAttributes();

            // Ensure same transaction is able to see updated value
            Assert.assertEquals(1, allAttrVals.size());
            Assert.assertEquals(allAttrVals.get("key1"), Arrays.asList("val2"));

        });
    }

    // KEYCLOAK-3608
    @Test
    @ModelTest
    public void testUpdateUserSingleAttribute(KeycloakSession session) {

        AtomicReference<Map<String, List<String>>> expectedAtomic = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUpdateUserSingleAtr) -> {
            KeycloakSession currentSession = sesUpdateUserSingleAtr;
            RealmModel realm = currentSession.realms().createRealm("original");


            Map<String, List<String>> expected = ImmutableMap.of(
                    "key1", Arrays.asList("value3"),
                    "key2", Arrays.asList("value2"));
            expectedAtomic.set(expected);

            UserModel user = currentSession.users().addUser(realm, "user");

            user.setSingleAttribute("key1", "value1");
            user.setSingleAttribute("key2", "value2");

            // Overwrite the first attribute
            user.setSingleAttribute("key1", "value3");

            Assert.assertEquals(expected, user.getAttributes());
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUpdateUserSingleAtr2) -> {
            KeycloakSession currentSession = sesUpdateUserSingleAtr2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            Map<String, List<String>> expected = expectedAtomic.get();
            Assert.assertEquals(expected, currentSession.users().getUserByUsername("user", realm).getAttributes());

        });
    }

    @Test
    @ModelTest
    public void testSearchByString(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesSearchString1) -> {
            KeycloakSession currentSession = sesSearchString1;
            RealmModel realm = currentSession.realms().createRealm("original");

            currentSession.users().addUser(realm, "user1");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesSearchString1) -> {
            KeycloakSession currentSession = sesSearchString1;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername("user1", realm);

            List<UserModel> users = currentSession.users().searchForUser("user", realm, 0, 7);
            Assert.assertTrue(users.contains(user1));
        });
    }

    @Test
    @ModelTest
    public void testSearchByUserAttribute(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesSearchAtr1) -> {
            KeycloakSession currentSession = sesSearchAtr1;
            RealmModel realm = currentSession.realms().createRealm("original");

            UserModel user1 = currentSession.users().addUser(realm, "user1");
            UserModel user2 = currentSession.users().addUser(realm, "user2");
            UserModel user3 = currentSession.users().addUser(realm, "user3");
            RealmModel otherRealm = currentSession.realms().createRealm("other");
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

            UserModel user1 = currentSession.users().getUserByUsername("user1", realm);
            UserModel user2 = currentSession.users().getUserByUsername("user2", realm);
            UserModel user3 = currentSession.users().getUserByUsername("user3", realm);

            List<UserModel> users = currentSession.users().searchForUserByUserAttribute("key1", "value1", realm);
            Assert.assertEquals(2, users.size());
            Assert.assertTrue(users.contains(user1));
            Assert.assertTrue(users.contains(user2));

            users = currentSession.users().searchForUserByUserAttribute("key2", "value21", realm);
            Assert.assertEquals(2, users.size());
            Assert.assertTrue(users.contains(user1));
            Assert.assertTrue(users.contains(user3));

            users = currentSession.users().searchForUserByUserAttribute("key2", "value22", realm);
            Assert.assertEquals(1, users.size());
            Assert.assertTrue(users.contains(user2));

            users = currentSession.users().searchForUserByUserAttribute("key3", "value3", realm);
            Assert.assertEquals(0, users.size());
        });
    }

    @Test
    @ModelTest
    public void testServiceAccountLink(KeycloakSession session) throws Exception {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesServiceLink1) -> {
            KeycloakSession currentSession = sesServiceLink1;
            RealmModel realm = currentSession.realms().createRealm("original");

            ClientModel client = realm.addClient("foo");

            UserModel user1 = currentSession.users().addUser(realm, "user1");
            user1.setFirstName("John");
            user1.setLastName("Doe");

            UserModel user2 = currentSession.users().addUser(realm, "user2");
            user2.setFirstName("John");
            user2.setLastName("Doe");

            // Search
            Assert.assertNull(currentSession.users().getServiceAccount(client));
            List<UserModel> users = currentSession.users().searchForUser("John Doe", realm);
            Assert.assertEquals(2, users.size());
            Assert.assertTrue(users.contains(user1));
            Assert.assertTrue(users.contains(user2));

            // Link service account
            user1.setServiceAccountClientLink(client.getId());
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesServiceLink2) -> {
            KeycloakSession currentSession = sesServiceLink2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername("user1", realm);
            UserModel user2 = currentSession.users().getUserByUsername("user2", realm);

            // Search and assert service account user not found
            ClientModel client = realm.getClientByClientId("foo");
            UserModel searched = currentSession.users().getServiceAccount(client);
            Assert.assertEquals(searched, user1);
            List<UserModel> users = currentSession.users().searchForUser("John Doe", realm);
            Assert.assertEquals(1, users.size());
            Assert.assertFalse(users.contains(user1));
            Assert.assertTrue(users.contains(user2));

            users = currentSession.users().getUsers(realm, false);
            Assert.assertEquals(1, users.size());
            Assert.assertFalse(users.contains(user1));
            Assert.assertTrue(users.contains(user2));

            users = currentSession.users().getUsers(realm, true);
            Assert.assertEquals(2, users.size());
            Assert.assertTrue(users.contains(user1));
            Assert.assertTrue(users.contains(user2));

            Assert.assertEquals(2, currentSession.users().getUsersCount(realm, true));
            Assert.assertEquals(1, currentSession.users().getUsersCount(realm, false));

            // Remove client
            RealmManager realmMgr = new RealmManager(currentSession);
            ClientManager clientMgr = new ClientManager(realmMgr);

            clientMgr.removeClient(realm, client);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesServiceLink3) -> {
            KeycloakSession currentSession = sesServiceLink3;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            // Assert service account removed as well
            Assert.assertNull(currentSession.users().getUserByUsername("user1", realm));
        });
    }

    @Test
    @ModelTest
    public void testGrantToAll(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesGrantToAll1) -> {
            KeycloakSession currentSession = sesGrantToAll1;

            if (currentSession.realms().getRealm("realm1") != null)
                currentSession.realms().removeRealm("realm1");

            RealmModel realm1 = currentSession.realms().createRealm("realm1");

            realm1.addRole("role1");
            currentSession.users().addUser(realm1, "user1");
            currentSession.users().addUser(realm1, "user2");

            if (currentSession.realms().getRealm("realm2") != null)
                currentSession.realms().removeRealm("realm2");

            RealmModel realm2 = currentSession.realms().createRealm("realm2");
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
            UserModel user1 = currentSession.users().getUserByUsername("user1", realm1);
            UserModel user2 = currentSession.users().getUserByUsername("user2", realm1);
            Assert.assertTrue(user1.hasRole(role1));
            Assert.assertTrue(user2.hasRole(role1));

            RealmModel realm2 = currentSession.realms().getRealmByName("realm2");
            UserModel realm2User1 = currentSession.users().getUserByUsername("user1", realm2);
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
            RealmModel realm = currentSession.realms().createRealm("original");

            UserModel user1 = currentSession.users().addUser(realm, "user1");
            currentSession.users().setNotBeforeForUser(realm, user1, 10);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUserNotBefore2) -> {
            KeycloakSession currentSession = sesUserNotBefore2;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername("user1", realm);
            int notBefore = currentSession.users().getNotBeforeOfUser(realm, user1);
            Assert.assertEquals(10, notBefore);

            // Try to update
            currentSession.users().setNotBeforeForUser(realm, user1, 20);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesUserNotBefore3) -> {
            KeycloakSession currentSession = sesUserNotBefore3;
            RealmModel realm = currentSession.realms().getRealmByName("original");

            UserModel user1 = currentSession.users().getUserByUsername("user1", realm);
            int notBefore = currentSession.users().getNotBeforeOfUser(realm, user1);
            Assert.assertEquals(20, notBefore);
        });
    }

    public static void assertEquals(UserModel expected, UserModel actual) {
        Assert.assertEquals(expected.getUsername(), actual.getUsername());
        Assert.assertEquals(expected.getCreatedTimestamp(), actual.getCreatedTimestamp());
        Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
        Assert.assertEquals(expected.getLastName(), actual.getLastName());

        String[] expectedRequiredActions = expected.getRequiredActions().toArray(new String[expected.getRequiredActions().size()]);
        Arrays.sort(expectedRequiredActions);
        String[] actualRequiredActions = actual.getRequiredActions().toArray(new String[actual.getRequiredActions().size()]);
        Arrays.sort(actualRequiredActions);

        Assert.assertArrayEquals(expectedRequiredActions, actualRequiredActions);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }
}
