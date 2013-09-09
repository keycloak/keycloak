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
import org.picketlink.idm.credential.util.TimeBasedOTP;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@RunWith(Arquillian.class)
public class TotpTest extends AbstractDroneTest {

    private TimeBasedOTP totp;
    private String totpSecret;

    @Before
    public void before() throws MalformedURLException {
        super.before();

        totp = new TimeBasedOTP();
    }

    public void configureTotp() {
        selenium.open(authServerUrl + "/rest/realms/demo/account/totp");
        selenium.waitForPageToLoad("10000");

        Assert.assertTrue(selenium.isTextPresent("To setup Google Authenticator"));

        totpSecret = selenium.getValue("totpSecret");
        String code = totp.generate(totpSecret);

        selenium.type("id=totp", code);
        selenium.click("css=input[type=\"submit\"]");
        selenium.waitForPageToLoad("30000");

        Assert.assertTrue(selenium.isTextPresent("Google Authenticator enabled"));
    }

    @Test
    public void loginWithTotpFailure() {
        registerUser("loginWithTotpFailure", "password");
        configureTotp();
        logout();

        selenium.type("id=username", "loginWithTotpFailure");
        selenium.type("id=password", "password");

        selenium.click("css=input[type=\"submit\"]");
        selenium.waitForPageToLoad(DEFAULT_WAIT);

        Assert.assertEquals("Log in to demo", selenium.getTitle());

        selenium.type("id=totp", "123456");

        selenium.click("css=input[type=\"submit\"]");
        selenium.waitForPageToLoad(DEFAULT_WAIT);

        Assert.assertTrue(selenium.isTextPresent("Invalid username or password"));
    }

    @Test
    public void loginWithTotpSuccess() {
        registerUser("loginWithTotpSuccess", "password");
        configureTotp();
        logout();

        selenium.type("id=username", "loginWithTotpSuccess");
        selenium.type("id=password", "password");

        selenium.click("css=input[type=\"submit\"]");
        selenium.waitForPageToLoad(DEFAULT_WAIT);

        Assert.assertEquals("Log in to demo", selenium.getTitle());

        selenium.type("id=totp", totp.generate(totpSecret));

        selenium.click("css=input[type=\"submit\"]");
        selenium.waitForPageToLoad(DEFAULT_WAIT);

        Assert.assertEquals("loginWithTotpSuccess", selenium.getText("id=user"));
    }

    @Test
    public void setupTotp() {
        registerUser("setupTotp", "password");
        configureTotp();
    }

}
