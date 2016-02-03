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

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AddressClaimSet;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set the 'name' claim to be first + last name.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AddressMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN);
        property.setLabel(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_LABEL);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_HELP_TEXT);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        property.setLabel(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_LABEL);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-address-mapper";

    public static ProtocolMapperModel createAddressMapper() {
        Map<String, String> config;
        ProtocolMapperModel address = new ProtocolMapperModel();
        address.setName("address");
        address.setProtocolMapper(PROVIDER_ID);
        address.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        address.setConsentRequired(true);
        address.setConsentText("${address}");
        config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        address.setConfig(config);
        return address;
    }
    public static ProtocolMapperModel createAddressMapper(boolean idToken, boolean accessToken) {
        Map<String, String> config;
        ProtocolMapperModel address = new ProtocolMapperModel();
        address.setName("address");
        address.setProtocolMapper(PROVIDER_ID);
        address.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        address.setConsentRequired(true);
        address.setConsentText("${address}");
        config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.toString(idToken));
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, Boolean.toString(accessToken));
        address.setConfig(config);
        return address;
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
        return "User Address";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps user address attributes (street, locality, region, postal_code, and country) to the OpenID Connect 'address' claim.";
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) return token;
        setClaim(token, userSession);
        return token;
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) return token;
        setClaim(token, userSession);
        return token;
    }

    protected void setClaim(IDToken token, UserSessionModel userSession) {
        UserModel user = userSession.getUser();
        AddressClaimSet addressSet = new AddressClaimSet();
        addressSet.setStreetAddress(user.getFirstAttribute("street"));
        addressSet.setLocality(user.getFirstAttribute("locality"));
        addressSet.setRegion(user.getFirstAttribute("region"));
        addressSet.setPostalCode(user.getFirstAttribute("postal_code"));
        addressSet.setCountry(user.getFirstAttribute("country"));
        token.getOtherClaims().put("address", addressSet);
    }

}
