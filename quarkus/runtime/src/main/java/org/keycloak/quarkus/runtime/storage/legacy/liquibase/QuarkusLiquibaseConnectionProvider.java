/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.legacy.liquibase;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import liquibase.Scope;
import liquibase.ui.LoggerUIService;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProvider;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

public class QuarkusLiquibaseConnectionProvider implements LiquibaseConnectionProviderFactory, LiquibaseConnectionProvider,
        EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(QuarkusLiquibaseConnectionProvider.class);

    private volatile boolean initialized = false;
    private ClassLoaderResourceAccessor resourceAccessor;

    @Override
    public LiquibaseConnectionProvider create(KeycloakSession session) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    baseLiquibaseInitialization(session);
                    initialized = true;
                }
            }
        }
        return this;
    }

    protected void baseLiquibaseInitialization(KeycloakSession session) {
        resourceAccessor = new ClassLoaderResourceAccessor(getClass().getClassLoader());

        // initialize Liquibase using a custom scope
        final Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(Scope.Attr.ui.name(), new LoggerUIService());
        try {
            Scope scope = Scope.getCurrentScope();
            scope.enter(scopeValues);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Liquibase: " + e.getMessage(), e);
        }

        // disables XML validation
        for (ChangeLogParser parser : ChangeLogParserFactory.getInstance().getParsers()) {
            if (parser instanceof XMLChangeLogSAXParser) {
                Method getSaxParserFactory = null;
                try {
                    getSaxParserFactory = XMLChangeLogSAXParser.class.getDeclaredMethod("getSaxParserFactory");
                    getSaxParserFactory.setAccessible(true);
                    SAXParserFactory saxParserFactory = (SAXParserFactory) getSaxParserFactory.invoke(parser);
                    saxParserFactory.setValidating(false);
                    saxParserFactory.setSchema(null);
                } catch (Exception e) {
                    logger.warnf("Failed to disable liquibase XML validations");
                } finally {
                    if (getSaxParserFactory != null) {
                        getSaxParserFactory.setAccessible(false);
                    }
                }
            }
        }
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
        return "quarkus";
    }

    @Override
    public Liquibase getLiquibase(Connection connection, String defaultSchema) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        if (defaultSchema != null) {
            database.setDefaultSchemaName(defaultSchema);
        }

        String changelog = QuarkusJpaUpdaterProvider.CHANGELOG;

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

    @Override
    public int order() {
        return 100;
    }

    @Override
    public boolean isSupported() {
        return !Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }
}
