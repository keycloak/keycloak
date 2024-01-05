package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import java.time.Clock;

/**
 * {@link VerifiableCredentialsSigningService} implementing the SD_JWT_VC format. It returns a String, containing
 * the signed SD-JWT
 * <p>
 * {@see https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html}
 * {@see https://www.ietf.org/archive/id/draft-fett-oauth-selective-disclosure-jwt-02.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class SdJwtSigningService extends SigningService<String> {


    public SdJwtSigningService(KeycloakSession keycloakSession, String keyId, Clock clock, String algorithmType) {
        super(keycloakSession, keyId, clock, algorithmType);
    }

    @Override
    public String signCredential(VerifiableCredential verifiableCredential) {
        throw new UnsupportedOperationException("SD-JWT Signing is not yet supported.");
    }
}
