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

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.common.util.Time;
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
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProvider.TimerTaskContext;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

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
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                Stream<RealmModel> realms = session.realms().getRealmsWithProviderTypeStream(UserStorageProvider.class);
                realms.forEach(realm -> {
                    Stream<UserStorageProviderModel> providers = ((StorageProviderRealmModel) realm).getUserStorageProvidersStream();
                    providers.forEachOrdered(provider -> {
                        UserStorageProviderFactory factory = (UserStorageProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, provider.getProviderId());
                        if (factory instanceof ImportSynchronization && provider.isImportEnabled()) {
                            refreshPeriodicSyncForProvider(sessionFactory, timer, provider, realm);
                        }
                    });
                });

                ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);
                clusterProvider.registerListener(USER_STORAGE_TASK_KEY, new UserStorageClusterListener(sessionFactory));
            }
        });
    }

    private static class Holder {
        ExecutionResult<SynchronizationResult> result;
    }

    public static SynchronizationResult syncAllUsers(final KeycloakSessionFactory sessionFactory, final String realmId, final UserStorageProviderModel provider) {
        UserStorageProviderFactory factory = (UserStorageProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, provider.getProviderId());
        if (!(factory instanceof ImportSynchronization) || !provider.isImportEnabled() || !provider.isEnabled()) {
            return SynchronizationResult.ignored();

        }

        final Holder holder = new Holder();

        // Ensure not executed concurrently on this or any other cluster node
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);
                // shared key for "full" and "changed" . Improve if needed
                String taskKey = provider.getId() + "::sync";

                // 30 seconds minimal timeout for now
                int timeout = Math.max(30, provider.getFullSyncPeriod());
                holder.result = clusterProvider.executeIfNotExecuted(taskKey, timeout, new Callable<SynchronizationResult>() {

                    @Override
                    public SynchronizationResult call() throws Exception {
                        int lastSync = Time.currentTime();
                        SynchronizationResult result = ((ImportSynchronization) factory).sync(sessionFactory, realmId, provider);
                        if (!result.isIgnored()) {
                            updateLastSyncInterval(sessionFactory, provider, realmId, lastSync);
                        }
                        return result;
                    }

                });
            }

        });

        if (holder.result == null || !holder.result.isExecuted()) {
            logger.debugf("syncAllUsers for federation provider %s was ignored as it's already in progress", provider.getName());
            return SynchronizationResult.ignored();
        } else {
            return holder.result.getResult();
        }
    }

    public static SynchronizationResult syncChangedUsers(final KeycloakSessionFactory sessionFactory, final String realmId, final UserStorageProviderModel provider) {
        UserStorageProviderFactory factory = (UserStorageProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, provider.getProviderId());
        if (!(factory instanceof ImportSynchronization) || !provider.isImportEnabled() || !provider.isEnabled()) {
            return SynchronizationResult.ignored();

        }
        final Holder holder = new Holder();

        // Ensure not executed concurrently on this or any other cluster node
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);
                // shared key for "full" and "changed" . Improve if needed
                String taskKey = provider.getId() + "::sync";

                // 30 seconds minimal timeout for now
                int timeout = Math.max(30, provider.getChangedSyncPeriod());
                holder.result = clusterProvider.executeIfNotExecuted(taskKey, timeout, new Callable<SynchronizationResult>() {

                    @Override
                    public SynchronizationResult call() throws Exception {
                        // See when we did last sync.
                        int oldLastSync = provider.getLastSync();
                        int lastSync = Time.currentTime();
                        SynchronizationResult result = ((ImportSynchronization) factory).syncSince(Time.toDate(oldLastSync), sessionFactory, realmId, provider);
                        if (!result.isIgnored()) {
                            updateLastSyncInterval(sessionFactory, provider, realmId, lastSync);
                        }
                        return result;
                    }

                });
            }

        });

        if (holder.result == null || !holder.result.isExecuted()) {
            logger.debugf("syncChangedUsers for federation provider %s was ignored as it's already in progress", provider.getName());
            return SynchronizationResult.ignored();
        } else {
            return holder.result.getResult();
        }
    }


    public static void notifyToRefreshPeriodicSyncAll(KeycloakSession session, RealmModel realm, boolean removed) {
        ((StorageProviderRealmModel) realm).getUserStorageProvidersStream().forEachOrdered(fedProvider ->
                notifyToRefreshPeriodicSync(session, realm, fedProvider, removed));
    }

    public static void notifyToRefreshPeriodicSyncSingle(KeycloakSession session, RealmModel realm, ComponentModel component, boolean removed) {
        notifyToRefreshPeriodicSync(session, realm, new UserStorageProviderModel(component), removed);
    }

    // Ensure all cluster nodes are notified
    public static void notifyToRefreshPeriodicSync(KeycloakSession session, RealmModel realm, UserStorageProviderModel provider, boolean removed) {
        UserStorageProviderFactory factory = (UserStorageProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, provider.getProviderId());
        if (!(factory instanceof ImportSynchronization) || !provider.isImportEnabled()) {
            return;

        }
        final ClusterProvider cp = session.getProvider(ClusterProvider.class);
        if (cp != null) {
            UserStorageProviderClusterEvent event = UserStorageProviderClusterEvent.createEvent(removed, realm.getId(), provider);
            cp.notify(USER_STORAGE_TASK_KEY, event, false);
        }
    }


    // Executed once it receives notification that some UserFederationProvider was created or updated
    protected static void refreshPeriodicSyncForProvider(final KeycloakSessionFactory sessionFactory, TimerProvider timer, final UserStorageProviderModel provider, final RealmModel realm) {
        logger.debugf("Going to refresh periodic sync settings for provider '%s' in realm '%s' with realmId '%s'. Full sync period: %d , changed users sync period: %d",
                provider.getName(), realm.getName(), realm.getId(), provider.getFullSyncPeriod(), provider.getChangedSyncPeriod());

        String fullSyncTaskName = createSyncTaskName(provider, UserStorageSyncTask.SyncMode.FULL);
        if (provider.getFullSyncPeriod() > 0) {
            // schedule periodic full sync for this provider
            UserStorageSyncTask task = new UserStorageSyncTask(provider, realm, sessionFactory, UserStorageSyncTask.SyncMode.FULL);
            timer.schedule(task, provider.getFullSyncPeriod() * 1000L, fullSyncTaskName);
        } else {
            // cancel potentially dangling task
            timer.cancelTask(fullSyncTaskName);
        }

        String changedSyncTaskName = createSyncTaskName(provider, UserStorageSyncTask.SyncMode.CHANGED);
        if (provider.getChangedSyncPeriod() > 0) {
            // schedule periodic changed user sync for this provider
            UserStorageSyncTask task = new UserStorageSyncTask(provider, realm, sessionFactory, UserStorageSyncTask.SyncMode.CHANGED);
            timer.schedule(task, provider.getChangedSyncPeriod() * 1000L, changedSyncTaskName);
        } else {
            // cancel potentially dangling task
            timer.cancelTask(changedSyncTaskName);
        }
    }

    public static class UserStorageSyncTask implements Runnable {

        private final UserStorageProviderModel provider;

        private final RealmModel realm;

        private final KeycloakSessionFactory sessionFactory;

        private final SyncMode syncMode;

        public static enum SyncMode {
            FULL, CHANGED
        }

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
                        refreshPeriodicSyncForProvider(sessionFactory, timer, fedEvent.getStorageProvider(), realm);
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
