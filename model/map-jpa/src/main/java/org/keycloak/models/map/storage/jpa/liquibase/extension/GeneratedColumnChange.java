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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import liquibase.change.AbstractChange;
import liquibase.change.AddColumnConfig;
import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.change.ChangeStatus;
import liquibase.change.ChangeWithColumns;
import liquibase.change.ColumnConfig;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.change.core.AddColumnChange;
import liquibase.database.Database;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.ValidationErrors;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.AddColumnStatement;

/**
 * Extension used to a column whose values are generated from a property of a JSON file stored in one of the table's columns.
 * <p/>
 * Example configuration in the changelog:
 * <pre>
 *     &lt;databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *                    xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
 *                    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
 *                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd
 *                    http://www.liquibase.org/xml/ns/dbchangelog-ext
 *                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd"&gt;
 *
 *     &lt;changeSet author="keycloak" id="some_id"&gt;
 *         ...
 *         &lt;ext:addGeneratedColumn tableName="test"&gt;
 *             &lt;ext:column name="new_column" type="VARCHAR(36)" jsonColumn="metadata" jsonProperty="alias"/&gt;
 *         &lt;/ext:addGeneratedColumn&gt;
 *     &lt;/changeSet&gt;
 * </pre>
 * The above configuration is adding a new column, named {@code new_column}, whose values are generated from the {@code alias} property
 * of the JSON file stored in column {@code metadata}. If, for example, a particular entry in the table contains the JSON
 * {@code {"name":"duke","alias":"jduke"}} in column {@code metadata}, the value generated for the new column will be {@code jduke}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@DatabaseChange(name = "addGeneratedColumn", description = "Adds new generated columns to a table. The columns must reference a JSON property inside an existing JSON column.",
        priority = ChangeMetaData.PRIORITY_DEFAULT, appliesTo = "table")
public class GeneratedColumnChange extends AbstractChange implements ChangeWithColumns<JsonEnabledColumnConfig> {

    private final ExtendedAddColumnChange delegate;
    private Map<String, JsonEnabledColumnConfig> configMap = new HashMap<>();

    public GeneratedColumnChange() {
        this.delegate = new ExtendedAddColumnChange();
    }

    @DatabaseChangeProperty(mustEqualExisting ="relation.catalog")
    public String getCatalogName() {
        return this.delegate.getCatalogName();
    }

    public void setCatalogName(final String catalogName) {
        this.delegate.setCatalogName(catalogName);
    }

    @DatabaseChangeProperty(mustEqualExisting ="relation.schema")
    public String getSchemaName() {
        return this.delegate.getSchemaName();
    }

    public void setSchemaName(final String schemaName) {
        this.delegate.setSchemaName(schemaName);
    }

    @DatabaseChangeProperty(mustEqualExisting ="table", description = "Name of the table to add the generated column to")
    public String getTableName() {
        return this.delegate.getTableName();
    }

    public void setTableName(final String tableName) {
        this.delegate.setTableName(tableName);
    }

    @Override
    public void addColumn(final JsonEnabledColumnConfig column) {
        this.delegate.addColumn(column);
        this.configMap.put(column.getName(), column);
    }

    @Override
    @DatabaseChangeProperty(description = "Generated columns information", requiredForDatabase = "all")
    public List<JsonEnabledColumnConfig> getColumns() {
        return this.delegate.getColumns().stream().map(JsonEnabledColumnConfig.class::cast).collect(Collectors.toList());
    }

    @Override
    public void setColumns(final List<JsonEnabledColumnConfig> columns) {
        columns.forEach(this.delegate::addColumn);
        this.configMap = this.getColumns().stream()
                .collect(Collectors.toMap(ColumnConfig::getName, Function.identity()));
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        if (database instanceof PostgresDatabase) {
            for (AddColumnConfig config : delegate.getColumns()) {
                String columnType = config.getType();
                // if postgres, change JSON type to JSONB before generating the statements as JSONB is more efficient.
                if (columnType.equalsIgnoreCase("JSON")) {
                    config.setType("JSONB");
                }
            }
        }

        // AddColumnChange always produces an AddColumnStatement in the first position of the returned array.
        AddColumnStatement delegateStatement = (AddColumnStatement) Arrays.stream(this.delegate.generateStatements(database))
                .findFirst().get();

        // convert the regular AddColumnStatements into GeneratedColumnStatements, adding the extension properties.
        if (!delegateStatement.isMultiple()) {
            // single statement - convert it directly.
            JsonEnabledColumnConfig config = configMap.get(delegateStatement.getColumnName());
            if (config != null) {
                return new SqlStatement[] {new GeneratedColumnStatement(delegateStatement, config.getJsonColumn(),
                        config.getJsonProperty())};
            }
        }
        else {
            // multiple statement - convert all sub-statements.
            List<GeneratedColumnStatement> generatedColumnStatements = delegateStatement.getColumns().stream()
                    .filter(c -> configMap.containsKey(c.getColumnName()))
                    .map(c -> new GeneratedColumnStatement(c, configMap.get(c.getColumnName()).getJsonColumn(),
                            configMap.get(c.getColumnName()).getJsonProperty()))
                    .collect(Collectors.toList());

            // add all GeneratedColumnStatements into a composite statement and return the composite.
            return new SqlStatement[]{new GeneratedColumnStatement(generatedColumnStatements)};
        }
        return new SqlStatement[0];
    }

    @Override
    protected Change[] createInverses() {
        return this.delegate.createInverses();
    }

    @Override
    public ChangeStatus checkStatus(Database database) {
        return delegate.checkStatus(database);
    }

    @Override
    public String getConfirmationMessage() {
        return delegate.getConfirmationMessage();
    }

    @Override
    public String getSerializedObjectNamespace() {
        return GENERIC_CHANGELOG_EXTENSION_NAMESPACE;
    }

    @Override
    public ValidationErrors validate(Database database) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.checkRequiredField("columns", this.delegate.getColumns());
        // validate each generated column.
        this.delegate.getColumns().stream().map(JsonEnabledColumnConfig.class::cast).forEach(
                config -> {
                    if (config.isAutoIncrement() != null && config.isAutoIncrement()) {
                        validationErrors.addError("Generated column " + config.getName() + " cannot be auto-incremented");
                    } else if (config.getValueObject() != null) {
                        validationErrors.addError("Generated column " + config.getName() + " cannot be configured with a value");
                    } else if (config.getDefaultValueObject() != null) {
                        validationErrors.addError("Generated column " + config.getName() + " cannot be configured with a default value");
                    }
                    // we can expand this check if we decide to allow other types of generated columns in the future - for now
                    // ensure the column can be properly generated from a json property stored on a json column.
                    validationErrors.checkRequiredField("jsonColumn", config.getJsonColumn());
                    validationErrors.checkRequiredField("jsonProperty", config.getJsonProperty());
                });
        validationErrors.addAll(super.validate(database));
        return validationErrors;
    }

    /**
     * Simple extension that makes protected methods public so they can be accessed as a delegate.
     */
    private static class ExtendedAddColumnChange extends AddColumnChange {
        @Override
        public Change[] createInverses() {
            return super.createInverses();
        }
    }
}
