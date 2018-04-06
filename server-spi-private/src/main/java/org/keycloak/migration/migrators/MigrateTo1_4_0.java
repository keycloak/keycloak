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

import org.keycloak.component.ComponentModel;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.DefaultRequiredActions;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStorageProviderModel;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrateTo1_4_0 implements Migration {
    public static final ModelVersion VERSION = new ModelVersion("1.4.0");
    public ModelVersion getVersion() {
        return VERSION;
    }

    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            migrateRealm(session, realm);
        }

    }

    protected void migrateRealm(KeycloakSession session, RealmModel realm) {
        if (realm.getAuthenticationFlows().size() == 0) {
            DefaultAuthenticationFlows.migrateFlows(realm);
            DefaultRequiredActions.addActions(realm);
        }
        ImpersonationConstants.setupImpersonationService(session, realm);

        migrateLDAPMappers(session, realm);
        migrateUsers(session, realm);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(session, realm);

    }

    private void migrateLDAPMappers(KeycloakSession session, RealmModel realm) {
        List<String> mandatoryInLdap = Arrays.asList("username", "username-cn", "first name", "last name");
        for (UserStorageProviderModel providerModel : realm.getUserStorageProviders()) {
            if (providerModel.getProviderId().equals(LDAPConstants.LDAP_PROVIDER)) {
                List<ComponentModel> mappers = realm.getComponents(providerModel.getId());
                for (ComponentModel mapper : mappers) {
                    if (mandatoryInLdap.contains(mapper.getName())) {
                        mapper = new ComponentModel(mapper);  // don't want to modify cache
                        mapper.getConfig().putSingle("is.mandatory.in.ldap", "true");
                        realm.updateComponent(mapper);
                    }

                }
            }
        }
    }

    private void migrateUsers(KeycloakSession session, RealmModel realm) {
        List<UserModel> users = session.userLocalStorage().getUsers(realm, false);
        for (UserModel user : users) {
            String email = user.getEmail();
            email = KeycloakModelUtils.toLowerCaseSafe(email);
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                UserCache userCache = session.userCache();
                if (userCache != null) {
                    userCache.evict(realm, user);
                }
            }
        }
    }
}
