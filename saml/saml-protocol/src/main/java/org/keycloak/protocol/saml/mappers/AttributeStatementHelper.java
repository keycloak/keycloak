package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.identity.federation.saml.v2.assertion.AttributeStatementType;
import org.picketlink.identity.federation.saml.v2.assertion.AttributeType;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AttributeStatementHelper {
    public static final String ATTRIBUTE_STATEMENT_CATEGORY = "AttributeStatement Mapper";
    public static final String URI_REFERENCE = "URI Reference";
    public static final String URI_REFERENCE_HELP_TEXT = "Attribute name for the SAML URI Reference attribute name format";
    public static final String BASIC = "Basic name";
    public static final String BASIC_HELP_TEXT = "Attribute name for the SAML Basic attribute name format";
    public static final String FRIENDLY_NAME = "Friendly Name";
    public static final String FRIENDLY_NAME_HELP_TEXT = "Standard SAML attribute setting.  An optional, more human-readable form of the attribute's name that can be provided if the actual attribute name is cryptic.";

    public static void addUriReferenceAttribute(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, String attributeValue) {
        String attributeName = mappingModel.getConfig().get(URI_REFERENCE);
        AttributeType attribute = new AttributeType(attributeName);
        attribute.setNameFormat(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get());
        String friendlyName = mappingModel.getConfig().get(FRIENDLY_NAME);
        if (friendlyName != null && friendlyName.trim().equals("")) friendlyName = null;
        if (friendlyName != null) attribute.setFriendlyName(friendlyName);
        attribute.addAttributeValue(attributeValue);
        attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));
    }

    public static void addBasicAttribute(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, String attributeValue) {
        String attributeName = mappingModel.getConfig().get(BASIC);
        AttributeType attribute = new AttributeType(attributeName);
        attribute.setNameFormat(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get());
        String friendlyName = mappingModel.getConfig().get(FRIENDLY_NAME);
        if (friendlyName != null && friendlyName.trim().equals("")) friendlyName = null;
        if (friendlyName != null) attribute.setFriendlyName(friendlyName);
        attribute.addAttributeValue(attributeValue);
        attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));
    }

    protected static void addUriReferenceProperties(List<ProtocolMapper.ConfigProperty> configProperties) {
        ProtocolMapper.ConfigProperty property;
        property = new ProtocolMapper.ConfigProperty();
        property.setName(FRIENDLY_NAME);
        property.setLabel(FRIENDLY_NAME);
        property.setHelpText(FRIENDLY_NAME_HELP_TEXT);
        configProperties.add(property);
        property = new ProtocolMapper.ConfigProperty();
        property.setName(URI_REFERENCE);
        property.setLabel(URI_REFERENCE);
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
        property.setName(BASIC);
        property.setLabel(BASIC);
        property.setHelpText(BASIC_HELP_TEXT);
        configProperties.add(property);
    }
}
