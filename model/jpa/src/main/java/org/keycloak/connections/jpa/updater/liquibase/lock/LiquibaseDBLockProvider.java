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

import java.sql.Connection;
import java.sql.SQLException;

import liquibase.Liquibase;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.lockservice.LockService;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.dblock.DBLockProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LiquibaseDBLockProvider implements DBLockProvider {

    private static final Logger logger = Logger.getLogger(LiquibaseDBLockProvider.class);

    // 3 should be sufficient (Potentially one failure for createTable and one for insert record)
    private int DEFAULT_MAX_ATTEMPTS = 3;


    private final LiquibaseDBLockProviderFactory factory;
    private final KeycloakSession session;

    private CustomLockService lockService;
    private Connection dbConnection;
    private boolean initialized = false;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    public LiquibaseDBLockProvider(LiquibaseDBLockProviderFactory factory, KeycloakSession session) {
        this.factory = factory;
        this.session = session;
    }

    private void lazyInit() {
        if (!initialized) {
            LiquibaseConnectionProvider liquibaseProvider = session.getProvider(LiquibaseConnectionProvider.class);
            JpaConnectionProviderFactory jpaProviderFactory = (JpaConnectionProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(JpaConnectionProvider.class);

            this.dbConnection = jpaProviderFactory.getConnection();
            String defaultSchema = jpaProviderFactory.getSchema();

            try {
                Liquibase liquibase = liquibaseProvider.getLiquibase(dbConnection, defaultSchema);

                this.lockService = new CustomLockService();
                lockService.setChangeLogLockWaitTime(factory.getLockWaitTimeoutMillis());
                lockService.setDatabase(liquibase.getDatabase());
                initialized = true;
            } catch (LiquibaseException exception) {
                safeRollbackConnection();
                safeCloseConnection();
                throw new IllegalStateException(exception);
            }
        }
    }

    // Assumed transaction was rolled-back and we want to start with new DB connection
    private void restart() {
        safeCloseConnection();
        this.dbConnection = null;
        this.lockService = null;
        initialized = false;
        lazyInit();
    }


    @Override
    public void waitForLock() {
        lazyInit();

        while (maxAttempts > 0) {
            try {
                lockService.waitForLock();
                factory.setHasLock(true);
                this.maxAttempts = DEFAULT_MAX_ATTEMPTS;
                return;
            } catch (LockRetryException le) {
                // Indicates we should try to acquire lock again in different transaction
                safeRollbackConnection();
                restart();
                maxAttempts--;
            } catch (RuntimeException re) {
                safeRollbackConnection();
                safeCloseConnection();
                throw re;
            }
        }
    }


    @Override
    public void releaseLock() {
        lazyInit();

        lockService.releaseLock();
        lockService.reset();
        factory.setHasLock(false);
    }

    @Override
    public boolean hasLock() {
        return factory.hasLock();
    }

    @Override
    public boolean supportsForcedUnlock() {
        // Implementation based on "SELECT FOR UPDATE" can't force unlock as it's locked by other transaction
        return false;
    }

    @Override
    public void destroyLockInfo() {
        lazyInit();

        try {
            this.lockService.destroy();
            dbConnection.commit();
            logger.debug("Destroyed lock table");
        } catch (DatabaseException | SQLException de) {
            logger.error("Failed to destroy lock table");
            safeRollbackConnection();
        }
    }

    @Override
    public void close() {
        safeCloseConnection();
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
            } catch (SQLException e) {
                logger.warn("Failed to close connection", e);
            }
        }
    }
}
