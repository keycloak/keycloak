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

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HardcodedLDAPAttributeMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "hardcoded-ldap-attribute-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty attrName = createConfigProperty(HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_NAME,
                "LDAP Attribute Name",
                "Name of the LDAP attribute, which will be added to the new user during registration",
                ProviderConfigProperty.STRING_TYPE,
                null,
                true);

        ProviderConfigProperty attrValue = createConfigProperty(HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_VALUE,
                "LDAP Attribute Value",
                "Value of the LDAP attribute, which will be added to the new user during registration. You can either hardcode any value like 'foo' but you can also use some special tokens. "
                        + "Only supported token right now is '${RANDOM}' , which will be replaced with some randomly generated String.",
                ProviderConfigProperty.STRING_TYPE,
                null,
                true);

        configProperties.add(attrName);
        configProperties.add(attrValue);
    }

    @Override
    public String getHelpText() {
        return "This mapper is supported just if syncRegistrations is enabled. When new user is registered in Keycloak, he will be written to the LDAP with the hardcoded value of some specified attribute.";
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
                .checkRequired(HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_NAME, "LDAP Attribute Name")
                .checkRequired(HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_VALUE, "LDAP Attribute Value");
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new HardcodedLDAPAttributeMapper(mapperModel, federationProvider);
    }


}
