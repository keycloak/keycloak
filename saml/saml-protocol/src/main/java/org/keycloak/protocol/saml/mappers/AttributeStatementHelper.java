package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlProtocol;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.identity.federation.saml.v2.assertion.AttributeStatementType;
import org.picketlink.identity.federation.saml.v2.assertion.AttributeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AttributeStatementHelper {
    public static final String SAML_ATTRIBUTE_NAME = "SAML Attribute Name";
    public static final String ATTRIBUTE_STATEMENT_CATEGORY = "AttributeStatement Mapper";
    public static final String URI_REFERENCE_LABEL = "URI Reference";
    public static final String URI_REFERENCE_HELP_TEXT = "Attribute name for the SAML URI Reference attribute name format";
    public static final String BASIC_LABEL = "Basic name";
    public static final String BASIC_HELP_TEXT = "Attribute name for the SAML Basic attribute name format";
    public static final String FRIENDLY_NAME = "Friendly Name";
    public static final String FRIENDLY_NAME_HELP_TEXT = "Standard SAML attribute setting.  An optional, more human-readable form of the attribute's name that can be provided if the actual attribute name is cryptic.";

    public static void addAttribute(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel,
                                    String attributeNameFormat, String attributeValue) {
        String attributeName = mappingModel.getConfig().get(SAML_ATTRIBUTE_NAME);
        AttributeType attribute = new AttributeType(attributeName);
        attribute.setNameFormat(attributeNameFormat);
        String friendlyName = mappingModel.getConfig().get(FRIENDLY_NAME);
        if (friendlyName != null && !friendlyName.trim().equals("")) attribute.setFriendlyName(friendlyName);
        attribute.addAttributeValue(attributeValue);
        attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));
    }

    public static void addUriReferenceAttribute(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, String attributeValue) {
        String attributeNameFormat = JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get();
        addAttribute(attributeStatement, mappingModel, attributeNameFormat, attributeValue);
    }

    public static void addBasicAttribute(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, String attributeValue) {
        addAttribute(attributeStatement, mappingModel, JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get(), attributeValue);
    }

    protected static void addUriReferenceProperties(List<ProtocolMapper.ConfigProperty> configProperties) {
        ProtocolMapper.ConfigProperty property;
        property = new ProtocolMapper.ConfigProperty();
        property.setName(FRIENDLY_NAME);
        property.setLabel(FRIENDLY_NAME);
        property.setHelpText(FRIENDLY_NAME_HELP_TEXT);
        configProperties.add(property);
        property = new ProtocolMapper.ConfigProperty();
        property.setName(SAML_ATTRIBUTE_NAME);
        property.setLabel(URI_REFERENCE_LABEL);
        property.setHelpText(URI_REFERENCE_HELP_TEXT);
        configProperties.add(property);
    }
    protected static void addBasicProperties(List<ProtocolMapper.ConfigProperty> configProperties) {
        ProtocolMapper.ConfigProperty property;
        property = new ProtocolMapper.ConfigProperty();
        property.setName(FRIENDLY_NAME);
        property.setLabel(FRIENDLY_NAME);
        property.setHelpText(FRIENDLY_NAME_HELP_TEXT);
        configProperties.add(property);
        property = new ProtocolMapper.ConfigProperty();
        property.setName(SAML_ATTRIBUTE_NAME);
        property.setLabel(BASIC_LABEL);
        property.setHelpText(BASIC_HELP_TEXT);
        configProperties.add(property);
    }

    public static void addAttributeMapper(RealmModel realm, String name, String userAttribute, String samlAttributeName, String friendlyName, boolean consentRequired, String consentText, boolean appliedByDefault, String mapperId) {
        ProtocolMapperModel mapper = realm.getProtocolMapperByName(SamlProtocol.LOGIN_PROTOCOL, name);
        if (mapper != null) return;
        mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        mapper.setConsentRequired(consentRequired);
        mapper.setConsentText(consentText);
        mapper.setAppliedByDefault(appliedByDefault);
        Map<String, String> config = new HashMap<String, String>();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
        config.put(SAML_ATTRIBUTE_NAME, samlAttributeName);
        if (friendlyName != null) {
            config.put(FRIENDLY_NAME, friendlyName);
        }
        mapper.setConfig(config);
        realm.addProtocolMapper(mapper);
    }
}
