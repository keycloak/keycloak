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

import java.util.ArrayList;
import java.util.List;

import liquibase.database.Database;
import liquibase.database.core.PostgresDatabase;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.DatabaseDataType;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.core.AddColumnGenerator;
import liquibase.statement.core.AddColumnStatement;

/**
 * A {@link SqlGenerator} implementation that supports {@link GeneratedColumnStatement}s. It generates the SQL required
 * to add a column whose values are generated from a property of a JSON file stored in one of the table columns.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class GeneratedColumnSqlGenerator extends AddColumnGenerator {

    /**
     * Override the priority. This is needed because {@link GeneratedColumnStatement} is a subtype of {@link AddColumnStatement}
     * and is thus a match for the standard column generators. By increasing the priority we ensure this is processed before
     * the other generators.
     *
     * @return this generator's priority.
     */
    @Override
    public int getPriority() {
        return SqlGenerator.PRIORITY_DEFAULT + 1;
    }

    /**
     * Implement {@link #supports(AddColumnStatement, Database)} to return {@code true} only if the statement type is an instance
     * of {@link GeneratedColumnStatement}.
     * </p>
     * This is needed because this generator is a sub-class of {@link AddColumnGenerator} and is thus registered as being
     * able to handle statements of type {@link AddColumnStatement}. Due to the increased priority, this generator ends up
     * being selected to handle standard {@code addColumn} changes, which is not desirable. By returning {@code true} only
     * when the statement is a {@link GeneratedColumnStatement} we ensure this implementation is selected only when a generated
     * column is being added, allowing liquibase to continue iterating through the chain of generators in order to select the
     * right generator to handle the standard {@code addColumn} changes.
     *
     * @param statement the {@link liquibase.statement.SqlStatement} to be processed.
     * @param database a reference to the database.
     * @return {@code true} if an only if the statement is a {@link GeneratedColumnStatement}; {@code false} otherwise.
     */
    @Override
    public boolean supports(AddColumnStatement statement, Database database) {
        // use this implementation for generated columns only.
        return statement instanceof GeneratedColumnStatement;
    }

    @Override
    protected Sql[] generateSingleColumn(final AddColumnStatement statement, final Database database) {

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(super.generateSingleColumBaseSQL(statement, database));
        sqlBuilder.append(super.generateSingleColumnSQL(statement, database));
        this.handleGeneratedColumn((GeneratedColumnStatement) statement, database, sqlBuilder);

        List<Sql> returnSql = new ArrayList<>();
        returnSql.add(new UnparsedSql(sqlBuilder.toString(), super.getAffectedColumn(statement)));

        super.addUniqueConstraintStatements(statement, database, returnSql);
        super.addForeignKeyStatements(statement, database, returnSql);

        return returnSql.toArray(new Sql[0]);
    }

    protected void handleGeneratedColumn(final GeneratedColumnStatement statement, final Database database, final StringBuilder sqlBuilder) {
        if (database instanceof PostgresDatabase) {
            // assemble the GENERATED ALWAYS AS section of the query using the json property selection function.
            DatabaseDataType columnType = DataTypeFactory.getInstance().fromDescription(statement.getColumnType(), database).toDatabaseDataType(database);
            sqlBuilder.append(" GENERATED ALWAYS AS ((").append(statement.getJsonColumn()).append("->>'").append(statement.getJsonProperty())
                    .append("')::").append(columnType).append(") stored");
        }
    }
}
