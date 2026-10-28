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
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Metadata for a single change-set as stored in the migration manifest. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeSetDescriptor {

    private int order;
    private String id;
    private String author;
    private String changeLogFile;
    private String checkSum;
    private String comments;
    private String description;
    private Set<String> dbms;
    private PreconditionContainerDescriptor preConditions;
    private boolean customTask;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean runAlways;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean runOnChange;
    private List<ChangeSetStep> steps = new ArrayList<>();

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getChangeLogFile() {
        return changeLogFile;
    }

    public void setChangeLogFile(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    public String getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(String checkSum) {
        this.checkSum = checkSum;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getDbms() {
        return dbms;
    }

    public void setDbms(Set<String> dbms) {
        this.dbms = dbms;
    }

    public PreconditionContainerDescriptor getPreConditions() {
        return preConditions;
    }

    public void setPreConditions(PreconditionContainerDescriptor preConditions) {
        this.preConditions = preConditions;
    }

    public boolean isCustomTask() {
        return customTask;
    }

    public void setCustomTask(boolean customTask) {
        this.customTask = customTask;
    }

    public boolean isRunAlways() {
        return runAlways;
    }

    public void setRunAlways(boolean runAlways) {
        this.runAlways = runAlways;
    }

    public boolean isRunOnChange() {
        return runOnChange;
    }

    public void setRunOnChange(boolean runOnChange) {
        this.runOnChange = runOnChange;
    }

    public List<ChangeSetStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ChangeSetStep> steps) {
        this.steps = steps;
    }
}
