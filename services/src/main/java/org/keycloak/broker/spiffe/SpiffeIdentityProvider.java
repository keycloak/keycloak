package org.keycloak.broker.spiffe;

import java.nio.charset.StandardCharsets;

import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.authenticators.client.AbstractJWTClientValidator;
import org.keycloak.authentication.authenticators.client.FederatedJWTClientValidator;
import org.keycloak.broker.provider.ClientAssertionIdentityProvider;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;

import org.jboss.logging.Logger;

/**
 * Implementation for https://datatracker.ietf.org/doc/draft-schwenkschuster-oauth-spiffe-client-auth/
 *
 * Main differences for SPIFFE JWT SVIDs and regular client assertions:
 * <ul>
*  <li><code>jwt-spiffe</code> client assertion type</li>
 * <li><code>iss</code> claim is optional, uses SPIFFE IDs, which includes trust domain instead</li>
 * <li><code>jti</code> claim is optional, and SPIFFE vendors re-use/cache tokens</li>
 * <li><code>sub</code> is a SPIFFE ID with the syntax <code>spiffe://trust-domain/workload-identity</code></li>
 * <li>Keys are fetched from a SPIFFE bundle endpoint, where the JWKS has additional SPIFFE specific fields (<code>spiffe_sequence</code> and <code>spiffe_refresh_hint</code>, the JWK does not set the <code>alg></code></li>
 * </ul>
 */
public class SpiffeIdentityProvider implements ClientAssertionIdentityProvider<SpiffeIdentityProviderConfig> {

    private static final Logger LOGGER = Logger.getLogger(SpiffeIdentityProvider.class);

    private final KeycloakSession session;
    private final SpiffeIdentityProviderConfig config;

    public SpiffeIdentityProvider(KeycloakSession session, SpiffeIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public SpiffeIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public boolean verifyClientAssertion(ClientAuthenticationFlowContext context) throws Exception {
        FederatedJWTClientValidator validator = new FederatedJWTClientValidator(context, this::verifySignature,
                    null, config.getAllowedClockSkew(), true);
        validator.setExpectedClientAssertionType(SpiffeConstants.CLIENT_ASSERTION_TYPE);

        String trustedDomain = config.getTrustDomain();

        JsonWebToken token = validator.getState().getToken();
        if (!token.getSubject().startsWith(trustedDomain + "/")) {
            throw new RuntimeException("Invalid trust-domain");
        }

        return validator.validate();
    }

    private boolean verifySignature(AbstractJWTClientValidator validator) {
        try {
            String bundleEndpoint = config.getBundleEndpoint();
            JWSInput jws = validator.getState().getJws();
            JWSHeader header = jws.getHeader();
            String kid = header.getKeyId();
            String alg = header.getRawAlgorithm();

            String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(validator.getContext().getRealm().getId(), config.getInternalId());

            PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
            KeyWrapper publicKey = keyStorage.getPublicKey(modelKey, kid, alg, new SpiffeBundleEndpointLoader(session, bundleEndpoint));

            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, alg);
            if (signatureProvider == null) {
                LOGGER.debugf("Failed to verify token, signature provider not found for algorithm %s", alg);
                return false;
            }

            return signatureProvider.verifier(publicKey).verify(jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), jws.getSignature());
        } catch (Exception e) {
            LOGGER.debug("Failed to verify token signature", e);
            return false;
        }
    }

    @Override
    public void close() {
    }

}
