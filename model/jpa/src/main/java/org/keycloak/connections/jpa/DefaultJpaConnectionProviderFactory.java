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

package org.keycloak.connections.jpa;

import static org.keycloak.connections.jpa.util.JpaUtils.configureNamedQuery;
import static org.keycloak.connections.jpa.util.JpaUtils.getDatabaseType;
import static org.keycloak.connections.jpa.util.JpaUtils.loadSpecificNamedQueries;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.ServerStartupError;
import org.keycloak.common.util.StackUtil;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.timer.TimerProvider;
import org.keycloak.transaction.JtaTransactionManagerLookup;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultJpaConnectionProviderFactory implements JpaConnectionProviderFactory, ServerInfoAwareProviderFactory {

    private static final Logger logger = Logger.getLogger(DefaultJpaConnectionProviderFactory.class);

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
        logger.trace("Create JpaConnectionProvider");
        lazyInit(session);

        return new DefaultJpaConnectionProvider(createEntityManager(session));
    }

    private EntityManager createEntityManager(KeycloakSession session) {
        EntityManager em;
        if (!jtaEnabled) {
            logger.trace("enlisting EntityManager in JpaKeycloakTransaction");
            em = emf.createEntityManager();
        } else {

            em = emf.createEntityManager(SynchronizationType.SYNCHRONIZED);
        }
        em = PersistenceExceptionConverter.create(session, em);
        if (!jtaEnabled) {
            session.getTransactionManager().enlist(new JpaKeycloakTransaction(em));
        }
        return em;
    }

    private void addSpecificNamedQueries(KeycloakSession session, Connection connection) {
        EntityManager em = null;
        try {
            em = createEntityManager(session);
            String dbKind = getDatabaseType(connection.getMetaData().getDatabaseProductName());
            for (Map.Entry<Object, Object> query : loadSpecificNamedQueries(dbKind.toLowerCase()).entrySet()) {
                String queryName = query.getKey().toString();
                String querySql = query.getValue().toString();
                configureNamedQuery(queryName, querySql, em);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            JpaUtils.closeEntityManager(em);
        }
    }

    @Override
    public void close() {
        if (emf != null) {
            emf.close();
        }
    }

    @Override
    public String getId() {
        return "default";
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
                        logger.debugf("Initializing JPA connections%s", StackUtil.getShortStackTrace());

                        Map<String, Object> properties = new HashMap<>();

                        String unitName = "keycloak-default";

                        String dataSource = config.get("dataSource");
                        if (dataSource != null) {
                            if (config.getBoolean("jta", jtaEnabled)) {
                                properties.put(AvailableSettings.JPA_JTA_DATASOURCE, dataSource);
                            } else {
                                properties.put(AvailableSettings.JPA_NON_JTA_DATASOURCE, dataSource);
                            }
                        } else {
                            properties.put(AvailableSettings.JPA_JDBC_URL, config.get("url"));
                            properties.put(AvailableSettings.JPA_JDBC_DRIVER, config.get("driver"));

                            String user = config.get("user");
                            if (user != null) {
                                properties.put(AvailableSettings.JPA_JDBC_USER, user);
                            }
                            String password = config.get("password");
                            if (password != null) {
                                properties.put(AvailableSettings.JPA_JDBC_PASSWORD, password);
                            }
                        }

                        String schema = getSchema();
                        if (schema != null) {
                            properties.put(JpaUtils.HIBERNATE_DEFAULT_SCHEMA, schema);
                        }

                        MigrationStrategy migrationStrategy = getMigrationStrategy();
                        boolean initializeEmpty = config.getBoolean("initializeEmpty", true);
                        File databaseUpdateFile = getDatabaseUpdateFile();

                        properties.put("hibernate.show_sql", config.getBoolean("showSql", false));
                        properties.put("hibernate.format_sql", config.getBoolean("formatSql", true));

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
                            if (jtaEnabled) {
                                properties.put(AvailableSettings.JTA_PLATFORM, new AbstractJtaPlatform() {
                                    @Override
                                    protected TransactionManager locateTransactionManager() {
                                        return jtaLookup.getTransactionManager();
                                    }

                                    @Override
                                    protected UserTransaction locateUserTransaction() {
                                        return null;
                                    }
                                });
                            }
                            Collection<ClassLoader> classLoaders = new ArrayList<>();
                            if (properties.containsKey(AvailableSettings.CLASSLOADERS)) {
                                classLoaders.addAll((Collection<ClassLoader>) properties.get(AvailableSettings.CLASSLOADERS));
                            }
                            classLoaders.add(getClass().getClassLoader());
                            properties.put(AvailableSettings.CLASSLOADERS, classLoaders);
                            emf = JpaUtils.createEntityManagerFactory(session, unitName, properties, jtaEnabled);
                            addSpecificNamedQueries(session, connection);
                            logger.trace("EntityManagerFactory created");

                            if (globalStatsInterval != -1) {
                                startGlobalStats(session, globalStatsInterval);
                            }

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

            logger.infof("Database info: %s", operationalInfo.toString());
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
        JpaUpdaterProvider updater = session.getProvider(JpaUpdaterProvider.class, LiquibaseJpaUpdaterProviderFactory.PROVIDER_ID);

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
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession lockSession) {
                DBLockManager dbLockManager = new DBLockManager(lockSession);
                DBLockProvider dbLock2 = dbLockManager.getDBLock();
                dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
                try {
                    updater.update(connection, schema);
                } finally {
                    dbLock2.releaseLock();
                }
            }
        });
    }

    protected void export(Connection connection, String schema, File databaseUpdateFile, KeycloakSession session, JpaUpdaterProvider updater) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession lockSession) {
                DBLockManager dbLockManager = new DBLockManager(lockSession);
                DBLockProvider dbLock2 = dbLockManager.getDBLock();
                dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
                try {
                    updater.export(connection, schema, databaseUpdateFile);
                } finally {
                    dbLock2.releaseLock();
                }
            }
        });
    }

    @Override
    public Connection getConnection() {
        try {
            String dataSourceLookup = config.get("dataSource");
            if (dataSourceLookup != null) {
                DataSource dataSource = (DataSource) new InitialContext().lookup(dataSourceLookup);
                return dataSource.getConnection();
            } else {
                Class.forName(config.get("driver"));
                return DriverManager.getConnection(StringPropertyReplacer.replaceProperties(config.get("url"), System.getProperties()), config.get("user"), config.get("password"));
            }
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

    private void migrateModel(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), MigrationModelManager::migrate);
    }
}
