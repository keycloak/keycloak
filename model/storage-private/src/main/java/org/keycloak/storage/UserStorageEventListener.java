package org.keycloak.storage;

import java.util.stream.Stream;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.StorageProviderRealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.storage.UserStorageProviderModel.SyncMode;
import org.keycloak.storage.user.ImportSynchronization;

import org.jboss.logging.Logger;

import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

public final class UserStorageEventListener implements ClusterListener, ProviderEventListener {

    private static final Logger logger = Logger.getLogger(UserStorageEventListener.class);
    private static final String USER_STORAGE_TASK_KEY = "user-storage";

    private final KeycloakSessionFactory sessionFactory;

    public UserStorageEventListener(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void eventReceived(ClusterEvent event) {
        UserStorageProviderClusterEvent fedEvent = (UserStorageProviderClusterEvent) event;
        runJobInTransactionInRealm(fedEvent.getRealmId(),
                session -> refreshScheduledTasks(session, fedEvent.getStorageProvider(), fedEvent.isRemoved()));
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent) {
            runJobInTransaction(sessionFactory, session -> {
                session.realms().getRealmsWithProviderTypeStream(UserStorageProvider.class)
                        .forEach(realm -> {
                            try {
                                session.getContext().setRealm(realm);
                                Stream<UserStorageProviderModel> providers = ((StorageProviderRealmModel) realm).getUserStorageProvidersStream();
                                providers.forEachOrdered(provider -> reScheduleTasks(session, provider));
                            } finally {
                                session.getContext().setRealm(null);
                            }
                });

                ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);

                if (clusterProvider != null) {
                    clusterProvider.registerListener(USER_STORAGE_TASK_KEY, this);
                }
            });
        } else if (event instanceof StoreSyncEvent ev) {
            UserStorageProviderModel model = ev.getModel() == null ? null: new UserStorageProviderModel(ev.getModel());
            KeycloakSession session = ev.getSession();
            boolean removed = ev.getRemoved();
            RealmModel contextRealm = session.getContext().getRealm();
            RealmModel realm = ev.getRealm();

            try {
                session.getContext().setRealm(realm);

                if (model != null) {
                    notifyStoreSyncClusterUpdate(session, realm, model, removed);
                    refreshScheduledTasks(session, model, removed);
                } else {
                    notifyAllStoreSyncClusterUpdate(session, realm, removed);
                    ((StorageProviderRealmModel) realm).getUserStorageProvidersStream().forEachOrdered(fedProvider -> {
                        refreshScheduledTasks(session, fedProvider, removed);
                    });
                }
            } finally {
                session.getContext().setRealm(contextRealm);
            }
        }
    }

    private void cancelTasks(KeycloakSession session, UserStorageProviderModel fedProvider) {
        new UserStorageSyncTask(fedProvider, SyncMode.FULL).cancel(session);
        new UserStorageSyncTask(fedProvider, SyncMode.CHANGED).cancel(session);
    }

    private void reScheduleTasks(KeycloakSession session, UserStorageProviderModel provider) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        UserStorageProviderFactory<?> factory = (UserStorageProviderFactory<?>) sessionFactory.getProviderFactory(UserStorageProvider.class, provider.getProviderId());
        RealmModel realm = session.getContext().getRealm();

        if (!(factory instanceof ImportSynchronization)) {
            logger.debugf("Not refreshing periodic sync settings for provider '%s' in realm '%s'", provider.getName(), realm.getName());
            return;
        }

        logger.debugf("Going to refresh periodic sync settings for provider '%s' in realm '%s' with realmId '%s'. Full sync period: %d , changed users sync period: %d",
                provider.getName(), realm.getName(), realm.getId(), provider.getFullSyncPeriod(), provider.getChangedSyncPeriod());
        scheduleTask(session, provider, SyncMode.FULL);
        scheduleTask(session, provider, SyncMode.CHANGED);
    }

    private void scheduleTask(KeycloakSession session, UserStorageProviderModel provider, SyncMode mode) {
        UserStorageSyncTask task = new UserStorageSyncTask(provider, mode);

        if (!task.schedule(session)) {
            // cancel potentially dangling task
            task.cancel(session);
        }
    }

    private void notifyAllStoreSyncClusterUpdate(KeycloakSession session, RealmModel realm, boolean removed) {
        ((StorageProviderRealmModel) realm).getUserStorageProvidersStream().forEachOrdered(fedProvider ->
                notifyStoreSyncClusterUpdate(session, realm, fedProvider, removed));
    }

    // Ensure all cluster nodes are notified
    private void notifyStoreSyncClusterUpdate(KeycloakSession session, RealmModel realm, UserStorageProviderModel provider, boolean removed) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        UserStorageProviderFactory<?> factory = (UserStorageProviderFactory<?>) sessionFactory.getProviderFactory(UserStorageProvider.class, provider.getProviderId());

        if (!(factory instanceof ImportSynchronization)) {
            return;
        }

        ClusterProvider cp = session.getProvider(ClusterProvider.class);

        if (cp != null) {
            UserStorageProviderClusterEvent event = UserStorageProviderClusterEvent.createEvent(removed, realm.getId(), provider);
            cp.notify(USER_STORAGE_TASK_KEY, event, true);
        }
    }

    private void refreshScheduledTasks(KeycloakSession session, UserStorageProviderModel model, boolean removed) {
        if (removed) {
            cancelTasks(session, model);
        } else {
            reScheduleTasks(session, model);
        }
    }

    private void runJobInTransactionInRealm(String realmId, KeycloakSessionTask task) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            if (realm == null) {
                throw new RuntimeException("Failed to execute session task. Realm with id " + realmId + " not found.");
            }

            session.getContext().setRealm(realm);
            task.run(session);
        });
    }
}
