package org.keycloak.testsuite.adapter.example;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.ClientSession.ClientSessionEvent;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.HawtioPage;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannel.Streaming;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public abstract class AbstractFuseAdminAdapterTest extends AbstractExampleAdapterTest {
    
    @Page
    private HawtioPage hawtioPage;
    
    private SshClient client;
    
    protected enum Result { OK, NOT_FOUND, NO_CREDENTIALS, NO_ROLES };
    
    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation fuseRealm = loadRealm(new File(EXAMPLES_HOME_DIR + "/fuse/demorealm.json"));
        testRealms.add(fuseRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(DEMO);
        testRealmLoginPage.setAuthRealm(DEMO);
    }
        
    @Test
    public void hawtioLoginTest() throws Exception {
        // Note that this does works only in Fuse 6 with Hawtio 1 since Fuse 7 contains Hawtio 2, and is thus overriden in Fuse 7 test classes
        hawtioPage.navigateTo();
        testRealmLoginPage.form().login("user", "invalid-password");
        assertCurrentUrlDoesntStartWith(hawtioPage);

        testRealmLoginPage.form().login("invalid-user", "password");
        assertCurrentUrlDoesntStartWith(hawtioPage);

        testRealmLoginPage.form().login("root", "password");
        assertCurrentUrlStartsWith(hawtioPage.toString() + "/welcome", hawtioPage.getDriver());
        hawtioPage.logout();
        assertCurrentUrlStartsWith(testRealmLoginPage);
        
        hawtioPage.navigateTo();
        testRealmLoginPage.form().login("mary", "password");
        assertThat(driver.getPageSource(), not(containsString("welcome")));
    }
    
    
    
    @Test
    public void sshLoginTest() throws Exception {
        // Note that this does not work for Fuse 7 since the error codes have changed, and is thus overriden for Fuse 7 test classes
        assertCommand("mary", "password", "shell:date", Result.NO_CREDENTIALS);
        assertCommand("john", "password", "shell:info", Result.NO_CREDENTIALS);
        assertCommand("john", "password", "shell:date", Result.OK);
        assertCommand("root", "password", "shell:info", Result.OK);
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
