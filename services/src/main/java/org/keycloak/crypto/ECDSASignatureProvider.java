package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public class ECDSASignatureProvider implements SignatureProvider {

    private final KeycloakSession session;
    private final String algorithm;

    public ECDSASignatureProvider(KeycloakSession session, String algorithm) {
        this.session = session;
        this.algorithm = algorithm;
    }

    @Override
    public SignatureSignerContext signer() throws SignatureException {
        return new ServerECDSASignatureSignerContext(session, algorithm);
    }

    @Override
    public SignatureSignerContext signer(KeyWrapper key) throws SignatureException {
        SignatureProvider.checkKeyForSignature(key, algorithm, KeyType.EC);
        return new ServerECDSASignatureSignerContext(key);
    }

    @Override
    public SignatureVerifierContext verifier(String kid) throws VerificationException {
        return new ServerECDSASignatureVerifierContext(session, kid, algorithm);
    }

    @Override
    public SignatureVerifierContext verifier(KeyWrapper key) throws VerificationException {
        SignatureProvider.checkKeyForVerification(key, algorithm, KeyType.EC);
        return new ServerECDSASignatureVerifierContext(key);
    }

    @Override
    public boolean isAsymmetricAlgorithm() {
        return true;
    }

    public static byte[] concatenatedRSToASN1DER(final byte[] signature, int signLength) throws IOException {
        return CryptoIntegration.getProvider().getEcdsaCryptoProvider().concatenatedRSToASN1DER(signature, signLength);
    }

    public static byte[] asn1derToConcatenatedRS(final byte[] derEncodedSignatureValue, int signLength) throws IOException {
        return CryptoIntegration.getProvider().getEcdsaCryptoProvider().asn1derToConcatenatedRS(derEncodedSignatureValue, signLength);
    }

    public enum ECDSA {
        ES256(64),
        ES384(96),
        ES512(132);

        private final int signatureLength;

        ECDSA(int signatureLength) {
            this.signatureLength = signatureLength;
        }

        public int getSignatureLength() {
            return this.signatureLength;
        }
    }
}
