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

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mappings AuthenticatedClientSessionModel.note to an claim.
 */
public class ClientSessionNoteMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, OIDCAccessTokenResponseMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.CLIENT_SESSION_NOTE);
        property.setLabel(ProtocolMapperUtils.CLIENT_SESSION_MODEL_NOTE_LABEL);
        property.setHelpText(ProtocolMapperUtils.CLIENT_SESSION_MODEL_NOTE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, ClientSessionNoteMapper.class);
    }

    public static final String PROVIDER_ID = "oidc-clientsessionmodel-note-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Client Session Note";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a custom client session note to a token claim.";
    }

    @Override
    protected void setClaim(AccessTokenResponse accessTokenResponse, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {

        String noteName = mappingModel.getConfig().get(ProtocolMapperUtils.CLIENT_SESSION_NOTE);
        String noteValue = clientSessionCtx.getClientSession().getNote(noteName);
        if (noteValue == null) return;
        OIDCAttributeMapperHelper.mapClaim(accessTokenResponse, mappingModel, noteValue);
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String clientSessionNote,
                                                        String tokenClaimName, String jsonType,
                                                        boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return createClaimMapper(name, clientSessionNote, tokenClaimName, jsonType, accessToken, idToken, false, introspectionEndpoint);
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String clientSessionNote,
                                                        String tokenClaimName, String jsonType,
                                                        boolean accessToken, boolean idToken, boolean userInfo, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(ProtocolMapperUtils.CLIENT_SESSION_NOTE, clientSessionNote);
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, tokenClaimName);
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, jsonType);
        if (accessToken) {
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        }
        if (idToken) {
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        }
        if (userInfo) {
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        }
        if (introspectionEndpoint) {
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        }
        mapper.setConfig(config);
        return mapper;
    }
}
