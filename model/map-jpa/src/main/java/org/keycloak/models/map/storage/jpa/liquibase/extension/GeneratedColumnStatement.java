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

import java.util.List;

import liquibase.statement.ColumnConstraint;
import liquibase.statement.core.AddColumnStatement;

/**
 * A {@link liquibase.statement.SqlStatement} that extends the standard {@link AddColumnStatement} to include properties
 * to identify the JSON column and JSON property that are to be used to generated the values for the column being added.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class GeneratedColumnStatement extends AddColumnStatement {

    private String jsonColumn;
    private String jsonProperty;

    public GeneratedColumnStatement(final AddColumnStatement statement, final String jsonColumn, final String jsonProperty) {
        super(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName(), statement.getColumnName(),
                statement.getColumnType(), statement.getDefaultValue(), statement.getRemarks(),
                statement.getConstraints().toArray(new ColumnConstraint[0]));
        this.jsonColumn = jsonColumn;
        this.jsonProperty = jsonProperty;
    }

    public GeneratedColumnStatement(final List<GeneratedColumnStatement> statements) {
        super(statements.toArray(new GeneratedColumnStatement[0]));
    }

    /**
     * Obtains the name of the column that holds JSON files.
     *
     * @return the name of the JSON column.
     */
    public String getJsonColumn() {
        return this.jsonColumn;
    }

    /**
     * Obtains the name of the property in the JSON file whose value is to be used as the generated value for the new column.
     *
     * @return the name of the JSON property.
     */
    public String getJsonProperty() {
        return this.jsonProperty;
    }
}
