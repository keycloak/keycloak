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

package org.keycloak.models.sessions.infinispan;

import java.util.Iterator;
import java.util.Map;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.cache.infinispan.events.AuthenticationSessionAuthNoteUpdateEvent;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.SessionWrapperPredicate;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.persistence.manager.PersistenceManager;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanAuthenticationSessionProvider implements AuthenticationSessionProvider {

    private final KeycloakSession session;
    private final int authSessionsLimit;
    protected final InfinispanChangelogBasedTransaction<String, RootAuthenticationSessionEntity> sessionTx;
    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    public InfinispanAuthenticationSessionProvider(KeycloakSession session,
                                                   InfinispanChangelogBasedTransaction<String, RootAuthenticationSessionEntity> sessionTx, int authSessionsLimit) {
        this.session = session;
        this.authSessionsLimit = authSessionsLimit;
        this.sessionTx = sessionTx;
        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);
        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
    }


    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm) {
        return createRootAuthenticationSession(realm, sessionTx.generateKey());
    }


    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm, String id) {
        RootAuthenticationSessionEntity entity = new RootAuthenticationSessionEntity(id);
        entity.setRealmId(realm.getId());
        entity.setTimestamp(Time.currentTime());

        SessionUpdateTask<RootAuthenticationSessionEntity> createAuthSessionTask = Tasks.addIfAbsentSync();
        sessionTx.addTask(entity.getId(), createAuthSessionTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);
        return wrap(realm, entity);
    }


    private RootAuthenticationSessionAdapter wrap(RealmModel realm, RootAuthenticationSessionEntity entity) {
        return entity == null ? null : new RootAuthenticationSessionAdapter(session, this, realm, entity, authSessionsLimit);
    }


    private RootAuthenticationSessionEntity getRootAuthenticationSessionEntity(String authSessionId) {
        SessionEntityWrapper<RootAuthenticationSessionEntity> entityWrapper = sessionTx.get(authSessionId);
        return entityWrapper==null ? null : entityWrapper.getEntity();
    }

    @Override
    public void removeAllExpired() {
        // Rely on expiration of cache entries provided by infinispan. Nothing needed here
    }

    @Override
    public void removeExpired(RealmModel realm) {
        // Rely on expiration of cache entries provided by infinispan. Nothing needed here
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        // Send message to all DCs. The remoteCache will notify client listeners on all DCs for remove authentication sessions
        clusterEventsSenderTx.addEvent(
                RealmRemovedSessionEvent.createEvent(RealmRemovedSessionEvent.class, InfinispanAuthenticationSessionProviderFactory.REALM_REMOVED_AUTHSESSION_EVENT, session, realm.getId())
        );
    }

    protected void onRealmRemovedEvent(String realmId) {
        Cache<String, SessionEntityWrapper<RootAuthenticationSessionEntity>> cache = sessionTx.getCache();
        Iterator<Map.Entry<String, SessionEntityWrapper<RootAuthenticationSessionEntity>>> itr = CacheDecorators.localCache(cache)
                .entrySet()
                .stream()
                .filter(SessionWrapperPredicate.create(realmId))
                .iterator();

        while (itr.hasNext()) {
            CacheDecorators.localCache(cache)
                    .remove(itr.next().getKey());
        }
    }


    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        // No update anything on clientRemove for now. AuthenticationSessions of removed client will be handled at runtime if needed.

//        clusterEventsSenderTx.addEvent(
//                ClientRemovedSessionEvent.create(session, InfinispanAuthenticationSessionProviderFactory.CLIENT_REMOVED_AUTHSESSION_EVENT, realm.getId(), false, client.getId()),
//                ClusterProvider.DCNotify.ALL_DCS);
    }


    @Override
    public void updateNonlocalSessionAuthNotes(AuthenticationSessionCompoundId compoundId, Map<String, String> authNotesFragment) {
        if (compoundId == null) {
            return;
        }

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(
          InfinispanAuthenticationSessionProviderFactory.AUTHENTICATION_SESSION_EVENTS,
          AuthenticationSessionAuthNoteUpdateEvent.create(compoundId.getRootSessionId(), compoundId.getTabId(), authNotesFragment),
          true,
          ClusterProvider.DCNotify.ALL_BUT_LOCAL_DC
        );
    }


    @Override
    public RootAuthenticationSessionModel getRootAuthenticationSession(RealmModel realm, String authenticationSessionId) {
        RootAuthenticationSessionEntity entity = getRootAuthenticationSessionEntity(authenticationSessionId);
        return wrap(realm, entity);
    }


    @Override
    public void removeRootAuthenticationSession(RealmModel realm, RootAuthenticationSessionModel authenticationSession) {
        SessionUpdateTask<RootAuthenticationSessionEntity> removeTask = Tasks.removeSync();
        sessionTx.addTask(authenticationSession.getId(), removeTask);
    }

    @Override
    public void close() {

    }

    public Cache<String, SessionEntityWrapper<RootAuthenticationSessionEntity>> getCache() {
        return sessionTx.getCache();
    }

    public InfinispanChangelogBasedTransaction<String, RootAuthenticationSessionEntity> getRootAuthSessionTransaction() {
        return sessionTx;
    }

    @Override
    public void migrate(String modelVersion) {
        if ("26.1.0".equals(modelVersion)) {
            InfinispanConnectionProvider infinispanConnectionProvider = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, RootAuthenticationSessionEntity> authSessionsCache = infinispanConnectionProvider.getCache(AUTHENTICATION_SESSIONS_CACHE_NAME);
            CompletionStages.join(ComponentRegistry.componentOf(authSessionsCache, PersistenceManager.class).clearAllStores(PersistenceManager.AccessMode.BOTH));
        }
    }
}
