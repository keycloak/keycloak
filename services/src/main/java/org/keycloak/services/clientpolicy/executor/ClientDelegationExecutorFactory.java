package org.keycloak.services.clientpolicy.executor;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.tokenexchange.DelegationChain;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class ClientDelegationExecutorFactory implements ClientPolicyExecutorProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "client-delegation";

    public static final String ALLOW_CHAINING = "allow-chaining";

    @Override
    public String getHelpText() {
        return "It decides whether a client may continue a token exchange delegation chain. Whether a client may act on behalf of a user at all stays with the 'delegate' fine-grained admin permission, which is always checked and cannot be waived here. A chain never holds more than " + DelegationChain.MAX_CHAIN_DEPTH + " actors, which is not configurable.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()

                .property()
                .name(ALLOW_CHAINING)
                .label("Allow chaining")
                .helpText("If ON, a token this client received by delegation can be exchanged again, passing the delegation on to a further client. The default value is false, so a delegation ends at the first hop by default. When several policies match the same request, chaining is allowed only if all of them allow it.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()

                .build();
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new ClientDelegationExecutor(session);
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
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Feature.TOKEN_EXCHANGE_DELEGATION);
    }
}
