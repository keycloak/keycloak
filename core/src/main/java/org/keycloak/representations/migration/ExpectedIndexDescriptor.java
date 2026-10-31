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
package org.keycloak.representations.migration;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * An index that must exist once the full master changelog has been applied for a given dialect.
 * Used by the database index checker to detect indexes that were skipped (for example by the
 * index-creation threshold) or lost.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpectedIndexDescriptor {

    private String tableName;
    private String indexName;
    private String sql;

    public ExpectedIndexDescriptor() {
    }

    public ExpectedIndexDescriptor(String tableName, String indexName, String sql) {
        this.tableName = tableName;
        this.indexName = indexName;
        this.sql = sql;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /** The {@code CREATE INDEX} statement(s) for the dialect, joined with {@code "; "}. */
    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
