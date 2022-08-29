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

package org.keycloak.models.map.storage.jpa.liquibase.extension;

import java.util.Arrays;
import java.util.stream.Collectors;

import liquibase.database.Database;
import liquibase.database.core.CockroachDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.core.CreateIndexStatement;
import liquibase.structure.core.Index;
import liquibase.structure.core.Table;
import liquibase.util.StringUtil;

/**
 * A {@link SqlGenerator} implementation that supports {@link CreateJsonIndexStatement}s. It generates the SQL required
 * to create an index for properties of JSON files stored in one of the table columns.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CreateJsonIndexGenerator extends AbstractSqlGenerator<CreateJsonIndexStatement> {

    /**
     * Override the priority. This is needed because {@link CreateJsonIndexStatement} is a subtype of {@link CreateIndexStatement}
     * and is thus a match for the standard index generators. By increasing the priority we ensure this is processed before
     * the other generators.
     *
     * @return this generator's priority.
     */
    @Override
    public int getPriority() {
        return SqlGenerator.PRIORITY_DATABASE + 1;
    }

    @Override
    public ValidationErrors validate(CreateJsonIndexStatement createIndexStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.checkRequiredField("tableName", createIndexStatement.getTableName());
        validationErrors.checkRequiredField("columns", createIndexStatement.getColumns());
        Arrays.stream(createIndexStatement.getColumns()).map(JsonEnabledColumnConfig.class::cast)
                .forEach(config -> {
                    validationErrors.checkRequiredField("jsonColumn", config.getJsonColumn());
                });
        return validationErrors;
    }

    @Override
    public Sql[] generateSql(CreateJsonIndexStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {

        if (!(database instanceof PostgresDatabase)) {
            // for now return an empty SQL for DBs that don't support JSON indexes natively.
            return new Sql[0];
        }

        StringBuilder builder = new StringBuilder();
        builder.append("CREATE ");
        if (statement.isUnique() != null && statement.isUnique()) {
            builder.append("UNIQUE ");
        }
        builder.append("INDEX ");

        if (statement.getIndexName() != null) {
            builder.append(database.escapeObjectName(statement.getIndexName(), Index.class)).append(" ");
        }

        builder.append("ON ").append(database.escapeTableName(statement.getTableCatalogName(), statement.getTableSchemaName(),
                statement.getTableName()));
        this.handleJsonIndex(statement, database, builder);
        if (StringUtil.trimToNull(statement.getTablespace()) != null && database.supportsTablespaces()) {
            builder.append(" TABLESPACE ").append(statement.getTablespace());
        }

        return new Sql[]{new UnparsedSql(builder.toString(), getAffectedIndex(statement))};
    }

    protected void handleJsonIndex(final CreateJsonIndexStatement statement, final Database database, final StringBuilder builder) {
        if (database instanceof CockroachDatabase) {
            builder.append(" USING gin (");
            builder.append(Arrays.stream(statement.getColumns()).map(JsonEnabledColumnConfig.class::cast)
                    .map(c -> c.getJsonProperty() == null ? c.getJsonColumn() :
                            "(" + c.getJsonColumn() + "->'" + c.getJsonProperty() + "')")
                    .collect(Collectors.joining(", ")))
                    .append(")");
        }
        else if (database instanceof PostgresDatabase) {
            builder.append(" USING gin (");
            builder.append(Arrays.stream(statement.getColumns()).map(JsonEnabledColumnConfig.class::cast)
                    .map(c -> c.getJsonProperty() == null ? c.getJsonColumn() :
                            "(" + c.getJsonColumn() + "->'" + c.getJsonProperty() + "') jsonb_path_ops")
                    .collect(Collectors.joining(", ")))
                    .append(")");
        }
    }

    protected Index getAffectedIndex(CreateIndexStatement statement) {
        return new Index().setName(statement.getIndexName()).setTable((Table) new Table().setName(statement.getTableName())
                .setSchema(statement.getTableCatalogName(), statement.getTableSchemaName()));
    }
}
