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

package org.keycloak.connections.jpa.updater.liquibase;

import liquibase.database.DatabaseConnection;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.statement.core.RawSqlStatement;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PostgresPlusDatabase extends PostgresDatabase {

    public static final String POSTGRESPLUS_PRODUCT_NAME = "EnterpriseDB";

    @Override
    public String getShortName() {
        return "postgresplus";
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return POSTGRESPLUS_PRODUCT_NAME;
    }

    @Override
    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return POSTGRESPLUS_PRODUCT_NAME.equalsIgnoreCase(conn.getDatabaseProductName());
    }

    @Override
    public String getDefaultDriver(String url) {
        String defaultDriver = super.getDefaultDriver(url);

        if (defaultDriver == null) {
            if (url.startsWith("jdbc:edb:")) {
                defaultDriver = "com.edb.Driver";
            }
        }

        return defaultDriver;
    }

    @Override
    protected String getConnectionSchemaName() {
        try {
            String currentSchema = ExecutorService.getInstance().getExecutor(this)
                    .queryForObject(new RawSqlStatement("select current_schema"), String.class);
            return currentSchema;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get current schema", e);
        }
    }

}
