/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
 * @author <a href="mailto:stoffus@stoffus.com">Christopher Svensson</a>
 * @version $Revision: 1 $
 */
public class FullNameMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<ProviderConfigProperty>();
    public static final String PROVIDER_ID = "saml-full-name-mapper";

    static {
        AttributeStatementHelper.setConfigProperties(CONFIG_PROPERTIES);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
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
        return "Maps the user's first and last name to a SAML attribute. Format is <first> + ' ' + <last>";
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

    public static ProtocolMapperModel create(String name, String samlAttributeName, String nameFormat, String friendlyName, String value, boolean consentRequired, String consentText) {
        return AttributeStatementHelper.createAttributeMapper(name, null, samlAttributeName, nameFormat, friendlyName, consentRequired, consentText, PROVIDER_ID);
    }

}