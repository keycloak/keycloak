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

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.testsuite.pages.ChangePasswordPage;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@RunWith(Arquillian.class)
public class ResetPasswordTest extends AbstractDroneTest {

    @Deployment(name = "properties", testable = false, order = 1)
    public static WebArchive propertiesDeployment() {
        return ShrinkWrap.create(WebArchive.class, "properties.war").addClass(SystemPropertiesSetter.class)
                .addAsWebInfResource("web-properties-email-verfication.xml", "web.xml");
    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected ChangePasswordPage changePasswordPage;

    @Test
    public void resetPassword() throws IOException, MessagingException {
        appPage.open();

        Assert.assertTrue(loginPage.isCurrent());
        
        // TODO Replace with clicking reset password link when added
        String url = browser.getCurrentUrl();
        url = url.replace("tokens/login", "account/password-reset");
        url = url + "&username=bburke@redhat.com";

        browser.navigate().to(url);

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String body = (String) message.getContent();
        String changePasswordUrl = body.split("\n")[0];

        browser.navigate().to(changePasswordUrl.trim());

        changePasswordPage.changePassword("new-password", "new-password");

        Assert.assertTrue(loginPage.isCurrent());

        loginPage.login("bburke@redhat.com", "password");
        Assert.assertTrue(loginPage.isCurrent());
        Assert.assertEquals("Invalid username or password", loginPage.getError());

        loginPage.login("bburke@redhat.com", "new-password");

        Assert.assertTrue(appPage.isCurrent());
        Assert.assertEquals("bburke@redhat.com", appPage.getUser());
    }

    @Test
    public void tempPassword() {
        appPage.open();

        Assert.assertTrue(loginPage.isCurrent());

        loginPage.login("reset@pass.com", "temp-password");

        Assert.assertTrue(changePasswordPage.isCurrent());

        changePasswordPage.changePassword("new-password", "new-password");

        Assert.assertTrue(loginPage.isCurrent());

        loginPage.login("reset@pass.com", "new-password");

        Assert.assertTrue(appPage.isCurrent());
        Assert.assertEquals("reset@pass.com", appPage.getUser());
    }

}
