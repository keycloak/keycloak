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
package org.keycloak.connections.jpa.updater.liquibase.conn;

import liquibase.change.ColumnConfig;
import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.core.MySQLDatabase;
import liquibase.exception.DatabaseException;
import liquibase.executor.jvm.ChangelogJdbcMdcListener;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.core.AddPrimaryKeyStatement;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;

/**
 *
 * @author hmlnarik
 */
public class CustomChangeLogHistoryService extends StandardChangeLogHistoryService {

    private boolean serviceInitialized;

    @Override
    public boolean supports(Database database) {
        return database instanceof MySQLDatabase;
    }

    @Override
    public void init() throws DatabaseException {
        super.init();

        if (serviceInitialized) return;

        serviceInitialized = true;

        if (!existsDatabaseChangelogPK()) {
            createDatabaseChangelogPK();
        }
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Ensure bigger priority than StandardChangeLogHistoryService
    }

    private boolean existsDatabaseChangelogPK() throws DatabaseException {
        try {
            PrimaryKey example = new PrimaryKey();
            Table table = new Table();
            table.setSchema(new Schema(getLiquibaseCatalogName(), getLiquibaseSchemaName()));
            table.setName(getDatabaseChangeLogTableName());
            example.setTable(table);
            return SnapshotGeneratorFactory.getInstance().has(example, getDatabase());
        } catch (InvalidExampleException e) {
            throw new DatabaseException(e);
        }
    }

    private void createDatabaseChangelogPK() throws DatabaseException {
        AddPrimaryKeyStatement pkStatement = new AddPrimaryKeyStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(),
                ColumnConfig.arrayFromNames("ID, AUTHOR, FILENAME"), "PK_DATABASECHANGELOG");
        try {
            ChangelogJdbcMdcListener.execute(getDatabase(), ex -> ex.execute(pkStatement));
            getDatabase().commit();
        } catch (DatabaseException e) {
            // if PK already exists just ignore the exception
            if (!existsDatabaseChangelogPK()) {
                throw e;
            }
        }
    }
}
