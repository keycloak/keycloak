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

import freemarker.ext.beans.HashAdapter;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialAuthentication;
import org.keycloak.credential.UserCredentialStoreManager;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.infinispan.UserAdapter;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.ApplicationServlet;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStorageFailureTest {
    public static ComponentModel memoryProvider = null;
    public static String realmName;
    public static final String LOCAL_USER = "localUser";
    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setName("failure");
            model.setPriority(0);
            model.setProviderId(FailableHardcodedStorageProviderFactory.PROVIDER_ID);
            model.setParentId(appRealm.getId());
            memoryProvider = appRealm.addComponentModel(model);
            realmName = appRealm.getName();

            ClientModel offlineClient = appRealm.addClient("offline-client");
            offlineClient.setEnabled(true);
            offlineClient.setDirectAccessGrantsEnabled(true);
            offlineClient.setSecret("secret");
            HashSet<String> redirects = new HashSet<>();
            redirects.add(Constants.AUTH_SERVER_ROOT + "/offline-client");
            offlineClient.setRedirectUris(redirects);
            offlineClient.setServiceAccountsEnabled(true);
            offlineClient.setFullScopeAllowed(true);

            UserModel serviceAccount = manager.getSession().users().addUser(appRealm, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + offlineClient.getClientId());
            serviceAccount.setEnabled(true);
            RoleModel role = appRealm.getRole("offline_access");
            Assert.assertNotNull(role);
            serviceAccount.grantRole(role);
            serviceAccount.setServiceAccountClientLink(offlineClient.getClientId());

            UserModel localUser = manager.getSession().userLocalStorage().addUser(appRealm, LOCAL_USER);
            localUser.setEnabled(true);


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

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);


    // this is a hack so that UserModel doesn't have to be available when offline token is imported.
    // see related JIRA - KEYCLOAK-5350 and corresponding test

    /**
     *  KEYCLOAK-5350
     */
    @Test
    public void testKeycloak5350() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        oauth.redirectUri(Constants.AUTH_SERVER_ROOT + "/offline-client");
        oauth.doLogin(FailableHardcodedStorageProvider.username, "password");

        Event loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, Constants.AUTH_SERVER_ROOT + "/offline-client")
                .event();

        final String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "secret");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);
        events.clear();

        evictUser(FailableHardcodedStorageProvider.username);

        KeycloakSession session;
        RealmModel realm;
        UserModel user;

        toggleForceFail(true);

        // make sure failure is turned on
        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName(realmName);
        try {
            user = session.users().getUserByUsername(FailableHardcodedStorageProvider.username, realm);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("FORCED FAILURE", e.getMessage());

        }
        keycloakRule.stopSession(session, false);

        // restart server to make sure we can still boot if user storage is down
        keycloakRule.restartServer();
        keycloakRule.deployServlet("app", "/app", ApplicationServlet.class);

        toggleForceFail(false);


        // test that once user storage provider is available again we can still access the token.
        tokenResponse = oauth.doRefreshTokenRequest(offlineTokenString, "secret");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        token = oauth.verifyToken(tokenResponse.getAccessToken());
        offlineTokenString = tokenResponse.getRefreshToken();
        offlineToken = oauth.verifyRefreshToken(offlineTokenString);
        events.clear();


    }

    protected void evictUser(String username) {
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName(realmName);
        UserModel user = session.users().getUserByUsername(username, realm);
        session.userCache().evict(realm, user);
        keycloakRule.stopSession(session, true);
    }

    protected void toggleForceFail(boolean toggle) {
        KeycloakSession session;
        RealmModel realm;
        session = keycloakRule.startSession();
        memoryProvider.getConfig().putSingle("fail", Boolean.toString(toggle));
        realm = session.realms().getRealmByName(realmName);
        realm.updateComponent(memoryProvider);
        keycloakRule.stopSession(session, true);
    }

    protected void toggleProviderEnabled(boolean toggle) {
        KeycloakSession session;
        RealmModel realm;
        session = keycloakRule.startSession();
        UserStorageProviderModel model = new UserStorageProviderModel(memoryProvider);
        model.setEnabled(toggle);
        realm = session.realms().getRealmByName(realmName);
        realm.updateComponent(model);
        keycloakRule.stopSession(session, true);
    }

    private void loginSuccessAndLogout(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);
        System.out.println(driver.getCurrentUrl());
        System.out.println(driver.getPageSource());
        Assert.assertTrue(appPage.isCurrent());
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
        oauth.openLogout();
    }

    @Test
    public void testKeycloak5926() {

        // make sure local copy is deleted
        {
            KeycloakSession session = keycloakRule.startSession();
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.userLocalStorage().getUserByUsername(FailableHardcodedStorageProvider.username, realm);
            if (user != null) {
                session.userLocalStorage().removeUser(realm, user);
            }
            keycloakRule.stopSession(session, true);
        }

        // query user to make sure its imported
        {
            KeycloakSession session = keycloakRule.startSession();
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(FailableHardcodedStorageProvider.username, realm);
            Assert.assertNotNull(user);
            keycloakRule.stopSession(session, true);
        }



        evictUser(FailableHardcodedStorageProvider.username);
        evictUser(LOCAL_USER);

        toggleForceFail(true);
        {
            KeycloakSession session = keycloakRule.startSession();
            // make sure we can still query local users
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel local = session.users().getUserByUsername(LOCAL_USER, realm);
            Assert.assertNotNull(local);
            // assert that lookup of user storage user fails
            try {
                UserModel user = session.users().getUserByUsername(FailableHardcodedStorageProvider.username, realm);
                Assert.fail();
            } catch (Exception e) {
                Assert.assertEquals("FORCED FAILURE", e.getMessage());

            }

            keycloakRule.stopSession(session, true);
        }
        // test that we can still login to a user
        loginSuccessAndLogout("test-user@localhost", "password");

        toggleProviderEnabled(false);
        {
            KeycloakSession session = keycloakRule.startSession();
            // make sure we can still query local users
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel local = session.users().getUserByUsername(LOCAL_USER, realm);
            Assert.assertNotNull(local);
            List<UserModel> result;
            result = session.users().searchForUser(LOCAL_USER, realm);
            Assert.assertEquals(1, result.size());
            session.users().searchForUser(FailableHardcodedStorageProvider.username, realm);
            Assert.assertEquals(1, result.size());
            session.users().searchForUser(LOCAL_USER, realm, 0, 2);
            Assert.assertEquals(1, result.size());
            session.users().searchForUser(FailableHardcodedStorageProvider.username, realm, 0, 2);
            Assert.assertEquals(1, result.size());
            Map<String, String> localParam = new HashMap<>();
            localParam.put("username", LOCAL_USER);
            Map<String, String> hardcodedParam = new HashMap<>();
            hardcodedParam.put("username", FailableHardcodedStorageProvider.username);

            result = session.users().searchForUser(localParam, realm);
            Assert.assertEquals(1, result.size());
            session.users().searchForUser(hardcodedParam, realm);
            Assert.assertEquals(1, result.size());
            session.users().searchForUser(localParam, realm, 0, 2);
            Assert.assertEquals(1, result.size());
            session.users().searchForUser(hardcodedParam, realm, 0, 2);
            Assert.assertEquals(1, result.size());

            session.users().getUsers(realm);
            session.users().getUsersCount(realm);



            UserModel user = session.users().getUserByUsername(FailableHardcodedStorageProvider.username, realm);
            Assert.assertFalse(user instanceof CachedUserModel);
            Assert.assertEquals(FailableHardcodedStorageProvider.username, user.getUsername());
            Assert.assertEquals(FailableHardcodedStorageProvider.email, user.getEmail());
            Assert.assertFalse(user.isEnabled());
            try {
                user.setEmail("error@error.com");
                Assert.fail();
            } catch (Exception ex) {

            }
            keycloakRule.stopSession(session, true);
        }
        // make sure user isn't cached as provider is disabled
        {
            KeycloakSession session = keycloakRule.startSession();
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(FailableHardcodedStorageProvider.username, realm);
            Assert.assertFalse(user instanceof CachedUserModel);
            Assert.assertEquals(FailableHardcodedStorageProvider.username, user.getUsername());
            Assert.assertEquals(FailableHardcodedStorageProvider.email, user.getEmail());
            keycloakRule.stopSession(session, true);
        }

        // make ABSOLUTELY sure user isn't cached as provider is disabled
        {
            KeycloakSession session = keycloakRule.startSession();
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(FailableHardcodedStorageProvider.username, realm);
            Assert.assertFalse(user instanceof CachedUserModel);
            Assert.assertEquals(FailableHardcodedStorageProvider.username, user.getUsername());
            Assert.assertEquals(FailableHardcodedStorageProvider.email, user.getEmail());
            keycloakRule.stopSession(session, true);
        }



        toggleProviderEnabled(true);
        toggleForceFail(false);

        // user should be cachable now
        {
            KeycloakSession session = keycloakRule.startSession();
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(FailableHardcodedStorageProvider.username, realm);
            Assert.assertTrue(user instanceof CachedUserModel);
            Assert.assertEquals(FailableHardcodedStorageProvider.username, user.getUsername());
            Assert.assertEquals(FailableHardcodedStorageProvider.email, user.getEmail());
            keycloakRule.stopSession(session, true);
        }

        events.clear();
    }


    //@Test
    public void testIDE() throws Exception {
        Thread.sleep(100000000);
    }

}
