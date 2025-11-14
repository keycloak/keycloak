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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AddressClaimSet;
import org.keycloak.representations.IDToken;

/**
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AddressMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String STREET = "street";

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AddressMapper.class);

        configProperties.add(createConfigProperty(STREET));
        configProperties.add(createConfigProperty(AddressClaimSet.LOCALITY));
        configProperties.add(createConfigProperty(AddressClaimSet.REGION));
        configProperties.add(createConfigProperty(AddressClaimSet.POSTAL_CODE));
        configProperties.add(createConfigProperty(AddressClaimSet.COUNTRY));
        configProperties.add(createConfigProperty(AddressClaimSet.FORMATTED));
    }

    protected static ProviderConfigProperty createConfigProperty(String claimName) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(getModelPropertyName(claimName));
        property.setLabel("addressClaim." + claimName + ".label");
        property.setHelpText("addressClaim." + claimName + ".tooltip");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(claimName);
        return property;
    }

    public static String getModelPropertyName(String claimName) {
        return "user.attribute." + claimName;
    }

    public static final String PROVIDER_ID = "oidc-address-mapper";

    public static ProtocolMapperModel createAddressMapper() {
        return createAddressMapper(true, true, true, true);
    }

    public static ProtocolMapperModel createAddressMapper(boolean idToken, boolean accessToken, boolean userInfo, boolean introspectionEndpoint) {
        Map<String, String> config;
        ProtocolMapperModel address = new ProtocolMapperModel();
        address.setName("address");
        address.setProtocolMapper(PROVIDER_ID);
        address.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.toString(accessToken));
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, Boolean.toString(idToken));
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, Boolean.toString(userInfo));
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, Boolean.toString(introspectionEndpoint));

        config.put(getModelPropertyName(STREET), STREET);
        config.put(getModelPropertyName(AddressClaimSet.LOCALITY), AddressClaimSet.LOCALITY);
        config.put(getModelPropertyName(AddressClaimSet.REGION), AddressClaimSet.REGION);
        config.put(getModelPropertyName(AddressClaimSet.POSTAL_CODE), AddressClaimSet.POSTAL_CODE);
        config.put(getModelPropertyName(AddressClaimSet.COUNTRY), AddressClaimSet.COUNTRY);
        config.put(getModelPropertyName(AddressClaimSet.FORMATTED), AddressClaimSet.FORMATTED);

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
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        UserModel user = userSession.getUser();
        AddressClaimSet addressSet = new AddressClaimSet();
        addressSet.setStreetAddress(getUserModelAttributeValue(user, mappingModel, STREET));
        addressSet.setLocality(getUserModelAttributeValue(user, mappingModel, AddressClaimSet.LOCALITY));
        addressSet.setRegion(getUserModelAttributeValue(user, mappingModel, AddressClaimSet.REGION));
        addressSet.setPostalCode(getUserModelAttributeValue(user, mappingModel, AddressClaimSet.POSTAL_CODE));
        addressSet.setCountry(getUserModelAttributeValue(user, mappingModel, AddressClaimSet.COUNTRY));
        addressSet.setFormattedAddress(getUserModelAttributeValue(user, mappingModel, AddressClaimSet.FORMATTED));
        token.getOtherClaims().put("address", addressSet);
    }

    private String getUserModelAttributeValue(UserModel user, ProtocolMapperModel mappingModel, String claim) {
        String modelPropertyName = getModelPropertyName(claim);
        String userAttrName = mappingModel.getConfig().get(modelPropertyName);

        if (userAttrName == null) {
            userAttrName = claim;
        }

        return user.getFirstAttribute(userAttrName);
    }

}
