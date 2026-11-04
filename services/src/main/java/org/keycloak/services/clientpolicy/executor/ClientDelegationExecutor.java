package org.keycloak.services.clientpolicy.executor;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenExchangeDelegationRequestContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Decides whether a client may continue a delegation chain. The token exchange delegation provider enforces the
 * outcome, as chaining is default-deny and an executor can only veto, never grant.
 */
public class ClientDelegationExecutor implements ClientPolicyExecutorProvider<ClientDelegationExecutor.Configuration> {

    protected final KeycloakSession session;
    private Configuration configuration;

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty(ClientDelegationExecutorFactory.ALLOW_CHAINING)
        protected Boolean allowChaining;

        public Boolean isAllowChaining() {
            return allowChaining;
        }

        public void setAllowChaining(Boolean allowChaining) {
            this.allowChaining = allowChaining;
        }
    }

    public ClientDelegationExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return ClientDelegationExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config != null ? config : new Configuration();
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (context instanceof TokenExchangeDelegationRequestContext delegationContext) {
            delegationContext.restrictChainingAllowed(Boolean.TRUE.equals(configuration.isAllowChaining()));
        }
    }
}
