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
package org.keycloak.tests.model;

import java.util.function.Consumer;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.infinispan.entities.CachedUser;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.CacheableStorageProviderModel.CachePolicy;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.providers.federation.CountingUserStorageProvider;
import org.keycloak.tests.providers.federation.CountingUserStorageProviderFactory;

import org.infinispan.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies that the user cache stores the username and the email lookup query, also when the user was already loaded
 * (and thereby managed) in the same session by the respective other lookup. This is what happens on a login by email
 * when the user profile is validated afterwards.
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class UserCacheQueryTest {

    private static final String REALM_NAME = "user-cache-query";
    private static final String USERNAME = "alice";
    private static final String EMAIL = "alice@example.org";
    private static final String LOCAL_USERNAME = "bob";
    private static final String LOCAL_EMAIL = "bob@example.org";
    private static final String COLLIDING_REALM_1 = "user-cache-query-collision-1";
    private static final String COLLIDING_REALM_2 = "user-cache-query-collision-2";

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void before() {
        runOnServer.run(UserCacheQueryTest::setup);
    }

    @AfterEach
    public void after() {
        runOnServer.run(UserCacheQueryTest::tearDown);
    }

    private static void setup(KeycloakSession session) {
        CountingUserStorageProvider.reset();
        CountingUserStorageProvider.addUser(USERNAME, EMAIL);

        inSession(session, s -> {
            RealmModel realm = new RealmManager(s).createRealm(REALM_NAME);
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setName("counting");
            model.setProviderId(CountingUserStorageProviderFactory.PROVIDER_ID);
            model.setParentId(realm.getId());
            model.setCachePolicy(CachePolicy.MAX_LIFESPAN);
            model.setMaxLifespan(10 * 60 * 1000);
            realm.addComponentModel(model);
        });
    }

    private static void tearDown(KeycloakSession session) {
        inSession(session, s -> {
            RealmManager realmManager = new RealmManager(s);
            RealmModel realm = realmManager.getRealmByName(REALM_NAME);
            if (realm != null) {
                realmManager.removeRealm(realm);
            }
        });
        CountingUserStorageProvider.reset();
    }

    @TestOnServer
    public void usernameQueryIsCachedAfterEmailLookup(KeycloakSession session) {
        // cold: both lookups reach the store once
        inSession(session, s -> {
            lookupByEmail(s);
            lookupByUsername(s);
        });
        assertThat(CountingUserStorageProvider.BY_EMAIL_CALLS.get(), equalTo(1));
        assertThat(CountingUserStorageProvider.BY_USERNAME_CALLS.get(), equalTo(1));

        // warm: neither lookup reaches the store anymore, no matter how often it is repeated
        CountingUserStorageProvider.resetCounters();
        for (int i = 0; i < 3; i++) {
            inSession(session, s -> {
                lookupByEmail(s);
                lookupByUsername(s);
            });
        }
        assertThat(CountingUserStorageProvider.BY_EMAIL_CALLS.get(), equalTo(0));
        assertThat(CountingUserStorageProvider.BY_USERNAME_CALLS.get(), equalTo(0));
    }

    @TestOnServer
    public void emailQueryIsCachedAfterUsernameLookup(KeycloakSession session) {
        inSession(session, s -> {
            lookupByUsername(s);
            lookupByEmail(s);
        });
        assertThat(CountingUserStorageProvider.BY_USERNAME_CALLS.get(), equalTo(1));
        assertThat(CountingUserStorageProvider.BY_EMAIL_CALLS.get(), equalTo(1));

        CountingUserStorageProvider.resetCounters();
        for (int i = 0; i < 3; i++) {
            inSession(session, s -> {
                lookupByUsername(s);
                lookupByEmail(s);
            });
        }
        assertThat(CountingUserStorageProvider.BY_USERNAME_CALLS.get(), equalTo(0));
        assertThat(CountingUserStorageProvider.BY_EMAIL_CALLS.get(), equalTo(0));
    }

    @TestOnServer
    public void queriesAreNotCachedForUncachedUsers(KeycloakSession session) {
        inSession(session, s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            ComponentModel component = realm.getComponentsStream(realm.getId(), "org.keycloak.storage.UserStorageProvider").findFirst().orElseThrow();
            UserStorageProviderModel model = new UserStorageProviderModel(component);
            model.setCachePolicy(CachePolicy.NO_CACHE);
            realm.updateComponent(model);
        });

        CountingUserStorageProvider.resetCounters();
        for (int i = 0; i < 2; i++) {
            inSession(session, s -> {
                assertThat(lookupByEmail(s), not(instanceOf(CachedUserModel.class)));
                assertThat(lookupByUsername(s), not(instanceOf(CachedUserModel.class)));
            });
        }
        assertThat(CountingUserStorageProvider.BY_EMAIL_CALLS.get(), equalTo(2));
        assertThat(CountingUserStorageProvider.BY_USERNAME_CALLS.get(), equalTo(2));
    }

    @TestOnServer
    public void queryIsNotCachedForUserInvalidatedInSession(KeycloakSession session) {
        inSession(session, s -> {
            UserModel user = lookupByEmail(s);
            ((CachedUserModel) user).invalidate();
            lookupByUsername(s);
        });

        // the invalidated user must not have left a username query behind, the store is asked again...
        CountingUserStorageProvider.resetCounters();
        inSession(session, s -> {
            lookupByEmail(s);
            lookupByUsername(s);
        });
        assertThat(CountingUserStorageProvider.BY_USERNAME_CALLS.get(), equalTo(1));

        // ...and from then on the queries are cached again
        CountingUserStorageProvider.resetCounters();
        inSession(session, s -> {
            lookupByEmail(s);
            lookupByUsername(s);
        });
        assertThat(CountingUserStorageProvider.BY_EMAIL_CALLS.get(), equalTo(0));
        assertThat(CountingUserStorageProvider.BY_USERNAME_CALLS.get(), equalTo(0));
    }

    @TestOnServer
    public void getUserByUsernameResolvesUserWhenCachedEntryBelongsToAnotherRealm(KeycloakSession session) {
        try {
            String bobId = setUpCollidingRealms(session);

            // realm2's own user must still resolve, even though the shared cache entry for its id was forged to belong to realm1
            inSession(session, s -> {
                RealmModel realm1 = s.realms().getRealmByName(COLLIDING_REALM_1);
                RealmModel realm2 = s.realms().getRealmByName(COLLIDING_REALM_2);

                // force the forged cache entry into this session's managed-user map under realm1, as if it had
                // already been resolved earlier in the session, so the lookup below cannot just rely on the
                // distributed cache's own realm check
                s.users().getUserById(realm1, bobId);

                UserModel user = s.users().getUserByUsername(realm2, "bob");
                assertThat(user, notNullValue());
                assertThat(user.getEmail(), equalTo("bob@example.org"));

                // the resolved user must not have been lost to a poisoned null managed-user entry for the rest of the session
                assertThat(s.users().getUserById(realm2, bobId), notNullValue());
            });
        } finally {
            removeCollidingRealms(session);
        }
    }

    @TestOnServer
    public void getUserByEmailResolvesUserWhenCachedEntryBelongsToAnotherRealm(KeycloakSession session) {
        try {
            String bobId = setUpCollidingRealms(session);

            // realm2's own user must still resolve, even though the shared cache entry for its id was forged to belong to realm1
            inSession(session, s -> {
                RealmModel realm1 = s.realms().getRealmByName(COLLIDING_REALM_1);
                RealmModel realm2 = s.realms().getRealmByName(COLLIDING_REALM_2);

                // force the forged cache entry into this session's managed-user map under realm1, as if it had
                // already been resolved earlier in the session, so the lookup below cannot just rely on the
                // distributed cache's own realm check
                s.users().getUserById(realm1, bobId);

                UserModel user = s.users().getUserByEmail(realm2, "bob@example.org");
                assertThat(user, notNullValue());
                assertThat(user.getUsername(), equalTo("bob"));

                // the resolved user must not have been lost to a poisoned null managed-user entry for the rest of the session
                assertThat(s.users().getUserById(realm2, bobId), notNullValue());
            });
        } finally {
            removeCollidingRealms(session);
        }
    }

    /**
     * Sets up two realms, each with their own local user (realm1/alice, realm2/bob). Since a user's id is the
     * primary key of its table, two real users can never actually share the same id. To still exercise the shared
     * user cache's behavior for that case (which is only keyed by id, not by realm), this forges a collision by
     * directly planting a {@link CachedUser} for alice (realm1) under bob's (realm2) id, as if the shared cache had
     * (wrongly) returned alice's entry for a lookup of bob's id.
     *
     * @return bob's id, under which the forged cache entry for realm1 was planted
     */
    private static String setUpCollidingRealms(KeycloakSession session) {
        String[] ids = new String[2];
        inSession(session, s -> {
            RealmModel realm1 = new RealmManager(s).createRealm(COLLIDING_REALM_1);
            UserModel alice = s.users().addUser(realm1, "alice");
            alice.setEmail("alice@example.org");
            ids[0] = alice.getId();
        });
        inSession(session, s -> {
            RealmModel realm2 = new RealmManager(s).createRealm(COLLIDING_REALM_2);
            UserModel bob = s.users().addUser(realm2, "bob");
            bob.setEmail("bob@example.org");
            ids[1] = bob.getId();
        });
        String bobId = ids[1];

        inSession(session, s -> {
            RealmModel realm1 = s.realms().getRealmByName(COLLIDING_REALM_1);
            UserModel alice = s.users().getUserByUsername(realm1, "alice");
            CachedUser forged = new CachedUser(0, realm1, alice, 0);

            Cache<String, Object> userCache = s.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_CACHE_NAME);
            Cache<String, Object> revisions = s.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME);
            userCache.put(bobId, forged);
            revisions.put(bobId, 0L);
        });

        return bobId;
    }

    private static void removeCollidingRealms(KeycloakSession session) {
        inSession(session, s -> {
            RealmManager realmManager = new RealmManager(s);
            for (String realmName : new String[] { COLLIDING_REALM_1, COLLIDING_REALM_2 }) {
                RealmModel realm = realmManager.getRealmByName(realmName);
                if (realm != null) {
                    realmManager.removeRealm(realm);
                }
            }
        });
    }

    @TestOnServer
    public void queriesAreCachedForLocalUsers(KeycloakSession session) {
        inSession(session, s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            UserModel user = s.users().addUser(realm, LOCAL_USERNAME);
            user.setEmail(LOCAL_EMAIL);
        });

        inSession(session, s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            assertThat(s.users().getUserByEmail(realm, LOCAL_EMAIL), notNullValue());
            assertThat(s.users().getUserByUsername(realm, LOCAL_USERNAME), notNullValue());
        });

        inSession(session, s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            Cache<String, Object> userCache = s.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_CACHE_NAME);
            assertThat(userCache.containsKey(realm.getId() + ".email." + LOCAL_EMAIL), equalTo(true));
            assertThat(userCache.containsKey(realm.getId() + ".username." + LOCAL_USERNAME), equalTo(true));
        });
    }

    private static UserModel lookupByEmail(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        UserModel user = session.users().getUserByEmail(realm, EMAIL);
        assertThat(user, notNullValue());
        assertThat(user.getUsername(), equalTo(USERNAME));
        return user;
    }

    private static UserModel lookupByUsername(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        UserModel user = session.users().getUserByUsername(realm, USERNAME);
        assertThat(user, notNullValue());
        assertThat(user.getEmail(), equalTo(EMAIL));
        return user;
    }

    /** Runs the work in a new session and transaction, like a separate request would. */
    private static void inSession(KeycloakSession session, Consumer<KeycloakSession> work) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), work::accept);
    }
}
