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

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.TotpPage;
import org.keycloak.testsuite.rule.Page;
import org.picketlink.idm.credential.util.TimeBasedOTP;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@RunWith(Arquillian.class)
public class TotpTest extends AbstractDroneTest {

    @Page
    private LoginTotpPage loginTotpPage;

    private TimeBasedOTP totp;

    @Page
    protected TotpPage totpPage;

    @Before
    public void before() throws MalformedURLException {
        totp = new TimeBasedOTP();
    }

    @Test
    public void loginWithTotpFailure() {
        appPage.open();
        loginPage.register();
        registerPage.register("name", "email", "loginWithTotpFailure", "password", "password");
        totpPage.open();

        String totpSecret = totpPage.getTotpSecret();
        totpPage.configure(totp.generate(totpSecret));

        appPage.open();
        appPage.logout();

        loginPage.login("loginWithTotpSuccess", "password");

        Assert.assertFalse(appPage.isCurrent());

        loginTotpPage.login("123456");

        Assert.assertTrue(loginTotpPage.isCurrent());
        Assert.assertEquals("Invalid username or password", loginTotpPage.getError());
    }

    @Test
    public void loginWithTotpSuccess() {
        appPage.open();
        loginPage.register();
        registerPage.register("name", "email", "loginWithTotpSuccess", "password", "password");
        totpPage.open();

        String totpSecret = totpPage.getTotpSecret();
        totpPage.configure(totp.generate(totpSecret));

        appPage.open();
        appPage.logout();

        loginPage.login("loginWithTotpSuccess", "password");

        Assert.assertFalse(appPage.isCurrent());

        loginTotpPage.login(totp.generate(totpSecret));

        Assert.assertTrue(appPage.isCurrent());
    }

    @Test
    public void setupTotp() {
        appPage.open();
        loginPage.register();
        registerPage.register("name", "email", "setupTotp", "password", "password");

        totpPage.open();

        Assert.assertTrue(totpPage.isCurrent());

        totpPage.configure(totp.generate(totpPage.getTotpSecret()));

        Assert.assertTrue(driver.getPageSource().contains("Google Authenticator enabled"));
    }

}
