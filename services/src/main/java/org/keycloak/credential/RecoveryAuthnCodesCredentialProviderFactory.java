package org.keycloak.credential;

import org.keycloak.models.KeycloakSession;

public class RecoveryAuthnCodesCredentialProviderFactory implements CredentialProviderFactory<RecoveryAuthnCodesCredentialProvider> {

    public static final String PROVIDER_ID = "keycloak-recovery-authn-codes";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public RecoveryAuthnCodesCredentialProvider create(KeycloakSession session) {
        return new RecoveryAuthnCodesCredentialProvider(session);
    }

}
