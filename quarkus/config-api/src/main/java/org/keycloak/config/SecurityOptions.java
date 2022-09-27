package org.keycloak.config;

public class SecurityOptions {

    public enum FipsMode {
        enabled("org.keycloak.crypto.fips.FIPS1402Provider"),
        strict("org.keycloak.crypto.fips.Fips1402StrictCryptoProvider"),
        disabled("org.keycloak.crypto.def.DefaultCryptoProvider");

        private String providerClassName;

        FipsMode(String providerClassName) {
            this.providerClassName = providerClassName;
        }

        public boolean isFipsEnabled() {
            return this.equals(enabled) || this.equals(strict);
        }

        public String getProviderClassName() {
            return providerClassName;
        }
    }

    public static final Option<FipsMode> FIPS_MODE = new OptionBuilder<>("fips-mode", FipsMode.class)
            .category(OptionCategory.SECURITY)
            .buildTime(true)
            .description("Sets the FIPS mode. If 'enabled' is set, FIPS is enabled but on non-approved mode. For full FIPS compliance, set 'strict' to run on approved mode.")
            .defaultValue(FipsMode.disabled)
            .build();
}
