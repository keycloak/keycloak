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
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

/**
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HardcodedClaim extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper,
        OIDCAccessTokenResponseMapper, TokenIntrospectionTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String CLAIM_VALUE = "claim.value";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);

        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CLAIM_VALUE);
        property.setLabel("Claim value");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Value of the claim you want to hard code.  'true' and 'false can be used for boolean values.");
        configProperties.add(property);

        OIDCAttributeMapperHelper.addJsonTypeConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, HardcodedClaim.class);
    }

    public static final String PROVIDER_ID = "oidc-hardcoded-claim-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded claim";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode a claim into the token.";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {

        String attributeValue = mappingModel.getConfig().get(CLAIM_VALUE);
        if (attributeValue == null) return;
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeValue);
    }

    @Override
    protected void setClaim(AccessTokenResponse accessTokenResponse, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {

        String attributeValue = mappingModel.getConfig().get(CLAIM_VALUE);
        if (attributeValue == null) return;
        OIDCAttributeMapperHelper.mapClaim(accessTokenResponse, mappingModel, attributeValue);
    }

    public static class Builder extends OIDCProtocolMapperBuilder<Builder> {
        private Builder(String name, String claimName, String claimValue) {
            super(name, PROVIDER_ID);
            claimName(claimName);
            config(CLAIM_VALUE, claimValue);
        }
    }

    public static Builder builder(String name, String claimName, String claimValue) {
        return new Builder(name, claimName, claimValue);
    }

}
