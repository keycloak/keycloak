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
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.filters.KeycloakTransactionCommitter;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.scheduled.ClearExpiredEvents;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.services.util.JsonConfigProvider;
import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.timer.TimerProvider;
import org.keycloak.transaction.JtaTransactionManagerLookup;
import org.keycloak.util.JsonSerialization;

import javax.servlet.ServletContext;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {
    // This param name is defined again in Keycloak Server Subsystem class
    // org.keycloak.subsystem.server.extension.KeycloakServerDeploymentProcessor.  We have this value in
    // two places to avoid dependency between Keycloak Subsystem and Keycloak Services module.
    public static final String KEYCLOAK_CONFIG_PARAM_NAME = "org.keycloak.server-subsystem.Config";

    public static final String KEYCLOAK_EMBEDDED = "keycloak.embedded";

    public static final String SERVER_CONTEXT_CONFIG_PROPERTY_OVERRIDES = "keycloak.server.context.config.property-overrides";

    private static final Logger logger = Logger.getLogger(KeycloakApplication.class);

    protected boolean embedded = false;

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected KeycloakSessionFactory sessionFactory;
    protected String contextPath;

    public KeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        try {
            if ("true".equals(context.getInitParameter(KEYCLOAK_EMBEDDED))) {
                embedded = true;
            }

            loadConfig(context);

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

            ExportImportManager[] exportImportManager = new ExportImportManager[1];

            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                @Override
                public void run(KeycloakSession lockSession) {
                    DBLockManager dbLockManager = new DBLockManager(lockSession);
                    dbLockManager.checkForcedUnlock();
                    DBLockProvider dbLock = dbLockManager.getDBLock();
                    dbLock.waitForLock();
                    try {
                        exportImportManager[0] = migrateAndBootstrap();
                    } finally {
                        dbLock.releaseLock();
                    }
                }

            });


            if (exportImportManager[0].isRunExport()) {
                exportImportManager[0].runExport();
            }

            boolean bootstrapAdminUser = false;
            KeycloakSession session = sessionFactory.create();
            try {
                session.getTransactionManager().begin();
                bootstrapAdminUser = new ApplianceBootstrap(session).isNoMasterUser();

                session.getTransactionManager().commit();
            } finally {
                session.close();
            }

            sessionFactory.publish(new PostMigrationEvent());

            singletons.add(new WelcomeResource(bootstrapAdminUser));

            setupScheduledTasks(sessionFactory);
        } catch (Throwable t) {
            if (!embedded) {
                exit(1);
            }
            throw t;
        }
    }

    // Migrate model, bootstrap master realm, import realms and create admin user. This is done with acquired dbLock
    protected ExportImportManager migrateAndBootstrap() {
        ExportImportManager exportImportManager;
        logger.debug("Calling migrateModel");
        migrateModel();

        logger.debug("bootstrap");
        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransactionManager().begin();
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


            ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
            exportImportManager = new ExportImportManager(session);

            boolean createMasterRealm = applianceBootstrap.isNewInstall();
            if (exportImportManager.isRunImport() && exportImportManager.isImportMasterIncluded()) {
                createMasterRealm = false;
            }

            if (createMasterRealm) {
                applianceBootstrap.createMasterRealm(contextPath);
            }
            session.getTransactionManager().commit();
        } catch (RuntimeException re) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().rollback();
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

        return exportImportManager;
    }


    protected void migrateModel() {
        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransactionManager().begin();
            MigrationModelManager.migrate(session);
            session.getTransactionManager().commit();
        } catch (Exception e) {
            session.getTransactionManager().rollback();
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

    public static void loadConfig(ServletContext context) {
        try {
            JsonNode node = null;

            String dmrConfig = loadDmrConfig(context);
            if (dmrConfig != null) {
                node = new ObjectMapper().readTree(dmrConfig);
                ServicesLogger.LOGGER.loadingFrom("standalone.xml or domain.xml");
            }

            String configDir = System.getProperty("jboss.server.config.dir");
            if (node == null && configDir != null) {
                File f = new File(configDir + File.separator + "keycloak-server.json");
                if (f.isFile()) {
                    ServicesLogger.LOGGER.loadingFrom(f.getAbsolutePath());
                    node = new ObjectMapper().readTree(f);
                }
            }

            if (node == null) {
                URL resource = Thread.currentThread().getContextClassLoader().getResource("META-INF/keycloak-server.json");
                if (resource != null) {
                    ServicesLogger.LOGGER.loadingFrom(resource);
                    node = new ObjectMapper().readTree(resource);
                }
            }

            if (node != null) {
                Map<String, String> propertyOverridesMap = new HashMap<>();
                String propertyOverrides = context.getInitParameter(SERVER_CONTEXT_CONFIG_PROPERTY_OVERRIDES);
                if (context.getInitParameter(SERVER_CONTEXT_CONFIG_PROPERTY_OVERRIDES) != null) {
                    JsonNode jsonObj = new ObjectMapper().readTree(propertyOverrides);
                    jsonObj.fields().forEachRemaining(e -> propertyOverridesMap.put(e.getKey(), e.getValue().asText()));
                }
                Properties properties = new SystemEnvProperties(propertyOverridesMap);
                Config.init(new JsonConfigProvider(node, properties));
            } else {
                throw new RuntimeException("Keycloak config not found.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }
    
    private static String loadDmrConfig(ServletContext context) {
        String dmrConfig = context.getInitParameter(KEYCLOAK_CONFIG_PARAM_NAME);
        if (dmrConfig == null) return null;

        ModelNode dmrConfigNode = ModelNode.fromString(dmrConfig);
        if (dmrConfigNode.asPropertyList().isEmpty()) return null;
        
        // note that we need to resolve expressions BEFORE we convert to JSON
        return dmrConfigNode.resolve().toJSONString(true);
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
            timer.schedule(new ScheduledTaskRunner(sessionFactory, new ClearExpiredUserSessions()), interval, "ClearExpiredUserSessions");
            new UserStorageSyncManager().bootstrapPeriodic(sessionFactory, timer);
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
            session.getTransactionManager().begin();

            try {
                RealmManager manager = new RealmManager(session);
                manager.setContextPath(getContextPath());

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
                            } else {
                                UserModel user = session.users().addUser(realm, userRep.getUsername());
                                user.setEnabled(userRep.isEnabled());
                                RepresentationToModel.createCredentials(userRep, session, realm, user);
                                RepresentationToModel.createRoleMappings(userRep, user, realm);
                            }

                            session.getTransactionManager().commit();
                            ServicesLogger.LOGGER.addUserSuccess(userRep.getUsername(), realmRep.getRealm());
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

    private void exit(int status) {
        new Thread() {
            @Override
            public void run() {
                System.exit(status);
            }
        }.start();
    }

}
