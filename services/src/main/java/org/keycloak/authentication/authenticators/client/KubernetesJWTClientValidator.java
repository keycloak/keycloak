package org.keycloak.authentication.authenticators.client;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.services.ServicesLogger;
import org.keycloak.utils.StringUtil;

/**
 * Validator for Kubernetes Signed JWTs using the Kubernetes JWKS endpoint.
 *
 * @author <a href="mailto:sebastian.laskawiec@defenseunicorns.com></a>
 */
public class KubernetesJWTClientValidator extends JWTClientValidator {

    public KubernetesJWTClientValidator(ClientAuthenticationFlowContext context, String clientAuthenticatorProviderId) {
        super(context, clientAuthenticatorProviderId);
    }

    /**
     * Kubernetes Signed JWTs shouldn't be checked against reuse. They are typically mounted into the Pod and depending
     * on the workload application can be cached and reused or not.
     */
    @Override
    protected boolean isTokenReuseCheckRequired() {
        return false;
    }

    @Override
    public boolean validateClient() {
        String clientId = params.getFirst(OAuth2Constants.CLIENT_ID);
        if (clientId == null) {
            throw new RuntimeException("Can't identify client. Missing client_id parameter");
        }

        context.getEvent().client(clientId);
        client = realm.getClientByClientId(clientId);

        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientModel(client);
        if (StringUtil.isNotBlank(config.getJwksUrl()) && !KubernetesJWTConstants.KUBERNETES_JWKS_URL.equals(config.getJwksUrl())) {
            // In this case we don't want to disclose any error to the client but instead, we need to log it.
            ServicesLogger.LOGGER.incorrectKubernetesJWKSURL(config.getJwksUrl(), KubernetesJWTConstants.KUBERNETES_JWKS_URL);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, null);
            return false;
        }

        if (client == null) {
            context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
            return false;
        } else {
            context.setClient(client);
        }

        if (!client.isEnabled()) {
            context.failure(AuthenticationFlowError.CLIENT_DISABLED, null);
            return false;
        }

        if (!clientAuthenticatorProviderId.equals(client.getClientAuthenticatorType())) {
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, null);
            return false;
        }

        return true;
    }
}
