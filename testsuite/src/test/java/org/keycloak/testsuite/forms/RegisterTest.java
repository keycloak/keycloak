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
package org.keycloak.testsuite.forms;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegisterTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected RegisterPage registerPage;

    @Test
    public void registerExistingUser() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("name", "email", "test-user@localhost", "password", "password");

        registerPage.assertCurrent();
        Assert.assertEquals("Username already exists", registerPage.getError());
    }

    @Test
    public void registerUserInvalidPasswordConfirm() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("name", "email", "registerUserInvalidPasswordConfirm", "password", "invalid");

        registerPage.assertCurrent();
        Assert.assertEquals("Password confirmation doesn't match", registerPage.getError());
    }

    @Test
    public void registerUserMissingPassword() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("name", "email", "registerUserMissingPassword", null, null);

        registerPage.assertCurrent();
        Assert.assertEquals("Please specify password", registerPage.getError());
    }

    @Test
    public void registerUserMissingUsername() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("name", "email", null, "password", "password");

        registerPage.assertCurrent();
        Assert.assertEquals("Please specify username", registerPage.getError());
    }

    @Test
    public void registerUserSuccess() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("name", "email", "registerUserSuccess", "password", "password");

        Assert.assertTrue("Expected authorization response", oauth.isAuthorizationResponse());
    }

}
