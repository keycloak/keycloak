/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.oauth;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthGrantServlet;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @BeforeClass
    public static void before() {
        keycloakRule.deployServlet("grant", "/grant", OAuthGrantServlet.class);
    }

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected OAuthGrantPage grantPage;

    private static String GRANT_APP_URL = "http://localhost:8081/grant/";

    private static String ACCESS_GRANTED = "Access rights granted.";
    private static String ACCESS_NOT_GRANTED = "Access rights not granted.";

    private static String ROLE_USER = "Have User privileges";
    private static String ROLE_CUSTOMER = "Have Customer User privileges";

    private static String GRANT_ROLE = "user";
    private static String GRANT_APP = "test-app";
    private static String GRANT_APP_ROLE = "customer-user";

    @Test
    public void oauthGrantAcceptTest() throws IOException {

        driver.navigate().to(GRANT_APP_URL);

        Assert.assertFalse(driver.getPageSource().contains(ACCESS_GRANTED));
        Assert.assertFalse(driver.getPageSource().contains(ACCESS_NOT_GRANTED));

        loginPage.isCurrent();
        loginPage.login("test-user@localhost", "password");

        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));

        grantPage.accept();

        Assert.assertTrue(driver.getPageSource().contains(ACCESS_GRANTED));
        Assert.assertFalse(driver.getPageSource().contains(ACCESS_NOT_GRANTED));

        Assert.assertTrue(driver.getPageSource().contains("Role:"+ GRANT_ROLE +"."));
        Assert.assertTrue(driver.getPageSource().contains("App:"+ GRANT_APP +";"+ GRANT_APP_ROLE +"."));
    }

    @Test
    public void oauthGrantCancelTest() throws IOException {

        driver.navigate().to(GRANT_APP_URL);

        Assert.assertFalse(driver.getPageSource().contains(ACCESS_GRANTED));
        Assert.assertFalse(driver.getPageSource().contains(ACCESS_NOT_GRANTED));

        loginPage.isCurrent();
        loginPage.login("test-user@localhost", "password");

        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));

        grantPage.cancel();

        Assert.assertFalse(driver.getPageSource().contains(ACCESS_GRANTED));
        Assert.assertTrue(driver.getPageSource().contains(ACCESS_NOT_GRANTED));
    }
}
