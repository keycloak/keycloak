package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;

public class ServerECDSASignatureVerifierContext extends  AsymmetricSignatureVerifierContext {
    public ServerECDSASignatureVerifierContext(KeycloakSession session, String kid, String algorithm) throws VerificationException {
        super(ServerAsymmetricSignatureVerifierContext.getKey(session, kid, algorithm));
    }

    public ServerECDSASignatureVerifierContext(KeyWrapper key) {
        super(key);
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
