package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;

import java.time.Clock;

/**
 * Abstract base class to provide the Signing Services common functionality
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public abstract class SigningService<T> implements VerifiableCredentialsSigningService<T> {

    protected final KeycloakSession keycloakSession;
    protected final String keyId;
    protected final Clock clock;
    // values of the type field are defined by the implementing service. Could f.e. the security suite for ldp_vc or the algorithm to be used for jwt_vc
    protected final String type;

    protected SigningService(KeycloakSession keycloakSession, String keyId, Clock clock, String type) {
        this.keycloakSession = keycloakSession;
        this.keyId = keyId;
        this.clock = clock;
        this.type = type;
    }

    protected KeyWrapper getKey(String kid, String algorithm) {
        return keycloakSession.keys().getKey(keycloakSession.getContext().getRealm(), kid, KeyUse.SIG, algorithm);
    }

    @Override
    public void close() {
        // no-op
    }
}
