package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import org.keycloak.provider.Spi;

public class CredentialOfferStorageSpi implements Spi {
    @Override public String getName() { return "credential-offer-storage"; }
    @Override public Class<CredentialOfferStorage> getProviderClass() { return CredentialOfferStorage.class; }
    @Override public Class<CredentialOfferStorageFactory> getProviderFactoryClass() { return CredentialOfferStorageFactory.class; }
    @Override public boolean isInternal() { return false; }
}
