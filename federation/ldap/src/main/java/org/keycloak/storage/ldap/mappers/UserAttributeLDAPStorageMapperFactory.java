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

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAttributeLDAPStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "user-attribute-ldap-mapper";
    protected static final List<ProviderConfigProperty> configProperties;

    static {
        List<ProviderConfigProperty> props = getConfigProps(null);
        configProperties = props;
    }

    private static List<ProviderConfigProperty> getConfigProps(ComponentModel parent) {
        String readOnly = "false";
        if (parent != null) {
            LDAPConfig ldapConfig = new LDAPConfig(parent.getConfig());
            readOnly = ldapConfig.getEditMode() == UserStorageProvider.EditMode.WRITABLE ? "false" : "true";
        }
        return ProviderConfigurationBuilder.create()
                    .property().name(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE)
                               .label("User Model Attribute")
                               .helpText("Name of mapped UserModel property or UserModel attribute in Keycloak DB. For example 'firstName', 'lastName, 'email', 'street' etc.")
                               .type(ProviderConfigProperty.STRING_TYPE)
                               .add()
                    .property().name(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE).label("LDAP Attribute").helpText("Name of mapped attribute on LDAP object. For example 'cn', 'sn, 'mail', 'street' etc.")
                               .type(ProviderConfigProperty.STRING_TYPE)
                               .add()
                    .property().name(UserAttributeLDAPStorageMapper.READ_ONLY).label("Read Only")
                               .helpText("Read-only attribute is imported from LDAP to Keycloak DB, but it's not saved back to LDAP when user is updated in Keycloak.")
                               .type(ProviderConfigProperty.BOOLEAN_TYPE)
                               .defaultValue(readOnly)
                               .add()
                    .property().name(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP).label("Always Read Value From LDAP")
                               .helpText("If on, then during reading of the user will be value of attribute from LDAP always used instead of the value from Keycloak DB")
                               .type(ProviderConfigProperty.BOOLEAN_TYPE).defaultValue("false").add()
                    .property().name(UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP).label("Is Mandatory In LDAP")
                               .helpText("If true, attribute is mandatory in LDAP. Hence if there is no value in Keycloak DB, the empty value will be set to be propagated to LDAP")
                               .type(ProviderConfigProperty.BOOLEAN_TYPE)
                               .defaultValue("false").add()
                    .build();
    }

    @Override
    public String getHelpText() {
        return "Used to map single attribute from LDAP user to attribute of UserModel in Keycloak DB";
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
        checkMandatoryConfigAttribute(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, "User Model Attribute", config);
        checkMandatoryConfigAttribute(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, "LDAP Attribute", config);

    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider, RealmModel realm) {
        return new UserAttributeLDAPStorageMapper(mapperModel, federationProvider, realm);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(RealmModel realm, ComponentModel parent) {
        return getConfigProps(parent);
    }
}
