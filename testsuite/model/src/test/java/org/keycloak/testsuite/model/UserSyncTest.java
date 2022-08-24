/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.managers.UserStorageSyncManager;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;

@RequireProvider(UserProvider.class)
@RequireProvider(ClusterProvider.class)
@RequireProvider(RealmProvider.class)
@RequireProvider(value = UserStorageProvider.class, only = LDAPStorageProviderFactory.PROVIDER_NAME)
public class UserSyncTest extends KeycloakModelTest {

    private static final int NUMBER_OF_USERS = 5000;
    private String realmId;
    private String userFederationId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().createRealm("realm");
            realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
            this.realmId = realm.getId();
        });

        getParameters(UserStorageProviderModel.class).forEach(fs -> inComittedTransaction(session -> {
            if (userFederationId != null || !fs.isImportEnabled()) return;
            RealmModel realm = session.realms().getRealm(realmId);

            fs.setParentId(realmId);

            ComponentModel res = realm.addComponentModel(fs);

            // Check if the provider implements ImportSynchronization interface
            UserStorageProviderFactory userStorageProviderFactory = (UserStorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, res.getProviderId());
            if (!ImportSynchronization.class.isAssignableFrom(userStorageProviderFactory.getClass())) {
                return;
            }

            userFederationId = res.getId();
            log.infof("Added %s user federation provider: %s", fs.getName(), res.getId());
        }));

        assumeThat("Cannot run UserSyncTest because there is no user federation provider that supports sync", userFederationId, notNullValue());
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Override
    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return true;
    }

    @Test
    public void testManyUsersImport() {
        IntStream.range(0, NUMBER_OF_USERS).parallel().forEach(index -> inComittedTransaction(index, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "user" + i, "User" + i + "FN", "User" + i + "LN", "user" + i + "@email.org", null, "12" + i);
            return null;
        }));

        assertThat(withRealm(realmId, (session, realm) -> UserStoragePrivateUtil.userLocalStorage(session).getUsersCount(realm)), is(0));

        long start = System.currentTimeMillis();
        SynchronizationResult res = withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            return new UserStorageSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), realm.getId(), providerModel);
        });
        long end = System.currentTimeMillis();
        long timeNeeded = end - start;

        // The sync shouldn't take more than 18 second per user
        assertThat(String.format("User sync took %f seconds per user, but it should take less than 18 seconds", 
                (float)(timeNeeded) / NUMBER_OF_USERS), timeNeeded, Matchers.lessThan((long) (18 * NUMBER_OF_USERS)));
        assertThat(res.getAdded(), is(NUMBER_OF_USERS));
        assertThat(withRealm(realmId, (session, realm) -> UserStoragePrivateUtil.userLocalStorage(session).getUsersCount(realm)), is(NUMBER_OF_USERS));
    }
}

