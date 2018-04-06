/*
 * Copyright (c) eHealth
 */
package org.keycloak.broker.saml.mappers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:frelibert@yahoo.com">Frederik Libert</a>
 *
 */
public class UserAttributeStatementMapper extends AbstractIdentityProviderMapper {

    private static final String USER_ATTR_LOCALE = "locale";

    private static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    public static final String ATTRIBUTE_NAME_PATTERN = "attribute.name.pattern";

    public static final String USER_ATTRIBUTE_FIRST_NAME = "user.attribute.firstName";

    public static final String USER_ATTRIBUTE_LAST_NAME = "user.attribute.lastName";

    public static final String USER_ATTRIBUTE_EMAIL = "user.attribute.email";

    public static final String USER_ATTRIBUTE_LANGUAGE = "user.attribute.language";
    
    private static final String USE_FRIENDLY_NAMES = "use.friendly.names";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME_PATTERN);
        property.setLabel("Attribute Name Pattern");
        property.setHelpText("Pattern of attribute names in assertion that must be mapped. Leave blank to map all attributes.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(property);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE_FIRST_NAME);
        property.setLabel("User Attribute FirstName");
        property.setHelpText("Define which saml Attribute must be mapped to the User property firstName.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(property);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE_LAST_NAME);
        property.setLabel("User Attribute LastName");
        property.setHelpText("Define which saml Attribute must be mapped to the User property lastName.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(property);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE_EMAIL);
        property.setLabel("User Attribute Email");
        property.setHelpText("Define which saml Attribute must be mapped to the User property email.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(property);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE_LANGUAGE);
        property.setLabel("User Attribute Language");
        property.setHelpText("Define which saml Attribute must be mapped to the User attribute locale.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(property);
        property = new ProviderConfigProperty();
        property.setName(USE_FRIENDLY_NAMES);
        property.setLabel("Use Attribute Friendly Name");
        property.setHelpText("Define which name to give to each mapped user attribute: name or friendlyName.");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        CONFIG_PROPERTIES.add(property);
    }

    public static final String PROVIDER_ID = "saml-user-attributestatement-idp-mapper";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS.clone();
    }

    @Override
    public String getDisplayCategory() {
        return "AttributeStatement Importer";
    }

    @Override
    public String getDisplayType() {
        return "AttributeStatement Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String firstNameAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_FIRST_NAME);
        String lastNameAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_LAST_NAME);
        String emailAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_EMAIL);
        String langAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_LANGUAGE);
        Boolean useFriendlyNames = Boolean.valueOf(mapperModel.getConfig().get(USE_FRIENDLY_NAMES));
        List<AttributeType> attributesInContext = findAttributesInContext(context, getAttributePattern(mapperModel));
        for (AttributeType a : attributesInContext) {
            String attribute = useFriendlyNames ? a.getFriendlyName() : a.getName();
            List<String> attributeValuesInContext = a.getAttributeValue().stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
            if (!attributeValuesInContext.isEmpty()) {
                // set as attribute anyway
                context.setUserAttribute(attribute, attributeValuesInContext);
                // set as special field ?
                if (Objects.equals(attribute, emailAttribute)) {
                    setIfNotEmpty(context::setEmail, attributeValuesInContext);
                } else if (Objects.equals(attribute, firstNameAttribute)) {
                    setIfNotEmpty(context::setFirstName, attributeValuesInContext);
                } else if (Objects.equals(attribute, lastNameAttribute)) {
                    setIfNotEmpty(context::setLastName, attributeValuesInContext);
                } else if (Objects.equals(attribute, langAttribute)) {
                    context.setUserAttribute(USER_ATTR_LOCALE, attributeValuesInContext);
                } 
            }
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String firstNameAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_FIRST_NAME);
        String lastNameAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_LAST_NAME);
        String emailAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_EMAIL);
        String langAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_LANGUAGE);
        Boolean useFriendlyNames = Boolean.valueOf(mapperModel.getConfig().get(USE_FRIENDLY_NAMES));
        List<AttributeType> attributesInContext = findAttributesInContext(context, getAttributePattern(mapperModel));

        Set<String> assertedUserAttributes = new HashSet<String>();
        for (AttributeType a : attributesInContext) {
            String attribute = useFriendlyNames ? a.getFriendlyName() : a.getName();
            List<String> attributeValuesInContext = a.getAttributeValue().stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
            List<String> currentAttributeValues = user.getAttributes().get(attribute);
            if (attributeValuesInContext == null) {
                // attribute no longer sent by brokered idp, remove it
                user.removeAttribute(attribute);
            } else if (currentAttributeValues == null) {
                // new attribute sent by brokered idp, add it
                user.setAttribute(attribute, attributeValuesInContext);
            } else if (!CollectionUtil.collectionEquals(attributeValuesInContext, currentAttributeValues)) {
                // attribute sent by brokered idp has different values as before, update it
                user.setAttribute(attribute, attributeValuesInContext);
            }
            if (Objects.equals(attribute, emailAttribute)) {
                setIfNotEmpty(context::setEmail, attributeValuesInContext);
            } else if (Objects.equals(attribute, firstNameAttribute)) {
                setIfNotEmpty(context::setFirstName, attributeValuesInContext);
            } else if (Objects.equals(attribute, lastNameAttribute)) {
                setIfNotEmpty(context::setLastName, attributeValuesInContext);
            } else if (Objects.equals(attribute, langAttribute)) {
                if(attributeValuesInContext == null) {
                    user.removeAttribute(USER_ATTR_LOCALE);
                } else {
                    user.setAttribute(USER_ATTR_LOCALE, attributeValuesInContext);
                }
                assertedUserAttributes.add(USER_ATTR_LOCALE);
            } 
            // Mark attribute as handled
            assertedUserAttributes.add(attribute);
        }
        // Remove user attributes that were not referenced in assertion.
        user.getAttributes().keySet().stream().filter(a -> !assertedUserAttributes.contains(a)).forEach(a -> user.removeAttribute(a));
    }

    @Override
    public String getHelpText() {
        return "Import all saml attributes found in attributestatements in assertion into user properties or attributes.";
    }

    private Optional<Pattern> getAttributePattern(IdentityProviderMapperModel mapperModel) {
        String attributePatternConfig = mapperModel.getConfig().get(ATTRIBUTE_NAME_PATTERN);
        return Optional.ofNullable(attributePatternConfig != null ? Pattern.compile(attributePatternConfig) : null);
    }

    private List<AttributeType> findAttributesInContext(BrokeredIdentityContext context, Optional<Pattern> attributePattern) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

        return assertion.getAttributeStatements().stream()//
            .flatMap(statement -> statement.getAttributes().stream())//
            .filter(item -> !attributePattern.isPresent() || attributePattern.get().matcher(item.getAttribute().getName()).matches())//
            .map(ASTChoiceType::getAttribute)//
            .collect(Collectors.toList());
    }

    private void setIfNotEmpty(Consumer<String> consumer, List<String> values) {
        if (values != null && !values.isEmpty()) {
            consumer.accept(values.get(0));
        }
    }

}
