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

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.events.AuthenticationSessionAuthNoteUpdateEvent;
import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.events.ClientRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.AuthenticationSessionPredicate;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmInfoUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanAuthenticationSessionProvider implements AuthenticationSessionProvider {

    private static final Logger log = Logger.getLogger(InfinispanAuthenticationSessionProvider.class);

    private final KeycloakSession session;
    private final Cache<String, AuthenticationSessionEntity> cache;
    protected final InfinispanKeycloakTransaction tx;
    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    public InfinispanAuthenticationSessionProvider(KeycloakSession session, Cache<String, AuthenticationSessionEntity> cache) {
        this.session = session;
        this.cache = cache;

        this.tx = new InfinispanKeycloakTransaction();
        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(tx);
        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
    }

    @Override
    public AuthenticationSessionModel createAuthenticationSession(RealmModel realm, ClientModel client) {
        String id = KeycloakModelUtils.generateId();
        return createAuthenticationSession(id, realm, client);
    }

    @Override
    public AuthenticationSessionModel createAuthenticationSession(String id, RealmModel realm, ClientModel client) {
        AuthenticationSessionEntity entity = new AuthenticationSessionEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setTimestamp(Time.currentTime());
        entity.setClientUuid(client.getId());

        tx.put(cache, id, entity);

        AuthenticationSessionAdapter wrap = wrap(realm, entity);
        return wrap;
    }

    private AuthenticationSessionAdapter wrap(RealmModel realm, AuthenticationSessionEntity entity) {
        return entity==null ? null : new AuthenticationSessionAdapter(session, this, cache, realm, entity);
    }

    @Override
    public AuthenticationSessionModel getAuthenticationSession(RealmModel realm, String authenticationSessionId) {
        AuthenticationSessionEntity entity = getAuthenticationSessionEntity(realm, authenticationSessionId);
        return wrap(realm, entity);
    }

    private AuthenticationSessionEntity getAuthenticationSessionEntity(RealmModel realm, String authSessionId) {
        // Chance created in this transaction
        AuthenticationSessionEntity entity = tx.get(cache, authSessionId);

        if (entity == null) {
            entity = cache.get(authSessionId);
        }

        return entity;
    }

    @Override
    public void removeAuthenticationSession(RealmModel realm, AuthenticationSessionModel authenticationSession) {
        tx.remove(cache, authenticationSession.getId());
    }

    @Override
    public void removeExpired(RealmModel realm) {
        log.debugf("Removing expired sessions");

        int expired = Time.currentTime() - RealmInfoUtil.getDettachedClientSessionLifespan(realm);


        // Each cluster node cleanups just local sessions, which are those owned by himself (+ few more taking l1 cache into account)
        Iterator<Map.Entry<String, AuthenticationSessionEntity>> itr = CacheDecorators.localCache(cache)
                .entrySet()
                .stream()
                .filter(AuthenticationSessionPredicate.create(realm.getId()).expired(expired))
                .iterator();

        int counter = 0;
        while (itr.hasNext()) {
            counter++;
            AuthenticationSessionEntity entity = itr.next().getValue();
            tx.remove(CacheDecorators.localCache(cache), entity.getId());
        }

        log.debugf("Removed %d expired authentication sessions for realm '%s'", counter, realm.getName());
    }


    @Override
    public void onRealmRemoved(RealmModel realm) {
        clusterEventsSenderTx.addEvent(InfinispanAuthenticationSessionProviderFactory.REALM_REMOVED_AUTHSESSION_EVENT, RealmRemovedSessionEvent.create(realm.getId()), true);
    }

    protected void onRealmRemovedEvent(String realmId) {
        Iterator<Map.Entry<String, AuthenticationSessionEntity>> itr = CacheDecorators.localCache(cache)
                .entrySet()
                .stream()
                .filter(AuthenticationSessionPredicate.create(realmId))
                .iterator();

        while (itr.hasNext()) {
            CacheDecorators.localCache(cache)
                    .remove(itr.next().getKey());
        }
    }


    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        clusterEventsSenderTx.addEvent(InfinispanAuthenticationSessionProviderFactory.CLIENT_REMOVED_AUTHSESSION_EVENT, ClientRemovedSessionEvent.create(realm.getId(), client.getId()), true);
    }

    protected void onClientRemovedEvent(String realmId, String clientUuid) {
        Iterator<Map.Entry<String, AuthenticationSessionEntity>> itr = CacheDecorators.localCache(cache)
                .entrySet()
                .stream()
                .filter(AuthenticationSessionPredicate.create(realmId).client(clientUuid))
                .iterator();

        while (itr.hasNext()) {
            CacheDecorators.localCache(cache)
                    .remove(itr.next().getKey());
        }
    }


    @Override
    public void updateNonlocalSessionAuthNotes(String authSessionId, Map<String, String> authNotesFragment) {
        if (authSessionId == null) {
            return;
        }

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(
          InfinispanAuthenticationSessionProviderFactory.AUTHENTICATION_SESSION_EVENTS,
          AuthenticationSessionAuthNoteUpdateEvent.create(authSessionId, authNotesFragment),
          true,
          ClusterProvider.DCNotify.ALL_BUT_LOCAL_DC
        );
    }

    @Override
    public void close() {

    }

    public Cache<String, AuthenticationSessionEntity> getCache() {
        return cache;
    }
}
