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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.thoughtworks.selenium.DefaultSelenium;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractDroneTest {

    @Deployment(name = "app", testable = false, order = 2)
    public static WebArchive appDeployment() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml")
                .resolve("org.keycloak:keycloak-core", "org.keycloak:keycloak-as7-adapter").withoutTransitivity().asFile();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "app.war").addClasses(TestApplication.class)
                .addAsLibraries(libs).addAsWebInfResource("jboss-deployment-structure.xml")
                .addAsWebInfResource("app-web.xml", "web.xml").addAsWebInfResource("app-jboss-web.xml", "jboss-web.xml")
                .addAsWebInfResource("app-resteasy-oauth.json", "resteasy-oauth.json").addAsWebResource("user.jsp");
        return archive;
    }

    @Deployment(name = "auth-server", testable = false, order = 1)
    public static WebArchive deployment() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity()
                .asFile();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "auth-server.war").addClasses(TestApplication.class)
                .addAsLibraries(libs).addAsWebInfResource("jboss-deployment-structure.xml").addAsWebInfResource("web.xml")
                .addAsResource("persistence.xml", "META-INF/persistence.xml")
                .addAsResource("testrealm.json", "META-INF/testrealm.json");

        return archive;
    }

    URL appUrl;

    URL authServerUrl;

    String DEFAULT_WAIT = "10000";

    @Drone
    DefaultSelenium selenium;

    @After
    public void after() {
        logout();
    }

    @Before
    public void before() throws MalformedURLException {
        authServerUrl = new URL("http://localhost:8080/auth-server");
        appUrl = new URL("http://localhost:8080/app/user.jsp");
    }

    public void login(String username, String password) {
        login(username, password, null);
    }

    public void login(String username, String password, String expectErrorMessage) {
        selenium.open(appUrl.toString());
        selenium.waitForPageToLoad(DEFAULT_WAIT);

        Assert.assertEquals("Log in to demo", selenium.getTitle());

        if (username != null) {
            selenium.type("id=username", username);
        }

        if (password != null) {
            selenium.type("id=password", password);
        }

        selenium.click("css=input[type=\"submit\"]");

        selenium.waitForPageToLoad(DEFAULT_WAIT);

        if (expectErrorMessage == null) {
            Assert.assertEquals(username, selenium.getText("id=user"));
        } else {
            Assert.assertTrue(selenium.isTextPresent(expectErrorMessage));
        }
    }

    public void logout() {
        selenium.open(authServerUrl + "/rest/realms/demo/tokens/logout?redirect_uri=" + appUrl);
        selenium.waitForPageToLoad(DEFAULT_WAIT);

        Assert.assertEquals("Log in to demo", selenium.getTitle());
    }

    public void registerUser(String username, String password) {
        registerUser(username, password, null);
    }

    public void registerUser(String username, String password, String expectErrorMessage) {
        selenium.open(appUrl.toString());
        selenium.waitForPageToLoad(DEFAULT_WAIT);

        selenium.click("link=Register");
        selenium.waitForPageToLoad(DEFAULT_WAIT);
        selenium.type("id=name", "Test User");
        selenium.type("id=email", "test@user.com");
        if (username != null) {
            selenium.type("id=username", username);
        }
        if (password != null) {
            selenium.type("id=password", password);
            selenium.type("id=password-confirm", password);
        }
        selenium.click("css=input[type=\"submit\"]");
        selenium.waitForPageToLoad(DEFAULT_WAIT);

        if (expectErrorMessage == null) {
            Assert.assertEquals(username, selenium.getText("id=user"));
        } else {
            Assert.assertTrue(selenium.isTextPresent(expectErrorMessage));
        }
    }

}
