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

import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Holds information about the validation state.
 */
public class ValidationContext {

    /**
     * Holds the {@link KeycloakSession} in which the validation is performed.
     */
    private final KeycloakSession session;

    /**
     * Holds the {@link ValidationUseCase}.
     */
    private final ValidationUseCase type;

    /**
     * Holds the {@link ValidationError} found during validation.
     */
    private final Set<ValidationError> errors;

    public ValidationContext(KeycloakSession session, ValidationUseCase type) {
        // we deliberately use a LinkedHashSet here to retain the order of errors.
        this(session, type, new LinkedHashSet<>());
    }

    protected ValidationContext(KeycloakSession session, ValidationUseCase type, Set<ValidationError> errors) {
        this.session = session;
        this.type = type;
        this.errors = errors;
    }

    public void addError(ValidationError error) {
        errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public KeycloakSession getSession() {
        return session;
    }

    public ValidationUseCase getType() {
        return type;
    }

    public Set<ValidationError> getErrors() {
        return errors;
    }

    public ValidationResult toResult() {
        return new ValidationResult(!hasErrors(), Collections.unmodifiableSet(getErrors()));
    }
}