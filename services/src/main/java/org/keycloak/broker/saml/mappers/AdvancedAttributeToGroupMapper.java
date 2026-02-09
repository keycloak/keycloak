/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.saml.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.provider.ProviderConfigProperty;

import static org.keycloak.utils.RegexUtils.valueMatchesRegex;

/**
 * @author <a href="mailto:denis.bernard@avanade.com">Denis Bernard</a>
 */
public class AdvancedAttributeToGroupMapper extends AbstractAttributeToGroupMapper {

    public static final String PROVIDER_ID = "saml-advanced-group-idp-mapper";
    public static final String ATTRIBUTE_PROPERTY_NAME = "attributes";
    public static final String ARE_ATTRIBUTE_VALUES_REGEX_PROPERTY_NAME = "are.attribute.values.regex";

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    public static final String[] COMPATIBLE_PROVIDERS = {
            SAMLIdentityProviderFactory.PROVIDER_ID
    };

    private static final List<ProviderConfigProperty> configProperties =
            new ArrayList<>();

    static {
        ProviderConfigProperty attributeMappingProperty = new ProviderConfigProperty();
        attributeMappingProperty.setName(ATTRIBUTE_PROPERTY_NAME);
        attributeMappingProperty.setLabel("Attributes");
        attributeMappingProperty.setHelpText("Name and value of the attributes to search for in token. You can reference nested attributes using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        attributeMappingProperty.setType(ProviderConfigProperty.MAP_TYPE);
        configProperties.add(attributeMappingProperty);

        ProviderConfigProperty isAttributeRegexProperty = new ProviderConfigProperty();
        isAttributeRegexProperty.setName(ARE_ATTRIBUTE_VALUES_REGEX_PROPERTY_NAME);
        isAttributeRegexProperty.setLabel("Regex Attribute Values");
        isAttributeRegexProperty.setHelpText("If enabled attribute values are interpreted as regular expressions.");
        isAttributeRegexProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(isAttributeRegexProperty);

        ProviderConfigProperty groupProperty = new ProviderConfigProperty();
        groupProperty.setName(ConfigConstants.GROUP);
        groupProperty.setLabel("Group");
        groupProperty.setHelpText("Group to assign the user to if attribute is present.");
        groupProperty.setType(ProviderConfigProperty.GROUP_TYPE);
        configProperties.add(groupProperty);
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Advanced Attribute to Group";
    }

    @Override
    public String getHelpText() {
        return "If all attributes exists, assign the user to the specified group.";
    }

    @Override
    protected boolean applies(final IdentityProviderMapperModel mapperModel, final BrokeredIdentityContext context) {
        Map<String, List<String>> attributes = mapperModel.getConfigMap(ATTRIBUTE_PROPERTY_NAME);
        boolean areAttributeValuesRegexes = Boolean.parseBoolean(mapperModel.getConfig().get(ARE_ATTRIBUTE_VALUES_REGEX_PROPERTY_NAME));

        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        Set<AttributeStatementType> attributeAssertions = assertion.getAttributeStatements();
        if (attributeAssertions == null) {
            return false;
        }

        for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
            String attributeKey = entry.getKey();
            for (String value : entry.getValue()) {
                List<Object> attributeValues = attributeAssertions.stream()
                        .flatMap(statements -> statements.getAttributes().stream())
                        .filter(choiceType -> attributeKey.equals(choiceType.getAttribute().getName())
                        || attributeKey.equals(choiceType.getAttribute().getFriendlyName()))
                        // Several statements with same name are treated like one with several values
                        .flatMap(choiceType -> choiceType.getAttribute().getAttributeValue().stream())
                        .collect(Collectors.toList());

                boolean attributeValueMatch = areAttributeValuesRegexes ? valueMatchesRegex(value, attributeValues) : attributeValues.contains(value);
                if (!attributeValueMatch) {
                    return false;
                }
            }
        }

        return true;
    }
}
