package org.keycloak.broker.kubernetes;

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

import org.jboss.logging.Logger;

public class KubernetesIdentityProvider implements ClientAssertionIdentityProvider<KubernetesIdentityProviderConfig> {

    private static final Logger LOGGER = Logger.getLogger(KubernetesIdentityProvider.class);

    private final KeycloakSession session;
    private final KubernetesIdentityProviderConfig config;

    public KubernetesIdentityProvider(KeycloakSession session, KubernetesIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public boolean verifyClientAssertion(ClientAuthenticationFlowContext context) throws Exception {
        FederatedJWTClientValidator validator = new FederatedJWTClientValidator(context, this::verifySignature, config.getIssuer(), config.getAllowedClockSkew(), true);
        validator.setMaximumExpirationTime(3600); // Kubernetes defaults to 1 hour (https://kubernetes.io/docs/concepts/storage/projected-volumes/#serviceaccounttoken)
        return validator.validate();
    }

    private boolean verifySignature(AbstractJWTClientValidator validator) {
        try {
            JWSInput jws = validator.getState().getJws();
            JWSHeader header = jws.getHeader();
            String kid = header.getKeyId();
            String alg = header.getRawAlgorithm();

            String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(validator.getContext().getRealm().getId(), config.getInternalId());
            PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
            KeyWrapper publicKey = keyStorage.getPublicKey(modelKey, kid, alg, new KubernetesJwksEndpointLoader(session, config.getIssuer()));

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
    public KubernetesIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public void close() {

    }
}
