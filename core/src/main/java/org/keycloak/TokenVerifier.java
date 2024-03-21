import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.exceptions.TokenSignatureInvalidException;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.crypto.HMACProvider;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.JsonWebToken;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenVerifier<T extends JsonWebToken> {

    private static final Logger LOG = Logger.getLogger(TokenVerifier.class.getName());

    public static interface Predicate<T extends JsonWebToken> {
        boolean test(T t) throws VerificationException;
    }

    public static final Predicate<JsonWebToken> SUBJECT_EXISTS_CHECK = new Predicate<JsonWebToken>() {
        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            String subject = t.getSubject();
            if (subject == null) {
                throw new VerificationException("Subject missing in token");
            }

            return true;
        }
    };

    public static final Predicate<JsonWebToken> IS_ACTIVE = new Predicate<JsonWebToken>() {
        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (!t.isActive()) {
                throw new TokenNotActiveException(t, "Token is not active");
            }

            return true;
        }
    };

    public static class FederatedIdentityCheck implements Predicate<JsonWebToken> {

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            String federatedIdentity = t.getOtherClaims().get("federated_identity");
            if (federatedIdentity == null || federatedIdentity.isEmpty()) {
                throw new VerificationException("Federated identity missing in token");
            }

            // Add any additional checks or validations you need for federated_identity
            // ...

            return true;
        }
    };

    // ... rest of the existing code ...

    private String tokenString;
    private Class<? extends T> clazz;
    private PublicKey publicKey;
    private SecretKey secretKey;
    private String realmUrl;
    private List<String> expectedTokenType = Arrays.asList(TokenUtil.TOKEN_TYPE_BEARER, TokenUtil.TOKEN_TYPE_DPOP);
    private boolean checkTokenType = true;
    private boolean checkRealmUrl = true;
    private final LinkedList<Predicate<? super T>> checks = new LinkedList<>();

    private JWSInput jws;
    private T token;

    private SignatureVerifierContext verifier = null;

    public TokenVerifier<T> verifierContext(SignatureVerifierContext verifier) {
        this.verifier = verifier;
        return this;
    }

    // ... rest of the existing code ...

    /**
     * Adds default checks to the token verification:
     * <ul>
     * <li>... other checks ...</li>
     * <li>Custom check for federated_identity</li>
     * </ul>
     * @return This token verifier.
     */
    public TokenVerifier<T> withDefaultChecks()  {
        return withChecks(
                RealmUrlCheck.NULL_INSTANCE,
                SUBJECT_EXISTS_CHECK,
                TokenTypeCheck.INSTANCE_DEFAULT_TOKEN_TYPE,
                IS_ACTIVE,
                new FederatedIdentityCheck()  // Add the custom check for federated_identity
        );
    }

    // ... rest of the existing code ...

    public TokenVerifier<T> verify() throws VerificationException {
        if (getToken() == null) {
            parse();
        }
        if (jws != null) {
            verifySignature();
        }

        for (Predicate<? super T> check : checks) {
            if (!check.test(getToken())) {
                throw new VerificationException("JWT check failed for check " + check);
            }
        }

        return this;
    }

    // ... rest of the existing code ...
}
