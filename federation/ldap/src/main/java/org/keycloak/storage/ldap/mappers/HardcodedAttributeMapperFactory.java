/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

 package org.keycloak.storage.ldap.mappers;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.ldap.LDAPStorageProvider;

public class HardcodedAttributeMapperFactory extends AbstractLDAPStorageMapperFactory {

   public static final String PROVIDER_ID = "hardcoded-attribute-mapper";


 protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty attrName = createConfigProperty(HardcodedAttributeMapper.USER_MODEL_ATTRIBUTE,
                "User Model Attribute Name",
                "Name of the model attribute, which will be added when importing user from ldap",
                ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE,
                null,
                true);

        ProviderConfigProperty attrValue = createConfigProperty(HardcodedAttributeMapper.ATTRIBUTE_VALUE,
                "Attribute Value",
                "Value of the model attribute, which will be added when importing user from ldap.",
                ProviderConfigProperty.STRING_TYPE,
                null,
                true);

        configProperties.add(attrName);
        configProperties.add(attrValue);
    }

    @Override
    public String getHelpText() {
        return "This mapper will hardcode any model user attribute and some property (like emailVerified or enabled) when importing user from ldap.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        ConfigurationValidationHelper.check(config)
                .checkRequired(HardcodedAttributeMapper.USER_MODEL_ATTRIBUTE, "Attribute Name")
                .checkRequired(HardcodedAttributeMapper.ATTRIBUTE_VALUE, "Attribute Value");
        if(config.get(HardcodedAttributeMapper.USER_MODEL_ATTRIBUTE).equalsIgnoreCase("username") || config.get(HardcodedAttributeMapper.USER_MODEL_ATTRIBUTE).equalsIgnoreCase("email")){
            throw new ComponentValidationException("Attribute Name cannot be set to username or email");
        }
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new HardcodedAttributeMapper(mapperModel, federationProvider);
    }


   
}