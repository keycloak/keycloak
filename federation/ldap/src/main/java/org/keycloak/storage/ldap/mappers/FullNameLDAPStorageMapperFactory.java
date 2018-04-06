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
import org.keycloak.models.LDAPConstants;
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
public class FullNameLDAPStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID =  "full-name-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties;

    static {
        configProperties = getConfigProps(null);
    }

    private static List<ProviderConfigProperty> getConfigProps(ComponentModel parent) {
        boolean readOnly = false;
        if (parent != null) {
            LDAPConfig config = new LDAPConfig(parent.getConfig());
            readOnly = config.getEditMode() != UserStorageProvider.EditMode.WRITABLE;
        }


        return ProviderConfigurationBuilder.create()
                .property().name(FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE)
                           .label("LDAP Full Name Attribute")
                           .helpText("Name of LDAP attribute, which contains fullName of user. Usually it will be 'cn' ")
                           .type(ProviderConfigProperty.STRING_TYPE)
                           .defaultValue(LDAPConstants.CN)
                           .add()
                .property().name(FullNameLDAPStorageMapper.READ_ONLY)
                           .label("Read Only")
                           .helpText("For Read-only is data imported from LDAP to Keycloak DB, but it's not saved back to LDAP when user is updated in Keycloak.")
                           .type(ProviderConfigProperty.BOOLEAN_TYPE)
                           .defaultValue(String.valueOf(readOnly))
                .add()
                .property().name(FullNameLDAPStorageMapper.WRITE_ONLY)
                           .label("Write Only")
                           .helpText("For Write-only is data propagated to LDAP when user is created or updated in Keycloak. But this mapper is not used to propagate data from LDAP back into Keycloak. " +
                        "This setting is useful if you configured separate firstName and lastName attribute mappers and you want to use those to read attribute from LDAP into Keycloak")
                           .type(ProviderConfigProperty.BOOLEAN_TYPE)
                           .defaultValue(String.valueOf(!readOnly))
                            .add()
                           .build();
    }

    @Override
    public String getHelpText() {
        return "Used to map full-name of user from single attribute in LDAP (usually 'cn' attribute) to firstName and lastName attributes of UserModel in Keycloak DB";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(RealmModel realm, ComponentModel parent) {
        return getConfigProps(parent);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        checkMandatoryConfigAttribute(FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE, "LDAP Full Name Attribute", config);


        boolean readOnly = AbstractLDAPStorageMapper.parseBooleanParameter(config, FullNameLDAPStorageMapper.READ_ONLY);
        boolean writeOnly = AbstractLDAPStorageMapper.parseBooleanParameter(config, FullNameLDAPStorageMapper.WRITE_ONLY);

        ComponentModel parent = realm.getComponent(config.getParentId());
        if (parent == null) {
            throw new ComponentValidationException("can't find parent component model");

        }
        LDAPConfig cfg = new LDAPConfig(parent.getConfig());
        UserStorageProvider.EditMode editMode = cfg.getEditMode();

        if (writeOnly && cfg.getEditMode() != UserStorageProvider.EditMode.WRITABLE) {
            throw new ComponentValidationException("ldapErrorCantWriteOnlyForReadOnlyLdap");
        }
        if (writeOnly && readOnly) {
            throw new ComponentValidationException("ldapErrorCantWriteOnlyAndReadOnly");
        }
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new FullNameLDAPStorageMapper(mapperModel, federationProvider);
    }
}
