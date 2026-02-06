package org.keycloak.tests.admin.client.policies;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProviderFactory;

public class YesClientPolicyCondition implements ClientPolicyConditionProvider<YesClientPolicyCondition.Configuration>, ClientPolicyConditionProviderFactory {
    public static final String PROVIDER_ID = "yes-client-policy-condition";
    private static final YesClientPolicyCondition SINGLETON = new YesClientPolicyCondition();

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) {
        return ClientPolicyVote.YES;
    }

    @Override
    public Class<YesClientPolicyCondition.Configuration> getConditionConfigurationClass() {
        return YesClientPolicyCondition.Configuration.class;
    }

    @Override
    public ClientPolicyConditionProvider<YesClientPolicyCondition.Configuration> create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public String getHelpText() {
        return "Client policy condition that always returns YES";
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

    @Override
    public void setupConfiguration(Configuration configuration) {

    }

    @Override
    public boolean isNegativeLogic() {
        return false;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

    }
}
