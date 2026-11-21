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

package org.keycloak.broker.saml.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.saml.mappers.SamlMetadataDescriptorUpdater;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.StringUtil;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC;

public class EmailAttributeToNameMapper extends AbstractIdentityProviderMapper implements SamlMetadataDescriptorUpdater {

    public static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};

    public static final String ATTRIBUTE_NAME = "attribute.name";
    public static final String ATTRIBUTE_FRIENDLY_NAME = "attribute.friendly.name";
    public static final String ATTRIBUTE_NAME_FORMAT = "attribute.name.format";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));
    private static final String DEFAULT_EMAIL_ATTRIBUTE = "email";
    private static final String NAME_SPLIT_REGEX = "[._-]+";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME);
        property.setLabel("Email Attribute Name");
        property.setHelpText("Name of the email attribute to search for in assertion. You can leave this blank and specify a friendly name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(DEFAULT_EMAIL_ATTRIBUTE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_FRIENDLY_NAME);
        property.setLabel("Email Friendly Name");
        property.setHelpText("Friendly name of the email attribute to search for in assertion. You can leave this blank and specify a name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME_FORMAT);
        property.setLabel("Name Format");
        property.setHelpText("Name format of attribute to specify in the RequestedAttribute element. Default to basic format.");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(UserAttributeMapper.NAME_FORMATS);
        property.setDefaultValue(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.name());
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "saml-email-attribute-to-name-idp-mapper";

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
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Email to Name Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        NameParts nameParts = getNameParts(mapperModel, context);
        if (nameParts == null) {
            return;
        }

        if (!StringUtil.isNullOrEmpty(nameParts.firstName)) {
            context.setFirstName(nameParts.firstName);
        }
        if (!StringUtil.isNullOrEmpty(nameParts.lastName)) {
            context.setLastName(nameParts.lastName);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        NameParts nameParts = getNameParts(mapperModel, context);
        if (nameParts == null) {
            return;
        }

        setIfNotEmptyAndDifferent(user::setFirstName, user::getFirstName, nameParts.firstName);
        setIfNotEmptyAndDifferent(user::setLastName, user::getLastName, nameParts.lastName);
    }

    @Override
    public String getHelpText() {
        return "Derive first and last name from the email attribute in the SAML assertion.";
    }

    @Override
    public void updateMetadata(IdentityProviderMapperModel mapperModel, EntityDescriptorType entityDescriptor) {
        String attributeName = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        String attributeFriendlyName = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);

        RequestedAttributeType requestedAttribute = new RequestedAttributeType(attributeName);
        requestedAttribute.setIsRequired(null);
        requestedAttribute.setNameFormat(mapperModel.getConfig().get(ATTRIBUTE_NAME_FORMAT) != null
                ? JBossSAMLURIConstants.valueOf(mapperModel.getConfig().get(ATTRIBUTE_NAME_FORMAT)).get()
                : ATTRIBUTE_FORMAT_BASIC.get());

        if (!StringUtil.isNullOrEmpty(attributeFriendlyName)) {
            requestedAttribute.setFriendlyName(attributeFriendlyName);
        }

        for (EntityDescriptorType.EDTChoiceType choiceType : entityDescriptor.getChoiceType()) {
            List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();
            for (EntityDescriptorType.EDTDescriptorChoiceType descriptor : descriptors) {
                for (AttributeConsumingServiceType attributeConsumingService : descriptor.getSpDescriptor().getAttributeConsumingService()) {
                    boolean alreadyPresent = attributeConsumingService.getRequestedAttribute().stream()
                            .anyMatch(t -> (attributeName == null || attributeName.equalsIgnoreCase(t.getName())) &&
                                    (attributeFriendlyName == null || attributeFriendlyName.equalsIgnoreCase(t.getFriendlyName())));

                    if (!alreadyPresent) {
                        attributeConsumingService.addRequestedAttribute(requestedAttribute);
                    }
                }
            }
        }
    }

    private NameParts getNameParts(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attributeName = getAttributeNameFromMapperModel(mapperModel);
        if (StringUtil.isNullOrEmpty(attributeName)) {
            attributeName = DEFAULT_EMAIL_ATTRIBUTE;
        }

        List<String> attributeValuesInContext = findAttributeValuesInContext(attributeName, context);
        if (attributeValuesInContext.isEmpty()) {
            return null;
        }

        String email = attributeValuesInContext.get(0);
        String localPart = UsernameTemplateMapper.getEmailLocalPart(email);
        if (StringUtil.isNullOrEmpty(localPart)) {
            return null;
        }

        String[] parts = localPart.split(NAME_SPLIT_REGEX);
        if (parts.length == 0) {
            return null;
        }

        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[parts.length - 1] : null;
        return new NameParts(firstName, lastName);
    }

    private String getAttributeNameFromMapperModel(IdentityProviderMapperModel mapperModel) {
        String attributeName = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        if (attributeName == null) {
            attributeName = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);
        }
        return attributeName;
    }

    private void setIfNotEmptyAndDifferent(Consumer<String> consumer, Supplier<String> currentValueSupplier, String value) {
        if (!StringUtil.isNullOrEmpty(value) && !value.equals(currentValueSupplier.get())) {
            consumer.accept(value);
        }
    }

    private Predicate<AttributeStatementType.ASTChoiceType> elementWith(String attributeName) {
        return attributeType -> {
            AttributeType attribute = attributeType.getAttribute();
            return Objects.equals(attribute.getName(), attributeName)
                    || Objects.equals(attribute.getFriendlyName(), attributeName);
        };
    }

    private List<String> findAttributeValuesInContext(String attributeName, BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

        return assertion.getAttributeStatements().stream()
                .flatMap(statement -> statement.getAttributes().stream())
                .filter(elementWith(attributeName))
                .flatMap(attributeType -> attributeType.getAttribute().getAttributeValue().stream())
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    private static class NameParts {
        private final String firstName;
        private final String lastName;

        private NameParts(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}

