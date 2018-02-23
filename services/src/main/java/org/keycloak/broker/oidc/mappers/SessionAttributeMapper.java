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

package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class SessionAttributeMapper extends AbstractClaimMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};
    public static final String ATTRIBUTE = "attribute";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        property1 = new ProviderConfigProperty();
        property1.setName(CLAIM);
        property1.setLabel("Claim");
        property1.setHelpText("Name of claim to search for in token.  You can reference nested claims using a '.', i.e. 'address.locality'.");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE);
        property.setLabel("User Session Attribute");
        property.setHelpText("Name of user session attribute you want to fill");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-session-attribute-idp-mapper";

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
        return "Claim to session attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Claim to session attribute Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        setSessionAttribute(mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        setSessionAttribute(mapperModel, context);
    }

    private void setSessionAttribute(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String claimName = mapperModel.getConfig().get(CLAIM);
        if(StringUtil.isNullOrEmpty(claimName)){
            return;
        }
        String attributeName = mapperModel.getConfig().get(ATTRIBUTE);
        if(StringUtil.isNullOrEmpty(attributeName)){
            return;
        }
        Object claimValue = getClaimValue(mapperModel, context);
        if (claimValue != null) {
            context.getAuthenticationSession().setUserSessionNote(attributeName, claimValue.toString());
        }
    }

    @Override
    public String getHelpText() {
        return "Import declared claim if it exists in ID, access token or the claim set returned by the user profile endpoint to the specified session attribute.";
    }
}
