package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;
import org.keycloak.util.Time;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UsersSyncManager {

    protected static final Logger logger = Logger.getLogger(UsersSyncManager.class);

    /**
     * Check federationProviderModel of all realms and possibly start periodic sync for them
     *
     * @param sessionFactory
     * @param timer
     */
    public void bootstrapPeriodic(final KeycloakSessionFactory sessionFactory, final TimerProvider timer) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                List<RealmModel> realms = session.realms().getRealms();
                for (final RealmModel realm : realms) {
                    List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();
                    for (final UserFederationProviderModel fedProvider : federationProviders) {
                        refreshPeriodicSyncForProvider(sessionFactory, timer, fedProvider, realm.getId());
                    }
                }
            }
        });
    }

    public void syncAllUsers(final KeycloakSessionFactory sessionFactory, String realmId, final UserFederationProviderModel fedProvider) {
        final UserFederationProviderFactory fedProviderFactory = (UserFederationProviderFactory) sessionFactory.getProviderFactory(UserFederationProvider.class, fedProvider.getProviderName());
        updateLastSyncInterval(sessionFactory, fedProvider, realmId);
        fedProviderFactory.syncAllUsers(sessionFactory, realmId, fedProvider);
    }

    public void syncChangedUsers(final KeycloakSessionFactory sessionFactory, String realmId, final UserFederationProviderModel fedProvider) {
        final UserFederationProviderFactory fedProviderFactory = (UserFederationProviderFactory) sessionFactory.getProviderFactory(UserFederationProvider.class, fedProvider.getProviderName());

        // See when we did last sync.
        int oldLastSync = fedProvider.getLastSync();
        updateLastSyncInterval(sessionFactory, fedProvider, realmId);
        fedProviderFactory.syncChangedUsers(sessionFactory, realmId, fedProvider, Time.toDate(oldLastSync));
    }

    public void refreshPeriodicSyncForProvider(final KeycloakSessionFactory sessionFactory, TimerProvider timer, final UserFederationProviderModel fedProvider, final String realmId) {
        if (fedProvider.getFullSyncPeriod() > 0) {
            // We want periodic full sync for this provider
            timer.schedule(new Runnable() {

                @Override
                public void run() {
                    try {
                        syncAllUsers(sessionFactory, realmId, fedProvider);
                    } catch (Throwable t) {
                        logger.error("Error occurred during full sync of users", t);
                    }
                }

            }, fedProvider.getFullSyncPeriod() * 1000, fedProvider.getId() + "-FULL");
        } else {
            timer.cancelTask(fedProvider.getId() + "-FULL");
        }

        if (fedProvider.getChangedSyncPeriod() > 0) {
            // We want periodic sync of just changed users for this provider
            timer.schedule(new Runnable() {

                @Override
                public void run() {
                    try {
                        syncChangedUsers(sessionFactory, realmId, fedProvider);
                    } catch (Throwable t) {
                        logger.error("Error occurred during sync of changed users", t);
                    }
                }

            }, fedProvider.getChangedSyncPeriod() * 1000, fedProvider.getId() + "-CHANGED");

        } else {
            timer.cancelTask(fedProvider.getId() + "-CHANGED");
        }
    }

    public void removePeriodicSyncForProvider(TimerProvider timer, final UserFederationProviderModel fedProvider) {
        timer.cancelTask(fedProvider.getId() + "-FULL");
        timer.cancelTask(fedProvider.getId() + "-CHANGED");
    }

    // Update interval of last sync for given UserFederationProviderModel. Do it in separate transaction
    private void updateLastSyncInterval(final KeycloakSessionFactory sessionFactory, final UserFederationProviderModel fedProvider, final String realmId) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                RealmModel persistentRealm = session.realms().getRealm(realmId);
                List<UserFederationProviderModel> persistentFedProviders = persistentRealm.getUserFederationProviders();
                for (UserFederationProviderModel persistentFedProvider : persistentFedProviders) {
                    if (fedProvider.getId().equals(persistentFedProvider.getId())) {
                        // Update persistent provider in DB
                        int lastSync = Time.currentTime();
                        persistentFedProvider.setLastSync(lastSync);
                        persistentRealm.updateUserFederationProvider(persistentFedProvider);

                        // Update "cached" reference
                        fedProvider.setLastSync(lastSync);
                    }
                }
            }

        });
    }

}
