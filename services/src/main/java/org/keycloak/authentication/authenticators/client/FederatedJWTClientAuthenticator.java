package org.keycloak.authentication.authenticators.client;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.broker.provider.ClientAssertionIdentityProvider;
import org.keycloak.broker.provider.ClientAssertionIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.resources.IdentityBrokerService;

import org.jboss.logging.Logger;

public class FederatedJWTClientAuthenticator extends AbstractClientAuthenticator implements EnvironmentDependentProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(FederatedJWTClientAuthenticator.class);

    public static final String PROVIDER_ID = "federated-jwt";

    public static final String JWT_CREDENTIAL_ISSUER_KEY = "jwt.credential.issuer";
    public static final String JWT_CREDENTIAL_SUBJECT_KEY = "jwt.credential.sub";

    private static final List<ProviderConfigProperty> CLIENT_CONFIG =
            ProviderConfigurationBuilder.create()
                    .property()
                    .name(JWT_CREDENTIAL_ISSUER_KEY)
                    .label("Identity provider")
                    .helpText("Issuer of the client assertion. Use the alias of an identity provider set up in this realm.")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .required(true)
                    .add()
                    .property().name(JWT_CREDENTIAL_SUBJECT_KEY)
                    .label("Federated subject")
                    .helpText("External clientId (subject) as provided by the identity provider.")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .required(true)
                    .add()
                    .build();

    private final List<ClientAssertionIdentityProviderFactory.ClientAssertionStrategy> strategies = new LinkedList<>();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.getProviderFactoriesStream(IdentityProvider.class)
                .filter(ClientAssertionIdentityProviderFactory.class::isInstance)
                .map(ClientAssertionIdentityProviderFactory.class::cast)
                .map(ClientAssertionIdentityProviderFactory::getClientAssertionStrategy)
                .filter(Objects::nonNull)
                .forEach(strategies::add);

        strategies.add(new DefaultClientAssertionStrategy());
    }

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        try {
            // Mark it as attempted for all items that return directly
            context.attempted();

            ClientAssertionState clientAssertionState = context.getState(ClientAssertionState.class, ClientAssertionState.supplier());

            if (clientAssertionState == null || clientAssertionState.getClientAssertionType() == null) {
                return;
            }

            ClientAssertionIdentityProviderFactory.ClientAssertionStrategy strategy = findStrategy(clientAssertionState.getClientAssertionType());
            if (strategy == null) {
                return;
            }

            ClientAssertionIdentityProviderFactory.LookupResult lookup = strategy.lookup(context);
            if (lookup == null || lookup.identityProviderModel() == null || lookup.clientModel() == null) {
                return;
            }

            ClientAssertionIdentityProvider<?> identityProvider = getClientAssertionIdentityProvider(context.getSession(), lookup.identityProviderModel());
            ClientModel client = lookup.clientModel();
            clientAssertionState.setClient(client);

            if (!PROVIDER_ID.equals(client.getClientAuthenticatorType())) return;

            if (identityProvider.verifyClientAssertion(context)) {
                context.success();
            } else {
                context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS);
            }
        } catch (Exception e) {
            LOGGER.warn("Authentication failed", e);
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS);
        }
    }

    private ClientAssertionIdentityProviderFactory.ClientAssertionStrategy findStrategy(String assertionType) {
        return strategies.stream().filter(c -> c.isSupportedAssertionType(assertionType)).findFirst().orElse(null);
    }

    private ClientAssertionIdentityProvider<?> getClientAssertionIdentityProvider(KeycloakSession session, IdentityProviderModel identityProviderModel) {
        if (identityProviderModel == null) {
            return null;
        }
        return IdentityBrokerService.getIdentityProvider(session, identityProviderModel, ClientAssertionIdentityProvider.class);
    }

    @Override
    public String getDisplayType() {
        return "Signed JWT - Federated";
    }

    @Override
    public String getHelpText() {
        return "Validates client based on signed JWT issued and signed by an external identity provider";
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return ConfigurableAuthenticatorFactory.REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        return CLIENT_CONFIG;
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        return Collections.emptyMap();
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        return Collections.emptySet();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_AUTH_FEDERATED);
    }

}
