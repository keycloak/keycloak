package org.keycloak.storage;

import java.util.stream.Stream;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.StorageProviderRealmModel;
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
        String realmId = fedEvent.getRealmId();

        runJobInTransaction(sessionFactory, session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            if (realm == null) {
                throw new RuntimeException("Failed to execute session task. Realm with id " + realmId + " not found.");
            }

            session.getContext().setRealm(realm);
            refreshScheduledTasks(session, fedEvent.getStorageProvider(), fedEvent.isRemoved());
        });
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent) {
            runJobInTransaction(sessionFactory, session -> {
                session.realms().getRealmsWithProviderTypeStream(UserStorageProvider.class)
                        .forEach(realm -> {
                            try {
                                session.getContext().setRealm(realm);
                                getUserStorageProvidersStream(realm).forEachOrdered(provider -> reScheduleTasks(session, provider));
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
            boolean removed = ev.getRemoved();
            String realmId = ev.getRealm().getId();

            runJobInTransaction(sessionFactory, session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                if (realm == null) {
                    return;
                }
                session.getContext().setRealm(realm);

                if (model != null) {
                    refreshScheduledTasks(session, model, removed);
                    notifyStoreSyncClusterUpdate(session, realm, model, removed);
                } else {
                    getUserStorageProvidersStream(realm).forEachOrdered(fedProvider -> {
                        refreshScheduledTasks(session, fedProvider, removed);
                        notifyStoreSyncClusterUpdate(session, realm, fedProvider, removed);
                    });
                }
            });
        }
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
            new UserStorageSyncTask(model, SyncMode.FULL).cancel(session);
            new UserStorageSyncTask(model, SyncMode.CHANGED).cancel(session);
        } else {
            reScheduleTasks(session, model);
        }
    }

    private Stream<UserStorageProviderModel> getUserStorageProvidersStream(RealmModel realm) {
        if (realm instanceof StorageProviderRealmModel s) {
            return s.getUserStorageProvidersStream();
        }

        return Stream.empty();
    }
}
