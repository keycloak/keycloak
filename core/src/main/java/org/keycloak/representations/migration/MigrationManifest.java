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

/** The top-level migration manifest loaded from {@code META-INF/schema/migration-manifest.json}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrationManifest {

    private int version = 1;
    private String generatedAt;
    private String liquibaseVersion;
    private List<ChangeSetDescriptor> changeSets = new ArrayList<>();
    private Map<String, List<ExpectedIndexDescriptor>> expectedIndexes = new LinkedHashMap<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    /** Version of Liquibase that generated the manifest; recorded in the {@code LIQUIBASE} history column. */
    public String getLiquibaseVersion() {
        return liquibaseVersion;
    }

    public void setLiquibaseVersion(String liquibaseVersion) {
        this.liquibaseVersion = liquibaseVersion;
    }

    public List<ChangeSetDescriptor> getChangeSets() {
        return changeSets;
    }

    public void setChangeSets(List<ChangeSetDescriptor> changeSets) {
        this.changeSets = changeSets;
    }

    /** Indexes expected to exist after the full changelog is applied, keyed by dialect. */
    public Map<String, List<ExpectedIndexDescriptor>> getExpectedIndexes() {
        return expectedIndexes;
    }

    public void setExpectedIndexes(Map<String, List<ExpectedIndexDescriptor>> expectedIndexes) {
        this.expectedIndexes = expectedIndexes;
    }
}
