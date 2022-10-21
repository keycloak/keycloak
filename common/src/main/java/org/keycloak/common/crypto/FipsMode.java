package org.keycloak.common.crypto;

public enum FipsMode {
    enabled("org.keycloak.crypto.elytron.WildFlyElytronProvider"),
    strict("org.keycloak.crypto.elytron.WildFlyElytronProvider"),
    disabled("org.keycloak.crypto.elytron.WildFlyElytronProvider");

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
