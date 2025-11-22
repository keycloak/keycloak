package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import org.keycloak.provider.ProviderFactory;

public interface CredentialOfferStorageFactory extends ProviderFactory<CredentialOfferStorage> {

    @Override
    default void close() { }

}
