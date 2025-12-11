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
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Allows adding user attributes to the credential subject
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCUserAttributeMapper extends OID4VCUserPropertyMapper {

    public static final String MAPPER_ID = "oid4vc-unmanaged-attribute-mapper";
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
        userAttributeConfig.setLabel("User property");
        userAttributeConfig.setHelpText("The user attributes to be added to the credential subject.");
        userAttributeConfig.setType(ProviderConfigProperty.STRING_TYPE);
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

    @Override
    public String getDisplayType() {
        return "User Attribute Mapper";
    }

    @Override
    public String getHelpText() {
        return "Maps user attributes to credential claims.";
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
