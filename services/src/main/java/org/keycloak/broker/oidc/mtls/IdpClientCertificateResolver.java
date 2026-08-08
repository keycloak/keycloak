package org.keycloak.broker.oidc.mtls;

import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Resolves the realm key used as the client certificate for an OIDC IdP configured with
 * tls_client_auth. The key is looked up by its key-provider component id; the key {@code use}
 * (sig/enc) is intentionally ignored — the private key + certificate chain are all mTLS needs.
 * Only keys with an enabled status are considered.
 */
public class IdpClientCertificateResolver {

    private final KeycloakSession session;

    public IdpClientCertificateResolver(KeycloakSession session) {
        this.session = session;
    }

    public KeyWrapper resolve(RealmModel realm, String keyProviderId) {
        KeyWrapper key = session.keys().getKeysStream(realm)
                .filter(k -> keyProviderId.equals(k.getProviderId()))
                .filter(k -> k.getStatus() != null && k.getStatus().isEnabled())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No enabled realm key found for client certificate provider id: " + keyProviderId));

        if (key.getPrivateKey() == null) {
            throw new IllegalStateException(
                    "Realm key " + keyProviderId + " has no private key; cannot be used for tls_client_auth.");
        }
        if (key.getCertificate() == null
                && (key.getCertificateChain() == null || key.getCertificateChain().isEmpty())) {
            throw new IllegalStateException(
                    "Realm key " + keyProviderId + " has no certificate; cannot be used for tls_client_auth.");
        }
        return key;
    }
}
