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
package org.keycloak.models.sessions.infinispan;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletionStages;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.events.RemoveAllUserLoginFailuresEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.SessionWrapperPredicate;
import org.keycloak.models.sessions.infinispan.util.FuturesHelper;

import org.infinispan.Cache;
import org.jboss.logging.Logger;

import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;

/**
 *
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class InfinispanUserLoginFailureProvider implements UserLoginFailureProvider {

    private static final Logger log = Logger.getLogger(InfinispanUserLoginFailureProvider.class);

    protected final KeycloakSession session;


    protected final InfinispanChangelogBasedTransaction<LoginFailureKey, LoginFailureEntity> loginFailuresTx;
    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    public InfinispanUserLoginFailureProvider(KeycloakSession session,
                                              InfinispanChangelogBasedTransaction<LoginFailureKey, LoginFailureEntity> loginFailuresTx) {
        this.session = session;
        this.loginFailuresTx = loginFailuresTx;
        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);
        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
    }


    @Override
    public UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId) {
        log.tracef("getUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        LoginFailureKey key = new LoginFailureKey(realm.getId(), userId);
        LoginFailureEntity entity = getLoginFailureEntity(key);
        return wrap(key, entity);
    }

    @Override
    public UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId) {
        log.tracef("addUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        LoginFailureKey key = new LoginFailureKey(realm.getId(), userId);
        LoginFailureEntity entity = new LoginFailureEntity(realm.getId(), userId);

        SessionUpdateTask<LoginFailureEntity> createLoginFailureTask = Tasks.addIfAbsentSync();
        loginFailuresTx.addTask(key, createLoginFailureTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        return wrap(key, entity);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String userId) {
        log.tracef("removeUserLoginFailure(%s, %s)%s", realm, userId, getShortStackTrace());

        SessionUpdateTask<LoginFailureEntity> removeTask = Tasks.removeSync();
        loginFailuresTx.addTask(new LoginFailureKey(realm.getId(), userId), removeTask);
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        log.tracef("removeAllUserLoginFailures(%s)%s", realm, getShortStackTrace());

        clusterEventsSenderTx.addEvent(
                RemoveAllUserLoginFailuresEvent.createEvent(RemoveAllUserLoginFailuresEvent.class, InfinispanUserLoginFailureProviderFactory.REMOVE_ALL_LOGIN_FAILURES_EVENT, session, realm.getId())
        );
    }

    protected void removeAllLocalUserLoginFailuresEvent(String realmId) {
        log.tracef("removeAllLocalUserLoginFailuresEvent(%s)%s", realmId, getShortStackTrace());

        FuturesHelper futures = new FuturesHelper();

        Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> localCache = CacheDecorators.localCache(loginFailuresTx.getCache());

        // Go through local cache data only
        // entries from other nodes will be removed by each instance receiving the event
        localCache
                .entrySet()
                .stream()
                .filter(SessionWrapperPredicate.create(realmId))
                .map(Mappers.loginFailureId())
                .forEach(loginFailureKey -> {
                    // Remove loginFailure from remoteCache too. Use removeAsync for better perf
                    Future<?> future = removeKeyFromCache(localCache, loginFailureKey);
                    futures.addTask(future);
                });

        futures.waitForAllToFinish();

        log.debugf("Removed %d login failures in realm %s", futures.size(), realmId);
    }

    UserLoginFailureModel wrap(LoginFailureKey key, LoginFailureEntity entity) {
        return entity != null ? new UserLoginFailureAdapter(this, key, entity) : null;
    }

    private LoginFailureEntity getLoginFailureEntity(LoginFailureKey key) {
        InfinispanChangelogBasedTransaction<LoginFailureKey, LoginFailureEntity> tx = getLoginFailuresTx();
        SessionEntityWrapper<LoginFailureEntity> entityWrapper = tx.get(key);
        return entityWrapper==null ? null : entityWrapper.getEntity();
    }

    InfinispanChangelogBasedTransaction<LoginFailureKey, LoginFailureEntity> getLoginFailuresTx() {
        return loginFailuresTx;
    }

    @Override
    public void close() {

    }

    @Override
    public void migrate(String modelVersion) {
        if ("26.5.0".equals(modelVersion)) {
            // This version introduced updated lifetimes for login failures. Recalculate values for existing entries.
            Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> cache = loginFailuresTx.getCache();
            AggregateCompletionStage<Void> stage = CompletionStages.aggregateCompletionStage();
            cache.getAdvancedCache().entrySet()
                    .forEach(entry -> {
                        RealmModel realm = session.realms().getRealm(entry.getKey().realmId());
                        if (!realm.isBruteForceProtected()) {
                            stage.dependsOn(removeKeyFromCache(cache, entry.getKey()));
                        } else {
                            updateLifetimeOfCacheEntry(entry, realm, stage, cache);
                        }
                    });
            CompletionStages.join(stage.freeze());
        }
    }

    @Override
    public void updateWithLatestRealmSettings(RealmModel realm) {
        Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> cache = loginFailuresTx.getCache();
        AggregateCompletionStage<Void> stage = CompletionStages.aggregateCompletionStage();
        if (!realm.isBruteForceProtected()) {
            cache.getAdvancedCache().entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getKey().realmId(), realm.getId()))
                    .forEach(entry -> stage.dependsOn(removeKeyFromCache(cache, entry.getKey())));
        } else {
            cache.getAdvancedCache().entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getKey().realmId(), realm.getId()))
                    .forEach(entry -> {
                        updateLifetimeOfCacheEntry(entry, realm, stage, cache);
                    });
        }
        CompletionStages.join(stage.freeze());
    }

    private static void updateLifetimeOfCacheEntry(Map.Entry<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> entry, RealmModel realm, AggregateCompletionStage<Void> stage, Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> cache) {
        long lifespanMs = SessionTimeouts.getLoginFailuresLifespanMs(realm, null, entry.getValue().getEntity());
        long maxIdleMs = SessionTimeouts.getLoginFailuresMaxIdleMs(realm, null, entry.getValue().getEntity());
        stage.dependsOn(
                cache.getAdvancedCache().computeIfPresentAsync(entry.getKey(),
                        // Keep the original value - this should only update the lifespan and idle time
                        (loginFailureKey, loginFailureEntitySessionEntityWrapper) -> loginFailureEntitySessionEntityWrapper,
                        lifespanMs, TimeUnit.MILLISECONDS, maxIdleMs, TimeUnit.MILLISECONDS)
        );
    }

    private static CompletableFuture<SessionEntityWrapper<LoginFailureEntity>> removeKeyFromCache(Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> cache, LoginFailureKey key) {
        return cache.removeAsync(key);
    }

}
