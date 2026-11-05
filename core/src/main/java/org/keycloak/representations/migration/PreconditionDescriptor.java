/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.migration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** One precondition node in the precondition AST as stored in the migration manifest. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreconditionDescriptor {

    private String type;
    private String tableName;
    private String columnName;
    private String indexName;
    private String foreignKeyName;
    private String primaryKeyName;
    private String constraintName;
    private String dbmsType;
    private String sql;
    private String expectedResult;
    private String changeSetId;
    private String changeSetAuthor;
    private String changeSetFile;
    private List<PreconditionDescriptor> nestedConditions;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getForeignKeyName() {
        return foreignKeyName;
    }

    public void setForeignKeyName(String foreignKeyName) {
        this.foreignKeyName = foreignKeyName;
    }

    public String getPrimaryKeyName() {
        return primaryKeyName;
    }

    public void setPrimaryKeyName(String primaryKeyName) {
        this.primaryKeyName = primaryKeyName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public String getDbmsType() {
        return dbmsType;
    }

    public void setDbmsType(String dbmsType) {
        this.dbmsType = dbmsType;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getChangeSetId() {
        return changeSetId;
    }

    public void setChangeSetId(String changeSetId) {
        this.changeSetId = changeSetId;
    }

    public String getChangeSetAuthor() {
        return changeSetAuthor;
    }

    public void setChangeSetAuthor(String changeSetAuthor) {
        this.changeSetAuthor = changeSetAuthor;
    }

    public String getChangeSetFile() {
        return changeSetFile;
    }

    public void setChangeSetFile(String changeSetFile) {
        this.changeSetFile = changeSetFile;
    }

    public List<PreconditionDescriptor> getNestedConditions() {
        return nestedConditions;
    }

    public void setNestedConditions(List<PreconditionDescriptor> nestedConditions) {
        this.nestedConditions = nestedConditions;
    }

    public void addNestedCondition(PreconditionDescriptor condition) {
        if (this.nestedConditions == null) {
            this.nestedConditions = new ArrayList<>();
        }
        this.nestedConditions.add(condition);
    }
}
