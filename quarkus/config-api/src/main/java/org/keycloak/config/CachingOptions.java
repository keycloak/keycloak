package org.keycloak.config;

import java.io.File;

public class CachingOptions {

    public static final String CACHE_CONFIG_FILE_PROPERTY = "cache-config-file";

    private static final String CACHE_EMBEDDED_MTLS_PREFIX = "cache-embedded-mtls";
    public static final String CACHE_EMBEDDED_MTLS_ENABLED_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-enabled";
    public static final String CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-key-store-file";
    public static final String CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-key-store-password";
    public static final String CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-trust-store-file";
    public static final String CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-trust-store-password";

    private static final String CACHE_REMOTE_PREFIX = "cache-remote";
    public static final String CACHE_REMOTE_HOST_PROPERTY = CACHE_REMOTE_PREFIX + "-host";
    public static final String CACHE_REMOTE_PORT_PROPERTY = CACHE_REMOTE_PREFIX + "-port";
    public static final String CACHE_REMOTE_USERNAME_PROPERTY = CACHE_REMOTE_PREFIX + "-username";
    public static final String CACHE_REMOTE_PASSWORD_PROPERTY = CACHE_REMOTE_PREFIX + "-password";

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
            .buildTime(true)
            .build();

    public enum Stack {
        tcp,
        udp,
        kubernetes,
        ec2,
        azure,
        google;
    }

    public static final Option<Stack> CACHE_STACK = new OptionBuilder<>("cache-stack", Stack.class)
            .category(OptionCategory.CACHE)
            .description("Define the default stack to use for cluster communication and node discovery. This option only takes effect "
                    + "if 'cache' is set to 'ispn'. Default: udp.")
            .buildTime(true)
            .build();

    public static final Option<File> CACHE_CONFIG_FILE = new OptionBuilder<>(CACHE_CONFIG_FILE_PROPERTY, File.class)
            .category(OptionCategory.CACHE)
            .description("Defines the file from which cache configuration should be loaded from. "
                    + "The configuration file is relative to the 'conf/' directory.")
            .buildTime(true)
            .build();

    public static final Option<Boolean> CACHE_EMBEDDED_MTLS_ENABLED = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_ENABLED_PROPERTY, Boolean.class)
            .category(OptionCategory.CACHE)
            .description("Encrypts the network communication between Keycloak servers.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> CACHE_EMBEDDED_MTLS_KEYSTORE = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The Keystore file path. The Keystore must contain the certificate to use by the TLS protocol. " +
                    "By default, it lookup 'cache-mtls-keystore.p12' under conf/ directory.")
            .buildTime(true)
            .build();

    public static final Option<String> CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The password to access the Keystore.")
            .buildTime(true)
            .build();

    public static final Option<String> CACHE_EMBEDDED_MTLS_TRUSTSTORE = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The Truststore file path. " +
                    "It should contain the trusted certificates or the Certificate Authority that signed the certificates. " +
                    "By default, it lookup 'cache-mtls-truststore.p12' under conf/ directory.")
            .buildTime(true)
            .build();

    public static final Option<String> CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD = new OptionBuilder<>(CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description("The password to access the Truststore.")
            .buildTime(true)
            .build();

    public static final Option<String> CACHE_REMOTE_HOST = new OptionBuilder<>(CACHE_REMOTE_HOST_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description(String.format("The hostname of the remote server for the remote store configuration. "
                    + "It replaces the 'host' attribute of 'remote-server' tag of the configuration specified via XML file (see '%s' option.). "
                    + "If the option is specified, '%s' and '%s' are required as well and the related configuration in XML file should not be present.", 
                    CACHE_CONFIG_FILE_PROPERTY, CACHE_REMOTE_USERNAME_PROPERTY, CACHE_REMOTE_PASSWORD_PROPERTY))
            .buildTime(true)
            .build();

    public static final Option<Integer> CACHE_REMOTE_PORT = new OptionBuilder<>(CACHE_REMOTE_PORT_PROPERTY, Integer.class)
            .category(OptionCategory.CACHE)
            .description(String.format("The port of the remote server for the remote store configuration. "
                    + "It replaces the 'port' attribute of 'remote-server' tag of the configuration specified via XML file (see '%s' option.). "
                    + "If the option is specified, '%s', '%s' and '%s' are required as well and the related configuration in XML file should not be present.", 
                    CACHE_CONFIG_FILE_PROPERTY, CACHE_REMOTE_HOST_PROPERTY, CACHE_REMOTE_USERNAME_PROPERTY, CACHE_REMOTE_PASSWORD_PROPERTY))
            .defaultValue(11222)
            .buildTime(true)
            .build();

    public static final Option<String> CACHE_REMOTE_USERNAME = new OptionBuilder<>(CACHE_REMOTE_USERNAME_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description(String.format("The username for the authentication to the remote server for the remote store. "
                    + "It replaces the 'username' attribute of 'digest' tag of the configuration specified via XML file (see '%s' option.). "
                    + "If the option is specified, '%s' and '%s' are required as well and the related configuration in XML file should not be present.", 
                    CACHE_CONFIG_FILE_PROPERTY, CACHE_REMOTE_HOST_PROPERTY, CACHE_REMOTE_PASSWORD_PROPERTY))
            .buildTime(true)
            .build();

    public static final Option<String> CACHE_REMOTE_PASSWORD = new OptionBuilder<>(CACHE_REMOTE_PASSWORD_PROPERTY, String.class)
            .category(OptionCategory.CACHE)
            .description(String.format("The password for the authentication to the remote server for the remote store. "
                    + "It replaces the 'password' attribute of 'digest' tag of the configuration specified via XML file (see '%s' option.). "
                    + "If the option is specified, '%s' and '%s' are required as well and the related configuration in XML file should not be present.", 
                    CACHE_CONFIG_FILE_PROPERTY, CACHE_REMOTE_HOST_PROPERTY, CACHE_REMOTE_USERNAME_PROPERTY))
            .buildTime(true)
            .build();

}
