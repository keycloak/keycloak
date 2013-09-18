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
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.openqa.selenium.WebDriver;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@RunWith(Arquillian.class)
public class RequiredActionEmailVerificationTest {

    @Deployment(name = "app", testable = false, order = 3)
    public static WebArchive appDeployment() {
        return Deployments.appDeployment();
    }

    @Deployment(name = "auth-server", testable = false, order = 2)
    public static WebArchive deployment() {
        return Deployments.deployment().addAsResource("testrealm-email.json", "META-INF/testrealm.json");
    }

    @Deployment(name = "properties", testable = false, order = 1)
    public static WebArchive propertiesDeployment() {
        return ShrinkWrap.create(WebArchive.class, "properties.war").addClass(SystemPropertiesSetter.class)
                .addAsWebInfResource("web-properties-email-verfication.xml", "web.xml");
    }

    @Page
    protected AppPage appPage;

    @Drone
    protected WebDriver browser;

    @Page
    protected LoginPage loginPage;

    @Page
    protected RegisterPage registerPage;

    private GreenMail greenMail;

    @Before
    public void before() {
        ServerSetup setup = new ServerSetup(3025, "localhost", "smtp");

        greenMail = new GreenMail(setup);
        greenMail.start();
    }

    @After
    public void after() {
        appPage.open();
        if (appPage.isCurrent()) {
            appPage.logout();
        }
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    @Test
    public void verifyEmail() throws IOException, MessagingException {
        appPage.open();

        loginPage.register();
        registerPage.register("name", "email", "verifyEmail", "password", "password");

        Assert.assertTrue(browser.getPageSource().contains("Please verify your email address"));

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String body = (String) message.getContent();
        String verificationUrl = body.split("\n")[0];

        browser.navigate().to(verificationUrl.trim());

        Assert.assertTrue(appPage.isCurrent());
        Assert.assertEquals("verifyEmail", appPage.getUser());
    }

}
