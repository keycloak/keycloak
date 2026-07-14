/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
import java.util.List;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

/**
 * Mappings UserSessionModel.note to an ID Token claim.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionNoteMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, OIDCAccessTokenResponseMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_SESSION_NOTE);
        property.setLabel(ProtocolMapperUtils.USER_SESSION_MODEL_NOTE_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_SESSION_MODEL_NOTE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        configProperties.add(property);
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, UserSessionNoteMapper.class);
    }

    public static final String PROVIDER_ID = "oidc-usersessionmodel-note-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Session Note";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a custom user session note to a token claim.";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {

        String noteName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_SESSION_NOTE);
        String noteValue = userSession.getNote(noteName);
        if (noteValue == null) return;
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, noteValue);
    }

    @Override
    protected void setClaim(AccessTokenResponse accessTokenResponse, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {

        String noteName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_SESSION_NOTE);
        String noteValue = userSession.getNote(noteName);
        if (noteValue == null) return;
        OIDCAttributeMapperHelper.mapClaim(accessTokenResponse, mappingModel, noteValue);
    }

    public static class Builder extends OIDCProtocolMapperBuilder<Builder> {
        private Builder(String name, String userSessionNote) {
            super(name, PROVIDER_ID);
            config(ProtocolMapperUtils.USER_SESSION_NOTE, userSessionNote);
        }
    }

    public static Builder builder(String name, String userSessionNote) {
        return new Builder(name, userSessionNote);
    }

}
