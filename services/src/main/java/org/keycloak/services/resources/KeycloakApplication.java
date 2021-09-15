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
import org.keycloak.common.util.Resteasy;
import org.keycloak.config.ConfigProviderFactory;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
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
import org.keycloak.services.filters.KeycloakSecurityHeadersFilter;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.scheduled.ClearExpiredClientInitialAccessTokens;
import org.keycloak.services.scheduled.ClearExpiredEvents;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.services.scheduled.StartIdPScheduledTasks;
import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.timer.TimerProvider;
import org.keycloak.transaction.JtaTransactionManagerLookup;
import org.keycloak.util.JsonSerialization;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.ws.rs.core.Application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    public static final AtomicBoolean BOOTSTRAP_ADMIN_USER = new AtomicBoolean(false);

    private static final Logger logger = Logger.getLogger(KeycloakApplication.class);

    protected final PlatformProvider platform = Platform.getPlatform();

    protected Set<Object> singletons = new HashSet<>();
    protected Set<Class<?>> classes = new HashSet<>();

    protected static KeycloakSessionFactory sessionFactory;

    public KeycloakApplication() {

        try {

            logger.debugv("PlatformProvider: {0}", platform.getClass().getName());
            logger.debugv("RestEasy provider: {0}", Resteasy.getProvider().getClass().getName());

            loadConfig();

            singletons.add(new RobotsResource());
            singletons.add(new RealmsResource());
            singletons.add(new AdminRoot());
            classes.add(ThemeResource.class);
            classes.add(JsResource.class);

            classes.add(KeycloakSecurityHeadersFilter.class);
            classes.add(KeycloakErrorHandler.class);
            classes.add(KcUnrecognizedPropertyExceptionHandler.class);

            singletons.add(new ObjectMapperResolver());
            singletons.add(new WelcomeResource());

            platform.onStartup(this::startup);
            platform.onShutdown(this::shutdown);

        } catch (Throwable t) {
            platform.exit(t);
        }

    }

    protected void startup() {
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

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                boolean shouldBootstrapAdmin = new ApplianceBootstrap(session).isNoMasterUser();
                BOOTSTRAP_ADMIN_USER.set(shouldBootstrapAdmin);
            }

        });

        sessionFactory.publish(new PostMigrationEvent());

        setupScheduledTasks(sessionFactory);
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
            importRealms();
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

    public static KeycloakSessionFactory createSessionFactory() {
        DefaultKeycloakSessionFactory factory = new DefaultKeycloakSessionFactory();
        factory.init();
        return factory;
    }

    public static void setupScheduledTasks(final KeycloakSessionFactory sessionFactory) {
        long interval = Config.scope("scheduled").getLong("interval", 900L) * 1000;

        KeycloakSession session = sessionFactory.create();
        try {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredEvents(), interval), interval, "ClearExpiredEvents");
            timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredClientInitialAccessTokens(), interval), interval, "ClearExpiredClientInitialAccessTokens");
            timer.schedule(new ScheduledTaskRunner(sessionFactory, new ClearExpiredUserSessions()), interval, ClearExpiredUserSessions.TASK_NAME);
            new UserStorageSyncManager().bootstrapPeriodic(sessionFactory, timer);
            ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor("idp-scheduled tasks");
            StartIdPScheduledTasks idPScheduledTasks = new StartIdPScheduledTasks();
            ScheduledTaskRunner task = new ScheduledTaskRunner(sessionFactory, idPScheduledTasks);
            executor.submit(task);
        } finally {
            session.close();
        }
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

    public void importRealms() {
        String files = System.getProperty("keycloak.import");
        if (files != null) {
            StringTokenizer tokenizer = new StringTokenizer(files, ",");
            while (tokenizer.hasMoreTokens()) {
                String file = tokenizer.nextToken().trim();
                RealmRepresentation rep;
                try {
                    rep = loadJson(new FileInputStream(file), RealmRepresentation.class);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                importRealm(rep, "file " + file);
            }
        }
    }

    public void importRealm(RealmRepresentation rep, String from) {
        KeycloakSession session = sessionFactory.create();
        boolean exists = false;
        try {
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
                session.getTransactionManager().commit();
            } catch (Throwable t) {
                session.getTransactionManager().rollback();
                if (!exists) {
                    ServicesLogger.LOGGER.unableToImportRealm(t, rep.getRealm(), from);
                }
            }
        } finally {
            session.close();
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
                        KeycloakSession session = sessionFactory.create();

                        try {
                            session.getTransactionManager().begin();
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

                            session.getTransactionManager().commit();
                        } catch (ModelDuplicateException e) {
                            session.getTransactionManager().rollback();
                            ServicesLogger.LOGGER.addUserFailedUserExists(userRep.getUsername(), realmRep.getRealm());
                        } catch (Throwable t) {
                            session.getTransactionManager().rollback();
                            ServicesLogger.LOGGER.addUserFailed(t, userRep.getUsername(), realmRep.getRealm());
                        } finally {
                            session.close();
                        }
                    }
                }

                if (!addUserFile.delete()) {
                    ServicesLogger.LOGGER.failedToDeleteFile(addUserFile.getAbsolutePath());
                }
            }
        }
    }

    private static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

}
