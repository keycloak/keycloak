package org.keycloak.tests.providers.client.policies;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

/**
 * Test executor that rejects only the TOKEN_EXCHANGE_RESPONSE event, so that tests can verify that
 * the client policies are triggered when the token exchange response is built.
 */
public class RejectTokenExchangeResponseExecutor implements ClientPolicyExecutorProviderFactory,
        ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    public static final String PROVIDER_ID = "reject-token-exchange-response-executor";
    public static final String ERROR_DESCRIPTION = "Token exchange response rejected by client policy";

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (context.getEvent() == ClientPolicyEvent.TOKEN_EXCHANGE_RESPONSE) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, ERROR_DESCRIPTION);
        }
    }

    @Override
    public RejectTokenExchangeResponseExecutor create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Test Client Policy executor that rejects only the token exchange response event.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}