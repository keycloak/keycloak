package org.keycloak.protocol.oid4vc.issuance.credentialoffer.preauth;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage.CredentialOfferState;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.JwtPreAuthCode;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.utils.StringUtil;
import org.keycloak.wellknown.WellKnownProvider;

import org.jboss.logging.Logger;

import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;

/**
 * Implementation of {@link PreAuthCodeHandler} for JWT pre-authorized codes.
 */
public class JwtPreAuthCodeHandler implements PreAuthCodeHandler {

    private static final Logger logger = Logger.getLogger(JwtPreAuthCodeHandler.class);

    private final KeycloakSession session;
    private final RealmModel realm;

    public JwtPreAuthCodeHandler(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    @Override
    public String createPreAuthCode(CredentialOfferState offerState) {
        // Building
        String salt = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
        JwtPreAuthCode jwtBody = (JwtPreAuthCode) new JwtPreAuthCode()
                .salt(salt)
                .credentialOfferState(offerState)
                .issuer(getCredentialIssuer())
                .addAudience(getTokenEndpoint())
                .issuedNow()
                .exp(offerState.getExpiration());

        // Signing
        SignatureSignerContext signer = getSignerContext(offerState);
        return new JWSBuilder()
                .type(OAuth2Constants.JWT)
                .jsonContent(jwtBody)
                .sign(signer);
    }

    @Override
    public CredentialOfferState verifyPreAuthCode(String preAuthCode) throws VerificationException {
        // Parse the JWT
        JWSInput jwsInput;
        try {
            jwsInput = new JWSInput(preAuthCode);
        } catch (JWSInputException e) {
            throw new VerificationException("Failed to parse pre-auth code: Not JWT", e);
        }

        // Verify JWT (Signature + claim conformance)
        TokenVerifier<JwtPreAuthCode> verifier = TokenVerifier.create(preAuthCode, JwtPreAuthCode.class);
        verifier.verifierContext(getVerifierContext(jwsInput.getHeader()));
        getPropertyVerifiers().forEach(verifier::withChecks);
        verifier.verify();

        // Parse payload as JwtPreAuthCode and extract CredentialOfferState
        try {
            JwtPreAuthCode jwtBody = jwsInput.readJsonContent(JwtPreAuthCode.class);
            return jwtBody.getCredentialOfferState();
        } catch (JWSInputException e) {
            throw new VerificationException("Failed to recover credential offer state", e);
        }
    }

    private String getCredentialIssuer() {
        return OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
    }

    private String getTokenEndpoint() {
        WellKnownProvider oidcProvider = session.getProvider(
                WellKnownProvider.class, OIDCWellKnownProviderFactory.PROVIDER_ID);
        OIDCConfigurationRepresentation oidcConfig = (OIDCConfigurationRepresentation) oidcProvider.getConfig();
        return oidcConfig.getTokenEndpoint();
    }

    /**
     * Retrieves the preferred signing algorithms for JWT pre-auth codes,
     * based on the configuration of credentials under consideration.
     */
    private List<String> getPreferredSigningAlgs(CredentialOfferState offerState) {
        List<String> configIds = Optional.ofNullable(offerState)
                .map(CredentialOfferState::getCredentialsOffer)
                .map(CredentialsOffer::getCredentialConfigurationIds)
                .orElse(List.of());

        Stream<CredentialScopeModel> credentialScopes = session.clientScopes()
                .getClientScopesByProtocol(realm, OID4VC_PROTOCOL)
                .map(CredentialScopeModel::new)
                .filter(s -> configIds.contains(
                        s.getCredentialConfigurationId()
                ));

        return credentialScopes.map(CredentialScopeModel::getSigningAlg)
                .filter(StringUtil::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * Retrieves a SignatureSignerContext for signing JWT pre-auth codes, based on the preferred
     * signing algorithms derived from the credential offer state.
     */
    private SignatureSignerContext getSignerContext(CredentialOfferState offerState) {
        List<String> preferredAlgs = getPreferredSigningAlgs(offerState);
        logger.debugf("Preferred signing algorithms for JWT pre-auth code: %s", preferredAlgs);
        for (String alg : preferredAlgs) {
            try {
                return getSignerContext(alg);
            } catch (RuntimeException ignored) {
                logger.debugf("No active signing key/context found for algorithm %s, skipping", alg);
            }
        }

        try {
            // Default to Algorithm.ES256 if no preferred algorithm is found or signing key available.
            // Under normal circumstances, an ES256 key should be generated on the fly.
            logger.debugf("Falling back to default algorithm %s for signing JWT pre-auth codes", Algorithm.ES256);
            return getSignerContext(Algorithm.ES256);
        } catch (RuntimeException e) {
            throw new IllegalStateException("No active signing key/context found for JWT pre-auth code signing", e);
        }
    }

    /**
     * Retrieves the SignatureSignerContext associated with the given algorithm.
     */
    private SignatureSignerContext getSignerContext(String alg) {
        KeyWrapper signingKey = session.keys().getActiveKey(realm, KeyUse.SIG, alg);
        if (signingKey == null) {
            throw new IllegalArgumentException(String.format("No active signing key found for algorithm %s", alg));
        }

        SignatureProvider signatureProvider = session.getProvider(
                SignatureProvider.class,
                signingKey.getAlgorithm());

        return signatureProvider.signer(signingKey);
    }

    /**
     * Retrieves a SignatureVerifierContext for verifying JWT pre-auth codes,
     * based on signing metadata in the JWS header and available keys.
     */
    private SignatureVerifierContext getVerifierContext(JWSHeader jwsHeader) throws VerificationException {
        String kid = jwsHeader.getKeyId();
        String alg = jwsHeader.getAlgorithm().toString();
        KeyWrapper key = session.keys().getKey(realm, kid, KeyUse.SIG, alg);

        if (key == null) {
            throw new VerificationException(String.format(
                    "No key found for verifying JWT pre-auth code with alg '%s' and kid '%s'",
                    alg, kid));
        }

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, key.getAlgorithm());
        return signatureProvider.verifier(key);
    }

    /**
     * Property verifiers.
     */
    private List<TokenVerifier.Predicate<JwtPreAuthCode>> getPropertyVerifiers() {
        TokenVerifier.Predicate<JwtPreAuthCode> offerStateCheck = jwt -> {
            if (jwt.getCredentialOfferState() == null
                    || jwt.getCredentialOfferState().getCredentialsOffer() == null) {
                throw new VerificationException("Not a jwt pre-auth code: no credential offer state found");
            }

            return true;
        };

        TokenVerifier.Predicate<JwtPreAuthCode> issuerCheck = jwt -> {
            String expectedIssuer = getCredentialIssuer();
            if (!expectedIssuer.equals(jwt.getIssuer())) {
                String message = String.format(
                        "Unexpected issuer of jwt pre-auth code: %s (expected) != %s (actual)",
                        expectedIssuer, jwt.getIssuer());
                throw new VerificationException(message);
            }
            return true;
        };

        TokenVerifier.Predicate<JwtPreAuthCode> audienceCheck = jwt -> {
            String expectedAudience = getTokenEndpoint();
            List<String> actualAudiences = Optional.ofNullable(jwt.getAudience())
                    .map(Arrays::asList)
                    .orElse(List.of());

            if (actualAudiences.isEmpty() || !actualAudiences.contains(expectedAudience)) {
                String message = String.format(
                        "Invalid audience of jwt pre-auth code: %s (expected) not in %s (actual)",
                        expectedAudience, actualAudiences);
                throw new VerificationException(message);
            }

            return true;
        };

        TokenVerifier.Predicate<JwtPreAuthCode> expirationCheck = jwt -> {
            Long exp = jwt.getExp();
            if (exp == null) {
                throw new VerificationException("Jwt pre-auth code has no expiration time");
            }

            long now = Time.currentTime();
            if (exp < now) {
                String message = String.format("Jwt pre-auth code not valid: %s (exp) < %s (now)", exp, now);
                throw new VerificationException(message, Errors.EXPIRED_CODE);
            }

            return true;
        };

        return List.of(offerStateCheck, issuerCheck, audienceCheck, expirationCheck);
    }

    @Override
    public void close() {
    }
}
