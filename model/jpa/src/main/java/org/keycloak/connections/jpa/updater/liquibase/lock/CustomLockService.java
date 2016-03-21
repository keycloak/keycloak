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
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import liquibase.database.Database;
import liquibase.database.core.DerbyDatabase;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.lockservice.StandardLockService;
import liquibase.logging.LogFactory;
import liquibase.sql.visitor.AbstractSqlVisitor;
import liquibase.sql.visitor.SqlVisitor;
import liquibase.statement.core.CreateDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.DropTableStatement;
import liquibase.statement.core.InitializeDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.RawSqlStatement;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.reflections.Reflections;

/**
 * Liquibase lock service, which has some bugfixes and assumes timeouts to be configured in milliseconds
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CustomLockService extends StandardLockService {

    private static final Logger log = Logger.getLogger(CustomLockService.class);

    private long changeLogLocRecheckTimeMillis = -1;

    @Override
    public void setChangeLogLockRecheckTime(long changeLogLocRecheckTime) {
        super.setChangeLogLockRecheckTime(changeLogLocRecheckTime);
        this.changeLogLocRecheckTimeMillis = changeLogLocRecheckTime;
    }

    // Bug in StandardLockService.getChangeLogLockRecheckTime()
    @Override
    public Long getChangeLogLockRecheckTime() {
        if (changeLogLocRecheckTimeMillis == -1) {
            return super.getChangeLogLockRecheckTime();
        } else {
            return changeLogLocRecheckTimeMillis;
        }
    }

    @Override
    public void init() throws DatabaseException {
        boolean createdTable = false;
        Executor executor = ExecutorService.getInstance().getExecutor(database);

        if (!hasDatabaseChangeLogLockTable()) {

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Create Database Lock Table");
                }
                executor.execute(new CreateDatabaseChangeLogLockTableStatement());
                database.commit();
            } catch (DatabaseException de) {
                log.warn("Failed to create lock table. Maybe other transaction created in the meantime. Retrying...");
                if (log.isDebugEnabled()) {
                    log.debug(de.getMessage(), de); //Log details at debug level
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

            createdTable = true;
        }


        if (!isDatabaseChangeLogLockTableInitialized(createdTable)) {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Initialize Database Lock Table");
                }
                executor.execute(new InitializeDatabaseChangeLogLockTableStatement());
                database.commit();

            } catch (DatabaseException de) {
                log.warn("Failed to insert first record to the lock table. Maybe other transaction inserted in the meantime. Retrying...");
                if (log.isDebugEnabled()) {
                    log.debug(de.getMessage(), de); // Log details at debug level
                }
                database.rollback();
                throw new LockRetryException(de);
            }

            log.debug("Initialized record in the database lock table");
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

    @Override
    public void waitForLock() throws LockException {
        boolean locked = false;
        long startTime = Time.toMillis(Time.currentTime());
        long timeToGiveUp = startTime + (getChangeLogLockWaitTime());

        while (!locked && Time.toMillis(Time.currentTime()) < timeToGiveUp) {
            locked = acquireLock();
            if (!locked) {
                int remainingTime = ((int)(timeToGiveUp / 1000)) - Time.currentTime();
                log.debugf("Waiting for changelog lock... Remaining time: %d seconds", remainingTime);
                try {
                    Thread.sleep(getChangeLogLockRecheckTime());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!locked) {
            DatabaseChangeLogLock[] locks = listLocks();
            String lockedBy;
            if (locks.length > 0) {
                DatabaseChangeLogLock lock = locks[0];
                lockedBy = lock.getLockedBy() + " since " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(lock.getLockGranted());
            } else {
                lockedBy = "UNKNOWN";
            }
            throw new LockException("Could not acquire change log lock.  Currently locked by " + lockedBy);
        }
    }


}
