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

package org.keycloak.testsuite.model.user;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.Test;

import static org.keycloak.models.LDAPConstants.LDAP_ID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeThat;

@RequireProvider(UserProvider.class)
@RequireProvider(ClusterProvider.class)
@RequireProvider(RealmProvider.class)
@RequireProvider(value = UserStorageProvider.class, only = LDAPStorageProviderFactory.PROVIDER_NAME)
public class FederatedUserTest extends KeycloakModelTest {

    private String realmId;
    private String userFederationId;

    @Override
    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return true;
    }

    @Override
    public void createEnvironment(KeycloakSession s) {
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().createRealm("realm");
            s.getContext().setRealm(realm);
            realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
            this.realmId = realm.getId();
        });

        getParameters(UserStorageProviderModel.class).forEach(fs -> inComittedTransaction(session -> {
            if (userFederationId != null || !fs.isImportEnabled()) return;
            RealmModel realm = session.realms().getRealm(realmId);
            s.getContext().setRealm(realm);

            fs.setParentId(realmId);

            ComponentModel res = realm.addComponentModel(fs);

            // Check if the provider implements ImportSynchronization interface
            UserStorageProviderFactory userStorageProviderFactory = (UserStorageProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, res.getProviderId());
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
        final RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);

        ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
        LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(s, ldapModel);
        LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, realm);

        s.realms().removeRealm(realmId);
    }

    private record TestContext (KeycloakSession session, RealmModel realm, String previousUserId, String previousLdapId) {};

    private void assertAttributeDifferentThanExternalStorage(Consumer<TestContext> assertion) {
        // create user1 in LDAP
        String ldapId = withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject ldapObject = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "user1", "User1" + "FN", "User1" + "LN", "user1@email.org", "my-street 9", "12");
            return ldapObject.getUuid();
        });

        // import user
        String previous = withRealm(realmId, (session, realm) ->
                session.users().getUserByUsername(realm, "user1").getId());

        withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject ldapObject = provider.loadLDAPUserByUuid(realm, ldapId);
            ldapObject.setSingleAttribute(LDAPConstants.UID, "changed");
            provider.getLdapIdentityStore().update(ldapObject);
            return null;
        });

        withRealm(realmId, (BiFunction<KeycloakSession, RealmModel, Void>) (session, realm) -> {
            assertion.accept(new TestContext(session, realm, previous, ldapId));
            return null;
        });
    }

    @Test
    public void testInvalidUsernameWhenDifferentThanExternalStorageNoCache() {
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            providerModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            realm.updateComponent(providerModel);
            return null;
        });

        assertAttributeDifferentThanExternalStorage((context) -> {
            KeycloakSession session = context.session();
            RealmModel realm = context.realm();
            UserModel cached = session.users().getUserByUsername(realm, "user1");
            assertThat(cached, nullValue());
            UserModel user = session.users().getUserByUsername(realm, "changed");
            assertThat(user.getFirstAttribute(LDAP_ID), is(context.previousLdapId()));
            assertThat(user.getId(), is(context.previousUserId()));
            assertThat(user.getUsername(), is("changed"));
            UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserById(realm, context.previousUserId());
            assertThat(localUser.getUsername(), is("changed"));
        });
    }

    @Test
    public void testInvalidUsernameWhenDifferentThanExternalStorageWithCache() {
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            providerModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.DEFAULT);
            realm.updateComponent(providerModel);
            return null;
        });

        assertAttributeDifferentThanExternalStorage((context) -> {
            KeycloakSession session = context.session();
            RealmModel realm = context.realm();
            // cache not yet invalidated, set a max lifespan if you want to eventually invalidate federated users
            UserModel cached = session.users().getUserByUsername(realm, "user1");
            assertThat(cached, notNullValue());
            UserModel user = session.users().getUserByUsername(realm, "changed");
            assertThat(user.getFirstAttribute(LDAP_ID), is(context.previousLdapId()));
            assertThat(user.getId(), is(context.previousUserId()));
            assertThat(user.getUsername(), is("changed"));
            UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserById(realm, context.previousUserId());
            assertThat(localUser.getUsername(), is("changed"));
            // cache now invalidated
            cached = session.users().getUserByUsername(realm, "user1");
            assertThat(cached, is(nullValue()));
        });
    }

    @Test
    public void testInvalidUsernameWhenDifferentThanExternalStorageWithCacheMaxLifespan() {
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            providerModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.MAX_LIFESPAN);
            providerModel.setMaxLifespan(60000);
            realm.updateComponent(providerModel);
            return null;
        });

        assertAttributeDifferentThanExternalStorage((context) -> {
            KeycloakSession session = context.session();
            RealmModel realm = context.realm();
            UserModel cached = session.users().getUserByUsername(realm, "user1");
            assertThat(cached, notNullValue());
            setTimeOffset(120000);
            cached = session.users().getUserByUsername(realm, "user1");
            assertThat(cached, nullValue());
            UserModel user = session.users().getUserByUsername(realm, "changed");
            assertThat(user, notNullValue());
            assertThat(user.getFirstAttribute(LDAP_ID), is(context.previousLdapId()));
            assertThat(user.getId(), is(context.previousUserId()));
            assertThat(user.getUsername(), is("changed"));
            UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserById(realm, context.previousUserId());
            assertThat(localUser.getUsername(), is("changed"));
            cached = session.users().getUserByUsername(realm, "user1");
            assertThat(cached, nullValue());
        });
    }
}
