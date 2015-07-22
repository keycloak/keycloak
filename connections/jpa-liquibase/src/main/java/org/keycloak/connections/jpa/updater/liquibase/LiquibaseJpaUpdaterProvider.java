package org.keycloak.connections.jpa.updater.liquibase;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.core.DB2Database;
import liquibase.database.core.DerbyDatabase;
import liquibase.database.core.FirebirdDatabase;
import liquibase.database.core.H2Database;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.core.InformixDatabase;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.core.SQLiteDatabase;
import liquibase.database.core.SybaseASADatabase;
import liquibase.database.core.SybaseDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.logging.LogFactory;
import liquibase.logging.LogLevel;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.servicelocator.ServiceLocator;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.models.KeycloakSession;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LiquibaseJpaUpdaterProvider implements JpaUpdaterProvider {

    private static final Logger logger = Logger.getLogger(LiquibaseJpaUpdaterProvider.class);

    private static final String CHANGELOG = "META-INF/jpa-changelog-master.xml";

    @Override
    public String getCurrentVersionSql(String defaultSchema) {
        return "SELECT ID from " + getTable("DATABASECHANGELOG", defaultSchema) + " ORDER BY DATEEXECUTED DESC LIMIT 1";
    }

    @Override
    public void update(KeycloakSession session, Connection connection, String defaultSchema) {
        logger.debug("Starting database update");

        // Need ThreadLocal as liquibase doesn't seem to have API to inject custom objects into tasks
        ThreadLocalSessionContext.setCurrentSession(session);

        try {
            Liquibase liquibase = getLiquibase(connection, defaultSchema);

            List<ChangeSet> changeSets = liquibase.listUnrunChangeSets((Contexts) null);
            if (!changeSets.isEmpty()) {
                if (changeSets.get(0).getId().equals(FIRST_VERSION)) {
                    Statement statement = connection.createStatement();
                    try {
                        statement.executeQuery("SELECT id FROM " + getTable("REALM", defaultSchema));

                        logger.infov("Updating database from {0} to {1}", FIRST_VERSION, changeSets.get(changeSets.size() - 1).getId());
                        liquibase.markNextChangeSetRan(null);
                    } catch (SQLException e) {
                        logger.info("Initializing database schema");
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        List<RanChangeSet> ranChangeSets = liquibase.getDatabase().getRanChangeSetList();
                        logger.debugv("Updating database from {0} to {1}", ranChangeSets.get(ranChangeSets.size() - 1).getId(), changeSets.get(changeSets.size() - 1).getId());
                    } else {
                        logger.infov("Updating database");
                    }
                }

                liquibase.update((Contexts) null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update database", e);
        } finally {
            ThreadLocalSessionContext.removeCurrentSession();
        }

        logger.debug("Completed database update");
    }

    @Override
    public void validate(Connection connection, String defaultSchema) {
        try {
            Liquibase liquibase = getLiquibase(connection, defaultSchema);

            liquibase.validate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate database", e);
        }
    }

    private Liquibase getLiquibase(Connection connection, String defaultSchema) throws Exception {
        ServiceLocator sl = ServiceLocator.getInstance();

        if (!System.getProperties().containsKey("liquibase.scan.packages")) {
            if (sl.getPackages().remove("liquibase.core")) {
                sl.addPackageToScan("liquibase.core.xml");
            }

            if (sl.getPackages().remove("liquibase.parser")) {
                sl.addPackageToScan("liquibase.parser.core.xml");
            }

            if (sl.getPackages().remove("liquibase.serializer")) {
                sl.addPackageToScan("liquibase.serializer.core.xml");
            }

            sl.getPackages().remove("liquibase.ext");
            sl.getPackages().remove("liquibase.sdk");
        }

        LogFactory.setInstance(new LogWrapper());
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        if (defaultSchema != null) {
            database.setDefaultSchemaName(defaultSchema);
        }
        return new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(getClass().getClassLoader()), database);
    }

    @Override
    public void close() {
    }

    private static class LogWrapper extends LogFactory {

        private liquibase.logging.Logger logger = new liquibase.logging.Logger() {
            @Override
            public void setName(String name) {
            }

            @Override
            public void setLogLevel(String level) {
            }

            @Override
            public void setLogLevel(LogLevel level) {
            }

            @Override
            public void setLogLevel(String logLevel, String logFile) {
            }

            @Override
            public void severe(String message) {
                LiquibaseJpaUpdaterProvider.logger.error(message);
            }

            @Override
            public void severe(String message, Throwable e) {
                LiquibaseJpaUpdaterProvider.logger.error(message, e);
            }

            @Override
            public void warning(String message) {
                LiquibaseJpaUpdaterProvider.logger.warn(message);
            }

            @Override
            public void warning(String message, Throwable e) {
                LiquibaseJpaUpdaterProvider.logger.warn(message, e);
            }

            @Override
            public void info(String message) {
                LiquibaseJpaUpdaterProvider.logger.debug(message);
            }

            @Override
            public void info(String message, Throwable e) {
                LiquibaseJpaUpdaterProvider.logger.debug(message, e);
            }

            @Override
            public void debug(String message) {
                LiquibaseJpaUpdaterProvider.logger.trace(message);
            }

            @Override
            public LogLevel getLogLevel() {
                if (LiquibaseJpaUpdaterProvider.logger.isTraceEnabled()) {
                    return LogLevel.DEBUG;
                } else if (LiquibaseJpaUpdaterProvider.logger.isDebugEnabled()) {
                    return LogLevel.INFO;
                } else {
                    return LogLevel.WARNING;
                }
            }

            @Override
            public void debug(String message, Throwable e) {
                LiquibaseJpaUpdaterProvider.logger.trace(message, e);
            }

            @Override
            public void setChangeLog(DatabaseChangeLog databaseChangeLog) {
            }

            @Override
            public void setChangeSet(ChangeSet changeSet) {
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };

        @Override
        public liquibase.logging.Logger getLog(String name) {
            return logger;
        }

        @Override
        public liquibase.logging.Logger getLog() {
            return logger;
        }

    }

    private String getTable(String table, String defaultSchema) {
        return defaultSchema != null ? defaultSchema + "." + table : table;
    }

}
