package org.keycloak.tests.admin.userstorage;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageProviderModel.SyncMode;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.federation.DummyUserFederationProviderFactory;

import org.junit.jupiter.api.Assertions;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class SyncTest {

    private static final String REALM_NAME = "sync-period";
    private static final int PERIOD = 60;
    private static final int SYNC_DURATION = 20;

    @TestOnServer
    public void fullSyncRunsOnEveryScheduledTick(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        DummyUserFederationProviderFactory dummyFactory = (DummyUserFederationProviderFactory)
                sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);

        String componentId;

        try {
            componentId = createFullSyncProvider(sessionFactory);
            int baseCounter = dummyFactory.getFullSyncCounter();

            Time.setOffset(0);

            // Tick 1 (offset 0): initial lastSync == 0, so the sync runs. The dummy advances the clock by SYNC_DURATION
            // to simulate a long sync, so lastSync ends up recorded at completion time.
            runScheduledFullSync(sessionFactory, componentId);
            Assertions.assertEquals(baseCounter + 1, dummyFactory.getFullSyncCounter(),
                    "The first scheduled sync should run");

            // The next timer tick fires one full period after the previous run STARTED.
            Time.setOffset(PERIOD);

            // Tick 2 (offset = PERIOD): a full period has elapsed since the previous run started, so the sync must run
            // again. Today it is wrongly skipped because lastSync was recorded at completion time
            // (only PERIOD - SYNC_DURATION has elapsed since then).
            runScheduledFullSync(sessionFactory, componentId);
            Assertions.assertEquals(baseCounter + 2, dummyFactory.getFullSyncCounter(),
                    "The scheduled sync must run on every tick, not every other tick (issue #53930)");
        } finally {
            Time.setOffset(0);
            KeycloakModelUtils.runJobInTransaction(sessionFactory, s -> {
                RealmManager realmManager = new RealmManager(s);
                RealmModel realm = realmManager.getRealmByName(REALM_NAME);
                if (realm != null) {
                    s.getContext().setRealm(realm);
                    realmManager.removeRealm(realm);
                }
            });
        }
    }

    private static String createFullSyncProvider(KeycloakSessionFactory sessionFactory) {
        return KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, s -> {
            RealmManager realmManager = new RealmManager(s);
            RealmModel realm = realmManager.createRealm(REALM_NAME);
            s.getContext().setRealm(realm);

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setName("period-dummy");
            model.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            model.setParentId(realm.getId());
            model.setImportEnabled(true);
            model.setFullSyncPeriod(PERIOD);
            model.setChangedSyncPeriod(-1);
            model.setLastSync(0, SyncMode.FULL);
            model.getConfig().putSingle(DummyUserFederationProviderFactory.SYNC_DELAY_SECONDS, String.valueOf(SYNC_DURATION));

            return realm.addComponentModel(model).getId();
        });
    }

    private static void runScheduledFullSync(KeycloakSessionFactory sessionFactory, String componentId) {
        UserStorageProviderModel provider = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            return new UserStorageProviderModel(realm.getComponent(componentId));
        });

        UserStoragePrivateUtil.runScheduledSync(sessionFactory, provider, SyncMode.FULL);
    }
}
