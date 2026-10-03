package org.keycloak.protocol.saml.mappers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Mappings User's full name to an AttributeStatement.
 */
public class FullNameMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {

    public static final String PROVIDER_ID = "saml-full-name-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        AttributeStatementHelper.setConfigProperties(configProperties);
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User's full name";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps the user's first and last name to the SAML Assertion. Format is <first> + ' ' + <last>";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        List<String> parts = new LinkedList<>();

        Optional.ofNullable(user.getFirstName()).filter(s -> !s.isEmpty()).ifPresent(parts::add);
        Optional.ofNullable(user.getLastName()).filter(s -> !s.isEmpty()).ifPresent(parts::add);

        if (!parts.isEmpty()) {
            AttributeStatementHelper.addAttribute(attributeStatement, mappingModel, String.join(" ", parts));
        }
    }

    public static ProtocolMapperModel create(String name,
                                             String samlAttributeName, String nameFormat, String friendlyName, String value) {
        String mapperId = PROVIDER_ID;

        return AttributeStatementHelper.createAttributeMapper(name, null, samlAttributeName, nameFormat, friendlyName,
                mapperId);
    }

}
