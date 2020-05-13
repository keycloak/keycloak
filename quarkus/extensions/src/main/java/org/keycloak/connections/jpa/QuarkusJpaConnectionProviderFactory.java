/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa;

import static org.keycloak.connections.liquibase.QuarkusJpaUpdaterProvider.VERIFY_AND_RUN_MASTER_CHANGELOG;

import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.ServerStartupError;
import org.keycloak.common.Version;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.transaction.JtaTransactionManagerLookup;

import javax.enterprise.inject.Instance;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class QuarkusJpaConnectionProviderFactory implements JpaConnectionProviderFactory, ServerInfoAwareProviderFactory {

    private static final Logger logger = Logger.getLogger(QuarkusJpaConnectionProviderFactory.class);

    private static final String SQL_GET_LATEST_VERSION = "SELECT VERSION FROM %sMIGRATION_MODEL";

    enum MigrationStrategy {
        UPDATE, VALIDATE, MANUAL
    }

    private EntityManagerFactory emf;

    private Config.Scope config;

    private Map<String, String> operationalInfo;

    private boolean jtaEnabled;
    private JtaTransactionManagerLookup jtaLookup;

    private KeycloakSessionFactory factory;

    @Override
    public JpaConnectionProvider create(KeycloakSession session) {
        logger.trace("Create QuarkusJpaConnectionProvider");
        EntityManager em;
        if (!jtaEnabled) {
            logger.trace("enlisting EntityManager in JpaKeycloakTransaction");
            em = emf.createEntityManager();
            try {
                SessionImpl.class.cast(em).connection().setAutoCommit(false);
            } catch (SQLException cause) {
                throw new RuntimeException(cause);
            }
        } else {

            em = emf.createEntityManager(SynchronizationType.SYNCHRONIZED);
        }
        em = PersistenceExceptionConverter.create(em);
        if (!jtaEnabled) session.getTransactionManager().enlist(new JpaKeycloakTransaction(em));
        return new DefaultJpaConnectionProvider(em);
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

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.factory = factory;
        checkJtaEnabled(factory);
        lazyInit();
    }

    protected void checkJtaEnabled(KeycloakSessionFactory factory) {
        jtaLookup = (JtaTransactionManagerLookup) factory.getProviderFactory(JtaTransactionManagerLookup.class);
        if (jtaLookup != null) {
            if (jtaLookup.getTransactionManager() != null) {
                jtaEnabled = true;
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

    protected void prepareOperationalInfo(Connection connection) {
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

    void migration(String schema, Connection connection, KeycloakSession session) {
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

        session.setAttribute(VERIFY_AND_RUN_MASTER_CHANGELOG, version == null || !version.equals(new ModelVersion(Version.VERSION_KEYCLOAK).toString()));

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
    }

    protected void update(Connection connection, String schema, KeycloakSession session, JpaUpdaterProvider updater) {
        DBLockManager dbLockManager = new DBLockManager(session);
        DBLockProvider dbLock2 = dbLockManager.getDBLock();
        dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
        try {
            updater.update(connection, schema);
        } finally {
            dbLock2.releaseLock();
        }
    }

    protected void export(Connection connection, String schema, File databaseUpdateFile, KeycloakSession session,
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

    @Override
    public Connection getConnection() {
        SessionFactoryImpl entityManagerFactory = SessionFactoryImpl.class.cast(emf);

        try {
            return entityManagerFactory.getJdbcServices().getBootstrapJdbcConnectionAccess().obtainConnection();
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to obtain JDBC connection", cause);
        }
    }

    @Override
    public String getSchema() {
        return config.get("schema");
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return operationalInfo;
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

    private void lazyInit() {
        Instance<EntityManagerFactory> instance = CDI.current().select(EntityManagerFactory.class);

        if (!instance.isResolvable()) {
            throw new RuntimeException("Failed to resolve " + EntityManagerFactory.class + " from Quarkus runtime");
        }

        emf = instance.get();

        try (Connection connection = getConnection()) {
            if (jtaEnabled) {
                KeycloakModelUtils.suspendJtaTransaction(factory, () -> {
                    KeycloakSession session = factory.create();
                    try {
                        migration(getSchema(), connection, session);
                    } finally {
                        session.close();
                    }
                });
            } else {
                KeycloakModelUtils.runJobInTransaction(factory, session -> {
                    migration(getSchema(), connection, session);
                });
            }
            prepareOperationalInfo(connection);
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to migrate model", cause);
        }
    }

    @Override
    public int order() {
        return 100;
    }
}
