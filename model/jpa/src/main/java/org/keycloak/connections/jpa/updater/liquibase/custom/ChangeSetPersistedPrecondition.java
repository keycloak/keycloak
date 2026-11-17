/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.liquibase.custom;

import org.keycloak.connections.jpa.updater.liquibase.LiquibaseConstants;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.exception.CustomPreconditionErrorException;
import liquibase.exception.CustomPreconditionFailedException;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.precondition.CustomPrecondition;
import liquibase.statement.core.RawParameterizedSqlStatement;

/**
 * Passes when the given changeset is recorded in the persisted changelog table (DATABASECHANGELOG).
 * <p>
 * Unlike {@code changeSetExecuted}, which reads Liquibase's in-memory history, this queries the table. When a script
 * is generated (migration-strategy=manual) Liquibase also adds changesets that are only written to the script to its
 * in-memory history, so {@code changeSetExecuted} cannot tell them apart from changesets that have really been applied
 * to the database. This precondition reflects what the database contains at the time the script is generated.
 * <p>
 * The table is resolved and escaped through the {@link Database}, so a changelog table in a schema that needs quoting
 * (for example {@code keycloak-t}) is handled.
 */
public class ChangeSetPersistedPrecondition implements CustomPrecondition {

    private String id;
    private String author;
    private String file;

    public void setId(String id) {
        this.id = id;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public void check(Database database) throws CustomPreconditionFailedException, CustomPreconditionErrorException {
        String table = database.escapeTableName(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(),
                database.getDatabaseChangeLogTableName());
        try {
            long count = Scope.getCurrentScope().getSingleton(ExecutorService.class)
                    .getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database)
                    .queryForLong(new RawParameterizedSqlStatement(
                            "SELECT COUNT(*) FROM " + table + " WHERE ID = ? AND AUTHOR = ? AND FILENAME = ?", id, author, file));
            if (count == 0) {
                throw new CustomPreconditionFailedException(
                        "Changeset " + file + "::" + id + "::" + author + " is not recorded in " + table);
            }
        } catch (DatabaseException e) {
            throw new CustomPreconditionErrorException("Could not query " + table, e);
        }
    }
}
