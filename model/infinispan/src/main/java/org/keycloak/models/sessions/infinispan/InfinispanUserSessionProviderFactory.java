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

import java.io.Serializable;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.initializer.InfinispanUserSessionInitializer;
import org.keycloak.models.sessions.infinispan.initializer.OfflineUserSessionLoader;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProviderFactory.class);

    private Config.Scope config;

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, SessionEntity> cache = connections.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);
        Cache<String, SessionEntity> offlineSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME);
        Cache<LoginFailureKey, LoginFailureEntity> loginFailures = connections.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);

        return new InfinispanUserSessionProvider(session, cache, offlineSessionsCache, loginFailures);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        // Max count of worker errors. Initialization will end with exception when this number is reached
        final int maxErrors = config.getInt("maxErrors", 20);

        // Count of sessions to be computed in each segment
        final int sessionsPerSegment = config.getInt("sessionsPerSegment", 100);

        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof PostMigrationEvent) {
                    loadPersistentSessions(factory, maxErrors, sessionsPerSegment);
                }
            }
        });
    }


    @Override
    public void loadPersistentSessions(final KeycloakSessionFactory sessionFactory, final int maxErrors, final int sessionsPerSegment) {
        log.debug("Start pre-loading userSessions and clientSessions from persistent storage");

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                Cache<String, Serializable> cache = connections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                InfinispanUserSessionInitializer initializer = new InfinispanUserSessionInitializer(sessionFactory, cache, new OfflineUserSessionLoader(), maxErrors, sessionsPerSegment, "offlineUserSessions");
                initializer.initCache();
                initializer.loadPersistentSessions();
            }

        });

        log.debug("Pre-loading userSessions and clientSessions from persistent storage finished");
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "infinispan";
    }

}

