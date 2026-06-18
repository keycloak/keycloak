package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.util.Map;
import java.util.Optional;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.TrustMaterialRequest;
import org.keycloak.broker.provider.TrustMaterialResolver;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * Implementation of AttestationKeyResolver that loads trusted keys for attestation validation
 * from configured trust-material IdPs in the client.
 */
public class TrustedAttestationKeyResolver implements AttestationKeyResolver {

    private static final Logger logger = Logger.getLogger(TrustedAttestationKeyResolver.class);

    private final KeycloakSession session;

    public TrustedAttestationKeyResolver(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Loads trusted key for attestation validation from configured trust-material IdPs in client.
     * The trust-material IdP aliases are configured via client attribute {@link OID4VCIConstants#OID4VCI_ATTESTER_TRUST_IDPS_ATTR}.
     *
     * @return JWK for the resolved trusted key, or null if not found.
     */
    @Override
    public JWK resolveKey(String kid, Map<String, Object> header, Map<String, Object> payload) {
        ClientModel client = session.getContext().getClient();
        if (client == null) {
            throw new IllegalStateException("Cannot load trust-material IdP aliases because client is null");
        }

        String trustIdpsConfig = client.getAttribute(OID4VCIConstants.OID4VCI_ATTESTER_TRUST_IDPS_ATTR);
        if (StringUtil.isBlank(trustIdpsConfig)) {
            logger.warnf("No trust-material IdP aliases configured for client: %s", client.getClientId());
            return null;
        }

        String algorithm = header != null ? (String) header.get(JWK.ALGORITHM) : null;
        String issuer = payload != null ? (String) payload.get(OAuth2Constants.ISSUER) : null;

        TrustMaterialRequest request = TrustMaterialRequest.builder()
                .kid(kid)
                .algorithm(algorithm)
                .issuer(issuer)
                .build();

        Optional<JWK> jwk = new TrustMaterialResolver().resolveKey(session, trustIdpsConfig, request);
        if (jwk.isEmpty()) {
            logger.debugf("Key with kid '%s' not found in configured trusted attester keys", kid);
        }

        return jwk.orElse(null);
    }
}
