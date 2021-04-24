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

package org.keycloak.connections.liquibase;

import java.lang.reflect.Method;
import java.sql.Connection;

import javax.xml.parsers.SAXParserFactory;

import liquibase.database.core.MariaDBDatabase;
import liquibase.database.core.MySQLDatabase;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.MySQL8VarcharType;
import org.keycloak.connections.jpa.updater.liquibase.PostgresPlusDatabase;
import org.keycloak.connections.jpa.updater.liquibase.UpdatedMariaDBDatabase;
import org.keycloak.connections.jpa.updater.liquibase.UpdatedMySqlDatabase;
import org.keycloak.connections.jpa.updater.liquibase.conn.CustomChangeLogHistoryService;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProvider;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import liquibase.Liquibase;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.servicelocator.ServiceLocator;

public class QuarkusLiquibaseConnectionProvider implements LiquibaseConnectionProviderFactory, LiquibaseConnectionProvider {

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
        FastServiceLocator locator = (FastServiceLocator) ServiceLocator.getInstance();

        JpaConnectionProviderFactory jpaConnectionProvider = (JpaConnectionProviderFactory) session
                .getKeycloakSessionFactory().getProviderFactory(JpaConnectionProvider.class);

        // register our custom databases
        locator.register(new PostgresPlusDatabase());
        locator.register(new UpdatedMySqlDatabase());
        locator.register(new UpdatedMariaDBDatabase());
        
        // registers only the database we are using
        try (Connection connection = jpaConnectionProvider.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            if (database.getDatabaseProductName().equals(MySQLDatabase.PRODUCT_NAME)) {
                // Adding CustomVarcharType for MySQL 8 and newer
                DataTypeFactory.getInstance().register(MySQL8VarcharType.class);

                ChangeLogHistoryServiceFactory.getInstance().register(new CustomChangeLogHistoryService());
            } else if (database.getDatabaseProductName().equals(MariaDBDatabase.PRODUCT_NAME)) {
                // Adding CustomVarcharType for MySQL 8 and newer
                DataTypeFactory.getInstance().register(MySQL8VarcharType.class);
            }

            DatabaseFactory.getInstance().clearRegistry();
            locator.register(database);
        } catch (Exception cause) {
            throw new RuntimeException("Failed to configure Liquibase database", cause);
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
}
