package org.keycloak.authentication.authenticators.client;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.broker.provider.ClientAssertionIdentityProvider;
import org.keycloak.broker.spiffe.SpiffeConstants;
import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.resources.IdentityBrokerService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FederatedJWTClientAuthenticator extends AbstractClientAuthenticator implements EnvironmentDependentProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(FederatedJWTClientAuthenticator.class);

    public static final String PROVIDER_ID = "federated-jwt";

    public static final String JWT_CREDENTIAL_ISSUER_KEY = "jwt.credential.issuer";
    public static final String JWT_CREDENTIAL_SUBJECT_KEY = "jwt.credential.sub";

    private static final List<ProviderConfigProperty> CLIENT_CONFIG = List.of(
            new ProviderConfigProperty(JWT_CREDENTIAL_ISSUER_KEY, "Identity provider", "Issuer of the client assertion", ProviderConfigProperty.STRING_TYPE, null),
            new ProviderConfigProperty(JWT_CREDENTIAL_SUBJECT_KEY, "Federated subject", "External clientId (subject)", ProviderConfigProperty.STRING_TYPE, null)
    );

    private static final Set<String> SUPPORTED_ASSERTION_TYPES = Set.of(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT, SpiffeConstants.CLIENT_ASSERTION_TYPE);

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        try {
            ClientAssertionState clientAssertionState = context.getState(ClientAssertionState.class, ClientAssertionState.supplier());

            if (clientAssertionState == null || clientAssertionState.getClientAssertionType() == null) {
                return;
            }

            if (!SUPPORTED_ASSERTION_TYPES.contains(clientAssertionState.getClientAssertionType())) {
                return;
            }

            AlternativeLookupProvider lookupProvider = context.getSession().getProvider(AlternativeLookupProvider.class);

            String federatedClientId = clientAssertionState.getToken().getSubject();

            ClientModel client = lookupProvider.lookupClientFromClientAttributes(
                    context.getSession(),
                    Map.of(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, federatedClientId));
            if (client == null) return;

            String idpAlias = client.getAttribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY);

            IdentityProviderModel identityProviderModel = context.getSession().identityProviders().getByAlias(idpAlias);
            ClientAssertionIdentityProvider identityProvider = getClientAssertionIdentityProvider(context.getSession(), identityProviderModel);
            if (identityProvider == null) return;

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

    private ClientAssertionIdentityProvider getClientAssertionIdentityProvider(KeycloakSession session, IdentityProviderModel identityProviderModel) {
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
