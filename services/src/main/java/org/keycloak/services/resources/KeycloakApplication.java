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
public abstract class KeycloakApplication extends Application {

    private static final String KC_TMPDIR = "kc.io.tmpdir";

    private static final Logger logger = Logger.getLogger(KeycloakApplication.class);

    private static KeycloakSessionFactory sessionFactory;

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
        KeycloakApplication.sessionFactory = createSessionFactory();

        setTransactionTimeout();
        var exportImportManager = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, session -> {
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

        resetTransactionTimeout();
        sessionFactory.publish(new PostMigrationEvent(sessionFactory));
    }

    protected int getTransactionTimeout(KeycloakSessionFactory sessionFactory) {
        return Math.toIntExact(TimeUnit.MINUTES.toSeconds(5));
    }

    protected void shutdown() {
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

    protected abstract KeycloakSessionFactory createSessionFactory();

    public static KeycloakSessionFactory getSessionFactory() {
        return sessionFactory;
    }

    private void setTransactionTimeout() {
        try {
            var transactionTimeoutSeconds = getTransactionTimeout(sessionFactory);
            KeycloakModelUtils.setTransactionLimit(sessionFactory, transactionTimeoutSeconds);
        } catch (Exception e) {
            logger.debug("Failed to set the transaction timeout, using the default value");
        }
    }

    private void resetTransactionTimeout() {
        try {
            KeycloakModelUtils.setTransactionLimit(sessionFactory, 0);
        } catch (Exception e) {
            logger.debug("Failed to reset the transaction timeout");
        }
    }

}
