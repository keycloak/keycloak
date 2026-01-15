/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Allows adding user properties to the credential subject
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
        subjectPropertyNameConfig.setLabel(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME_LABEL);
        subjectPropertyNameConfig.setHelpText(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME_TOOLTIP);
        subjectPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(subjectPropertyNameConfig);

        ProviderConfigProperty userAttributeConfig = new ProviderConfigProperty();
        userAttributeConfig.setName(USER_ATTRIBUTE_KEY);
        userAttributeConfig.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        userAttributeConfig.setHelpText(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_HELP_TEXT);
        userAttributeConfig.setType(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE);
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

    public void setClaim(VerifiableCredential verifiableCredential,
                         UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override
    public void setClaim(Map<String, Object> claims, UserSessionModel userSessionModel) {
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

    public static ProtocolMapperModel create(String mapperName, String claimName, String userAttribute,
                                             boolean aggregateAttributes) {
        ProtocolMapperModel mapperModel = new ProtocolMapperModel();
        mapperModel.setName(mapperName);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CLAIM_NAME, claimName);
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
        return "Maps user attributes or properties to credential claims.";
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
