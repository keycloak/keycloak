/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.protocol.oidc.mappers;

import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.common.Profile;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 * This protocol mapper populates the 'acr' claim in the OIDC tokens based on the authentication flow completed by the user
 * and the ACR to auth flow mappings configured for the initiating client.
 */
public class AcrFlowProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(AcrFlowProtocolMapper.class);

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AcrFlowProtocolMapper.class);
    }

    public static final String PROVIDER_ID = "oidc-acr-flow-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Authentication Context Class Reference (ACR) Flow Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps the successful authentication flow to the 'acr' claim of the token";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        String acr = getAcr(clientSession);
        token.setAcr(acr);
    }

    public static ProtocolMapperModel create(String name, boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        return mapper;
    }

    /**
     * Extract the completed flow from the clientSession notes and return the associated ACR value
     * @param clientSession The authenticated client session
     * @return The ACR value associated with the completed auth flow
     */
    protected String getAcr(AuthenticatedClientSessionModel clientSession) {
        List<String> acrValues = AcrUtils.getAcrValues(
                clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM),
                clientSession.getNote(OIDCLoginProtocol.ACR_PARAM), clientSession.getClient());

        Map<String, String> acrFlowMap = AcrUtils.getAcrFlowMap(clientSession.getClient());
        String completedFlow = AcrUtils.getCompletedFlowId(clientSession);

        String acr = AcrUtils.mapFlowToAcr(completedFlow, acrValues, acrFlowMap);

        logger.tracef("Authentication flow completed when authenticated to client %s: %d", clientSession.getClient().getClientId(), completedFlow);

        return acr;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
