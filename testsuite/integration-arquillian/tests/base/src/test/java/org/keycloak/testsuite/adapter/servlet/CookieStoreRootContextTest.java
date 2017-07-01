/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.CustomerCookiePortalRoot;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.containers.ContainerConstants;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
public class CookieStoreRootContextTest extends DemoServletsAdapterTest {

    // Javascript browser needed KEYCLOAK-4703
    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    @Page
    @JavascriptBrowser
    protected OIDCLogin jsDriverTestRealmLoginPage;

    @Page
    private CustomerCookiePortalRoot customerCookiePortalRoot;

    @Rule
    public AssertEvents assertEvents = new AssertEvents(this);

    @Deployment(name = CustomerCookiePortalRoot.DEPLOYMENT_NAME)
    protected static WebArchive customerCookiePortalRoot() {
        WebArchive original = servletDeployment(CustomerCookiePortalRoot.DEPLOYMENT_NAME, AdapterActionsFilter.class, CustomerServlet.class, ErrorServlet.class, ServletTestUtils.class);

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "ROOT.war");

        archive.merge(original);

        return archive;
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        configPage.setConsoleRealm(DEMO);
        loginEventsPage.setConsoleRealm(DEMO);
        applicationsPage.setAuthRealm(DEMO);
        loginEventsPage.setConsoleRealm(DEMO);
    }
    
    @Test
    public void testTokenInCookieSSORoot() {
        // Login
        String tokenCookie = loginToCustomerCookiePortalRoot();
        Cookie cookie = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE);
        assertEquals("/", cookie.getPath());

        // SSO to second app
        customerPortal.navigateTo();
        assertLogged();

        customerCookiePortalRoot.navigateTo();
        assertLogged();
        cookie = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE);
        String tokenCookie2 = cookie.getValue();
        assertEquals(tokenCookie, tokenCookie2);
        assertEquals("/", cookie.getPath());

        // Logout with httpServletRequest
        logoutFromCustomerCookiePortalRoot();

        // Also should be logged-out from the second app
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    private String loginToCustomerCookiePortalRoot() {
        customerCookiePortalRoot.navigateTo("relative");
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerCookiePortalRoot.getInjectedUrl().toString() + "relative");
        assertLogged();

        // Assert no JSESSIONID cookie
        Assert.assertNull(driver.manage().getCookieNamed("JSESSIONID"));

        return driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
    }
    
    private void logoutFromCustomerCookiePortalRoot() {
        String logout = customerCookiePortalRoot.logoutURL();
        driver.navigate().to(logout);
        WaitUtils.waitUntilElement(By.id("customer_portal_logout")).is().present();
        assertNull(driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE));
        customerCookiePortalRoot.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }
}
