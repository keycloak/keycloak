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

package org.keycloak.testsuite.cluster;


import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.Cookie;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractFailoverClusterTest extends AbstractClusterTest {

    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";

    public static final Integer SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("session.cache.owners", "1"));
    public static final Integer OFFLINE_SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("offline.session.cache.owners", "1"));
    public static final Integer LOGIN_FAILURES_CACHE_OWNERS = Integer.parseInt(System.getProperty("login.failure.cache.owners", "1"));

    public static final Integer REBALANCE_WAIT = Integer.parseInt(System.getProperty("rebalance.wait", "5000"));

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Page
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

    @Before
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
                .requiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString())
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.toString())
                .password("password")
                .build();

        String userId = ApiUtil.createUserWithAdminClient(adminClient.realm("test"), user);
        getCleanup().addUserId(userId);

        oauth.clientId("test-app");
    }

    @After
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
        assertTrue(loginPage.isCurrent());
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());
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
        Assert.assertEquals("You are logged out", infoPage.getInfo());
    }

    protected Cookie verifyLoggedIn(Cookie sessionCookieForVerification) {
        // verify on realm path
        URLUtils.navigateToUri(AUTH_SERVER_ROOT + "/realms/test/");
        Cookie sessionCookieOnRealmPath = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookieOnRealmPath);
        assertEquals(sessionCookieOnRealmPath.getValue(), sessionCookieForVerification.getValue());
        // verify on target page
        appPage.open();
        assertTrue(appPage.isCurrent());
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        assertEquals(sessionCookie.getValue(), sessionCookieForVerification.getValue());
        return sessionCookie;
    }

    protected void verifyLoggedOut() {
        // verify on target page
        oauth.openLoginForm();
        driver.navigate().refresh();
        assertTrue(loginPage.isCurrent());
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNull(sessionCookie);
    }
}
