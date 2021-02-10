package org.keycloak.crypto;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSHeader;


public class DelegatingSignatureVerfierContext implements SignatureVerifierContext{

    private final TokenVerifier<?> tokenVerifier;

    private final SignatureVerifierContextAccessor verifierContextAccessor;

    public DelegatingSignatureVerfierContext(TokenVerifier<?> tokenVerifier, SignatureVerifierContextAccessor verifierContextAccessor) {
        this.tokenVerifier = tokenVerifier;
        this.verifierContextAccessor = verifierContextAccessor;
    }

    @Override
    public String getKid() {
        // not used here, since we access the kid in #verify(...)
        return null;
    }

    @Override
    public String getAlgorithm() {
        // not used here, since we access the algorithm in #verify(...)
        return null;
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) throws VerificationException {
        JWSHeader header = tokenVerifier.getHeader();
        Algorithm algorithm = header.getAlgorithm();
        SignatureVerifierContext verifierContext = verifierContextAccessor.create(algorithm.name(), header.getKeyId());
        return verifierContext.verify(data, signature);
    }

}
