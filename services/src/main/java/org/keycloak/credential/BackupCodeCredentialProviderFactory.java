package org.keycloak.credential;

import org.keycloak.models.KeycloakSession;

public class BackupCodeCredentialProviderFactory implements CredentialProviderFactory<BackupCodeCredentialProvider> {

    public static final String PROVIDER_ID = "keycloak-backup-codes";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public BackupCodeCredentialProvider create(KeycloakSession session) {
        return new BackupCodeCredentialProvider(session);
    }

}
