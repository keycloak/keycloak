/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.datastore;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.services.scheduled.ClearExpiredClientInitialAccessTokens;
import org.keycloak.services.scheduled.ClearExpiredEvents;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.DatastoreProviderFactory;
import org.keycloak.storage.LegacyStoreMigrateRepresentationEvent;
import org.keycloak.storage.LegacyStoreSyncEvent;
import org.keycloak.storage.managers.UserStorageSyncManager;
import org.keycloak.timer.TimerProvider;

public class LegacyDatastoreProviderFactory implements DatastoreProviderFactory, ProviderEventListener, EnvironmentDependentProviderFactory {

    private static final String PROVIDER_ID = "legacy";
    private long clientStorageProviderTimeout;
    private long roleStorageProviderTimeout;
    private Runnable onClose;

    @Override
    public DatastoreProvider create(KeycloakSession session) {
        return new LegacyDatastoreProvider(this, session);
    }

    @Override
    public void init(Scope config) {
        clientStorageProviderTimeout = Config.scope("client").getLong("storageProviderTimeout", 3000L);
        roleStorageProviderTimeout = Config.scope("role").getLong("storageProviderTimeout", 3000L);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this);
        onClose = () -> factory.unregister(this);
    }

    @Override
    public void close() {
        onClose.run();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    public long getClientStorageProviderTimeout() {
        return clientStorageProviderTimeout;
    }

    public long getRoleStorageProviderTimeout() {
        return roleStorageProviderTimeout;
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent) {
            setupScheduledTasks(((PostMigrationEvent) event).getFactory());
        } else if (event instanceof LegacyStoreSyncEvent) {
            LegacyStoreSyncEvent ev = (LegacyStoreSyncEvent) event;
            UserStorageSyncManager.notifyToRefreshPeriodicSyncAll(ev.getSession(), ev.getRealm(), ev.getRemoved());
        } else if (event instanceof LegacyStoreMigrateRepresentationEvent) {
            LegacyStoreMigrateRepresentationEvent ev = (LegacyStoreMigrateRepresentationEvent) event;
            MigrationModelManager.migrateImport(ev.getSession(), ev.getRealm(), ev.getRep(), ev.isSkipUserDependent());
        }
    }    

    public static void setupScheduledTasks(final KeycloakSessionFactory sessionFactory) {
        long interval = Config.scope("scheduled").getLong("interval", 900L) * 1000;

        KeycloakSession session = sessionFactory.create();
        try {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            if (timer != null) {
                timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredEvents(), interval), interval, "ClearExpiredEvents");
                timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredClientInitialAccessTokens(), interval), interval, "ClearExpiredClientInitialAccessTokens");
                timer.schedule(new ScheduledTaskRunner(sessionFactory, new ClearExpiredUserSessions()), interval, ClearExpiredUserSessions.TASK_NAME);
                UserStorageSyncManager.bootstrapPeriodic(sessionFactory, timer);
            }
        } finally {
            session.close();
        }
    }

    @Override
    public boolean isSupported() {
        return ! Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

}
