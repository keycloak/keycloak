package org.keycloak.keys;

import java.security.PublicKey;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class EcdsaKeyMetadata extends KeyMetadata {

    private PublicKey publicKey;

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
