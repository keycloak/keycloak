package org.keycloak.protocol.oid4vc.issuance.signing;


import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import java.time.Clock;

/**
 * {@link VerifiableCredentialsSigningService} implementing the JWT_VC format. It returns a string, containing the
 * Signed JWT-Credential
 * {@see https://identity.foundation/jwt-vc-presentation-profile/}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class JwtSigningService extends SigningService<String> {

    public JwtSigningService(KeycloakSession keycloakSession, String keyId, Clock clock, String algorithmType) {
        super(keycloakSession, keyId, clock, algorithmType);
    }

    @Override
    public String signCredential(VerifiableCredential verifiableCredential) {

        throw new UnsupportedOperationException("JWT-VC Credentials Signing is not yet supported.");
    }

}