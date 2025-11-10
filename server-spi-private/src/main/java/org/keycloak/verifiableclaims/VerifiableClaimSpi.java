package org.keycloak.verifiableclaims;

import org.keycloak.provider.Spi;

public final class VerifiableClaimSpi implements Spi {
    public static final String NAME = "verifiable-claims";
    @Override public String getName() { return NAME; }
    @Override @SuppressWarnings("rawtypes") public Class getProviderClass() { return VerifiableClaimProvider.class; }
    @Override @SuppressWarnings("rawtypes") public Class getProviderFactoryClass() { return VerifiableClaimProviderFactory.class; }
    @Override public boolean isInternal() { return false; }
}
