package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappings UserModel property (the property name of a getter method) to an AttributeStatement.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HardcodedAttributeMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    public static final String PROVIDER_ID = "saml-hardcode-attribute-mapper";
    public static final String ATTRIBUTE_VALUE = "attribute.value";
    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    static {
        ConfigProperty property;
        AttributeStatementHelper.setConfigProperties(configProperties);
        property = new ConfigProperty();
        property.setName(ATTRIBUTE_VALUE);
        property.setLabel("Attribute value");
        property.setType(ConfigProperty.STRING_TYPE);
        property.setHelpText("Value of the attribute you want to hard code.");
        configProperties.add(property);

    }



    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded attribute";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode an attribute into the SAML Assertion.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        String attributeValue = mappingModel.getConfig().get(ATTRIBUTE_VALUE);
        AttributeStatementHelper.addAttribute(attributeStatement, mappingModel, attributeValue);

    }

    public static ProtocolMapperModel create(String name,
                                             String samlAttributeName, String nameFormat, String friendlyName, String value,
                                             boolean consentRequired, String consentText) {
        String mapperId = PROVIDER_ID;
        ProtocolMapperModel model = AttributeStatementHelper.createAttributeMapper(name, null, samlAttributeName, nameFormat, friendlyName,
                consentRequired, consentText, mapperId);
        model.getConfig().put(ATTRIBUTE_VALUE, value);
        return model;

    }

}
