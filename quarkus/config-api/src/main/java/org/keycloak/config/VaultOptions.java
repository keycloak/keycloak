package org.keycloak.config;

import java.nio.file.Path;

public class VaultOptions {

    public enum VaultType {

        file("file"),
        keystore("keystore");

        private final String provider;

        VaultType(String provider) {
            this.provider = provider;
        }

        public String getProvider() {
            return provider;
        }
    }

    public static final Option<VaultOptions.VaultType> VAULT = new OptionBuilder<>("vault", VaultType.class)
            .category(OptionCategory.VAULT)
            .description("Enables a vault provider.")
            .buildTime(true)
            .build();

    public static final Option<Path>  VAULT_DIR = new OptionBuilder<>("vault-dir", Path.class)
            .category(OptionCategory.VAULT)
            .description("If set, secrets can be obtained by reading the content of files within the given directory.")
            .build();

    public static final Option<String>  VAULT_PASS = new OptionBuilder<>("vault-pass", String.class)
            .category(OptionCategory.VAULT)
            .description("Password for the vault keystore.")
            .build();

    public static final Option<Path>  VAULT_FILE = new OptionBuilder<>("vault-file", Path.class)
            .category(OptionCategory.VAULT)
            .description("Path to the keystore file.")
            .build();

    public static final Option<String>  VAULT_TYPE = new OptionBuilder<>("vault-type", String.class)
            .category(OptionCategory.VAULT)
            .description("Specifies the type of the keystore file.")
            .defaultValue("PKCS12")
            .build();
}
