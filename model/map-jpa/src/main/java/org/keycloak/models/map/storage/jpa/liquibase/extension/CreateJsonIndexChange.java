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
import java.util.stream.Collectors;

import liquibase.change.AbstractChange;
import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.change.ChangeStatus;
import liquibase.change.ChangeWithColumns;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.change.core.CreateIndexChange;
import liquibase.change.core.DropIndexChange;
import liquibase.database.Database;
import liquibase.statement.SqlStatement;

/**
 * Extension used to create an index for properties of JSON files stored in the database. Some databases, like {@code Postgres},
 * have native support for these indexes while other databases may require different constructs to achieve this (like creation
 * of a separate column based on the JSON property and subsequent indexing of that column).
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
 *         &lt;ext:createJsonIndex tableName="test" indexName="some_index_name"&gt;
 *             &lt;ext:column jsonColumn="metadata" jsonProperty="name"/&gt;
 *         &lt;/ext:createJsonIndex&gt;
 *     &lt;/changeSet&gt;
 * </pre>
 * The above configuration is creating an inverted (GIN) index for the {@code name} property of JSON files stored in column
 * {@code metadata} in table {@code test}.
 * <p/>
 * The {@code jsonProperty} is optional - when it is absent the index will be created for the whole JSON.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@DatabaseChange(name="createJsonIndex", description = "Creates an index for one or more JSON properties",
        priority = ChangeMetaData.PRIORITY_DEFAULT, appliesTo = "index")
public class CreateJsonIndexChange extends AbstractChange implements ChangeWithColumns<JsonEnabledColumnConfig> {

    private final CreateIndexChange delegate;

    public CreateJsonIndexChange() {
        this.delegate = new CreateIndexChange();
    }

    @DatabaseChangeProperty
    public String getCatalogName() {
        return delegate.getCatalogName();
    }

    public void setCatalogName(String catalogName) {
        this.delegate.setCatalogName(catalogName);
    }

    @DatabaseChangeProperty(mustEqualExisting ="index.schema")
    public String getSchemaName() {
        return delegate.getSchemaName();
    }

    public void setSchemaName(String schemaName) {
        this.delegate.setSchemaName(schemaName);
    }

    @DatabaseChangeProperty(mustEqualExisting = "index.table", description = "Name of the table to add the index to", exampleValue = "person")
    public String getTableName() {
        return this.delegate.getTableName();
    }

    public void setTableName(String tableName) {
        this.delegate.setTableName(tableName);
    }

    @DatabaseChangeProperty(mustEqualExisting = "index", description = "Name of the index to create")
    public String getIndexName() {
        return this.delegate.getIndexName();
    }

    public void setIndexName(String indexName) {
        this.delegate.setIndexName(indexName);
    }

    @DatabaseChangeProperty(description = "Tablepace to create the index in.")
    public String getTablespace() {
        return this.delegate.getTablespace();
    }

    public void setTablespace(String tablespace) {
        this.delegate.setTablespace(tablespace);
    }

    @DatabaseChangeProperty(description = "Unique values index", since = "1.8")
    public Boolean isUnique() {
        return this.delegate.isUnique();
    }

    public void setUnique(Boolean isUnique) {
        this.delegate.setUnique(isUnique);
    }

    @DatabaseChangeProperty(isChangeProperty = false)
    public String getAssociatedWith() {
        return delegate.getAssociatedWith();
    }

    public void setAssociatedWith(String associatedWith) {
        this.delegate.setAssociatedWith(associatedWith);
    }

    @DatabaseChangeProperty
    public Boolean getClustered() {
        return this.delegate.getClustered();
    }

    public void setClustered(Boolean clustered) {
        this.delegate.setClustered(clustered);
    }

    @Override
    public void addColumn(JsonEnabledColumnConfig column) {
        delegate.addColumn(column);
    }

    @Override
    @DatabaseChangeProperty(mustEqualExisting = "index.column", description = "Column(s) to add to the index", requiredForDatabase = "all")
    public List<JsonEnabledColumnConfig> getColumns() {
        return this.delegate.getColumns().stream().map(JsonEnabledColumnConfig.class::cast).collect(Collectors.toList());
    }

    @Override
    public void setColumns(List<JsonEnabledColumnConfig> columns) {
        columns.forEach(this.delegate::addColumn);
    }

    @Override
    public String getConfirmationMessage() {
        return delegate.getConfirmationMessage();
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        return new SqlStatement[]{new CreateJsonIndexStatement(this.getCatalogName(), this.getSchemaName(), this.getTableName(),
                this.getIndexName(), this.isUnique(), this.getAssociatedWith(), this.getTablespace(), this.getClustered(),
                this.getColumns().toArray(new JsonEnabledColumnConfig[0]))};
    }

    @Override
    protected Change[] createInverses() {
        DropIndexChange inverse = new DropIndexChange();
        inverse.setSchemaName(getSchemaName());
        inverse.setTableName(getTableName());
        inverse.setIndexName(getIndexName());
        return new Change[]{inverse};
    }

    @Override
    public ChangeStatus checkStatus(Database database) {
        return delegate.checkStatus(database);
    }

    @Override
    public String getSerializedObjectNamespace() {
        return GENERIC_CHANGELOG_EXTENSION_NAMESPACE;
    }

    @Override
    public Object getSerializableFieldValue(String field) {
        return delegate.getSerializableFieldValue(field);
    }
}
