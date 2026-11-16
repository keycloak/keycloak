package org.keycloak.tests.providers.client.policies;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProviderFactory;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;

/**
 * Test Client Policy condition satisfied by an authorization request that includes the {@code resource} parameter, so
 * that whether it is satisfied depends on the values of an actual request, not only on the client.
 */
public class ResourceParameterClientPolicyCondition implements ClientPolicyConditionProviderFactory,
        ClientPolicyConditionProvider<ClientPolicyConditionConfigurationRepresentation> {

    public static final String PROVIDER_ID = "resource-parameter-client-policy-condition";

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) {
        if (context instanceof PreAuthorizationRequestContext request) {
            return request.getRequestParameters().containsKey(OAuth2Constants.RESOURCE) ? ClientPolicyVote.YES : ClientPolicyVote.NO;
        }
        return ClientPolicyVote.ABSTAIN;
    }

    @Override
    public void setupConfiguration(ClientPolicyConditionConfigurationRepresentation config) {
    }

    @Override
    public boolean isNegativeLogic() {
        return false;
    }

    @Override
    public ResourceParameterClientPolicyCondition create(KeycloakSession session) {
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
        return "Test Client Policy condition satisfied by an authorization request that includes the resource parameter.";
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
