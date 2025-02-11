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

package org.keycloak.quarkus.runtime.storage.infinispan.jgroups;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.keycloak.config.CachingOptions;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl.FileJGroupsTlsConfigurator;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl.JGroupsJdbcPingStackConfigurator;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl.JpaJGroupsTlsConfigurator;

/**
 * Configures the JGroups stacks before starting Infinispan.
 */
public class JGroupsConfigurator {

    private final ConfigurationBuilderHolder holder;
    private final List<JGroupsStackConfigurator> stackConfiguratorList;

    private JGroupsConfigurator(ConfigurationBuilderHolder holder, List<JGroupsStackConfigurator> stackConfiguratorList) {
        this.holder = holder;
        this.stackConfiguratorList = stackConfiguratorList;
    }

    private static void createJdbcPingConfigurator(ConfigurationBuilderHolder holder, List<JGroupsStackConfigurator> configurator) {
        var stackXmlAttribute = JGroupsUtil.transportStackOf(holder);
        if (stackXmlAttribute.isModified() && !isJdbcPingStack(stackXmlAttribute.get())) {
            CacheManagerFactory.logger.debugf("Custom stack configured (%s). JDBC_PING discovery disabled.", stackXmlAttribute.get());
            return;
        }
        CacheManagerFactory.logger.debug("JDBC_PING discovery enabled.");
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
        if (!Configuration.isTrue(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED)) {
            CacheManagerFactory.logger.debug("JGroups encryption disabled.");
            return;
        }

        if (Configuration.isBlank(CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE) && Configuration.isBlank(CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE)) {
            CacheManagerFactory.logger.debug("JGroups encryption enabled. Neither KeyStore and Truststore present, using the certificates from database.");
            configurator.add(JpaJGroupsTlsConfigurator.INSTANCE);
            return;
        }
        CacheManagerFactory.logger.debug("JGroups encryption enabled. KeyStore or Truststore present.");
        configurator.add(FileJGroupsTlsConfigurator.INSTANCE);
    }

    private static boolean isLocal(ConfigurationBuilderHolder holder) {
        return JGroupsUtil.transportOf(holder) == null;
    }

    public static JGroupsConfigurator create(ConfigurationBuilderHolder holder) {
        if (InfinispanUtils.isRemoteInfinispan() || isLocal(holder)) {
            CacheManagerFactory.logger.debug("Multi Site or local mode. Skipping JGroups configuration.");
            return new JGroupsConfigurator(holder, List.of());
        }
        // Configure stack from CLI options to Global Configuration
        var transportStack = Configuration.getRawValue("kc.cache-stack");
        if (transportStack != null) {
            JGroupsUtil.transportOf(holder).stack(transportStack);
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
     * @return {@code true} if it requires a {@link KeycloakSession} to perform the stack configuration.
     */
    public boolean requiresKeycloakSession() {
        return stackConfiguratorList.stream().anyMatch(JGroupsStackConfigurator::requiresKeycloakSession);
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
     * @param session The {@link KeycloakSession}. It is {@code null} when {@link #requiresKeycloakSession()} returns
     *                {@code false}.
     */
    public void configure(KeycloakSession session) {
        if (InfinispanUtils.isRemoteInfinispan() || isLocal()) {
            return;
        }
        stackConfiguratorList.forEach(jGroupsStackConfigurator -> jGroupsStackConfigurator.configure(holder, session));
        JGroupsUtil.warnDeprecatedStack(holder);
    }

}
