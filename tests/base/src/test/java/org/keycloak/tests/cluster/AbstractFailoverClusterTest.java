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

package org.keycloak.tests.cluster;


import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.Cookie;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public abstract class AbstractFailoverClusterTest extends AbstractClusterTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";

    public static final Integer SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("session.cache.owners", "1"));
    public static final Integer OFFLINE_SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("offline.session.cache.owners", "1"));
    public static final Integer LOGIN_FAILURES_CACHE_OWNERS = Integer.parseInt(System.getProperty("login.failure.cache.owners", "1"));

    public static final Integer REBALANCE_WAIT = Integer.parseInt(System.getProperty("rebalance.wait", "5000"));

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected LogoutConfirmPage logoutConfirmPage;

    @InjectPage
    protected InfoPage infoPage;

    @BeforeClass
    public static void modifyAppRoot() {
        // the test app needs to run in the test realm to be able to fetch cookies later
        OAuthClient.updateAppRootRealm("test");
    }

    @AfterClass
    public static void restoreAppRoot() {
        OAuthClient.resetAppRootRealm();
    }

    @BeforeEach
    public void setup() {
        try {
            adminClient.realm("test").remove();
        } catch (Exception ignore) {
        }

        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        adminClient.realms().create(testRealm);

        UserRepresentation user = UserBuilder.create()
                .username("login-test")
                .email("login@test.com")
                .enabled(true)
                .requiredActions(UserModel.RequiredAction.UPDATE_PASSWORD.toString())
                .requiredActions(UserModel.RequiredAction.UPDATE_PROFILE.toString())
                .password("password")
                .build();

        String userId = AdminApiUtil.createUserWithAdminClient(adminClient.realm("test"), user);
        getCleanup().addUserId(userId);

        oauth.client("test-app", "password");
    }

    @AfterEach
    public void after() {
        adminClient.realm("test").remove();
    }


    /**
     * failure --> failback --> failure of next node
     */
    protected void switchFailedNode() {
        assertFalse(controller.isStarted(getCurrentFailNode().getQualifier()));

        failback();
        pause(REBALANCE_WAIT);

        iterateCurrentFailNode();

        failure();
        pause(REBALANCE_WAIT);

        assertFalse(controller.isStarted(getCurrentFailNode().getQualifier()));
    }

    protected Cookie login() {
        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        return sessionCookie;
    }

    protected void logout() {
        oauth.openLogoutForm();

        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        // Info page present
        infoPage.assertCurrent();
        Assertions.assertEquals("You are logged out", infoPage.getInfo());
    }

    protected Cookie verifyLoggedIn(Cookie sessionCookieForVerification) {
        // verify on realm path
        URLUtils.navigateToUri(AUTH_SERVER_ROOT + "/realms/test/");
        Cookie sessionCookieOnRealmPath = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookieOnRealmPath);
        assertEquals(sessionCookieOnRealmPath.getValue(), sessionCookieForVerification.getValue());
        // verify on target page
        driver.navigate().to(oauth.getRedirectUri());
        Assertions.assertEquals(driver.getCurrentUrl(), oauth.getRedirectUri());
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        assertEquals(sessionCookie.getValue(), sessionCookieForVerification.getValue());
        return sessionCookie;
    }

    protected void verifyLoggedOut() {
        // verify on target page
        oauth.openLoginForm();
        driver.navigate().refresh();
        loginPage.assertCurrent();
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNull(sessionCookie);
    }
}
