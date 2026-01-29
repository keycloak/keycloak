package org.keycloak.services.clientpolicy.executor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.TokenRequestContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureResourceIndicatorExecutor implements ClientPolicyExecutorProvider<SecureResourceIndicatorExecutor.Configuration> {

    protected final KeycloakSession session;
    protected Configuration configuration;

    private static final Logger logger = Logger.getLogger(SecureResourceIndicatorExecutor.class);

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty(SecureResourceIndicatorExecutorFactory.PERMITTED_RESOURCES)
        protected List<String> allowPermittedResources = null;

        public Configuration() {
        }

        public List<String> getAllowPermittedResources() {
            return allowPermittedResources;
        }

        public void setAllowPermittedResources(List<String> allowPermittedResources) {
            this.allowPermittedResources = allowPermittedResources;
        }
    }

    public static final String ERR_NOT_PERMITTED_RESOURCE = "not allowed resource parameter value";
    public static final String ERR_NO_RESOURCE_IN_TOKEN_REQUEST = "resource parameter value in token request does not exist.";
    public static final String ERR_DIFFERENT_RESOURCE = "resource parameter value in token request does not match the one in authorization request.";

    public SecureResourceIndicatorExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return ConfidentialClientAcceptExecutorFactory.PROVIDER_ID;
    }

    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (!Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATOR)) {
            logger.warnf("RESOURCE_INDICATOR feature is disabled. So the executor does not work. " +
                    "Please enable RESOURCE_INDICATOR feature in order to be able to have token audience binding with resource parameter applied.");
            return;
        }

        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
                AuthorizationRequestContext authzRequestContext = (AuthorizationRequestContext) context;
                String resourceParam = authzRequestContext.getAuthorizationEndpointRequest().getResource();
                logger.debugv(" on authz request: resourceParam = {0}", resourceParam);
                List<String> allowResourceList = convertContentFilledList(configuration.getAllowPermittedResources());
                if (allowResourceList != null && !allowResourceList.isEmpty() && !allowResourceList.contains(resourceParam)) {
                        logger.warnv("not allowed resource parameter value: resource = {0}", resourceParam);
                        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, ERR_NOT_PERMITTED_RESOURCE);
                }
                return;
            case TOKEN_REQUEST:
                checkResourceParameterValue((TokenRequestContext) context);
                return;
            default:
        }
    }

    /**
     * When the authorization request does not include resource parameter but the token request includes that,
     * the resource parameter in the token request is ignored, not set to audience claim in an access token.
     */
    private void checkResourceParameterValue(TokenRequestContext context) throws ClientPolicyException {
        String resourceInTokenRequest = context.getParams().getFirst(OAuth2Constants.RESOURCE);
        AuthenticatedClientSessionModel clientSession = context.getParseResult().getClientSession();
        if (clientSession == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "client session is null");
        }
        String resourceInAuthorizationRequest = clientSession.getNote(OAuth2Constants.RESOURCE);

        logger.debugv(" on token request: resourceInAuthorizationRequest = {0}", resourceInAuthorizationRequest);

        if (resourceInAuthorizationRequest == null) {
            return;
        }

        if (resourceInTokenRequest == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, ERR_NO_RESOURCE_IN_TOKEN_REQUEST);
        }

        if (!resourceInTokenRequest.equals(resourceInAuthorizationRequest)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, ERR_DIFFERENT_RESOURCE);
        }
    }

    protected List<String> convertContentFilledList(List<String> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().filter(Objects::nonNull).filter(i->!i.isBlank()).distinct().toList();
    }

}
