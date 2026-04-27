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

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAttributeLDAPStorageMapperFactory extends AbstractLDAPStorageMapperFactory implements LDAPConfigDecorator {

    public static final String PROVIDER_ID = "user-attribute-ldap-mapper";
    protected static final List<ProviderConfigProperty> configProperties;

    static {
        List<ProviderConfigProperty> props = getConfigProps(null);
        configProperties = props;
    }

    static List<ProviderConfigProperty> getConfigProps(ComponentModel p) {
        String readOnly = "false";
        UserStorageProviderModel parent = new UserStorageProviderModel();
        if (p != null) {
            parent = new UserStorageProviderModel(p);
            LDAPConfig ldapConfig = new LDAPConfig(parent.getConfig());
            readOnly = ldapConfig.getEditMode() == UserStorageProvider.EditMode.WRITABLE ? "false" : "true";
        }
        ProviderConfigurationBuilder config = ProviderConfigurationBuilder.create()
                .property().name(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE)
                .label("User Model Attribute")
                .helpText("Name of the UserModel property or attribute you want to map the LDAP attribute into. For example 'firstName', 'lastName, 'email', 'street' etc.")
                .type(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE)
                .required(true)
                .add()
                .property().name(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE).label("LDAP Attribute").helpText("Name of mapped attribute on LDAP object. For example 'cn', 'sn, 'mail', 'street' etc.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()
                .property().name(UserAttributeLDAPStorageMapper.READ_ONLY).label("Read Only")
                .helpText("Read-only attribute is imported from LDAP to UserModel, but it's not saved back to LDAP when user is updated in Keycloak.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(readOnly)
                .add();
        if (parent.isImportEnabled()) {
            config.
            property().name(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP).label("Always Read Value From LDAP")
                    .helpText("If on, then during reading of the LDAP attribute value will always used instead of the value from Keycloak DB")
                    .type(ProviderConfigProperty.BOOLEAN_TYPE).defaultValue("false").add();
        }
        config.property().name(UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP).label("Is Mandatory In LDAP")
                .helpText("If true, attribute is mandatory in LDAP. When an attribute is mandatory the options attribute default value and force a default value apply to this mapper.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false").add()
                .property().name(UserAttributeLDAPStorageMapper.ATTRIBUTE_DEFAULT_VALUE).label("Attribute default value")
                .helpText("If there is no value in Keycloak DB and attribute is mandatory in LDAP, this value will be propagated to LDAP")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("").add()
                .property().name(UserAttributeLDAPStorageMapper.FORCE_DEFAULT_VALUE).label("Force a Default Value")
                .helpText("If true a empty default value is forced for mandatory attributes even when a default value is not specified. If false the mandatory attribute needs to be manually set during the transaction when the default value option is not configured.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("true").add()
                .property().name(UserAttributeLDAPStorageMapper.IS_BINARY_ATTRIBUTE).label("Is Binary Attribute")
                .helpText("Should be true for binary LDAP attributes")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false").add();
        return config.build();
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

        boolean isBinaryAttribute = config.get(UserAttributeLDAPStorageMapper.IS_BINARY_ATTRIBUTE, false);
        boolean alwaysReadValueFromLDAP = config.get(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, false);
        if (isBinaryAttribute && !alwaysReadValueFromLDAP) {
            throw new ComponentValidationException("With Binary attribute enabled, the ''Always read value from LDAP'' must be enabled too");
        }

    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new UserAttributeLDAPStorageMapper(mapperModel, federationProvider);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(RealmModel realm, ComponentModel parent) {
        return getConfigProps(parent);
    }


    @Override
    public void updateLDAPConfig(LDAPConfig ldapConfig, ComponentModel mapperModel) {
        boolean isBinaryAttribute = mapperModel.get(UserAttributeLDAPStorageMapper.IS_BINARY_ATTRIBUTE, false);
        if (isBinaryAttribute) {
            String ldapAttrName = mapperModel.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE);
            ldapConfig.addBinaryAttribute(ldapAttrName);
        }
    }
}
