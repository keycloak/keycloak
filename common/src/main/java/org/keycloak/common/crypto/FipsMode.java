package org.keycloak.common.crypto;

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
