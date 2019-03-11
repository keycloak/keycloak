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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.ClientSession.ClientSessionEvent;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.Hawtio2Page;
import org.keycloak.testsuite.adapter.page.HawtioPage;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.containers.ContainerConstants;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

@AppServerContainer(ContainerConstants.APP_SERVER_FUSE63)
@AppServerContainer(ContainerConstants.APP_SERVER_FUSE7X)
public class FuseAdminAdapterTest extends AbstractExampleAdapterTest {

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

    private SshClient client;

    protected enum Result { OK, NOT_FOUND, NO_CREDENTIALS, NO_ROLES };

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation fuseRealm = loadRealm(new File(TEST_APPS_HOME_DIR + "/fuse/demorealm.json"));
        testRealms.add(fuseRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmLoginPageFuse.setAuthRealm(DEMO);
    }

    @Before
    public void addJsDriver() {
        DroneUtils.addWebDriver(jsDriver);
    }

    @Test
    public void hawtio1LoginTest() throws Exception {
        // Note that this does work only in Fuse 6 with Hawtio 1, Fuse 7 contains Hawtio 2
        ContainerAssume.assumeNotAppServerFuse7();

        hawtioPage.navigateTo();
        testRealmLoginPageFuse.form().login("user", "invalid-password");
        assertCurrentUrlDoesntStartWith(hawtioPage);

        testRealmLoginPageFuse.form().login("invalid-user", "password");
        assertCurrentUrlDoesntStartWith(hawtioPage);

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
    public void hawtio2LoginTest() throws Exception {
        // Note that this does work only in Fuse 7 with Hawtio 2, Fuse 6 contains Hawtio 1
        ContainerAssume.assumeNotAppServerFuse6();

        hawtio2Page.navigateTo();
        WaitUtils.waitForPageToLoad();

        testRealmLoginPageFuse.form().login("user", "invalid-password");
        assertCurrentUrlDoesntStartWith(hawtio2Page);

        testRealmLoginPageFuse.form().login("invalid-user", "password");
        assertCurrentUrlDoesntStartWith(hawtio2Page);

        testRealmLoginPageFuse.form().login("root", "password");
        assertCurrentUrlStartsWith(hawtio2Page.toString());
        WaitUtils.waitForPageToLoad();
//        WaitUtils.waitUntilElement(By.xpath("//img[@alt='Red Hat Fuse Management Console']")).is().present();
        hawtio2Page.logout();
        WaitUtils.waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPageFuse);

        hawtio2Page.navigateTo();
        WaitUtils.waitForPageToLoad();

        log.debug("logging in as mary");
        testRealmLoginPageFuse.form().login("mary", "password");
        log.debug("Current URL: " + DroneUtils.getCurrentDriver().getCurrentUrl());
        assertCurrentUrlStartsWith(hawtio2Page.toString());
        WaitUtils.waitForPageToLoad();
//        WaitUtils.waitUntilElement(By.xpath("//img[@alt='Red Hat Fuse Management Console']")).is().present();
        assertThat(DroneUtils.getCurrentDriver().getPageSource(), not(containsString("Camel")));
    }    

    @Test
    public void sshLoginTestFuse6() throws Exception {
        // Note that this does not work for Fuse 7 since the error codes have changed
        ContainerAssume.assumeNotAppServerFuse7();

        assertCommand("mary", "password", "shell:date", Result.NO_CREDENTIALS);
        assertCommand("john", "password", "shell:info", Result.NO_CREDENTIALS);
        assertCommand("john", "password", "shell:date", Result.OK);
        assertCommand("root", "password", "shell:info", Result.OK);
    }

    @Test
    public void sshLoginTestFuse7() throws Exception {
        // Note that this works for Fuse 7 and newer
        ContainerAssume.assumeNotAppServerFuse6();

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
        assertThat(parsedOutput, Matchers.containsInAnyOrder(expectedRoles));
    }

    @Test
    public void jmxLoginTest() throws Exception {
        setJMXAuthentication("keycloak", "password");
        ObjectName mbean = new ObjectName("org.apache.karaf:type=config,name=root");
        //invalid credentials
        try {
            getJMXConnector("mary", "password1").getMBeanServerConnection();
            Assert.fail();
        } catch (SecurityException se) {}
        //no role
        MBeanServerConnection connection = getJMXConnector("mary", "password").getMBeanServerConnection();
        assertJmxInvoke(false, connection, mbean, "listProperties", new Object [] {""}, new String [] {String.class.getName()});
        assertJmxInvoke(false, connection, mbean, "setProperty", new Object [] {"", "x", "y"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        //read only role  
        connection = getJMXConnector("john", "password").getMBeanServerConnection();
        assertJmxInvoke(true, connection, mbean, "listProperties", new Object [] {""}, new String [] {String.class.getName()});
        assertJmxInvoke(false, connection, mbean, "setProperty", new Object [] {"", "x", "y"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        //read write role
        connection = getJMXConnector("root", "password").getMBeanServerConnection();
        assertJmxInvoke(true, connection, mbean, "listProperties", new Object [] {""}, new String [] {String.class.getName()});
        assertJmxInvoke(true, connection, mbean, "setProperty", new Object [] {"", "x", "y"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        setJMXAuthentication("karaf", "admin");
    }

    protected String assertCommand(String user, String password,  String command, Result result) throws Exception, IOException {
        if (!command.endsWith("\n"))
            command += "\n";

        String output = getCommandOutput(user, password, command);

        switch(result) {
        case OK:
            Assert.assertThat(output,
                    not(anyOf(containsString("Insufficient credentials"), Matchers.containsString("Command not found"))));
            break;
        case NOT_FOUND:
            Assert.assertThat(output,
                    containsString("Command not found"));
            break;
        case NO_CREDENTIALS:
            Assert.assertThat(output,
                    containsString("Insufficient credentials"));
            break;
        case NO_ROLES:
            Assert.assertThat(output,
                    containsString("Current user has no associated roles"));
            break;
        default:
            Assert.fail("Unexpected enum value: " + result);
        }

        return output;
    }
    
    protected String getCommandOutput(String user, String password, String command) throws Exception, IOException {
        if (!command.endsWith("\n"))
            command += "\n";

        try (ClientSession session = openSshChannel(user, password);
          ChannelExec channel = session.createExecChannel(command);
          ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            channel.setOut(out);
            channel.setErr(out);
            channel.open();
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED, ClientChannelEvent.EOF), 0);

            return new String(out.toByteArray());
        }
    }

    protected ClientSession openSshChannel(String username, String password) throws Exception {
        client = SshClient.setUpDefaultClient();
        client.start();
        ConnectFuture future = client.connect(username, "localhost", 8101);
        future.await();
        ClientSession session = future.getSession();

        Set<ClientSessionEvent> ret = EnumSet.of(ClientSessionEvent.WAIT_AUTH);
        while (ret.contains(ClientSessionEvent.WAIT_AUTH)) {
            session.addPasswordIdentity(password);
            session.auth().verify();
            ret = session.waitFor(EnumSet.of(ClientSessionEvent.WAIT_AUTH, ClientSessionEvent.CLOSED, ClientSessionEvent.AUTHED), 0);
        }
        if (ret.contains(ClientSessionEvent.CLOSED)) {
            throw new Exception("Could not open SSH channel");
        }

        return session;
    }

    protected void setJMXAuthentication(String realm, String password) throws Exception {
       assertCommand("admin", "password", "config:edit org.apache.karaf.management; config:propset jmxRealm " + realm + "; config:update", Result.OK);
       getMBeanServerConnection(10000, TimeUnit.MILLISECONDS, "admin", password);
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
    
    private MBeanServerConnection getMBeanServerConnection(long timeout, final TimeUnit unit, String username, String password) throws Exception {
        Exception lastException = null;
        long timeoutMillis = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < timeoutMillis) {
            try {
                return getJMXConnector(username, password).getMBeanServerConnection();
            } catch (Exception ex) {
                lastException = ex;
                Thread.sleep(500);
                ex.printStackTrace();
            }
        }
        TimeoutException timeoutException = new TimeoutException();
        timeoutException.initCause(lastException);
        throw timeoutException;
    }
    
    private JMXConnector getJMXConnector(String userName, String password) throws Exception {
        JMXServiceURL url = new JMXServiceURL(getJmxServiceUrl());
        String[] credentials = new String[] { userName, password };
        Map<String, ?> env = Collections.singletonMap(JMXConnector.CREDENTIALS, credentials);
        JMXConnector connector = JMXConnectorFactory.connect(url, env);
        return connector;
    }

    private String getJmxServiceUrl() throws Exception {
        return "service:jmx:rmi://localhost:44444/jndi/rmi://localhost:1099/karaf-root";
    }

}
