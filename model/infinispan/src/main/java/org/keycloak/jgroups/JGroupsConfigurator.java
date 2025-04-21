/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionSpi;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.jgroups.impl.JGroupsJdbcPingStackConfigurator;
import org.keycloak.jgroups.impl.JpaJGroupsTlsConfigurator;
import org.keycloak.models.KeycloakSession;

/**
 * Configures the JGroups stacks before starting Infinispan.
 */
public class JGroupsConfigurator {

    public static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final ConfigurationBuilderHolder holder;
    private final List<JGroupsStackConfigurator> stackConfiguratorList;

    private JGroupsConfigurator(ConfigurationBuilderHolder holder, List<JGroupsStackConfigurator> stackConfiguratorList) {
        this.holder = holder;
        this.stackConfiguratorList = stackConfiguratorList;
    }

    private static void createJdbcPingConfigurator(ConfigurationBuilderHolder holder, List<JGroupsStackConfigurator> configurator) {
        var stackXmlAttribute = JGroupsUtil.transportStackOf(holder);
        if (stackXmlAttribute.isModified() && !isJdbcPingStack(stackXmlAttribute.get())) {
            logger.debugf("Custom stack configured (%s). JDBC_PING discovery disabled.", stackXmlAttribute.get());
            return;
        }
        logger.debug("JDBC_PING discovery enabled.");
        if (!stackXmlAttribute.isModified()) {
            // defaults to jdbc-ping
            JGroupsUtil.transportOf(holder).stack("jdbc-ping");
        }
        configurator.add(JGroupsJdbcPingStackConfigurator.INSTANCE);
    }

    private static boolean isJdbcPingStack(String stackName) {
        return "jdbc-ping".equals(stackName) || "jdbc-ping-udp".equals(stackName);
    }

    private static void createTlsConfigurator(List<JGroupsStackConfigurator> configurator) {
        configurator.add(JpaJGroupsTlsConfigurator.INSTANCE);
    }

    private static boolean isLocal(ConfigurationBuilderHolder holder) {
        return JGroupsUtil.transportOf(holder).getTransport() == null;
    }

    public static JGroupsConfigurator create(ConfigurationBuilderHolder holder) {
        if (InfinispanUtils.isRemoteInfinispan() || isLocal(holder)) {
            logger.debug("Multi Site or local mode. Skipping JGroups configuration.");
            return new JGroupsConfigurator(holder, List.of());
        }
        // Configure stack from CLI options to Global Configuration
        var stack = Config.scope(InfinispanConnectionSpi.SPI_NAME, "quarkus").get("stack");
        if (stack != null) {
            JGroupsUtil.transportOf(holder).stack(stack);
        }
        var configurator = new ArrayList<JGroupsStackConfigurator>(2);
        createJdbcPingConfigurator(holder, configurator);
        createTlsConfigurator(configurator);
        return new JGroupsConfigurator(holder, List.copyOf(configurator));
    }

    /**
     * @return The {@link ConfigurationBuilderHolder} with the current Infinispan configuration.
     */
    public ConfigurationBuilderHolder holder() {
        return holder;
    }

    /**
     * @return {@code true} if Keycloak is run in local mode (development mode for example) and JGroups won't be used.
     */
    public boolean isLocal() {
        return isLocal(holder);
    }

    /**
     * Configures the JGroups stack.
     *
     * @param session The {@link KeycloakSession}.
     */
    public void configure(KeycloakSession session) {
        if (InfinispanUtils.isRemoteInfinispan() || isLocal()) {
            return;
        }
        stackConfiguratorList.forEach(jGroupsStackConfigurator -> jGroupsStackConfigurator.configure(holder, session));
        JGroupsUtil.warnDeprecatedStack(holder);
    }

}
