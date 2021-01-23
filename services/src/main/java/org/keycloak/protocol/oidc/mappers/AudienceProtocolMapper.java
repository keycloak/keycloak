/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AudienceProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String INCLUDED_CLIENT_AUDIENCE = "included.client.audience";
    private static final String INCLUDED_CLIENT_AUDIENCE_LABEL = "included.client.audience.label";
    private static final String INCLUDED_CLIENT_AUDIENCE_HELP_TEXT = "included.client.audience.tooltip";

    private static final String INCLUDED_CUSTOM_AUDIENCE = "included.custom.audience";
    private static final String INCLUDED_CUSTOM_AUDIENCE_LABEL = "included.custom.audience.label";
    private static final String INCLUDED_CUSTOM_AUDIENCE_HELP_TEXT = "included.custom.audience.tooltip";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(INCLUDED_CLIENT_AUDIENCE);
        property.setLabel(INCLUDED_CLIENT_AUDIENCE_LABEL);
        property.setHelpText(INCLUDED_CLIENT_AUDIENCE_HELP_TEXT);
        property.setType(ProviderConfigProperty.CLIENT_LIST_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(INCLUDED_CUSTOM_AUDIENCE);
        property.setLabel(INCLUDED_CUSTOM_AUDIENCE_LABEL);
        property.setHelpText(INCLUDED_CUSTOM_AUDIENCE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);


        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AudienceProtocolMapper.class);

        // Don't include audience in ID Token by default
        for (ProviderConfigProperty prop : configProperties) {
            if (OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN.equals(prop.getName())) {
                prop.setDefaultValue("false");
            }
        }
    }

    public static final String PROVIDER_ID = "oidc-audience-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Audience";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Add specified audience to the audience (aud) field of token";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        String audienceValue = mappingModel.getConfig().get(INCLUDED_CLIENT_AUDIENCE);

        if (audienceValue == null) {
            // Fallback to custom audience
            audienceValue = mappingModel.getConfig().get(INCLUDED_CUSTOM_AUDIENCE);
        }

        if (audienceValue == null) return;
        token.addAudience(audienceValue);
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String includedClientAudience,
                                                        String includedCustomAudience,
                                                        boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Map<String, String> config = new HashMap<>();
        if (includedClientAudience != null) {
            config.put(INCLUDED_CLIENT_AUDIENCE, includedClientAudience);
        }
        if (includedCustomAudience != null) {
            config.put(INCLUDED_CUSTOM_AUDIENCE, includedCustomAudience);
        }

        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        return mapper;
    }
}
