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

package org.keycloak.testsuite.model.infinispan;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.sessions.infinispan.remote.RemoteUserLoginFailureProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.testsuite.model.HotRodServerRule;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RequireProvider(UserLoginFailureProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class RetryAndBackOffTest extends KeycloakModelTest {

    private static final int MAX_RETRIES = 5;

    @ClassRule
    public static final TestRule SKIPPED_PROFILES = (base, description) -> {
        // we only want to test retries with the remote cache/multi-site
        Assume.assumeTrue(InfinispanUtils.isRemoteInfinispan());
        return base;
    };

    private String realmId;
    private String userId;
    private TimeOutInterceptor timeOutInterceptor;

    @Override
    public void createEnvironment(KeycloakSession session) {
        RealmModel realm = createRealm(session, "retry-and-backoff-test");
        session.getContext().setRealm(realm);
        realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realmId = realm.getId();
        var user = session.users().addUser(realm, "retry-user");
        user.setEmail("retry-user@localhost");
        userId = user.getId();
        timeOutInterceptor = injectInterceptor();
        ProviderFactory<?> provider = session.getKeycloakSessionFactory().getProviderFactory(UserLoginFailureProvider.class);
        assertTrue(provider instanceof RemoteUserLoginFailureProviderFactory);
        ((RemoteUserLoginFailureProviderFactory) provider).setMaxRetries(MAX_RETRIES);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    private static TimeOutInterceptor injectInterceptor() {
        var optRemote = getParameters(HotRodServerRule.class).findFirst();
        assertTrue(optRemote.isPresent());
        var cacheManager = optRemote.get().getHotRodCacheManager();
        return TimeOutInterceptor.getOrInject(cacheManager.getCache(LOGIN_FAILURE_CACHE_NAME));
    }

    @Test
    public void testRetryWithPut() {
        timeOutInterceptor.timeoutPuts(randomTimeoutFailures());

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().addUserLoginFailure(realm, userId);
            loginFailures.incrementFailures();
        });

        timeOutInterceptor.assertPutTimedOutExhausted();
    }

    @Test
    public void testRetryWithReplace() {
        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().addUserLoginFailure(realm, userId);
            loginFailures.incrementFailures();
        });

        timeOutInterceptor.timeoutReplace(randomTimeoutFailures());

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().getUserLoginFailure(realm, userId);
            loginFailures.incrementFailures();
        });

        timeOutInterceptor.assertReplaceTimedOutExhausted();
    }

    @Test
    public void testRetryWithRemove() {
        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().addUserLoginFailure(realm, userId);
            loginFailures.incrementFailures();
        });

        timeOutInterceptor.timeoutRemove(randomTimeoutFailures());

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            session.loginFailures().removeUserLoginFailure(realm, userId);
        });

        timeOutInterceptor.assertRemoveTimedOutExhausted();
    }

    @Test
    public void testRetryWithCompute() {
        // compute is implemented with get() and replace()
        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().addUserLoginFailure(realm, userId);
            loginFailures.incrementFailures();
        });

        // first replace returns false, it should switch to compute
        timeOutInterceptor.replaceReturnsFalse(1);
        timeOutInterceptor.timeoutReplace(randomTimeoutFailures());

        inComittedTransaction(session -> {
            var realm = session.realms().getRealm(realmId);
            var loginFailures = session.loginFailures().getUserLoginFailure(realm, userId);
            loginFailures.incrementFailures();
        });

        timeOutInterceptor.assertReplaceCheckFailExhausted();
        timeOutInterceptor.assertReplaceTimedOutExhausted();
    }

    @Test
    public void testExceptionThrown() {
        timeOutInterceptor.timeoutPuts(MAX_RETRIES + 1);

        try {
            var ce = assertThrows(CompletionException.class,
                    () -> inComittedTransaction(session -> {
                        var realm = session.realms().getRealm(realmId);
                        session.loginFailures().addUserLoginFailure(realm, userId);
                    }));
            assertTrue(String.valueOf(ce.getCause()), ce.getCause() instanceof HotRodClientException);
            assertTrue(ce.getCause().getLocalizedMessage(), ce.getCause().getLocalizedMessage().contains(TimeOutInterceptor.MESSAGE));
        } finally {
            timeOutInterceptor.reset();
        }
    }

    private static int randomTimeoutFailures() {
        // At most, 4 failures so the test don't take a long time to run.
        // Adds a little bit of randomness into the test.
        return Math.max(1, ThreadLocalRandom.current().nextInt(MAX_RETRIES));
    }

}
