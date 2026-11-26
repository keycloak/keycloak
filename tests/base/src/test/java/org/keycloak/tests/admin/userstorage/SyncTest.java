package org.keycloak.tests.admin.userstorage;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageProviderModel.SyncMode;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.federation.DummyUserFederationProviderFactory;

import org.junit.jupiter.api.Assertions;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class SyncTest {

    private static final String REALM_NAME = "sync-period";
    private static final int PERIOD = 60;
    private static final int SYNC_DURATION = 20;

    @InjectRealm(ref = REALM_NAME, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @TestOnServer
    public void fullSyncRunsOnEveryScheduledTick(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        DummyUserFederationProviderFactory dummyFactory = (DummyUserFederationProviderFactory)
                sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);

        String componentId;

        try {
            componentId = createFullSyncProvider(sessionFactory, 0);
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
        }
    }

    @TestOnServer
    public void longRunningSyncDoesNotRegressLastSync(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        String componentId;

        try {
            // The slow run (node A) starts now and records this instant as its start time.
            Time.setOffset(PERIOD);
            int slowRunStart = Time.currentTime();

            // A newer run on another node started one period later and already recorded its (larger) start time,
            // emulating the overlap allowed once the cluster lock has expired for a long-running sync.
            int concurrentLastSync = slowRunStart + PERIOD;

            // Provider whose sync runs long (SYNC_DURATION) and, while in progress, persists the newer lastSync.
            componentId = createFullSyncProvider(sessionFactory, concurrentLastSync);

            runScheduledFullSync(sessionFactory, componentId);

            // When the slow run finishes last, it must not move lastSync back to its own (older) start time
            // over the newer value written by the concurrent run.
            int lastSync = readFullLastSync(sessionFactory, componentId);
            Assertions.assertEquals(concurrentLastSync, lastSync,
                    "A slow run finishing after a newer concurrent run must not move lastSync backwards (issue #53930)");
        } finally {
            Time.setOffset(0);
        }
    }

    private String createFullSyncProvider(KeycloakSessionFactory sessionFactory, int concurrentLastSync) {
        return KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
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
            if (concurrentLastSync > 0) {
                model.getConfig().putSingle(DummyUserFederationProviderFactory.SIMULATE_CONCURRENT_LAST_SYNC, String.valueOf(concurrentLastSync));
            }

            return realm.addComponentModel(model).getId();
        });
    }

    private void runScheduledFullSync(KeycloakSessionFactory sessionFactory, String componentId) {
        UserStorageProviderModel provider = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            return new UserStorageProviderModel(realm.getComponent(componentId));
        });

        UserStoragePrivateUtil.runScheduledSync(sessionFactory, provider, SyncMode.FULL);
    }

    private int readFullLastSync(KeycloakSessionFactory sessionFactory, String componentId) {
        return KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            return new UserStorageProviderModel(realm.getComponent(componentId)).getLastSync(SyncMode.FULL);
        });
    }
}
