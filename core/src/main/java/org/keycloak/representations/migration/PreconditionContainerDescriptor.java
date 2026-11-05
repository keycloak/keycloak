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

/** Container for the preconditions of a change-set as stored in the migration manifest. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreconditionContainerDescriptor {

    private String onFail;
    private String onError;
    private String onSqlOutput;
    private List<PreconditionDescriptor> conditions = new ArrayList<>();

    public String getOnFail() {
        return onFail;
    }

    public void setOnFail(String onFail) {
        this.onFail = onFail;
    }

    public String getOnError() {
        return onError;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }

    public String getOnSqlOutput() {
        return onSqlOutput;
    }

    public void setOnSqlOutput(String onSqlOutput) {
        this.onSqlOutput = onSqlOutput;
    }

    public List<PreconditionDescriptor> getConditions() {
        return conditions;
    }

    public void setConditions(List<PreconditionDescriptor> conditions) {
        this.conditions = conditions;
    }
}
