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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.services.scheduled.ClearExpiredAdminEvents;
import org.keycloak.services.scheduled.ClearExpiredClientInitialAccessTokens;
import org.keycloak.services.scheduled.ClearExpiredEvents;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.DatastoreProviderFactory;
import org.keycloak.storage.StoreMigrateRepresentationEvent;
import org.keycloak.storage.StoreSyncEvent;
import org.keycloak.storage.managers.UserStorageSyncManager;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;
import java.util.Arrays;
import java.util.List;

public class DefaultDatastoreProviderFactory implements DatastoreProviderFactory, ProviderEventListener {

    private static final String PROVIDER_ID = "legacy";

    private static final Logger logger = Logger.getLogger(DefaultDatastoreProviderFactory.class);

    private long clientStorageProviderTimeout;
    private long roleStorageProviderTimeout;
    private Runnable onClose;

    @Override
    public DatastoreProvider create(KeycloakSession session) {
        return new DefaultDatastoreProvider(this, session);
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
        if (onClose != null) {
            onClose.run();
        }
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
        } else if (event instanceof StoreSyncEvent) {
            StoreSyncEvent ev = (StoreSyncEvent) event;
            UserStorageSyncManager.notifyToRefreshPeriodicSyncAll(ev.getSession(), ev.getRealm(), ev.getRemoved());
        } else if (event instanceof StoreMigrateRepresentationEvent) {
            StoreMigrateRepresentationEvent ev = (StoreMigrateRepresentationEvent) event;
            MigrationModelManager.migrateImport(ev.getSession(), ev.getRealm(), ev.getRep(), ev.isSkipUserDependent());
        }
    }    

    public void setupScheduledTasks(final KeycloakSessionFactory sessionFactory) {
        long interval = Config.scope("scheduled").getLong("interval", 900L) * 1000;

        try (KeycloakSession session = sessionFactory.create()) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            if (timer != null) {
                scheduleTasks(sessionFactory, timer, interval);
            }
        }
    }

    protected void scheduleTasks(KeycloakSessionFactory sessionFactory, TimerProvider timer, long interval) {
        for (ScheduledTask task : getScheduledTasks()) {
            scheduleTask(timer, sessionFactory, task, interval);
        }

        UserStorageSyncManager.bootstrapPeriodic(sessionFactory, timer);
    }

    protected List<ScheduledTask> getScheduledTasks() {
        return Arrays.asList(new ClearExpiredEvents(), new ClearExpiredAdminEvents(), new ClearExpiredClientInitialAccessTokens(), new ClearExpiredUserSessions());
    }

    protected void scheduleTask(TimerProvider timer, KeycloakSessionFactory sessionFactory, ScheduledTask task, long interval) {
        timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, task, interval), interval);
        logger.debugf("Scheduled cluster task %s with interval %s ms", task.getTaskName(), interval);
    }

}
