/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Denotes the result of a validation run.
 */
public class ValidationResult implements Consumer<Consumer<ValidationResult>> {

    /**
     * Singleton denoting that the Validation was successful without errors or warnings.
     */
    public static ValidationResult OK = new ValidationResult(true, Collections.emptyList());

    /**
     * Tells whether the validation outcome is considered valid.
     */
    private final boolean valid;

    /**
     * Holds the {@link ValidationProblem} detected during the validation run.
     */
    private final List<ValidationProblem> problems;

    public ValidationResult(boolean valid, List<ValidationProblem> problems) {
        this.valid = valid;
        this.problems = Collections.unmodifiableList(problems);
    }

    /**
     * @return true, if the Validation was successful without errors or warnings.
     */
    public boolean isOk() {
        return this == OK;
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationProblem> getProblems() {
        return problems;
    }

    public ValidationResult add(ValidationProblem problem) {
        getProblems().add(problem);
        return this;
    }

    public boolean hasProblems() {
        return !getProblems().isEmpty();
    }

    public boolean hasErrors() {
        return getProblems().stream().anyMatch(ValidationProblem::isError);
    }

    public boolean hasWarnings() {
        return getProblems().stream().anyMatch(ValidationProblem::isWarning);
    }

    public List<ValidationProblem> getWarnings() {
        return filter(getProblems().stream(), ValidationProblem::isWarning).collect(Collectors.toList());
    }

    public List<ValidationProblem> getWarnings(ValidationKey... keys) {
        return getWarnings(new HashSet<>(Arrays.asList(keys)));
    }

    public List<ValidationProblem> getWarnings(Set<ValidationKey> keys) {
        return filter(getProblems().stream(), ValidationProblem::isWarning, p -> keys.contains(p.getKey())).collect(Collectors.toList());
    }

    public List<ValidationProblem> getErrors() {
        return filter(getProblems().stream(), ValidationProblem::isError).collect(Collectors.toList());
    }

    public List<ValidationProblem> getErrors(ValidationKey... keys) {
        return getErrors(new HashSet<>(Arrays.asList(keys)));
    }

    public List<ValidationProblem> getErrors(Set<ValidationKey> keys) {
        return filter(getProblems().stream(), ValidationProblem::isError, p -> keys.contains(p.getKey())).collect(Collectors.toList());
    }

    @SafeVarargs
    private final Stream<ValidationProblem> filter(Stream<ValidationProblem> stream, Predicate<ValidationProblem>... preds) {
        Stream<ValidationProblem> filtered = stream;
        for (Predicate<ValidationProblem> pred : preds) {
            filtered = filtered.filter(pred);
        }
        return filtered;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", problems=" + problems +
                '}';
    }

    @Override
    public void accept(Consumer<ValidationResult> consumer) {
        consumer.accept(this);
    }
}
