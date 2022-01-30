/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.database.jpa;

import static org.keycloak.connections.jpa.util.JpaUtils.configureNamedQuery;
import static org.keycloak.quarkus.runtime.storage.database.liquibase.QuarkusJpaUpdaterProvider.VERIFY_AND_RUN_MASTER_CHANGELOG;
import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.runtime.Quarkus;

import org.hibernate.internal.SessionFactoryImpl;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.ServerStartupError;
import org.keycloak.common.Version;
import org.keycloak.connections.jpa.DefaultJpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.connections.jpa.PersistenceExceptionConverter;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.migration.MigrationModel;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.transaction.JtaTransactionManagerLookup;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public final class QuarkusJpaConnectionProviderFactory implements JpaConnectionProviderFactory, ServerInfoAwareProviderFactory {

    public static final String QUERY_PROPERTY_PREFIX = "kc.query.";
    private static final Logger logger = Logger.getLogger(QuarkusJpaConnectionProviderFactory.class);
    private static final String SQL_GET_LATEST_VERSION = "SELECT VERSION FROM %sMIGRATION_MODEL ORDER BY UPDATE_TIME DESC";

    enum MigrationStrategy {
        UPDATE, VALIDATE, MANUAL
    }

    private EntityManagerFactory emf;
    private Config.Scope config;
    private Map<String, String> operationalInfo;
    private KeycloakSessionFactory factory;

    @Override
    public JpaConnectionProvider create(KeycloakSession session) {
        logger.trace("Create QuarkusJpaConnectionProvider");
        return new DefaultJpaConnectionProvider(createEntityManager(session));
    }

    @Override
    public void close() {
        if (emf != null) {
            emf.close();
        }
    }

    @Override
    public String getId() {
        return "quarkus";
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    private void addSpecificNamedQueries(KeycloakSession session, Connection connection) {
        EntityManager em = createEntityManager(session);

        try {
            Map<String, Object> unitProperties = emf.getProperties();

            unitProperties.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(QUERY_PROPERTY_PREFIX))
                    .forEach(entry -> configureNamedQuery(entry.getKey().substring(QUERY_PROPERTY_PREFIX.length()), entry.getValue().toString(), em));
        } finally {
            JpaUtils.closeEntityManager(em);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.factory = factory;
        Instance<EntityManagerFactory> instance = CDI.current().select(EntityManagerFactory.class);

        if (!instance.isResolvable()) {
            throw new RuntimeException("Failed to resolve " + EntityManagerFactory.class + " from Quarkus runtime");
        }

        emf = instance.get();

        KeycloakSession session = factory.create();
        boolean schemaChanged;

        try (Connection connection = getConnection()) {
            createOperationalInfo(connection);
            addSpecificNamedQueries(session, connection);
            schemaChanged = createOrUpdateSchema(getSchema(), connection, session);
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to update database.", cause);
        } finally {
            session.close();
        }

        if (schemaChanged || Environment.isImportExportMode()) {
            runJobInTransaction(factory, this::initSchema);
        } else {
            //KEYCLOAK-19521 - We should think about a solution which doesn't involve another db lookup in the future.
            MigrationModel model = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
            Version.RESOURCES_VERSION = model.getResourcesTag();
        }
    }

    @Override
    public Connection getConnection() {
        SessionFactoryImpl entityManagerFactory = emf.unwrap(SessionFactoryImpl.class);

        try {
            return entityManagerFactory.getJdbcServices().getBootstrapJdbcConnectionAccess().obtainConnection();
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to obtain JDBC connection", cause);
        }
    }

    @Override
    public String getSchema() {
        return Configuration.getRawValue("kc.db-schema");
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return operationalInfo;
    }

    @Override
    public int order() {
        return 100;
    }

    private MigrationStrategy getMigrationStrategy() {
        String migrationStrategy = config.get("migrationStrategy");
        if (migrationStrategy == null) {
            // Support 'databaseSchema' for backwards compatibility
            migrationStrategy = config.get("databaseSchema");
        }

        if (migrationStrategy != null) {
            return MigrationStrategy.valueOf(migrationStrategy.toUpperCase());
        } else {
            return MigrationStrategy.UPDATE;
        }
    }

    private void initSchema(KeycloakSession session) {
        ExportImportManager exportImportManager = new ExportImportManager(session);
        
        /*
         * Migrate model is executed just in case following providers are "jpa".
         * In Map Storage, there is an assumption that migrateModel is not needed.
         */
        if ((Config.getProvider("realm") == null || "jpa".equals(Config.getProvider("realm"))) &&
            (Config.getProvider("client") == null || "jpa".equals(Config.getProvider("client"))) &&
            (Config.getProvider("clientScope") == null || "jpa".equals(Config.getProvider("clientScope")))) {

            logger.debug("Calling migrateModel");
            migrateModel(session);
        }

        DBLockManager dbLockManager = new DBLockManager(session);
        dbLockManager.checkForcedUnlock();
        DBLockProvider dbLock = dbLockManager.getDBLock();
        dbLock.waitForLock(DBLockProvider.Namespace.KEYCLOAK_BOOT);
        try {
            createMasterRealm(exportImportManager);
        } finally {
            dbLock.releaseLock();
        }
        if (exportImportManager.isRunExport()) {
            exportImportManager.runExport();
            Quarkus.asyncExit();
        }
    }

    private ExportImportManager createMasterRealm(ExportImportManager exportImportManager) {
        logger.debug("bootstrap");
        KeycloakSession session = factory.create();

        try {
            session.getTransactionManager().begin();
            JtaTransactionManagerLookup lookup = (JtaTransactionManagerLookup) factory
                    .getProviderFactory(JtaTransactionManagerLookup.class);
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
            boolean createMasterRealm = applianceBootstrap.isNewInstall();

            if (exportImportManager.isRunImport() && exportImportManager.isImportMasterIncluded()) {
                createMasterRealm = false;
            }

            if (createMasterRealm) {
                applianceBootstrap.createMasterRealm();
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
            Quarkus.asyncExit();
        } else {
            importRealms();
        }

        importAddUser();

        return exportImportManager;
    }

    private void migrateModel(KeycloakSession session) {
        try {
            MigrationModelManager.migrate(session);
        } catch (Exception e) {
            throw e;
        }
    }

    private void importRealms() {
        String files = System.getProperty("keycloak.import");
        if (files != null) {
            StringTokenizer tokenizer = new StringTokenizer(files, ",");
            while (tokenizer.hasMoreTokens()) {
                String file = tokenizer.nextToken().trim();
                RealmRepresentation rep;
                try {
                    rep = JsonSerialization.readValue(new FileInputStream(file), RealmRepresentation.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                importRealm(rep, "file " + file);
            }
        }
    }

    private void importRealm(RealmRepresentation rep, String from) {
        KeycloakSession session = factory.create();
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

    private void importAddUser() {
        String configDir = System.getProperty("jboss.server.config.dir");
        if (configDir != null) {
            File addUserFile = new File(configDir + File.separator + "keycloak-add-user.json");
            if (addUserFile.isFile()) {
                ServicesLogger.LOGGER.imprtingUsersFrom(addUserFile);

                List<RealmRepresentation> realms;
                try {
                    realms = JsonSerialization
                            .readValue(new FileInputStream(addUserFile), new TypeReference<List<RealmRepresentation>>() {
                            });
                } catch (IOException e) {
                    ServicesLogger.LOGGER.failedToLoadUsers(e);
                    return;
                }

                for (RealmRepresentation realmRep : realms) {
                    for (UserRepresentation userRep : realmRep.getUsers()) {
                        KeycloakSession session = factory.create();

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

    private String getSchema(String schema) {
        return schema == null ? "" : schema + ".";
    }

    private File getDatabaseUpdateFile() {
        String databaseUpdateFile = config.get("migrationExport", "keycloak-database-update.sql");
        return new File(databaseUpdateFile);
    }

    private void createOperationalInfo(Connection connection) {
        try {
            operationalInfo = new LinkedHashMap<>();
            DatabaseMetaData md = connection.getMetaData();
            operationalInfo.put("databaseUrl", md.getURL());
            operationalInfo.put("databaseUser", md.getUserName());
            operationalInfo.put("databaseProduct", md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            operationalInfo.put("databaseDriver", md.getDriverName() + " " + md.getDriverVersion());
            logger.debugf("Database info: %s", operationalInfo.toString());
        } catch (SQLException e) {
            logger.warn("Unable to prepare operational info due database exception: " + e.getMessage());
        }
    }

    private boolean createOrUpdateSchema(String schema, Connection connection, KeycloakSession session) {
        MigrationStrategy strategy = getMigrationStrategy();
        boolean initializeEmpty = config.getBoolean("initializeEmpty", true);
        File databaseUpdateFile = getDatabaseUpdateFile();

        String version = null;

        try {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery(String.format(SQL_GET_LATEST_VERSION, getSchema(schema)))) {
                    if (rs.next()) {
                        version = rs.getString(1);
                    }
                }
            }
        } catch (SQLException ignore) {
            // migration model probably does not exist so we assume the database is empty
        }

        JpaUpdaterProvider updater = session.getProvider(JpaUpdaterProvider.class);

        boolean requiresMigration = version == null || !version.equals(new ModelVersion(Version.VERSION_KEYCLOAK).toString());
        session.setAttribute(VERIFY_AND_RUN_MASTER_CHANGELOG, requiresMigration);

        JpaUpdaterProvider.Status status = updater.validate(connection, schema);

        if (status == JpaUpdaterProvider.Status.VALID) {
            logger.debug("Database is up-to-date");
        } else if (status == JpaUpdaterProvider.Status.EMPTY) {
            if (initializeEmpty) {
                update(connection, schema, session, updater);
            } else {
                switch (strategy) {
                    case UPDATE:
                        update(connection, schema, session, updater);
                        break;
                    case MANUAL:
                        export(connection, schema, databaseUpdateFile, session, updater);
                        throw new ServerStartupError("Database not initialized, please initialize database with " + databaseUpdateFile.getAbsolutePath(), false);
                    case VALIDATE:
                        throw new ServerStartupError("Database not initialized, please enable database initialization", false);
                }
            }
        } else {
            switch (strategy) {
                case UPDATE:
                    update(connection, schema, session, updater);
                    break;
                case MANUAL:
                    export(connection, schema, databaseUpdateFile, session, updater);
                    throw new ServerStartupError("Database not up-to-date, please migrate database with " + databaseUpdateFile.getAbsolutePath(), false);
                case VALIDATE:
                    throw new ServerStartupError("Database not up-to-date, please enable database migration", false);
            }
        }

        return requiresMigration;
    }

    private void update(Connection connection, String schema, KeycloakSession session, JpaUpdaterProvider updater) {
        DBLockManager dbLockManager = new DBLockManager(session);
        DBLockProvider dbLock2 = dbLockManager.getDBLock();
        dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
        try {
            updater.update(connection, schema);
        } finally {
            dbLock2.releaseLock();
        }
    }

    private void export(Connection connection, String schema, File databaseUpdateFile, KeycloakSession session,
            JpaUpdaterProvider updater) {
        DBLockManager dbLockManager = new DBLockManager(session);
        DBLockProvider dbLock2 = dbLockManager.getDBLock();
        dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
        try {
            updater.export(connection, schema, databaseUpdateFile);
        } finally {
            dbLock2.releaseLock();
        }
    }

    private EntityManager createEntityManager(KeycloakSession session) {
        // we need to auto join the transaction, hence the synchronized type
        // ideally, we should leverage how hibernate-orm creates the entity manager
        // but that breaks us, mainly due to flush which is always set to always
        // as per hibernate guys, we should consider how JTASessionOpener creates entity managers
        // but that brings lot of details that we need to investigate further
        return PersistenceExceptionConverter.create(session, emf.createEntityManager(SynchronizationType.SYNCHRONIZED));
    }
}
