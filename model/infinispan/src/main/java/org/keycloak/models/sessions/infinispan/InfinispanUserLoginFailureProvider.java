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

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.events.RemoveAllUserLoginFailuresEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.UserLoginFailurePredicate;
import org.keycloak.models.sessions.infinispan.util.FuturesHelper;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import java.util.concurrent.Future;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;

/**
 *
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class InfinispanUserLoginFailureProvider implements UserLoginFailureProvider {

    private static final Logger log = Logger.getLogger(InfinispanUserLoginFailureProvider.class);

    protected final KeycloakSession session;


    protected final Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> loginFailureCache;
    protected final InfinispanChangelogBasedTransaction<LoginFailureKey, LoginFailureEntity> loginFailuresTx;
    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    public InfinispanUserLoginFailureProvider(KeycloakSession session,
                                              RemoteCacheInvoker remoteCacheInvoker,
                                              Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> loginFailureCache) {
        this.session = session;
        this.loginFailureCache = loginFailureCache;
        this.loginFailuresTx = new InfinispanChangelogBasedTransaction<>(session, loginFailureCache, remoteCacheInvoker, SessionTimeouts::getLoginFailuresLifespanMs, SessionTimeouts::getLoginFailuresMaxIdleMs);
        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
        session.getTransactionManager().enlistAfterCompletion(loginFailuresTx);
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
        LoginFailureEntity entity = new LoginFailureEntity();
        entity.setRealmId(realm.getId());
        entity.setUserId(userId);

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
                RemoveAllUserLoginFailuresEvent.createEvent(RemoveAllUserLoginFailuresEvent.class, InfinispanUserLoginFailureProviderFactory.REMOVE_ALL_LOGIN_FAILURES_EVENT, session, realm.getId(), true),
                ClusterProvider.DCNotify.LOCAL_DC_ONLY);
    }

    protected void removeAllLocalUserLoginFailuresEvent(String realmId) {
        log.tracef("removeAllLocalUserLoginFailuresEvent(%s)%s", realmId, getShortStackTrace());

        FuturesHelper futures = new FuturesHelper();

        Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> localCache = CacheDecorators.localCache(loginFailureCache);

        Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> localCacheStoreIgnore = CacheDecorators.skipCacheLoaders(localCache);

        localCacheStoreIgnore
                .entrySet()
                .stream()
                .filter(UserLoginFailurePredicate.create(realmId))
                .map(Mappers.loginFailureId())
                .forEach(loginFailureKey -> {
                    // Remove loginFailure from remoteCache too. Use removeAsync for better perf
                    Future future = localCache.removeAsync(loginFailureKey);
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
}
