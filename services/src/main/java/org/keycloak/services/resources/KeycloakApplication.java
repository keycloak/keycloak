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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.models.*;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.services.managers.DBLockManager;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.filters.KeycloakTransactionCommitter;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.scheduled.ClearExpiredEvents;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.util.JsonConfigProvider;
import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.timer.TimerProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.common.util.SystemEnvProperties;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected KeycloakSessionFactory sessionFactory;
    protected String contextPath;

    public KeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        loadConfig();

        this.contextPath = context.getContextPath();
        this.sessionFactory = createSessionFactory();

        dispatcher.getDefaultContextObjects().put(KeycloakApplication.class, this);
        ResteasyProviderFactory.pushContext(KeycloakApplication.class, this); // for injection
        context.setAttribute(KeycloakSessionFactory.class.getName(), this.sessionFactory);

        singletons.add(new ServerVersionResource());
        singletons.add(new RobotsResource());
        singletons.add(new RealmsResource());
        singletons.add(new AdminRoot());
        classes.add(ThemeResource.class);
        classes.add(JsResource.class);

        classes.add(KeycloakTransactionCommitter.class);

        singletons.add(new ObjectMapperResolver(Boolean.parseBoolean(System.getProperty("keycloak.jsonPrettyPrint", "false"))));

        ExportImportManager exportImportManager;

        DBLockManager dbLockManager = new DBLockManager(sessionFactory.create());
        dbLockManager.checkForcedUnlock();
        DBLockProvider dbLock = dbLockManager.getDBLock();
        dbLock.waitForLock();
        try {
            migrateModel();

            KeycloakSession session = sessionFactory.create();
            try {
                session.getTransaction().begin();

                ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
                exportImportManager = new ExportImportManager(session);

                boolean createMasterRealm = applianceBootstrap.isNewInstall();
                if (exportImportManager.isRunImport() && exportImportManager.isImportMasterIncluded()) {
                    createMasterRealm = false;
                }

                if (createMasterRealm) {
                    applianceBootstrap.createMasterRealm(contextPath);
                }
                session.getTransaction().commit();
            } catch (RuntimeException re) {
                if (session.getTransaction().isActive()) {
                    session.getTransaction().rollback();
                }
                throw re;
            } finally {
                session.close();
            }

            if (exportImportManager.isRunImport()) {
                exportImportManager.runImport();
            } else {
                importRealms();
            }

            importAddUser();
        } finally {
            dbLock.releaseLock();
        }

        if (exportImportManager.isRunExport()) {
            exportImportManager.runExport();
        }

        boolean bootstrapAdminUser = false;
        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();
            bootstrapAdminUser = new ApplianceBootstrap(session).isNoMasterUser();

            session.getTransaction().commit();
        } finally {
            session.close();
        }

        sessionFactory.publish(new PostMigrationEvent());

        singletons.add(new WelcomeResource(bootstrapAdminUser));

        setupScheduledTasks(sessionFactory);
    }

    protected void migrateModel() {
        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();
            MigrationModelManager.migrate(session);
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            logger.migrationFailure(e);
            throw e;
        } finally {
            session.close();
        }
    }

    public String getContextPath() {
        return contextPath;
    }

    /**
     * Get base URI of WAR distribution, not JAX-RS
     *
     * @param uriInfo
     * @return
     */
    public URI getBaseUri(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().replacePath(getContextPath()).build();
    }

    public static void loadConfig() {
        try {
            JsonNode node = null;

            String configDir = System.getProperty("jboss.server.config.dir");
            if (configDir != null) {
                File f = new File(configDir + File.separator + "keycloak-server.json");
                if (f.isFile()) {
                    logger.loadingFrom(f.getAbsolutePath());
                    node = new ObjectMapper().readTree(f);
                }
            }

            if (node == null) {
                URL resource = Thread.currentThread().getContextClassLoader().getResource("META-INF/keycloak-server.json");
                if (resource != null) {
                    logger.loadingFrom(resource);
                    node = new ObjectMapper().readTree(resource);
                }
            }

            if (node != null) {
                Properties properties = new SystemEnvProperties();
                Config.init(new JsonConfigProvider(node, properties));
                return;
            } else {
                throw new RuntimeException("Config 'keycloak-server.json' not found");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public static KeycloakSessionFactory createSessionFactory() {
        DefaultKeycloakSessionFactory factory = new DefaultKeycloakSessionFactory();
        factory.init();
        return factory;
    }

    public static void setupScheduledTasks(final KeycloakSessionFactory sessionFactory) {
        long interval = Config.scope("scheduled").getLong("interval", 60L) * 1000;

        KeycloakSession session = sessionFactory.create();
        try {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredEvents(), interval), interval, "ClearExpiredEvents");
            timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredUserSessions(), interval), interval, "ClearExpiredUserSessions");
            new UsersSyncManager().bootstrapPeriodic(sessionFactory, timer);
        } finally {
            session.close();
        }
    }

    public KeycloakSessionFactory getSessionFactory() {
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
            session.getTransaction().begin();

            try {
                RealmManager manager = new RealmManager(session);
                manager.setContextPath(getContextPath());

                if (rep.getId() != null && manager.getRealm(rep.getId()) != null) {
                    logger.realmExists(rep.getRealm(), from);
                    exists = true;
                }

                if (manager.getRealmByName(rep.getRealm()) != null) {
                    logger.realmExists(rep.getRealm(), from);
                    exists = true;
                }
                if (!exists) {
                    RealmModel realm = manager.importRealm(rep);
                    logger.importedRealm(realm.getName(), from);
                }
                session.getTransaction().commit();
            } catch (Throwable t) {
                session.getTransaction().rollback();
                if (!exists) {
                    logger.unableToImportRealm(t, rep.getRealm(), from);
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
                logger.imprtingUsersFrom(addUserFile);

                List<RealmRepresentation> realms;
                try {
                    realms = JsonSerialization.readValue(new FileInputStream(addUserFile), new TypeReference<List<RealmRepresentation>>() {
                    });
                } catch (IOException e) {
                    logger.failedToLoadUsers(e);
                    return;
                }

                for (RealmRepresentation realmRep : realms) {
                    for (UserRepresentation userRep : realmRep.getUsers()) {
                        KeycloakSession session = sessionFactory.create();
                        try {
                            session.getTransaction().begin();

                            RealmModel realm = session.realms().getRealmByName(realmRep.getRealm());
                            if (realm == null) {
                                logger.addUserFailedRealmNotFound(userRep.getUsername(), realmRep.getRealm());
                            } else {
                                UserModel user = session.users().addUser(realm, userRep.getUsername());
                                user.setEnabled(userRep.isEnabled());
                                RepresentationToModel.createCredentials(userRep, user);
                                RepresentationToModel.createRoleMappings(userRep, user, realm);
                            }

                            session.getTransaction().commit();
                            logger.addUserSuccess(userRep.getUsername(), realmRep.getRealm());
                        } catch (ModelDuplicateException e) {
                            session.getTransaction().rollback();
                            logger.addUserFailedUserExists(userRep.getUsername(), realmRep.getRealm());
                        } catch (Throwable t) {
                            session.getTransaction().rollback();
                            logger.addUserFailed(t, userRep.getUsername(), realmRep.getRealm());
                        } finally {
                            session.close();
                        }
                    }
                }

                if (!addUserFile.delete()) {
                    logger.failedToDeleteFile(addUserFile.getAbsolutePath());
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
