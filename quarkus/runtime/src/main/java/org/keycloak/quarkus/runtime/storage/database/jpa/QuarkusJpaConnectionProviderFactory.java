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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.configuration.DurationConverter;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import io.quarkus.arc.Arc;

import org.jboss.logging.Logger;
import org.keycloak.ServerStartupError;
import org.keycloak.common.Version;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.database.Database;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.Configuration;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class QuarkusJpaConnectionProviderFactory extends AbstractJpaConnectionProviderFactory implements ServerInfoAwareProviderFactory {

    public static final String QUERY_PROPERTY_PREFIX = "kc.query.";
    public static final String DEFAULT_PERSISTENCE_UNIT = "keycloak-default";
    private static final Logger logger = Logger.getLogger(QuarkusJpaConnectionProviderFactory.class);
    private static final String SQL_GET_LATEST_VERSION = "SELECT ID, VERSION FROM %sMIGRATION_MODEL ORDER BY UPDATE_TIME DESC";

    enum MigrationStrategy {
        UPDATE, VALIDATE, MANUAL
    }

    private Map<String, String> operationalInfo;

    @Override
    public String getId() {
        return "quarkus";
    }

    private void addSpecificNamedQueries(KeycloakSession session) {
        EntityManager em = createEntityManager(entityManagerFactory, session, false);

        try {
            Map<String, Object> unitProperties = entityManagerFactory.getProperties();

            for (Map.Entry<String, Object> entry : unitProperties.entrySet()) {
                if (entry.getKey().startsWith(QUERY_PROPERTY_PREFIX)) {
                    configureNamedQuery(entry.getKey().substring(QUERY_PROPERTY_PREFIX.length()), entry.getValue().toString(), em);
                }
            }
        } finally {
            JpaUtils.closeEntityManager(em);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        super.postInit(factory);

        checkMySQLWaitTimeout();

        String id = null;
        String version = null;
        String schema = getSchema();
        boolean schemaChanged;

        try (Connection connection = getConnection(); KeycloakSession session = factory.create()) {
            try {
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet rs = statement.executeQuery(String.format(SQL_GET_LATEST_VERSION, getSchema(schema)))) {
                        if (rs.next()) {
                            id = rs.getString(1);
                            version = rs.getString(2);
                        }
                    }
                }
            } catch (SQLException ignore) {
                // migration model probably does not exist so we assume the database is empty
            }
            createOperationalInfo(connection);
            addSpecificNamedQueries(session);
            schemaChanged = createOrUpdateSchema(schema, version, connection, session);
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to update database.", cause);
        }

        if (schemaChanged || Environment.isNonServerMode()) {
            runJobInTransaction(factory, this::initSchema);
        } else {
            Version.RESOURCES_VERSION = id;
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("initializeEmpty")
                .type("boolean")
                .helpText("Initialize database if empty. If set to false the database has to be manually initialized. If you want to manually initialize the database set migrationStrategy to manual which will create a file with SQL commands to initialize the database.")
                .defaultValue(true)
                .add()
                .property()
                .name("migrationStrategy")
                .type("string")
                .helpText("Strategy to use to migrate database. Valid values are update, manual and validate. Update will automatically migrate the database schema. Manual will export the required changes to a file with SQL commands that you can manually execute on the database. Validate will simply check if the database is up-to-date.")
                .options("update", "manual", "validate")
                .defaultValue("update")
                .add()
                .property()
                .name("migrationExport")
                .type("string")
                .helpText("Path for where to write manual database initialization/migration file.")
                .add()
                .build();
    }

    @Override
    protected EntityManagerFactory getEntityManagerFactory() {
        Instance<EntityManagerFactory> instance = Arc.container().select(EntityManagerFactory.class);

        if (instance.isResolvable()) {
            return instance.get();
        }

        return getEntityManagerFactory(DEFAULT_PERSISTENCE_UNIT).orElseThrow(() -> new IllegalStateException("Failed to resolve the default entity manager factory"));
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
        logger.debug("Calling migrateModel");
        migrateModel(session);
    }

    private void migrateModel(KeycloakSession session) {
        // Using a lock to prevent concurrent migration in concurrently starting nodes
        DBLockManager dbLockManager = new DBLockManager(session);
        DBLockProvider dbLock = dbLockManager.getDBLock();
        dbLock.waitForLock(DBLockProvider.Namespace.DATABASE);
        try {
            MigrationModelManager.migrate(session);
        } finally {
            dbLock.releaseLock();
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

    private boolean createOrUpdateSchema(String schema, String version, Connection connection, KeycloakSession session) {
        MigrationStrategy strategy = getMigrationStrategy();
        boolean initializeEmpty = config.getBoolean("initializeEmpty", true);
        File databaseUpdateFile = getDatabaseUpdateFile();

        JpaUpdaterProvider updater = session.getProvider(JpaUpdaterProvider.class);

        boolean requiresMigration = version == null || !version.equals(new ModelVersion(Version.VERSION).toString());
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

    private void checkMySQLWaitTimeout() {
        String db = Configuration.getConfigValue(DatabaseOptions.DB).getValue();
        Database.Vendor vendor = Database.getVendor(db).orElseThrow();
        if (!(Database.Vendor.MYSQL == vendor || Database.Vendor.MARIADB == vendor))
            return;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW VARIABLES LIKE 'wait_timeout'")) {
            if (rs.next()) {
                var waitTimeout = rs.getInt(2);
                var poolMaxLifetime = DurationConverter.parseDuration(Configuration.getConfigValue(DatabaseOptions.DB_POOL_MAX_LIFETIME).getValue());
                if (poolMaxLifetime.getSeconds() >= waitTimeout) {
                    logger.warnf("%1$s 'wait_timeout=%2$d' is less than or equal to the configured '%3$s' duration. " +
                                "This can cause 'No operations allowed after connection closed' exceptions, which can impact Keycloak operations. " +
                                "To avoid such issues, set '%3$s' to a duration smaller than '%2$d' seconds.",
                          vendor, waitTimeout, DatabaseOptions.DB_POOL_MAX_LIFETIME.getKey(), poolMaxLifetime);
                }
            }
        } catch (SQLException e) {
            logger.warnf(e, "Unable to validate %s 'wait_timeout' due to database exception", vendor);
        }
    }
}
