/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.idm.authorization;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyRepresentation {

    private String id;
    private String name;
    private String description;
    private String type;
    private Logic logic = Logic.POSITIVE;
    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;
    private Map<String, String> config = new HashMap();
    private List<PolicyRepresentation> dependentPolicies;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<PolicyRepresentation> associatedPolicies = new ArrayList<>();

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DecisionStrategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    public Logic getLogic() {
        return logic;
    }

    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PolicyRepresentation> getAssociatedPolicies() {
        return associatedPolicies;
    }

    public void setAssociatedPolicies(List<PolicyRepresentation> associatedPolicies) {
        this.associatedPolicies = associatedPolicies;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PolicyRepresentation policy = (PolicyRepresentation) o;
        return Objects.equals(getId(), policy.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public void setDependentPolicies(List<PolicyRepresentation> dependentPolicies) {
        this.dependentPolicies = dependentPolicies;
    }

    public List<PolicyRepresentation> getDependentPolicies() {
        return this.dependentPolicies;
    }
}