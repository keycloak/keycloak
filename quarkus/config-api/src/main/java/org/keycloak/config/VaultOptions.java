package org.keycloak.config;

import java.io.File;

public class VaultOptions {

    public enum Provider {
        file;
    }

    public static final Option VAULT = new OptionBuilder<>("vault", Provider.class)
            .category(OptionCategory.VAULT)
            .description("Enables a vault provider.")
            .buildTime(true)
            .build();

    public static final Option VAULT_DIR = new OptionBuilder<>("vault-dir", File.class)
            .category(OptionCategory.VAULT)
            .description("If set, secrets can be obtained by reading the content of files within the given directory.")
            .build();

}
