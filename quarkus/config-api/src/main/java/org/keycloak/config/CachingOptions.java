package org.keycloak.config;

import java.io.File;
import java.util.List;

import com.google.common.base.CaseFormat;

public class CachingOptions {

    public static final String CACHE_CONFIG_FILE_PROPERTY = "cache-config-file";
    public static final String CACHE_CONFIG_MUTATE_PROPERTY = "cache-config-mutate";

    public static final String CACHE_EMBEDDED_PREFIX = "cache-embedded";
    private static final String CACHE_EMBEDDED_MTLS_PREFIX = CACHE_EMBEDDED_PREFIX + "-mtls";
    public static final String CACHE_EMBEDDED_MTLS_ENABLED_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-enabled";
    public static final String CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-key-store-file";
    public static final String CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-key-store-password";
    public static final String CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-trust-store-file";
    public static final String CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-trust-store-password";
    public static final String CACHE_EMBEDDED_MTLS_ROTATION_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-rotation-interval-days";
    public static final String CACHE_EMBEDDED_NETWORK_BIND_ADDRESS_PROPERTY = CACHE_EMBEDDED_PREFIX + "-network-bind-address";
    public static final String CACHE_EMBEDDED_NETWORK_BIND_PORT_PROPERTY = CACHE_EMBEDDED_PREFIX + "-network-bind-port";
    public static final String CACHE_EMBEDDED_NETWORK_EXTERNAL_ADDRESS_PROPERTY = CACHE_EMBEDDED_PREFIX + "-network-external-address";
    public static final String CACHE_EMBEDDED_NETWORK_EXTERNAL_PORT_PROPERTY = CACHE_EMBEDDED_PREFIX + "-network-external-port";

    private static final String CACHE_REMOTE_PREFIX = "cache-remote";
    public static final String CACHE_REMOTE_HOST_PROPERTY = CACHE_REMOTE_PREFIX + "-host";
    public static final String CACHE_REMOTE_PORT_PROPERTY = CACHE_REMOTE_PREFIX + "-port";
    public static final String CACHE_REMOTE_USERNAME_PROPERTY = CACHE_REMOTE_PREFIX + "-username";
    public static final String CACHE_REMOTE_PASSWORD_PROPERTY = CACHE_REMOTE_PREFIX + "-password";
    public static final String CACHE_REMOTE_TLS_ENABLED_PROPERTY = CACHE_REMOTE_PREFIX + "-tls-enabled";
    public static final String CACHE_REMOTE_BACKUP_SITES_PROPERTY = CACHE_REMOTE_PREFIX + "-backup-sites";

    private static final String CACHE_METRICS_PREFIX = "cache-metrics";
    public static final String CACHE_METRICS_HISTOGRAMS_ENABLED_PROPERTY = CACHE_METRICS_PREFIX + "-histograms-enabled";

    public static final String[] LOCAL_MAX_COUNT_CACHES = new String[]{"authorization", "crl", "keys", "realms", "users", };

    public static final String[] CLUSTERED_MAX_COUNT_CACHES = new String[]{"clientSessions", "offlineSessions", "offlineClientSessions", "sessions"};

    public enum Mechanism {
        ispn,
        local
    }

    public static final Option<Mechanism> CACHE = new OptionBuilder<>("cache", Mechanism.class)
            .category(OptionCategory.CACHE)
            .description("Defines the cache mechanism for high-availability. "
                    + "By default in production mode, a 'ispn' cache is used to create a cluster between multiple server nodes. "
                    + "By default in development mode, a 'local' cache disables clustering and is intended for development and testing purposes.")
            .defaultValue(Mechanism.ispn)
            .build();

    public enum Stack {
        jdbc_ping,
        kubernetes,
        jdbc_ping_udp,
        tcp,
        udp,
        ec2,
        azure,
        google;

        @Override
        public String toString() {
            return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, super.toString());
        }
    }

    public static final Option<Stack> CACHE_STACK = new OptionBuilder<>("cache-stack", Stack.class)
            .category(OptionCategory.CACHE)
            .strictExpectedValues(false)
            .description("Define the default stack to use for cluster communication and node discovery. Defaults to 'jdbc-ping' if not set.")
            // Do not set a default value here as it would otherwise overwrite an explicit stack chosen in cache config XML
            .deprecatedValues("Use 'jdbc-ping' instead by leaving it unset", Stack.azure, Stack.ec2, Stack.google, Stack.jdbc_ping_udp, Stack.kubernetes, Stack.tcp, Stack.udp)
            .build();

    public static final Option<File> CACHE_CONFIG_FILE = new OptionBuilder<>(CACHE_CONFIG_FILE_PROPERTY, File.class)
            .category(OptionCategory.CACHE)
            .description("Defines the file from which cache configuration should be loaded from. "
                    + "The configuration file is relative to the 'conf/' directory.")
            .build();

    public static final Option<Boolean> CACHE_CONFIG_MUTATE = new OptionBuilder<>(CACHE_CONFIG_MUTATE_PROPERTY, Boolean.class)
            .category(OptionCategory.CACHE)
            .description("Determines whether changes to the default cache configurations are allowed. This is only recommended for advanced use-cases where the default cache configurations are proven to be problematic. The only supported way to change the default cache configurations is via the other 'cache-...' options.")
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<Boolean> CACHE_EMBEDDED_MTLS_ENABLED = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_ENABLED_PROPERTY, Boolean.class)
            .category(OptionCategory.CACHE)
            .description("Encrypts the network communication between Keycloak servers. If no additional parameters about a keystore and truststore are provided, ephemeral key pairs and certificates are created and rotated automatically, which is recommended for standard setups.")
            .defaultValue(Boolean.TRUE)
            .build();

    public static final Option<String> CACHE_EMBEDDED_MTLS_KEYSTORE = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The Keystore file path. The Keystore must contain the certificate to use by the TLS protocol. " +
                    "By default, it looks up 'cache-mtls-keystore.p12' under conf/ directory.")
            .build();

    public static final Option<String> CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The password to access the Keystore.")
            .build();

    public static final Option<String> CACHE_EMBEDDED_MTLS_TRUSTSTORE = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The Truststore file path. " +
                    "It should contain the trusted certificates or the Certificate Authority that signed the certificates. " +
                    "By default, it lookup 'cache-mtls-truststore.p12' under conf/ directory.")
            .build();

    public static final Option<String> CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The password to access the Truststore.")
            .build();

    public static final Option<Integer> CACHE_EMBEDDED_MTLS_ROTATION = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_ROTATION_PROPERTY, Integer.class)
            .category(OptionCategory.CACHE)
            .defaultValue(30)
            .description("Rotation period in days of automatic JGroups MTLS certificates.")
            .build();

    public static final Option<String> CACHE_EMBEDDED_NETWORK_BIND_ADDRESS = new OptionBuilder<>(CACHE_EMBEDDED_NETWORK_BIND_ADDRESS_PROPERTY, String.class)
          .category(OptionCategory.CACHE)
          .description("IP address used by clustering transport. By default, SITE_LOCAL is used.")
          .build();

    public static final Option<Integer> CACHE_EMBEDDED_NETWORK_BIND_PORT = new OptionBuilder<>(CACHE_EMBEDDED_NETWORK_BIND_PORT_PROPERTY, Integer.class)
          .category(OptionCategory.CACHE)
          .description("The Port the clustering transport will bind to. By default, port 7800 is used.")
          .build();

    public static final Option<String> CACHE_EMBEDDED_NETWORK_EXTERNAL_ADDRESS = new OptionBuilder<>(CACHE_EMBEDDED_NETWORK_EXTERNAL_ADDRESS_PROPERTY, String.class)
          .category(OptionCategory.CACHE)
          .description("IP address that other instances in the cluster should use to contact this node. Set only if it is " +
                "different to %s, for example when this instance is behind a firewall.".formatted(CACHE_EMBEDDED_NETWORK_BIND_ADDRESS_PROPERTY))
          .build();

    public static final Option<Integer> CACHE_EMBEDDED_NETWORK_EXTERNAL_PORT = new OptionBuilder<>(CACHE_EMBEDDED_NETWORK_EXTERNAL_PORT_PROPERTY, Integer.class)
          .category(OptionCategory.CACHE)
          .description("Port that other instances in the cluster should use to contact this node. Set only if it is different " +
                "to %s, for example when this instance is behind a firewall".formatted(CACHE_EMBEDDED_NETWORK_BIND_PORT_PROPERTY))
          .build();

    public static final Option<String> CACHE_REMOTE_HOST = new OptionBuilder<>(CACHE_REMOTE_HOST_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The hostname of the external Infinispan cluster.")
            .build();

    public static final Option<Integer> CACHE_REMOTE_PORT = new OptionBuilder<>(CACHE_REMOTE_PORT_PROPERTY, Integer.class)
            .category(OptionCategory.CACHE)
            .description("The port of the external Infinispan cluster.")
            .defaultValue(11222)
            .build();

    public static final Option<String> CACHE_REMOTE_USERNAME = new OptionBuilder<>(CACHE_REMOTE_USERNAME_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description(String.format("The username for the authentication to the external Infinispan cluster. "
                            + "It is optional if connecting to an unsecure external Infinispan cluster. "
                            + "If the option is specified, '%s' is required as well.",
                    CACHE_REMOTE_PASSWORD_PROPERTY))
            .build();

    public static final Option<String> CACHE_REMOTE_PASSWORD = new OptionBuilder<>(CACHE_REMOTE_PASSWORD_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description(String.format("The password for the authentication to the external Infinispan cluster. "
                            + "It is optional if connecting to an unsecure external Infinispan cluster. "
                            + "If the option is specified, '%s' is required as well.",
                    CACHE_REMOTE_USERNAME_PROPERTY))
            .build();

    public static final Option<Boolean> CACHE_METRICS_HISTOGRAMS_ENABLED = new OptionBuilder<>(CACHE_METRICS_HISTOGRAMS_ENABLED_PROPERTY, Boolean.class)
            .category(OptionCategory.CACHE)
            .description("Enable histograms for metrics for the embedded caches.")
            .build();

    public static final Option<Boolean> CACHE_REMOTE_TLS_ENABLED = new OptionBuilder<>(CACHE_REMOTE_TLS_ENABLED_PROPERTY, Boolean.class)
            .category(OptionCategory.CACHE)
            .description("Enable TLS support to communicate with a secured remote Infinispan server. Recommended to be enabled in production.")
            .defaultValue(Boolean.TRUE)
            .build();

    public static final Option<List<String>> CACHE_REMOTE_BACKUP_SITES = OptionBuilder.listOptionBuilder(CACHE_REMOTE_BACKUP_SITES_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("Configures a list of backup sites names to where the external Infinispan cluster backups the Keycloak data.")
            .build();

    public static Option<Integer> maxCountOption(String cache) {
        return new OptionBuilder<>(cacheMaxCountProperty(cache), Integer.class)
              .category(OptionCategory.CACHE)
              .description(String.format("The maximum number of entries that can be stored in-memory by the %s cache.", cache))
              .build();
    }

    public static String cacheMaxCountProperty(String cacheName) {
        cacheName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, cacheName);
        return String.format("%s-%s-max-count", CACHE_EMBEDDED_PREFIX, cacheName);
    }
}
