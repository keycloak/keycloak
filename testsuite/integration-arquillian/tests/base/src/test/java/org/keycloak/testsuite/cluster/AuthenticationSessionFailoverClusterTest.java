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

import java.io.IOException;

import jakarta.mail.MessagingException;

import org.keycloak.cookie.CookieType;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import static org.keycloak.testsuite.util.WaitUtils.pause;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionFailoverClusterTest extends AbstractFailoverClusterTest {

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;


    @Page
    protected LoginUpdateProfilePage updateProfilePage;


    @Test
    public void failoverDuringAuthentication() throws Exception {

        boolean expectSuccessfulFailover = SESSION_CACHE_OWNERS >= 2;

        log.info("AUTHENTICATION FAILOVER TEST: cluster size = " + getClusterSize() + ", session-cache owners = " + SESSION_CACHE_OWNERS
                + " --> Testsing for " + (expectSuccessfulFailover ? "" : "UN") + "SUCCESSFUL session failover.");

        assertEquals(2, getClusterSize());

        failoverTest(expectSuccessfulFailover);
    }


    protected void failoverTest(boolean expectSuccessfulFailover) throws IOException, MessagingException {
        loginPage.open();

        String cookieValue1 = getAuthSessionCookieValue(driver);

        // Login and assert on "updatePassword" page
        loginPage.login("login-test", "password");
        updatePasswordPage.assertCurrent();

        // Route didn't change
        Assert.assertEquals(cookieValue1, getAuthSessionCookieValue(driver));

        log.info("Authentication session cookie: " + cookieValue1);

        setCurrentFailNodeForRoute(cookieValue1);

        failure();
        pause(REBALANCE_WAIT);
        logFailoverSetup();

        // Trigger the action now
        updatePasswordPage.changePassword("password", "password");

        if (expectSuccessfulFailover) {
            //Action was successful
            updateProfilePage.assertCurrent();

            String cookieValue2 = getAuthSessionCookieValue(driver);

            log.info("Authentication session cookie after failover: " + cookieValue2);

            // Cookie was moved to the second node
            Assert.assertEquals(cookieValue1.substring(0, 36), cookieValue2.substring(0, 36));
            Assert.assertNotEquals(cookieValue1, cookieValue2);

        } else {
            loginPage.assertCurrent();
            String error = loginPage.getError();
            log.info("Failover not successful as expected. Error on login page: " + error);
            Assert.assertNotNull(error);

            loginPage.login("login-test", "password");
            updatePasswordPage.changePassword("password", "password");
        }


        updateProfilePage.assertCurrent();

        // Successfully update profile and assert user logged
        updateProfilePage.prepareUpdate().firstName("John").lastName("Doe3").email("john@doe3.com").submit();
        appPage.assertCurrent();
    }

    public static String getAuthSessionCookieValue(WebDriver driver) {
        Cookie authSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        Assert.assertNotNull(authSessionCookie);
        return authSessionCookie.getValue();
    }
}
