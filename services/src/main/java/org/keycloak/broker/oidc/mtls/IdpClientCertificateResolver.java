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
        // Select an actually usable key: enabled, with a private key and a certificate (single cert or chain).
        // A provider may expose more than one enabled key, so usability is part of the selection rather than a
        // post-hoc check on the first match — otherwise a usable key would be missed because an earlier enabled
        // entry happened to be incomplete.
        return session.keys().getKeysStream(realm)
                .filter(k -> keyProviderId.equals(k.getProviderId()))
                .filter(k -> k.getStatus() != null && k.getStatus().isEnabled())
                .filter(IdpClientCertificateResolver::isUsableForMtls)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No enabled realm key with a private key and certificate found for client certificate provider id: "
                                + keyProviderId + "; cannot be used for tls_client_auth."));
    }

    private static boolean isUsableForMtls(KeyWrapper key) {
        boolean hasCertificate = key.getCertificate() != null
                || (key.getCertificateChain() != null && !key.getCertificateChain().isEmpty());
        // getPrivateKey() is typed as java.security.Key; require an actual PrivateKey to match what
        // IdpMtlsSslContextProvider.buildKeyManagers needs, so a non-PrivateKey entry is never selected only
        // to fail deterministically on the first mTLS request.
        return key.getPrivateKey() instanceof java.security.PrivateKey && hasCertificate;
    }
}
