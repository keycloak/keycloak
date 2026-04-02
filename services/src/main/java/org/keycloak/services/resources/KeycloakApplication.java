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
package org.keycloak.services.resources;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Application;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.services.managers.ApplianceBootstrap;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 *
 */
public abstract class KeycloakApplication<KSF extends KeycloakSessionFactory> extends Application {

    private static final String KC_TMPDIR = "kc.io.tmpdir";

    private static final Logger logger = Logger.getLogger(KeycloakApplication.class);

    private static volatile KeycloakSessionFactory sessionFactory;
    // Set to true when bootstrap is completed. It never changes back to false.
    private static volatile boolean bootstrapCompleted = false;

    public KeycloakApplication() {
        try {
            initTmpDirectory();
            logger.debugv("Application: {0}", this.getClass().getName());
            initAndStart();
        } catch (Throwable t) {
            exit(t);
        }
    }

    public static String getTmpDirectory() {
        return System.getProperty(KC_TMPDIR, System.getProperty("java.io.tmpdir"));
    }

    protected void initTmpDirectory() {
        String dataDir = getDataDir();
        File tmpDir = new File(dataDir, "tmp");
        tmpDir.mkdirs();
        if (tmpDir.isDirectory()) {
            logger.debugf("Using server tmp directory: %s", tmpDir.getAbsolutePath());
        } else {
            logger.warnf("Temporary directory %s does not exist and it was not possible to create it.", tmpDir.getAbsolutePath());
        }
        System.setProperty(KC_TMPDIR, tmpDir.getAbsolutePath());
    }

    protected abstract void exit(Throwable t);

    protected abstract String getDataDir();

    protected void startup() {
        Profile.getInstance().logUnsupportedFeatures();
        CryptoIntegration.init(KeycloakApplication.class.getClassLoader());
        var ksf = createSessionFactory();
        sessionFactory = ksf;

        if (supportsAsyncInitialization()) {
            final var executor = Executors.newSingleThreadExecutor();
            CompletableFuture.runAsync(() -> runBootstrap(ksf), executor)
                    .exceptionally(throwable -> {
                        exit(throwable);
                        return null;
                    })
                    .thenRun(executor::shutdown);
            return;
        }

        runBootstrap(ksf);
    }

    protected boolean supportsAsyncInitialization() {
        return false;
    }

    // synchronized to prevent shutdown while running bootstrapping
    private synchronized void runBootstrap(KSF keycloakSessionFactory) {
        var startTime = System.nanoTime();

        initKeycloakSessionFactory(keycloakSessionFactory);
        setTransactionTimeout(keycloakSessionFactory);
        var exportImportManager = KeycloakModelUtils.runJobInTransactionWithResult(keycloakSessionFactory, session -> {
            DBLockManager dbLockManager = new DBLockManager(session);
            dbLockManager.checkForcedUnlock();
            DBLockProvider dbLock = dbLockManager.getDBLock();
            dbLock.waitForLock(DBLockProvider.Namespace.KEYCLOAK_BOOT);
            try {
                return bootstrap(session);
            } finally {
                dbLock.releaseLock();
            }
        });

        if (exportImportManager.isRunExport()) {
            // the transaction timeout is stored in a thread-local, when exports creates a new transaction, it should fetch it.
            exportImportManager.runExport();
        }

        resetTransactionTimeout(keycloakSessionFactory);
        bootstrapCompleted = true;
        keycloakSessionFactory.publish(new PostMigrationEvent(keycloakSessionFactory));

        var duration = Duration.ofNanos(System.nanoTime() - startTime);
        logger.infof("Bootstrap completed in %f seconds", (double) duration.toMillis() / 1000);
    }

    protected int getTransactionTimeout(KSF sessionFactory) {
        return Math.toIntExact(TimeUnit.MINUTES.toSeconds(5));
    }

    // synchronized to prevent shutdown while running bootstrapping
    protected synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    // Bootstrap master realm, import realms and create admin user.
    protected ExportImportManager bootstrap(KeycloakSession session) {
        logger.debug("bootstrap");
        boolean existing = ExportImportConfig.isSingleTransaction();
        ExportImportConfig.setSingleTransaction(true);
        try {
            ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
            var exportImportManager = new ExportImportManager(session);
            var newInstall = applianceBootstrap.isNewInstall();
            if (newInstall) {
                if (!exportImportManager.isImportMasterIncluded()) {
                    applianceBootstrap.createMasterRealm();
                }
                // these are also running in the initial bootstrap transaction - if there is a problem, the server won't be initialized at all
                exportImportManager.runImport();
                createTemporaryAdmin(session);
            } else {
                exportImportManager.runImport();
            }
            return exportImportManager;
        } finally {
            ExportImportConfig.setSingleTransaction(existing);
        }
    }

    protected abstract void createTemporaryAdmin(KeycloakSession session);

    protected abstract void initAndStart();

    protected abstract KSF createSessionFactory();

    protected abstract void initKeycloakSessionFactory(KSF ksf);

    public static KeycloakSessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static boolean isBootstrapCompleted() {
        return bootstrapCompleted;
    }

    private void setTransactionTimeout(KSF keycloakSessionFactory) {
        try {
            var transactionTimeoutSeconds = getTransactionTimeout(keycloakSessionFactory);
            KeycloakModelUtils.setTransactionLimit(keycloakSessionFactory, transactionTimeoutSeconds);
        } catch (Exception e) {
            logger.debug("Failed to set the transaction timeout, using the default value");
        }
    }

    private void resetTransactionTimeout(KSF keycloakSessionFactory) {
        try {
            KeycloakModelUtils.setTransactionLimit(keycloakSessionFactory, 0);
        } catch (Exception e) {
            logger.debug("Failed to reset the transaction timeout");
        }
    }

}
