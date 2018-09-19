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

import org.infinispan.Cache;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.infinispan.events.AuthenticationSessionAuthNoteUpdateEvent;
import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.events.AbstractAuthSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.ClientRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanAuthenticationSessionProviderFactory implements AuthenticationSessionProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanAuthenticationSessionProviderFactory.class);

    private InfinispanKeyGenerator keyGenerator;

    private volatile Cache<String, RootAuthenticationSessionEntity> authSessionsCache;

    public static final String PROVIDER_ID = "infinispan";

    public static final String AUTHENTICATION_SESSION_EVENTS = "AUTHENTICATION_SESSION_EVENTS";

    public static final String REALM_REMOVED_AUTHSESSION_EVENT = "REALM_REMOVED_EVENT_AUTHSESSIONS";

    public static final String CLIENT_REMOVED_AUTHSESSION_EVENT = "CLIENT_REMOVED_SESSION_AUTHSESSIONS";

    @Override
    public void init(Config.Scope config) {

    }


    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof PostMigrationEvent) {

                    KeycloakModelUtils.runJobInTransaction(factory, (KeycloakSession session) -> {

                        registerClusterListeners(session);

                    });
                }
            }
        });
    }


    protected void registerClusterListeners(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        cluster.registerListener(REALM_REMOVED_AUTHSESSION_EVENT, new AbstractAuthSessionClusterListener<RealmRemovedSessionEvent>(sessionFactory) {

            @Override
            protected void eventReceived(KeycloakSession session, InfinispanAuthenticationSessionProvider provider, RealmRemovedSessionEvent sessionEvent) {
                provider.onRealmRemovedEvent(sessionEvent.getRealmId());
            }

        });

        cluster.registerListener(CLIENT_REMOVED_AUTHSESSION_EVENT, new AbstractAuthSessionClusterListener<ClientRemovedSessionEvent>(sessionFactory) {

            @Override
            protected void eventReceived(KeycloakSession session, InfinispanAuthenticationSessionProvider provider, ClientRemovedSessionEvent sessionEvent) {
                provider.onClientRemovedEvent(sessionEvent.getRealmId(), sessionEvent.getClientUuid());
            }
        });

        log.debug("Registered cluster listeners");
    }


    @Override
    public AuthenticationSessionProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanAuthenticationSessionProvider(session, keyGenerator, authSessionsCache);
    }

    private void updateAuthNotes(ClusterEvent clEvent) {
        if (! (clEvent instanceof AuthenticationSessionAuthNoteUpdateEvent)) {
            return;
        }

        AuthenticationSessionAuthNoteUpdateEvent event = (AuthenticationSessionAuthNoteUpdateEvent) clEvent;
        RootAuthenticationSessionEntity authSession = this.authSessionsCache.get(event.getAuthSessionId());
        updateAuthSession(authSession, event.getTabId(), event.getAuthNotesFragment());
    }


    private static void updateAuthSession(RootAuthenticationSessionEntity rootAuthSession, String tabId, Map<String, String> authNotesFragment) {
        if (rootAuthSession == null) {
            return;
        }

        AuthenticationSessionEntity authSession = rootAuthSession.getAuthenticationSessions().get(tabId);

        if (authSession != null) {
            if (authSession.getAuthNotes() == null) {
                authSession.setAuthNotes(new ConcurrentHashMap<>());
            }

            for (Entry<String, String> me : authNotesFragment.entrySet()) {
                String value = me.getValue();
                if (value == null) {
                    authSession.getAuthNotes().remove(me.getKey());
                } else {
                    authSession.getAuthNotes().put(me.getKey(), value);
                }
            }
        }
    }

    private void lazyInit(KeycloakSession session) {
        if (authSessionsCache == null) {
            synchronized (this) {
                if (authSessionsCache == null) {
                    InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                    authSessionsCache = connections.getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME);

                    keyGenerator = new InfinispanKeyGenerator();

                    ClusterProvider cluster = session.getProvider(ClusterProvider.class);
                    cluster.registerListener(AUTHENTICATION_SESSION_EVENTS, this::updateAuthNotes);

                    log.debugf("[%s] Registered cluster listeners", authSessionsCache.getCacheManager().getAddress());
                }
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
