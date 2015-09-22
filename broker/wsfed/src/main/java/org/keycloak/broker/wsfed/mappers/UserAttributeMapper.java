/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.broker.wsfed.mappers;

import org.keycloak.broker.wsfed.WSFedEndpoint;
import org.keycloak.broker.wsfed.WSFedIdentityProviderFactory;
import org.keycloak.broker.wsfed.SAML2RequestedToken;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class UserAttributeMapper extends AbstractIdentityProviderMapper {
    protected static final Logger logger = Logger.getLogger(AttributeToRoleMapper.class);

    public static final String[] COMPATIBLE_PROVIDERS = {WSFedIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String ATTRIBUTE_NAME = "attribute.name";
    public static final String ATTRIBUTE_FRIENDLY_NAME = "attribute.friendly.name";
    public static final String USER_ATTRIBUTE = "user.attribute";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME);
        property.setLabel("Attribute Name");
        property.setHelpText("Name of attribute to search for in assertion.  You can leave this blank and specify a friendly name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_FRIENDLY_NAME);
        property.setLabel("Friendly Name");
        property.setHelpText("Friendly name of attribute to search for in assertion.  You can leave this blank and specify a name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store saml attribute.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "wsfed-user-attribute-idp-mapper";

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
        return "WS-Fed Attribute Importer";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        Object value = getAttribute(mapperModel, context);
        if (value != null) {
            user.setSingleAttribute(attribute, value.toString());
        }
    }

    protected String getAttribute(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String name = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        if (name != null && name.trim().equals("")) name = null;
        String friendly = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);
        if (friendly != null && friendly.trim().equals("")) friendly = null;

        try {
            Object token = context.getContextData().get(WSFedEndpoint.WSFED_REQUESTED_TOKEN);

            if(token instanceof SAML2RequestedToken) {
                return getAttribute(((SAML2RequestedToken) token).getAssertionType(), name, friendly);
            }
            //TODO: else if token type == jwt
            else {
                logger.warn("WS-Fed user attribute mapper doesn't currently support this token type.");
            }
        }
        catch(Exception ex) {
            logger.warn("Unable to parse token response", ex);
        }

        return null;
    }

    protected String getAttribute(AssertionType assertion, String name, String friendly) {
        for (AttributeStatementType statement : assertion.getAttributeStatements()) {
            for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                AttributeType attr = choice.getAttribute();
                if (name != null && !name.equals(attr.getName())) {
                    continue;
                }
                if (friendly != null && !name.equals(attr.getFriendlyName())) {
                    continue;
                }

                List<Object> attributeValue = attr.getAttributeValue();
                if (attributeValue == null || attributeValue.isEmpty()) {
                    return null;
                }

                return attributeValue.get(0).toString();
            }
        }

        return null;
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        Object value = getAttribute(mapperModel, context);
        String current = user.getFirstAttribute(attribute);
        if (value != null && !value.equals(current)) {
            user.setSingleAttribute(attribute, value.toString());
        } else if (value == null) {
            user.removeAttribute(attribute);
        }

    }

    @Override
    public String getHelpText() {
        return "Import declared wsfed attribute if it exists in assertion into the specified user attribute.";
    }
}
