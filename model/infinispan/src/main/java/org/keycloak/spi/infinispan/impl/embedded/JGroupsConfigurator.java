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

package org.keycloak.spi.infinispan.impl.embedded;

import java.lang.invoke.MethodHandles;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;

import org.keycloak.Config;
import org.keycloak.common.util.Retry;
import org.keycloak.config.CachingOptions;
import org.keycloak.config.Option;
import org.keycloak.connections.infinispan.InfinispanConnectionSpi;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.jgroups.header.TracerHeader;
import org.keycloak.jgroups.protocol.KEYCLOAK_JDBC_PING2;
import org.keycloak.jgroups.protocol.OPEN_TELEMETRY;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.spi.infinispan.JGroupsCertificateProvider;
import org.keycloak.spi.infinispan.impl.Util;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.remoting.transport.jgroups.EmbeddedJGroupsChannelConfigurator;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.protocols.TCP;
import org.jgroups.protocols.TCP_NIO2;
import org.jgroups.protocols.UDP;
import org.jgroups.stack.Protocol;
import org.jgroups.util.DefaultSocketFactory;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.SocketFactory;
import org.jgroups.util.UUID;

import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_PREFIX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.JBOSS_NODE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.JBOSS_SITE_NAME;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.MACHINE_NAME;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.NODE_NAME;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.PROVIDER_ID;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.RACK_NAME;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.SITE_NAME;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.TRACING;

import static org.infinispan.configuration.global.TransportConfiguration.CLUSTER_NAME;
import static org.infinispan.configuration.global.TransportConfiguration.STACK;

/**
 * Utility class to configure JGroups based on the Keycloak configuration.
 */
public final class JGroupsConfigurator {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final String TLS_PROTOCOL_VERSION = "TLSv1.3";
    private static final String TLS_PROTOCOL = "TLS";
    public static final String JGROUPS_ADDRESS_SEQUENCE = "JGROUPS_ADDRESS_SEQUENCE";

    private JGroupsConfigurator() {
    }

    static {
        // Use custom Keycloak JDBC_PING implementation that workarounds issue https://issues.redhat.com/browse/JGRP-2870
        // The id 1025 follows this instruction: https://github.com/belaban/JGroups/blob/38219e9ec1c629fa2f7929e3b53d1417d8e60b61/conf/jg-protocol-ids.xml#L85
        ClassConfigurator.addProtocol((short) 1025, KEYCLOAK_JDBC_PING2.class);
        ClassConfigurator.addProtocol((short) 1026, OPEN_TELEMETRY.class);
        ClassConfigurator.add(TracerHeader.ID, TracerHeader.class);
    }

    /**
     * Checks if Infinispan is configured with or without a clustering.
     *
     * @param holder The {@link ConfigurationBuilderHolder} with the Infinispan configuration.
     * @return {@code true} if Infinispan is configured without clustering.
     */
    public static boolean isLocal(ConfigurationBuilderHolder holder) {
        return transportOf(holder).getTransport() == null;
    }

    /**
     * Checks if Infinispan is configured with or without a clustering.
     *
     * @param holder The {@link ConfigurationBuilderHolder} with the Infinispan configuration.
     * @return {@code true} if Infinispan is configured with clustering enabled.
     */
    public static boolean isClustered(ConfigurationBuilderHolder holder) {
        return transportOf(holder).getTransport() != null;
    }

    /**
     * Configures JGroups based on the Keycloak configuration.
     *
     * @param config  The Keycloak configuration.
     * @param holder  The {@link ConfigurationBuilderHolder} where the transport is configured.
     * @param session The {@link KeycloakSession} sessions for Database access.
     */
    public static void configureJGroups(Config.Scope config, ConfigurationBuilderHolder holder, KeycloakSession session) {
        if (isLocal(holder)) {
            return;
        }
        var stack = config.get(DefaultCacheEmbeddedConfigProviderFactory.STACK);
        if (stack != null) {
            transportOf(holder).stack(stack);
        }
        configureTransport(config);
        boolean tracingEnabled = config.getBoolean(TRACING, false);
        configureDiscovery(holder, session, tracingEnabled);
        configureTls(holder, session);
        warnDeprecatedStack(holder);
    }

    /**
     * Configures the topology information in the Infinispan transport.
     *
     * @param config The Keycloak configuration.
     * @param holder The {@link ConfigurationBuilderHolder} where the transport is configured.
     */
    public static void configureTopology(Config.Scope config, ConfigurationBuilderHolder holder) {
        if (System.getProperty(JBOSS_SITE_NAME) != null) {
            throw new IllegalArgumentException(
                    String.format("System property %s is in use. Use --spi-cache-embedded-%s-site-name config option instead",
                            JBOSS_SITE_NAME, PROVIDER_ID));
        }
        if (System.getProperty(JBOSS_NODE_NAME) != null) {
            throw new IllegalArgumentException(
                    String.format("System property %s is in use. Use --spi-cache-embedded-%s-node-name config option instead",
                            JBOSS_NODE_NAME, PROVIDER_ID));
        }
        var transport = transportOf(holder);
        //legacy option, for backwards compatibility --spi-connections-infinispan-quarkus-site-name
        var legacySiteName = Config.scope(InfinispanConnectionSpi.SPI_NAME, "quarkus").get("site-name");
        if (legacySiteName != null) {
            logger.warn("--spi-connections-infinispan-quarkus-site-name is deprecated and may be removed in the future. Use --spi-cache-embedded-%s-site-name".formatted(PROVIDER_ID));
        }
        var siteName = config.get(SITE_NAME, legacySiteName);
        if (siteName != null && !siteName.isEmpty()) {
            transport.siteId(siteName);
        }
        readConfigAndSet(config, RACK_NAME, transport::rackId);
        readConfigAndSet(config, MACHINE_NAME, transport::machineId);
        readConfigAndSet(config, NODE_NAME, transport::nodeName);
    }

    static void createJGroupsProperties(ProviderConfigurationBuilder builder) {
        Util.copyFromOption(builder, SystemProperties.BIND_ADDRESS.configKey, "address", ProviderConfigProperty.STRING_TYPE, CachingOptions.CACHE_EMBEDDED_NETWORK_BIND_ADDRESS, false);
        Util.copyFromOption(builder, SystemProperties.BIND_PORT.configKey, "port", ProviderConfigProperty.INTEGER_TYPE, CachingOptions.CACHE_EMBEDDED_NETWORK_BIND_PORT, false);
        Util.copyFromOption(builder, SystemProperties.EXTERNAL_ADDRESS.configKey, "address", ProviderConfigProperty.STRING_TYPE, CachingOptions.CACHE_EMBEDDED_NETWORK_EXTERNAL_ADDRESS, false);
        Util.copyFromOption(builder, SystemProperties.EXTERNAL_PORT.configKey, "port", ProviderConfigProperty.INTEGER_TYPE, CachingOptions.CACHE_EMBEDDED_NETWORK_EXTERNAL_PORT, false);
    }

    private static void configureTransport(Config.Scope config) {
        Arrays.stream(SystemProperties.values()).forEach(p -> p.set(config));
    }

    private static void configureTls(ConfigurationBuilderHolder holder, KeycloakSession session) {
        var provider = session.getProvider(JGroupsCertificateProvider.class);
        if (provider == null || !provider.isEnabled()) {
            return;
        }
        var factory = createSocketFactory(provider);
        transportOf(holder).addProperty(JGroupsTransport.SOCKET_FACTORY, factory);
        validateTlsAvailable(holder);
        logger.info("JGroups Encryption enabled (mTLS).");
    }

    private static SocketFactory createSocketFactory(JGroupsCertificateProvider provider) {
        try {
            var sslContext = SSLContext.getInstance(TLS_PROTOCOL);
            sslContext.init(new KeyManager[]{provider.keyManager()}, new TrustManager[]{provider.trustManager()}, null);
            return createFromContext(sslContext);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            // we should have valid certificates and keys.
            throw new RuntimeException(e);
        }
    }

    private static SocketFactory createFromContext(SSLContext context) {
        DefaultSocketFactory socketFactory = new DefaultSocketFactory(context);
        final SSLParameters serverParameters = new SSLParameters();
        serverParameters.setProtocols(new String[]{TLS_PROTOCOL_VERSION});
        serverParameters.setNeedClientAuth(true);
        socketFactory.setServerSocketConfigurator(socket -> ((SSLServerSocket) socket).setSSLParameters(serverParameters));
        return socketFactory;
    }

    private static void configureDiscovery(ConfigurationBuilderHolder holder, KeycloakSession session, boolean tracingEnabled) {
        var stackXmlAttribute = transportStackOf(holder);
        if (stackXmlAttribute.isModified() && !isJdbcPingStack(stackXmlAttribute.get())) {
            logger.debugf("Custom stack configured (%s). JDBC_PING discovery disabled.", stackXmlAttribute.get());
            return;
        }

        logger.debug("JDBC_PING discovery enabled.");
        if (!stackXmlAttribute.isModified()) {
            // defaults to jdbc-ping
            transportOf(holder).stack("jdbc-ping");
        }

        var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        var stackName = transportStackOf(holder).get();
        var isUdp = stackName.endsWith("udp");
        var tableName = JpaUtils.getTableNameForNativeQuery("JGROUPS_PING", em);
        var stack = getProtocolConfigurations(tableName, isUdp, tracingEnabled);
        var connectionFactory = (JpaConnectionProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(JpaConnectionProvider.class);

        String clusterName = transportOf(holder).attributes().attribute(CLUSTER_NAME).get();

        Address address = Retry.call(ignored -> KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(),
                s -> prepareJGroupsAddress(s, clusterName)),
                50, 10);
        holder.addJGroupsStack(new JpaFactoryAwareJGroupsChannelConfigurator(stackName, stack, connectionFactory, isUdp, address), null);

        transportOf(holder).stack(stackName);
        JGroupsConfigurator.logger.info("JGroups JDBC_PING discovery enabled.");
    }

    /**
     * Generate the next sequence of the address, and place it into the JGROUPS_PING table so other nodes can see it.
     * If we are the first = smallest entry, the other nodes will wait for us to become a coordinator
     * for max_join_attempts x all_clients_retry_timeout = 10 x 100 ms = 1 second. Otherwise, we will wait for that
     * one second. This prevents a split-brain scenario on a concurrent startup.
     */
    private static Address prepareJGroupsAddress(KeycloakSession session, String clusterName) {
        var cp = session.getProvider(JpaConnectionProvider.class);
        var tableName = JpaUtils.getTableNameForNativeQuery("JGROUPS_PING", cp.getEntityManager());

        long highestSequence = findHighestSequenceInTable(cp, clusterName, tableName);
        long mySequence = getNextSequence(session.getKeycloakSessionFactory(), highestSequence);
        return insertSequenceInTable(cp, clusterName, tableName, mySequence);
    }

    private static long findHighestSequenceInTable(JpaConnectionProvider cp, String clusterName, String tableName) {
        return cp.getEntityManager().<Connection, Long>callWithConnection(con -> {
            long maxSequence = -1;
            try (PreparedStatement s = con.prepareStatement("SELECT address FROM %s WHERE cluster_name=?".formatted(tableName))) {
                s.setString(1, clusterName);
                try (ResultSet resultSet = s.executeQuery()) {
                    while (resultSet.next()) {
                        String uuid = resultSet.getString(1);
                        Address addr = org.jgroups.util.Util.addressFromString(uuid);
                        if (addr instanceof UUID uuidAddr && uuidAddr.getMostSignificantBits() == 0 && uuidAddr.getLeastSignificantBits() > maxSequence) {
                            maxSequence = uuidAddr.getLeastSignificantBits();
                        }
                    }
                }
            }
            return maxSequence;
        });
    }

    private static long getNextSequence(KeycloakSessionFactory sf, long highestSequence) {
        // Run this in a separate transaction so that we already write the new ID to the database.
        // This helps us in case there is an inconsistency if some old restored database and some inconsistent nodes writing
        // their node IDs to the table. Given this is being called in a retry loop, we'll be making some progress.
        return KeycloakModelUtils.runJobInTransactionWithResult(sf, session -> {
            var storage = session.getProvider(ServerConfigStorageProvider.class);
            String seq = storage.loadOrCreate(JGROUPS_ADDRESS_SEQUENCE, () -> "0");
            long value = Math.max(highestSequence + 1, Long.parseLong(seq) + 1);
            storage.replace(JGROUPS_ADDRESS_SEQUENCE, seq, Long.toString(value));
            return value;
        });
    }

    private static Address insertSequenceInTable(JpaConnectionProvider cp, String clusterName, String tableName, long mySequence) {
        ExtendedUUID address = new ExtendedUUID(0, mySequence);
        cp.getEntityManager().<Connection>runWithConnection(con -> {
            try (PreparedStatement s = con.prepareStatement("INSERT INTO %s values (?, ?, ?, ?, ?)".formatted(tableName))) {
                s.setString(1, org.jgroups.util.Util.addressToString(new UUID(address.getMostSignificantBits(), address.getLeastSignificantBits()))); // address
                s.setString(2, "(starting)"); // name
                s.setString(3, clusterName); // cluster name
                s.setString(4, "127.0.0.1:0"); // ip = new IpAddress("localhost", 0).toString()
                s.setBoolean(5, false); // coord
                s.execute();
            }
        });
        return address;
    }

    private static List<ProtocolConfiguration> getProtocolConfigurations(String tableName, boolean udp, boolean tracingEnabled) {
        var list = new ArrayList<ProtocolConfiguration>(udp ? 1 : 2);
        list.add(new ProtocolConfiguration(KEYCLOAK_JDBC_PING2.class.getName(),
              Map.of(
                    // Leave initialize_sql blank as table is already created by Keycloak
                    "initialize_sql", "",
                    // Explicitly specify clear and select_all SQL to ensure "cluster_name" column is used, as the default
                    // "cluster" cannot be used with Oracle DB as it's a reserved word.
                    "clear_sql", String.format("DELETE from %s WHERE cluster_name=?", tableName),
                    "delete_single_sql", String.format("DELETE from %s WHERE address=?", tableName),
                    "insert_single_sql", String.format("INSERT INTO %s values (?, ?, ?, ?, ?)", tableName),
                    "select_all_pingdata_sql", String.format("SELECT address, name, ip, coord FROM %s WHERE cluster_name=?", tableName),
                    // This guarantees cleanup of stale data
                    "remove_all_data_on_view_change", "true",
                    // This guarantees that merging happens even after the info writer completed
                    "write_data_on_find", "true",
                    "register_shutdown_hook", "false",
                    "stack.combine", "REPLACE",
                    "stack.position", udp ? "PING" : "MPING"
              ))
        );

        if (!udp && InfinispanUtils.isVirtualThreadsEnabled())
            list.add(new ProtocolConfiguration(TCP.class.getSimpleName(), Map.of("bundler_type", "per-destination")));

        if (tracingEnabled) {
            list.add(new ProtocolConfiguration(OPEN_TELEMETRY.class.getName(), Map.of(
                    "stack.combine", "INSERT_ABOVE",
                    "stack.position", udp ? "UDP" : "TCP"
            )));
        }
        return list;
    }

    private static void warnDeprecatedStack(ConfigurationBuilderHolder holder) {
        var stackName = transportStackOf(holder).get();
        switch (stackName) {
            case "jdbc-ping-udp":
            case "tcp":
            case "udp":
            case "azure":
            case "ec2":
            case "google":
                logger.warnf("Stack '%s' is deprecated. We recommend to use 'jdbc-ping' instead", stackName);
        }
    }

    private static TransportConfigurationBuilder transportOf(ConfigurationBuilderHolder holder) {
        return holder.getGlobalConfigurationBuilder().transport();
    }

    private static Attribute<String> transportStackOf(ConfigurationBuilderHolder holder) {
        var transport = transportOf(holder);
        assert transport != null;
        return transport.attributes().attribute(STACK);
    }

    private static void validateTlsAvailable(ConfigurationBuilderHolder holder) {
        var stackName = transportStackOf(holder).get();
        if (stackName == null) {
            // unable to validate
            return;
        }
        var config = transportOf(holder).build();
        for (var protocol : config.transport().jgroups().configurator(stackName).getProtocolStack()) {
            var name = protocol.getProtocolName();
            if (name.equals(UDP.class.getSimpleName()) ||
                    name.equals(UDP.class.getName()) ||
                    name.equals(TCP_NIO2.class.getSimpleName()) ||
                    name.equals(TCP_NIO2.class.getName())) {
                throw new RuntimeException("Cache TLS is not available with protocol " + name);
            }
        }
    }

    private static boolean isJdbcPingStack(String stackName) {
        return "jdbc-ping".equals(stackName) || "jdbc-ping-udp".equals(stackName);
    }

    private static void readConfigAndSet(Config.Scope scope, String configKey, Consumer<String> consumer) {
        String value = scope.get(configKey);
        if (value != null && !value.isEmpty()) {
            consumer.accept(value);
        }
    }

    private static class JpaFactoryAwareJGroupsChannelConfigurator extends EmbeddedJGroupsChannelConfigurator {

        private final JpaConnectionProviderFactory factory;
        private final Address address;

        public JpaFactoryAwareJGroupsChannelConfigurator(String name, List<ProtocolConfiguration> stack, JpaConnectionProviderFactory factory, boolean isUdp, Address address) {
            super(name, stack, null, isUdp ? "udp" : "tcp");
            this.factory = Objects.requireNonNull(factory);
            this.address = address;
        }

        @Override
        protected JChannel amendChannel(JChannel channel) {
            channel.addAddressGenerator(() -> address);
            return super.amendChannel(channel);
        }

        @Override
        public void afterCreation(Protocol protocol) {
            super.afterCreation(protocol);
            if (protocol instanceof KEYCLOAK_JDBC_PING2 kcPing) {
                kcPing.setJpaConnectionProviderFactory(factory);
            }
        }
    }

    private enum SystemProperties {
        BIND_ADDRESS(CachingOptions.CACHE_EMBEDDED_NETWORK_BIND_ADDRESS, Global.BIND_ADDR, "jgroups.bind.address"),
        BIND_PORT(CachingOptions.CACHE_EMBEDDED_NETWORK_BIND_PORT, Global.BIND_PORT, "jgroups.bind.port"),
        EXTERNAL_ADDRESS(CachingOptions.CACHE_EMBEDDED_NETWORK_EXTERNAL_ADDRESS, Global.EXTERNAL_ADDR),
        EXTERNAL_PORT(CachingOptions.CACHE_EMBEDDED_NETWORK_EXTERNAL_PORT, Global.EXTERNAL_PORT);

        final Option<?> option;
        final String property;
        final String altProperty;
        final String configKey;

        SystemProperties(Option<?> option, String property) {
            this(option, property, null);
        }

        SystemProperties(Option<?> option, String property, String altProperty) {
            this.option = option;
            this.property = property;
            this.altProperty = altProperty;
            this.configKey = configKey();
        }

        void set(Config.Scope config) {
            String userConfig = fromConfig(config);
            if (userConfig == null) {
                // User property is either already set or missing, so do nothing
                return;
            }
            checkPropertyAlreadySet(userConfig, property);
            if (altProperty != null)
                checkPropertyAlreadySet(userConfig, altProperty);
            System.setProperty(property, userConfig);
        }

        void checkPropertyAlreadySet(String userValue, String property) {
            String userProp = System.getProperty(property);
            if (userProp != null) {
                logger.warnf("Conflicting system property '%s' and CLI arg '%s' set, utilising CLI value '%s'",
                      property, option.getKey(), userValue);
                System.clearProperty(property);
            }
        }

        String fromConfig(Config.Scope config) {
            if (option.getType() == Integer.class) {
                Integer val = config.getInt(configKey);
                return val == null ? null : val.toString();
            }
            return config.get(configKey);
        }

        String configKey() {
            // Strip the scope from the key and convert to camelCase
            String key = option.getKey().substring(CACHE_EMBEDDED_PREFIX.length() + 1);
            StringBuilder sb = new StringBuilder(key);
            for (int i = 0; i < sb.length(); i++) {
                if (sb.charAt(i) == '-') {
                    sb.deleteCharAt(i);
                    sb.replace(i, i+1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
                }
            }
            return sb.toString();
        }
    }
}
