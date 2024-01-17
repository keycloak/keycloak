package org.keycloak.config;

import java.io.File;

public class CachingOptions {

    private static final String CACHE_EMBEDDED_MTLS_PREFIX = "cache-embedded-mtls";
    public static final String CACHE_EMBEDDED_MTLS_ENABLED_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-enabled";
    public static final String CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-key-store-file";
    public static final String CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-key-store-password";
    public static final String CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-trust-store-file";
    public static final String CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY = CACHE_EMBEDDED_MTLS_PREFIX + "-trust-store-password";

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

    public static final Option<File> CACHE_CONFIG_FILE = new OptionBuilder<>("cache-config-file", File.class)
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

}
