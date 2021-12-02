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

import org.keycloak.cluster.ClusterProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.events.AuthenticationSessionAuthNoteUpdateEvent;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.RootAuthenticationSessionPredicate;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.utils.RealmInfoUtil;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanAuthenticationSessionProvider implements AuthenticationSessionProvider {

    private static final Logger log = Logger.getLogger(InfinispanAuthenticationSessionProvider.class);

    private final KeycloakSession session;
    private final Cache<String, RootAuthenticationSessionEntity> cache;
    private final InfinispanKeyGenerator keyGenerator;
    private final int authSessionsLimit;
    protected final InfinispanKeycloakTransaction tx;
    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    public InfinispanAuthenticationSessionProvider(KeycloakSession session, InfinispanKeyGenerator keyGenerator,
                                                   Cache<String, RootAuthenticationSessionEntity> cache, int authSessionsLimit) {
        this.session = session;
        this.cache = cache;
        this.keyGenerator = keyGenerator;
        this.authSessionsLimit = authSessionsLimit;

        this.tx = new InfinispanKeycloakTransaction();
        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(tx);
        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
    }

    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm) {
        String id = keyGenerator.generateKeyString(session, cache);
        return createRootAuthenticationSession(realm, id);
    }


    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm, String id) {
        RootAuthenticationSessionEntity entity = new RootAuthenticationSessionEntity();
        entity.setId(id);
        entity.setRealmId(realm.getId());
        entity.setTimestamp(Time.currentTime());

        int expirationSeconds = RealmInfoUtil.getDettachedClientSessionLifespan(realm);
        tx.put(cache, id, entity, expirationSeconds, TimeUnit.SECONDS);

        return wrap(realm, entity);
    }


    private RootAuthenticationSessionAdapter wrap(RealmModel realm, RootAuthenticationSessionEntity entity) {
        return entity==null ? null : new RootAuthenticationSessionAdapter(session, this, cache, realm, entity, authSessionsLimit);
    }


    private RootAuthenticationSessionEntity getRootAuthenticationSessionEntity(String authSessionId) {
        // Chance created in this transaction
        RootAuthenticationSessionEntity entity = tx.get(cache, authSessionId);
        return entity;
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
                RealmRemovedSessionEvent.createEvent(RealmRemovedSessionEvent.class, InfinispanAuthenticationSessionProviderFactory.REALM_REMOVED_AUTHSESSION_EVENT, session, realm.getId(), false),
                ClusterProvider.DCNotify.ALL_DCS);
    }

    protected void onRealmRemovedEvent(String realmId) {
        Iterator<Map.Entry<String, RootAuthenticationSessionEntity>> itr = CacheDecorators.localCache(cache)
                .entrySet()
                .stream()
                .filter(RootAuthenticationSessionPredicate.create(realmId))
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

    protected void onClientRemovedEvent(String realmId, String clientUuid) {

    }


    @Override
    public void updateNonlocalSessionAuthNotes(AuthenticationSessionCompoundId compoundId, Map<String, String> authNotesFragment) {
        if (compoundId == null) {
            return;
        }

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(
          InfinispanAuthenticationSessionProviderFactory.AUTHENTICATION_SESSION_EVENTS,
          AuthenticationSessionAuthNoteUpdateEvent.create(compoundId.getRootSessionId(), compoundId.getTabId(), compoundId.getClientUUID(), authNotesFragment),
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
        tx.remove(cache, authenticationSession.getId());
    }

    @Override
    public void close() {

    }

    public Cache<String, RootAuthenticationSessionEntity> getCache() {
        return cache;
    }


    protected String generateTabId() {
        return Base64Url.encode(SecretGenerator.getInstance().randomBytes(8));
    }
}
