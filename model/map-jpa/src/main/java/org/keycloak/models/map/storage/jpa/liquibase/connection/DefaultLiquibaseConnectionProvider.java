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

package org.keycloak.models.map.storage.jpa.liquibase.connection;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.ui.LoggerUIService;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

/**
 * A {@link MapLiquibaseConnectionProvider} implementation for the map-jpa module. This provider registers the custom {@code Liquibase}
 * changes and data types that were developed to better support working with data stored as JSON in the database.
 * </p>
 * An instance of this provider can be obtained via {@link KeycloakSession#getProvider(Class)} as follows:
 * <pre>
 *     MapLiquibaseConnectionProvider liquibaseProvider = session.getProvider(MapLiquibaseConnectionProvider.class);
 * </pre>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class DefaultLiquibaseConnectionProvider implements MapLiquibaseConnectionProvider {

    private static final Logger logger = Logger.getLogger(DefaultLiquibaseConnectionProvider.class);

    @SuppressWarnings("unused")
    public DefaultLiquibaseConnectionProvider(final KeycloakSession session) {
    }

    @Override
    public void close() {
    }

    @Override
    public Liquibase getLiquibaseForCustomUpdate(final Connection connection, final String defaultSchema, final String changelogLocation,
                                                 final ClassLoader classloader, final String changelogTableName) throws LiquibaseException {

        String scopeId = enterLiquibaseScope();
        try {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnectionFromPool(connection));
            if (defaultSchema != null) {
                database.setDefaultSchemaName(defaultSchema);
            }
            ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(classloader);
            database.setDatabaseChangeLogTableName(changelogTableName);

            logger.debugf("Using changelog file %s and changelogTableName %s", changelogLocation, database.getDatabaseChangeLogTableName());

            return new Liquibase(changelogLocation, resourceAccessor, database) {
                @Override
                public void close() throws LiquibaseException {
                    super.close();
                    exitLiquibaseScope(scopeId);
                }
            };
        } catch (LiquibaseException | RuntimeException ex) {
            // When this trows an exception, close the scope here.
            // If it returns the Liquibase object, the scope will be closed once the Liquibase object is being closed.
            exitLiquibaseScope(scopeId);
            throw ex;
        }
    }

    private void exitLiquibaseScope(String scopeId) {
        try {
            Scope.exit(scopeId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to exist scope: " + e.getMessage(), e);
        }
    }

    private String enterLiquibaseScope() {
        String scopeId;
        final Map<String, Object> scopeValues = new HashMap<>();
        // Setting the LoggerUIService here prevents Liquibase from logging each change set to the console using java.util.Logging in the Quarkus setup
        scopeValues.put(Scope.Attr.ui.name(), new LoggerUIService());
        try {
            scopeId = Scope.enter(scopeValues);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Liquibase: " + e.getMessage(), e);
        }
        return scopeId;
    }
}
