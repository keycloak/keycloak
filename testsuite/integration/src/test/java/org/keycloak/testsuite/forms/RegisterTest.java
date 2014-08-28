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
import org.keycloak.events.Details;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
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
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected RegisterPage registerPage;

    @WebResource
    protected OAuthClient oauth;

    @Test
    public void registerExistingUser() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerExistingUseremail", "test-user@localhost", "password", "password");

        registerPage.assertCurrent();
        Assert.assertEquals("Username already exists", registerPage.getError());

        events.expectRegister("test-user@localhost", "registerExistingUseremail").user((String) null).error("username_in_use").assertEvent();
    }

    @Test
    public void registerUserInvalidPasswordConfirm() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserInvalidPasswordConfirmemail", "registerUserInvalidPasswordConfirm", "password", "invalid");

        registerPage.assertCurrent();
        Assert.assertEquals("Password confirmation doesn't match", registerPage.getError());

        events.expectRegister("registerUserInvalidPasswordConfirm", "registerUserInvalidPasswordConfirmemail").user((String) null).error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserMissingPassword() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserMissingPasswordemail", "registerUserMissingPassword", null, null);

        registerPage.assertCurrent();
        Assert.assertEquals("Please specify password.", registerPage.getError());

        events.expectRegister("registerUserMissingPassword", "registerUserMissingPasswordemail").user((String) null).error("invalid_registration").assertEvent();
    }

    @Test
    public void registerPasswordPolicy() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setPasswordPolicy(new PasswordPolicy("length"));
            }
        });

        try {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerPasswordPolicyemail", "registerPasswordPolicy", "pass", "pass");

            registerPage.assertCurrent();
            Assert.assertEquals("Invalid password: minimum length 8", registerPage.getError());

            events.expectRegister("registerPasswordPolicy", "registerPasswordPolicyemail").user((String) null).error("invalid_registration").assertEvent();

            registerPage.register("firstName", "lastName", "registerPasswordPolicyemail", "registerPasswordPolicy", "password", "password");
            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = events.expectRegister("registerPasswordPolicy", "registerPasswordPolicyemail").assertEvent().getUserId();

            events.expectLogin().user(userId).detail(Details.USERNAME, "registerPasswordPolicy").assertEvent();
        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setPasswordPolicy(new PasswordPolicy(null));
                }
            });
        }
    }

    @Test
    public void registerUserMissingUsername() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserMissingUsernameemail", null, "password", "password");

        registerPage.assertCurrent();
        Assert.assertEquals("Please specify username", registerPage.getError());

        events.expectRegister(null, "registerUserMissingUsernameemail").removeDetail("username").error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserSuccess() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserSuccessemail", "registerUserSuccess", "password", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister("registerUserSuccess", "registerUserSuccessemail").assertEvent().getUserId();
        events.expectLogin().detail("username", "registerUserSuccess").user(userId).assertEvent();
    }

}
