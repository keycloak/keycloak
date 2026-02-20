package org.keycloak.protocol.oid4vc.issuance.credentialoffer.preauth;

import org.keycloak.common.VerificationException;
import org.keycloak.provider.Provider;

import static org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage.CredentialOfferState;

/**
 * Handles the production and verification of pre-authorized codes.
 */
public interface PreAuthCodeHandler extends Provider {

    /**
     * Generates a pre-authorized code for the given credential offer state (only non-sensitive fields).
     * The implementation is responsible for storing any necessary state to be recovered with verification.
     */
    String createPreAuthCode(CredentialOfferState offerState);

    /**
     * Verifies the given pre-authorized code and returns the associated credential offer state if valid.
     */
    CredentialOfferState verifyPreAuthCode(String preAuthCode) throws VerificationException;
}
