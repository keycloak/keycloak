/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.spi.infinispan.impl.embedded;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.jgroups.protocol.KEYCLOAK_JDBC_PING2;
import org.keycloak.models.KeycloakSessionFactory;

import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.remoting.transport.jgroups.EmbeddedJGroupsChannelConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests that a custom stack extending TCP and removing FD_SOCK2 gets the database connection factory injected into {@link KEYCLOAK_JDBC_PING2}.
 */
public class JGroupsConfiguratorCustomStackTest {

    @BeforeClass
    public static void initProfile() {
        Profile.defaults();
    }

    @AfterClass
    public static void resetProfile() {
        Profile.reset();
    }

    private static final String PING_PROTOCOL = "<jg:org.keycloak.jgroups.protocol.KEYCLOAK_JDBC_PING2 stack.combine=\"REPLACE\" stack.position=\"MPING\" write_data_on_find=\"false\"/>";

    private static ConfigurationBuilderHolder parse(String discovery) {
        return new ParserRegistry().parse("""
                <infinispan xmlns="urn:infinispan:config:16.0" xmlns:jg="urn:org:jgroups">
                  <jgroups>
                    <stack name="custom" extends="tcp">
                      <jg:FD_SOCK2 stack.combine="REMOVE"/>
                      <jg:VERIFY_SUSPECT2 timeout="5000" num_msgs="3" stack.combine="COMBINE"/>
                      %s
                    </stack>
                  </jgroups>
                  <cache-container>
                    <transport cluster="test" stack="custom"/>
                  </cache-container>
                </infinispan>
                """.formatted(discovery));
    }

    private static KeycloakSessionFactory sessionFactory(AtomicBoolean called) {
        var jpaFactory = Proxy.newProxyInstance(
                JpaConnectionProviderFactory.class.getClassLoader(),
                new Class<?>[]{JpaConnectionProviderFactory.class},
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        called.set(true);
                    }
                    return null;
                });
        return (KeycloakSessionFactory) Proxy.newProxyInstance(
                KeycloakSessionFactory.class.getClassLoader(),
                new Class<?>[]{KeycloakSessionFactory.class},
                (proxy, method, args) -> "getProviderFactory".equals(method.getName()) ? jpaFactory : null);
    }

    // Exposes the protected connection lookup
    private static class TestPing extends KEYCLOAK_JDBC_PING2 {
        void connect() {
            try {
                getConnection();
            } catch (SQLException e) {
                // the stub factory returns no connection; only the factory invocation matters here
            }
        }
    }

    @Test
    public void testFactoryInjectedIntoCustomStack() throws Exception {
        var holder = parse(PING_PROTOCOL);
        var called = new AtomicBoolean();

        assertTrue(JGroupsConfigurator.injectConnectionFactoryIntoCustomStack(holder, sessionFactory(called), "JGROUPS_PING", "custom"));

        var transport = holder.getGlobalConfigurationBuilder().transport();
        assertEquals("custom-keycloak", transport.attributes().attribute(TransportConfiguration.STACK).get());

        var configurator = (EmbeddedJGroupsChannelConfigurator) transport.build().transport().jgroups().configurator("custom-keycloak");
        var protocols = configurator.getProtocolStack();
        assertTrue(protocols.stream().anyMatch(p -> p.getProtocolName().equals(KEYCLOAK_JDBC_PING2.class.getName())));
        assertTrue(protocols.stream().noneMatch(p -> p.getProtocolName().contains("FD_SOCK2")));
        assertTrue(protocols.stream().noneMatch(p -> p.getProtocolName().contains("MPING")));
        var verifySuspect = protocols.stream().filter(p -> p.getProtocolName().equals("VERIFY_SUSPECT2")).findFirst().orElseThrow();
        assertEquals("5000", verifySuspect.getProperties().get("timeout"));

        var ping = protocols.stream().filter(p -> p.getProtocolName().equals(KEYCLOAK_JDBC_PING2.class.getName())).findFirst().orElseThrow();
        assertEquals("DELETE from JGROUPS_PING WHERE cluster_name=?", ping.getProperties().get("clear_sql"));
        assertEquals("", ping.getProperties().get("initialize_sql"));
        // explicit XML values win over the defaults
        assertEquals("false", ping.getProperties().get("write_data_on_find"));
        var ping2 = new TestPing();
        configurator.afterCreation(ping2);
        ping2.connect();
        assertTrue("The connection factory should be used by the protocol", called.get());
    }

    @Test
    public void testWrapperNameAvoidsExistingStack() {
        var holder = new ParserRegistry().parse("""
                <infinispan xmlns="urn:infinispan:config:16.0" xmlns:jg="urn:org:jgroups">
                  <jgroups>
                    <stack name="custom" extends="tcp">
                      %s
                    </stack>
                    <stack name="custom-keycloak" extends="tcp"/>
                  </jgroups>
                  <cache-container>
                    <transport cluster="test" stack="custom"/>
                  </cache-container>
                </infinispan>
                """.formatted(PING_PROTOCOL));

        assertTrue(JGroupsConfigurator.injectConnectionFactoryIntoCustomStack(holder, sessionFactory(new AtomicBoolean()), "JGROUPS_PING", "custom"));

        assertEquals("custom-keycloak-2", holder.getGlobalConfigurationBuilder().transport().attributes().attribute(TransportConfiguration.STACK).get());
    }

    @Test
    public void testInJvmTestStackExemptionIsNarrow() {
        var inJvm = new ParserRegistry().parse("""
                <infinispan xmlns="urn:infinispan:config:16.0" xmlns:jg="urn:org:jgroups">
                  <jgroups>
                    <stack name="test" extends="tcp">
                      <jg:SHARED_LOOPBACK stack.combine="REPLACE" stack.position="TCP"/>
                    </stack>
                  </jgroups>
                  <cache-container>
                    <transport cluster="test" stack="test"/>
                  </cache-container>
                </infinispan>
                """);
        assertTrue(JGroupsConfigurator.isInJvmTestStack(inJvm, "test"));

        var networked = parse("");
        networked.getGlobalConfigurationBuilder().transport().stack("custom");
        assertFalse(JGroupsConfigurator.isInJvmTestStack(networked, "custom"));
    }

    @Test
    public void testStackWithoutKeycloakJdbcPingUntouched() {
        var holder = parse("");

        assertFalse(JGroupsConfigurator.injectConnectionFactoryIntoCustomStack(holder, sessionFactory(new AtomicBoolean()), "JGROUPS_PING", "custom"));

        var transport = holder.getGlobalConfigurationBuilder().transport();
        assertEquals("custom", transport.attributes().attribute(TransportConfiguration.STACK).get());
        assertFalse(transport.build().transport().jgroups().stacks().stream().anyMatch(s -> s.name().equals("custom-keycloak")));
    }

    @Test
    public void testCrossSiteRejected() {
        var holder = parse(PING_PROTOCOL + "<jg:relay.RELAY2 site=\"LON\"/>");

        assertThrows(IllegalStateException.class,
                () -> JGroupsConfigurator.injectConnectionFactoryIntoCustomStack(holder, sessionFactory(new AtomicBoolean()), "JGROUPS_PING", "custom"));
    }
}
