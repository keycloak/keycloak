package org.keycloak.config;

public class ConfigKeystoreOptions {

    public static final Option<String> CONFIG_KEYSTORE = new OptionBuilder<>("config-keystore", String.class)
            .category(OptionCategory.CONFIG)
            .description("Specifies a path to the KeyStore Configuration Source.")
            .build();

    public static final Option<String> CONFIG_KEYSTORE_PASSWORD = new OptionBuilder<>("config-keystore-password", String.class)
            .category(OptionCategory.CONFIG)
            .description("Specifies a password to the KeyStore Configuration Source.")
            .build();

    public static final Option<String> CONFIG_KEYSTORE_TYPE = new OptionBuilder<>("config-keystore-type", String.class)
            .category(OptionCategory.CONFIG)
            .description("Specifies a type of the KeyStore Configuration Source.")
            .defaultValue("PKCS12")
            .build();

}
