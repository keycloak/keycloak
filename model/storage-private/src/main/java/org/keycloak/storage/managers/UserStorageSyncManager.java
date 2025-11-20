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
package org.keycloak.storage.managers;

import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.TriFunction;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.StorageProviderRealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.managers.UserStorageSyncManager.UserStorageSyncTask.SyncMode;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProvider.TimerTaskContext;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserStorageSyncManager {

    private static final String USER_STORAGE_TASK_KEY = "user-storage";

    private static final Logger logger = Logger.getLogger(UserStorageSyncManager.class);

    /**
     * Check federationProviderModel of all realms and possibly start periodic sync for them
     *
     * @param sessionFactory
     * @param timer
     */
    public static void bootstrapPeriodic(final KeycloakSessionFactory sessionFactory, final TimerProvider timer) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            Stream<RealmModel> realms = session.realms().getRealmsWithProviderTypeStream(UserStorageProvider.class);
            realms.forEach(realm -> {
                Stream<UserStorageProviderModel> providers = ((StorageProviderRealmModel) realm).getUserStorageProvidersStream();
                providers.forEachOrdered(provider -> {
                    refreshPeriodicSyncForProvider(session, provider, realm);
                });
            });

            ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);
            clusterProvider.registerListener(USER_STORAGE_TASK_KEY, new UserStorageClusterListener(sessionFactory));
        });
    }

    private static class Holder {
        ExecutionResult<SynchronizationResult> result;
    }

    public static SynchronizationResult syncAllUsers(final KeycloakSessionFactory sessionFactory, final String realmId, final UserStorageProviderModel provider) {
        return syncUsers(sessionFactory, provider.getChangedSyncPeriod(), realmId, provider, (sf, factory, pm) -> {
            return factory.sync(sessionFactory, realmId, pm);
        });
    }

    public static SynchronizationResult syncChangedUsers(final KeycloakSessionFactory sessionFactory, final String realmId, final UserStorageProviderModel provider) {
        return syncUsers(sessionFactory, provider.getChangedSyncPeriod(), realmId, provider, (sf, factory, pm) -> {
            // See when we did last sync.
            int oldLastSync = provider.getLastSync();
            return factory.syncSince(Time.toDate(oldLastSync), sessionFactory, realmId, provider);
        });
    }

    private static SynchronizationResult syncUsers(final KeycloakSessionFactory sessionFactory, int period, final String realmId, final UserStorageProviderModel provider, TriFunction<KeycloakSessionFactory, ImportSynchronization, UserStorageProviderModel, SynchronizationResult> syncFunction) {
        UserStorageProviderFactory<?> factory = (UserStorageProviderFactory<?>) sessionFactory.getProviderFactory(UserStorageProvider.class, provider.getProviderId());

        if (!(factory instanceof ImportSynchronization) || !provider.isImportEnabled() || !provider.isEnabled()) {
            return SynchronizationResult.ignored();
        }

        Holder holder = new Holder();

        // Ensure not executed concurrently on this or any other cluster node
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);
            // shared key for "full" and "changed" . Improve if needed
            String taskKey = provider.getId() + "::sync";
            // 30 seconds minimal timeout for now
            int timeout = Math.max(30, period);

            holder.result = clusterProvider.executeIfNotExecuted(taskKey, timeout, () -> {
                RealmModel realm = session.realms().getRealm(realmId);

                session.getContext().setRealm(realm);

                // Need to load component again in this transaction for updated data
                ComponentModel storageComponent = realm.getComponent(provider.getId());
                UserStorageProviderModel storageModel = new UserStorageProviderModel(storageComponent);
                SynchronizationResult result = syncFunction.apply(sessionFactory, (ImportSynchronization) factory, storageModel);

                if (!result.isIgnored()) {
                    updateLastSyncInterval(sessionFactory, storageModel, realmId, Time.currentTime());
                }

                return result;
            });
        });

        if (holder.result == null || !holder.result.isExecuted()) {
            logger.debugf("syncing users for federation provider %s was ignored as it's already in progress", provider.getName());
            return SynchronizationResult.ignored();
        } else {
            return holder.result.getResult();
        }
    }

    public static void notifyToRefreshPeriodicSyncAll(KeycloakSession session, RealmModel realm, boolean removed) {
        ((StorageProviderRealmModel) realm).getUserStorageProvidersStream().forEachOrdered(fedProvider ->
                notifyToRefreshPeriodicSync(session, realm, fedProvider, removed));
    }

    // Ensure all cluster nodes are notified
    public static void notifyToRefreshPeriodicSync(KeycloakSession session, RealmModel realm, UserStorageProviderModel provider, boolean removed) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        UserStorageProviderFactory<?> factory = (UserStorageProviderFactory<?>) sessionFactory.getProviderFactory(UserStorageProvider.class, provider.getProviderId());

        if (!(factory instanceof ImportSynchronization)) {
            return;

        }

        ClusterProvider cp = session.getProvider(ClusterProvider.class);

        if (cp != null) {
            UserStorageProviderClusterEvent event = UserStorageProviderClusterEvent.createEvent(removed, realm.getId(), provider);
            cp.notify(USER_STORAGE_TASK_KEY, event, false);
        }
    }


    // Executed once it receives notification that some UserFederationProvider was created or updated
    protected static void refreshPeriodicSyncForProvider(KeycloakSession session, UserStorageProviderModel provider, RealmModel realm) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        UserStorageProviderFactory<?> factory = (UserStorageProviderFactory<?>) sessionFactory.getProviderFactory(UserStorageProvider.class, provider.getProviderId());

        if (!(factory instanceof ImportSynchronization)) {
            logger.debugf("Not refreshing periodic sync settings for provider '%s' in realm '%s'", provider.getName(), realm.getName());
            return;
        }

        logger.debugf("Going to refresh periodic sync settings for provider '%s' in realm '%s' with realmId '%s'. Full sync period: %d , changed users sync period: %d",
                provider.getName(), realm.getName(), realm.getId(), provider.getFullSyncPeriod(), provider.getChangedSyncPeriod());
        scheduleUserStorageSyncTask(session, provider, UserStorageSyncTask.SyncMode.FULL, provider.getFullSyncPeriod(), realm);
        scheduleUserStorageSyncTask(session, provider, SyncMode.CHANGED, provider.getChangedSyncPeriod(), realm);
    }

    private static void scheduleUserStorageSyncTask(KeycloakSession session, UserStorageProviderModel provider, UserStorageSyncTask.SyncMode mode, int period, RealmModel realm) {
        String syncTaskName = createSyncTaskName(provider, mode);
        TimerProvider timer = session.getProvider(TimerProvider.class);

        if (timer == null) {
            logger.warnf("TimerProvider not available. Can't schedule user storage sync task for provider '%s' in realm '%s'", provider.getName(), realm.getName());
            return;
        }

        if (provider.isImportEnabled() && provider.isEnabled() && period > 0) {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            UserStorageSyncTask task = new UserStorageSyncTask(provider, realm, sessionFactory, mode);
            logger.debugf("Scheduling user periodic sync task '%s' for user storage provider provider '%s' in realm '%s' with period %d seconds", syncTaskName, provider.getName(), realm.getName(), provider.getChangedSyncPeriod());
            timer.schedule(task, period * 1000L, syncTaskName);
        } else {
            if (!provider.isEnabled()) {
                logger.debugf("Not scheduling periodic sync settings for provider '%s' in realm '%s'. Provider is disabled", provider.getName(), realm.getName());
                return;
            }

            if (!provider.isImportEnabled()) {
                logger.debugf("Not scheduling periodic sync settings for provider '%s' in realm '%s'. Import users is disabled", provider.getName(), realm.getName());
                return;
            }

            // cancel potentially dangling task
            logger.debugf("Cancelling any running user periodic sync task '%s' for user storage provider provider '%s' in realm '%s'", syncTaskName, provider.getName(), realm.getName());
            timer.cancelTask(syncTaskName);
        }
    }

    public static class UserStorageSyncTask implements Runnable {

        public enum SyncMode {
            FULL, CHANGED
        }

        private final UserStorageProviderModel provider;
        private final RealmModel realm;
        private final KeycloakSessionFactory sessionFactory;
        private final SyncMode syncMode;

        public UserStorageSyncTask(UserStorageProviderModel provider, RealmModel realm, KeycloakSessionFactory sessionFactory, SyncMode syncMode) {
            this.provider = provider;
            this.realm = realm;
            this.sessionFactory = sessionFactory;
            this.syncMode = syncMode;
        }

        @Override
        public void run() {
            try {
                boolean shouldPerformSync = shouldPerformNewPeriodicSync(provider.getLastSync(), provider.getChangedSyncPeriod());

                if (!shouldPerformSync) {
                    logger.debugf("Ignored periodic %s users-sync with storage provider %s due small time since last sync in realm %s", //
                            syncMode, provider.getName(), realm.getName());
                    return;
                }

                switch (syncMode) {
                    case FULL:
                        syncAllUsers(sessionFactory, realm.getId(), provider);
                        break;
                    case CHANGED:
                        syncChangedUsers(sessionFactory, realm.getId(), provider);
                        break;
                }
            } catch (Throwable t) {
                logger.errorf(t, "Error occurred during %s users-sync in realm %s", //
                        syncMode, realm.getName());
            }
        }
    }

    public static String createSyncTaskName(UserStorageProviderModel model, UserStorageSyncTask.SyncMode syncMode) {
        return UserStorageSyncTask.class.getSimpleName() + "-" + model.getId() + "-" + syncMode;
    }

    // Skip syncing if there is short time since last sync time.
    private static boolean shouldPerformNewPeriodicSync(int lastSyncTime, int period) {
        if (lastSyncTime <= 0) {
            return true;
        }

        int currentTime = Time.currentTime();
        int timeSinceLastSync = currentTime - lastSyncTime;

        return (timeSinceLastSync * 2 > period);
    }

    // Executed once it receives notification that some UserFederationProvider was removed
    protected static void removePeriodicSyncForProvider(TimerProvider timer, UserStorageProviderModel fedProvider) {
        cancelPeriodicSyncForProviderIfPresent(timer, fedProvider, UserStorageSyncTask.SyncMode.FULL);
        cancelPeriodicSyncForProviderIfPresent(timer, fedProvider, UserStorageSyncTask.SyncMode.CHANGED);
    }

    protected static void cancelPeriodicSyncForProviderIfPresent(TimerProvider timer, UserStorageProviderModel providerModel, UserStorageSyncTask.SyncMode syncMode) {
        String taskName = createSyncTaskName(providerModel, syncMode);
        TimerTaskContext existingTask = timer.cancelTask(taskName);
        if (existingTask != null) {
            logger.debugf("Cancelled periodic sync task with task-name '%s' for provider with id '%s' and name '%s'",
                    taskName, providerModel.getId(), providerModel.getName());
        }
    }

    // Update interval of last sync for given UserFederationProviderModel. Do it in separate transaction
    private static void updateLastSyncInterval(final KeycloakSessionFactory sessionFactory, UserStorageProviderModel provider, final String realmId, final int lastSync) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                RealmModel persistentRealm = session.realms().getRealm(realmId);
                ((StorageProviderRealmModel) persistentRealm).getUserStorageProvidersStream()
                        .filter(persistentFedProvider -> Objects.equals(provider.getId(), persistentFedProvider.getId()))
                        .forEachOrdered(persistentFedProvider -> {
                            // Update persistent provider in DB
                            persistentFedProvider.setLastSync(lastSync);
                            persistentRealm.updateComponent(persistentFedProvider);

                            // Update "cached" reference
                            provider.setLastSync(lastSync);
                        });
            }
        });
    }


    private static class UserStorageClusterListener implements ClusterListener {

        private final KeycloakSessionFactory sessionFactory;

        public UserStorageClusterListener(KeycloakSessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
        }

        @Override
        public void eventReceived(ClusterEvent event) {
            final UserStorageProviderClusterEvent fedEvent = (UserStorageProviderClusterEvent) event;
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                @Override
                public void run(KeycloakSession session) {
                    TimerProvider timer = session.getProvider(TimerProvider.class);
                    if (fedEvent.isRemoved()) {
                        removePeriodicSyncForProvider(timer, fedEvent.getStorageProvider());
                    } else {
                        RealmModel realm = session.realms().getRealm(fedEvent.getRealmId());
                        refreshPeriodicSyncForProvider(session, fedEvent.getStorageProvider(), realm);
                    }
                }

            });
        }
    }


    // Send to cluster during each update or remove of federationProvider, so all nodes can update sync periods
    @ProtoTypeId(65540)
    public static class UserStorageProviderClusterEvent implements ClusterEvent {

        private boolean removed;
        private String realmId;
        private UserStorageProviderModel storageProvider;

        @ProtoField(1)
        public boolean isRemoved() {
            return removed;
        }

        public void setRemoved(boolean removed) {
            this.removed = removed;
        }

        @ProtoField(2)
        public String getRealmId() {
            return realmId;
        }

        public void setRealmId(String realmId) {
            this.realmId = realmId;
        }

        @ProtoField(3)
        public UserStorageProviderModel getStorageProvider() {
            return storageProvider;
        }

        public void setStorageProvider(UserStorageProviderModel federationProvider) {
            this.storageProvider = federationProvider;
        }

        public static UserStorageProviderClusterEvent createEvent(boolean removed, String realmId, UserStorageProviderModel provider) {
            UserStorageProviderClusterEvent notification = new UserStorageProviderClusterEvent();
            notification.setRemoved(removed);
            notification.setRealmId(realmId);
            notification.setStorageProvider(provider);
            return notification;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserStorageProviderClusterEvent that = (UserStorageProviderClusterEvent) o;
            return removed == that.removed && Objects.equals(realmId, that.realmId) && Objects.equals(storageProvider.getId(), that.storageProvider.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(removed, realmId, storageProvider.getId());
        }
    }

}
