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

package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import org.apache.commons.collections4.ListUtils;

/**
 * Sets an ID for the credential, either randomly generated or statically configured
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCSubjectIdMapper extends OID4VCMapper {

    public static final String MAPPER_ID = "oid4vc-subject-id-mapper";

    public static final String CLAIM_NAME_ID = "id";
    public static final String USER_ATTRIBUTE_DID = "did";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty idPropertyNameConfig = new ProviderConfigProperty();
        idPropertyNameConfig.setName(CLAIM_NAME);
        idPropertyNameConfig.setLabel("ID Property Name");
        idPropertyNameConfig.setHelpText("Name of the property to contain the id.");
        idPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(idPropertyNameConfig);

        ProviderConfigProperty userAttributeConfig = new ProviderConfigProperty();
        userAttributeConfig.setName(USER_ATTRIBUTE_KEY);
        userAttributeConfig.setLabel("User attribute");
        userAttributeConfig.setHelpText("The user attribute to be added to the credential subject.");
        userAttributeConfig.setType(ProviderConfigProperty.LIST_TYPE);
        userAttributeConfig.setOptions(
                List.of(UserModel.USERNAME, UserModel.LOCALE, UserModel.FIRST_NAME, UserModel.LAST_NAME,
                        UserModel.DISABLED_REASON, UserModel.EMAIL, UserModel.EMAIL_VERIFIED));
        CONFIG_PROPERTIES.add(userAttributeConfig);
    }

    public static ProtocolMapperModel create(String name) {
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setName(name);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CLAIM_NAME, CLAIM_NAME_ID);
        configMap.put(USER_ATTRIBUTE_KEY, USER_ATTRIBUTE_DID);
        mapperModel.setConfig(configMap);
        mapperModel.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);
        mapperModel.setProtocolMapper(MAPPER_ID);
        return mapperModel;
    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    public void setClaimsForCredential(VerifiableCredential verifiableCredential, UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override
    public void setClaimsForSubject(Map<String, Object> claims, UserSessionModel userSessionModel) {
        UserModel userModel = userSessionModel.getUser();
        List<String> attributePath = getMetadataAttributePath();
        String propertyName = attributePath.get(attributePath.size() - 1);
        var userAttr = KeycloakModelUtils.resolveAttribute(userModel, USER_ATTRIBUTE_DID,false).stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (userAttr != null) {
            claims.put(propertyName, userAttr);
        }
    }

    @Override
    public List<String> getMetadataAttributePath() {
        return ListUtils.union(getAttributePrefix(), List.of(CLAIM_NAME_ID));
    }

    @Override
    public String getDisplayType() {
        return "Subject ID Mapper";
    }

    @Override
    public String getHelpText() {
        return "Assigns the Subject ID from the User's DID";
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new OID4VCSubjectIdMapper();
    }

    @Override
    public String getId() { return MAPPER_ID; }
}
