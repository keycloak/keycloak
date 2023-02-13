package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

public class ClientECDSASignatureVerifierContext extends AsymmetricSignatureVerifierContext {
    public ClientECDSASignatureVerifierContext(KeycloakSession session, ClientModel client, JWSInput input) throws VerificationException {
        super(getKey(session, client, input));
    }

    private static KeyWrapper getKey(KeycloakSession session, ClientModel client, JWSInput input) throws VerificationException {
        KeyWrapper key = PublicKeyStorageManager.getClientPublicKeyWrapper(session, client, input);
        if (key == null) {
            throw new VerificationException("Key not found");
        }
        if (!KeyType.EC.equals(key.getType())) {
            throw new VerificationException("Key Type is not EC: " + key.getType());
        }
        if (key.getAlgorithm() == null) {
            // defaults to the algorithm set to the JWS
            // validations should be performed prior to verifying signature in case there are restrictions on the algorithms
            // that can used for signing
            key.setAlgorithm(input.getHeader().getRawAlgorithm());
        } else if (!key.getAlgorithm().equals(input.getHeader().getRawAlgorithm())) {
            throw new VerificationException("Key Algorithms are different, key-algorithm=" + key.getAlgorithm()
                    + " jwt-algorithm=" + input.getHeader().getRawAlgorithm());
        }
        return key;
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) throws VerificationException {
        try {
            /*
            Fallback for backwards compatibility of ECDSA signed tokens which were issued in previous versions.
            TODO remove by https://issues.jboss.org/browse/KEYCLOAK-11911
             */
            int expectedSize = ECDSASignatureProvider.ECDSA.valueOf(getAlgorithm()).getSignatureLength();
            byte[] derSignature = expectedSize != signature.length && signature[0] == 0x30 ? signature : ECDSASignatureProvider.concatenatedRSToASN1DER(signature, expectedSize);
            return super.verify(data, derSignature);
        } catch (Exception e) {
            throw new VerificationException("Signing failed", e);
        }
    }
}
