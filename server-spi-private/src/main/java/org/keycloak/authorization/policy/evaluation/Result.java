/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.policy.evaluation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class provides a wrapper around contextual information regarding the evaluation of a policy
 * Specifically, it is meant to hold the evaluation status and identifying information of a single policy as well
 * as that policy's associated policies in a contained map. This allows for management of hierarchy from outside classes
 * As this class can hold an arbitrary amount of references to other instances of this class
 * By accessing the evaluation object on this class you can also surmise the "priority" of the result as compared to other results
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Result {

    private final ResourcePermission permission;
    private final Map<Policy, Result> nestedResults;
    private Evaluation evaluation;

    public Result(ResourcePermission permission, Evaluation evaluation) {
        this.permission = permission;
        this.evaluation = evaluation;
        this.nestedResults = new LinkedHashMap<>();
    }

    public ResourcePermission getPermission() {
        return permission;
    }

    public Collection<Result> getNestedResults() {
        return nestedResults.values();
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public void addNestedResult(Result result) {
        nestedResults.putIfAbsent(result.getPolicy(), result);
    }

    public Integer getPriority() {
        if(evaluation == null) {
            return 0;
        }
        return evaluation.getPriority();
    }

    public Effect getEffect() {
        return evaluation.getEffect();
    }

    public Policy getPolicy() {
        if(evaluation == null) {
            return null;
        }
        return evaluation.getPolicy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Result result = (Result) o;
        if(evaluation == null || result.evaluation == null) {
            return false;
        }
        return Objects.equals(evaluation.getPolicy(), result.evaluation.getPolicy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(evaluation);
    }
}
