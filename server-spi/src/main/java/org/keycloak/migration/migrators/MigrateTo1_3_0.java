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

package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationEventAwareProviderFactory;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.SearchControls;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrateTo1_3_0 {
    public static final ModelVersion VERSION = new ModelVersion("1.3.0");


    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            migrateLDAPProviders(session, realm);
        }

    }

    private void migrateLDAPProviders(KeycloakSession session, RealmModel realm) {
        List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();
        for (UserFederationProviderModel fedProvider : federationProviders) {

            if (fedProvider.getProviderName().equals(LDAPConstants.LDAP_PROVIDER)) {
                Map<String, String> config = fedProvider.getConfig();

                // Update config properties for LDAP federated provider
                if (config.get(LDAPConstants.SEARCH_SCOPE) == null) {
                    config.put(LDAPConstants.SEARCH_SCOPE, String.valueOf(SearchControls.SUBTREE_SCOPE));
                }

                String usersDn = config.remove("userDnSuffix");
                if (usersDn != null && config.get(LDAPConstants.USERS_DN) == null) {
                    config.put(LDAPConstants.USERS_DN, usersDn);
                }

                String usernameLdapAttribute = config.get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
                if (usernameLdapAttribute != null && config.get(LDAPConstants.RDN_LDAP_ATTRIBUTE) == null) {
                    if (usernameLdapAttribute.equalsIgnoreCase(LDAPConstants.SAM_ACCOUNT_NAME)) {
                        config.put(LDAPConstants.RDN_LDAP_ATTRIBUTE, LDAPConstants.CN);
                    } else {
                        config.put(LDAPConstants.RDN_LDAP_ATTRIBUTE, usernameLdapAttribute);
                    }
                }

                if (config.get(LDAPConstants.UUID_LDAP_ATTRIBUTE) == null) {
                    String uuidAttrName = LDAPConstants.getUuidAttributeName(config.get(LDAPConstants.VENDOR));
                    config.put(LDAPConstants.UUID_LDAP_ATTRIBUTE, uuidAttrName);
                }

                realm.updateUserFederationProvider(fedProvider);

                // Create default mappers for LDAP
                Set<UserFederationMapperModel> mappers = realm.getUserFederationMappersByFederationProvider(fedProvider.getId());
                if (mappers.isEmpty()) {
                    UserFederationProviderFactory ldapFactory = (UserFederationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, LDAPConstants.LDAP_PROVIDER);
                    if (ldapFactory != null) {
                        ((UserFederationEventAwareProviderFactory) ldapFactory).onProviderModelCreated(realm, fedProvider);
                    }
                }
            }
        }
    }
}
