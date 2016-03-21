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

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HardcodedLDAPRoleMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID = "hardcoded-ldap-role-mapper";
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty roleAttr = createConfigProperty(HardcodedLDAPRoleMapper.ROLE, "Role",
                "Role to grant to user.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To reference an application role the syntax is appname.approle, i.e. myapp.myrole",
                ProviderConfigProperty.ROLE_TYPE, null);
        configProperties.add(roleAttr);
    }

    @Override
    public String getHelpText() {
        return "When user is imported from LDAP, he will be automatically added into this configured role.";
    }

    @Override
    public String getDisplayCategory() {
        return ROLE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Role";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        return new HashMap<>();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationProviderModel fedProviderModel, UserFederationMapperModel mapperModel) throws FederationConfigValidationException {
        String roleName = mapperModel.getConfig().get(HardcodedLDAPRoleMapper.ROLE);
        if (roleName == null) {
            throw new FederationConfigValidationException("Role can't be null");
        }
        RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
        if (role == null) {
            throw new FederationConfigValidationException("There is no role corresponding to configured value");
        }
    }

    @Override
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new HardcodedLDAPRoleMapper(mapperModel, federationProvider, realm);
    }
}
