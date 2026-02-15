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
import java.io.IOException;
import java.nio.file.Path;

import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.ws.rs.core.Application;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.transaction.JtaTransactionManagerLookup;

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
            File tmpDir = initTmpDirectory();
            if (tmpDir.isDirectory()) {
                logger.debugf("Using server tmp directory: %s", tmpDir.getAbsolutePath());
            } else {
                logger.warnf("Temporary directory %s does not exist and it was not possible to create it.", tmpDir.getAbsolutePath());
            }
            System.setProperty(KC_TMPDIR, tmpDir.getAbsolutePath());
            logger.debugv("Application: {0}", this.getClass().getName());
            loadConfig();
        } catch (Throwable t) {
            exit(t);
        }
    }

    public static String getTmpDirectory() {
        return System.getProperty(KC_TMPDIR, System.getProperty("java.io.tmpdir"));
    }

    protected File initTmpDirectory() {
        String dataDir = getDataDir();

        File tmpDir;
        if (dataDir == null) {
            // Should happen just in non-script launch scenarios
            tmpDir = createTmpDirectory();
        } else {
            tmpDir = new File(dataDir, "tmp");
            tmpDir.mkdirs();
        }
        return tmpDir;
    }

    public static File createTmpDirectory() {
        try {
            File tmpDir = Path.of(System.getProperty("java.io.tmpdir"), "server-tmp").toFile();
            if (tmpDir.exists()) {
                org.apache.commons.io.FileUtils.deleteDirectory(tmpDir);
            }
            if (tmpDir.mkdirs()) {
                tmpDir.deleteOnExit();
            }
            return tmpDir;
        } catch (IOException ioex) {
            throw new RuntimeException("It was not possible to create temporary directory keycloak-quarkus-tmp", ioex);
        }
    }

    protected abstract void exit(Throwable t);

    protected abstract String getDataDir();

    protected void startup() {
        Profile.getInstance().logUnsupportedFeatures();
        CryptoIntegration.init(KeycloakApplication.class.getClassLoader());
        KeycloakApplication.sessionFactory = createSessionFactory();

        ExportImportManager[] exportImportManager = new ExportImportManager[1];

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession session) {
                DBLockManager dbLockManager = new DBLockManager(session);
                dbLockManager.checkForcedUnlock();
                DBLockProvider dbLock = dbLockManager.getDBLock();
                dbLock.waitForLock(DBLockProvider.Namespace.KEYCLOAK_BOOT);
                try {
                    exportImportManager[0] = bootstrap();
                } finally {
                    dbLock.releaseLock();
                }
            }
        });

        if (exportImportManager[0].isRunExport()) {
            exportImportManager[0].runExport();
        }

        sessionFactory.publish(new PostMigrationEvent(sessionFactory));
    }

    protected void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    private static class BootstrapState {
        ExportImportManager exportImportManager;
        boolean newInstall;
    }

    // Bootstrap master realm, import realms and create admin user.
    protected ExportImportManager bootstrap() {
        BootstrapState bootstrapState = new BootstrapState();

        logger.debug("bootstrap");
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession session) {
                // TODO what is the purpose of following piece of code? Leaving it as is for now.
                JtaTransactionManagerLookup lookup = (JtaTransactionManagerLookup) sessionFactory.getProviderFactory(JtaTransactionManagerLookup.class);
                if (lookup != null) {
                    if (lookup.getTransactionManager() != null) {
                        try {
                            Transaction transaction = lookup.getTransactionManager().getTransaction();
                            logger.debugv("bootstrap current transaction? {0}", transaction != null);
                            if (transaction != null) {
                                logger.debugv("bootstrap current transaction status? {0}", transaction.getStatus());
                            }
                        } catch (SystemException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                // TODO up here ^^

                ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
                var exportImportManager = bootstrapState.exportImportManager = new ExportImportManager(session);
                bootstrapState.newInstall = applianceBootstrap.isNewInstall();
                if (bootstrapState.newInstall) {
                    boolean existing = ExportImportConfig.isSingleTransaction();
                    ExportImportConfig.setSingleTransaction(true);
                    try {
                        if (!exportImportManager.isImportMasterIncluded()) {
                            applianceBootstrap.createMasterRealm();
                        }
                        // these are also running in the initial bootstrap transaction - if there is a problem, the server won't be initialized at all
                        exportImportManager.runImport();
                        createTemporaryAdmin(session);
                    } finally {
                        ExportImportConfig.setSingleTransaction(existing);
                    }
                }
            }
        });

        if (!bootstrapState.newInstall) {
            bootstrapState.exportImportManager.runImport();
        }

        return bootstrapState.exportImportManager;
    }

    protected abstract void createTemporaryAdmin(KeycloakSession session);

    protected abstract void loadConfig();

    protected abstract KeycloakSessionFactory createSessionFactory();

    public static KeycloakSessionFactory getSessionFactory() {
        return sessionFactory;
    }

}
