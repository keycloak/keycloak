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

package org.keycloak.federation.ldap.mappers.msad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.mappers.AbstractLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.AbstractLDAPFederationMapperFactory;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MSADUserAccountControlMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID = LDAPConstants.MSAD_USER_ACCOUNT_CONTROL_MAPPER;
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
    }

    @Override
    public String getHelpText() {
        return "Mapper specific to MSAD. It's able to integrate the MSAD user account state into Keycloak account state (account enabled, password is expired etc). It's using userAccountControl and pwdLastSet MSAD attributes for that. " +
                "For example if pwdLastSet is 0, the Keycloak user is required to update password, if userAccountControl is 514 (disabled account) the Keycloak user is disabled as well etc. Mapper is also able to handle exception code from LDAP user authentication.";
    }

    @Override
    public String getDisplayCategory() {
        return ATTRIBUTE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "MSAD User Account Control";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        return Collections.emptyMap();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationProviderModel fedProviderModel, UserFederationMapperModel mapperModel) throws FederationConfigValidationException {
    }

    @Override
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new MSADUserAccountControlMapper(mapperModel, federationProvider, realm);
    }
}
