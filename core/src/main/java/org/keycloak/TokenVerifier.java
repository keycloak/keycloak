/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak;

import org.keycloak.common.VerificationException;
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
import org.keycloak.util.TokenUtil;

import javax.crypto.SecretKey;

import java.security.PublicKey;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenVerifier<T extends JsonWebToken> {

    private static final Logger LOG = Logger.getLogger(TokenVerifier.class.getName());

    // This interface is here as JDK 7 is a requirement for this project.
    // Once JDK 8 would become mandatory, java.util.function.Predicate would be used instead.

    /**
     * Functional interface of checks that verify some part of a JWT.
     * @param <T> Type of the token handled by this predicate.
     */
    // @FunctionalInterface
    public static interface Predicate<T extends JsonWebToken> {
        /**
         * Performs a single check on the given token verifier.
         * @param t Token, guaranteed to be non-null.
         * @return
         * @throws VerificationException
         */
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

    /**
     * Check for token being neither expired nor used before it gets valid.
     * @see JsonWebToken#isActive()
     */
    public static final Predicate<JsonWebToken> IS_ACTIVE = new Predicate<JsonWebToken>() {
        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (! t.isActive()) {
                throw new TokenNotActiveException(t, "Token is not active");
            }

            return true;
        }
    };

    public static class RealmUrlCheck implements Predicate<JsonWebToken> {

        private static final RealmUrlCheck NULL_INSTANCE = new RealmUrlCheck(null);

        private final String realmUrl;

        public RealmUrlCheck(String realmUrl) {
            this.realmUrl = realmUrl;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (this.realmUrl == null) {
                throw new VerificationException("Realm URL not set");
            }

            if (! this.realmUrl.equals(t.getIssuer())) {
                throw new VerificationException("Invalid token issuer. Expected '" + this.realmUrl + "', but was '" + t.getIssuer() + "'");
            }

            return true;
        }
    };

    public static class TokenTypeCheck implements Predicate<JsonWebToken> {

        private static final TokenTypeCheck INSTANCE_BEARER = new TokenTypeCheck(TokenUtil.TOKEN_TYPE_BEARER);

        private final String tokenType;

        public TokenTypeCheck(String tokenType) {
            this.tokenType = tokenType;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (! tokenType.equalsIgnoreCase(t.getType())) {
                throw new VerificationException("Token type is incorrect. Expected '" + tokenType + "' but was '" + t.getType() + "'");
            }
            return true;
        }
    };


    public static class AudienceCheck implements Predicate<JsonWebToken> {

        private final String expectedAudience;

        public AudienceCheck(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (expectedAudience == null) {
                throw new VerificationException("Missing expectedAudience");
            }

            String[] audience = t.getAudience();
            if (audience == null) {
                throw new VerificationException("No audience in the token");
            }

            if (t.hasAudience(expectedAudience)) {
                return true;
            }

            throw new VerificationException("Expected audience not available in the token");
        }
    };


    public static class IssuedForCheck implements Predicate<JsonWebToken> {

        private final String expectedIssuedFor;

        public IssuedForCheck(String expectedIssuedFor) {
            this.expectedIssuedFor = expectedIssuedFor;
        }

        @Override
        public boolean test(JsonWebToken jsonWebToken) throws VerificationException {
            if (expectedIssuedFor == null) {
                throw new VerificationException("Missing expectedIssuedFor");
            }

            if (expectedIssuedFor.equals(jsonWebToken.getIssuedFor())) {
                return true;
            }

            throw new VerificationException("Expected issuedFor doesn't match");
        }
    }


    private String tokenString;
    private Class<? extends T> clazz;
    private PublicKey publicKey;
    private SecretKey secretKey;
    private String realmUrl;
    private String expectedTokenType = TokenUtil.TOKEN_TYPE_BEARER;
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

    protected TokenVerifier(String tokenString, Class<T> clazz) {
        this.tokenString = tokenString;
        this.clazz = clazz;
    }

    protected TokenVerifier(T token) {
        this.token = token;
    }

    /**
     * Creates an instance of {@code TokenVerifier} from the given string on a JWT of the given class.
     * The token verifier has no checks defined. Note that the checks are only tested when
     * {@link #verify()} method is invoked.
     * @param <T> Type of the token
     * @param tokenString String representation of JWT
     * @param clazz Class of the token
     * @return
     */
    public static <T extends JsonWebToken> TokenVerifier<T> create(String tokenString, Class<T> clazz) {
        return new TokenVerifier(tokenString, clazz);
    }

    /**
     * Creates an instance of {@code TokenVerifier} for the given token.
     * The token verifier has no checks defined. Note that the checks are only tested when
     * {@link #verify()} method is invoked.
     * <p>
     * <b>NOTE:</b> The returned token verifier cannot verify token signature since
     * that is not part of the {@link JsonWebToken} object.
     * @return
     */
    public static <T extends JsonWebToken> TokenVerifier<T> createWithoutSignature(T token) {
        return new TokenVerifier(token);
    }

    /**
     * Adds default checks to the token verification:
     * <ul>
     * <li>Realm URL (JWT issuer field: {@code iss}) has to be defined and match realm set via {@link #realmUrl(java.lang.String)} method</li>
     * <li>Subject (JWT subject field: {@code sub}) has to be defined</li>
     * <li>Token type (JWT type field: {@code typ}) has to be {@code Bearer}. The type can be set via {@link #tokenType(java.lang.String)} method</li>
     * <li>Token has to be active, ie. both not expired and not used before its validity (JWT issuer fields: {@code exp} and {@code nbf})</li>
     * </ul>
     * @return This token verifier.
     */
    public TokenVerifier<T> withDefaultChecks()  {
        return withChecks(
          RealmUrlCheck.NULL_INSTANCE,
          SUBJECT_EXISTS_CHECK,
          TokenTypeCheck.INSTANCE_BEARER,
          IS_ACTIVE
        );
    }

    private void removeCheck(Class<? extends Predicate<?>> checkClass) {
        for (Iterator<Predicate<? super T>> it = checks.iterator(); it.hasNext();) {
            if (it.next().getClass() == checkClass) {
                it.remove();
            }
        }
    }

    private void removeCheck(Predicate<? super T> check) {
        checks.remove(check);
    }

    private <P extends Predicate<? super T>> TokenVerifier<T> replaceCheck(Class<? extends Predicate<?>> checkClass, boolean active, P predicate) {
        removeCheck(checkClass);
        if (active) {
            checks.add(predicate);
        }
        return this;
    }

    private <P extends Predicate<? super T>> TokenVerifier<T> replaceCheck(Predicate<? super T> check, boolean active, P predicate) {
        removeCheck(check);
        if (active) {
            checks.add(predicate);
        }
        return this;
    }

    /**
     * Will test the given checks in {@link #verify()} method in addition to already set checks.
     * @param checks
     * @return
     */
    public TokenVerifier<T> withChecks(Predicate<? super T>... checks) {
        if (checks != null) {
            this.checks.addAll(Arrays.asList(checks));
        }
        return this;
    }

    /**
     * Sets the key for verification of RSA-based signature.
     * @param publicKey
     * @return
     */
    public TokenVerifier<T> publicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * Sets the key for verification of HMAC-based signature.
     * @param secretKey
     * @return 
     */
    public TokenVerifier<T> secretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    /**
     * @deprecated This method is here only for backward compatibility with previous version of {@code TokenVerifier}.
     * @return This token verifier
     */
    public TokenVerifier<T> realmUrl(String realmUrl) {
        this.realmUrl = realmUrl;
        return replaceCheck(RealmUrlCheck.class, checkRealmUrl, new RealmUrlCheck(realmUrl));
    }

    /**
     * @deprecated This method is here only for backward compatibility with previous version of {@code TokenVerifier}.
     * @return This token verifier
     */
    public TokenVerifier<T> checkTokenType(boolean checkTokenType) {
        this.checkTokenType = checkTokenType;
        return replaceCheck(TokenTypeCheck.class, this.checkTokenType, new TokenTypeCheck(expectedTokenType));
    }

    /**
     * @deprecated This method is here only for backward compatibility with previous version of {@code TokenVerifier}.
     * @return This token verifier
     */
    public TokenVerifier<T> tokenType(String tokenType) {
        this.expectedTokenType = tokenType;
        return replaceCheck(TokenTypeCheck.class, this.checkTokenType, new TokenTypeCheck(expectedTokenType));
    }

    /**
     * @deprecated This method is here only for backward compatibility with previous version of {@code TokenVerifier}.
     * @return This token verifier
     */
    public TokenVerifier<T> checkActive(boolean checkActive) {
        return replaceCheck(IS_ACTIVE, checkActive, IS_ACTIVE);
    }

    /**
     * @deprecated This method is here only for backward compatibility with previous version of {@code TokenVerifier}.
     * @return This token verifier
     */
    public TokenVerifier<T> checkRealmUrl(boolean checkRealmUrl) {
        this.checkRealmUrl = checkRealmUrl;
        return replaceCheck(RealmUrlCheck.class, this.checkRealmUrl, new RealmUrlCheck(realmUrl));
    }

    /**
     * Add check for verifying that token contains the expectedAudience
     *
     * @param expectedAudience Audience, which needs to be in the target token. Can't be null
     * @return This token verifier
     */
    public TokenVerifier<T> audience(String expectedAudience) {
        return this.replaceCheck(AudienceCheck.class, true, new AudienceCheck(expectedAudience));
    }

    /**
     * Add check for verifying that token issuedFor (azp claim) is the expected value
     *
     * @param expectedIssuedFor issuedFor, which needs to be in the target token. Can't be null
     * @return This token verifier
     */
    public TokenVerifier<T> issuedFor(String expectedIssuedFor) {
        return this.replaceCheck(IssuedForCheck.class, true, new IssuedForCheck(expectedIssuedFor));
    }

    public TokenVerifier<T> parse() throws VerificationException {
        if (jws == null) {
            if (tokenString == null) {
                throw new VerificationException("Token not set");
            }

            try {
                jws = new JWSInput(tokenString);
            } catch (JWSInputException e) {
                throw new VerificationException("Failed to parse JWT", e);
            }


            try {
                token = jws.readJsonContent(clazz);
            } catch (JWSInputException e) {
                throw new VerificationException("Failed to read access token from JWT", e);
            }
        }
        return this;
    }

    public T getToken() throws VerificationException {
        if (token == null) {
            parse();
        }
        return token;
    }

    public JWSHeader getHeader() throws VerificationException {
        parse();
        return jws.getHeader();
    }

    public void verifySignature() throws VerificationException {
        if (this.verifier != null) {
            try {
                if (!verifier.verify(jws.getEncodedSignatureInput().getBytes("UTF-8"), jws.getSignature())) {
                    throw new TokenSignatureInvalidException(token, "Invalid token signature");
                }
            } catch (Exception e) {
                throw new VerificationException(e);
            }
        } else {
            AlgorithmType algorithmType = getHeader().getAlgorithm().getType();

            if (null == algorithmType) {
                throw new VerificationException("Unknown or unsupported token algorithm");
            } else switch (algorithmType) {
                case RSA:
                    if (publicKey == null) {
                        throw new VerificationException("Public key not set");
                    }
                    if (!RSAProvider.verify(jws, publicKey)) {
                        throw new TokenSignatureInvalidException(token, "Invalid token signature");
                    }
                    break;
                case HMAC:
                    if (secretKey == null) {
                        throw new VerificationException("Secret key not set");
                    }
                    if (!HMACProvider.verify(jws, secretKey)) {
                        throw new TokenSignatureInvalidException(token, "Invalid token signature");
                    }
                    break;
                default:
                    throw new VerificationException("Unknown or unsupported token algorithm");
            }
        }
    }

    public TokenVerifier<T> verify() throws VerificationException {
        if (getToken() == null) {
            parse();
        }
        if (jws != null) {
            verifySignature();
        }

        for (Predicate<? super T> check : checks) {
            if (! check.test(getToken())) {
                throw new VerificationException("JWT check failed for check " + check);
            }
        }

        return this;
    }

    /**
     * Creates an optional predicate from a predicate that will proceed with check but always pass.
     * @param <T>
     * @param mandatoryPredicate
     * @return
     */
    public static <T extends JsonWebToken> Predicate<T> optional(final Predicate<T> mandatoryPredicate) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) throws VerificationException {
                try {
                    if (! mandatoryPredicate.test(t)) {
                        LOG.finer("[optional] predicate failed: " + mandatoryPredicate);
                    }

                    return true;
                } catch (VerificationException ex) {
                    LOG.log(Level.FINER, "[optional] predicate " + mandatoryPredicate + " failed.", ex);
                    return true;
                }
            }
        };
    }

    /**
     * Creates a predicate that will proceed with checks of the given predicates
     * and will pass if and only if at least one of the given predicates passes.
     * @param <T>
     * @param predicates
     * @return
     */
    public static <T extends JsonWebToken> Predicate<T> alternative(final Predicate<? super T>... predicates) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                for (Predicate<? super T> predicate : predicates) {
                    try {
                        if (predicate.test(t)) {
                            return true;
                        }

                        LOG.finer("[alternative] predicate failed: " + predicate);
                    } catch (VerificationException ex) {
                        LOG.log(Level.FINER, "[alternative] predicate " + predicate + " failed.", ex);
                    }
                }

                return false;
            }
        };
    }
}
