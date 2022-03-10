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
import liquibase.Scope;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.ui.LoggerUIService;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultLiquibaseConnectionProvider implements LiquibaseConnectionProviderFactory, LiquibaseConnectionProvider {

    private static final Logger logger = Logger.getLogger(DefaultLiquibaseConnectionProvider.class);

    public static final String INDEX_CREATION_THRESHOLD_PARAM = "keycloak.indexCreationThreshold";

    private int indexCreationThreshold;

    private static final AtomicBoolean INITIALIZATION = new AtomicBoolean(false);
    
    @Override
    public LiquibaseConnectionProvider create(KeycloakSession session) {
        if (! INITIALIZATION.get()) {
            // We need critical section synchronized on some static final field, otherwise
            // e.g. several Undertows or parallel model tests could attempt initializing Liquibase
            // in the same JVM at the same time which leads to concurrency failures
            synchronized (INITIALIZATION) {
                if (! INITIALIZATION.get()) {
                    baseLiquibaseInitialization();
                    INITIALIZATION.set(true);
                }
            }
        }
        return this;
    }

    protected void baseLiquibaseInitialization() {
        // we need to initialize the scope using the right classloader, or else Liquibase won't be able to locate the extensions.
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            Scope.getCurrentScope();
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }

        // using the initialized scope, create a child scope that sets the classloader and resource accessor so that any attempt
        // by Liquibase to load a class (e.g. custom change) using the scope's classloader uses the correct classloader.
        final Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(Scope.Attr.resourceAccessor.name(), new ClassLoaderResourceAccessor(this.getClass().getClassLoader()));
        scopeValues.put(Scope.Attr.classLoader.name(), this.getClass().getClassLoader());
        scopeValues.put(Scope.Attr.ui.name(), new LoggerUIService());
        try {
            Scope.enter(scopeValues);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Liquibase: " + e.getMessage(), e);
        }
    }

    @Override
    public void init(Config.Scope config) {
        indexCreationThreshold = config.getInt("indexCreationThreshold", 300000);
        logger.debugf("indexCreationThreshold is %d", indexCreationThreshold);
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

        ((AbstractJdbcDatabase) database).set(INDEX_CREATION_THRESHOLD_PARAM, indexCreationThreshold);
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

}
