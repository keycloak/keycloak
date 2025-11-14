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

package org.keycloak.spi.infinispan.impl.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.net.ssl.SSLContext;

import org.keycloak.Config;
import org.keycloak.config.CachingOptions;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderSpi;
import org.keycloak.spi.infinispan.CacheRemoteConfigProvider;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderFactory;
import org.keycloak.spi.infinispan.impl.embedded.CacheConfigurator;

import org.infinispan.client.hotrod.configuration.AuthenticationConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ExhaustedAction;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.skipSessionsCacheIfRequired;
import static org.keycloak.spi.infinispan.impl.Util.copyFromOption;

import static org.wildfly.security.sasl.util.SaslMechanismInformation.Names.SCRAM_SHA_512;

/**
 * The default implementation for {@link CacheRemoteConfigProviderFactory} and {@link CacheRemoteConfigProvider}.
 * <p>
 * It is used when an external Infinispan cluster is enabled.
 */
public class DefaultCacheRemoteConfigProviderFactory implements CacheRemoteConfigProviderFactory, CacheRemoteConfigProvider, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "default";
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    // configuration
    private static final String PROPERTIES_FILE = "propertiesFile";
    private static final String CLIENT_INTELLIGENCE = "clientIntelligence";
    public static final String HOSTNAME = "hostname";
    public static final String PORT = "port";
    private static final String TLS_ENABLED = "tlsEnabled";
    private static final String TLS_SNI_HOSTNAME = "tlsSniHostname";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CONNECTION_POOL_MAX_ACTIVE = "connectionPoolMaxActive";
    private static final String CONNECTION_POOL_EXHAUSTED_ACTION = "connectionPoolExhaustedAction";
    private static final String AUTH_REALM = "authRealm";
    private static final String SASL_MECHANISM = "saslMechanism";
    private static final String BACKUP_SITES = "backupSites";

    // configuration defaults
    private static final String CLIENT_INTELLIGENCE_DEFAULT = ClientIntelligence.getDefault().name();
    private static final int CONNECTION_POOL_MAX_ACTIVE_DEFAULT = 16;
    private static final String CONNECTION_POOL_EXHAUSTED_ACTION_DEFAULT = ExhaustedAction.CREATE_NEW.name();
    private static final String SASL_MECHANISM_DEFAULT = SCRAM_SHA_512;

    private volatile Configuration remoteConfiguration;
    private volatile Config.Scope keycloakConfiguration;

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isRemoteInfinispan();
    }

    @Override
    public CacheRemoteConfigProvider create(KeycloakSession session) {
        lazyInit();
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        this.keycloakConfiguration = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        lazyInit();
    }

    @Override
    public Optional<Configuration> configuration() {
        assert remoteConfiguration != null;
        return Optional.of(remoteConfiguration);
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        var builder = ProviderConfigurationBuilder.create();
        addHostNameAndPortConfig(builder);
        addClientIntelligenceConfig(builder);
        addPropertiesFileConfig(builder);
        addConnectionPoolConfig(builder);
        addTlsConfig(builder);
        addAuthenticationConfig(builder);
        addCreateRemoteCachesConfig(builder);
        return builder.build();
    }

    /**
     * Creates the {@link ConfigurationBuilder}.
     * <p>
     * This class is protected if power users need to extend this class for more advanced configuration. Using a
     * properties file is the recommended way to configure the client in more detail. Check
     * {@link ConfigurationProperties} for property keys.
     *
     * @return The {@link ConfigurationBuilder}. This instance can be modified.
     * @throws IOException if an error occurred when reading from the properties file (if configured).
     * @see ConfigurationProperties
     */
    protected ConfigurationBuilder createConfigurationBuilder() throws IOException {
        logger.info("Starting Infinispan remote cache manager (Hot Rod Client)");

        var builder = new ConfigurationBuilder();
        loadProperties(builder);
        builder.clientIntelligence(ClientIntelligence.valueOf(keycloakConfiguration.get(CLIENT_INTELLIGENCE, CLIENT_INTELLIGENCE_DEFAULT)));
        configureHostname(builder);
        configureConnectionPool(builder);
        configureTls(builder);
        configureAuthentication(builder);
        Marshalling.configure(builder);
        configureRemoteCaches(builder);

        return builder;
    }

    private void lazyInit() {
        if (remoteConfiguration != null) {
            return;
        }
        synchronized (this) {
            if (remoteConfiguration != null) {
                return;
            }
            try {
                remoteConfiguration = createConfigurationBuilder().build();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void loadProperties(ConfigurationBuilder builder) throws IOException {
        var path = keycloakConfiguration.get(PROPERTIES_FILE);
        if (path == null) {
            logger.debug("Hot Rod properties file not configured.");
            return;
        }
        var file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("Hot Rod properties file not found: " + path);
        }
        try (var is = new FileInputStream(file)) {
            var properties = new Properties();
            properties.load(is);
            builder.withProperties(properties);
        }
    }

    private void configureHostname(ConfigurationBuilder builder) {
        var host = keycloakConfiguration.get(HOSTNAME);
        if (host == null) {
            logger.debug("Hot Rod hostname not configured.");
            return;
        }
        var port = keycloakConfiguration.getInt(PORT, ConfigurationProperties.DEFAULT_HOTROD_PORT);
        logger.debugf("Hot Rod connecting to %s:%s", host, port);

        builder.addServer()
                .host(host)
                .port(port);
    }

    private void configureConnectionPool(ConfigurationBuilder builder) {
        builder.connectionPool()
                .maxActive(keycloakConfiguration.getInt(CONNECTION_POOL_MAX_ACTIVE, CONNECTION_POOL_MAX_ACTIVE_DEFAULT))
                .exhaustedAction(ExhaustedAction.valueOf(keycloakConfiguration.get(CONNECTION_POOL_EXHAUSTED_ACTION, CONNECTION_POOL_EXHAUSTED_ACTION_DEFAULT)));
    }

    private void configureTls(ConfigurationBuilder builder) {
        if (!keycloakConfiguration.getBoolean(TLS_ENABLED, Boolean.FALSE)) {
            logger.debug("Hot Rod TLS not enabled.");
            return;
        }
        var sniHostName = keycloakConfiguration.get(TLS_SNI_HOSTNAME);
        if (sniHostName == null) {
            sniHostName = keycloakConfiguration.get(HOSTNAME);
        }
        builder.security().ssl()
                .enable()
                .sslContext(createSSLContext())
                .sniHostName(sniHostName);
    }

    private void configureAuthentication(ConfigurationBuilder builder) {
        var username = keycloakConfiguration.get(USERNAME);
        var password = keycloakConfiguration.get(PASSWORD);
        if (username == null && password == null) {
            logger.debug("Hot Rod authentication not enabled.");
            return;
        }
        builder.security().authentication()
                .enable()
                .username(username)
                .password(password)
                .realm(keycloakConfiguration.get(AUTH_REALM, AuthenticationConfigurationBuilder.DEFAULT_REALM))
                .saslMechanism(keycloakConfiguration.get(SASL_MECHANISM, SASL_MECHANISM_DEFAULT));
    }

    private static SSLContext createSSLContext() {
        try {
            // uses the default Java Runtime TrustStore, or the one generated by Keycloak (see org.keycloak.truststore.TruststoreBuilder)
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureRemoteCaches(ConfigurationBuilder builder) {
        var sites = keycloakConfiguration.getArray(BACKUP_SITES);

        // hijack the embedded cache configuration :)
        var embeddedKeycloakConfig = Config.scope(CacheEmbeddedConfigProviderSpi.SPI_NAME, DefaultCacheRemoteConfigProviderFactory.PROVIDER_ID);
        skipSessionsCacheIfRequired(Arrays.stream(CLUSTERED_CACHE_NAMES))
                .forEach(name -> {
                    var cacheConfig = CacheConfigurator.getRemoteCacheConfiguration(name, embeddedKeycloakConfig, sites);
                    if (cacheConfig == null) {
                        return;
                    }
                    builder.remoteCache(name).configuration(cacheConfig.build().toStringConfiguration(name));
                });
    }

    // configuration option below

    private static void addHostNameAndPortConfig(ProviderConfigurationBuilder builder) {
        copyFromOption(builder, HOSTNAME, "hostname", ProviderConfigProperty.STRING_TYPE, CachingOptions.CACHE_REMOTE_HOST, false);
        copyFromOption(builder, PORT, "port", ProviderConfigProperty.INTEGER_TYPE, CachingOptions.CACHE_REMOTE_PORT, false);
    }

    private static void addClientIntelligenceConfig(ProviderConfigurationBuilder builder) {
        builder.property()
                .name(CLIENT_INTELLIGENCE)
                .helpText("Specifies the level of intelligence the Hot Rod client should have.")
                .label("intelligence")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(CLIENT_INTELLIGENCE_DEFAULT)
                .options(Arrays.stream(ClientIntelligence.values()).map(Enum::name).toList())
                .add();
    }

    private static void addPropertiesFileConfig(ProviderConfigurationBuilder builder) {
        builder.property()
                .name(PROPERTIES_FILE)
                .helpText("Path to the properties file with the Hot Rod client configuration.")
                .label("file")
                .type(ProviderConfigProperty.FILE_TYPE)
                .add();
    }

    private static void addConnectionPoolConfig(ProviderConfigurationBuilder builder) {
        builder.property()
                .name(CONNECTION_POOL_MAX_ACTIVE)
                .helpText("Sets the maximum number of connections per Infinispan server instance.")
                .label("maxActive")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue(CONNECTION_POOL_MAX_ACTIVE_DEFAULT)
                .add();
        builder.property()
                .name(CONNECTION_POOL_EXHAUSTED_ACTION)
                .helpText("Specifies what happens when asking for a connection from a server's pool, and that pool is exhausted.")
                .label("action")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(CONNECTION_POOL_EXHAUSTED_ACTION_DEFAULT)
                .options(Arrays.stream(ExhaustedAction.values()).map(Enum::name).toList())
                .add();
    }

    private static void addAuthenticationConfig(ProviderConfigurationBuilder builder) {
        copyFromOption(builder, USERNAME, "username", ProviderConfigProperty.STRING_TYPE, CachingOptions.CACHE_REMOTE_USERNAME, false);
        copyFromOption(builder, PASSWORD, "password", ProviderConfigProperty.STRING_TYPE, CachingOptions.CACHE_REMOTE_PASSWORD, true);
        builder.property()
                .name(AUTH_REALM)
                .helpText("Specifies the Infinispan server realm to be used for authentication.")
                .label("realm")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(AuthenticationConfigurationBuilder.DEFAULT_REALM)
                .add();
        builder.property()
                .name(SASL_MECHANISM)
                .helpText("Selects the SASL mechanism to use for the connection to the Infinispan server.")
                .label("mechanism")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(SASL_MECHANISM_DEFAULT)
                .add();
    }

    private static void addTlsConfig(ProviderConfigurationBuilder builder) {
        copyFromOption(builder, TLS_ENABLED, "enabled", ProviderConfigProperty.BOOLEAN_TYPE, CachingOptions.CACHE_REMOTE_TLS_ENABLED, false);
        builder.property()
                .name(TLS_SNI_HOSTNAME)
                .helpText("Specifies the TLS SNI hostname for the connection to the Infinispan server.")
                .label("hostname")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add();
    }

    private static void addCreateRemoteCachesConfig(ProviderConfigurationBuilder builder) {
        copyFromOption(builder, BACKUP_SITES, "sites", ProviderConfigProperty.LIST_TYPE, CachingOptions.CACHE_REMOTE_BACKUP_SITES, false);
    }
}
