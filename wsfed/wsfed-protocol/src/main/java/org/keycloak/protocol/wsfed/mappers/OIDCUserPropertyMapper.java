/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.wsfed.mappers;

import org.keycloak.protocol.wsfed.WSFedLoginProtocol;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;

import java.util.ArrayList;
import java.util.List;

public class OIDCUserPropertyMapper extends AbstractWsfedProtocolMapper implements WSFedOIDCAccessTokenMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        UserPropertyMapper mapper = new UserPropertyMapper();
        configProperties.addAll(mapper.getConfigProperties());
    }

    public static final String PROVIDER_ID = "wsfed-oidc-usermodel-property-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "OIDC User Property";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a built in user property to a token claim.";
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        UserPropertyMapper mapper = new UserPropertyMapper();
        return mapper.transformAccessToken(token, mappingModel, session, userSession, clientSession);
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String userAttribute,
                                                        String tokenClaimName, String claimType,
                                                        boolean consentRequired, String consentText,
                                                        boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper =  UserPropertyMapper.createClaimMapper(name, userAttribute,
                                                                                        tokenClaimName, claimType,
                                                                                        consentRequired, consentText,
                                                                                        accessToken, idToken);

        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(WSFedLoginProtocol.LOGIN_PROTOCOL);

        return mapper;
    }


}