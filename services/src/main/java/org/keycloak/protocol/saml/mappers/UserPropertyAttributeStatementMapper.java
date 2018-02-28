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

package org.keycloak.protocol.saml.mappers;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Mappings UserModel property (the property name of a getter method) to an AttributeStatement.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserPropertyAttributeStatementMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    public static final String PROVIDER_ID = "saml-user-property-mapper";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_PROPERTY_LABEL);
        property.setHelpText(ProtocolMapperUtils.SAML_USER_MODEL_PROPERTY_HELP_TEXT);
        CONFIG_PROPERTIES.add(property);
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
        return "User Property(ies)";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a built in user property(ies) like email, firstName, lastName or all together to a SAML attribute type.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement,
                                            ProtocolMapperModel mappingModel,
                                            KeycloakSession session,
                                            UserSessionModel userSession,
                                            AuthenticatedClientSessionModel clientSession) {
        String[] propertiesNames = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE).split(",");
        List<String> propertiesParts = new LinkedList<>();
        UserModel user = userSession.getUser();

        for(String propertyName : propertiesNames) {
            Optional.ofNullable(ProtocolMapperUtils.getUserModelValue(user, propertyName))
                    .filter(propertyValue -> !propertyValue.isEmpty())
                    .ifPresent(propertiesParts::add);
        }

        if (propertiesParts.isEmpty()) return;

        AttributeStatementHelper.addAttribute(attributeStatement, mappingModel, String.join(" ", propertiesParts));

    }

    public static ProtocolMapperModel createAttributeMapper(String name,
                                                            String userAttribute,
                                                            String samlAttributeName,
                                                            String nameFormat,
                                                            String friendlyName,
                                                            boolean consentRequired,
                                                            String consentText) {
        return AttributeStatementHelper
                .createAttributeMapper(name, userAttribute,
                        samlAttributeName, nameFormat,
                        friendlyName, consentRequired,
                        consentText, PROVIDER_ID);

    }
}
