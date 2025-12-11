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

package org.keycloak.connections.jpa.updater.liquibase.lock;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.util.Time;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseConstants;
import org.keycloak.models.dblock.DBLockProvider;

import liquibase.Scope;
import liquibase.database.core.DerbyDatabase;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.lockservice.StandardLockService;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.CreateDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.DropTableStatement;
import liquibase.statement.core.InitializeDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.LockDatabaseChangeLogStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.jboss.logging.Logger;

/**
 * Liquibase lock service, which has some bugfixes and assumes timeouts to be configured in milliseconds
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CustomLockService extends StandardLockService {

    private static final Logger log = Logger.getLogger(CustomLockService.class);

    @Override
    protected boolean isDatabaseChangeLogLockTableCreated() throws DatabaseException {
        boolean originalReturnValue = super.isDatabaseChangeLogLockTableCreated();
        if (originalReturnValue && database.getConnection().getDatabaseProductName().equals("H2")) {
            /* Liquibase only checks that the table exists. On the H2 database, creation of a table with a primary key is not atomic,
               and the primary key might not be visible yet. The primary key would be needed to prevent inserting the data into the table
               a second time. Inserting it a second time might lead to a failure when creating the primary key, which would then roll back
               the creation of the table. Therefore, at least on the H2 database, checking for the primary key is essential.

               An existing DATABASECHANGELOG might indicate that the insertion of data was completed previously.
               Still, this isn't working with the DBLockTest which deletes only the DATABASECHANGELOGLOCK table.

               See https://github.com/keycloak/keycloak/issues/15487 for more information.
             */
            Table lockTable = (Table) new Table().setName(database.getDatabaseChangeLogLockTableName()).setSchema(
                    new Schema(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName()));
            SnapshotGeneratorFactory instance = SnapshotGeneratorFactory.getInstance();

            try {
                DatabaseSnapshot snapshot = instance.createSnapshot(lockTable.getSchema().toCatalogAndSchema(), database,
                        new SnapshotControl(database, false, Table.class, PrimaryKey.class).setWarnIfObjectNotFound(false));
                Table lockTableFromSnapshot = snapshot.get(lockTable);
                if (lockTableFromSnapshot == null) {
                    throw new RuntimeException("DATABASECHANGELOGLOCK not found, although Liquibase claims it exists.");
                } else if (lockTableFromSnapshot.getPrimaryKey() == null) {
                    log.warn("Primary key not found - table creation not complete yet.");
                    return false;
                }
            } catch (InvalidExampleException e) {
                throw new RuntimeException(e);
            }
        }
        return originalReturnValue;
    }

    @Override
    public void init() throws DatabaseException {
        Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database);

        if (!isDatabaseChangeLogLockTableCreated()) {

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Create Database Lock Table");
                }
                executor.execute(new CreateDatabaseChangeLogLockTableStatement());
                database.commit();
            } catch (DatabaseException de) {
                log.warn("Failed to create lock table. Maybe other transaction created in the meantime. Retrying...", de);
                if (log.isTraceEnabled()) {
                    log.trace(de.getMessage(), de); //Log details at trace level
                }
                database.rollback();
                throw new LockRetryException(de);
            }

            log.debugf("Created database lock table with name: %s", database.escapeTableName(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName()));

            try {
                Field field = Reflections.findDeclaredField(StandardLockService.class, "hasDatabaseChangeLogLockTable");
                Reflections.setAccessible(field);
                field.set(CustomLockService.this, true);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
        }

        try {
            Set<Integer> currentIds = currentIdsInDatabaseChangeLogLockTable();
            Set<Integer> customNamespaceIds = Arrays.stream(DBLockProvider.Namespace.values())
                    .map(DBLockProvider.Namespace::getId)
                    .collect(Collectors.toSet());
            if (!currentIds.containsAll(customNamespaceIds)) {
                if (log.isTraceEnabled()) {
                    log.tracef("Initialize Database Lock Table, current locks %s", currentIds);
                }
                executor.execute(new CustomInitializeDatabaseChangeLogLockTableStatement(currentIds));
                database.commit();

                log.debug("Initialized record in the database lock table");
            }

        } catch (DatabaseException de) {
            log.warn("Failed to insert first record to the lock table. Maybe other transaction inserted in the meantime. Retrying...", de);
            if (log.isTraceEnabled()) {
                log.trace(de.getMessage(), de); // Log details at trace level
            }
            database.rollback();
            throw new LockRetryException(de);
        }


        // Keycloak doesn't support Derby, but keep it for sure...
        if (executor.updatesDatabase() && database instanceof DerbyDatabase && ((DerbyDatabase) database).supportsBooleanDataType()) { //check if the changelog table is of an old smallint vs. boolean format
            String lockTable = database.escapeTableName(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName());
            Object obj = executor.queryForObject(new RawSqlStatement("select min(locked) as test from " + lockTable + " fetch first row only"), Object.class);
            if (!(obj instanceof Boolean)) { //wrong type, need to recreate table
                executor.execute(new DropTableStatement(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName(), false));
                executor.execute(new CreateDatabaseChangeLogLockTableStatement());
                executor.execute(new InitializeDatabaseChangeLogLockTableStatement());
            }
        }

    }

    private Set<Integer> currentIdsInDatabaseChangeLogLockTable() throws DatabaseException {
        try {
            Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database);
            String idColumnName = database.escapeColumnName(database.getLiquibaseCatalogName(),
                    database.getLiquibaseSchemaName(),
                    database.getDatabaseChangeLogLockTableName(),
                    "ID");
            String lockTableName = database.escapeTableName(database.getLiquibaseCatalogName(),
                    database.getLiquibaseSchemaName(),
                    database.getDatabaseChangeLogLockTableName());
            SqlStatement sqlStatement = new RawSqlStatement("SELECT " + idColumnName + " FROM " + lockTableName);
            List<Map<String, ?>> rows = executor.queryForList(sqlStatement);
            Set<Integer> ids = rows.stream().map(columnMap -> ((Number) columnMap.get("ID")).intValue()).collect(Collectors.toSet());
            database.commit();
            return ids;
        } catch (UnexpectedLiquibaseException ulie) {
            // It can happen with MariaDB Galera 10.1 that UnexpectedLiquibaseException is rethrown due the DB lock.
            // It is sufficient to just rollback transaction and retry in that case.
            if (ulie.getCause() != null && ulie.getCause() instanceof DatabaseException) {
                throw (DatabaseException) ulie.getCause();
            } else {
                throw ulie;
            }
        }
    }

    @Override
    public void waitForLock() {
        waitForLock(new LockDatabaseChangeLogStatement());
    }

    public void waitForLock(DBLockProvider.Namespace lock) {
        waitForLock(new CustomLockDatabaseChangeLogStatement(lock.getId()));
    }

    private void waitForLock(LockDatabaseChangeLogStatement lockStmt) {
        boolean locked = false;
        long startTime = Time.toMillis(Time.currentTime());
        long timeToGiveUp = startTime + (getChangeLogLockWaitTime());
        boolean nextAttempt = true;

        while (nextAttempt) {
            locked = acquireLock(lockStmt);
            if (!locked) {
                int remainingTime = ((int)(timeToGiveUp / 1000)) - Time.currentTime();
                if (remainingTime > 0) {
                    log.debugf("Will try to acquire log another time. Remaining time: %d seconds", remainingTime);
                } else {
                    nextAttempt = false;
                }
            } else {
                nextAttempt = false;
            }
        }

        if (!locked) {
            int timeout = ((int)(getChangeLogLockWaitTime() / 1000));
            throw new IllegalStateException("Could not acquire change log lock within specified timeout " + timeout + " seconds.  Currently locked by other transaction");
        }
    }

    @Override
    public boolean acquireLock() {
        return acquireLock(new LockDatabaseChangeLogStatement());
    }

    private boolean acquireLock(LockDatabaseChangeLogStatement lockStmt) {
        if (hasChangeLogLock) {
            // We already have a lock
            return true;
        }

        Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database);

        try {
            database.rollback();

            // Ensure table created and lock record inserted
            this.init();
        } catch (DatabaseException de) {
            throw new IllegalStateException("Failed to retrieve lock", de);
        }

        try {
            log.debug("Trying to lock database");
            executor.execute(lockStmt);
            log.debug("Successfully acquired database lock");

            hasChangeLogLock = true;
            database.setCanCacheLiquibaseTableInfo(true);
            return true;

        } catch (DatabaseException de) {
            log.warn("Lock didn't yet acquired. Will possibly retry to acquire lock. Details: " + de.getMessage(), de);
            if (log.isTraceEnabled()) {
                log.debug(de.getMessage(), de);
            }
            return false;
        }
    }


    @Override
    public void releaseLock() {
        try {
            if (hasChangeLogLock) {
                log.debug("Going to release database lock");
                database.commit();
            } else {
                log.warn("Attempt to release lock, which is not owned by current transaction");
            }
        } catch (Exception e) {
            log.error("Database error during release lock", e);
        } finally {
            try {
                hasChangeLogLock = false;
                database.setCanCacheLiquibaseTableInfo(false);
                database.rollback();
            } catch (DatabaseException ignored) {
            }
        }
    }


}
