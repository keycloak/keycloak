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
 * Test client policy executor that fails on the {@link ClientPolicyEvent#UPDATED} event,
 * which is triggered after a client has been updated, so that tests can check that a failed update is not persisted.
 */
public class RejectClientUpdatedClientPolicyExecutor implements ClientPolicyExecutorProviderFactory, ClientPolicyExecutorProvider<RejectClientUpdatedClientPolicyExecutor.Configuration> {

    public static final String PROVIDER_ID = "reject-client-updated-client-policy-executor";
    public static final String ERROR_DETAIL = "Client update rejected by test executor";

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (context.getEvent() == ClientPolicyEvent.UPDATED) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, ERROR_DETAIL);
        }
    }

    @Override
    public RejectClientUpdatedClientPolicyExecutor create(KeycloakSession session) {
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
        return "Test Client Policy executor that rejects every client after it has been updated.";
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

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
    }
}
