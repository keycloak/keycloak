/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.ClaimType;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn.ID_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn.USERINFO;

public class ClaimsParameterTokenMapper extends AbstractOIDCProtocolMapper implements OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "oidc-claims-param-token-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ClaimsParameterTokenMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Claims parameter Token";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Claims specified by Claims parameter are put into tokens.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        String claims = clientSessionCtx.getClientSession().getNote(OIDCLoginProtocol.CLAIMS_PARAM);
        if (claims == null) return;

        if (TokenUtil.TOKEN_TYPE_ID.equals(token.getType())) {
            // ID Token
            putClaims("id_token", claims, token, mappingModel, userSession);
        } else {
            // UserInfo
            putClaims("userinfo", claims, token, mappingModel, userSession);
        }
    }

    private void putClaims(String tokenType, String claims, IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        JsonNode requestParams = null;

        try {
            requestParams = JsonSerialization.readValue(claims, JsonNode.class);
        } catch (IOException e) {
            return;
        }
        if (!requestParams.has(tokenType)) return;

        JsonNode tokenNode = requestParams.findValue(tokenType);

        OIDCWellKnownProvider.DEFAULT_CLAIMS_SUPPORTED.stream()
            .filter(i->tokenNode.has(i))
            .filter(i->tokenNode.findValue(i).has("essential"))
            .filter(i->tokenNode.findValue(i).findValue("essential").isBoolean())
            .filter(i->tokenNode.findValue(i).findValue("essential").asBoolean())
            .forEach(i -> {
                    // insert claim to Token
                    // "aud", "sub", "iss", "auth_time", "acr" are set as default.
                    // "name", "given_name", "family_name", "preferred_username", "email" need to be set explicitly using existing mapper.
                    if (i.equals(IDToken.NAME)) {
                        FullNameMapper fullNameMapper = new FullNameMapper();
                        fullNameMapper.setClaim(token, mappingModel, userSession);
                    } else if (i.equals(IDToken.GIVEN_NAME)) {
                        UserAttributeMapper userPropertyMapper = new UserAttributeMapper();
                        userPropertyMapper.setClaim(token, UserAttributeMapper.builder("requested firstName")
                                .userAttribute("firstName").claimName(IDToken.GIVEN_NAME).type(ClaimType.STRING)
                                .includeIn(ID_TOKEN, USERINFO).build(), userSession);
                    } else if (i.equals(IDToken.FAMILY_NAME)) {
                        UserAttributeMapper userPropertyMapper = new UserAttributeMapper();
                        userPropertyMapper.setClaim(token, UserAttributeMapper.builder("requested lastName")
                                .userAttribute("lastName").claimName(IDToken.FAMILY_NAME).type(ClaimType.STRING)
                                .includeIn(ID_TOKEN, USERINFO).build(), userSession);
                    } else if (i.equals(IDToken.PREFERRED_USERNAME)) {
                        UserAttributeMapper userPropertyMapper = new UserAttributeMapper();
                        userPropertyMapper.setClaim(token, UserAttributeMapper.builder("requested username")
                                .userAttribute("username").claimName(IDToken.PREFERRED_USERNAME).type(ClaimType.STRING)
                                .includeIn(ID_TOKEN, USERINFO).build(), userSession);
                    } else if (i.equals(IDToken.EMAIL)) {
                        UserAttributeMapper userPropertyMapper = new UserAttributeMapper();
                        userPropertyMapper.setClaim(token, UserAttributeMapper.builder("requested email")
                                .userAttribute("email").claimName(IDToken.EMAIL).type(ClaimType.STRING)
                                .includeIn(ID_TOKEN, USERINFO).build(), userSession);
                    }
            });
    }

    public static OIDCProtocolMapperBuilder<?> builder(String name) {
        return OIDCProtocolMapperBuilder.builder(name, PROVIDER_ID);
    }

}
