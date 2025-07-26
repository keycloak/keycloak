package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.IDToken;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ResourceIndicatorMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, TokenIntrospectionTokenMapper, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "oidc-resource-indicator-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(ResourceIndicatorMapper.class);

    public static final String PREFIX_RESOURCE_ON_TOKEN_ENDPOINT = "_KC_ON_TOKEN_ENDPOINT_";

    public static final String PERMITTED_RESOURCES = "allow-permitted-resources";
    public static final String RESOURCES_LABEL = "Permitted resources";
    public static final String RESOURCES_HELP_TEXT = "If filled, then the executor only accept resource parameter whose value exactly match one of the permitted resources.";

    static {
        List<ProviderConfigProperty> props = ProviderConfigurationBuilder.create()
                .property()
                .name(PERMITTED_RESOURCES)
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .label(RESOURCES_LABEL)
                .helpText(RESOURCES_HELP_TEXT)
                .add()
                .build();

        configProperties.addAll(props);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ResourceIndicatorMapper.class);
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Resource Indicators";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds requested OAuth2 Resource Indicators to audience claim.";
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATOR);
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        if (clientSessionCtx == null || clientSessionCtx.getClientSession() == null) return;

        String resourceInAuthorizationRequest = clientSessionCtx.getAttribute(OAuth2Constants.RESOURCE, String.class);
        logger.debugv(" mapper: resource in authorization request = {0}", resourceInAuthorizationRequest);
        if (resourceInAuthorizationRequest == null) return;

        String resourceOnTokenEndpoint = clientSessionCtx.getAttribute(PREFIX_RESOURCE_ON_TOKEN_ENDPOINT + OAuth2Constants.RESOURCE, String.class);
        logger.debugv(" mapper: resource in token request = {0}", resourceOnTokenEndpoint);
        if (resourceOnTokenEndpoint != null && !resourceOnTokenEndpoint.equals(resourceInAuthorizationRequest)) return;

        String permittedResouresString = mappingModel.getConfig().get(PERMITTED_RESOURCES);
        if (permittedResouresString == null || permittedResouresString.isBlank()) return;
        if (Arrays.stream(permittedResouresString.split("##")).noneMatch(resourceInAuthorizationRequest::equals)) {
            logger.debugv("no match with any permitted resource: resource = ", resourceInAuthorizationRequest);
            return;
        }
        
        token.addAudience(resourceInAuthorizationRequest);
    }

    public static ProtocolMapperModel create(String name, boolean accessToken, boolean introspectionEndpoint, List<String> permittedResources) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        if (accessToken) {
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        }
        if (introspectionEndpoint) {
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        }
        if (permittedResources != null && !permittedResources.isEmpty()) {
            config.put(PERMITTED_RESOURCES, String.join("##", permittedResources));
        }
        mapper.setConfig(config);
        return mapper;
    }
}
