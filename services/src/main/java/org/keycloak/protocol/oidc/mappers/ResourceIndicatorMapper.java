/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oidc.mappers;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oauth2.ResourceIndicators;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds the allowed requested resources to the {@code aud} claim of an access token.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ResourceIndicatorMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, TokenIntrospectionTokenMapper {

    public static final String PROVIDER_ID = "oidc-resource-indicator-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String RESOURCES_PROPERTY = "resourceIndicators";

    public static final String RESOURCES_LABEL = "resourceIndicators.label";

    public static final String RESOURCES_HELP_TEXT = "resourceIndicators.tooltip";

    public static final String ERROR_INVALID_RESOURCE_INDICATOR = "invalidResourceIndicatorError";

    public static final String ERROR_INVALID_RESOURCE_INDICATOR_URI = "invalidResourceIndicatorUriError";

    public static final String ERROR_INVALID_RESOURCE_INDICATOR_FRAGMENT = "invalidResourceIndicatorFragmentError";

    static {
        List<ProviderConfigProperty> props = ProviderConfigurationBuilder.create()
                .property()
                .name(RESOURCES_PROPERTY)
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
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {

        Set<?> resourceIndicators = clientSessionCtx.getAttribute(OAuth2Constants.RESOURCE, Set.class);
        if (resourceIndicators == null || resourceIndicators.isEmpty()) {
            return;
        }

        for (Object resourceIndicator : resourceIndicators) {
            token.addAudience(String.valueOf(resourceIndicator));
        }
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {

        Map<String, String> config = mapperModel.getConfig();
        if (config == null || config.isEmpty()) {
            return;
        }

        String allowedResourceIndicators = config.get(RESOURCES_PROPERTY);
        if (allowedResourceIndicators != null) {
            for (String resourceIndicator : Constants.CFG_DELIMITER_PATTERN.split(allowedResourceIndicators)) {

                if (resourceIndicator.contains("#")) {
                    throw new ProtocolMapperConfigException(ERROR_INVALID_RESOURCE_INDICATOR_FRAGMENT, ERROR_INVALID_RESOURCE_INDICATOR, resourceIndicator);
                }

                if (!ResourceIndicators.isValidResourceIndicatorFormat(resourceIndicator)) {
                    throw new ProtocolMapperConfigException(ERROR_INVALID_RESOURCE_INDICATOR_URI, ERROR_INVALID_RESOURCE_INDICATOR, resourceIndicator);
                }
            }
        }
    }

    public static ProtocolMapperModel create(String name, boolean accessToken, boolean introspectionEndpoint) {
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
        mapper.setConfig(config);
        return mapper;
    }

}
