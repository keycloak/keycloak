package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.picketlink.identity.federation.saml.v2.assertion.AttributeStatementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappings UserModel property (the property name of a getter method) to an AttributeStatement.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAttributeUriReferenceAttributeStatementMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_HELP_TEXT);
        configProperties.add(property);
        AttributeStatementHelper.addUriReferenceProperties(configProperties);

    }

    public static final String PROVIDER_ID = "saml-user-attribute-uri-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Attribute URI";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a custom user attribute to a to a SAML URI reference attribute type..";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String attributeName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        String attributeValue = user.getAttribute(attributeName);
        AttributeStatementHelper.addUriReferenceAttribute(attributeStatement, mappingModel, attributeValue);

    }
    public static void addAttributeMapper(RealmModel realm, String name,
                                          String userAttribute,
                                          String samlAttributeName,
                                          String friendlyName,
                                          boolean consentRequired, String consentText,
                                          boolean appliedByDefault) {
        String mapperId = PROVIDER_ID;
        AttributeStatementHelper.addAttributeMapper(realm, name, userAttribute, samlAttributeName, friendlyName, consentRequired, consentText, appliedByDefault, mapperId);

    }

}
