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
import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.resources.IdentityBrokerService;

import java.net.URI;
import java.net.URISyntaxException;
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

            final String issuer = clientAssertionState.getToken().getIssuer() != null ?
                    clientAssertionState.getToken().getIssuer() :
                    toIssuer(clientAssertionState.getToken().getSubject());
            if (issuer == null) return;

            AlternativeLookupProvider lookupProvider = context.getSession().getProvider(AlternativeLookupProvider.class);

            IdentityProviderModel identityProviderModel = lookupProvider.lookupIdentityProviderFromIssuer(context.getSession(), issuer);
            ClientAssertionIdentityProvider identityProvider = getClientAssertionIdentityProvider(context.getSession(), identityProviderModel);
            if (identityProvider == null) return;

            String federatedClientId = clientAssertionState.getToken().getSubject();

            ClientModel client = lookupProvider.lookupClientFromClientAttributes(
                    context.getSession(),
                    Map.of(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, identityProviderModel.getAlias(), FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, federatedClientId));
            if (client == null) return;

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
        IdentityProvider<?> identityProvider = IdentityBrokerService.getIdentityProvider(session, identityProviderModel);
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

    protected static String toIssuer(String subject) {
        try {
            URI uri = new URI(subject);
            String scheme = uri.getScheme();
            String authority = uri.getRawAuthority();
            return scheme != null && authority != null ? scheme + "://" + authority : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

}
