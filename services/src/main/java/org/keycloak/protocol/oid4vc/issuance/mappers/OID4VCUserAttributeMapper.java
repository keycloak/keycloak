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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Allows to add user attributes to the credential subject
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCUserAttributeMapper extends OID4VCMapper {

    public static final String MAPPER_ID = "oid4vc-user-attribute-mapper";
    public static final String AGGREGATE_ATTRIBUTES_KEY = "aggregateAttributes";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty subjectPropertyNameConfig = new ProviderConfigProperty();
        subjectPropertyNameConfig.setName(CLAIM_NAME);
        subjectPropertyNameConfig.setLabel("Claim Name");
        subjectPropertyNameConfig.setHelpText("The name of the claim added to the credential subject that is extracted " +
                                                      "from the user attributes.");
        subjectPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(subjectPropertyNameConfig);

        ProviderConfigProperty userAttributeConfig = new ProviderConfigProperty();
        userAttributeConfig.setName(USER_ATTRIBUTE_KEY);
        userAttributeConfig.setLabel("User attribute");
        userAttributeConfig.setHelpText("The user attribute to be added to the credential subject.");
        userAttributeConfig.setType(ProviderConfigProperty.LIST_TYPE);
        userAttributeConfig.setOptions(
                List.of(UserModel.USERNAME, UserModel.LOCALE, UserModel.FIRST_NAME, UserModel.LAST_NAME,
                        UserModel.DISABLED_REASON, UserModel.EMAIL, UserModel.EMAIL_VERIFIED));
        CONFIG_PROPERTIES.add(userAttributeConfig);

        ProviderConfigProperty aggregateAttributesConfig = new ProviderConfigProperty();
        aggregateAttributesConfig.setName(AGGREGATE_ATTRIBUTES_KEY);
        aggregateAttributesConfig.setLabel("Aggregate attributes");
        aggregateAttributesConfig.setHelpText("Should the mapper aggregate user attributes.");
        aggregateAttributesConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        CONFIG_PROPERTIES.add(aggregateAttributesConfig);
    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    public void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                       UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override
    public void setClaimsForSubject(Map<String, Object> claims, UserSessionModel userSessionModel) {
        List<String> attributePath = getMetadataAttributePath();
        String propertyName = attributePath.get(attributePath.size() - 1);
        String userAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_KEY);
        boolean aggregateAttributes = Optional.ofNullable(mapperModel.getConfig().get(AGGREGATE_ATTRIBUTES_KEY))
                .map(Boolean::parseBoolean).orElse(false);
        Collection<String> attributes =
                KeycloakModelUtils.resolveAttribute(userSessionModel.getUser(), userAttribute,
                        aggregateAttributes);
        attributes.removeAll(Collections.singleton(null));
        if (!attributes.isEmpty()) {
            claims.put(propertyName, String.join(",", attributes));
        }
    }

    public static ProtocolMapperModel create(String mapperName, String userAttribute, String propertyName,
                                             boolean aggregateAttributes) {
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setName(mapperName);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CLAIM_NAME, propertyName);
        configMap.put(USER_ATTRIBUTE_KEY, userAttribute);
        configMap.put(AGGREGATE_ATTRIBUTES_KEY, Boolean.toString(aggregateAttributes));
        mapperModel.setConfig(configMap);
        mapperModel.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);
        mapperModel.setProtocolMapper(MAPPER_ID);
        return mapperModel;
    }

    @Override
    public String getDisplayType() {
        return "User Attribute Mapper";
    }

    @Override
    public String getHelpText() {
        return "Maps user attributes to credential subject properties.";
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new OID4VCUserAttributeMapper();
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }
}
