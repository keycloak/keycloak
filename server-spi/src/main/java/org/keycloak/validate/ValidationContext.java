/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validate;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.KeycloakSession;

/**
 * Holds information about the validation state.
 */
public class ValidationContext {

    /**
     * Holds the {@link KeycloakSession} in which the validation is performed.
     */
    private final KeycloakSession session;

    /**
     * Holds the {@link ValidationError} found during validation.
     */
    private Set<ValidationError> errors;

    /**
     * Holds optional attributes that should be available to {@link Validator} implementations.
     */
    private final Map<String, Object> attributes;

    /**
     * Creates a new {@link ValidationContext} without a {@link KeycloakSession}.
     */
    public ValidationContext() {
        this(null, null);
    }

    /**
     * Creates a new {@link ValidationContext} with a {@link KeycloakSession}.
     *
     * @param session
     */
    public ValidationContext(KeycloakSession session) {
        // we deliberately use a LinkedHashSet here to retain the order of errors.
        this(session, null);
    }

    /**
     * Creates a new {@link ValidationContext}.
     *
     * @param session
     * @param errors
     */
    protected ValidationContext(KeycloakSession session, Set<ValidationError> errors) {
        this.session = session;
        this.errors = errors;
        this.attributes = new HashMap<>();
    }

    /**
     * Eases access to {@link Validator Validator's} for nested validation.
     *
     * @param validatorId
     * @return
     */
    public Validator validator(String validatorId) {
        return Validators.validator(session, validatorId);
    }

    /**
     * Adds an {@link ValidationError}.
     *
     * @param error
     */
    public void addError(ValidationError error) {
        if (errors == null)
            errors = new LinkedHashSet<>();
        errors.add(error);
    }

    /**
     * Convenience method for checking the validation status of the current {@link ValidationContext}.
     * <p>
     * This is an alternative to {@code toResult().isValid()} for brief validations.
     *
     * @return
     */
    public boolean isValid() {
        return errors == null || errors.isEmpty();
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public Set<ValidationError> getErrors() {
        return errors != null ? errors : Collections.emptySet();
    }

    /**
     * Creates a {@link ValidationResult} based on the current errors;
     *
     * @return
     */
    public ValidationResult toResult() {
        return new ValidationResult(getErrors());
    }

    @Override
    public String toString() {
        return "ValidationContext{" + "valid=" + isValid() + ", errors=" + errors + ", attributes=" + attributes + '}';
    }
}