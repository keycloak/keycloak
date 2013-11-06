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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginTotpTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
            UserModel user = appRealm.getUser("test-user@localhost");

            UserCredentialModel credentials = new UserCredentialModel();
            credentials.setType(CredentialRepresentation.TOTP);
            credentials.setValue("totpSecret");
            appRealm.updateCredential(user, credentials);

            user.setTotp(true);
        }

    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    private LoginTotpPage loginTotpPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    @Before
    public void before() throws MalformedURLException {
        totp = new TimeBasedOTP();
    }

    @Test
    public void loginWithTotpFailure() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        loginTotpPage.assertCurrent();

        loginTotpPage.login("123456");

        loginPage.assertCurrent();
        Assert.assertEquals("Invalid username or password", loginPage.getError());
    }

    @Test
    public void loginWithTotpSuccess() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        loginTotpPage.assertCurrent();

        loginTotpPage.login(totp.generate("totpSecret"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

}
