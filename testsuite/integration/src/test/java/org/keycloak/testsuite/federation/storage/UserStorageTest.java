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
package org.keycloak.testsuite.federation.storage;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.infinispan.UserAdapter;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStorageTest {
    public static ComponentModel memoryProvider = null;
    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setName("memory");
            model.setPriority(0);
            model.setProviderId(UserMapStorageFactory.PROVIDER_ID);
            model.setParentId(appRealm.getId());
            memoryProvider = appRealm.addComponentModel(model);

            model = new UserStorageProviderModel();
            model.setName("read-only-user-props");
            model.setPriority(1);
            model.setProviderId(UserPropertyFileStorageFactory.PROVIDER_ID);
            model.setParentId(appRealm.getId());
            model.getConfig().putSingle("propertyFile", "/storage-test/read-only-user-password.properties");
            appRealm.addComponentModel(model);
            model = new UserStorageProviderModel();
            model.setName("user-props");
            model.setPriority(2);
            model.setParentId(appRealm.getId());
            model.setProviderId(UserPropertyFileStorageFactory.PROVIDER_ID);
            model.getConfig().putSingle("propertyFile", "/storage-test/user-password.properties");
            model.getConfig().putSingle("federatedStorage", "true");
            appRealm.addComponentModel(model);
        }
    });
    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    private void loginSuccessAndLogout(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
        oauth.openLogout();
    }

    public void loginBadPassword(String username) {
        loginPage.open();
        loginPage.login("username", "badpassword");
        Assert.assertEquals("Invalid username or password.", loginPage.getError());
    }

    @Test
    public void testLoginSuccess() {
        loginSuccessAndLogout("tbrady", "goat");
        loginSuccessAndLogout("thor", "hammer");
        loginBadPassword("tbrady");
    }

    @Test
    public void testUpdate() {
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("test");
        UserModel thor = session.users().getUserByUsername("thor", realm);
        thor.setFirstName("Stian");
        thor.setLastName("Thorgersen");
        thor.setEmailVerified(true);
        long thorCreated = System.currentTimeMillis() - 100;
        thor.setCreatedTimestamp(thorCreated);
        thor.setEmail("thor@hammer.com");
        thor.setSingleAttribute("test-attribute", "value");
        RoleModel role = realm.addRole("foo-role");
        thor.grantRole(role);
        GroupModel group = realm.createGroup("my-group");
        thor.joinGroup(group);
        thor.addRequiredAction("POOP");
        keycloakRule.stopSession(session, true);

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        thor = session.users().getUserByUsername("thor", realm);
        Assert.assertEquals("Stian", thor.getFirstName());
        Assert.assertEquals("Thorgersen", thor.getLastName());
        Assert.assertEquals("thor@hammer.com", thor.getEmail());
        Assert.assertEquals("value", thor.getFirstAttribute("test-attribute"));
        Assert.assertTrue(thor.isEmailVerified());
        Assert.assertTrue(thor instanceof UserAdapter);
        Set<RoleModel> roles = thor.getRoleMappings();
        System.out.println("num roles " + roles.size());
        Assert.assertTrue(roles.size() > 1);
        role = realm.getRole("foo-role");
        Assert.assertTrue(thor.hasRole(role));

        Set<GroupModel> groups = thor.getGroups();
        boolean foundGroup = false;
        for (GroupModel g : groups) {
            if (g.getName().equals("my-group")) foundGroup = true;

        }
        Assert.assertTrue(foundGroup);
        System.out.println("num groups " + groups.size());
        Assert.assertTrue(thor.getRequiredActions().iterator().next().equals("POOP"));
        thor.removeRequiredAction("POOP");
        thor.updateCredential(UserCredentialModel.password("lightning"));
        keycloakRule.stopSession(session, true);
        loginSuccessAndLogout("thor", "lightning");
    }

    @Test
    public void testQuery() {
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("test");

        // Test paging
        List<UserModel> localUsers = session.userLocalStorage().getUsers(realm, false);
        Set<UserModel> queried = new HashSet<>();
        // tests assumes that local storage is queried first
        int first = localUsers.size();
        while (queried.size() < 8) {
            List<UserModel> results = session.users().getUsers(realm, first, 3);
            if (results.size() == 0) break;
            first += results.size();
            queried.addAll(results);

        }
        Set<String> usernames = new HashSet<>();
        for (UserModel user : queried) {
            usernames.add(user.getUsername());
            System.out.println(user.getUsername());

        }
        Assert.assertEquals(8, queried.size());
        Assert.assertTrue(usernames.contains("thor"));
        Assert.assertTrue(usernames.contains("zeus"));
        Assert.assertTrue(usernames.contains("apollo"));
        Assert.assertTrue(usernames.contains("perseus"));
        Assert.assertTrue(usernames.contains("tbrady"));
        Assert.assertTrue(usernames.contains("rob"));
        Assert.assertTrue(usernames.contains("jules"));
        Assert.assertTrue(usernames.contains("danny"));

        // test searchForUser
        List<UserModel> users = session.users().searchForUser("tbrady", realm);
        Assert.assertTrue(users.size() == 1);
        Assert.assertTrue(users.get(0).getUsername().equals("tbrady"));

        // test getGroupMembers()
        GroupModel gods = realm.createGroup("gods");
        UserModel user = null;
        user = session.users().getUserByUsername("apollo", realm);
        user.joinGroup(gods);
        user = session.users().getUserByUsername("zeus", realm);
        user.joinGroup(gods);
        user = session.users().getUserByUsername("thor", realm);
        user.joinGroup(gods);
        queried.clear();
        usernames.clear();

        first = 0;
        while (queried.size() < 8) {
            List<UserModel> results = session.users().getGroupMembers(realm, gods, first, 1);
            if (results.size() == 0) break;
            first += results.size();
            queried.addAll(results);

        }
        for (UserModel u : queried) {
            usernames.add(u.getUsername());
            System.out.println(u.getUsername());

        }
        Assert.assertEquals(3, queried.size());
        Assert.assertTrue(usernames.contains("apollo"));
        Assert.assertTrue(usernames.contains("zeus"));
        Assert.assertTrue(usernames.contains("thor"));

        // search by single attribute
        System.out.println("search by single attribute");
        user = session.users().getUserByUsername("thor", realm);
        user.setSingleAttribute("weapon", "hammer");

        users = session.users().searchForUserByUserAttribute("weapon", "hammer", realm);
        for (UserModel u : users) {
            System.out.println(u.getUsername());

        }
        Assert.assertEquals(1, users.size());
        Assert.assertEquals("thor", users.get(0).getUsername());


        keycloakRule.stopSession(session, true);
    }

    @Test
    public void testRegistration() {
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("test");
        UserModel user = session.users().addUser(realm, "memuser");
        user.updateCredential(UserCredentialModel.password("password"));
        keycloakRule.stopSession(session, true);
        loginSuccessAndLogout("memuser", "password");
        loginSuccessAndLogout("memuser", "password");
        loginSuccessAndLogout("memuser", "password");

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        user = session.users().getUserByUsername("memuser", realm);
        Assert.assertEquals(memoryProvider.getId(), StorageId.resolveProviderId(user));
        Assert.assertEquals(1, user.getCredentialsDirectly().size());
        session.users().removeUser(realm, user);
        Assert.assertNull(session.users().getUserByUsername("memuser", realm));
        keycloakRule.stopSession(session, true);

    }

    @Test
    public void testLifecycle() {
        UserMapStorage.allocations.set(0);
        UserMapStorage.closings.set(0);
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("test");
        UserModel user = session.users().addUser(realm, "memuser");
        Assert.assertNotNull(user);
        user = session.users().getUserByUsername("nonexistent", realm);
        Assert.assertNull(user);
        keycloakRule.stopSession(session, true);
        Assert.assertEquals(1, UserMapStorage.allocations.get());
        Assert.assertEquals(1, UserMapStorage.closings.get());

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        user = session.users().getUserByUsername("memuser", realm);
        session.users().removeUser(realm, user);
        Assert.assertNull(session.users().getUserByUsername("memuser", realm));
        keycloakRule.stopSession(session, true);

    }

}
