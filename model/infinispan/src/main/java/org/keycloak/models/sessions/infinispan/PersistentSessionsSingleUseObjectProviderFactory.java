/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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


import org.infinispan.commons.api.BasicCache;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.session.RevokedTokenPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.services.scheduled.ClearExpiredRevokedTokens;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Extends the {@link InfinispanSingleUseObjectProviderFactory} to read expired tokens from the database on startup if no other node has loaded the data already.
 * @author Alexander Schwartz
 */
public class PersistentSessionsSingleUseObjectProviderFactory extends InfinispanSingleUseObjectProviderFactory {

    volatile boolean initialized;

    private final static String LOADED = "loaded" + SingleUseObjectProvider.REVOKED_KEY;

    @Override
    public PersistentSessionsSingleUseObjectProvider create(KeycloakSession session) {
        initialize(session);
        return new PersistentSessionsSingleUseObjectProvider(session, singleUseObjectCache);
    }

    private void initialize(KeycloakSession session) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    RevokedTokenPersisterProvider provider = session.getProvider(RevokedTokenPersisterProvider.class);
                    BasicCache<String, SingleUseObjectValueEntity> cache = singleUseObjectCache.get();
                    if (cache.get(LOADED) == null) {
                        // in a cluster, multiple Keycloak instances might load the same data in parallel, but that wouldn't matter
                        provider.getAllRevokedTokens().forEach(revokedToken -> {
                            long lifespanSeconds = revokedToken.expiry() - Time.currentTime();
                            if (lifespanSeconds > 0) {
                                cache.put(revokedToken.tokenId() + SingleUseObjectProvider.REVOKED_KEY, new SingleUseObjectValueEntity(Collections.EMPTY_MAP),
                                        InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanSeconds)), TimeUnit.MILLISECONDS);
                            }
                        });
                        cache.put(LOADED, new SingleUseObjectValueEntity(Collections.EMPTY_MAP));
                    }
                    initialized = true;
                }
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        super.postInit(factory);
        factory.register(new ProviderEventListener() {
            public void onEvent(ProviderEvent event) {
                if (event instanceof PostMigrationEvent) {
                    KeycloakSessionFactory sessionFactory = ((PostMigrationEvent) event).getFactory();
                    try (KeycloakSession session = sessionFactory.create()) {
                        TimerProvider timer = session.getProvider(TimerProvider.class);
                        if (timer != null) {
                            long interval = Config.scope("scheduled").getLong("interval", 900L) * 1000;
                            scheduleTask(sessionFactory, timer, interval);
                        }
                        // load sessions during startup, not on first request to avoid congestion
                        initialize(session);
                    }
                }
            }

            private void scheduleTask(KeycloakSessionFactory sessionFactory, TimerProvider timer, long interval) {
                timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredRevokedTokens(), interval), interval);
            }
        });
    }

    @Override
    public String getId() {
        return InfinispanUtils.EMBEDDED_PROVIDER_ID + "-persistence";
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isEmbeddedInfinispan() && Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS);
    }

}
