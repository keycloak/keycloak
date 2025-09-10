package org.keycloak.authentication.authenticators.client;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.broker.provider.ClientAssertionIdentityProvider;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.spiffe.SpiffeConstants;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.resources.IdentityBrokerService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FederatedJWTClientAuthenticator extends AbstractClientAuthenticator implements EnvironmentDependentProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(FederatedJWTClientAuthenticator.class);

    public static final String PROVIDER_ID = "federated-jwt";

    public static final String JWT_CREDENTIAL_ISSUER_KEY = "jwt.credential.issuer";

    private static final List<ProviderConfigProperty> CONFIG = List.of(
            new ProviderConfigProperty(JWT_CREDENTIAL_ISSUER_KEY, "Identity provider", "Issuer of the client assertion", ProviderConfigProperty.STRING_TYPE, null)
    );

    private static final Set<String> SUPPORTED_ASSERTION_TYPES = Set.of(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT, SpiffeConstants.CLIENT_ASSERTION_TYPE);

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    // TODO Should share code with JWTClientAuthenticator/JWTClientValidator rather than duplicating, but that requires quite a bit of refactoring
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        try {
            ClientAssertionState clientAssertionState = context.getState(ClientAssertionState.class, ClientAssertionState.supplier());
            JsonWebToken token = clientAssertionState.getToken();

            if (!SUPPORTED_ASSERTION_TYPES.contains(clientAssertionState.getClientAssertionType())) {
                return;
            }

            ClientModel client = lookupClient(context, token.getSubject());
            if (client == null) return;

            if (!PROVIDER_ID.equals(client.getClientAuthenticatorType())) {
                return;
            }

            ClientAssertionIdentityProvider identityProvider = lookupIdentityProvider(context, client);
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

    private ClientModel lookupClient(ClientAuthenticationFlowContext context, String subject) {
        ClientModel client = context.getRealm().getClientByClientId(subject);
        if (client == null) {
            context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND);
            return null;
        }
        if (!client.isEnabled()) {
            context.failure(AuthenticationFlowError.CLIENT_DISABLED);
            return null;
        }
        return client;
    }

    private ClientAssertionIdentityProvider lookupIdentityProvider(ClientAuthenticationFlowContext context, ClientModel client) {
        String idpAlias = client.getAttribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY);
        IdentityProvider<?> identityProvider = IdentityBrokerService.getIdentityProvider(context.getSession(), idpAlias);
        if (identityProvider instanceof ClientAssertionIdentityProvider clientAssertionProvider) {
            return clientAssertionProvider;
        } else {
            throw new RuntimeException("Provider does not support client assertions");
        }
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
        return CONFIG;
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
