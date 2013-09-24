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
package org.keycloak.testsuite;

import java.net.MalformedURLException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.Driver;
import org.keycloak.testsuite.rule.Page;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;
import org.picketlink.idm.credential.util.TimeBasedOTP;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@RunWith(Arquillian.class)
public class RequiredActionTotpSetupTest {

    @Deployment(name = "app", testable = false, order = 2)
    public static WebArchive appDeployment() {
        return Deployments.appDeployment();
    }

    @Deployment(name = "auth-server", testable = false, order = 1)
    public static WebArchive deployment() {
        return Deployments.deployment().addAsResource("testrealm-totp.json", "META-INF/testrealm.json");
    }

    @Rule
    public WebRule webRule = new WebRule(this);

    @Page
    protected AppPage appPage;

    @Driver
    protected WebDriver driver;

    @Page
    protected LoginConfigTotpPage totpPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected RegisterPage registerPage;

    protected TimeBasedOTP totp;

    @Before
    public void before() throws MalformedURLException {
        totp = new TimeBasedOTP();
    }

    @After
    public void after() {
        appPage.open();
        if (appPage.isCurrent()) {
            appPage.logout();
        }
    }

    @Test
    public void setupTotp() {
        appPage.open();

        loginPage.register();
        registerPage.register("name", "email", "setupTotp", "password", "password");

        Assert.assertTrue(totpPage.isCurrent());

        totpPage.configure(totp.generate(totpPage.getTotpSecret()));

        Assert.assertTrue(appPage.isCurrent());
        Assert.assertEquals("setupTotp", appPage.getUser());
    }

}
