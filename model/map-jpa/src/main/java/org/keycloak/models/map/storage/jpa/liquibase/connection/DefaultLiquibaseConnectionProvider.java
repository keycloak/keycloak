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
import java.util.concurrent.atomic.AtomicBoolean;

import liquibase.Liquibase;
import liquibase.change.ChangeFactory;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.jpa.liquibase.extension.GeneratedColumnSqlGenerator;
import org.keycloak.models.map.storage.jpa.liquibase.extension.CreateJsonIndexChange;
import org.keycloak.models.map.storage.jpa.liquibase.extension.CreateJsonIndexGenerator;
import org.keycloak.models.map.storage.jpa.liquibase.extension.GeneratedColumnChange;
import org.keycloak.models.map.storage.jpa.liquibase.extension.JsonDataType;

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

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public DefaultLiquibaseConnectionProvider(final KeycloakSession session) {
        if (! INITIALIZED.get()) {
            // TODO: all liquibase providers should probably synchronize on the same object.
            synchronized (INITIALIZED) {
                if (! INITIALIZED.get()) {
                    initializeLiquibase();
                    INITIALIZED.set(true);
                }
            }
        }
    }

    /**
     * Registers the custom changes/types so we can work with data stored in JSON format.
     */
    protected void initializeLiquibase() {

        // Add custom JSON data type
        DataTypeFactory.getInstance().register(JsonDataType.class);

        // Add custom change to generate columns from properties in JSON files stored in the DB.
        ChangeFactory.getInstance().register(GeneratedColumnChange.class);
        SqlGeneratorFactory.getInstance().register(new GeneratedColumnSqlGenerator());

        // Add custom change to create indexes for properties in JSON files stored in the DB.
        ChangeFactory.getInstance().register(CreateJsonIndexChange.class);
        SqlGeneratorFactory.getInstance().register(new CreateJsonIndexGenerator());
    }

    @Override
    public void close() {
    }

    @Override
    public Liquibase getLiquibaseForCustomUpdate(final Connection connection, final String defaultSchema, final String changelogLocation,
                                                 final ClassLoader classloader, final String changelogTableName) throws LiquibaseException {

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
