/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.adapter.example.fuse;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.utils.fuse.FuseUtils.assertCommand;
import static org.keycloak.testsuite.utils.fuse.FuseUtils.getCommandOutput;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.Hawtio2Page;
import org.keycloak.testsuite.adapter.page.HawtioPage;
import org.keycloak.testsuite.adapter.page.fuse.AdminInterface;
import org.keycloak.testsuite.adapter.page.fuse.CustomerListing;
import org.keycloak.testsuite.adapter.page.fuse.CustomerPortalFuseExample;
import org.keycloak.testsuite.adapter.page.fuse.ProductPortalFuseExample;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.utils.fuse.FuseUtils.Result;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

@AppServerContainer(ContainerConstants.APP_SERVER_FUSE63)
@AppServerContainer(ContainerConstants.APP_SERVER_FUSE7X)
public class FuseAdapterTest extends AbstractExampleAdapterTest {

    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    @Page
    @JavascriptBrowser
    private HawtioPage hawtioPage;
    @Page
    @JavascriptBrowser
    private Hawtio2Page hawtio2Page;
    @Page
    @JavascriptBrowser
    private OIDCLogin testRealmLoginPageFuse;
    @Page
    @JavascriptBrowser
    private AuthRealm loginPageFuse;
    @Page
    @JavascriptBrowser
    protected CustomerPortalFuseExample customerPortal;
    @Page
    @JavascriptBrowser
    protected CustomerListing customerListing;
    @Page
    @JavascriptBrowser
    protected AdminInterface adminInterface;
    @Page
    @JavascriptBrowser
    protected ProductPortalFuseExample productPortal;
    @Page
    @JavascriptBrowser
    protected Account testRealmAccount;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation fuseRealm = loadRealm(new File(TEST_APPS_HOME_DIR + "/fuse/demorealm.json"));
        testRealms.add(fuseRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmLoginPageFuse.setAuthRealm(DEMO);
        testRealmPage.setAuthRealm(DEMO);
        testRealmLoginPage.setAuthRealm(DEMO);
        testRealmAccount.setAuthRealm(DEMO);
        loginPageFuse.setAuthRealm(DEMO);
    }

    @Before
    public void addJsDriver() {
        DroneUtils.addWebDriver(jsDriver);
    }

    @Override
    public boolean isImportAfterEachMethod() {
        return false;
    }

    @Test
    @AppServerContainer(value = ContainerConstants.APP_SERVER_FUSE7X, skip = true)
    public void hawtio1LoginTest() throws Exception {
        hawtioPage.navigateTo();
        WaitUtils.waitForPageToLoad();
        assertCurrentUrlStartsWith(loginPageFuse);
        testRealmLoginPageFuse.form().login("user", "invalid-password");
        assertCurrentUrlStartsWith(loginPageFuse);

        testRealmLoginPageFuse.form().login("invalid-user", "password");
        assertCurrentUrlStartsWith(loginPageFuse);

        testRealmLoginPageFuse.form().login("root", "password");
        assertCurrentUrlStartsWith(hawtioPage.toString() + "/welcome");
        hawtioPage.logout();
        assertCurrentUrlStartsWith(testRealmLoginPageFuse);

        hawtioPage.navigateTo();
        log.debug("logging in as mary");
        testRealmLoginPageFuse.form().login("mary", "password");
        log.debug("Previous WARN waitForPageToLoad time exceeded! is expected");
        assertThat(DroneUtils.getCurrentDriver().getPageSource(), 
                allOf(
                    containsString("Unauthorized User"),
                    not(containsString("welcome"))
                )
        );
    }

    @Test
    @AppServerContainer(value = ContainerConstants.APP_SERVER_FUSE63, skip = true)
    public void hawtio2LoginTest() throws Exception {

        Assume.assumeTrue("This test doesn't work with phantomjs", !"phantomjs".equals(System.getProperty("js.browser")));

        hawtio2Page.navigateTo();
        WaitUtils.waitForPageToLoad();

        assertCurrentUrlStartsWith(loginPageFuse);
        testRealmLoginPageFuse.form().login("user", "invalid-password");
        assertCurrentUrlStartsWith(loginPageFuse);

        testRealmLoginPageFuse.form().login("invalid-user", "password");
        assertCurrentUrlStartsWith(loginPageFuse);

        log.debug("logging in as root");
        testRealmLoginPageFuse.form().login("root", "password");
        assertCurrentUrlStartsWith(hawtio2Page.toString());
        
        assertHawtio2Page("camel", true);
        assertHawtio2Page("jmx", true);
        assertHawtio2Page("osgi", true);
        assertHawtio2Page("logs", true);

        hawtio2Page.logout();
        WaitUtils.waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPageFuse);

        hawtio2Page.navigateTo();
        WaitUtils.waitForPageToLoad();

        log.debug("logging in as mary");
        testRealmLoginPageFuse.form().login("mary", "password");
        log.debug("Current URL: " + DroneUtils.getCurrentDriver().getCurrentUrl());
        assertCurrentUrlStartsWith(hawtio2Page.toString());
        
        assertHawtio2Page("camel", false);
        assertHawtio2Page("jmx", false);
        assertHawtio2Page("osgi", false);
        assertHawtio2Page("logs", false);
    }

    private void assertHawtio2Page(String urlFragment, boolean expectedSuccess) {
        DroneUtils.getCurrentDriver().navigate().to(hawtio2Page.getUrl() + "/" + urlFragment);
        WaitUtils.waitForPageToLoad();
        WaitUtils.waitUntilElement(By.xpath("//img[@alt='Red Hat Fuse Management Console'] | //img[@ng-src='img/fuse-logo.svg']")).is().present();
        if (expectedSuccess) {
            assertCurrentUrlStartsWith(hawtio2Page.getUrl() + "/" + urlFragment);
        } else {
            assertCurrentUrlStartsWith(hawtio2Page.getUrl() + "/jvm");
        }
    }

    @Test
    @AppServerContainer(value = ContainerConstants.APP_SERVER_FUSE7X, skip = true)
    public void sshLoginTestFuse6() throws Exception {
        assertCommand("mary", "password", "shell:date", Result.NO_CREDENTIALS);
        assertCommand("john", "password", "shell:info", Result.NO_CREDENTIALS);
        assertCommand("john", "password", "shell:date", Result.OK);
        assertCommand("root", "password", "shell:info", Result.OK);
    }

    @Test
    @AppServerContainer(value = ContainerConstants.APP_SERVER_FUSE63, skip = true)
    public void sshLoginTestFuse7() throws Exception {
        assertCommand("mary", "password", "shell:date", Result.NOT_FOUND);
        assertCommand("john", "password", "shell:info", Result.NOT_FOUND);
        assertCommand("john", "password", "shell:date", Result.OK);
        assertRoles("root", 
          "ssh",
          "jmxAdmin",
          "admin",
          "manager",
          "viewer",
          "Administrator",
          "Auditor",
          "Deployer",
          "Maintainer",
          "Operator",
          "SuperUser"
        );
    }

    private void assertRoles(String username, String... expectedRoles) throws Exception {
        final String commandOutput = getCommandOutput(username, "password", "jaas:whoami -r --no-format");
        final List<String> parsedOutput = Arrays.asList(commandOutput.split("\\n+"));
        assertThat(parsedOutput, containsInAnyOrder(expectedRoles));
    }

    @Test
    public void jmxLoginTest() throws Exception {
        ObjectName mbean = new ObjectName("org.apache.karaf:type=config,name=root");
        log.debug("jmxLoginTest - testing: invalid credentials");
        try (JMXConnector jmxConnector = getJMXConnector(10, TimeUnit.SECONDS, "mary", "password1")) {
            jmxConnector.getMBeanServerConnection();
            Assert.fail();
        } catch (TimeoutException expected) {
            assertThat(expected.getCause().toString(), containsString("java.lang.SecurityException: Authentication failed"));
        }
        log.debug("jmxLoginTest - testing: no role");
        try (JMXConnector jmxConnector = getJMXConnector("mary", "password")) {
            MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
            assertJmxInvoke(false, connection, mbean, "listProperties", new Object [] {""}, new String [] {String.class.getName()});
            assertJmxInvoke(false, connection, mbean, "setProperty", new Object [] {"", "x", "y"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        }
        log.debug("jmxLoginTest - testing: read only role");
        try (JMXConnector jmxConnector = getJMXConnector("john", "password")) {
            MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
            assertJmxInvoke(true, connection, mbean, "listProperties", new Object [] {""}, new String [] {String.class.getName()});
            assertJmxInvoke(false, connection, mbean, "setProperty", new Object [] {"", "x", "y"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        }
        log.debug("jmxLoginTest - testing: read write role");
        try (JMXConnector jmxConnector = getJMXConnector("root", "password")) {
            MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
            assertJmxInvoke(true, connection, mbean, "listProperties", new Object [] {""}, new String [] {String.class.getName()});
            assertJmxInvoke(true, connection, mbean, "setProperty", new Object [] {"", "x", "y"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        }
    }

    private Object assertJmxInvoke(boolean expectSuccess, MBeanServerConnection connection, ObjectName mbean, String method,
            Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        try {
            Object result = connection.invoke(mbean, method, params, signature);
            assertTrue(expectSuccess);
            return result;
        } catch (SecurityException se) {
            assertTrue(!expectSuccess);
            return null;
        }
    }

    private JMXConnector getJMXConnector(String username, String password) throws Exception {
        return getJMXConnector(2, TimeUnit.MINUTES, username, password);
    }

    private JMXConnector getJMXConnector(long timeout, TimeUnit unit, String username, String password) throws Exception {
        Exception lastException = null;
        long timeoutMillis = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < timeoutMillis) {
            try {
                Map<String, ?> env = Collections.singletonMap(JMXConnector.CREDENTIALS, new String[] { username, password });
                return JMXConnectorFactory.connect(new JMXServiceURL(getJmxServiceUrl()), env);
            } catch (Exception ex) {
                lastException = ex;
                Thread.sleep(500);
                log.debug("Loop: Getting MBean Server Connection: last caught exception: " + lastException.getClass().getName());
            }
        }
        log.error("Failed to get MBean Server Connection within " + timeout + " " + unit.toString());
        TimeoutException timeoutException = new TimeoutException();
        timeoutException.initCause(lastException);
        throw timeoutException;
    }

    private String getJmxServiceUrl() throws Exception {
        return "service:jmx:rmi://localhost:44444/jndi/rmi://localhost:1099/karaf-root";
    }

    @Test
    public void testCustomerListingAndAccountManagement() {
        customerPortal.navigateTo();
        assertCurrentUrlStartsWith(customerPortal);

        customerPortal.clickCustomerListingLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPageFuse.form().login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(customerListing);

        assertThat(DroneUtils.getCurrentDriver().getPageSource(), allOf(
            containsString("Username: bburke@redhat.com"),
            containsString("Bill Burke"),
            containsString("Stian Thorgersen")
        ));

        // account mgmt
        customerListing.clickAccountManagement();

        assertCurrentUrlStartsWith(testRealmAccount);
        assertThat(testRealmAccount.getUsername(), equalTo("bburke@redhat.com"));

        DroneUtils.getCurrentDriver().navigate().back();
        customerListing.clickLogOut();

        // assert user not logged in
        customerPortal.clickCustomerListingLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

    }

    @Test
    public void testAdminInterface() {
        customerPortal.navigateTo();
        assertCurrentUrlStartsWith(customerPortal);

        customerPortal.clickAdminInterfaceLink();
        WaitUtils.waitForPageToLoad();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPageFuse.form().login("admin", "password");
        assertCurrentUrlStartsWith(adminInterface);
        assertThat(DroneUtils.getCurrentDriver().getPageSource(), containsString("Hello admin!"));
        assertThat(DroneUtils.getCurrentDriver().getPageSource(), containsString("This second sentence is returned from a Camel RestDSL endpoint"));

        customerListing.navigateTo();
        WaitUtils.waitForPageToLoad();
        customerListing.clickLogOut();
        WaitUtils.waitForPageToLoad();

        WaitUtils.pause(2500);
        customerPortal.navigateTo();//needed for phantomjs
        WaitUtils.waitForPageToLoad();
        customerPortal.clickAdminInterfaceLink();
        WaitUtils.waitForPageToLoad();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPageFuse.form().login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(adminInterface);
        assertThat(DroneUtils.getCurrentDriver().getPageSource(), containsString("Status code is 403"));
    }

    @Test
    public void testProductPortal() {
        productPortal.navigateTo();
        WaitUtils.waitForPageToLoad();

        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPageFuse.form().login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(productPortal);

        assertThat(productPortal.getProduct1UnsecuredText(), containsString("401: Unauthorized"));
        assertThat(productPortal.getProduct1SecuredText(), containsString("Product received: id=1"));
        assertThat(productPortal.getProduct2SecuredText(), containsString("Product received: id=2"));

        productPortal.clickLogOutLink();
        WaitUtils.waitForPageToLoad();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }
}
