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
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.infinispan.UserAdapter;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.StorageProviderModel;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserFederationStorageTest {
    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            StorageProviderModel model = new StorageProviderModel();
            model.setDisplayName("read-only-user-props");
            model.setPriority(1);
            model.setProviderName(UserPropertyFileStorageFactory.PROVIDER_ID);
            model.getConfig().put("property.file", "/storage-test/read-only-user-password.properties");
            appRealm.addStorageProvider(model);
            model = new StorageProviderModel();
            model.setDisplayName("user-props");
            model.setPriority(2);
            model.setProviderName(UserPropertyFileStorageFactory.PROVIDER_ID);
            model.getConfig().put("property.file", "/storage-test/user-password.properties");
            model.getConfig().put("USER_FEDERATED_STORAGE", "true");
            appRealm.addStorageProvider(model);
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
        Assert.assertEquals("my-group", groups.iterator().next().getName());
        System.out.println("num groups " + groups.size());
        Assert.assertTrue(thor.getRequiredActions().iterator().next().equals("POOP"));
        thor.removeRequiredAction("POOP");
        thor.updateCredential(UserCredentialModel.password("lightning"));
        keycloakRule.stopSession(session, true);
        loginSuccessAndLogout("thor", "lightning");
    }

}
