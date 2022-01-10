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

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentFactory;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;

import javax.naming.directory.SearchControls;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrateTo1_3_0 implements Migration {
    public static final ModelVersion VERSION = new ModelVersion("1.3.0");

    public ModelVersion getVersion() {
        return VERSION;
    }

    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> migrateLDAPProviders(session, realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateLDAPProviders(session, realm);
    }

    private void migrateLDAPProviders(KeycloakSession session, RealmModel realm) {
        realm.getUserStorageProvidersStream().forEachOrdered(fedProvider -> {
            if (fedProvider.getProviderId().equals(LDAPConstants.LDAP_PROVIDER)) {
                fedProvider = new UserStorageProviderModel(fedProvider);  // copy don't want to muck with cache
                MultivaluedHashMap<String, String> config = fedProvider.getConfig();

                // Update config properties for LDAP federation provider
                if (config.get(LDAPConstants.SEARCH_SCOPE) == null) {
                    config.putSingle(LDAPConstants.SEARCH_SCOPE, String.valueOf(SearchControls.SUBTREE_SCOPE));
                }

                List<String> usersDn = config.remove("userDnSuffix");
                if (usersDn != null && !usersDn.isEmpty() && config.getFirst(LDAPConstants.USERS_DN) == null) {
                    config.put(LDAPConstants.USERS_DN, usersDn);
                }

                String usernameLdapAttribute = config.getFirst(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
                if (usernameLdapAttribute != null && config.getFirst(LDAPConstants.RDN_LDAP_ATTRIBUTE) == null) {
                    if (usernameLdapAttribute.equalsIgnoreCase(LDAPConstants.SAM_ACCOUNT_NAME)) {
                        config.putSingle(LDAPConstants.RDN_LDAP_ATTRIBUTE, LDAPConstants.CN);
                    } else {
                        config.putSingle(LDAPConstants.RDN_LDAP_ATTRIBUTE, usernameLdapAttribute);
                    }
                }

                if (config.getFirst(LDAPConstants.UUID_LDAP_ATTRIBUTE) == null) {
                    String uuidAttrName = LDAPConstants.getUuidAttributeName(config.getFirst(LDAPConstants.VENDOR));
                    config.putSingle(LDAPConstants.UUID_LDAP_ATTRIBUTE, uuidAttrName);
                }

                realm.updateComponent(fedProvider);

                // Create default mappers for LDAP
                if (realm.getComponentsStream(fedProvider.getId()).count() == 0) {
                    ProviderFactory ldapFactory = session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, LDAPConstants.LDAP_PROVIDER);
                    if (ldapFactory != null) {
                        ((ComponentFactory) ldapFactory).onCreate(session, realm, fedProvider);
                    }
                }
            }
        });
    }
}
