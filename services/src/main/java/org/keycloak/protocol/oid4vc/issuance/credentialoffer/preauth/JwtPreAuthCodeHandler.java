package org.keycloak.protocol.oid4vc.issuance.credentialoffer.preauth;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.JwtPreAuthCode;
import org.keycloak.protocol.oid4vc.model.PreAuthCodeCtx;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.wellknown.WellKnownProvider;

/**
 * Implementation of {@link PreAuthCodeHandler} for JWT pre-authorized codes.
 */
public class JwtPreAuthCodeHandler implements PreAuthCodeHandler {

    private final KeycloakSession session;

    public JwtPreAuthCodeHandler(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String createPreAuthCode(PreAuthCodeCtx preAuthCodeCtx) {
        // Clone and nullify exp claim to avoid duplication
        PreAuthCodeCtx clonedCtx = preAuthCodeCtx.clone();
        clonedCtx.setExpiresAt(null);

        // Building
        String salt = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
        JwtPreAuthCode jwtBody = (JwtPreAuthCode) new JwtPreAuthCode()
                .salt(salt)
                .context(clonedCtx)
                .issuer(getCredentialIssuer())
                .addAudience(getTokenEndpoint())
                .issuedNow()
                .exp(preAuthCodeCtx.getExpiresAt());

        // Signing
        return session.tokens().encode(jwtBody);
    }

    @Override
    public PreAuthCodeCtx verifyPreAuthCode(String preAuthCode) throws VerificationException {
        // Decode and verify the JWT pre-auth code, and recover jwt payload
        JwtPreAuthCode jwtPayload = session.tokens().decode(preAuthCode, JwtPreAuthCode.class);
        if (jwtPayload == null) {
            throw new VerificationException("Pre-auth code decoding/verification failed");
        }

        // Perform routine verification on claims
        verifyPreAuthCodeClaims(jwtPayload);

        // Restore exp information to pre-auth code context
        PreAuthCodeCtx preAuthCodeCtx = jwtPayload.getContext();
        preAuthCodeCtx.setExpiresAt(jwtPayload.getExp());

        return preAuthCodeCtx;
    }

    /**
     * Performs routine verification of the claims in the JWT pre-auth code.
     */
    private void verifyPreAuthCodeClaims(JwtPreAuthCode jwtPayload) throws VerificationException {
        if (jwtPayload.getContext() == null) {
            throw new VerificationException("Not a jwt pre-auth code: no pre-auth code context found");
        }

        String expectedIssuer = getCredentialIssuer();
        if (!expectedIssuer.equals(jwtPayload.getIssuer())) {
            String message = String.format(
                    "Unexpected issuer of jwt pre-auth code: %s (expected) != %s (actual)",
                    expectedIssuer, jwtPayload.getIssuer());
            throw new VerificationException(message);
        }

        String expectedAudience = getTokenEndpoint();
        List<String> actualAudiences = Optional.ofNullable(jwtPayload.getAudience())
                .map(Arrays::asList)
                .orElse(List.of());

        if (actualAudiences.isEmpty() || !actualAudiences.contains(expectedAudience)) {
            String message = String.format(
                    "Invalid audience of jwt pre-auth code: %s (expected) not in %s (actual)",
                    expectedAudience, actualAudiences);
            throw new VerificationException(message);
        }

        Long exp = jwtPayload.getExp();
        if (exp == null) {
            throw new VerificationException("Jwt pre-auth code has no expiration time");
        }

        long now = Time.currentTime();
        if (exp < now) {
            String message = String.format("Jwt pre-auth code not valid: %s (exp) < %s (now)", exp, now);
            throw new VerificationException(message, Errors.EXPIRED_CODE);
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

    @Override
    public void close() {
    }
}
