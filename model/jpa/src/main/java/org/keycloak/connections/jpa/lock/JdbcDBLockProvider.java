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

package org.keycloak.connections.jpa.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.util.Retry;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.logging.Logger;

/**
 * Database lock implemented directly on top of JDBC. It uses the same {@code DATABASECHANGELOGLOCK} table that was
 * historically managed by Liquibase, so it stays compatible with existing deployments. The lock is a pessimistic
 * row lock ({@code SELECT ... FOR UPDATE}, same algorithm as Hibernate's {@code LockMode.PESSIMISTIC_WRITE}) held
 * for the lifetime of the transaction of the dedicated connection.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JdbcDBLockProvider implements DBLockProvider {

    private static final Logger logger = Logger.getLogger(JdbcDBLockProvider.class);

    static final String LOCK_TABLE_NAME = "DATABASECHANGELOGLOCK";

    // 10 should be sufficient
    private final int DEFAULT_MAX_ATTEMPTS = 10;

    private final JdbcDBLockProviderFactory factory;
    private final KeycloakSession session;

    private Connection dbConnection;
    private String schema;
    private String lockTable;
    private SqlDialect dialect;
    private boolean initialized = false;
    private boolean hasLock = false;
    private Namespace namespaceLocked = null;

    public JdbcDBLockProvider(JdbcDBLockProviderFactory factory, KeycloakSession session) {
        this.factory = factory;
        this.session = session;
    }


    private void lazyInit() {
        if (!initialized) {
            JpaConnectionProviderFactory jpaProviderFactory = (JpaConnectionProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(JpaConnectionProvider.class);

            this.dbConnection = jpaProviderFactory.getConnection();
            this.schema = jpaProviderFactory.getSchema();

            try {
                // The row lock is only held until the end of the transaction, so auto-commit must be disabled
                dbConnection.setAutoCommit(false);
                this.dialect = SqlDialect.of(dbConnection);
                this.lockTable = schema == null ? LOCK_TABLE_NAME : quote(schema) + "." + LOCK_TABLE_NAME;
                initialized = true;
            } catch (SQLException exception) {
                safeRollbackConnection();
                safeCloseConnection();
                throw new IllegalStateException(exception);
            }
        }
    }

    private String quote(String identifier) throws SQLException {
        if (SqlDialect.isSimpleIdentifier(identifier)) {
            return identifier;
        }
        String quoteString = dbConnection.getMetaData().getIdentifierQuoteString();
        if (quoteString == null || quoteString.isBlank()) {
            return identifier;
        }
        quoteString = quoteString.trim();
        return quoteString + identifier.replace(quoteString, quoteString + quoteString) + quoteString;
    }

    // Assumed transaction was rolled-back and we want to start with new DB connection
    private void restart() {
        safeCloseConnection();
        lazyInit();
    }

    @Override
    public void waitForLock(Namespace lock) {
        KeycloakModelUtils.suspendJtaTransaction(session.getKeycloakSessionFactory(), () -> {

            lazyInit();

            if (this.hasLock) {
                if (lock.equals(this.namespaceLocked)) {
                    logger.warnf("Locking namespace %s which was already locked in this provider", lock);
                    return;
                } else {
                    throw new RuntimeException("Trying to get a lock when one was already taken by the provider");
                }
            }

            logger.debugf("Going to lock namespace=%s", lock);
            Retry.executeWithBackoff((int iteration) -> {

                waitForLockInternal(lock);
                namespaceLocked = lock;

            }, (int iteration, Throwable e) -> {

                safeRollbackConnection();
                if (e instanceof LockRetryException && iteration < (DEFAULT_MAX_ATTEMPTS - 1)) {
                    // Indicates we should try to acquire lock again in different transaction
                    restart();
                } else {
                    safeCloseConnection();
                    // Fail fast with the original exception, as retrying would only hit an uninitialized provider
                    if (e instanceof RuntimeException re) {
                        throw re;
                    }
                    throw (Error) e;
                }

            }, DEFAULT_MAX_ATTEMPTS, 10);
        });

    }

    private void waitForLockInternal(Namespace lock) {
        boolean locked = false;
        long timeToGiveUp = System.currentTimeMillis() + factory.getLockWaitTimeoutMillis();

        while (true) {
            locked = acquireLock(lock);
            if (locked) {
                break;
            }
            long remainingTime = (timeToGiveUp - System.currentTimeMillis()) / 1000;
            if (remainingTime > 0) {
                logger.debugf("Will try to acquire log another time. Remaining time: %d seconds", remainingTime);
            } else {
                break;
            }
        }

        if (!locked) {
            int timeout = (int) (factory.getLockWaitTimeoutMillis() / 1000);
            throw new IllegalStateException("Could not acquire change log lock within specified timeout " + timeout + " seconds.  Currently locked by other transaction");
        }
    }

    private boolean acquireLock(Namespace lock) {
        try {
            dbConnection.rollback();

            // Ensure table created and lock records inserted
            initLockTable();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to retrieve lock", e);
        }

        try (Statement statement = dbConnection.createStatement()) {
            logger.debug("Trying to lock database");
            String sql = selectForUpdateSql(lock.getId());
            logger.debugf("SQL command for pessimistic lock: %s", sql);
            statement.execute(sql);
            logger.debug("Successfully acquired database lock");

            hasLock = true;
            return true;
        } catch (SQLException e) {
            logger.warn("Lock didn't yet acquired. Will possibly retry to acquire lock. Details: " + e.getMessage(), e);
            return false;
        }
    }

    private String selectForUpdateSql(int id) {
        String sqlBase = "SELECT ID FROM " + lockTable;
        String sqlWhere = " WHERE ID=" + id;

        switch (dialect) {
            case MYSQL, MARIADB, POSTGRESQL, H2, ORACLE:
                return sqlBase + sqlWhere + " FOR UPDATE";
            case MSSQL:
                return sqlBase + " WITH (UPDLOCK, ROWLOCK)" + sqlWhere;
            default:
                logger.warn("No direct support for the current database. Database lock may not work correctly");
                return sqlBase + sqlWhere;
        }
    }

    // Creates the lock table and the lock records if they are missing
    private void initLockTable() throws SQLException {
        Set<Integer> currentIds = null;
        try {
            currentIds = currentIdsInLockTable();
        } catch (SQLException e) {
            // Table most probably does not exist yet
            logger.debug("Lock table could not be read, it will be created", e);
            dbConnection.rollback();
        }

        if (currentIds != null && dialect == SqlDialect.H2 && !lockTableHasPrimaryKey()) {
            /* On the H2 database, creation of a table with a primary key is not atomic, and the primary key might not be visible yet.
               The primary key is needed to prevent inserting the data into the table a second time. Inserting it a second time
               might lead to a failure when creating the primary key, which would then roll back the creation of the table.
               Therefore, at least on the H2 database, checking for the primary key is essential.

               See https://github.com/keycloak/keycloak/issues/15487 for more information.
             */
            logger.warn("Primary key not found - table creation not complete yet.");
            dbConnection.rollback();
            throw new LockRetryException("Lock table primary key not found");
        }

        if (currentIds == null) {
            try (Statement statement = dbConnection.createStatement()) {
                logger.trace("Create Database Lock Table");
                statement.execute(createTableSql());
                dbConnection.commit();
            } catch (SQLException e) {
                logger.warn("Failed to create lock table. Maybe other transaction created in the meantime. Retrying...", e);
                dbConnection.rollback();
                throw new LockRetryException(e);
            }

            logger.debugf("Created database lock table with name: %s", lockTable);

            try {
                currentIds = currentIdsInLockTable();
            } catch (SQLException e) {
                dbConnection.rollback();
                throw new LockRetryException(e);
            }
        }

        Set<Integer> customNamespaceIds = Arrays.stream(Namespace.values())
                .map(Namespace::getId)
                .collect(Collectors.toSet());
        if (!currentIds.containsAll(customNamespaceIds)) {
            logger.tracef("Initialize Database Lock Table, current locks %s", currentIds);
            try (PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO " + lockTable + " (ID, LOCKED) VALUES (?, ?)")) {
                for (int id : customNamespaceIds) {
                    if (!currentIds.contains(id)) {
                        statement.setInt(1, id);
                        statement.setBoolean(2, false);
                        statement.executeUpdate();
                    }
                }
                dbConnection.commit();
                logger.debug("Initialized record in the database lock table");
            } catch (SQLException e) {
                logger.warn("Failed to insert first record to the lock table. Maybe other transaction inserted in the meantime. Retrying...", e);
                dbConnection.rollback();
                throw new LockRetryException(e);
            }
        }
    }

    private Set<Integer> currentIdsInLockTable() throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (Statement statement = dbConnection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT ID FROM " + lockTable)) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        dbConnection.commit();
        return ids;
    }

    private boolean lockTableHasPrimaryKey() throws SQLException {
        String effectiveSchema = schema != null ? schema : dbConnection.getSchema();
        try (ResultSet rs = dbConnection.getMetaData().getPrimaryKeys(null, null, LOCK_TABLE_NAME)) {
            while (rs.next()) {
                if (effectiveSchema == null || effectiveSchema.equalsIgnoreCase(rs.getString("TABLE_SCHEM"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String createTableSql() {
        String booleanType = switch (dialect) {
            case MYSQL, MARIADB -> "BIT(1)";
            case ORACLE -> "NUMBER(1)";
            case MSSQL -> "BIT";
            default -> "BOOLEAN";
        };
        String timestampType = switch (dialect) {
            case POSTGRESQL -> "TIMESTAMP WITHOUT TIME ZONE";
            case MYSQL, MARIADB, MSSQL -> "DATETIME";
            default -> "TIMESTAMP";
        };
        String varcharType = dialect == SqlDialect.ORACLE ? "VARCHAR2(255)" : "VARCHAR(255)";
        return "CREATE TABLE " + lockTable + " (" +
                "ID INT NOT NULL, " +
                "LOCKED " + booleanType + " NOT NULL, " +
                "LOCKGRANTED " + timestampType + ", " +
                "LOCKEDBY " + varcharType + ", " +
                "CONSTRAINT PK_DATABASECHANGELOGLOCK PRIMARY KEY (ID))";
    }

    @Override
    public void releaseLock() {
        KeycloakModelUtils.suspendJtaTransaction(session.getKeycloakSessionFactory(), () -> {
            lazyInit();

            logger.debugf("Going to release database lock namespace=%s", namespaceLocked);
            namespaceLocked = null;

            try {
                if (hasLock) {
                    logger.debug("Going to release database lock");
                    dbConnection.commit();
                } else {
                    logger.warn("Attempt to release lock, which is not owned by current transaction");
                }
            } catch (SQLException e) {
                logger.error("Database error during release lock", e);
            } finally {
                hasLock = false;
                safeRollbackConnection();
            }
        });
    }

    @Override
    public Namespace getCurrentLock() {
        return this.namespaceLocked;
    }

    @Override
    public boolean supportsForcedUnlock() {
        // Implementation based on "SELECT FOR UPDATE" can't force unlock as it's locked by other transaction
        return false;
    }

    @Override
    public void destroyLockInfo() {
        KeycloakModelUtils.suspendJtaTransaction(session.getKeycloakSessionFactory(), () -> {
            lazyInit();

            try (Statement statement = dbConnection.createStatement()) {
                statement.execute("DROP TABLE " + lockTable);
                dbConnection.commit();
                logger.debug("Destroyed lock table");
            } catch (SQLException e) {
                logger.error("Failed to destroy lock table");
                safeRollbackConnection();
            }
        });
    }

    @Override
    public void close() {
        KeycloakModelUtils.suspendJtaTransaction(session.getKeycloakSessionFactory(), this::safeCloseConnection);
    }

    private void safeRollbackConnection() {
        if (dbConnection != null) {
            try {
                this.dbConnection.rollback();
            } catch (SQLException se) {
                logger.warn("Failed to rollback connection after error", se);
            }
        }
    }

    private void safeCloseConnection() {
        // Close to prevent in-mem databases from closing
        if (dbConnection != null) {
            try {
                dbConnection.close();
                dbConnection = null;
                initialized = false;
                hasLock = false;
            } catch (SQLException e) {
                logger.warn("Failed to close connection", e);
            }
        }
    }
}
