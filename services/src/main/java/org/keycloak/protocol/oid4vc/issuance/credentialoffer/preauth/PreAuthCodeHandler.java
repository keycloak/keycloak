package org.keycloak.protocol.oid4vc.issuance.credentialoffer.preauth;

import org.keycloak.common.VerificationException;
import org.keycloak.protocol.oid4vc.model.PreAuthCodeCtx;
import org.keycloak.provider.Provider;

/**
 * Handles the production and verification of pre-authorized codes.
 */
public interface PreAuthCodeHandler extends Provider {

    /**
     * Generates a pre-authorized code for a given context of non-sensitive fields.
     * The implementation is responsible for embedding this context so it is recovered with verification.
     */
    String createPreAuthCode(PreAuthCodeCtx ctx);

    /**
     * Verifies the given pre-authorized code and returns its associated context if valid.
     */
    PreAuthCodeCtx verifyPreAuthCode(String preAuthCode) throws VerificationException;
}
