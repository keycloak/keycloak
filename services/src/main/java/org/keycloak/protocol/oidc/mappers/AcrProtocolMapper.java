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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AcrProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(AcrProtocolMapper.class);

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AcrProtocolMapper.class);
    }

    public static final String PROVIDER_ID = "oidc-acr-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Authentication Context Class Reference (ACR)";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps the achieved LoA (Level of Authentication) to the 'acr' claim of the token";
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

    protected String getAcr(AuthenticatedClientSessionModel clientSession) {
        int loa = LoAUtil.getCurrentLevelOfAuthentication(clientSession);
        logger.tracef("Loa level when authenticated to client %s: %d", clientSession.getClient().getClientId(), loa);
        if (loa < Constants.MINIMUM_LOA) {
            loa = AuthenticationManager.isSSOAuthentication(clientSession) ? 0 : 1;
        }

        Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(clientSession.getClient());
        String acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, AcrUtils.getRequiredAcrValues(
                clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM)));
        if (acr == null) {
            acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, AcrUtils.getAcrValues(
                    clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM),
                    clientSession.getNote(OIDCLoginProtocol.ACR_PARAM), clientSession.getClient()));
            if (acr == null) {
                acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, acrLoaMap.keySet());
                if (acr == null) {
                    acr = String.valueOf(loa);
                }
            }
        }

        logger.tracef("Level sent in the token to client %s: %s. Original loa from the authentication: %d", clientSession.getClient().getClientId(), acr, loa);
        return acr;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.STEP_UP_AUTHENTICATION);
    }
}
