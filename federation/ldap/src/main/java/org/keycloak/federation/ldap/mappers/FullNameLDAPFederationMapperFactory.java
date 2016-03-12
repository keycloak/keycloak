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

package org.keycloak.federation.ldap.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FullNameLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID =  "full-name-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty userModelAttribute = createConfigProperty(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, "LDAP Full Name Attribute",
                "Name of LDAP attribute, which contains fullName of user. Usually it will be 'cn' ", ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(userModelAttribute);

        ProviderConfigProperty readOnly = createConfigProperty(FullNameLDAPFederationMapper.READ_ONLY, "Read Only",
                "For Read-only is data imported from LDAP to Keycloak DB, but it's not saved back to LDAP when user is updated in Keycloak.", ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(readOnly);

        ProviderConfigProperty writeOnly = createConfigProperty(FullNameLDAPFederationMapper.WRITE_ONLY, "Write Only",
                "For Write-only is data propagated to LDAP when user is created or updated in Keycloak. But this mapper is not used to propagate data from LDAP back into Keycloak. " +
                        "This setting is useful if you configured separate firstName and lastName attribute mappers and you want to use those to read attribute from LDAP into Keycloak", ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(writeOnly);
    }

    @Override
    public String getHelpText() {
        return "Used to map full-name of user from single attribute in LDAP (usually 'cn' attribute) to firstName and lastName attributes of UserModel in Keycloak DB";
    }

    @Override
    public String getDisplayCategory() {
        return ATTRIBUTE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Full Name";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        Map<String, String> defaultValues = new HashMap<>();
        LDAPConfig config = new LDAPConfig(providerModel.getConfig());

        defaultValues.put(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN);

        boolean readOnly = config.getEditMode() != UserFederationProvider.EditMode.WRITABLE;
        defaultValues.put(FullNameLDAPFederationMapper.READ_ONLY, String.valueOf(readOnly));

        String writeOnly = String.valueOf(!readOnly);
        defaultValues.put(FullNameLDAPFederationMapper.WRITE_ONLY, writeOnly);

        return defaultValues;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationProviderModel fedProviderModel, UserFederationMapperModel mapperModel) throws FederationConfigValidationException {
        checkMandatoryConfigAttribute(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, "LDAP Full Name Attribute", mapperModel);

        boolean readOnly = AbstractLDAPFederationMapper.parseBooleanParameter(mapperModel, FullNameLDAPFederationMapper.READ_ONLY);
        boolean writeOnly = AbstractLDAPFederationMapper.parseBooleanParameter(mapperModel, FullNameLDAPFederationMapper.WRITE_ONLY);

        LDAPConfig cfg = new LDAPConfig(fedProviderModel.getConfig());
        UserFederationProvider.EditMode editMode = cfg.getEditMode();

        if (writeOnly && cfg.getEditMode() != UserFederationProvider.EditMode.WRITABLE) {
            throw new FederationConfigValidationException("ldapErrorCantWriteOnlyForReadOnlyLdap");
        }
        if (writeOnly && readOnly) {
            throw new FederationConfigValidationException("ldapErrorCantWriteOnlyAndReadOnly");
        }
    }

    @Override
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new FullNameLDAPFederationMapper(mapperModel, federationProvider, realm);
    }
}
