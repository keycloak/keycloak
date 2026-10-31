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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One execution step within a change-set as stored in the migration manifest.
 * Step types: {@code sql}, {@code customTask}, {@code createIndex}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeSetStep {

    private String type;
    private Map<String, List<String>> statements;
    private String className;
    private String tableName;
    private String indexName;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, List<String>> getStatements() {
        return statements;
    }

    public void setStatements(Map<String, List<String>> statements) {
        this.statements = statements;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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

    public static ChangeSetStep customTask(String className) {
        ChangeSetStep step = new ChangeSetStep();
        step.setType("customTask");
        step.setClassName(className);
        return step;
    }

    /** Convenience method to append a SQL statement for a dialect (creates the map/list lazily). */
    public void addStatement(String dialect, String sql) {
        if (statements == null) {
            statements = new LinkedHashMap<>();
        }
        statements.computeIfAbsent(dialect, k -> new ArrayList<>()).add(sql);
    }
}
