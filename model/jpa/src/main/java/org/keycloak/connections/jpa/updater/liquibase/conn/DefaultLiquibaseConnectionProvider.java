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

package org.keycloak.connections.jpa.updater.liquibase.conn;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.LiquibaseException;
import liquibase.logging.LogService;
import liquibase.logging.LogType;
import liquibase.logging.LoggerContext;
import liquibase.logging.core.AbstractLogger;
import liquibase.logging.core.AbstractLoggerFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.servicelocator.ServiceLocator;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.keycloak.Config;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.liquibase.MSSQLTextType;
import org.keycloak.connections.jpa.updater.liquibase.PostgresPlusDatabase;
import org.keycloak.connections.jpa.updater.liquibase.MySQL8VarcharType;
import org.keycloak.connections.jpa.updater.liquibase.UpdatedMariaDBDatabase;
import org.keycloak.connections.jpa.updater.liquibase.UpdatedMySqlDatabase;
import org.keycloak.connections.jpa.updater.liquibase.lock.CustomInsertLockRecordGenerator;
import org.keycloak.connections.jpa.updater.liquibase.lock.CustomLockDatabaseChangeLogGenerator;
import org.keycloak.connections.jpa.updater.liquibase.lock.DummyLockService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.sql.Connection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultLiquibaseConnectionProvider implements LiquibaseConnectionProviderFactory, LiquibaseConnectionProvider {

    private static final Logger logger = Logger.getLogger(DefaultLiquibaseConnectionProvider.class);

    private volatile boolean initialized = false;
    
    @Override
    public LiquibaseConnectionProvider create(KeycloakSession session) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    baseLiquibaseInitialization();
                    initialized = true;
                }
            }
        }
        return this;
    }

    protected void baseLiquibaseInitialization() {
        ServiceLocator sl = ServiceLocator.getInstance();
        sl.setResourceAccessor(new ClassLoaderResourceAccessor(getClass().getClassLoader()));

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

            String lockPackageName = DummyLockService.class.getPackage().getName();
            logger.debugf("Added package %s to liquibase", lockPackageName);
            sl.addPackageToScan(lockPackageName);
        }

        LogService.setLoggerFactory(new WrapperLoggerFactory());

        // Adding PostgresPlus support to liquibase
        DatabaseFactory.getInstance().register(new PostgresPlusDatabase());
        // Adding newer version of MySQL/MariaDB support to liquibase
        DatabaseFactory.getInstance().register(new UpdatedMySqlDatabase());
        DatabaseFactory.getInstance().register(new UpdatedMariaDBDatabase());

        // Adding CustomVarcharType for MySQL 8 and newer
        DataTypeFactory.getInstance().register(MySQL8VarcharType.class);

        // Adding custom text type for MSSQL
        DataTypeFactory.getInstance().register(MSSQLTextType.class);

        // Change command for creating lock and drop DELETE lock record from it
        SqlGeneratorFactory.getInstance().register(new CustomInsertLockRecordGenerator());

        // Use "SELECT FOR UPDATE" for locking database
        SqlGeneratorFactory.getInstance().register(new CustomLockDatabaseChangeLogGenerator());
    }


    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public Liquibase getLiquibase(Connection connection, String defaultSchema) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        if (defaultSchema != null) {
            database.setDefaultSchemaName(defaultSchema);
        }

        String changelog = LiquibaseJpaUpdaterProvider.CHANGELOG;
        ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(getClass().getClassLoader());

        logger.debugf("Using changelog file %s and changelogTableName %s", changelog, database.getDatabaseChangeLogTableName());
        
        return new Liquibase(changelog, resourceAccessor, database);
    }

    @Override
    public Liquibase getLiquibaseForCustomUpdate(Connection connection, String defaultSchema, String changelogLocation, ClassLoader classloader, String changelogTableName) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        if (defaultSchema != null) {
            database.setDefaultSchemaName(defaultSchema);
        }

        ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(classloader);
        database.setDatabaseChangeLogTableName(changelogTableName);

        logger.debugf("Using changelog file %s and changelogTableName %s", changelogLocation, database.getDatabaseChangeLogTableName());

        return new Liquibase(changelogLocation, resourceAccessor, database);
    }

    private class LogWrapper extends AbstractLogger {

        @Override
        public void severe(LogType target, String message) {
            logger.error(message);
        }

        @Override
        public void severe(LogType target, String message, Throwable e) {
            logger.error(message, e);
        }

        @Override
        public void warning(LogType target, String message) {
            logger.warn(message);
        }

        @Override
        public void warning(LogType target, String message, Throwable e) {
            logger.warn(message, e);
        }

        @Override
        public void info(LogType logType, String message) {
            logger.debug(message);
        }

        @Override
        public void info(LogType target, String message, Throwable e) {
            logger.debug(message, e);
        }

        @Override
        public void debug(LogType target, String message) {
            logger.trace(message);
        }

        @Override
        public void debug(LogType target, String message, Throwable e) {
            logger.trace(message, e);
        }
    }

    private class WrapperLoggerFactory extends AbstractLoggerFactory {

        @Override
        public liquibase.logging.Logger getLog(Class clazz) {
            return createLoggerImpl();
        }

        @Override
        public LoggerContext pushContext(String key, Object object) {
            return new WrapperLoggerContext(key, object);
        }

        protected liquibase.logging.Logger createLoggerImpl() {
            return new LogWrapper();
        }

        @Override
        public void close() {

        }
    }

    private class WrapperLoggerContext implements LoggerContext {

        private final String key;

        public WrapperLoggerContext(String key, Object value) {
            MDC.put(key, String.valueOf(value));
            this.key = key;
        }

        @Override
        public void showMoreProgress() {}

        @Override
        public void showMoreProgress(int percentComplete) {}

        @Override
        public void close() {
            MDC.remove(key);
        }
    }
}
