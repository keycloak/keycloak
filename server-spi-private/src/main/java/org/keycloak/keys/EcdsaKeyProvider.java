package org.keycloak.keys;

import org.keycloak.jose.jws.AlgorithmType;

import java.security.PrivateKey;
import java.security.PublicKey;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public interface EcdsaKeyProvider extends KeyProvider<EcdsaKeyMetadata> {

    default AlgorithmType getType() {
        return AlgorithmType.ECDSA;
    }

    /**
     * Return the private key for the active keypair, or <code>null</code> if no active key is available.
     *
     * @return
     */
    PrivateKey getPrivateKey();

    /**
     * Return the public key for the specified kid, or <code>null</code> if the kid is unknown.
     *
     * @param kid
     * @return
     */
    PublicKey getPublicKey(String kid);

}
