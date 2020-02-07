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

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.ServerStartupError;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.timer.TimerProvider;
import org.keycloak.transaction.JtaTransactionManagerLookup;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class QuarkusJpaConnectionProviderFactory implements JpaConnectionProviderFactory, ServerInfoAwareProviderFactory {

    private static final Logger logger = Logger.getLogger(QuarkusJpaConnectionProviderFactory.class);

    enum MigrationStrategy {
        UPDATE, VALIDATE, MANUAL
    }

    private volatile EntityManagerFactory emf;

    private Config.Scope config;

    private Map<String, String> operationalInfo;

    private boolean jtaEnabled;
    private JtaTransactionManagerLookup jtaLookup;

    private KeycloakSessionFactory factory;

    @Override
    public JpaConnectionProvider create(KeycloakSession session) {
        logger.trace("Create QuarkusJpaConnectionProvider");
        lazyInit(session);

        EntityManager em;
        if (!jtaEnabled) {
            logger.trace("enlisting EntityManager in JpaKeycloakTransaction");
            em = emf.createEntityManager();
        } else {

            em = emf.createEntityManager(SynchronizationType.SYNCHRONIZED);
        }
        em = PersistenceExceptionConverter.create(em);
        if (!jtaEnabled) session.getTransactionManager().enlist(new JpaKeycloakTransaction(em));
        return new QuarkusJpaConnectionProvider(em);
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

    }

    protected void checkJtaEnabled(KeycloakSessionFactory factory) {
        jtaLookup = (JtaTransactionManagerLookup) factory.getProviderFactory(JtaTransactionManagerLookup.class);
        if (jtaLookup != null) {
            if (jtaLookup.getTransactionManager() != null) {
                jtaEnabled = true;
            }
        }
    }

    private void lazyInit(KeycloakSession session) {
        if (emf == null) {
            synchronized (this) {
                if (emf == null) {
                    KeycloakModelUtils.suspendJtaTransaction(session.getKeycloakSessionFactory(), () -> {
                        logger.debug("Initializing Quarkus JPA connections");

                        Map<String, Object> properties = new HashMap<>();

                        String unitName = "keycloak-default";

                        String schema = getSchema();
                        if (schema != null) {
                            properties.put(JpaUtils.HIBERNATE_DEFAULT_SCHEMA, schema);
                        }

                        MigrationStrategy migrationStrategy = getMigrationStrategy();
                        boolean initializeEmpty = config.getBoolean("initializeEmpty", true);
                        File databaseUpdateFile = getDatabaseUpdateFile();

                        properties.put("hibernate.show_sql", config.getBoolean("showSql", false));
                        properties.put("hibernate.format_sql", config.getBoolean("formatSql", true));
                        properties.put(AvailableSettings.CONNECTION_HANDLING,
                                PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT);

                        properties.put(AvailableSettings.DATASOURCE, getDataSource());

                        Connection connection = getConnection();
                        try {
                            prepareOperationalInfo(connection);

                            String driverDialect = detectDialect(connection);
                            if (driverDialect != null) {
                                properties.put("hibernate.dialect", driverDialect);
                            }

                            migration(migrationStrategy, initializeEmpty, schema, databaseUpdateFile, connection, session);

                            int globalStatsInterval = config.getInt("globalStatsInterval", -1);
                            if (globalStatsInterval != -1) {
                                properties.put("hibernate.generate_statistics", true);
                            }

                            logger.trace("Creating EntityManagerFactory");
                            logger.tracev("***** create EMF jtaEnabled {0} ", jtaEnabled);

                            Collection<ClassLoader> classLoaders = new ArrayList<>();
                            if (properties.containsKey(AvailableSettings.CLASSLOADERS)) {
                                classLoaders.addAll((Collection<ClassLoader>) properties.get(AvailableSettings.CLASSLOADERS));
                            }
                            classLoaders.add(getClass().getClassLoader());
                            properties.put(AvailableSettings.CLASSLOADERS, classLoaders);
                            emf = JpaUtils.createEntityManagerFactory(session, unitName, properties, jtaEnabled);
                            logger.trace("EntityManagerFactory created");

                            if (globalStatsInterval != -1) {
                                startGlobalStats(session, globalStatsInterval);
                            }
                        } finally {
                            // Close after creating EntityManagerFactory to prevent in-mem databases from closing
                            if (connection != null) {
                                try {
                                    connection.close();
                                } catch (SQLException e) {
                                    logger.warn("Can't close connection", e);
                                }
                            }
                        }
                    });
                }
            }
        }
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


    protected String detectDialect(Connection connection) {
        String driverDialect = config.get("driverDialect");
        if (driverDialect != null && driverDialect.length() > 0) {
            return driverDialect;
        } else {
            try {
                String dbProductName = connection.getMetaData().getDatabaseProductName();
                String dbProductVersion = connection.getMetaData().getDatabaseProductVersion();

                // For MSSQL2014, we may need to fix the autodetected dialect by hibernate
                if (dbProductName.equals("Microsoft SQL Server")) {
                    String topVersionStr = dbProductVersion.split("\\.")[0];
                    boolean shouldSet2012Dialect = true;
                    try {
                        int topVersion = Integer.parseInt(topVersionStr);
                        if (topVersion < 12) {
                            shouldSet2012Dialect = false;
                        }
                    } catch (NumberFormatException nfe) {
                    }
                    if (shouldSet2012Dialect) {
                        String sql2012Dialect = "org.hibernate.dialect.SQLServer2012Dialect";
                        logger.debugf("Manually override hibernate dialect to %s", sql2012Dialect);
                        return sql2012Dialect;
                    }
                }
                // For Oracle19c, we may need to set dialect explicitly to workaround https://hibernate.atlassian.net/browse/HHH-13184
                if (dbProductName.equals("Oracle") && connection.getMetaData().getDatabaseMajorVersion() > 12) {
                    logger.debugf("Manually specify dialect for Oracle to org.hibernate.dialect.Oracle12cDialect");
                    return "org.hibernate.dialect.Oracle12cDialect";
                }
            } catch (SQLException e) {
                logger.warnf("Unable to detect hibernate dialect due database exception : %s", e.getMessage());
            }

            return null;
        }
    }

    protected void startGlobalStats(KeycloakSession session, int globalStatsIntervalSecs) {
        logger.debugf("Started Hibernate statistics with the interval %s seconds", globalStatsIntervalSecs);
        TimerProvider timer = session.getProvider(TimerProvider.class);
        timer.scheduleTask(new HibernateStatsReporter(emf), globalStatsIntervalSecs * 1000, "ReportHibernateGlobalStats");
    }

    void migration(MigrationStrategy strategy, boolean initializeEmpty, String schema, File databaseUpdateFile, Connection connection, KeycloakSession session) {
        JpaUpdaterProvider updater = session.getProvider(JpaUpdaterProvider.class);

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
        runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession lockSession) -> {
            DBLockManager dbLockManager = new DBLockManager(lockSession);
            DBLockProvider dbLock2 = dbLockManager.getDBLock();
            dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
            try {
                updater.update(connection, schema);
            } finally {
                dbLock2.releaseLock();
            }
        }, KeycloakTransactionManager.JTAPolicy.NOT_SUPPORTED);
    }

    protected void export(Connection connection, String schema, File databaseUpdateFile, KeycloakSession session, JpaUpdaterProvider updater) {
        runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession lockSession) -> {
            DBLockManager dbLockManager = new DBLockManager(lockSession);
            DBLockProvider dbLock2 = dbLockManager.getDBLock();
            dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
            try {
                updater.export(connection, schema, databaseUpdateFile);
            } finally {
                dbLock2.releaseLock();
            }
        }, KeycloakTransactionManager.JTAPolicy.NOT_SUPPORTED);
    }

    @Override
    public Connection getConnection() {
        try {
            DataSource dataSource = getDataSource();
            logger.tracev("CDI DataSource: {0}", dataSource);
            return dataSource.getConnection();
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to database", e);
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

    private DataSource getDataSource() {
        return CDI.current().select(DataSource.class).get();
    }

    private static void runJobInTransaction(KeycloakSessionFactory factory, KeycloakSessionTask task, KeycloakTransactionManager.JTAPolicy jtaPolicy) {
        KeycloakSession session = factory.create();
        KeycloakTransactionManager tx = session.getTransactionManager();
        if (jtaPolicy != null)
            tx.setJTAPolicy(jtaPolicy);
        
        try {
            tx.begin();
            task.run(session);

            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
        } catch (RuntimeException re) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            session.close();
        }
    }

}
