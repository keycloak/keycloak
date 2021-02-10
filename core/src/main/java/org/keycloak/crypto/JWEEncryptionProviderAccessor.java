package org.keycloak.crypto;

import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

public interface JWEEncryptionProviderAccessor {

    JWEEncryptionProvider getJweEncryptionProvider(String encAlgorithm);
}
