package org.keycloak.broker.oidc.mtls;

/**
 * Signals the <em>expected</em> outcome that no usable client-certificate key could be resolved for a
 * tls_client_auth IdP (no enabled key with a private key and certificate for the configured provider id).
 *
 * <p>This is deliberately distinct from operational failures of the key-storage path (transient/internal
 * errors): configuration validation translates only this exception into an invalid-configuration error,
 * and lets other {@link RuntimeException}s propagate as server errors. Extends {@link IllegalStateException}
 * so existing callers that catch that type keep working.
 */
public class ClientCertificateKeyNotFoundException extends IllegalStateException {

    public ClientCertificateKeyNotFoundException(String message) {
        super(message);
    }
}
