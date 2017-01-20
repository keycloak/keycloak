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
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.ClientSession.ClientSessionEvent;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.HawtioPage;

public abstract class AbstractFuseAdminAdapterTest extends AbstractExampleAdapterTest {
    
    @Page
    private HawtioPage hawtioPage;
    
    private SshClient client;
    
    private ClientChannel channel;
    
    private ClientSession session;
    
    enum Result { OK, NOT_FOUND, NO_CREDENTIALS, NO_ROLES };
    
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
        hawtioPage.navigateTo();
        testRealmLoginPage.form().login("user", "invalid-password");
        assertCurrentUrlDoesntStartWith(hawtioPage);

        testRealmLoginPage.form().login("invalid-user", "password");
        assertCurrentUrlDoesntStartWith(hawtioPage);

        testRealmLoginPage.form().login("root", "password");
        assertCurrentUrlStartsWith(hawtioPage.getDriver(), hawtioPage.toString() + "/welcome");
        hawtioPage.logout();
        assertCurrentUrlStartsWith(testRealmLoginPage);
        
        hawtioPage.navigateTo();
        testRealmLoginPage.form().login("mary", "password");
        assertTrue(!driver.getPageSource().contains("welcome"));
    }
    
    
    
    @Test
    public void sshLoginTest() throws Exception {
        assertCommand("mary", "password", "shell:date", Result.NO_ROLES);
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

    private String assertCommand(String user, String password,  String command, Result result) throws Exception, IOException {
        if (!command.endsWith("\n"))
            command += "\n";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pipe = openSshChannel(user, password, out, out);
        pipe.write(command.getBytes());
        pipe.flush();

        closeSshChannel(pipe);
        String output = new String(out.toByteArray());

        switch(result) {
        case OK:
            Assert.assertFalse("Should not contain 'Insufficient credentials' or 'Command not found': " + output,
                    output.contains("Insufficient credentials") || output.contains("Command not found"));
            break;
        case NOT_FOUND:
            Assert.assertTrue("Should contain 'Command not found': " + output,
                    output.contains("Command not found"));
            break;
        case NO_CREDENTIALS:
            Assert.assertTrue("Should contain 'Insufficient credentials': " + output,
                    output.contains("Insufficient credentials"));
            break;
        case NO_ROLES:
            Assert.assertTrue("Should contain 'Current user has no associated roles': " + output,
                    output.contains("Current user has no associated roles"));
            break;
        default:
            Assert.fail("Unexpected enum value: " + result);
        }
        return output;
    }
    
    private OutputStream openSshChannel(String username, String password, OutputStream ... outputs) throws Exception {
        client = SshClient.setUpDefaultClient();
        client.start();
        ConnectFuture future = client.connect(username, "localhost", 8101);
        future.await();
        session = future.getSession();

        Set<ClientSessionEvent> ret = EnumSet.of(ClientSessionEvent.WAIT_AUTH);
        while (ret.contains(ClientSessionEvent.WAIT_AUTH)) {
            session.addPasswordIdentity(password);
            session.auth().verify();
            ret = session.waitFor(EnumSet.of(ClientSessionEvent.WAIT_AUTH, ClientSessionEvent.CLOSED, ClientSessionEvent.AUTHED), 0);
        }
        if (ret.contains(ClientSessionEvent.CLOSED)) {
            throw new Exception("Could not open SSH channel");
        }
        channel = session.createChannel("shell");
        PipedOutputStream pipe = new PipedOutputStream();
        channel.setIn(new PipedInputStream(pipe));

        OutputStream out;
        if (outputs.length >= 1) {
            out = outputs[0];
        } else {
            out = new ByteArrayOutputStream();
        }
        channel.setOut(out);

        OutputStream err;
        if (outputs.length >= 2) {
            err = outputs[1];
        } else {
            err = new ByteArrayOutputStream();
        }
        channel.setErr(err);
        channel.open();

        return pipe;
    }
    
    private void setJMXAuthentication(String realm, String password) throws Exception {
       assertCommand("admin", "password", "config:edit org.apache.karaf.management; config:propset jmxRealm " + realm + "; config:update", Result.OK);
       getMBeanServerConnection(10000, TimeUnit.MILLISECONDS, "admin", password);
    }
    
    private void closeSshChannel(OutputStream pipe) throws IOException {
        pipe.write("logout\n".getBytes());
        pipe.flush();

        channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
        session.close(true);
        client.stop();

        client = null;
        channel = null;
        session = null;
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
