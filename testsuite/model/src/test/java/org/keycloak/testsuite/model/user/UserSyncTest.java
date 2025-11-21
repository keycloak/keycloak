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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import javax.naming.directory.BasicAttribute;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPOperationManager;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapperFactory;
import org.keycloak.storage.managers.UserStorageSyncManager;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.keycloak.models.LDAPConstants.LDAP_ID;
import static org.keycloak.storage.UserStorageProviderModel.REMOVE_INVALID_USERS_ENABLED;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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
            return UserStorageSyncManager.syncAllUsers(session.getKeycloakSessionFactory(), realm.getId(), providerModel);
        });
        long end = System.currentTimeMillis();
        long timeNeeded = end - start;

        // The sync shouldn't take more than 18 second per user
        assertThat(String.format("User sync took %f seconds per user, but it should take less than 18 seconds",
                (float) (timeNeeded) / NUMBER_OF_USERS), timeNeeded, Matchers.lessThan((long) (18 * NUMBER_OF_USERS)));
        assertThat(res.getAdded(), is(NUMBER_OF_USERS));
        assertThat(withRealm(realmId, (session, realm) -> UserStoragePrivateUtil.userLocalStorage(session).getUsersCount(realm)), is(NUMBER_OF_USERS));
    }

    @Test
    public void testRemovedLDAPUserShouldNotFailGetUserByEmail() {
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            // disable cache
            providerModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            realm.updateComponent(providerModel);

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "user", "UserFN", "UserLN", "user@email.org", "userStreet", "1450");
            return null;
        });

        assertThat(withRealm(realmId, (session, realm) -> session.users().getUserByEmail(realm, "user@email.org")), is(notNullValue()));

        withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeLDAPUserByUsername(ldapFedProvider, realm, ldapFedProvider.getLdapIdentityStore().getConfig(), "user");
            return null;
        });

        assertThat(withRealm(realmId, (session, realm) -> session.users().getUserByEmail(realm, "user@email.org")), is(nullValue()));
    }

    @Test
    public void testAlwaysReadValueFromLDAPWorksWithNoCachePolicy() {
        // Create mapper from sn to a new user attribute
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            // disable cache
            providerModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            realm.updateComponent(providerModel);

            // Create mapper
            ComponentModel mapperModel = KeycloakModelUtils.createComponentModel("My-street-mapper", providerModel.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, LDAPConstants.STREET,
                    UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.STREET,
                    UserAttributeLDAPStorageMapper.READ_ONLY, "false",
                    UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true",
                    UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "false");
            realm.addComponentModel(mapperModel);

            return null;
        });

        final String MY_STREET_NAME = "my-street 9";

        // create 1 user in LDAP
        withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            int i = 1;
            LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "user" + i, "User" + i + "FN", "User" + i + "LN", "user" + i + "@email.org", MY_STREET_NAME, "12" + i);
            return null;
        });


        // Read attribute that should be mapped by created mapper
        String id = withRealm(realmId, (session, realm) -> {
            UserModel user1 = session.users().getUserByUsername(realm, "user1");
            assertThat(user1.getAttributes().get(LDAPConstants.STREET).get(0), is(equalTo(MY_STREET_NAME)));
            return user1.getId();
        });

        // Remove attribute from the LDAP for given user
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, providerModel);

            LDAPObject user1LdapObject = ldapFedProvider.loadLDAPUserByUsername(realm, "user1");
            LDAPOperationManager ldapOperationManager = new LDAPOperationManager(session, ldapFedProvider.getLdapIdentityStore().getConfig());

            ldapOperationManager.removeAttribute(user1LdapObject.getDn().getLdapName(), new BasicAttribute(LDAPConstants.STREET));
            return null;
        });

        // Check local storage contains the old value
        withRealm(realmId, (session, realm) -> {
            UserModel user1 = UserStoragePrivateUtil.userLocalStorage(session).getUserById(realm, id);
            assertThat(user1.getAttributes().get(LDAPConstants.STREET).get(0), is(equalTo(MY_STREET_NAME)));
            return user1.getId();
        });

        // Read from Keycloak by id
        withRealm(realmId, (session, realm) -> {
            UserModel user1 = session.users().getUserById(realm, id);
            assertThat(user1.getAttributes().get(LDAPConstants.STREET), is(nullValue()));
            assertThat(user1.getFirstAttribute(LDAPConstants.STREET), is(nullValue()));
            assertThat(user1.getAttributeStream(LDAPConstants.STREET).findFirst().isPresent(), is(false));
            return null;
        });

        // Read from Keycloak by query
        withRealm(realmId, (session, realm) -> {
            UserModel user1 = session.users().searchForUserStream(realm, Collections.emptyMap()).findFirst().orElse(null);
            assertThat(user1.getAttributes().get(LDAPConstants.STREET), is(nullValue()));
            assertThat(user1.getFirstAttribute(LDAPConstants.STREET), is(nullValue()));
            assertThat(user1.getAttributeStream(LDAPConstants.STREET).findFirst().isPresent(), is(false));
            return null;
        });
    }

    @Test
    public void testInvalidUsersAreDeleted() {
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            providerModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            realm.updateComponent(providerModel);
            return null;
        });

        // create user1 in LDAP
        withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            int i = 1;
            LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "user" + i, "User" + i + "FN", "User" + i + "LN", "user" + i + "@email.org", "my-street 9", "12" + i);
            return null;
        });

        // import user
        String oldUserId = withRealm(realmId, (session, realm) -> {
            UserModel user1 = session.users().getUserByUsername(realm, "user1");
            user1.setSingleAttribute("LDAP_ID", "WRONG");
            return user1.getId();
        });

        // validate imported user, user will be deleted and re-created
        withRealm(realmId, (session, realm) -> {
            UserModel user = session.users().getUserByUsername(realm, "user1");
            assertThat(user, notNullValue());
            assertThat(user.getId(), not(equalTo(oldUserId)));
            return null;
        });
    }

    @Test
    public void testInvalidUsersAreNotDeleted() {
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            providerModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            providerModel.getConfig().putSingle(REMOVE_INVALID_USERS_ENABLED, "false"); // prevent local delete
            realm.updateComponent(providerModel);
            return null;
        });

        // create user1 in LDAP
        withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            int i = 1;
            LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "user" + i, "User" + i + "FN", "User" + i + "LN", "user" + i + "@email.org", "my-street 9", "12" + i);
            return null;
        });

        AtomicReference<String> ldapId = new AtomicReference<>();

        // import user
        withRealm(realmId, (session, realm) -> {
            UserModel user1 = session.users().getUserByUsername(realm, "user1");
            ldapId.set(user1.getFirstAttribute(LDAPConstants.LDAP_ID));
            user1.setSingleAttribute(LDAPConstants.LDAP_ID, "WRONG");
            return user1;
        });

        // validate imported user
        withRealm(realmId, (session, realm) -> {
            UserModel user = session.users().getUserByUsername(realm, "user1");
            assertThat(user, is(notNullValue()));
            assertThat(user.isEnabled(), is(false));
            UserModel deletedUser = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, "user1");
            assertThat(deletedUser, is(notNullValue()));
            return deletedUser;
        });


        // remove user1 from LDAP
        withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            UserModel user = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, "user1");
            user.setSingleAttribute(LDAPConstants.LDAP_ID, ldapId.get());
            assertThat(ldapFedProvider.removeUser(realm, user), is(true));
            return null;
        });

        // can delete the local user
        withRealm(realmId, (session, realm) -> {
            UserModel user = session.users().getUserByUsername(realm, "user1");
            assertThat(session.users().removeUser(realm, user), is(true));
            user = session.users().getUserByUsername(realm, "user1");
            assertThat(user, is(nullValue()));
            return null;
        });
    }

    @Test
    public void testInvalidUsernameWhenDifferentThanExternalStorage() {
        withRealm(realmId, (session, realm) -> {
            UserStorageProviderModel providerModel = new UserStorageProviderModel(realm.getComponent(userFederationId));
            providerModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            providerModel.getConfig().putSingle(REMOVE_INVALID_USERS_ENABLED, Boolean.FALSE.toString()); // prevent local delete
            realm.updateComponent(providerModel);
            return null;
        });

        // create user1 in LDAP
        String ldapId = withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            int i = 1;
            LDAPObject ldapObject = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "user" + i, "User" + i + "FN", "User" + i + "LN", "user" + i + "@email.org", "my-street 9", "12" + i);
            return ldapObject.getUuid();
        });

        // import user
        String userId = withRealm(realmId, (session, realm) -> {
            return session.users().getUserByUsername(realm, "user1").getId();
        });

        withRealm(realmId, (session, realm) -> {
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject ldapObject = provider.loadLDAPUserByUuid(realm, ldapId);
            ldapObject.setSingleAttribute(LDAPConstants.UID, "changed");
            provider.getLdapIdentityStore().update(ldapObject);
            return null;
        });

        // user id changed, user cannot be resolved
        withRealm(realmId, (session, realm) -> {
            assertThat(session.users().getUserByUsername(realm, "user1"), nullValue());
            return null;
        });

        // cache and local database reflecting the change in the database for the existing account
        withRealm(realmId, (session, realm) -> {
            UserModel user = session.users().getUserByUsername(realm, "changed");
            assertThat(user.getFirstAttribute(LDAP_ID), is(ldapId));
            assertThat(user.getId(), is(userId));
            assertThat(user.getUsername(), is("changed"));
            UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserById(realm, userId);
            assertThat(localUser.getUsername(), is("changed"));
            return null;
        });
    }
}
