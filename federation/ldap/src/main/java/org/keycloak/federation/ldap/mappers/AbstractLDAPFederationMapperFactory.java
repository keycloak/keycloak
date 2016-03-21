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

import org.keycloak.Config;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.mappers.UserFederationMapperFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.UserFederationMapperSyncConfigRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPFederationMapperFactory implements UserFederationMapperFactory {

    // Used to map attributes from LDAP to UserModel attributes
    public static final String ATTRIBUTE_MAPPER_CATEGORY = "Attribute Mapper";

    // Used to map roles from LDAP to UserModel users
    public static final String ROLE_MAPPER_CATEGORY = "Role Mapper";


    // Used to map group from LDAP to UserModel users
    public static final String GROUP_MAPPER_CATEGORY = "Group Mapper";

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public UserFederationMapper create(KeycloakSession session) {
        return new LDAPFederationMapperBridge(this);
    }

    // Used just by LDAPFederationMapperBridge.
    protected abstract AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm);

    @Override
    public String getFederationProviderType() {
        return LDAPFederationProviderFactory.PROVIDER_NAME;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public UserFederationMapperSyncConfigRepresentation getSyncConfig() {
        return new UserFederationMapperSyncConfigRepresentation(false, null, false, null);
    }

    @Override
    public void close() {
    }

    public static ProviderConfigProperty createConfigProperty(String name, String label, String helpText, String type, Object defaultValue) {
        ProviderConfigProperty configProperty = new ProviderConfigProperty();
        configProperty.setName(name);
        configProperty.setLabel(label);
        configProperty.setHelpText(helpText);
        configProperty.setType(type);
        configProperty.setDefaultValue(defaultValue);
        return configProperty;
    }

    protected void checkMandatoryConfigAttribute(String name, String displayName, UserFederationMapperModel mapperModel) throws FederationConfigValidationException {
        String attrConfigValue = mapperModel.getConfig().get(name);
        if (attrConfigValue == null || attrConfigValue.trim().isEmpty()) {
            throw new FederationConfigValidationException("Missing configuration for '" + displayName + "'");
        }
    }


}
