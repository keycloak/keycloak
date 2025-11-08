/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.loginfailure;

import java.util.List;
import java.util.stream.IntStream;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.infinispan.client.hotrod.RemoteCache;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

@RequireProvider(UserLoginFailureProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class RemoteLoginFailureTest extends KeycloakModelTest {

    private static final int NUM_USERS = 10;

    private String realmId;
    private List<String> userIds;

    @Override
    public void createEnvironment(KeycloakSession session) {
        RealmModel realm = createRealm(session, "remote-login-failure-test");
        session.getContext().setRealm(realm);
        realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realmId = realm.getId();

        userIds = IntStream.range(0, NUM_USERS)
                .mapToObj(index -> "user-" + index)
                .map(username -> {
                    var user = session.users().addUser(realm, username);
                    user.setEmail(username + "@localhost");
                    return user.getId();
                })
                .toList();

    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testLoginFailureCreation() {
        Assume.assumeTrue(InfinispanUtils.isRemoteInfinispan());
        var cache = getLoginFailureCache();

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().addUserLoginFailure(realm, userIds.get(0));
            loginFailures.incrementFailures();
        });

        var entity = cache.get(new LoginFailureKey(realmId, userIds.get(0)));
        assertEntity(entity, realmId, userIds.get(0), 0, null, 1, 0, 0);
    }

    @Test
    public void testLoginFailureChangeLog() {
        Assume.assumeTrue(InfinispanUtils.isRemoteInfinispan());
        var cache = getLoginFailureCache();
        var key = new LoginFailureKey(realmId, userIds.get(0));
        var entity = new LoginFailureEntity(realmId, userIds.get(0));
        entity.setLastFailure(1000);
        entity.setNumFailures(2);
        entity.setNumTemporaryLockouts(10);
        entity.setLastIPFailure("127.0.0.1");
        entity.setFailedLoginNotBefore(2000);

        cache.put(key, entity);

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().getUserLoginFailure(realm, userIds.get(0));

            // update all fields
            loginFailures.setLastFailure(10000);
            loginFailures.incrementFailures();
            loginFailures.incrementTemporaryLockouts();
            loginFailures.setLastIPFailure("127.0.1.1");
            loginFailures.setFailedLoginNotBefore(20000);
        });

        entity = cache.get(key);
        assertEntity(entity, realmId, userIds.get(0), 10000, "127.0.1.1", 3, 20000, 11);
    }

    @Test
    public void testLoginFailureChangeLogWithConcurrent() {
        Assume.assumeTrue(InfinispanUtils.isRemoteInfinispan());
        var cache = getLoginFailureCache();
        var key = new LoginFailureKey(realmId, userIds.get(0));
        var entity = new LoginFailureEntity(realmId, userIds.get(0));
        entity.setLastFailure(1000);
        entity.setNumFailures(2);
        entity.setNumTemporaryLockouts(10);
        entity.setLastIPFailure("127.0.0.1");
        entity.setFailedLoginNotBefore(2000);

        cache.put(key, entity);

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().getUserLoginFailure(realm, userIds.get(0));

            // update all fields
            loginFailures.setLastFailure(10000);
            loginFailures.incrementFailures();
            loginFailures.incrementTemporaryLockouts();
            loginFailures.setLastIPFailure("127.0.1.1");
            loginFailures.setFailedLoginNotBefore(20000);

            createRandomEntityInCache(cache, 20, 30, realmId, userIds.get(0));
        });

        entity = cache.get(key);
        assertEntity(entity, realmId, userIds.get(0), 10000, "127.0.1.1", 21, 20000, 31);
    }

    @Test
    public void testLoginFailureClear() {
        Assume.assumeTrue(InfinispanUtils.isRemoteInfinispan());
        var cache = getLoginFailureCache();
        var key = new LoginFailureKey(realmId, userIds.get(0));
        var entity = new LoginFailureEntity(realmId, userIds.get(0));
        entity.setLastFailure(1000);
        entity.setNumFailures(2);
        entity.setNumTemporaryLockouts(10);
        entity.setLastIPFailure("127.0.0.1");
        entity.setFailedLoginNotBefore(2000);

        cache.put(key, entity);

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().getUserLoginFailure(realm, userIds.get(0));
            loginFailures.incrementTemporaryLockouts();
            loginFailures.clearFailures();

            // create a conflict? should not make a difference
            createRandomEntityInCache(cache, 1, 0, realmId, userIds.get(0));
        });

        entity = cache.get(key);
        assertEntity(entity, realmId, userIds.get(0), 0, null, 0, 0, 0);
    }

    @Test
    public void testLoginFailureRemove() {
        Assume.assumeTrue(InfinispanUtils.isRemoteInfinispan());
        var cache = getLoginFailureCache();
        var key = new LoginFailureKey(realmId, userIds.get(0));
        var entity = new LoginFailureEntity(realmId, userIds.get(0));

        cache.put(key, entity);

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            session.loginFailures().removeUserLoginFailure(realm, userIds.get(0));
        });

        entity = cache.get(key);
        Assert.assertNull(entity);
    }

    @Test
    public void testLoginFailureRemoveAll() {
        Assume.assumeTrue(InfinispanUtils.isRemoteInfinispan());
        var cache = getLoginFailureCache();

        // clear garbage from previous tests
        cache.clear();

        for (var userId : userIds) {
            var entity = new LoginFailureEntity(realmId, userId);
            cache.put(new LoginFailureKey(realmId, userId), entity);
        }

        Assert.assertEquals(10, cache.size());

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            session.loginFailures().removeAllUserLoginFailures(realm);
        });

        Assert.assertEquals(0, cache.size());
    }

    private static void createRandomEntityInCache(RemoteCache<LoginFailureKey, LoginFailureEntity> cache, int failures, int temporaryLockouts, String realmId, String userId) {
        var key = new LoginFailureKey(realmId, userId);
        var entity = new LoginFailureEntity(realmId, userId);
        entity.setLastFailure(5000); // does not matter
        entity.setNumFailures(failures);
        entity.setNumTemporaryLockouts(temporaryLockouts);
        entity.setLastIPFailure("127.0.0.1");
        entity.setFailedLoginNotBefore(5000);

        cache.put(key, entity);
    }

    private static void assertEntity(LoginFailureEntity entity, String realmId, String userId, long lastFailure, String lastIpFailure, int failures, int failedLoginNotBefore, int temporaryLockouts) {
        Assert.assertNotNull(entity);
        Assert.assertEquals(realmId, entity.getRealmId());
        Assert.assertEquals(userId, entity.getUserId());
        Assert.assertEquals(lastFailure, entity.getLastFailure());
        Assert.assertEquals(lastIpFailure, entity.getLastIPFailure());
        Assert.assertEquals(failures, entity.getNumFailures());
        Assert.assertEquals(failedLoginNotBefore, entity.getFailedLoginNotBefore());
        Assert.assertEquals(temporaryLockouts, entity.getNumTemporaryLockouts());
    }

    private RemoteCache<LoginFailureKey, LoginFailureEntity> getLoginFailureCache() {
        return getInfinispanConnectionProvider().getRemoteCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);
    }

    private InfinispanConnectionProvider getInfinispanConnectionProvider() {
        return inComittedTransaction(RemoteLoginFailureTest::getInfinispanConnectionProviderWithSession);
    }

    private static InfinispanConnectionProvider getInfinispanConnectionProviderWithSession(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class);
    }

}
