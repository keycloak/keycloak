package org.keycloak.encryption;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

public interface EncryptionProviderFactory extends ProviderFactory<EncryptionProvider> {
    @Override
    EncryptionProvider create(KeycloakSession session);
}
