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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.models.dblock.DBLockProvider;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.core.InitializeDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.InsertStatement;

/**
 * We need to remove DELETE SQL command, which liquibase adds by default when inserting record to table lock. This is causing buggy behaviour
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CustomInsertLockRecordGenerator extends AbstractSqlGenerator<InitializeDatabaseChangeLogLockTableStatement> {

    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Ensure bigger priority than InitializeDatabaseChangeLogLockTableGenerator
    }

    @Override
    public ValidationErrors validate(InitializeDatabaseChangeLogLockTableStatement initializeDatabaseChangeLogLockTableStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        return new ValidationErrors();
    }

    @Override
    public Sql[] generateSql(InitializeDatabaseChangeLogLockTableStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        // get the IDs that are already in the database if migration
        Set<Integer> currentIds = new HashSet<>();
        if (statement instanceof CustomInitializeDatabaseChangeLogLockTableStatement) {
            currentIds = ((CustomInitializeDatabaseChangeLogLockTableStatement) statement).getCurrentIds();
        }

        // generate all the IDs that are currently missing in the lock table
        List<Sql> result = new ArrayList<>();
        for (DBLockProvider.Namespace lock : DBLockProvider.Namespace.values()) {
            if (!currentIds.contains(lock.getId())) {
                InsertStatement insertStatement = new InsertStatement(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName())
                        .addColumnValue("ID", lock.getId())
                        .addColumnValue("LOCKED", Boolean.FALSE);
                result.addAll(Arrays.asList(SqlGeneratorFactory.getInstance().generateSql(insertStatement, database)));
            }
        }

        return result.toArray(new Sql[result.size()]);
    }
}
