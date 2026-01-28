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
import javax.naming.InitialContext;
import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.keycloak.Config;
import org.keycloak.ServerStartupError;
import org.keycloak.common.util.StackUtil;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.connections.jpa.support.EntityManagerProxy;
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

import liquibase.GlobalConfiguration;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.jboss.logging.Logger;

import static org.keycloak.connections.jpa.util.JpaUtils.configureNamedQuery;
import static org.keycloak.connections.jpa.util.JpaUtils.getDatabaseType;
import static org.keycloak.connections.jpa.util.JpaUtils.loadSpecificNamedQueries;

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

        return new DefaultJpaConnectionProvider(createEntityManager(session, true));
    }

    private EntityManager createEntityManager(KeycloakSession session, boolean sessionManaged) {
        EntityManager em;
        if (!jtaEnabled) {
            logger.trace("enlisting EntityManager in JpaKeycloakTransaction");
            em = emf.createEntityManager();
        } else {

            em = emf.createEntityManager(SynchronizationType.SYNCHRONIZED);
        }
        em = EntityManagerProxy.create(session, em, sessionManaged);
        if (!jtaEnabled) {
            session.getTransactionManager().enlist(new JpaKeycloakTransaction(em));
        }
        return em;
    }

    private void addSpecificNamedQueries(KeycloakSession session, Connection connection) {
        EntityManager em = null;
        try {
            em = createEntityManager(session, false);
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
                                properties.put(AvailableSettings.JAKARTA_JTA_DATASOURCE, dataSource);
                            } else {
                                properties.put(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource);
                            }
                        } else {
                            String url = config.get("url");
                            String driver = config.get("driver");
                            url = augmentJdbcUrl(driver, url);
                            properties.put(AvailableSettings.JAKARTA_JDBC_URL, url);
                            properties.put(AvailableSettings.JAKARTA_JDBC_DRIVER, driver);

                            String user = config.get("user");
                            if (user != null) {
                                properties.put(AvailableSettings.JAKARTA_JDBC_USER, user);
                            }
                            String password = config.get("password");
                            if (password != null) {
                                properties.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, password);
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

                            String driverDialect = config.get("driverDialect");
                            // use configured dialect, else rely on Hibernate detection
                            if (driverDialect != null && !driverDialect.isBlank()) {
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

                            logger.debug("Calling migrateModel");
                            migrateModel(session);
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

    protected void startGlobalStats(KeycloakSession session, int globalStatsIntervalSecs) {
        logger.debugf("Started Hibernate statistics with the interval %s seconds", globalStatsIntervalSecs);
        TimerProvider timer = session.getProvider(TimerProvider.class);
        timer.scheduleTask(new HibernateStatsReporter(emf), globalStatsIntervalSecs * 1000);
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
                String url = config.get("url");
                String driver = config.get("driver");
                url = augmentJdbcUrl(driver, url);
                Class.forName(driver);
                return DriverManager.getConnection(StringPropertyReplacer.replaceProperties(url, System.getProperties()::getProperty), config.get("user"), config.get("password"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private String augmentJdbcUrl(String driver, String url) {
        if (driver.equals("org.postgresql.xa.PGXADataSource") || driver.equals("org.postgresql.Driver")) {
            url = addPostgreSQLKeywords(url);
        }
        if (driver.equals("org.h2.Driver")) {
            url = addH2NonKeywords(url);
        }
        return url;
    }

    @Override
    public String getSchema() {
        String schema = config.get("schema");
        if (schema != null && schema.contains("-") && ! Boolean.parseBoolean(System.getProperty(GlobalConfiguration.PRESERVE_SCHEMA_CASE.getKey()))) {
            System.setProperty(GlobalConfiguration.PRESERVE_SCHEMA_CASE.getKey(), "true");
            logger.warnf("The passed schema '%s' contains a dash. Setting liquibase config option PRESERVE_SCHEMA_CASE to true. See https://github.com/keycloak/keycloak/issues/20870 for more information.", schema);
        }
        return schema;
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
        // Using a lock to prevent concurrent migration in concurrently starting nodes
        DBLockManager dbLockManager = new DBLockManager(session);
        DBLockProvider dbLock = dbLockManager.getDBLock();
        dbLock.waitForLock(DBLockProvider.Namespace.DATABASE);
        try {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), MigrationModelManager::migrate);
        } finally {
            dbLock.releaseLock();
        }
    }

    /**
     * Starting with H2 version 2.x, marking "VALUE" as a non-keyword is necessary as some columns are named "VALUE" in the Keycloak schema.
     * <p />
     * Alternatives considered and rejected:
     * <ul>
     * <li>customizing H2 Database dialect -&gt; wouldn't work for existing Liquibase scripts.</li>
     * <li>adding quotes to <code>@Column(name="VALUE")</code> annotations -&gt; would require testing for all DBs, wouldn't work for existing Liquibase scripts.</li>
     * </ul>
     * Downsides of this solution: Release notes needed to point out that any H2 JDBC URL parameter with <code>NON_KEYWORDS</code> needs to add the keyword <code>VALUE</code> manually.
     * @return JDBC URL with <code>NON_KEYWORDS=VALUE</code> appended if the URL doesn't contain <code>NON_KEYWORDS=</code> yet
     */
    private String addH2NonKeywords(String jdbcUrl) {
        if (!jdbcUrl.contains("NON_KEYWORDS=")) {
            jdbcUrl = jdbcUrl + ";NON_KEYWORDS=VALUE";
        }
        return jdbcUrl;
    }

    /**
     * For a PostgreSQL cluster, Keycloak would need to connect to the primary node that is writable.
     * The `targetServerType` should avoid connecting to a reader instance accidentally during node failover.

     * @return JDBC URL with <code>targetServerType=primary</code> appended if the URL doesn't contain <code>targetServerType=</code> yet
     */
    private String addPostgreSQLKeywords(String jdbcUrl) {
        if (!jdbcUrl.contains("targetServerType=")) {
            if (jdbcUrl.contains("?")) {
                jdbcUrl = jdbcUrl + "&";
            } else {
                jdbcUrl = jdbcUrl + "?";
            }
            jdbcUrl = jdbcUrl + "targetServerType=primary";
        }
        return jdbcUrl;
    }

}
