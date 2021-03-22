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


import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.page.AbstractPage;
import org.keycloak.testsuite.page.PageWithLogOutAction;
import org.openqa.selenium.Cookie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;

public abstract class AbstractFailoverClusterTest extends AbstractClusterTest {

    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";

    public static final Integer SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("session.cache.owners", "1"));
    public static final Integer OFFLINE_SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("offline.session.cache.owners", "1"));
    public static final Integer LOGIN_FAILURES_CACHE_OWNERS = Integer.parseInt(System.getProperty("login.failure.cache.owners", "1"));

    public static final Integer REBALANCE_WAIT = Integer.parseInt(System.getProperty("rebalance.wait", "5000"));

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
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

    protected Cookie login(AbstractPage targetPage) {
        targetPage.navigateTo();
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login(ADMIN, ADMIN);
        assertCurrentUrlStartsWith(targetPage);
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        return sessionCookie;
    }

    protected void logout(AbstractPage targetPage) {
        if (!(targetPage instanceof PageWithLogOutAction)) {
            throw new IllegalArgumentException(targetPage.getClass().getSimpleName() + " must implement PageWithLogOutAction interface");
        }
        targetPage.navigateTo();
        assertCurrentUrlStartsWith(targetPage);
        ((PageWithLogOutAction) targetPage).logOut();
    }

    protected Cookie verifyLoggedIn(AbstractPage targetPage, Cookie sessionCookieForVerification) {
        // verify on realm path
        masterRealmPage.navigateTo();
        Cookie sessionCookieOnRealmPath = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookieOnRealmPath);
        assertEquals(sessionCookieOnRealmPath.getValue(), sessionCookieForVerification.getValue());
        // verify on target page
        targetPage.navigateTo();
        assertCurrentUrlStartsWith(targetPage);
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        assertEquals(sessionCookie.getValue(), sessionCookieForVerification.getValue());
        return sessionCookie;
    }

    protected void verifyLoggedOut(AbstractPage targetPage) {
        // verify on target page
        targetPage.navigateTo();
        driver.navigate().refresh();
        assertCurrentUrlStartsWith(loginPage);
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNull(sessionCookie);
    }
}
