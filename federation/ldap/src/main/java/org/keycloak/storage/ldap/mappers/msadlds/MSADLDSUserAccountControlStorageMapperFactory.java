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

package org.keycloak.storage.ldap.mappers.msadlds;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:slawomir@dabek.name">Slawomir Dabek</a>
 */
public class MSADLDSUserAccountControlStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = LDAPConstants.MSADLDS_USER_ACCOUNT_CONTROL_MAPPER;
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
    }

    @Override
    public String getHelpText() {
        return "Mapper specific to MSAD LDS. It's able to integrate the MSAD LDS user account state into Keycloak account state (account enabled, password is expired etc). It's using msDS-UserAccountDisabled and pwdLastSet MSAD attributes for that. " +
                "For example if pwdLastSet is 0, the Keycloak user is required to update password, if msDS-UserAccountDisabled is 'TRUE' the Keycloak user is disabled as well etc. Mapper is also able to handle exception code from LDAP user authentication.";
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
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new MSADLDSUserAccountControlStorageMapper(mapperModel, federationProvider);
    }
}
