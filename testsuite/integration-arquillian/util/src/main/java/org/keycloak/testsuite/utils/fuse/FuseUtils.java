/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.utils.fuse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.jboss.arquillian.core.spi.Validate;
import org.jboss.logging.Logger;
import org.junit.Assert;

import static org.hamcrest.Matchers.*;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

public class FuseUtils {

    private static final Logger log = Logger.getLogger(FuseUtils.class);
    private static boolean initiated = false;

    private static final String managementUser = System.getProperty("app.server.management.user", "admin");
    private static final String managementPassword = System.getProperty("app.server.management.password", "password");
    private static final String additionalFuseRepos = System.getProperty("additional.fuse.repos");
    private static final String userHome = System.getProperty("user.home");
    private static final String projectVersion = System.getProperty("project.version");
    private static final String mvnRepoLocal;
    private static final String mvnLocalSettings;

    public enum Result { OK, NOT_FOUND, NO_CREDENTIALS, NO_ROLES, EMPTY };

    static {
        Validate.notNullOrEmpty(managementUser, "app.server.management.user is not set.");
        Validate.notNullOrEmpty(managementPassword, "app.server.management.password is not set.");
        Validate.notNullOrEmpty(additionalFuseRepos, "additional.fuse.repos is not set.");

        mvnRepoLocal = System.getProperty("maven.repo.local", userHome + "/.m2/repository");
        mvnLocalSettings = System.getProperty("maven.local.settings", userHome + "/.m2/settings.xml");
    }

    public static void setUpFuse(String appServer) throws IOException {
        if (!initiated) {
            switch (appServer) {
                case ContainerConstants.APP_SERVER_FUSE7X :
                    setUpFuse7();
                    break;
                case ContainerConstants.APP_SERVER_FUSE63 :
                    setUpFuse6();
                    break;
                default:
                    throw new UnsupportedOperationException(appServer + " is not supported!");
            }
            initiated = true;
        }
    }

    private static void setUpFuse7() throws IOException {
        log.debug("Going to set up fuse server");

        assertCommand(managementUser, managementPassword,
            "config:edit org.ops4j.pax.url.mvn; " +
            "config:property-set org.ops4j.pax.url.mvn.localRepository " + mvnRepoLocal + "; " +
            "config:property-set org.ops4j.pax.url.mvn.settings " + mvnLocalSettings + "; " +
            "config:property-append org.ops4j.pax.url.mvn.repositories  " + additionalFuseRepos + "; " +
            "config:update",
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "config:edit jmx.acl.org.apache.karaf.security.jmx; " +
            "config:property-append list* viewer; " +
            "config:property-append set* jmxAdmin; " +
            "config:property-append * jmxAdmin,admin; " +
            "config:update",
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "config:edit org.apache.karaf.management; " +
            "config:property-set jmxRealm keycloak;" +
            "config:update",
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "feature:repo-add mvn:org.keycloak/keycloak-osgi-features/" + projectVersion + "/xml/features; " +
            "feature:repo-add mvn:org.keycloak.testsuite/fuse-example-keycloak-features/" + projectVersion + "/xml/features; " +
            "feature:install pax-http-undertow; " +
            "feature:install keycloak-jaas keycloak-pax-http-undertow; " +
            "feature:install keycloak-fuse-7.0-example",
        Result.OK);

        assertCommand(managementUser, managementPassword,
            "config:edit --factory --alias cxf org.ops4j.pax.web.context; " +
            "config:property-set bundle.symbolicName org.apache.cxf.cxf-rt-transports-http; " +
            "config:property-set context.id default; " +
            "config:property-set context.param.keycloak.config.resolver org.keycloak.adapters.osgi.HierarchicalPathBasedKeycloakConfigResolver; " +
            "config:property-set login.config.authMethod KEYCLOAK; " +
            "config:property-set security.cxf.url /cxf/customerservice/*; " +
            "config:property-set security.cxf.roles \"admin, user\"; " +
            "config:update",
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "system:property -p hawtio.keycloakEnabled true; " +
            "system:property -p hawtio.realm keycloak; " +
            "system:property -p hawtio.keycloakClientConfig ${karaf.etc}/keycloak-hawtio-client.json; " +
            "system:property -p hawtio.keycloakServerConfig ${karaf.etc}/keycloak-bearer.json; " +
            "system:property -p hawtio.roles admin,manager,viewer,ssh; " +
            "system:property -p hawtio.rolePrincipalClasses org.keycloak.adapters.jaas.RolePrincipal,org.apache.karaf.jaas.boot.principal.RolePrincipal;" +
            "restart io.hawt.hawtio-war",
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "config:edit org.apache.karaf.shell; " +
            "config:property-set sshRealm keycloak; " +
            "config:update",
        Result.EMPTY);

        log.debug("Fuse server should be ready");
    }

    private static void setUpFuse6() throws IOException {
        log.debug("Going to set up fuse server");

        assertCommand(managementUser, managementPassword,
            "config:edit org.ops4j.pax.url.mvn; " +
            "config:propset org.ops4j.pax.url.mvn.localRepository " + mvnRepoLocal + "; " +
            "config:propset org.ops4j.pax.url.mvn.settings " + mvnLocalSettings + "; " +
            "config:propappend org.ops4j.pax.url.mvn.repositories  " + additionalFuseRepos + "; " +
            "config:update",
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "config:edit jmx.acl.org.apache.karaf.security.jmx; " +
            "config:propappend list* viewer; " +
            "config:propappend set* jmxAdmin; " +
            "config:propappend * jmxAdmin,admin; " +
            "config:update",
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "config:edit org.apache.karaf.management; " +
            "config:propset jmxRealm keycloak;" +
            "config:update",
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "features:addurl mvn:org.keycloak/keycloak-osgi-features/" + projectVersion + "/xml/features; " +
            "features:addurl mvn:org.keycloak.testsuite/fuse-example-keycloak-features/" + projectVersion + "/xml/features; " +
            "features:install keycloak-fuse-6.3-example",
        Result.OK);

        String appServerHome = System.getProperty("app.server.home");
        Validate.notNullOrEmpty(appServerHome, "app.server.home is not set.");
        assertCommand(managementUser, managementPassword,
            "system-property -p hawtio.roles admin,user; " +
            "system-property -p hawtio.keycloakEnabled true; " +
            "system-property -p hawtio.realm keycloak; " +
            "system-property -p hawtio.keycloakClientConfig file://" + appServerHome + "/etc/keycloak-hawtio-client.json; " +
            "system-property -p hawtio.rolePrincipalClasses org.keycloak.adapters.jaas.RolePrincipal,org.apache.karaf.jaas.boot.principal.RolePrincipal; ",
        Result.EMPTY);

        String output = getCommandOutput(managementUser, managementPassword, "osgi:list | grep hawtio | grep web;");
        Assert.assertThat(output, containsString("hawtio"));
        String id = output.substring(output.indexOf("[") + 1, output.indexOf("]")).trim();
        log.debug("osgi hawtio-web id: " + id);
        assertCommand(managementUser, managementPassword,
            "osgi:restart " + id,
        Result.EMPTY);

        assertCommand(managementUser, managementPassword,
            "config:edit org.apache.karaf.shell; " +
            "config:propset sshRealm keycloak; " +
            "config:update",
        Result.EMPTY);

        log.debug("Fuse server should be ready");
    }

    public static String assertCommand(String user, String password, String command, Result result) throws IOException {
        if (!command.endsWith("\n"))
            command += "\n";

        String output = getCommandOutput(user, password, command);

        log.debug("Command: " + command + ", user: " + user + ", password: " + password + ", output: " + output);

        switch(result) {
            case EMPTY:
                 Assert.assertThat(output, isEmptyString());
                 break;
            case OK:
                Assert.assertThat(output,
                    not(anyOf(
                        containsString("Insufficient credentials"), 
                        containsString("Command not found"),
                        containsString("Error executing command"),
                        containsString("Authentication failed"))
                    ));
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
    
    public static String getCommandOutput(String user, String password, String command) throws IOException {
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

    private static ClientSession openSshChannel(String username, String password) throws IOException {
        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        ConnectFuture future = client.connect(username, "localhost", 8101);
        future.await();
        ClientSession session = future.getSession();

        Set<ClientSession.ClientSessionEvent> ret = EnumSet.of(ClientSession.ClientSessionEvent.WAIT_AUTH);
        while (ret.contains(ClientSession.ClientSessionEvent.WAIT_AUTH)) {
            session.addPasswordIdentity(password);
            session.auth().verify();
            ret = session.waitFor(EnumSet.of(ClientSession.ClientSessionEvent.WAIT_AUTH, ClientSession.ClientSessionEvent.CLOSED, ClientSession.ClientSessionEvent.AUTHED), 0);
        }
        if (ret.contains(ClientSession.ClientSessionEvent.CLOSED)) {
            throw new RuntimeException("Could not open SSH channel");
        }

        return session;
    }
}
