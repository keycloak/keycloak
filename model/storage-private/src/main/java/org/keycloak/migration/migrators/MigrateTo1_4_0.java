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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.component.ComponentModel;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.StorageProviderRealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.DefaultRequiredActions;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageUtil;

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
        session.realms().getRealmsStream().forEach(realm -> migrateRealm(session, realm));
    }

    protected void migrateRealm(KeycloakSession session, RealmModel realm) {
        if (realm.getAuthenticationFlowsStream().count() == 0) {
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
        ((StorageProviderRealmModel) realm).getUserStorageProvidersStream()
                .filter(providerModel -> Objects.equals(providerModel.getProviderId(), LDAPConstants.LDAP_PROVIDER))
                .forEachOrdered(providerModel -> realm.getComponentsStream(providerModel.getId())
                        .filter(mapper -> mandatoryInLdap.contains(mapper.getName()))
                        .forEach(mapper -> {
                            mapper = new ComponentModel(mapper);  // don't want to modify cache
                            mapper.getConfig().putSingle("is.mandatory.in.ldap", "true");
                            realm.updateComponent(mapper);
                        }));
    }

    private void migrateUsers(KeycloakSession session, RealmModel realm) {
        Map<String, String> searchAttributes = new HashMap<>(1);
        searchAttributes.put(UserModel.INCLUDE_SERVICE_ACCOUNT, Boolean.FALSE.toString());

        UserStoragePrivateUtil.userLocalStorage(session).searchForUserStream(realm, searchAttributes)
                .forEach(user -> {
                    String email = KeycloakModelUtils.toLowerCaseSafe(user.getEmail());
                    if (email != null && !email.equals(user.getEmail())) {
                        user.setEmail(email);
                        UserCache userCache = UserStorageUtil.userCache(session);
                        if (userCache != null) {
                            userCache.evict(realm, user);
                        }
                    }
                });
    }
}
