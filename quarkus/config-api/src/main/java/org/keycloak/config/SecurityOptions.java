package org.keycloak.config;

import org.keycloak.common.crypto.FipsMode;

public class SecurityOptions {

    public static final Option<FipsMode> FIPS_MODE = new OptionBuilder<>("fips-mode", FipsMode.class)
            .category(OptionCategory.SECURITY)
            .buildTime(true)
            .description("Sets the FIPS mode. If 'enabled' is set, FIPS is enabled but on non-approved mode. For full FIPS compliance, set 'strict' to run on approved mode.")
            .defaultValue(FipsMode.disabled)
            .build();

    public static final Option<String> PKCS11_CONFIG_FILE = new OptionBuilder<>("pkcs11-config-file", String.class)
            .category(OptionCategory.SECURITY)
            .buildTime(true)
            .description("The path to a file to be used to configure the underlying PKCS11 security provider.")
            .build();
}
