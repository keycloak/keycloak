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

import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * {@link ValidationContext} for nested {@link Validation Validation's}
 */
public class NestedValidationContext extends ValidationContext {

    private final KeycloakSession session;

    private final ValidationRegistry validationRegistry;

    /**
     * Holds the {@link List} of {@link ValidationProblem ValidationProblem's}
     */
    private final List<ValidationProblem> problems;

    public NestedValidationContext(ValidationContext parent, KeycloakSession session, ValidationRegistry validationRegistry) {
        super(parent);
        this.session = session;
        this.validationRegistry = validationRegistry;
        this.problems = new ArrayList<>();
    }

    public KeycloakSession getSession() {
        return session;
    }

    public List<ValidationProblem> getProblems() {
        return problems;
    }

    public ValidationRegistry getValidationRegistry() {
        return validationRegistry;
    }

    /**
     * @param problem
     */
    public void addProblem(ValidationProblem problem) {
        Objects.requireNonNull(problem, "problem");
        getProblems().add(problem);
    }

    /**
     * @param key
     * @param message
     */
    public void addError(ValidationKey key, String message) {
        addError(key, message, null);
    }

    /**
     * @param key
     * @param message
     */
    public void addError(ValidationKey key, String message, Exception exception) {
        addProblem(ValidationProblem.error(key, message, exception));
    }

    /**
     * @param key
     * @param message
     */
    public void addWarning(ValidationKey key, String message) {
        addProblem(ValidationProblem.warning(key, message));
    }


    /**
     * Provides support for nested validations, e.g. delegate complex field validations to other validations.
     *
     * @param key
     * @param value
     * @return
     */
    public boolean validateNested(ValidationKey key, Object value) {
        return validationRegistry.resolveValidations(this, key, value).stream()
                .allMatch(v -> v.validate(key, value, this));
    }

    public boolean evaluateAndReportErrorIfFalse(BooleanSupplier supplier, ValidationKey key, String message) {
        boolean result = supplier.getAsBoolean();
        if (!result) {
            addError(key, message);
        }
        return result;
    }
}
