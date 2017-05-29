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

    private volatile Cache<String, AuthenticationSessionEntity> authSessionsCache;

    public static final String AUTHENTICATION_SESSION_EVENTS = "AUTHENTICATION_SESSION_EVENTS";

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public AuthenticationSessionProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanAuthenticationSessionProvider(session, authSessionsCache);
    }

    private void updateAuthNotes(ClusterEvent clEvent) {
        if (! (clEvent instanceof AuthenticationSessionAuthNoteUpdateEvent)) {
            return;
        }

        AuthenticationSessionAuthNoteUpdateEvent event = (AuthenticationSessionAuthNoteUpdateEvent) clEvent;
        AuthenticationSessionEntity authSession = this.authSessionsCache.get(event.getAuthSessionId());
        updateAuthSession(authSession, event.getAuthNotesFragment());
    }

    private static void updateAuthSession(AuthenticationSessionEntity authSession, Map<String, String> authNotesFragment) {
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

                    ClusterProvider cluster = session.getProvider(ClusterProvider.class);
                    cluster.registerListener(AUTHENTICATION_SESSION_EVENTS, this::updateAuthNotes);

                    log.debugf("[%s] Registered cluster listeners", authSessionsCache.getCacheManager().getAddress());
                }
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "infinispan";
    }
}
