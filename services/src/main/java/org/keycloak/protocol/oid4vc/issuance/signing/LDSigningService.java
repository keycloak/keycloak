package org.keycloak.protocol.oid4vc.issuance.signing;


import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import java.time.Clock;

/**
 * {@link VerifiableCredentialsSigningService} implementing the LDP_VC format. It returns a Verifiable Credential,
 * containing the created LDProof.
 * <p>
 * {@see https://www.w3.org/TR/vc-data-model/}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class LDSigningService extends SigningService<VerifiableCredential> {


    public LDSigningService(KeycloakSession keycloakSession, String keyId, Clock clock, String ldpType) {
        super(keycloakSession, keyId, clock, ldpType);

    }

    @Override
    public VerifiableCredential signCredential(VerifiableCredential verifiableCredential) {

        throw new UnsupportedOperationException("LD-Credentials Signing is not yet supported.");
    }

}
