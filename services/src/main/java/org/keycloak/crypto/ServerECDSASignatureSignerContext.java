package org.keycloak.crypto;

import org.keycloak.models.KeycloakSession;

public class ServerECDSASignatureSignerContext extends AsymmetricSignatureSignerContext {

    public ServerECDSASignatureSignerContext(KeycloakSession session, String algorithm) throws SignatureException {
        super(ServerAsymmetricSignatureSignerContext.getKey(session, algorithm));
    }

    public ServerECDSASignatureSignerContext(KeyWrapper key) {
        super(key);
    }

    @Override
    public byte[] sign(byte[] data) throws SignatureException {
        try {
            int size = ECDSASignatureProvider.ECDSA.valueOf(getAlgorithm()).getSignatureLength();
            return ECDSASignatureProvider.asn1derToConcatenatedRS(super.sign(data), size);
        } catch (Exception e) {
            throw new SignatureException("Signing failed", e);
        }
    }
}
