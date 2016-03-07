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

package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.adapter.page.SessionPortal;

import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;

import org.keycloak.testsuite.auth.page.account.Sessions;
import org.keycloak.testsuite.auth.page.login.Login;

import static org.keycloak.testsuite.admin.ApiUtil.findClientResourceByClientId;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

import org.keycloak.testsuite.util.SecondBrowser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractSessionServletAdapterTest extends AbstractServletsAdapterTest {

    @Page
    private SessionPortal sessionPortalPage;

    @Page
    private Sessions testRealmSessions;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmSessions.setAuthRealm(DEMO);
    }

    @Deployment(name = SessionPortal.DEPLOYMENT_NAME)
    protected static WebArchive sessionPortal() {
        return servletDeployment(SessionPortal.DEPLOYMENT_NAME, "keycloak.json", SessionServlet.class);
    }

    @After
    public void afterSessionServletAdapterTest() {
        sessionPortalPage.navigateTo();
        driver.manage().deleteAllCookies();
    }

    @Drone
    @SecondBrowser
    protected WebDriver driver2;

    @Test
    public void testSingleSessionInvalidated() {

        loginAndCheckSession(driver, testRealmLoginPage);

        // cannot pass to loginAndCheckSession becayse loginPage is not working together with driver2, therefore copypasta
        driver2.navigate().to(sessionPortalPage.toString());
        assertCurrentUrlStartsWithLoginUrlOf(driver2, testRealmPage);
        driver2.findElement(By.id("username")).sendKeys("bburke@redhat.com");
        driver2.findElement(By.id("password")).sendKeys("password");
        driver2.findElement(By.id("password")).submit();
        assertCurrentUrlEquals(driver2, sessionPortalPage);
        String pageSource = driver2.getPageSource();
        assertTrue(pageSource.contains("Counter=1"));
        // Counter increased now
        driver2.navigate().to(sessionPortalPage.toString());
        pageSource = driver2.getPageSource();
        assertTrue(pageSource.contains("Counter=2"));

        // Logout in browser1
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, sessionPortalPage.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        // Assert that I am logged out in browser1
        sessionPortalPage.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        // Assert that I am still logged in browser2 and same session is still preserved
        driver2.navigate().to(sessionPortalPage.toString());
        assertCurrentUrlEquals(driver2, sessionPortalPage);
        pageSource = driver2.getPageSource();
        assertTrue(pageSource.contains("Counter=3"));

        driver2.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(driver2, testRealmPage);

    }

    @Test
    public void testSessionInvalidatedAfterFailedRefresh() {
        RealmRepresentation testRealmRep = testRealmResource().toRepresentation();
        ClientResource sessionPortalRes = null;
        for (ClientRepresentation clientRep : testRealmResource().clients().findAll()) {
            if ("session-portal".equals(clientRep.getClientId())) {
                sessionPortalRes = testRealmResource().clients().get(clientRep.getId());
            }
        }
        assertNotNull(sessionPortalRes);
        sessionPortalRes.toRepresentation().setAdminUrl("");
        int origTokenLifespan = testRealmRep.getAccessCodeLifespan();
        testRealmRep.setAccessCodeLifespan(1);
        testRealmResource().update(testRealmRep);

        // Login
        loginAndCheckSession(driver, testRealmLoginPage);

        // Logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, sessionPortalPage.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);

        // Assert that http session was invalidated
        sessionPortalPage.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), sessionPortalPage.toString());
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Counter=1"));

        sessionPortalRes.toRepresentation().setAdminUrl(sessionPortalPage.toString());
        testRealmRep.setAccessCodeLifespan(origTokenLifespan);
        testRealmResource().update(testRealmRep);
    }

    @Test
    public void testAdminApplicationLogout() {
        // login as bburke
        loginAndCheckSession(driver, testRealmLoginPage);

        // logout mposolda with admin client
        UserRepresentation mposolda = testRealmResource().users().search("mposolda", null, null, null, null, null).get(0);
        testRealmResource().users().get(mposolda.getId()).logout();
        
        // bburke should be still logged with original httpSession in our browser window
        sessionPortalPage.navigateTo();
        assertEquals(driver.getCurrentUrl(), sessionPortalPage.toString());
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Counter=3"));
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, sessionPortalPage.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
    }

    @Test
    public void testAccountManagementSessionsLogout() {
        // login as bburke
        loginAndCheckSession(driver, testRealmLoginPage);
        testRealmSessions.navigateTo();
        testRealmSessions.logoutAll();
        // Assert I need to login again (logout was propagated to the app)
        loginAndCheckSession(driver, testRealmLoginPage);
    }

    private void loginAndCheckSession(WebDriver driver, Login login) {
        sessionPortalPage.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        login.form().login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), sessionPortalPage.toString());
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Counter=1"));

        // Counter increased now
        sessionPortalPage.navigateTo();
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Counter=2"));
    }

}
