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
import org.keycloak.broker.provider.HardcodedRoleMapper;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import java.util.ArrayList;
import java.util.List;

public class AttributeToRoleMapper extends AbstractIdentityProviderMapper {
    protected static final Logger logger = Logger.getLogger(AttributeToRoleMapper.class);

    public static final String[] COMPATIBLE_PROVIDERS = {WSFedIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String ATTRIBUTE_NAME = "attribute.name";
    public static final String ATTRIBUTE_FRIENDLY_NAME = "attribute.friendly.name";
    public static final String ATTRIBUTE_VALUE = "attribute.value";

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
        property.setName(ATTRIBUTE_VALUE);
        property.setLabel("Attribute Value");
        property.setHelpText("Value the attribute must have.  If the attribute is a list, then the value must be contained in the list.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(HardcodedRoleMapper.ROLE);
        property.setLabel("Role");
        property.setHelpText("Role to grant to user.  To reference an application role the syntax is appname.approle, i.e. myapp.myrole");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "wsfed-role-idp-mapper";

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
        return "Role Mapper";
    }

    @Override
    public String getDisplayType() {
        return "WS-Fed Attribute to Role";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String roleName = mapperModel.getConfig().get(HardcodedRoleMapper.ROLE);
        if (isAttributePresent(mapperModel, context)) {
            RoleModel role = HardcodedRoleMapper.getRoleFromString(realm, roleName);
            if (role == null) throw new IdentityBrokerException("Unable to find role: " + roleName);
            user.grantRole(role);
        }
    }

    protected boolean isAttributePresent(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String name = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        if (name != null && name.trim().equals("")) name = null;
        String friendly = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);
        if (friendly != null && friendly.trim().equals("")) friendly = null;
        String desiredValue = mapperModel.getConfig().get(ATTRIBUTE_VALUE);

        try {
            Object token = context.getContextData().get(WSFedEndpoint.WSFED_REQUESTED_TOKEN);

            if(token instanceof SAML2RequestedToken) {
                return isAttributePresent(((SAML2RequestedToken) token).getAssertionType(), name, friendly, desiredValue);
            }
            //TODO: else if token type == jwt
            else {
                logger.warn("WS-Fed attribute role mapper doesn't currently support this token type.");
            }
        }
        catch(Exception ex) {
            logger.warn("Unable to parse token response", ex);
        }

        return false;
    }

    protected boolean isAttributePresent(AssertionType assertion, String name, String friendly, String desiredValue) {
        for (AttributeStatementType statement : assertion.getAttributeStatements()) {
            for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                AttributeType attr = choice.getAttribute();
                if (name != null && !name.equals(attr.getName())){
                    continue;
                }
                if (friendly != null && !name.equals(attr.getFriendlyName())){
                    continue;
                }
                for (Object val : attr.getAttributeValue()) {
                    if (val.equals(desiredValue)){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String roleName = mapperModel.getConfig().get(HardcodedRoleMapper.ROLE);
        RoleModel role = HardcodedRoleMapper.getRoleFromString(realm, roleName);
        if (role == null) throw new IdentityBrokerException("Unable to find role: " + roleName);

        if (isAttributePresent(mapperModel, context)) {
            user.grantRole(role);
        }
        else {
            user.deleteRoleMapping(role);
        }
    }

    @Override
    public String getHelpText() {
        return "If a claim exists, grant the user the specified realm or application role.";
    }
}
