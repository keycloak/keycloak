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

import java.util.Set;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserLoginFailureProviderFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.sessions.infinispan.changes.CacheHolder;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangesUtils;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.events.AbstractUserSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveAllUserLoginFailuresEvent;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionProvider;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class InfinispanUserLoginFailureProviderFactory implements UserLoginFailureProviderFactory<InfinispanUserLoginFailureProvider>, EnvironmentDependentProviderFactory, ProviderEventListener {

    private static final Logger log = Logger.getLogger(InfinispanUserLoginFailureProviderFactory.class);
    public static final String REALM_REMOVED_SESSION_EVENT = "REALM_REMOVED_EVENT_SESSIONS";
    public static final String REMOVE_ALL_LOGIN_FAILURES_EVENT = "REMOVE_ALL_LOGIN_FAILURES_EVENT";

    private CacheHolder<LoginFailureKey, LoginFailureEntity> cacheHolder;

    @Override
    public InfinispanUserLoginFailureProvider create(KeycloakSession session) {
        return new InfinispanUserLoginFailureProvider(session, createTransaction(session));
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        factory.register(this);
        try (var session = factory.create()) {
            cacheHolder = InfinispanChangesUtils.createWithCache(session, LOGIN_FAILURE_CACHE_NAME, SessionTimeouts::getLoginFailuresLifespanMs, SessionTimeouts::getLoginFailuresMaxIdleMs);
        }
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanConnectionProvider.class, InfinispanTransactionProvider.class);
    }

    protected void registerClusterListeners(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        cluster.registerListener(REALM_REMOVED_SESSION_EVENT,
                new AbstractUserSessionClusterListener<RealmRemovedSessionEvent, UserLoginFailureProvider>(sessionFactory, UserLoginFailureProvider.class) {

                    @Override
                    protected void eventReceived(UserLoginFailureProvider provider, RealmRemovedSessionEvent sessionEvent) {
                        if (provider instanceof InfinispanUserLoginFailureProvider) {
                            ((InfinispanUserLoginFailureProvider) provider).removeAllLocalUserLoginFailuresEvent(sessionEvent.getRealmId());
                        }
                    }
        });

        cluster.registerListener(REMOVE_ALL_LOGIN_FAILURES_EVENT,
                new AbstractUserSessionClusterListener<RemoveAllUserLoginFailuresEvent, UserLoginFailureProvider>(sessionFactory, UserLoginFailureProvider.class) {

            @Override
            protected void eventReceived(UserLoginFailureProvider provider, RemoveAllUserLoginFailuresEvent sessionEvent) {
                if (provider instanceof InfinispanUserLoginFailureProvider) {
                    ((InfinispanUserLoginFailureProvider) provider).removeAllLocalUserLoginFailuresEvent(sessionEvent.getRealmId());
                }
            }

        });

        log.debug("Registered cluster listeners");
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

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent pme) {
            KeycloakModelUtils.runJobInTransaction(pme.getFactory(), this::registerClusterListeners);
        } else if (event instanceof UserModel.UserRemovedEvent userRemovedEvent) {
            UserLoginFailureProvider provider = userRemovedEvent.getKeycloakSession().getProvider(UserLoginFailureProvider.class, getId());
            provider.removeUserLoginFailure(userRemovedEvent.getRealm(), userRemovedEvent.getUser().getId());
        }
    }

    private InfinispanChangelogBasedTransaction<LoginFailureKey, LoginFailureEntity> createTransaction(KeycloakSession session) {
        var tx = new InfinispanChangelogBasedTransaction<>(session, cacheHolder);
        session.getProvider(InfinispanTransactionProvider.class).registerTransaction(tx);
        return tx;
    }
}
