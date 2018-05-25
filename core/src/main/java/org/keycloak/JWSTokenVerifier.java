package org.keycloak;

import java.security.PublicKey;
import java.util.logging.Logger;

import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class JWSTokenVerifier {

    private static final Logger LOG = Logger.getLogger(JWSTokenVerifier.class.getName());

    protected final TokenVerifier<AccessToken> tokenVerifier;

    public static JWSTokenVerifier create(String tokenString) throws VerificationException  {
        JWSTokenVerifier tokenVerifier = null;
        try {
            JWSInput jws = new JWSInput(tokenString);
            AlgorithmType algorithmType = jws.getHeader().getAlgorithm().getType();
            if (AlgorithmType.RSA.equals(algorithmType)) {
                tokenVerifier = RSATokenVerifier.create(tokenString);
            } else if (AlgorithmType.ECDSA.equals(algorithmType)) {
                tokenVerifier = ECDSATokenVerifier.create(tokenString);
            }
        } catch (JWSInputException e) {
            LOG.warning("JWS parse error.");
            throw new VerificationException("JWT check failed. " + e);
        }
        return tokenVerifier;
    }

    protected JWSTokenVerifier(String tokenString) {
        this.tokenVerifier = TokenVerifier.create(tokenString, AccessToken.class).withDefaultChecks();
    }

    public JWSTokenVerifier publicKey(PublicKey publicKey) {
        tokenVerifier.publicKey(publicKey);
        return this;
    }

    public JWSTokenVerifier realmUrl(String realmUrl) {
        tokenVerifier.realmUrl(realmUrl);
        return this;
    }

    public JWSTokenVerifier checkTokenType(boolean checkTokenType) {
        tokenVerifier.checkTokenType(checkTokenType);
        return this;
    }

    public JWSTokenVerifier checkActive(boolean checkActive) {
        tokenVerifier.checkActive(checkActive);
        return this;
    }

    public JWSTokenVerifier checkRealmUrl(boolean checkRealmUrl) {
        tokenVerifier.checkRealmUrl(checkRealmUrl);
        return this;
    }

    public JWSTokenVerifier parse() throws VerificationException {
        tokenVerifier.parse();
        return this;
    }

    public AccessToken getToken() throws VerificationException {
        return tokenVerifier.getToken();
    }

    public JWSHeader getHeader() throws VerificationException {
        return tokenVerifier.getHeader();
    }

    public JWSTokenVerifier verify() throws VerificationException {
        tokenVerifier.verify();
        return this;
    }
}
