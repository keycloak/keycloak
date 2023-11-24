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

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Resteasy;
import org.keycloak.config.ConfigProviderFactory;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.Strategy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.locking.GlobalLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.platform.Platform;
import org.keycloak.platform.PlatformProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.error.KeycloakErrorHandler;
import org.keycloak.services.error.KcUnrecognizedPropertyExceptionHandler;
import org.keycloak.services.error.KeycloakMismatchedInputExceptionHandler;
import org.keycloak.services.filters.KeycloakSecurityHeadersFilter;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.transaction.JtaTransactionManagerLookup;
import org.keycloak.util.JsonSerialization;

import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.ws.rs.core.Application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    private static final Logger logger = Logger.getLogger(KeycloakApplication.class);

    protected final PlatformProvider platform = Platform.getPlatform();

    protected Set<Object> singletons = new HashSet<>();
    protected Set<Class<?>> classes = new HashSet<>();

    private static KeycloakSessionFactory sessionFactory;

    public KeycloakApplication() {

        try {

            logger.debugv("PlatformProvider: {0}", platform.getClass().getName());
            logger.debugv("RestEasy provider: {0}", Resteasy.getProvider().getClass().getName());

            loadConfig();

            classes.add(RobotsResource.class);
            classes.add(RealmsResource.class);
            if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_API)) {
                classes.add(AdminRoot.class);
            }
            classes.add(ThemeResource.class);

            if (Profile.isFeatureEnabled(Profile.Feature.JS_ADAPTER)) {
                classes.add(JsResource.class);
            }

            classes.add(KeycloakSecurityHeadersFilter.class);
            classes.add(KeycloakErrorHandler.class);
            classes.add(KcUnrecognizedPropertyExceptionHandler.class);
            classes.add(KeycloakMismatchedInputExceptionHandler.class);

            singletons.add(new ObjectMapperResolver());
            classes.add(WelcomeResource.class);
            classes.add(LoadbalancerResource.class);

            platform.onStartup(this::startup);
            platform.onShutdown(this::shutdown);

        } catch (Throwable t) {
            platform.exit(t);
        }

    }

    protected void startup() {
        CryptoIntegration.init(KeycloakApplication.class.getClassLoader());
        KeycloakApplication.sessionFactory = createSessionFactory();

        ExportImportManager[] exportImportManager = new ExportImportManager[1];

        // Release all locks acquired by currently used GlobalLockProvider if keycloak.globalLock.forceUnlock is equal
        //   to true. This can be used to recover from a state where there are some stale locks that were not correctly
        //   unlocked
        if (Boolean.getBoolean("keycloak.globalLock.forceUnlock")) {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession session) {
                    GlobalLockProvider locks = session.getProvider(GlobalLockProvider.class);
                    locks.forceReleaseAllLocks();
                }
            });
        }

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession session) {
                GlobalLockProvider locks = session.getProvider(GlobalLockProvider.class);
                exportImportManager[0] = locks.withLock(GlobalLockProvider.Constants.KEYCLOAK_BOOT, innerSession -> bootstrap());
            }
        });

        if (exportImportManager[0].isRunExport()) {
            exportImportManager[0].runExport();
        }

        sessionFactory.publish(new PostMigrationEvent(sessionFactory));
    }

    protected void shutdown() {
        if (sessionFactory != null)
            sessionFactory.close();
    }

    // Bootstrap master realm, import realms and create admin user.
    protected ExportImportManager bootstrap() {
        ExportImportManager[] exportImportManager = new ExportImportManager[1];

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
                exportImportManager[0] = new ExportImportManager(session);

                boolean createMasterRealm = applianceBootstrap.isNewInstall();
                if (exportImportManager[0].isRunImport() && exportImportManager[0].isImportMasterIncluded()) {
                    createMasterRealm = false;
                }

                if (createMasterRealm) {
                    applianceBootstrap.createMasterRealm();
                }
            }
        });

        if (exportImportManager[0].isRunImport()) {
            exportImportManager[0].runImport();
        } else {
            importRealms(exportImportManager[0]);
        }

        importAddUser();

        return exportImportManager[0];
    }

    protected void loadConfig() {

        ServiceLoader<ConfigProviderFactory> loader = ServiceLoader.load(ConfigProviderFactory.class, KeycloakApplication.class.getClassLoader());

        try {
            ConfigProviderFactory factory = loader.iterator().next();
            logger.debugv("ConfigProvider: {0}", factory.getClass().getName());
            Config.init(factory.create().orElseThrow(() -> new RuntimeException("Failed to load Keycloak configuration")));
        } catch (NoSuchElementException e) {
            throw new RuntimeException("No valid ConfigProvider found");
        }

    }

    protected KeycloakSessionFactory createSessionFactory() {
        DefaultKeycloakSessionFactory factory = new DefaultKeycloakSessionFactory();
        factory.init();
        return factory;
    }

    public static KeycloakSessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    public void importRealms(ExportImportManager exportImportManager) {
        String dir = System.getProperty("keycloak.import");
        if (dir != null) {
            try {
                System.setProperty(ExportImportConfig.STRATEGY, Strategy.IGNORE_EXISTING.toString());
                exportImportManager.runImportAtStartup(dir);
            } catch (IOException cause) {
                throw new RuntimeException("Failed to import realms", cause);
            }
        }
    }

    public void importRealm(RealmRepresentation rep, String from) {
        boolean exists = false;
        try (KeycloakSession session = sessionFactory.create()) {
            session.getTransactionManager().begin();

            try {
                RealmManager manager = new RealmManager(session);

                if (rep.getId() != null && manager.getRealm(rep.getId()) != null) {
                    ServicesLogger.LOGGER.realmExists(rep.getRealm(), from);
                    exists = true;
                }

                if (manager.getRealmByName(rep.getRealm()) != null) {
                    ServicesLogger.LOGGER.realmExists(rep.getRealm(), from);
                    exists = true;
                }
                if (!exists) {
                    RealmModel realm = manager.importRealm(rep);
                    ServicesLogger.LOGGER.importedRealm(realm.getName(), from);
                }
            } catch (Throwable t) {
                session.getTransactionManager().setRollbackOnly();
                throw t;
            }
        } catch (Throwable t) {
            if (!exists) {
                ServicesLogger.LOGGER.unableToImportRealm(t, rep.getRealm(), from);
            }
        }
    }

    public void importAddUser() {
        String configDir = System.getProperty("jboss.server.config.dir");
        if (configDir != null) {
            File addUserFile = new File(configDir + File.separator + "keycloak-add-user.json");
            if (addUserFile.isFile()) {
                ServicesLogger.LOGGER.imprtingUsersFrom(addUserFile);

                List<RealmRepresentation> realms;
                try {
                    realms = JsonSerialization.readValue(new FileInputStream(addUserFile), new TypeReference<List<RealmRepresentation>>() {
                    });
                } catch (IOException e) {
                    ServicesLogger.LOGGER.failedToLoadUsers(e);
                    return;
                }

                for (RealmRepresentation realmRep : realms) {
                    for (UserRepresentation userRep : realmRep.getUsers()) {
                        try {
                            KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
                                RealmModel realm = session.realms().getRealmByName(realmRep.getRealm());

                                if (realm == null) {
                                    ServicesLogger.LOGGER.addUserFailedRealmNotFound(userRep.getUsername(), realmRep.getRealm());
                                }

                                UserProvider users = session.users();

                                if (users.getUserByUsername(realm, userRep.getUsername()) != null) {
                                    ServicesLogger.LOGGER.notCreatingExistingUser(userRep.getUsername());
                                } else {
                                    UserModel user = users.addUser(realm, userRep.getUsername());
                                    user.setEnabled(userRep.isEnabled());
                                    RepresentationToModel.createCredentials(userRep, session, realm, user, false);
                                    RepresentationToModel.createRoleMappings(userRep, user, realm);
                                    ServicesLogger.LOGGER.addUserSuccess(userRep.getUsername(), realmRep.getRealm());
                                }
                            });
                        } catch (ModelDuplicateException e) {
                            ServicesLogger.LOGGER.addUserFailedUserExists(userRep.getUsername(), realmRep.getRealm());
                        } catch (Throwable t) {
                            ServicesLogger.LOGGER.addUserFailed(t, userRep.getUsername(), realmRep.getRealm());
                        }
                    }
                }

                if (!addUserFile.delete()) {
                    ServicesLogger.LOGGER.failedToDeleteFile(addUserFile.getAbsolutePath());
                }
            }
        }
    }
}
