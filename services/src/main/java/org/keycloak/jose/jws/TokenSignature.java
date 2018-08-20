package org.keycloak.jose.jws;

import org.jboss.logging.Logger;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.SignatureContext;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.TokenUtil;

public class TokenSignature {

    private static final Logger logger = Logger.getLogger(TokenSignature.class);

    private TokenSignature() {
    }

    public static String sign(KeycloakSession session, String sigAlgName, JsonWebToken jwt) {
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, sigAlgName);
        SignatureContext signer = signatureProvider.signer();

        String encodedToken = new JWSBuilder().type("JWT").jsonContent(jwt).sign(signer);
        return encodedToken;
    }

    public static boolean verify(KeycloakSession session, JWSInput jws) {
        String sigAlgName = jws.getHeader().getAlgorithm().name();

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, sigAlgName);
        if (signatureProvider == null) return false;

        String kid = jws.getHeader().getKeyId();
        // Backwards compatibility. Old offline tokens and cookies didn't have KID in the header
        if (kid == null) {
            logger.debugf("KID is null in token. Using the realm active key to verify token signature.");
            kid = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, sigAlgName).getKid();
        }

        try {
            return signatureProvider.verifier(kid).verify(jws.getEncodedSignatureInput().getBytes("UTF-8"), jws.getSignature());
        } catch (Exception e) {
            return false;
        }
    }

}
