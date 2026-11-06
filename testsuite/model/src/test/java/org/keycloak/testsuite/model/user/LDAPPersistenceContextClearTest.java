/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.user;

import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.support.EntityManagers;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assume.assumeThat;

/**
 * Federated users must stay usable after the persistence context is cleared mid-transaction.
 * <p>
 * {@code MultipleStepsExportProvider.flushUsers()} calls {@link EntityManagers#flush(KeycloakSession, boolean)}
 * with {@code clear=true} between user batches, while the whole export runs in a single transaction. Anything
 * that caches a {@link UserModel} for the lifetime of that transaction therefore ends up holding a detached
 * {@code UserEntity}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
@RequireProvider(value = UserStorageProvider.class, only = LDAPStorageProviderFactory.PROVIDER_NAME)
public class LDAPPersistenceContextClearTest extends KeycloakModelTest {

    private static final String USERNAME = "stale-user";

    private String realmId;
    private String userFederationId;
    private String userId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().createRealm("realm");
            session.getContext().setRealm(realm);
            realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
            this.realmId = realm.getId();
        });

        getParameters(UserStorageProviderModel.class).forEach(fs -> inComittedTransaction(session -> {
            if (userFederationId != null || !fs.isImportEnabled()) return;
            RealmModel r = session.realms().getRealm(realmId);
            session.getContext().setRealm(r);
            fs.setParentId(realmId);
            ComponentModel component = r.addComponentModel(fs);
            userFederationId = component.getId();
        }));

        assumeThat("Test requires an LDAP user federation provider with import enabled", userFederationId, notNullValue());

        withRealm(realmId, (session, r) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(r);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapProvider, r, USERNAME, "First", "Last", USERNAME + "@keycloak.org", null, "1234");
            return null;
        });

        // Import the user in its own transaction. The test below then loads an already existing UserEntity,
        // whose requiredActions collection is still uninitialized - which is the state the export hits.
        this.userId = withRealm(realmId, (session, r) -> {
            UserModel user = session.users().getUserByUsername(r, USERNAME);
            assertThat(user, notNullValue());
            return user.getId();
        });
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);

        ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(s, ldapModel);
        LDAPTestUtils.removeAllLDAPUsers(ldapProvider, realm);

        s.realms().removeRealm(realmId);
    }

    @Test
    public void federatedUserIsUsableAfterPersistenceContextClear() {
        withRealm(realmId, (session, realm) -> {
            // kc.sh export runs in a freshly started process, so its user cache is cold. Without this the
            // first read below is served from the cache populated while importing the user, never reaches
            // the LDAP provider, and therefore never enlists the user in LDAPStorageUserManager.
            UserCache userCache = UserStorageUtil.userCache(session);
            if (userCache != null) {
                userCache.clear();
            }

            // The export runs the whole realm + every user batch inside a single batched transaction,
            // via ExportImportSessionTask.runTask(factory, Mode.BATCHED).
            EntityManagers.runInBatch(session, () -> {
                // First read enlists the user in LDAPStorageUserManager, which caches the proxied UserModel -
                // and with it the UserEntity managed by the current persistence context - for the whole
                // transaction. Required actions are deliberately not read, so the collection stays uninitialized.
                UserModel user = session.users().getUserById(realm, userId);
                assertThat(user, notNullValue());

                // What MultipleStepsExportProvider.flushUsers() does between user batches.
                EntityManagers.flush(session, true);

                // Before the fix this throws LazyInitializationException: the LDAP provider hands back the
                // proxy cached above, which is still backed by the now detached UserEntity.
                UserModel reloaded = session.users().getUserById(realm, userId);
                assertThat(reloaded.getRequiredActionsStream().collect(Collectors.toSet()), empty());
            }, true);
            return null;
        });
    }

    @Test
    public void federatedUserIsWritableAfterPersistenceContextClear() {
        withRealm(realmId, (session, realm) -> {
            UserCache userCache = UserStorageUtil.userCache(session);
            if (userCache != null) {
                userCache.clear();
            }

            EntityManagers.runInBatch(session, () -> {
                // Enlists the user in LDAPStorageUserManager, including the LDAPTransaction that writes will
                // need later in this same transaction.
                UserModel user = session.users().getUserById(realm, userId);
                assertThat(user, notNullValue());

                // What MultipleStepsExportProvider.flushUsers() does between user batches.
                EntityManagers.flush(session, true);

                // Before the fix, invalidating the cached proxy on clear by simply dropping its managed-user
                // entry would make this throw IllegalStateException from LDAPStorageUserManager.getTransaction,
                // since the entry (and with it the already-enlisted LDAPTransaction) would be gone.
                UserModel reloaded = session.users().getUserById(realm, userId);
                reloaded.setEmail("changed-" + USERNAME + "@keycloak.org");
            }, true);
            return null;
        });
    }
}
