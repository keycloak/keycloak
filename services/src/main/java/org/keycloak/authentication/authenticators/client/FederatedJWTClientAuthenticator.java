package org.keycloak.authentication.authenticators.client;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.broker.provider.ClientAssertionContext;
import org.keycloak.broker.provider.ClientAssertionIdentityProvider;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.JWSInput;
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

    private static final Set<String> SUPPORTED_ASSERTION_TYPES = Set.of(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    // TODO Should share code with JWTClientAuthenticator/JWTClientValidator rather than duplicating, but that requires quite a bit of refactoring
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        try {
            MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();
            String clientAssertionType = params.getFirst(OAuth2Constants.CLIENT_ASSERTION_TYPE);
            if (!SUPPORTED_ASSERTION_TYPES.contains(clientAssertionType)) {
                return;
            }

            String clientAssertion = params.getFirst(OAuth2Constants.CLIENT_ASSERTION);
            if (clientAssertion == null) {
                return;
            }

            JWSInput jws = new JWSInput(clientAssertion);
            JsonWebToken token = jws.readJsonContent(JsonWebToken.class);
            context.getEvent().detail(Details.CLIENT_ASSERTION_ID, token.getId());
            context.getEvent().detail(Details.CLIENT_ASSERTION_ISSUER, token.getIssuer());

            ClientModel client = lookupClient(context, token.getSubject());
            if (client == null) return;

            ClientAssertionIdentityProvider identityProvider = lookupIdentityProvider(context, client);

            ClientAssertionContext clientAssertionContext = new DefaultClientAssertionContext(client, clientAssertionType, jws, token);
            if (identityProvider.verifyClientAssertion(clientAssertionContext)) {
                context.success();
            } else {
                LOGGER.warnv("Failed to authenticate client: {0}", clientAssertionContext.getError());
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
        context.setClient(client);
        context.getEvent().client(client);
        context.getEvent().detail(Details.CLIENT_ASSERTION_SUB, subject);
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

    private static class DefaultClientAssertionContext implements ClientAssertionContext {

        private final ClientModel client;
        private final String assertionType;
        private final JWSInput jwsInput;
        private final JsonWebToken token;
        private String error;

        public DefaultClientAssertionContext(ClientModel client, String assertionType, JWSInput jwsInput, JsonWebToken token) {
            this.client = client;
            this.assertionType = assertionType;
            this.jwsInput = jwsInput;
            this.token = token;
        }

        @Override
        public String getAssertionType() {
            return assertionType;
        }

        @Override
        public JWSInput getJwsInput() {
            return jwsInput;
        }

        @Override
        public JsonWebToken getToken() {
            return token;
        }

        @Override
        public ClientModel getClient() {
            return client;
        }

        @Override
        public boolean isFailure() {
            return error != null;
        }

        @Override
        public String getError() {
            return error;
        }

        @Override
        public boolean failure(String error) {
            this.error = error;
            return false;
        }

    }

}
