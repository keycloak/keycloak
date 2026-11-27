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

package org.keycloak.connections.jpa.updater.liquibase;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import liquibase.database.Database;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.CreateIndexGeneratorPostgres;
import liquibase.statement.core.CreateIndexStatement;
import liquibase.structure.DatabaseObject;

/**
 * Creates indexes on YugabyteDB using {@code CREATE INDEX NONCONCURRENTLY}.
 * <p>
 * By default, YugabyteDB builds an index online, which waits for all transactions that were open when the build started.
 * Keycloak holds its database lock in a transaction for the whole migration, so an online build would wait for the migration to finish, and never complete.
 * Inside a transaction block, YugabyteDB falls back to a non-concurrent build, but {@link YugabyteDatabase} runs Liquibase in auto-commit mode.
 * That is necessary because YugabyteDB bumps the table or catalog version on every DDL statement, and a session with an older cached version fails on its next statement in the same transaction with "schema version mismatch".
 * In auto-commit mode, each statement starts with a fresh version, which avoids this error for changesets that mix DDL with queries or inserts.
 */
public class YugabyteCreateIndexGenerator extends CreateIndexGeneratorPostgres {

    private static final Pattern CREATE_INDEX = Pattern.compile("^CREATE (UNIQUE )?INDEX ", Pattern.CASE_INSENSITIVE);

    @Override
    public int getPriority() {
        return super.getPriority() + 1;
    }

    @Override
    public boolean supports(CreateIndexStatement statement, Database database) {
        return database instanceof YugabyteDatabase && super.supports(statement, database);
    }

    @Override
    public Sql[] generateSql(CreateIndexStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        return Arrays.stream(super.generateSql(statement, database, sqlGeneratorChain))
                .map(sql -> {
                    Matcher matcher = CREATE_INDEX.matcher(sql.toSql());
                    if (!matcher.find()) {
                        return sql;
                    }
                    String rewritten = "CREATE " + (matcher.group(1) != null ? "UNIQUE " : "") + "INDEX NONCONCURRENTLY " + sql.toSql().substring(matcher.end());
                    return (Sql) new UnparsedSql(rewritten, sql.getEndDelimiter(), sql.getAffectedDatabaseObjects().toArray(new DatabaseObject[0]));
                })
                .toArray(Sql[]::new);
    }
}
