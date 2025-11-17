package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class InMemoryCredentialOfferStorageFactory implements CredentialOfferStorageFactory {

    private static CredentialOfferStorage INSTANCE;

    @Override
    public CredentialOfferStorage create(KeycloakSession session) {
        if (INSTANCE==null) {
            INSTANCE = new InMemoryCredentialOfferStorage();
        }
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return "in_memory";
    }
}
