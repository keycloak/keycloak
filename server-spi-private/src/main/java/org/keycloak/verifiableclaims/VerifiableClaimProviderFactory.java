package org.keycloak.verifiableclaims;

import org.keycloak.provider.ProviderFactory;

public interface VerifiableClaimProviderFactory extends ProviderFactory<VerifiableClaimProvider> {
    @Override default String getId() { return "default-verifiable-claims"; }
}
