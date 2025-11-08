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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.infinispan.events.AuthenticationSessionAuthNoteUpdateEvent;
import org.keycloak.models.sessions.infinispan.changes.CacheHolder;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangesUtils;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.events.AbstractAuthSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionProvider;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanAuthenticationSessionProviderFactory implements AuthenticationSessionProviderFactory<InfinispanAuthenticationSessionProvider>, EnvironmentDependentProviderFactory, ProviderEventListener {

    private static final Logger log = Logger.getLogger(InfinispanAuthenticationSessionProviderFactory.class);

    private CacheHolder<String, RootAuthenticationSessionEntity> cacheHolder;

    private int authSessionsLimit;

    public static final String AUTH_SESSIONS_LIMIT = "authSessionsLimit";

    public static final int DEFAULT_AUTH_SESSIONS_LIMIT = 300;

    public static final String AUTHENTICATION_SESSION_EVENTS = "AUTHENTICATION_SESSION_EVENTS";

    public static final String REALM_REMOVED_AUTHSESSION_EVENT = "REALM_REMOVED_EVENT_AUTHSESSIONS";

    @Override
    public void init(Config.Scope config) {
        authSessionsLimit = getAuthSessionsLimit(config);
    }

    public static int getAuthSessionsLimit(Config.Scope config) {
        var limit = config.getInt(AUTH_SESSIONS_LIMIT, DEFAULT_AUTH_SESSIONS_LIMIT);
        // use default if provided value is not a positive number
        return limit <= 0 ? DEFAULT_AUTH_SESSIONS_LIMIT : limit;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this);
        try (var session = factory.create()) {
            cacheHolder = InfinispanChangesUtils.createWithCache(session, AUTHENTICATION_SESSIONS_CACHE_NAME, SessionTimeouts::getAuthSessionLifespanMS, SessionTimeouts::getAuthSessionMaxIdleMS, SecretGenerator.SECURE_ID_GENERATOR);
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("authSessionsLimit")
                .type("int")
                .helpText("The maximum number of concurrent authentication sessions per RootAuthenticationSession.")
                .defaultValue(DEFAULT_AUTH_SESSIONS_LIMIT)
                .add()
                .build();
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent pme) {
            KeycloakModelUtils.runJobInTransaction(pme.getFactory(), this::registerClusterListeners);
        }
    }

    protected void registerClusterListeners(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        cluster.registerListener(REALM_REMOVED_AUTHSESSION_EVENT, new AbstractAuthSessionClusterListener<RealmRemovedSessionEvent>(sessionFactory) {

            @Override
            protected void eventReceived(InfinispanAuthenticationSessionProvider provider, RealmRemovedSessionEvent sessionEvent) {
                provider.onRealmRemovedEvent(sessionEvent.getRealmId());
            }

        });
        cluster.registerListener(AUTHENTICATION_SESSION_EVENTS, this::updateAuthNotes);

        log.debug("Registered cluster listeners");
    }

    @Override
    public InfinispanAuthenticationSessionProvider create(KeycloakSession session) {
        return new InfinispanAuthenticationSessionProvider(session, createTransaction(session), authSessionsLimit);
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanConnectionProvider.class, InfinispanTransactionProvider.class);
    }

    private void updateAuthNotes(ClusterEvent clEvent) {
        if (! (clEvent instanceof AuthenticationSessionAuthNoteUpdateEvent event)) {
            return;
        }

        var distribution = cacheHolder.cache().getAdvancedCache().getDistributionManager();
        if (distribution != null && !distribution.getCacheTopology().getDistribution(event.getAuthSessionId()).isPrimary()) {
            // Distribution is null for non-clustered caches (local-cache, used by start-dev mode).
            // If not the primary owner of the key, skip event handling.
            return;
        }

        SessionEntityWrapper<RootAuthenticationSessionEntity> authSession = cacheHolder.cache().get(event.getAuthSessionId());
        updateAuthSession(authSession, event.getTabId(), event.getAuthNotesFragment());
    }

    private void updateAuthSession(SessionEntityWrapper<RootAuthenticationSessionEntity> rootAuthSessionWrapper, String tabId, Map<String, String> authNotesFragment) {
        if (rootAuthSessionWrapper == null || rootAuthSessionWrapper.getEntity() == null) {
            return;
        }

        RootAuthenticationSessionEntity rootAuthSession = rootAuthSessionWrapper.getEntity();
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

        cacheHolder.cache().replace(rootAuthSession.getId(), new SessionEntityWrapper<>(rootAuthSessionWrapper.getLocalMetadata(), rootAuthSession));
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return InfinispanUtils.EMBEDDED_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isEmbeddedInfinispan();
    }

    private InfinispanChangelogBasedTransaction<String, RootAuthenticationSessionEntity> createTransaction(KeycloakSession session) {
        var tx = new InfinispanChangelogBasedTransaction<>(session, cacheHolder);
        session.getProvider(InfinispanTransactionProvider.class).registerTransaction(tx);
        return tx;
    }
}
