package org.keycloak.jose.jws.crypto;

import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.jose.jws.JWSInput;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class JWSSignatureProvider implements SignatureProvider {
    private static final Logger LOG = Logger.getLogger(JWSSignatureProvider.class.getName());

    public static boolean verify(JWSInput input, PublicKey publicKey) {
        AlgorithmType algorithmType = input.getHeader().getAlgorithm().getType();
        boolean isVerified = false;

        dumpJwsHeader(input);

        if (AlgorithmType.RSA.equals(algorithmType)) isVerified = RSAProvider.verify(input, publicKey);
        else if (AlgorithmType.ECDSA.equals(algorithmType)) isVerified = ECDSAProvider.verify(input, publicKey);
        return isVerified;
    }

    @Override
    public boolean verify(JWSInput input, String key) {
        return verify(input, key);
    }

    private static void dumpJwsHeader(JWSInput input) {
        final String className = JWSSignatureProvider.class.getName();
        LOG.logp(Level.FINE, className, "verify", new StringBuilder("jws.getHeader().getAlgorithm() = ").append(input.getHeader().getAlgorithm()).toString());
        LOG.logp(Level.FINE, className, "verify", new StringBuilder("jws.getHeader().getKeyId() = ").append(input.getHeader().getKeyId()).toString());
        LOG.logp(Level.FINE, className, "verify", new StringBuilder("jws.getHeader().getType() = ").append(input.getHeader().getType()).toString());
    }
}
