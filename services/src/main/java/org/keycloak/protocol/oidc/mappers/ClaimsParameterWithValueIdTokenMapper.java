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
 */

package org.keycloak.protocol.oidc.mappers;

import java.io.IOException;
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
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClaimsParameterWithValueIdTokenMapper extends AbstractOIDCProtocolMapper implements OIDCIDTokenMapper {

    private static final Logger LOGGER = Logger.getLogger(ClaimsParameterWithValueIdTokenMapper.class);

    public static final String PROVIDER_ID = "oidc-claims-param-value-idtoken-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String CLAIM_NAME = "claim.name";

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CLAIM_NAME);
        property.setLabel("Claim name");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Name of the claim you want to set its value. 'true' and 'false can be used for boolean values.");
        configProperties.add(property);

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ClaimsParameterWithValueIdTokenMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Claims parameter with value ID Token";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Claims specified by Claims parameter with value are put into an ID token.";
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
            putClaims(ClaimsRepresentation.ClaimContext.ID_TOKEN, claims, token, mappingModel, userSession);
        }
    }

    private void putClaims(ClaimsRepresentation.ClaimContext tokenType, String claims, IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        String claimName = mappingModel.getConfig().get(CLAIM_NAME);
        if (claimName == null) return;

        ClaimsRepresentation claimsRep = null;

        try {
            claimsRep = JsonSerialization.readValue(claims, ClaimsRepresentation.class);
        } catch (IOException e) {
            LOGGER.warn("Invalid claims parameter", e);
            return;
        }

        if (!claimsRep.isPresent(claimName, tokenType) || claimsRep.isPresentAsNullClaim(claimName, tokenType)) {
            return;
        }

        ClaimsRepresentation.ClaimValue<String> claimValue = claimsRep.getClaimValue(claimName, tokenType, String.class);
        if (!claimValue.isEssential()) {
            return;
        }

        String claim = claimValue.getValue();
        if (claim == null) {
            return;
        }

        HardcodedClaim hardcodedClaimMapper = new HardcodedClaim();
        hardcodedClaimMapper.setClaim(token, HardcodedClaim.create("hard", claimName, claim, "String", false, true, false), userSession);
    }

    public static ProtocolMapperModel createMapper(String name, String attributeValue, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<String, String>();
        config.put(CLAIM_NAME, attributeValue);
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        return mapper;
    }

}
